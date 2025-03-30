package com.wearezeta.auto.common.driver;

import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.process.AsyncProcess;
import java.util.logging.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;

public class AppiumLocalServer {

    private static final Logger log = ZetaLogger.getLog(AppiumLocalServer.class.getSimpleName());
    private static final String APPIUM_URL = Config.current().getAppiumUrl(AppiumLocalServer.class);
    private static final boolean enableAppiumOutput = Config.current().enableAppiumOutput(AppiumLocalServer.class);
    private static final int CONNECT_TIMEOUT_MS = 500;
    private static final int READ_TIMEOUT_MS = 1000;
    private static AsyncProcess process;

    public static void start() {
        log.info("Start local appium server (because not on grid and no other server is running)...");
        // Log PATH for better debugging
        log.info("PATH = " + System.getenv("PATH"));
        // Run appium via AsyncProcess with --relaxed-security (error and standard output are logged)
        process = new AsyncProcess(new String[]{"appium", "--relaxed-security"}, enableAppiumOutput, true);
        process.restart();
        waitUntilIsRunning();
    }

    public static void stop() {
        process.stop(Timedelta.ofSeconds(5));
    }

    public static boolean isRunning() {
        try {
            URL url = new URL(APPIUM_URL);
            if (url.getHost().equals("localhost") || url.getHost().equals("127.0.0.1")) {
                if (isDefaultPortUsed(url.getHost(), url.getPort())) {
                    return true;
                }
            }
        } catch (MalformedURLException e) {
            log.severe("Appium URL is malformed: " + APPIUM_URL);
        }
        return false;
    }

    private static boolean isDefaultPortUsed(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);

            // If the code makes it this far without an exception it means
            // something is using the port and has responded.
            log.info(String.format("Port %s is not available - seems appium is already running locally on %s",
                    port, APPIUM_URL));
            return true;
        } catch (IOException e) {
            log.warning(String.format("Port %s seems to be available - this means appium is not running on %s",
                    port, APPIUM_URL));
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    log.severe("Could not close connection for used port test: " + e.getMessage());
                }
            }
        }
        return false;
    }

    private static void waitUntilIsRunning() {
        try {
            URL url = new URL(APPIUM_URL + (isOnGrid() ? "/status" : "/sessions"));
            waitUntilAvailable(url, Duration.ofSeconds(5), Duration.ofMillis(500));
        } catch (MalformedURLException e) {
            log.info("Malformed URL: " + e.getMessage());
        }
    }

    private static void waitUntilAvailable(URL url, Duration timeout, Duration interval) {
        final Timedelta started = Timedelta.now();
        do {
            HttpURLConnection connection = null;
            try {
                log.info("Polling " + url);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return;
                }
            } catch (IOException e) {
                // Ok, try again.
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            Timedelta.ofDuration(interval).sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, Timedelta.ofDuration(timeout)));

        throw new RuntimeException("Local appium server was not started on " + APPIUM_URL);
    }

    private static boolean isOnGrid() {
        return Config.current().isOnGrid(AppiumLocalServer.class);
    }
}
