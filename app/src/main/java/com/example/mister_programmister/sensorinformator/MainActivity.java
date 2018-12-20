package com.example.mister_programmister.sensorinformator;

import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    TextView tvText;
    TextView ansText;
    SensorManager sensorManager;
    Sensor sensorLight;
   // Sensor sensorAccel;
    Sensor sensorLinAccel;
    Sensor sensorGravity;
    Sensor sensorRot;
    Sensor sensorStep;
    Sensor sensorPressure;
    Sensor sensorProxim;

    StringBuilder sb = new StringBuilder();

    private Analysis anal;
    private Map<String, float[]> sensorsValues;
    float[] valuesScreen = new float[1];

    PowerManager powerManager;

    private int period = 500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvText = findViewById(R.id.tvText);
        ansText = findViewById(R.id.ansText);
        sensorsValues = new HashMap<>();

        anal = new Analysis(period);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorRot = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorStep = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorProxim = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        sensorLinAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mapInizialization();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(listeners, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listeners, sensorRot, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listeners, sensorStep, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listeners, sensorPressure, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listeners, sensorLinAccel, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listeners, sensorProxim, SensorManager.SENSOR_DELAY_NORMAL);

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showInfo();
                    }
                });
            }
        };
        timer.schedule(task, 0, period);
    }

    void mapInizialization(){
        float a[] = new float[4];
        sensorsValues.put(anal.sLight, a);
        sensorsValues.put(anal.sRot, a);
        sensorsValues.put(anal.sSteps, a);
        sensorsValues.put(anal.sPressure, a);
        sensorsValues.put(anal.sLinAccel, a);
        sensorsValues.put(anal.sProxim, a);
    }

    void showInfo() {
        sb.setLength(0);
        sb.append("Light: " + sensorsValues.get(anal.sLight)[0])
                .append("\n\nRotation: " + sensorsValues.get(anal.sRot)[0] + " " +
                        sensorsValues.get(anal.sRot)[1] + " " + sensorsValues.get(anal.sRot)[2])

                .append("\n\nRot Euler: " + anal.eulerAngels[0] + " " +
                        anal.eulerAngels[1] + " " + anal.eulerAngels[2])

                .append("\n\nMotionDetect: " + sensorsValues.get(anal.sSteps)[0])
                .append("\n\nScreen: " + valuesScreen[0])
                .append("\n\nPressure: " +  sensorsValues.get(anal.sPressure)[0] * 0.750062f)
                .append("\n\nTypingCount: " +  anal.typingCount)
                .append("\n\nProximity: " +  sensorsValues.get(anal.sLinAccel)[0] + " " +
                        sensorsValues.get(anal.sLinAccel)[1] + " " +
                        sensorsValues.get(anal.sLinAccel)[2]);
        tvText.setText(sb);

        ansText.setText(anal.getInfo(sensorsValues));
        valuesScreen[0] = powerManager.isInteractive() ? 1.0f : 0.f;
        sensorsValues.put(anal.sDisplay,  valuesScreen);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(listeners, sensorLight);
    }

    SensorEventListener listeners = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] tmpSens = event.values.clone();
            sensorsValues.put(translate(event), tmpSens);
        }
    };

    private String translate(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                return anal.sRot;
            case Sensor.TYPE_LIGHT:
                return anal.sLight;
            case Sensor.TYPE_STEP_COUNTER:
                return anal.sSteps;
            case Sensor.TYPE_PRESSURE:
                return anal.sPressure;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return anal.sLinAccel;
            case Sensor.TYPE_PROXIMITY:
                return anal.sProxim;
                /*
            case Sensor.TYPE_ACCELEROMETER:
                return anal.sAccel;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return anal.sMagnit;
                */
        }
        return "";
    }

}