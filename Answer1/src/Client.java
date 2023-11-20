/*
 * Name       : Parampal Singh
 * Student no.: 7003114
 */

import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.metrics.Meter;
import org.omg.CORBA.TIMEOUT;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.TimeUnit;


public class Client {
    // Constants for server configuration
    private static final String host = "localhost";
    private static final int port = 12335;
    private static String key = "helloworld123456"; // Key to be at least 16 bits
    private static final int requestLimit = 5; // Maximum number of requests allowed in the time window
    private static final long timeWindow = 1; // Time window in minutes
    private final Meter meter = GlobalOpenTelemetry.meterBuilder("io.opentelemetry.metrics.CPU").build();

    // Metric for tracking CPU usage
    LongCounter cpuUsageCounter = meter
            .counterBuilder("cpu.usage")
            .setDescription("Total CPU usage")
            .setUnit("1")
            .build();

    // Variables to track request count and time
    private static int requestCount = 0;
    private static long lastRequestTime = System.currentTimeMillis();


    public Client() {
        try {
            // Checking if a request can be made based on rate limiting
            if (canMakeRequest()) {
                // Creating a socket connection to the server
                Socket s = new Socket(host, port);
                ObjectOutputStream os = new ObjectOutputStream(new GZIPOutputStream(s.getOutputStream())); // Zips the input file
                String folder = "/Users/parampalsingh/desktop/client";
                String encryptedData = encrypt(folder); // Encrypting the folder path
                os.writeObject(encryptedData);
                os.close();
                s.close();
                System.out.println("File Transfer Request Sent.");
                double cpuUsage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
                cpuUsageCounter.add(1);
                updateRequestCount();
            } else { System.out.println("Rate limit exceeded. Please try again later."); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    /*
     * Method to encrypt data using AES encryption
     */
    private String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Method to check if a request can be made based on rate limiting
     */
    private boolean canMakeRequest() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime > TimeUnit.MINUTES.toMillis(timeWindow)) {
            // Resetting count if the time window has passed
            requestCount = 0;
            lastRequestTime = currentTime;
        }
        return requestCount < requestLimit;
    }

    /*
     * Method to update the request count
     */
    private void updateRequestCount() { requestCount++; }

    public static void main(String args[]) { new Client(); }
}
