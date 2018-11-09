package motionlogger_app.bigdata.net.motionloggerapp;

import android.content.Context;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.kinesis.kinesisrecorder.*;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import java.io.File;

public class KinesisClient {
    private CognitoCredentialsProvider credentialsProvider;
    private KinesisRecorder kinesisRecorder;
    private static String identityPoolID = "";
    private static String streamName = "";
    private static Regions region = Regions.DEFAULT_REGION;

    public KinesisClient(Context context, File dir) {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                identityPoolID,
                region);
        kinesisRecorder = new KinesisRecorder(dir, region, credentialsProvider);
    }

    public void collectData(byte[] data) {
        kinesisRecorder.saveRecord(data, streamName);
        kinesisRecorder.submitAllRecords();
    }
}
