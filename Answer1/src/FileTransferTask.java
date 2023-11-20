/*
 * Name       : Parampal Singh
 * Student no.: 7003114
 */

import java.io.*;
import java.util.concurrent.RecursiveAction;

class FileTransferTask extends RecursiveAction {
    private String source;
    private String destination;
    private String completed;

    public FileTransferTask(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    protected void compute() {
        // Creating File objects for source and destination paths
        File sourceFile = new File(source);
        File destinationFile = new File(destination);

        // Handling file transfer for individual files
        if (sourceFile.isFile()) {
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                // Reading and writing file content in chunks
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("File transferred: " + source);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                FileTransferTask[] tasks = new FileTransferTask[files.length];
                for (int i = 0; i < files.length; i++) {
                    tasks[i] = new FileTransferTask(files[i].getPath(),
                            destination + File.separator + files[i].getName());
                }
                // Invoking tasks in parallel using ForkJoinPool
                invokeAll(tasks);
            }
        }
    }
}
