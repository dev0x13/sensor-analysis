package net.bigdata.spark_analysis;
/*
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kinesis.KinesisInputDStream;
import org.apache.spark.streaming.kinesis.KinesisInitialPositions.Latest;

import com.amazonaws.regions.Regions;
import org.apache.spark.streaming.kinesis.SparkAWSCredentials;

public final class SensorJobJava {
    public static void main(String[] args) throws Exception {
        // 1) Set default log4j logging level to WARN to hide Spark logs
        Logger.getRootLogger().setLevel(Level.WARN);

        // 2) Setup Amazon Kinesis properties
        String kinesisAppName = "sensor-data-analyzer";
        String streamName = "SensorsData";
        String endpointUrl = "kinesis.us-east-2.amazonaws.com";

        // 3) Setup Spark Streaming and Kinesis batch interval
        Duration batchInterval = new Duration(1000);

        // 3) Setup the Spark config and StreamingContext
        SparkConf sparkConfig = new SparkConf().setAppName("SensorAnalysisWorker");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConfig, batchInterval);

        // 4) Setup AWS credentials
        // TODO: move to args
        String accessKey = "AKIAI7DA2HSJKOZ4I55Q",
               secretKey = "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";

        SparkAWSCredentials.Builder sparkAWSCredentialsBuilder = new SparkAWSCredentials.Builder();
        sparkAWSCredentialsBuilder.basicCredentials(accessKey, secretKey);
        SparkAWSCredentials credentials = sparkAWSCredentialsBuilder.build();

        // 5) Crete the input stream
        KinesisInputDStream.Builder streamBuilder =
                KinesisInputDStream.builder()
                .streamingContext(jssc)
                .kinesisCredentials(credentials)
                .dynamoDBCredentials(credentials)
                .cloudWatchCredentials(credentials)
                .checkpointAppName(kinesisAppName)
                .streamName(streamName)
                .endpointUrl(endpointUrl)
                .regionName(Regions.US_EAST_2.getName())
                .initialPosition(new Latest())
                .checkpointInterval(batchInterval)
                .storageLevel(StorageLevel.MEMORY_ONLY());

        JavaDStream<byte[]> stream = new JavaDStream<>(streamBuilder.build(), scala.reflect.ClassTag$.MODULE$.apply(byte[].class));

        // 6) Deserialize each byte array
        JavaDStream<MotionPack> motionPacks = stream.map(data -> {
            Gson gson = new Gson();
            String json = new String(data, StandardCharsets.UTF_8);
            return gson.fromJson(json, MotionPack.class);
        });

        // 7) Print result data
        //motionPacks.print();

//        stream.foreachRDD((rdd, time) -> {
//            System.out.printf("Amount of XMLs: %d\n", rdd.count());
//        });

        //events.print();

        // 8) Start the streaming context and await termination
        jssc.start();
        jssc.awaitTermination();
    }
}
*/