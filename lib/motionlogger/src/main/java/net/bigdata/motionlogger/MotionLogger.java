package net.bigdata.motionlogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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
import android.util.Log;

import net.bigdata.motionlogger.enums.Status;

import static java.lang.StrictMath.min;

public class MotionLogger extends Service {
    /**
     * Different representations of data are needed (e.g. for serialization)
     */
    public enum SensorDataRepr {
        FLOAT_ARRAY(0),
        DOUBLE_LIST(1);

        private final int value;

        SensorDataRepr(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Message handler for the MotionLogger service
     */
    private static class MessageHandler extends Handler {
        private Messenger client;
        private MotionLogger motionLogger;

        private MessageHandler(MotionLogger motionLogger) {
            this.motionLogger = motionLogger;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    client = msg.replyTo;
                    break;
                case MSG_START_LOGGING:
                    motionLogger.startLogging();
                    break;
                case MSG_STOP_LOGGING:
                    motionLogger.stopLogging();
                    break;
                case MSG_FLUSH:
                    Message message = Message.obtain(null, MSG_FLUSH);
                    Bundle dataToSend = new Bundle();
                    dataToSend.putSerializable(KEY_DATA, motionLogger.flush());
                    message.setData(dataToSend);
                    try {
                        client.send(message);
                    }
                    catch (RemoteException e) {
                        Log.d(TAG, "Send message with data failed");
                    }
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
        public Object data;

        public MotionEvent(String label, Object data) {
            this.label = label;
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
                motionLogger.collectedData.put(sensorType, new HashMap<>());
            }

            switch (motionLogger.sensorDataRepr) {
                case FLOAT_ARRAY:
                    motionLogger.collectedData.get(sensorType)
                            .put(Long.toString(System.currentTimeMillis()), new MotionEvent(motionLogger.label, event.values));

                    break;
                case DOUBLE_LIST:
                    List<Double> valuesList = new ArrayList<>();

                    for (double e : event.values) {
                        valuesList.add(e);
                    }

                    motionLogger.collectedData.get(sensorType)
                            .put(Long.toString(System.currentTimeMillis()), new MotionEvent(motionLogger.label, valuesList));

                    break;
            }
        }

        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {}
    }

    /**
     * Message types
     */
    public final static int MSG_REGISTER_CLIENT = 0;
    public final static int MSG_START_LOGGING = 1;
    public final static int MSG_STOP_LOGGING = 2;
    public final static int MSG_FLUSH = 3;
    public final static int MSG_SET_LABEL = 4;

    public final static String KEY_DATA = "DATA";
    public final static String KEY_LABEL = "LABEL";
    public final static String KEY_DATA_REPR = "DATA_REPR";
    public final static String TAG = "MotionLogger";

    private String label = "";

    private Status status = Status.IDLE;

    private Messenger messenger;

    private SensorManager sensorManager;

    private PowerManager.WakeLock partialWakeLock;

    private ConcurrentMap<String, Map<String, Object>> collectedData;

    private SensorDataRepr sensorDataRepr = SensorDataRepr.FLOAT_ARRAY;

    private void startLogging() {
        if (status == Status.IDLE) {
            collectedData = new ConcurrentHashMap<>();

            List<Sensor> sensorsList = sensorManager.getSensorList(Sensor.TYPE_ALL);

            if (sensorsList != null) {
                for (Sensor s : sensorsList) {
                    sensorManager.registerListener(
                            new CustomSensorEventListener(s.getStringType(), this),
                            sensorManager.getSensorList(s.getType()).get(0),
                            SensorManager.SENSOR_DELAY_FASTEST);
                }
            }

            status = Status.WORKING;
        }
    }

    private HashMap<String, Map<String, Object>> flush() {
        if (status == Status.WORKING) {
            HashMap<String, Map<String, Object>> mapToReturn = new HashMap<>(collectedData);

            collectedData = new ConcurrentHashMap<>();

            return mapToReturn;
        }

        return null;
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
        Bundle extras = intent.getExtras();

        if (extras != null) {
            int dataRepr = min(extras.getInt(KEY_DATA_REPR), SensorDataRepr.values().length - 1);
            sensorDataRepr = SensorDataRepr.values()[dataRepr];
        }

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
