package org.aksw.iguana.cc.utils;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI Utils class
 */
public class CLIProcessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CLIProcessManager.class);

    /**
     * Creates a process
     * @param command
     * @return
     */
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
            LOGGER.error("New process could not be created: {}", e);
        }

        return process;
    }

    /**
     * Destroys a process forcibly
     * @param process
     */
    public static void destroyProcess(Process process) {
        process.destroyForcibly();
    }

    /**
     * Short handler for destroyProcess and createProcess
     * @param process
     * @param command
     * @return
     */
    public static Process destroyAndCreateNewProcess(Process process, String command) {
        destroyProcess(process);
        return createProcess(command);
    }

    /**
     * Create n processes of the same command
     * @param n the amount of processes created
     * @param command the command to create the process with
     * @return
     */
    public static List<Process> createProcesses(int n, String command) {
        List<Process> processList = new ArrayList<>(5);
        for (int i = 0; i < n; i++) {
            processList.add(createProcess(command));
        }

        return processList;
    }

    /**
     * Count and returns the no. of lines of one process until a certain string appears,
     * @param process
     * @param successString the string of the process after the no of line should be returned
     * @param errorString the error string, will throw an IOException if this appeared.
     * @return
     * @throws IOException
     */
    public static long countLinesUntilStringOccurs(Process process, String successString, String errorString) throws IOException {
        String line;
        LOGGER.debug("Will look for: {} or as error: {}",successString, errorString);
        StringBuilder output = new StringBuilder();

        long size = -1;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        try {
            while ((line = reader.readLine()) != null) {
                if (line.contains(errorString)) {
                    LOGGER.debug("Found error");
                    LOGGER.debug("Query finished with {}", errorString);

                    throw new IOException(line);
                } else if (line.contains(successString)) {
                    LOGGER.debug("Query finished with {}", successString);
                    break;
                }

                // Only save first 1000 lines of the output
                if (size < 1000) {
                    output.append(line).append("\n");
                }
                size++;
            }

        } catch (IOException e) {
            LOGGER.debug("Exception in reading the output of the process. ", e);
            throw e;
        }

        return size;
    }

    public static void executeCommand(Process process, String command) throws IOException {
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        output.write(command + "\n");
        output.flush();
    }

    /**
     * Checks if the process input stream is ready to be read.
     * @param process
     * @return
     * @throws IOException
     */
    public static boolean isReaderReady(Process process) throws IOException {
        return new BufferedReader(new InputStreamReader(process.getInputStream())).ready();
    }
}
