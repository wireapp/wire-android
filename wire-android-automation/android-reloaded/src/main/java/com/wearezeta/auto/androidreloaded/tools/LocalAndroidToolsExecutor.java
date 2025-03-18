package com.wearezeta.auto.androidreloaded.tools;

import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.wearezeta.auto.common.CommonUtils.outputErrorStreamToLog;

public class LocalAndroidToolsExecutor {
    private static final Logger log = ZetaLogger.getLog(LocalAndroidToolsExecutor.class.getSimpleName());

    public static String getAdbOutput(String... args) {
        return getAdbOutput(ADB_COMMAND_TYPE.DEFAULT, args);
    }

    public static String getAaptDumpBadgingOutput(String... args) {
        return getAaptOutput(AAPT_COMMAND_TYPE.DUMP_BADGING, args);
    }

    public static String getAdbShellOutput(final String... commands) {
        final String fullCmd = String.join("; ", commands);
        return getAdbOutput(ADB_COMMAND_TYPE.SHELL, fullCmd);
    }

    private static final Timedelta COMMAND_TIMEOUT = Timedelta.ofMinutes(3);
    private static final File ADB_BINARY = new File("/usr/local/bin/adb");
    private static final File AAPT_BINARY = new File("/usr/local/bin/aapt");

    private enum ADB_COMMAND_TYPE {
        DEFAULT, SHELL, EXEC_OUT
    }

    private enum AAPT_COMMAND_TYPE {
        DUMP_BADGING, DUMP_PERMISSIONS
    }

    private static String getAaptOutput(AAPT_COMMAND_TYPE commandType, String... args) {
        List<String> argumentsPrefix = new ArrayList<String>() {{
            add("dump");
        }};
        switch (commandType) {
            case DUMP_BADGING:
                argumentsPrefix.add("badging");
                break;
            case DUMP_PERMISSIONS:
                argumentsPrefix.add("permissions");
            default:
                throw new IllegalArgumentException(String.format("Unsupported command type %s", commandType));
        }
        final List<String> fullArgs = new ArrayList<>();
        fullArgs.addAll(argumentsPrefix);
        fullArgs.addAll(Arrays.asList(args));
        return getCommandOutput(AAPT_BINARY, fullArgs);
    }

    private static String getAdbOutput(ADB_COMMAND_TYPE commandType, String... args) {
        String argumentsPrefix = null;
        switch (commandType) {
            case DEFAULT:
                break;
            case SHELL:
                argumentsPrefix = "shell";
                break;
            case EXEC_OUT:
                argumentsPrefix = "exec-out";
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported command type %s", commandType));
        }
        final List<String> fullArgs = new ArrayList<>();
        if (argumentsPrefix != null && !argumentsPrefix.isEmpty()) {
            fullArgs.add(argumentsPrefix);
        }
        fullArgs.addAll(Arrays.asList(args));
        return getCommandOutput(ADB_BINARY, fullArgs);
    }

    private static String getCommandOutput(File binary, List<String> args) {
        if (!binary.exists()) {
            throw new IllegalStateException(
                    String.format("%s tool is expected to be accessible at path %s", binary.getName(),
                            binary.getAbsolutePath())
            );
        }

        log.info(String.format("Executing %s command with arguments: %s", binary.getName(), args));
        String[] fullCmd = new ArrayList<String>() {{
            add(binary.getAbsolutePath());
            addAll(args);
        }}.toArray(new String[0]);
        try {
            final Process process = new ProcessBuilder(fullCmd).start();
            final StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String s;
                while ((s = in.readLine()) != null) {
                    result.append(s).append("\n");
                }
                outputErrorStreamToLog(process.getErrorStream());
            }
            process.waitFor(COMMAND_TIMEOUT.asMinutes(), TimeUnit.MINUTES);
            return result.toString().trim();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
