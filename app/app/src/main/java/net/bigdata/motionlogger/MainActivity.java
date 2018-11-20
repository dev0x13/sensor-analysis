package net.bigdata.motionlogger;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.bigdata.motionlogger.sensor_service.MotionLogger;
import net.bigdata.motionlogger.kinesis.KinesisClient;

public class MainActivity extends AppCompatActivity {

    // User states enumeration
    public enum UserState {
        SCREEN_ON("SCREEN_ON"),
        SCREEN_OFF("SCREEN_OFF");

        private final String text;

        UserState(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** A messenger for receiving messages from the service */
    private final Messenger callbackMessenger = new Messenger(new CallbackHandler(this));

    /** A messenger for sending messages to the service */
    private Messenger sensorLoggerService;

    /** A broadcast receiver for screen off detection */
    private BroadcastReceiver broadcastReceiver;

    private KinesisClient kinesisClient;

    /**
     * Call-back handler class for communication with the SensorLoggerService
     */
    private static class CallbackHandler extends Handler {
        private MainActivity mainActivity;

        CallbackHandler(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void handleMessage(Message msg) {}
    }

    private final ServiceConnection motionLoggerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            sensorLoggerService = new Messenger(service);

            try {
                // Init motion logger's labeling
                setMotionLoggerUserState(UserState.SCREEN_ON);

                // Start motion logging
                Message message = Message.obtain(null, MotionLogger.MSG_START_LOGGING);
                sensorLoggerService.send(message);

            } catch (final RemoteException e) {
                System.out.println(e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            sensorLoggerService = null;
        }
    };

    public void setMotionLoggerUserState(UserState userState) {
        Message message = Message.obtain(null, MotionLogger.MSG_SET_LABEL);
        Bundle dataToSend = new Bundle();
        dataToSend.putString(MotionLogger.KEY_LABEL, userState.toString());
        message.setData(dataToSend);

        try {
            sensorLoggerService.send(message);
        } catch (RemoteException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Add screen off/on listener
        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        setMotionLoggerUserState(UserState.SCREEN_OFF);
                    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                        setMotionLoggerUserState(UserState.SCREEN_ON);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(broadcastReceiver, filter);

        // Bind motion logger service
        Intent motionLogger = new Intent(MainActivity.this, MotionLogger.class);
        startService(motionLogger);
        bindService(motionLogger, motionLoggerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unbindService(motionLoggerConnection);
    }
}
