package org.aksw.iguana.tp.utils;

import org.apache.commons.lang.SystemUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CLIProcessManager {

    public static Process createProcess(String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            if (SystemUtils.IS_OS_LINUX) {

                processBuilder.command("bash", "-c", command);

            } else if (SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd.exe", "-c", command);
            }
            process = processBuilder.start();

        } catch (IOException e) {
            System.out.println("New process could not be created: " + e.getLocalizedMessage());
        }

        return process;
    }

    public static void destroyProcess(Process process) {
        process.destroyForcibly();
    }

    public static Process destroyAndCreateNewProcess(Process process, String command) {
        destroyProcess(process);
        return createProcess(command);
    }

    public static List<Process> createProcesses(int n, String command) {
        List<Process> processList = new ArrayList<>(5);
        for (int i = 0; i < n; i++) {
            processList.add(createProcess(command));
        }

        return processList;
    }

    public static long countLinesUntilStringOccurs(Process process, String successString, String errorString) throws IOException {
        String line;
        System.out.println("Will look for: " + successString + " or as error: " + errorString);
        StringBuilder output = new StringBuilder();

        long size = -1;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        try {
            while ((line = reader.readLine()) != null) {
                if (line.contains(errorString)) {
                    System.out.println("Found error");
                    System.out.println("Query finished with " + errorString);

                    throw new IOException(line);
                } else if (line.contains(successString)) {
                    System.out.println("Query finished with " + successString);
                    break;
                }

                // Only save first 1000 lines of the output
                if (size < 1000) {
                    output.append(line).append("\n");
                }
                size++;
            }

        } catch (IOException e) {
            System.out.println("Exception in reading the output of the process: " + e.getLocalizedMessage());
            throw e;
        }

        System.out.println(output.substring(0, Math.min(1000, output.length())));
        return size;
    }

    public static void executeCommand(Process process, String command) throws IOException {
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        output.write(command + "\n");
        output.flush();
    }

    public static boolean isReaderReady(Process process) throws IOException {
        return new BufferedReader(new InputStreamReader(process.getInputStream())).ready();
    }
}
