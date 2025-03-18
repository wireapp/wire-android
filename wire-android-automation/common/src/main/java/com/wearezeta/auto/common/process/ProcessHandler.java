package com.wearezeta.auto.common.process;

import com.wearezeta.auto.common.log.ZetaLogger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.logging.Logger;

public class ProcessHandler {
    
    private static final Logger log = ZetaLogger.getLog(ProcessHandler.class.getName());
    private static final ScheduledExecutorService POOL = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(POOL::shutdownNow));
    }
    private Process process = null;
    private final String[] cmd;
    private final List<String> output = new CopyOnWriteArrayList<>();
    private Future mergedOutputMonitor = null;
    
    
    public ProcessHandler(String[] cmd) {
        Objects.requireNonNull(cmd);
        this.cmd = cmd;
    }
    
    public ProcessHandler startProcess(long timeout, TimeUnit unit) {
        startProcess();
        try {
            if(!process.waitFor(timeout, unit)){
                log.warning("Process timeout exceeded - stopping process");
            }
        } catch (InterruptedException e) {
            log.warning("Waiting for process failed - stopping process: " + e.getMessage());
        }
        stopProcess();
        return this;
    }
    
    public ProcessHandler startProcess() {
        if (process == null || !process.isAlive()) {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);//merge error and input steam into one stream
            log.fine(String.format("Starting process '%s'", Arrays.toString(cmd)));
            try {
                process = builder.start();
            } catch (IOException e) {
                throw new IllegalStateException("Starting process failed: " + e.getMessage());
            }
            mergedOutputMonitor = createOutputLogger(process.getInputStream());
        } else {
            throw new IllegalStateException("Process is already running");
        }
        return this;
    }
    
    public ProcessHandler stopProcess() {
        log.fine(String.format("Stopping process '%s'", Arrays.toString(cmd)));
        if (process.isAlive()) {
            try {
                int retryCount = 3;
                do {
                    retryCount--;
                    process.destroy();
                    process.waitFor(5, TimeUnit.SECONDS);
                } while (process.isAlive() && retryCount > 0);
                if (process.isAlive()) {
                    log.severe("Could not shutdown process - It may has died");
                }
            } catch (InterruptedException ex) {
                log.severe("Could not shutdown process: " + ex.getMessage());
            }
        }
        try {
            mergedOutputMonitor.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.severe("Could not get output monitor before timeout ends: " + e.getMessage());
        } catch (Exception e) {
            log.severe("Could not get output monitor: " + e.getMessage());
        }
        mergedOutputMonitor.cancel(true);
        return this;
    }
    
    public List<String> getOutput() {
        return output;
    }
    
    public int getExitCode() {
        return process.exitValue();
    }

    private Future createOutputLogger(final InputStream stream) {
        return POOL.submit(() -> {
            try (Scanner sc = new Scanner(stream)) {
                while (sc.hasNext()) {
                    String line = sc.next();
                    output.add(line);
                    log.info(String.format("\t%s", line));
                }
            } catch (Exception e) {
                log.fine("Broken output logger: " + e.getMessage());
            }
        });
    }
    
}
