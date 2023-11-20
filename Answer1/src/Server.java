/*
 * Name       : Parampal Singh
 * Student no.: 7003114
 */

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Tracer;

import java.lang.management.ManagementFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

class Server {
    // Constants for server configuration
    private static final int port = 12335;
    private static final String destinationFolder = "/Users/parampalsingh/desktop/server";

    private static final int REQUEST_LIMIT = 5; // Maximum number of requests allowed in the time window
    private static final long TIME_WINDOW = 1; // Time window in minutes

    // Variables to track request count and time
    private static int requestCount = 0;
    private static long lastRequestTime = System.currentTimeMillis();

    // Logger for server activities
    private static final Logger logger = LoggerFactory.getLogger(FileTransferTask.class);
    public final Tracer tracer = GlobalOpenTelemetry.getTracer("io.opentelemetry.traces.Todo");
    private static final Meter meter = GlobalOpenTelemetry.meterBuilder("io.opentelemetry.metrics.CPU").build();

    // Metric for tracking CPU usage
    static LongCounter cpuUsageCounter = meter
            .counterBuilder("cpu.usage")
            .setDescription("Total CPU usage")
            .setUnit("1")
            .build();

    // Constructor for the Server class
    public Server() {
        // Creating a tracing span for server initialization
        Span span = tracer.spanBuilder("Server Listing").startSpan();
        span.addEvent("Init");
        span.setAttribute("http.method", "GET");
        try {
            ServerSocket serverSocket = new ServerSocket(port); // Initializing the server socket
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                new ServerThread(clientSocket).start(); // Handles each client in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            span.addEvent("End");
            span.end();
        }
    }

    // Nested class for handling client connections in separate threads
    private static class ServerThread extends Thread {
        private Socket s;
        private Tracer tracer;

        // Constructor for ServerThread class
        public ServerThread(Socket clientSocket) {
            this.s = clientSocket;
        }

        /*
         * Run method to be executed when a thread is started
         */
        public void run() {
            // Tracing span for handling file transfer
            // Span span = tracer.spanBuilder("Completing Transfer").startSpan();
            // span.addEvent("Init");
            // span.setAttribute("http.method","POST");

            try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(s.getInputStream()))) {
                if (canProcessRequest()) {
                    // Reading and decrypting data from the client
                    String encryptedData = (String) ois.readObject();
                    String decryptedData = decrypt(encryptedData);

                    // Using ForkJoinPool to execute the file transfer task
                    ForkJoinPool fp = new ForkJoinPool();
                    FileTransferTask ft = new FileTransferTask(decryptedData, destinationFolder);
                    fp.invoke(ft);

                    System.out.println("File transfer from Client to Server Completed: " + s.getInetAddress());

                    // Tracking CPU usage and updating request count
                    double cpuUsage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
                    cpuUsageCounter.add(1);
                    updateRequestCount();
                } else {
                    // Displaying a message when rate limit is exceeded
                    System.out.println("Rate limit exceeded. Ignoring the request.");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                // Closing the tracing span after completing the file transfer
                // span.addEvent("End");
                // span.end();
            }
        }

        /*
         * Method to decrypt the received data
         */
        private String decrypt(String encryptedData) {
            String key = "helloworld123456";
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
                IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getBytes(StandardCharsets.UTF_8));
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

                byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
                return new String(decryptedBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /*
         * Method to check if the server can process the incoming request
         */
        private boolean canProcessRequest() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime > TimeUnit.MINUTES.toMillis(TIME_WINDOW)) {
                // Reset count if the time window has passed
                requestCount = 0;
                lastRequestTime = currentTime;
            }
            return requestCount < REQUEST_LIMIT;
        }

        /*
         * Method to update the request count
         */
        private void updateRequestCount() { requestCount++; }
    }

    public static void main(String[] args) { new Server(); }
}
