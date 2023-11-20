/*
 * Name       : Parampal Singh
 * Student no.: 7003114
 */

import java.io.*;
import java.util.concurrent.RecursiveAction;

class FileTransferTask extends RecursiveAction {
    private String source;       // Source file or directory path
    private String destination;  // Destination directory path
    private String completed;    // Flag indicating whether the transfer is completed

    public FileTransferTask(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    /*
     * Recursive action to perform the file transfer task.
     */
    protected void compute() {
        File sourceFile = new File(source);
        File destinationFile = new File(destination);

        if (sourceFile.isFile()) {
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("File transferred: " + source);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (sourceFile.isDirectory()) {
            File[] files = sourceFile.listFiles();
            if (files != null) {
                FileTransferTask[] tasks = new FileTransferTask[files.length];
                for (int i = 0; i < files.length; i++) {
                    tasks[i] = new FileTransferTask(files[i].getPath(),
                            destination + File.separator + files[i].getName());
                }
                invokeAll(tasks);
            }
        }
    }
}
