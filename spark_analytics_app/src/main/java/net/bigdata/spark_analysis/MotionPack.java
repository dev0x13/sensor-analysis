package net.bigdata.spark_analysis;

import java.io.Serializable;
import java.util.HashMap;

public class MotionPack implements Serializable {
    public static class MotionEvent implements Serializable {
        public String label;
        public float[] data;

        public MotionEvent() {}

        public MotionEvent(String label, float[] data) {
            this.label = label;
            this.data = data;
        }
    }

    public String username;
    public HashMap<String, HashMap<String, MotionEvent>> data;

    public MotionPack() {}

    public MotionPack(String username, HashMap<String, HashMap<String, MotionEvent>> data) {
        this.username = username;
        this.data = data;
    }
}
