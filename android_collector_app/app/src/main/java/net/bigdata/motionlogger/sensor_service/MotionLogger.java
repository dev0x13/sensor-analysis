package net.bigdata.motionlogger.sensor_service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import net.bigdata.motionlogger.enums.Status;
import net.bigdata.motionlogger.kinesis.KinesisClient;

public class MotionLogger extends Service {
    /**
     * Message handler for the MotionLogger service
     */
    private static class MessageHandler extends Handler {
        private MotionLogger motionLogger;

        private MessageHandler(MotionLogger motionLogger) {
            this.motionLogger = motionLogger;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_START_LOGGING:
                    motionLogger.startLogging();
                    break;
                case MSG_STOP_LOGGING:
                    motionLogger.stopLogging();
                    break;
                case MSG_SET_LABEL:
                    Bundle dataReceived = msg.getData();
                    if (dataReceived != null) {
                        motionLogger.label = dataReceived.getString(KEY_LABEL);
                    }
                    break;
            }
        }
    }

    private static class MotionEvent implements Serializable {
        public String label;
        public float[] data;

        public MotionEvent(String label, float[] data) {
            this.label = label;
            this.data = data;
        }
    }

    private static class MotionPack implements Serializable {
        public String username;
        public HashMap<String, HashMap<String, MotionEvent>> data;

        public MotionPack(String username, HashMap<String, HashMap<String, MotionEvent>> data) {
            this.username = username;
            this.data = data;
        }
    }

    /**
     * Sensor event listener with a certain sensor
     */
    private static class CustomSensorEventListener implements SensorEventListener {
        private String sensorType;
        private MotionLogger motionLogger;

        private CustomSensorEventListener(String sensorType,
                                          MotionLogger motionLogger) {
            this.sensorType = sensorType;
            this.motionLogger = motionLogger;
        }

        @Override
        public void onSensorChanged(final SensorEvent event) {
            if (!event.sensor.getStringType().equals(sensorType)) {
                return;
            }

            if (!motionLogger.collectedData.containsKey(sensorType)) {
                motionLogger.collectedData.put(sensorType, new HashMap<String, MotionEvent>());
            }

            motionLogger.collectedData.get(sensorType)
                    .put(Long.toString(System.currentTimeMillis()), new MotionEvent(motionLogger.label, event.values));
        }

        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {}
    }

    /**
     * Message types
     */
    public final static int MSG_START_LOGGING = 1;
    public final static int MSG_STOP_LOGGING = 2;
    public final static int MSG_SET_LABEL = 4;

    public final static String KEY_LABEL = "LABEL";
    public final static String TAG = "MotionLogger";

    private String label = "";

    private Status status = Status.IDLE;

    private Messenger messenger;

    private SensorManager sensorManager;

    private PowerManager.WakeLock partialWakeLock;

    private ConcurrentMap<String, HashMap<String, MotionEvent>> collectedData;

    private KinesisClient kinesisClient;

    private String username;

    private void startLogging() {
        if (status == Status.IDLE) {
            collectedData = new ConcurrentHashMap<>();

            List<Sensor> sensorsList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            sensorsList.add(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER));

            if (sensorsList != null) {
                for (Sensor s : sensorsList) {
                    sensorManager.registerListener(
                            new CustomSensorEventListener(s.getStringType(), this),
                            sensorManager.getSensorList(s.getType()).get(0),
                            SensorManager.SENSOR_DELAY_FASTEST);
                }
            }

            status = Status.WORKING;

            String accessKey = "AKIAI7DA2HSJKOZ4I55Q";
            String secretKey = "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";
            kinesisClient = new KinesisClient(this.getDir("kinesis_data_storage", 0), accessKey, secretKey);

            // Create and start data flush periodic task
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Gson gson = new Gson();
                            MotionPack motionPack = new MotionPack(username, new HashMap<>(collectedData));
                            collectedData.clear();
                            String json = gson.toJson(motionPack);
                            kinesisClient.collectData(json.getBytes());
                        }
                    }
                    ).start();
                }
            };

            Timer timer = new Timer();
            timer.schedule(timerTask, 1000, 1000);
        }
    }

    private void stopLogging() {
        if (status == Status.WORKING) {
            status = Status.IDLE;
            collectedData.clear();
        }
    }

    @Override
    public void onCreate() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        messenger = new Messenger(new MessageHandler(this));

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (pm != null) {
            partialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            partialWakeLock.acquire();
        }
    }

    @Override
    public void onDestroy() {
        partialWakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        username = intent.getStringExtra("username");

        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }
}
