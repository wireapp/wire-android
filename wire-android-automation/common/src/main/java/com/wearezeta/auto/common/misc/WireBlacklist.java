package com.wearezeta.auto.common.misc;

import com.wearezeta.auto.common.Platform;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.s3.S3BucketClient;
import java.util.logging.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WireBlacklist {
    private static final String S3_KEY = "AKIARLM3WMDQWTVSAY6I";
    private static final String S3_SECRET = Credentials.get("BLACKLIST_S3_SECRET");
    private static final String S3_BUCKET = "clientblacklist";

    private static final String MIN_VERSION_KEY = "min_version";
    private static final String EXCLUDE_KEY = "exclude";
    private static final String CURRENT_BACKEND = "staging";

    private static final int DEFAULT_MIN_VERSION = 1;

    private static final Logger log = ZetaLogger.getLog(WireBlacklist.class.getSimpleName());

    private final Platform platform;
    private final S3BucketClient s3BucketClient;

    private int minVersion = DEFAULT_MIN_VERSION;
    private final List<Integer> versionsToExclude = new ArrayList<>();

    public WireBlacklist(Platform platform) {
        this.platform = platform;
        if (S3_SECRET == null) {
            throw new RuntimeException("Please set environment variable BLACKLIST_S3_SECRET locally to be able to run test");
        }
        this.s3BucketClient = new S3BucketClient(S3_BUCKET, S3_KEY, S3_SECRET);
    }

    public int getMinVersion() {
        return minVersion;
    }

    public WireBlacklist setMinVersion(int minVersion) {
        this.minVersion = minVersion;
        return this;
    }

    public List<Integer> getVersionsToExclude() {
        return Collections.unmodifiableList(versionsToExclude);
    }

    public WireBlacklist setVersionsToExclude(List<Integer> versionsToExclude) {
        this.versionsToExclude.clear();
        this.versionsToExclude.addAll(versionsToExclude);
        return this;
    }

    public WireBlacklist setVersionsToExclude(Integer... versionsToExclude) {
        return setVersionsToExclude(Arrays.asList(versionsToExclude));
    }

    private String getS3Path() {
        return String.format("%s/%s", CURRENT_BACKEND, platform.name().toLowerCase());
    }

    public WireBlacklist upload() {
        final File json;
        try {
            json = toFile(File.createTempFile("data", ".json"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            log.info(String.format("Change blacklist on %s", getS3Path()));
            s3BucketClient.uploadFile(json, getS3Path());
        } finally {
            json.delete();
        }
        return this;
    }

    public void uploadMinVersion(int buildNumber) {
        this.setMinVersion(buildNumber);
        this.upload();
    }

    public void uploadExcludeVersion(int buildNumber) {
        this.setVersionsToExclude(buildNumber);
        this.upload();
    }

    public static WireBlacklist uploadDefault(Platform platform) {
        return new WireBlacklist(platform).upload();
    }

    public WireBlacklist download() {
        final File json;
        try {
            json = toFile(File.createTempFile("data", ".json"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            s3BucketClient.downloadFile(getS3Path(), json);
        } finally {
            json.delete();
        }
        return this.fromFile(json);
    }

    private File toFile(File dstFile) {
        final JSONObject result = new JSONObject();
        // In the internal JSON representation all numbers are packed into strings
        result.put(MIN_VERSION_KEY, Integer.toString(getMinVersion()));
        result.put(EXCLUDE_KEY, getVersionsToExclude()
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        try (final PrintWriter writer = new PrintWriter(dstFile, "UTF-8")) {
            result.write(writer, 2, 2);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        log.info(String.format("Blacklist file content: %s", result));
        return dstFile;
    }

    private WireBlacklist fromFile(File srcFile) {
        final JSONObject result = new JSONObject(srcFile);
        if (result.has(MIN_VERSION_KEY)) {
            setMinVersion(Integer.parseInt(result.getString(MIN_VERSION_KEY)));
        } else {
            setMinVersion(DEFAULT_MIN_VERSION);
        }
        if (result.has(EXCLUDE_KEY)) {
            setVersionsToExclude(result.getJSONArray(EXCLUDE_KEY)
                    .toList()
                    .stream()
                    .map(x -> Integer.parseInt((String) x))
                    .collect(Collectors.toList()));
        } else {
            setVersionsToExclude(Collections.emptyList());
        }
        log.fine(String.format("Loaded Wire blacklist %s", result.toString()));
        return this;
    }
}
