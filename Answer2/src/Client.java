/*
 * Name       : Parampal Singh
 * Student no.: 7003114
 */

import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.*;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.TimeUnit;

public class Client {
    String folder = "/Users/parampalsingh/desktop/client"; // Location of folder where files are located
    private static final String host = "localhost";
    private static final int port = 12345;
    private static String key = "helloworld123456"; // Key to be at least 16 bits
    private static final int requestLimit = 5; // Maximum number of requests allowed in the time window
    private static final long timeWindow = 1; // Time window in minutes
    private static int requestCount = 0;
    private static long lastRequestTime = System.currentTimeMillis();

    // Collections for sample data
    private static List<Long> timestamps = new ArrayList<>();
    private static List<Boolean> allowedList = new ArrayList<>();
    private static List<Integer> requestCountList = new ArrayList<>();

    public Client() {
        System.out.println("Connecting with Server...");
        try {
            System.out.println("Connected, no exceptions found.\n");
            System.out.println("Verifying if request could be made...");
            if (canMakeRequest()) {
                Socket s = new Socket(host, port);
                ObjectOutputStream os = new ObjectOutputStream(new GZIPOutputStream(s.getOutputStream())); // Zips the input file
                String encryptedData = encrypt(folder);
                os.writeObject(encryptedData);
                os.close();
                s.close();
                System.out.println("File Transfer Request Sent.");
                updateRequestCount();
            } else {
                System.out.println("Rate limit exceeded. Request cannot be made.\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Encrypts the given data using AES encryption.
     *
     * @param data The data to be encrypted.
     * @return The encrypted data.
     */
    private String encrypt(String data) {
        try {
            System.out.println("Executing encryption...");
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
     * Checks if a new request can be made based on rate limiting.
     *
     * @return True if the request can be made, false otherwise.
     */
    private boolean canMakeRequest() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime > TimeUnit.MINUTES.toMillis(timeWindow)) {
            requestCount = 0; // Resetting count if the time window has passed
            lastRequestTime = currentTime;
        }
        boolean allowed = false; // Bug: Removed the check for request limit, always blocking requests without checking the limit

        // Log the data for statistical analysis
        System.out.println("Timestamp: " + (currentTime - lastRequestTime) + ", Request Allowed: " + allowed);

        // Sample collection for analysis
        timestamps.add(currentTime - lastRequestTime);
        allowedList.add(allowed);
        requestCountList.add(requestCount);

        return allowed;
        // return requestCount < requestLimit; /*------------ ACTUAL LINE ------------*/
    }

    /*
     * Updates the count of processed requests.
     */
    private void updateRequestCount() { requestCount++; }

    public static void main(String args[]) {
        new Client();

        System.out.println("Data collected when running Client: ");
        System.out.println("Collected Timestamps: " + timestamps);
        System.out.println("Collected Allowed List: " + allowedList);
        System.out.println("Collected Request Count List: " + requestCountList);
    }
}
