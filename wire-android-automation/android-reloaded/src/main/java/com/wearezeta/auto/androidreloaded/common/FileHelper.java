package com.wearezeta.auto.androidreloaded.common;

import com.google.common.collect.ImmutableList;
import com.wearezeta.auto.androidreloaded.pages.AndroidPage;
import com.wearezeta.auto.common.CommonUtils;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.FilenameHelper;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public class FileHelper extends AndroidPage {

    private static final String SDCARD_EXCHANGE_ROOT = "/sdcard/wire";
    private static final String SDCARD_EXCHANGE_ROOT_10_AND_11 = "/sdcard/wire";
    private static final String SDCARD_DOWNLOADS_ROOT = "/sdcard/download";
    private static final String SDCARD_DOWNLOADS_ROOT_11 = "/sdcard/download";

    public FileHelper(WebDriver driver) {
        super(driver);
    }

    public void pullDownloadedFileFromSdcard(String fileName) {
        String sourceFilePath;
        if (getOSVersion().compareTo(VERSION_11_0) > 0) {
            sourceFilePath = String.format("%s/%s", SDCARD_DOWNLOADS_ROOT_11, fileName);
        } else {
            sourceFilePath = String.format("%s/%s", SDCARD_DOWNLOADS_ROOT, fileName);
        }
        final File destinationFile =
                new File(String.format("%s/%s",
                        Config.current().getBuildPath(this.getClass()), fileName));
        if (destinationFile.exists()) {
            destinationFile.delete();
        }
        byte[] bytes = getDriver().pullFile(sourceFilePath);
        try {
            Files.write(destinationFile.toPath(), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void removeFileFromSdcard(String fileName) {
        String filePathExchange;
        if (getOSVersion().compareTo(VERSION_10_0) > 0) {
            filePathExchange = String.format("%s/%s", SDCARD_EXCHANGE_ROOT_10_AND_11, fileName);
        } else if (getOSVersion().compareTo(VERSION_11_0) > 0) {
            filePathExchange = String.format("%s/%s", SDCARD_EXCHANGE_ROOT_10_AND_11, fileName);
        } else {
            filePathExchange = String.format("%s/%s", SDCARD_EXCHANGE_ROOT, fileName);
        }
        String filePathDownloads;
        if (getOSVersion().compareTo(VERSION_11_0) > 0 ){
            filePathDownloads = String.format("%s/%s", SDCARD_DOWNLOADS_ROOT_11, fileName);
        } else {
            filePathDownloads = String.format("%s/%s", SDCARD_DOWNLOADS_ROOT, fileName);
        }
        AndroidPage.executeShell(getDriver(), "rm -f", ImmutableList.of(String.format("%s", filePathExchange)));
        AndroidPage.executeShell(getDriver(), "rm -f", ImmutableList.of(String.format("%s", filePathDownloads)));
    }
}
