package motionlogger_app.bigdata.net.motionloggerapp;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.mobileconnectors.kinesis.kinesisrecorder.*;
import com.amazonaws.regions.Regions;

import java.io.File;

public class KinesisClient {
    private KinesisRecorder kinesisRecorder;
    private static String streamName = "SensorsData";
    private static Regions region = Regions.US_EAST_2;

    public KinesisClient(File dir, String accessKey, String secretKey) {

        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(basicAWSCredentials);

        kinesisRecorder = new KinesisRecorder(
                dir,
                region,
                awsStaticCredentialsProvider
        );
    }

    @SuppressLint("StaticFieldLeak")
    public void collectData(byte[] data) {
        kinesisRecorder.saveRecord(data, streamName);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... v) {
                try {
                    kinesisRecorder.submitAllRecords();
                } catch (AmazonClientException ace) {
                    Log.e("INIT", "Network error.", ace);
                }
                return null;
            }
        }.execute();
    }
}
