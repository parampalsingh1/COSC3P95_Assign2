/*
 * Name       : Parampal Singh
 * Student no.: 7003114
 */

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
    private static final int port = 12345;
    private static final String destinationFolder = "/Users/parampalsingh/desktop/server"; // Location where files to be stored

    private static final int REQUEST_LIMIT = 5; // Maximum number of requests allowed in the time window
    private static final long TIME_WINDOW = 1; // Time window in minutes

    private static int requestCount = 0;
    private static long lastRequestTime = System.currentTimeMillis();

    public Server() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                new ServerThread(clientSocket).start(); // Handles each client in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ServerThread extends Thread {
        private Socket s;

        public ServerThread(Socket clientSocket) {
            this.s = clientSocket;
        }

        /*
         * Run method for the thread. Handles client requests.
         */
        public void run() {
            try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(s.getInputStream()))) {
                if (canProcessRequest()) {
                    String encryptedData = (String) ois.readObject();
                    String decryptedData = decrypt(encryptedData);
                    ForkJoinPool fp = new ForkJoinPool();
                    FileTransferTask ft = new FileTransferTask(decryptedData, destinationFolder);
                    fp.invoke(ft);
                    System.out.println("File transfer from Client to Server Completed: " + s.getInetAddress());
                    updateRequestCount();
                } else {
                    System.out.println("Rate limit exceeded. Ignoring the request.");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /*
         * Decrypts the encrypted data using AES algorithm.
         *
         * @param encryptedData The data to be decrypted.
         * @return The decrypted data.
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
         * Checks if a new client request can be processed based on rate limiting.
         *
         * @return True if the request can be processed, false otherwise.
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
         * Updates the count of processed requests.
         */
        private void updateRequestCount() {requestCount++;}
    }

    public static void main(String[] args) {
        new Server();
    }
}
