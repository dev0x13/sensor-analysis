package com.example.mister_programmister.sensorinformator;

import java.util.Map;

public class Analysis {

    private enum Position {
        inHand, inPocket, onTable
    }

    private enum UserState {
        stand, walk, sleep, sit
    }

    private boolean typing;
    private boolean onStreet;
    private boolean speaking;
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

    private final float speakTangLimts[] ={1.2f, 1.75f};
    private final float speakKrenLimts[] ={0.2f, 1.0f};

    public float eulerAngels[];

    public final String sLight = "light";
    public final String sRot = "Rotation";
    public final String sDisplay = "Display";
    public final String sSteps = "StepCounter";
    public final String sPressure = "Pressure";
    public final String sLinAccel = "linear Accelerometr";
    public final String sProxim = "Proximity";
    public final String sMagnit = "Magnetic Field";

    private Position currPos = Position.inHand;
    private UserState currState = UserState.stand;

    private float accelTrack[];
    private final float accelTrackCoeffs[] = {1.f, 0.35f, 0.15f};
    public int typingCount = 0;
    /**
     *
     * @param period - time interval in milliseconds at which data is received
     */
    Analysis(int period){
        this.period = period;
        timeStep = (float) period  / 1000.f;
        accelTrack = new float[3];
        eulerAngels = new float[3];
        typing = false;
    }

    private void updateInfo(Map<String, float[]> data){

        float q[] = new float[4];
        for(int i = 0; i < 3; i++) {
            q[i+1] = data.get(sRot)[i];
        }
        q[0] = data.get(sRot)[3];
        quatToEuler(q);

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
            if ((iterations - startWalkIter) * timeStep > 3.5f) {
                if(eulerAngels[0] > 0.165)
                    currState = UserState.stand;
                else
                    currState = UserState.sit;
            }

            if ((iterations - startSleepIter) * timeStep > 3600.f) { // 3 hours
                currState = UserState.sleep;
            }
        }


        accelTrack[(int)(iterations % 3)] =
                (float)Math.sqrt(data.get(sLinAccel)[1] * data.get(sLinAccel)[1] +
                            data.get(sLinAccel)[2] * data.get(sLinAccel)[2] +
                            data.get(sLinAccel)[0] * data.get(sLinAccel)[0]);

        float accelZAver = (accelTrackCoeffs[0] * accelTrack[(int)(iterations % 3)] +
                            accelTrackCoeffs[1] * accelTrack[(int)((iterations - 1) % 3)] +
                            accelTrackCoeffs[1] *accelTrack[(int)((iterations - 2) % 3)]) /
                (accelTrackCoeffs[0] +  accelTrackCoeffs[1] +  accelTrackCoeffs[2]);


        if (accelZAver < 1.4f && accelZAver > 0.45f)
        {
            if(typingCount < 10)
            typingCount += 3;
        }
        else
        {
            if(typingCount > 0 )
                typingCount -= 2;
        }

        if(typingCount >= 6 && !typing)
        {
            typingCount++;
            typing = true;
        }

        if(typingCount < 4 || data.get(sDisplay)[0] == 0.f)
            typing = false;




        if(data.get(sDisplay)[0] == 0.1f)
            startSleepIter  = iterations;

        if(data.get(sLight)[0] < 300.f)
            onStreet = false;

        floor = (int)Math.floor((18.4f * 1000 * Math.log10(768.f / (data.get(sPressure)[0] * 0.750062f)) - 20) / 5);

        currStepCount = data.get(sSteps)[0];

        speaking = false;
        if(data.get(sProxim)[0] < 1 &&
                eulerAngels[0] > speakTangLimts[0] && eulerAngels[0] < speakTangLimts[1] &&
                ((eulerAngels[1] > speakKrenLimts[0] && eulerAngels[1] < speakKrenLimts[1]) ||
                (eulerAngels[1] > -speakKrenLimts[1] && eulerAngels[1] < -speakKrenLimts[0])))
            speaking = true;
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
            case sit:
                userStateInfo += "User is sitting";
                break;
        }

        if(onStreet){
            userStateInfo += "on the street";
        } else {
            userStateInfo += " in the building on";
            if(floor >= 1)
                userStateInfo += " " + floor + " floor";
        }

        if (typing)
            userStateInfo += "\nUser is typing";

        if (speaking)
            userStateInfo += "\nUser is talking";

        sb.append(userStateInfo);
        return sb.toString();
    }

    private void quatToEuler(float[] q){
        // roll (x-axis rotation)
        double sinr_cosp = +2.0 * (q[1] * q[1] + q[2] * q[3]);
        double cosr_cosp = +1.0 - 2.0 * (q[1] * q[1] + q[2] * q[2]);
        eulerAngels[0] = (float)Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = +2.0 * (q[0] * q[2] - q[3] * q[1]);
        if (Math.abs(sinp) >= 1)
            eulerAngels[1] = (float)Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            eulerAngels[1] = (float)Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = +2.0 * (q[0] * q[3] + q[1] * q[2]);
        double cosy_cosp = +1.0 - 2.0 * (q[2] * q[2] + q[3] * q[3]);
        eulerAngels[2] = (float)Math.atan2(siny_cosp, cosy_cosp);
    }
}
