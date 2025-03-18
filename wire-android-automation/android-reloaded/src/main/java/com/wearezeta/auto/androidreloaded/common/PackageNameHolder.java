package com.wearezeta.auto.androidreloaded.common;

import com.wearezeta.auto.androidreloaded.tools.LocalAndroidToolsExecutor;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.log.ZetaLogger;

import java.util.logging.Logger;

public class PackageNameHolder {
    private static final Logger log = ZetaLogger.getLog(PackageNameHolder.class.getSimpleName());

    private static String cachedAutodetectedPackageName = null;

    public static String getPackageName() {
        if (Boolean.valueOf(Config.current().getPackageAutoDetection(PackageNameHolder.class))) {
            log.info("Auto detect package via aapt...");
            if (cachedAutodetectedPackageName == null) {
                String appPath = getPath();
                if (!appPath.startsWith("http")) {
                    String aaptDumpBadgingOutput = LocalAndroidToolsExecutor.getAaptDumpBadgingOutput(appPath);
                    String packageLine = aaptDumpBadgingOutput.split("\n")[0];
                    if (packageLine.isEmpty()) {
                        throw new IllegalStateException("Package hasn't been found");
                    }
                    String namePart = packageLine.split(":")[1].trim().split(" ")[0];
                    String nameValueWithQuotes = namePart.split("=")[1];
                    cachedAutodetectedPackageName = nameValueWithQuotes.replaceAll("'", "");
                }
            }
            return cachedAutodetectedPackageName;
        }
        return getPackageNameFromConfig();
    }

    private static String getPath() {
        return Config.current().getAndroidApplicationPath(PackageNameHolder.class);
    }

    private static String getPackageNameFromConfig() {
        return Config.current().getAndroidPackage(PackageNameHolder.class);
    }
}
