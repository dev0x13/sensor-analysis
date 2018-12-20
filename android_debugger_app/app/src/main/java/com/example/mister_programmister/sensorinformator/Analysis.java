package com.example.mister_programmister.sensorinformator;

import java.util.Map;

public class Analysis {

    private enum Position {
        inHand, inPocket, onTable
    }

    private enum UserState {
        stand, walk, sleep
    }

    private boolean chatting;
    private boolean onStreet;
    private int floor;

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
    public final String sPressure = "Pressure";
    public final String sAccel = "Accelerometr";

    private Position currPos = Position.inHand;
    private UserState currState = UserState.stand;

    private float accelTrack[];
    public final float accelTrackCoeffs[] = {1.f, 0.35f, 0.15f};
    /**
     *
     * @param period - time interval in milliseconds at which data is received
     */
    Analysis(int period){
        this.period = period;
        timeStep = (float) period  / 1000.f;
        accelTrack = new float[3];
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


        accelTrack[(int)(iterations % 3)] = (float)Math.sqrt(data.get(sAccel)[1] * data.get(sAccel)[1] +
                data.get(sAccel)[2] * data.get(sAccel)[2]);
        float accelZAver = (accelTrackCoeffs[0] * accelTrack[(int)(iterations % 3)] +
                            accelTrackCoeffs[1] * accelTrack[(int)((iterations - 1) % 3)] +
                            accelTrackCoeffs[1] *accelTrack[(int)((iterations - 2) % 3)]) /
                (accelTrackCoeffs[0] +  accelTrackCoeffs[1] +  accelTrackCoeffs[2]);

        chatting = false;
        if (accelZAver < 0.3f && accelZAver > 0.063f && data.get(sAccel)[0] < 1.f
                && data.get(sAccel)[1] < 0.7f && data.get(sAccel)[2] < 0.6f)
            chatting = true;

        if(data.get(sDisplay)[0] == 0.1f)
            startSleepIter  = iterations;

        if(data.get(sLight)[0] < 300.f)
            onStreet = false;

        floor = (int)Math.floor((18.4f * 1000 * Math.log10(770.f / (data.get(sPressure)[0] * 0.750062f)) - 20) / 5);

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

        if(onStreet){
            userStateInfo += "on the street";
        } else {
            if(floor >= 1)
                userStateInfo += " in the building on " + floor + " floor";
        }

        if (chatting)
            userStateInfo += "\nUser is typing";

        sb.append(userStateInfo);
        return sb.toString();
    }
}
