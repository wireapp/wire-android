package com.wearezeta.auto.common.credentials;

import com.wearezeta.auto.common.log.ZetaLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class Credentials {

    private static final Logger log = ZetaLogger.getLog(Credentials.class.getSimpleName());

    public static String get(String id) {
        String environmentVariable = System.getenv(id);
        if (environmentVariable == null) {
            log.info(String.format("Please approve 1Password prompt to read %s...", id));
            // if environment variable is not set search in 1password (not possible on Jenkins)
            return readFrom1Password(id, "password");
        } else {
            log.info(String.format("Received secret from environment variable %s", id));
        }
        return environmentVariable;
    }

    private static String readFrom1Password(String id, String field) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("op", "item", "get", "--vault", "Test Automation", id, "--fields", field, "--reveal");
        builder.directory(new File(System.getProperty("user.home")));
        try {
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            // exit 1 if none or multiple, exit code 0 if only one
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    error.append(line);
                }
                throw new RuntimeException(String.format("1Password found none or multiple items for id '%s':\n%s", id, error));
            }
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Do you have 1Password CLI installed? Starting 1Password CLI failed: "
                    + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException("Could not get password. 1Password CLI failed: " + e.getMessage());
        }
    }
}
