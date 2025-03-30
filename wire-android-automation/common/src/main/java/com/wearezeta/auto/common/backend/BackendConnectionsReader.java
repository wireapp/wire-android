package com.wearezeta.auto.common.backend;

import com.beust.jcommander.Strings;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.log.ZetaLogger;
import org.json.JSONArray;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/*
 *  This reader either reads the backend information from a file given via command line parameter (if present).
 *  Or it reads the information by asking 1Password CLI through a shell script. The result is always a JSON.
 */
public class BackendConnectionsReader {

    private final static Logger log = ZetaLogger.getLog(BackendConnectionsReader.class.getSimpleName());
    private final static String commandLineParameter;
    private final static Duration scriptTimeout = Duration.ofSeconds(60);

    static {
        commandLineParameter = Config.common().getBackendConnections(BackendConnectionsReader.class);
    }

    public static JSONArray read() {
        try {
            String pathname;
            if (!commandLineParameter.isEmpty()) {
                pathname = commandLineParameter;
                log.info("Get backend connections from file: " + pathname);
            } else {
                pathname = createFileFrom1PasswordEntries();
            }
            String content = Files.readString(new File(pathname).toPath(), Charset.defaultCharset());
            return new JSONArray(content);
        } catch (IOException e) {
            throw new RuntimeException("Could not read backend connections from file: " + e.getMessage());
        }
    }

    private static String createFileFrom1PasswordEntries() {
        log.info("Get backend connections via shell script from 1Password...");
        File tempfile;
        try (InputStream is = BackendConnectionsReader.class.getResourceAsStream("/backendConnections.sh")) {
            tempfile = File.createTempFile("backendConnections", ".sh");
            Files.copy(is, tempfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Copied common/src/main/resources/backendConnections.sh into tempfile "
                    + tempfile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write content of backendConnections.sh into temporary file"
                    + e.getMessage(), e);
        }

        try {
            String[] cmd = new String[]{"sh", tempfile.getAbsolutePath()};
            log.info("Execute shell script to create JSON for backend connections: " + Strings.join(" ", cmd));
            final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            final Process process = processBuilder.start();
            outputErrorStreamToLog(process.getErrorStream());
            process.waitFor(scriptTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Could not execute shell script: " + e.getMessage(), e);
        }

        // The shell script saves the JSON in the working dir. Get working dir and add JSON file name:
        return Paths.get("").toAbsolutePath() + "/backendConnections.json";
    }

    private static void outputErrorStreamToLog(InputStream stream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder sb = new StringBuilder("\n");
            String s;
            while ((s = br.readLine()) != null) {
                sb.append("\t").append(s).append("\n");
            }
            String output = sb.toString();
            if (!output.trim().isEmpty()) {
                log.warning(output);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read error stream from shell script: " + e.getMessage(), e);
        }
    }

}
