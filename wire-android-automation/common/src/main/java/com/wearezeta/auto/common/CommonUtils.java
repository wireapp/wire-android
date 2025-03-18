package com.wearezeta.auto.common;

import com.beust.jcommander.Strings;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.imagecomparator.QRCode;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.*;

public class CommonUtils {

    private static final String ENGLISH_ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private static final String ENGLISH_ALPHANUMERIC_UNDERSCORE = ENGLISH_ALPHANUMERIC + "_";

    private static final String ENGLISH_ALPHANUMERIC_MARKDOWN = ENGLISH_ALPHANUMERIC + "*" + "-" + " ";

    private static final Logger log = ZetaLogger.getLog(CommonUtils.class.getSimpleName());

    public static boolean executeOsCommandWithTimeout(String[] cmd, long timeoutSeconds) throws Exception {
        Process process = Runtime.getRuntime().exec(cmd);
        log.fine("Process started for cmdline " + Strings.join(" ", cmd));
        outputErrorStreamToLog(process.getErrorStream());
        return process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Be careful when using this method - it will block forever if the commands stucks and fails to terminate
     *
     * @param cmd command arguments array
     * @return command return code
     * @throws Exception
     */
    public static int executeOsXCommand(String[] cmd) {
        try {
            final Process process = new ProcessBuilder(cmd).start();
            log.fine("Process started for cmdline " + Strings.join(" ", cmd));
            outputErrorStreamToLog(process.getErrorStream());
            return process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Timedelta DEFAULT_COMMAND_TIMEOUT = Timedelta.ofSeconds(60);

    public static String executeOsXCommandWithOutput(String[] cmd) {
        return executeOsXCommandWithOutput(cmd, DEFAULT_COMMAND_TIMEOUT);
    }

    public static String executeOsXCommandWithOutput(String[] cmd, Timedelta timeout) {
        try {
            final Process process = new ProcessBuilder(cmd).start();
            log.fine("Process started for cmdline " + Strings.join(" ", cmd));
            String output;
            try (InputStream stream = process.getInputStream()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                StringBuilder sb = new StringBuilder("\n");
                String s;
                while ((s = br.readLine()) != null) {
                    sb.append("\t").append(s).append("\n");
                }
                output = sb.toString();
            }
            outputErrorStreamToLog(process.getErrorStream());
            process.waitFor(timeout.asMillis(), TimeUnit.MILLISECONDS);
            return output;
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String setEnvironmentVariableKubeConfigAndExecuteCommand(String backendName, String[] cmd,
                                                                           Timedelta timeout) {
        try {
            // Make sure the column names get changed correctly
            backendName = BackendConnections.get(backendName).getBackendName();
            // Get kubeconfig via environment variable
            String kubeConfigVariable = "KUBECONFIG_" + sanitizeEnvironmentVariable(backendName);
            log.info("Get environment variable " + kubeConfigVariable);
            String kubeConfigLocation = Credentials.get(kubeConfigVariable);
            if (kubeConfigLocation == null) {
                throw new RuntimeException(kubeConfigVariable + " is empty");
            }
            if (!Files.exists(Paths.get(kubeConfigLocation))) {
                throw new RuntimeException("KubeConfig File does not exist or path is incorrect: "
                        + kubeConfigLocation);
            }

            final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Map<String, String> environment = processBuilder.environment();
            environment.put("KUBECONFIG", kubeConfigLocation);

            final Process process = processBuilder.start();
            log.info("Process started for cmdline " + Strings.join(" ", cmd));
            String output;
            try (InputStream stream = process.getInputStream()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                StringBuilder sb = new StringBuilder("\n");
                String s;
                while ((s = br.readLine()) != null) {
                    sb.append("\t").append(s).append("\n");
                }
                output = sb.toString();
            }
            outputErrorStreamToLog(process.getErrorStream());
            process.waitFor(timeout.asMillis(), TimeUnit.MILLISECONDS);
            log.info("Output: " + output);
            return output;
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void outputErrorStreamToLog(InputStream stream) {
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
            throw new IllegalStateException(e);
        }
    }

    public static String generateGUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateRandomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    public static String generateRandomAlphanumericPlusUnderscoreString(int length) {
        return RandomStringUtils.random(length, ENGLISH_ALPHANUMERIC_UNDERSCORE);
    }

    public static String generateLongMarkdownString(int length) {
        return RandomStringUtils.random(length, ENGLISH_ALPHANUMERIC_MARKDOWN);
    }

    public static String generateRandomNumericString(int length) {
        return RandomStringUtils.randomNumeric(length);
    }

    public static String uriEncode(String s) {
        try {
            return URLEncoder.encode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String encodeSHA256Base64(String item) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(item.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        final byte[] digest = md.digest();
        return Base64.encodeBase64String(digest);
    }

    private static class UIScriptExecutionMonitor implements Callable<Void> {

        private File flag;
        private File script;

        UIScriptExecutionMonitor(File flag, File script) {
            this.flag = flag;
            this.script = script;
        }

        @Override
        public Void call() {
            try {
                do {
                    Timedelta.ofMillis(300).sleep();
                } while (this.flag.exists());
                return null;
            } finally {
                this.script.delete();
                if (this.flag.exists()) {
                    this.flag.delete();
                }
            }
        }
    }

    /**
     * It is highly recommended to use these methods if it is necessary to interact with UI from a script. Otherwise it will be
     * blocked by Mac OS as unsecure, because only Terminal.app is explicitly authorized to interact with UI.
     *
     * @param content the full script content, WITHOUT shebang
     * @return monitoring Future. Use it to block execution until shell script execution is done
     * @throws Exception
     */
    public static Future<Void> executeUIShellScript(String[] content) {
        try {
            final File result = File.createTempFile("script", ".sh");

            final File executionFlag = File.createTempFile("execution", ".flag");
            final List<String> scriptContent = new ArrayList<>();
            scriptContent.add("#!/bin/bash");
            Collections.addAll(scriptContent, content);
            scriptContent.add(String.format("rm -f %s", executionFlag.getAbsolutePath()));

            try (Writer output = new BufferedWriter(new FileWriter(result))) {
                output.write(String.join("\n", scriptContent));
            }
            Runtime.getRuntime().exec(new String[]{"chmod", "u+x", result.getAbsolutePath()}).waitFor();
            Runtime.getRuntime().exec(new String[]{"/usr/bin/open", "-a", "Terminal", result.getAbsolutePath(), "-g"})
                    .waitFor();
            return Executors.newSingleThreadExecutor().submit(new UIScriptExecutionMonitor(executionFlag, result));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Future<Void> executeUIAppleScript(String[] content) {
        final List<String> scriptContent = new ArrayList<>();
        scriptContent.add("/usr/bin/osascript \\");
        for (int idx = 0; idx < content.length; idx++) {
            if (idx < content.length - 1) {
                scriptContent.add(String.format("  -e '%s' \\", content[idx]));
            } else {
                scriptContent.add(String.format("  -e '%s'", content[idx]));
            }
        }
        String[] asArray = new String[scriptContent.size()];
        return executeUIShellScript(scriptContent.toArray(asArray));
    }

    public static boolean isRunningOnJenkinsNode() {
        return System.getenv("BUILD_NUMBER") != null;
    }

    public static boolean isDesktop() {
        return System.getProperty("com.wire.app.path") != null;
    }

    public static boolean isWebapp() {
        return System.getProperty("webappApplicationPath") != null;
    }

    /**
     * Create Random Access File
     *
     * @param filePath the file path include the file name
     * @param size     the expected file size, such as 5MB, 10KB, or 4.00MB or 10.00KB or 10kb, 10Kb
     * @throws Exception
     */
    public static void createRandomAccessFile(String filePath, String size) {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "rws")) {
            final long fileSize = getFileSizeFromString(size);
            file.setLength(fileSize);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create Random Movie
     * Notice the reason why the while loop without body is : it waits for the size of Video output chanel
     * greater than the expected size
     * Thus the size of final output file cannot be exact same to your expected size
     *
     * @param filePath          the path you want to save the output video
     * @param size              the expected size of video
     * @param baseImageFilePath the picture you want to use to generate the video
     * @throws Exception
     */
    public static void generateVideoFile(String filePath, String size, String baseImageFilePath) {
        final long fileSizeInByte = getFileSizeFromString(size);
        final Double expectedOverhead = fileSizeInByte * 0.06;
        File file = new File(filePath);
        try {
            SequenceEncoder sequenceEncoder = SequenceEncoder.create24Fps(file);
            BufferedImage in = ImageIO.read(new File(baseImageFilePath));
            // Convert image to a size dividable by 2 for converting into YUV2 color space
            Picture renderedFrame = Picture.create(in.getWidth() * 4, in.getHeight() * 4, ColorSpace.RGB);
            AWTUtil.fromBufferedImage(in, renderedFrame);
            sequenceEncoder.encodeNativeFrame(renderedFrame);
            while (file.length() + expectedOverhead.intValue() < fileSizeInByte) {
                sequenceEncoder.encodeNativeFrame(renderedFrame);
            }
            sequenceEncoder.finish();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static int generateVideoFileWithQRCode(String filePath, String size, String text, int[] dimensions) {
        final long fileSizeInByte = getFileSizeFromString(size);
        final Double expectedOverhead = fileSizeInByte * 0.06;
        File file = new File(filePath);
        try {
            int frameCounter = 0;
            SequenceEncoder sequenceEncoder = SequenceEncoder.create24Fps(file);
            BufferedImage in = QRCode.generateCode(text, Color.BLACK, Color.WHITE, dimensions[1], 1);
            // Convert image to a size dividable by 2 for converting into YUV2 color space
            Picture renderedFrame = Picture.create(in.getWidth(), in.getHeight(), ColorSpace.RGB);
            AWTUtil.fromBufferedImage(in, renderedFrame);
            sequenceEncoder.encodeNativeFrame(renderedFrame);
            while (file.length() + expectedOverhead.intValue() < fileSizeInByte) {
                sequenceEncoder.encodeNativeFrame(renderedFrame);
                frameCounter ++;
            }
            sequenceEncoder.finish();
            // Calculating video duration in seconds
            return (frameCounter/24);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Creates an image with QR codes in corners and center
     */
    public static BufferedImage generateQRImageWithOrientation(String orientation) {
        BufferedImage center = null;
        int width = 0;
        int height = 0;

        BufferedImage topLeft = QRCode.generateCode("Top Left", Color.BLACK, Color.YELLOW, 200, 0);
        BufferedImage botRight = QRCode.generateCode("Bot Right", Color.BLACK, Color.YELLOW, 200, 0);
        if (orientation.equals("landscape")) {
            center = QRCode.generateCode("landscape", Color.BLACK, Color.YELLOW, 200, 0);
            width = 1460;
            height = 640;
        } else if (orientation.equals("portrait")) {
            center = QRCode.generateCode("portrait", Color.BLACK, Color.YELLOW, 200, 0);
            width = 667;
            height = 1500;
        } else {
            throw new IllegalArgumentException("Not a supported image orientation");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        addImage(image, topLeft, 0, 0);
        addImage(image, botRight, width - 200, height - 200);
        addImage(image, center, (width / 2) - 100, (height / 2) - 100);
        return image;
    }

    /**
     * Adds an image onto another one
     */
    private static void addImage(BufferedImage backgroundImage, BufferedImage imageToAdd, int x, int y) {
        Graphics2D g2d = backgroundImage.createGraphics();
        g2d.drawImage(imageToAdd, x, y, null);
        g2d.dispose();
    }

    /**
     * Convert formatted file size such as 50KB, 30.00MB into bytes
     */
    public static long getFileSizeFromString(String size) {
        final String[] sizeParts = size.split("(?<=\\d)\\s*(?=[a-zA-Z])");
        final int fileSize = Double.valueOf(sizeParts[0]).intValue();
        final String type = sizeParts.length > 1 ? sizeParts[1] : "";
        switch (type.toUpperCase()) {
            case "MB":
                return fileSize * 1024 * 1024;
            case "KB":
                return fileSize * 1024;
            default:
                return fileSize;
        }
    }

    /**
     * Create Random audio file in WAV format.
     *
     * @param filePath the path you want to save the output video
     * @param length   length the length in format 00:00 (minutes:seconds) of the audio file.
     */
    public static void generateAudioFile(String filePath, String length) {
        final int expectedLength = getTimeInSecondsFromString(length);
        byte[] pcm_data = new byte[44100 * expectedLength];
        double L1 = 44100.0 / 240.0;
        double L2 = 44100.0 / 245.0;
        for (int i = 0; i < pcm_data.length; i++) {
            pcm_data[i] = (byte) (55 * Math.sin((i / L1) * Math.PI * 2));
            pcm_data[i] += (byte) (55 * Math.sin((i / L2) * Math.PI * 2));
        }

        AudioFormat format = new AudioFormat(44100, 8, 1, true, true);
        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcm_data), format, pcm_data.length / format
                .getFrameSize());
        try {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filePath));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts time that is given as a String in format 00:00 to seconds. Example: "01:12" will return 72 seconds.
     *
     * @param length String in format 00:00
     * @return time in seconds
     */
    public static int getTimeInSecondsFromString(String length) {
        if (!length.contains(":")) {
            throw new IllegalArgumentException("Time does not contain : separator");
        }
        int minutes = Integer.valueOf(length.split(":")[0]);
        int seconds = Integer.valueOf(length.split(":")[1]);
        return minutes * 60 + seconds;
    }

    /**
     * Wait until the block do not throw exception or timeout
     */
    public static <T> Optional<T> waitUntil(Timedelta timeout, Timedelta interval, Callable<T> function) {
        final Timedelta started = Timedelta.now();
        do {
            try {
                return ofNullable(function.call());
            } catch (Exception e) {
               //log.fine(String.format("'Wait until' block has caught an exception: %s", e.getMessage()));
            }
            interval.sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout));
        return Optional.empty();
    }

    public static <T> Optional<T> waitUntil(Timedelta timeout, Timedelta interval, Function<Integer, T> function) {
        final Timedelta started = Timedelta.now();
        int retry = 0;
        do {
            try {
                return ofNullable(function.apply(retry++));
            } catch (Exception e) {
                //log.fine(String.format("'Wait until' block has caught an exception: %s", e.getMessage()));
            }
            interval.sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout));
        return Optional.empty();
    }

    /**
     * Wait until the block get true
     *
     * Deprecated: Use selenium's FluentWait instead
     */
    @Deprecated
    public static boolean waitUntilTrue(Timedelta timeout, Timedelta interval, Supplier<Boolean> function) {
        final Timedelta started = Timedelta.now();
        do {
            if (function.get()) {
                return true;
            }
            interval.sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout));
        return function.get();
    }

    @Deprecated
    public static boolean waitUntilTrue(Timedelta timeout, Timedelta interval, Function<Integer, Boolean> function) {
        final Timedelta started = Timedelta.now();
        int retry = 0;
        do {
            if (function.apply(retry++)) {
                return true;
            }
            interval.sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout));
        return function.apply(retry);
    }

    private static final int BUFFER_SIZE = 4096;

    private static void extractFile(ZipInputStream zipIn, File resultFile) {
        byte[] bytesIn = new byte[BUFFER_SIZE];
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(resultFile))) {
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static File extractAppFromIpa(File ipaFile)  {
        if (!ipaFile.exists()) {
            throw new IllegalArgumentException(String.format(
                    "Please make sure the file %s exists and is accessible", ipaFile.getAbsolutePath())
            );
        }
        File result = null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(ipaFile))) {
            final Path root = Files.createTempDirectory(null);
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                try {
                    final String entryName = zipEntry.getName();
                    final File currentPath = new File(root.toString() + File.separator + entryName);
                    if (result == null && entryName.endsWith(".app/")) {
                        result = currentPath;
                    }
                    if (entryName.contains(".app")) {
                        if (zipEntry.isDirectory()) {
                            if (!currentPath.mkdirs()) {
                                throw new IllegalStateException(String.format(
                                        "Cannot create %s output folder", currentPath.getCanonicalPath())
                                );
                            }
                        } else {
                            extractFile(zis, currentPath);
                        }
                    }
                } finally {
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return ofNullable(result).orElseThrow(
                () -> new IllegalArgumentException(String.format("Cannot find a compressed .app inside %s",
                        ipaFile.getAbsolutePath()))
        );
    }

    /**
     * Create a markdown URL
     * @param text the text that should be displayed
     * @param url the URL that should be opened once the text is tapped
     * @return String in markdown format for displaying a URL
     */
    public static String formatMarkdownURL(String text, String url) {
        return "[" + text + "](" + url + ")";
    }

    /**
     * Helper method to create screenshots in png form in the correct element size for the custom logo feature
     * @param image
     * @param fileName
     */
    public static void transformBufferedImageToPNG(BufferedImage image, String filePath, String fileName) {
        try {
            ImageIO.write(image, "png",
                    new File(filePath, fileName));
        } catch (IOException e) {
            log.info(String.format("Could not save the Buffered Image to a PNG at given location '%s'", filePath + fileName));
            e.printStackTrace();
        }
    }

    public static String getPDFContent(URL url) throws Exception {
        File pdf = File.createTempFile("invoicePDF", "pdf");
        log.fine("Copying " + url + " to local temp file invoicePDF.pdf");
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(pdf);
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        PDDocument document = PDDocument.load(pdf);
        PDFTextStripper pdfStriper = new PDFTextStripper();
        String pdfContent = pdfStriper.getText(document);
        document.close();
        return pdfContent;
    }

    // Environment variables can only contain alphanumeric characters and underscores on some shells (bash). This method
    // replaces all other characters with underscores to support compatibility with all shells.
    private static String sanitizeEnvironmentVariable(String variable) {
        return variable.replaceAll("[^0-9a-zA-Z_]", "_");
    }
}
