package com.example.mister_programmister.sensorinformator;

import java.util.Map;

public class Analysis {

    private enum Position {
        inHand, inPocket, onTable
    }

    private enum UserState {
        stand, walk, sleep
    }

    private StringBuilder sb = new StringBuilder();

    private long iterations = 0;
    private long startWalkIter;
    private long startSleepIter;

    private int period;
    private float timeStep; //  period / 1 sec;
    private float currStepCount;

    private final float posE = 0.03f;
    private final float lightE = 3f;

    public final String sLight = "light";
    public final String sRot = "Rotation";
    public final String sDisplay = "Display";
    public final String sSteps = "StepCounter";


    private Position currPos = Position.inHand;
    private UserState currState = UserState.stand;
    /**
     *
     * @param period - time interval in milliseconds at which data is received
     */
    Analysis(int period){
        this.period = period;
        timeStep = (float) period  / 1000.f;
    }

    private void updateInfo(Map<String, float[]> data){
        if(Math.pow(Math.pow(data.get(sRot)[0],2) + Math.pow(data.get(sRot)[1],2),0.5) < posE) {
            currPos = Position.onTable;
            startSleepIter = iterations;
        } else {
            if (data.get(sLight)[0] <lightE  && data.get(sDisplay)[0] == 0.f) {
                currPos = Position.inPocket;
            } else {
                currPos = Position.inHand;
            }
        }

        if(iterations * timeStep < 3.f) { // first 3 seconds don't count
            currStepCount = data.get(sSteps)[0];
            return;
        }

        if (data.get(sSteps)[0] - currStepCount > 1) {
            currState = UserState.walk;
            startWalkIter = iterations;
        }
        else {
            if ((iterations - startWalkIter) * timeStep > 3.5f)
                currState = UserState.stand;

            if ((iterations - startSleepIter) * timeStep > 3600.f) { // 3 hours
                currState = UserState.sleep;
            }
        }
        if(data.get(sDisplay)[0] == 0.1f)
            startSleepIter  = iterations;

        currStepCount = data.get(sSteps)[0];
    }

    public String getInfo(Map<String, float[]> data){
        updateInfo(data);
        iterations++;

        String positionInfo = "The smartphone is";
        switch (currPos) {
            case onTable:
                positionInfo += " on the table";
                break;
            case inHand:
                positionInfo += " in the hands";
                break;
            case inPocket:
                positionInfo += " in the pocket";
                break;
        }

        sb.setLength(0);
        sb.append(positionInfo);

        String userStateInfo = "\n";
        switch (currState) {
            case sleep:
                userStateInfo += "User is sleeping";
                break;
            case walk:
                userStateInfo += "User is walking";
                break;
            case stand:
                userStateInfo += "User is standing";
                break;
        }
        sb.append(userStateInfo);
        return sb.toString();
    }
}
