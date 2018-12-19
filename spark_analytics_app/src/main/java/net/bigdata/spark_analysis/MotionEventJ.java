package net.bigdata.spark_analysis;

import java.io.Serializable;

public class MotionEventJ implements Serializable {
    public String label;
    public float[] data;

    public MotionEventJ(String label, float[] data) {
        this.label = label;
        this.data = data;
    }
}