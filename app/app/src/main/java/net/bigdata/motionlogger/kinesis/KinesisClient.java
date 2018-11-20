package net.bigdata.motionlogger.kinesis;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.kinesis.kinesisrecorder.KinesisRecorder;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.ValidationUtils;

import java.io.File;

public class KinesisClient {

    public class AWSStaticCredentialsProvider implements AWSCredentialsProvider {

        private final AWSCredentials credentials;

        public AWSStaticCredentialsProvider(AWSCredentials credentials) {
            this.credentials = ValidationUtils.assertNotNull(credentials, "credentials");
        }

        public AWSCredentials getCredentials() {
            return credentials;
        }

        public void refresh() {}
    }


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
