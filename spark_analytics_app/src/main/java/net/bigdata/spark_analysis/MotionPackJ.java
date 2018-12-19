package net.bigdata.spark_analysis;

import java.io.Serializable;
import java.util.HashMap;

public class MotionPackJ implements Serializable {
    public String username;
    public HashMap<String, HashMap<String, MotionEventJ>> data;

    public MotionPackJ(String username, HashMap<String, HashMap<String, MotionEventJ>> data) {
        this.username = username;
        this.data = data;
    }

    @Override
    public String toString() {
        return "MotionPack{" +
                "username='" + username + '\'' +
                ", number of events =" + data.size() +
                '}';
    }
}
