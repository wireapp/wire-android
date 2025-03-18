package com.wearezeta.auto.common.process;

import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import java.util.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Optional.ofNullable;

public class AsyncProcess {
    private static final Timedelta IS_RUNNING_CHECK_INTERVAL = Timedelta.ofMillis(100);
    private static final String STDOUT_LOG_PREFIX = "STDOUT: ";
    private static final String STDERR_LOG_PREFIX = "STDERR: ";
    private static final int MAX_LOG_BUFFER_SIZE = 100000;

    private static final Logger log = ZetaLogger.getLog(AsyncProcess.class.getSimpleName());

    private final String[] cmd;
    private final boolean shouldLogStdOut;
    private List<String> stdOut = new CopyOnWriteArrayList<>();
    private List<String> stdErr = new CopyOnWriteArrayList<>();
    private final boolean shouldLogStdErr;
    private Process process;
    private Thread stdOutMonitor;
    private Thread stdErrMonitor;

    public AsyncProcess(String[] cmd, boolean shouldLogStdOut, boolean shouldLogStdErr) {
        this.cmd = cmd;
        this.shouldLogStdOut = shouldLogStdOut;
        this.shouldLogStdErr = shouldLogStdErr;
    }

    private Thread createListenerThread(final BufferedReader reader, final String logPrefix) {
        return new Thread() {
            @Override
            public void run() {
                do {
                    final String logLine;
                    try {
                        if (!reader.ready()) {
                            Timedelta.ofSeconds(1).sleep();
                            continue;
                        }
                        logLine = reader.readLine();
                    } catch (Exception e) {
                        this.interrupt();
                        break;
                    }

                    if (logPrefix.equals(STDOUT_LOG_PREFIX)) {
                        if (AsyncProcess.this.shouldLogStdOut) {
                            log.fine(String.format("%s%s", logPrefix, logLine));
                        }
                        ofNullable(AsyncProcess.this.stdOut).ifPresent(x -> {
                            if (x.size() >= MAX_LOG_BUFFER_SIZE) {
                                x.remove(0);
                            }
                            x.add(logLine);
                        });
                    }
                    if (logPrefix.equals(STDERR_LOG_PREFIX)) {
                        if (AsyncProcess.this.shouldLogStdErr) {
                            log.fine(String.format("%s%s", logPrefix, logLine));
                        }
                        ofNullable(AsyncProcess.this.stdErr).ifPresent(x -> {
                            if (x.size() >= MAX_LOG_BUFFER_SIZE) {
                                x.remove(0);
                            }
                            x.add(logLine);
                        });
                    }
                } while (!this.isInterrupted());
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public synchronized AsyncProcess restart() {
        if (isRunning()) {
            stop(9, new int[]{this.getPid()}, Timedelta.ofSeconds(2));
        }
        log.fine(String.format("Starting Process with command '%s'", Arrays.toString(cmd)));
        try {
            process = new ProcessBuilder(cmd).start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        stdOut.clear();
        stdOutMonitor = createListenerThread(stdout, STDOUT_LOG_PREFIX);
        stdOutMonitor.start();
        final BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        stdErr.clear();
        stdErrMonitor = createListenerThread(stderr, STDERR_LOG_PREFIX);
        stdErrMonitor.start();
        return this;
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    /**
     * @param signal  system signal number, for example 2 is SIGINT. If null then
     *                Java will try to kill the process using the standard lib
     * @param pids    list of system pids to kill
     * @param timeout application termination timeout
     * @throws Exception
     */
    public synchronized void stop(Integer signal, int[] pids, Timedelta timeout) {
        try {
            if (process == null || !isRunning()) {
                return;
            }
            if (signal == null) {
                ofNullable(process).ifPresent(Process::destroy);
            } else {
                for (final int pid : pids) {
                    final String killCmd = String.format("kill -%s %s", signal, pid);
                    log.fine("Executing: " + killCmd);
                    Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", killCmd});
                }
            }
            long milliSecondsElapsed = 0;
            while (isRunning()) {
                IS_RUNNING_CHECK_INTERVAL.sleep();
                milliSecondsElapsed += IS_RUNNING_CHECK_INTERVAL.asMillis();
                if (milliSecondsElapsed >= timeout.asMillis()) {
                    throw new IllegalStateException(
                            String.format("The application %s has not been stopped after %s timeout",
                                    Arrays.toString(this.cmd), timeout));
                }
            }
            log.fine(String.format("The application %s has been successfully stopped after %s millisecond(s)",
                    Arrays.toString(this.cmd), milliSecondsElapsed));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            ofNullable(stdOutMonitor).ifPresent(x -> {
                if (x.isAlive()) {
                    x.interrupt();
                }
            });
            ofNullable(stdErrMonitor).ifPresent(x -> {
                if (x.isAlive()) {
                    x.interrupt();
                }
            });
        }
    }

    public void stop(Timedelta timeout) {
        this.stop(null, null, timeout);
    }

    public String getStdout() {
        return String.join("\n", stdOut);
    }

    public void resetStdOut() {
        stdOut.clear();
    }

    public String getStderr() {
        return String.join("\n", stdErr);
    }

    public void resetStdErr() {
        stdErr.clear();
    }

    /**
     * http://www.golesny.de/p/code/javagetpid
     *
     * @return process id
     */
    public int getPid() {
        if (process == null || !this.isRunning()) {
            throw new IllegalStateException("PID is not available while the process is not running");
        }
        try {
            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = process.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                return f.getInt(process);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        throw new UnsupportedOperationException("getPid implementation is not available for non-Unix systems");
    }
}
