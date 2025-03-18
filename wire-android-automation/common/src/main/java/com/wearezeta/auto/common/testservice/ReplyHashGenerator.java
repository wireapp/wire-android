package com.wearezeta.auto.common.testservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.wearezeta.auto.common.log.ZetaLogger;
import java.util.logging.Logger;

public class ReplyHashGenerator {

    private static final Logger log = ZetaLogger.getLog(ReplyHashGenerator.class.getSimpleName());

    public static String generateHash(String text, Long timestamp) {
        byte[] timestampArray = convertLong(timestamp);
        byte[] textArray = text.getBytes(StandardCharsets.UTF_16);
        byte[] array = concat(textArray, timestampArray);
        log.info(prettyPrint(array));
        String sha256 = calculateSHA256(array);
        log.info(sha256);
        return sha256;
    }

    public static String generateHash(double latitude, double longitude, long timestamp) {
        byte[] latitudeArray = convertLong(Math.round(latitude * 1000));
        byte[] longitudeArray = convertLong(Math.round(longitude * 1000));
        byte[] timestampArray = convertLong(timestamp);
        byte[] array1 = concat(latitudeArray, longitudeArray);
        byte[] array2 = concat(array1, timestampArray);
        log.info(prettyPrint(array2));
        String sha256 = calculateSHA256(array2);
        log.info(sha256);
        return sha256;
    }

    private static byte[] convertLong(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(a);
            outputStream.write(b);
        } catch (IOException e) {
            log.severe("Could not concatenate byte arrays");
        }
        return outputStream.toByteArray();
    }

    private static String calculateSHA256(byte[] byteArray) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            log.severe("Could not find SHA-256 algorithm");
        }
        md.update(byteArray);
        byte[] digest = md.digest();
        return String.format("%064x", new BigInteger(1, digest));
    }

    private static String prettyPrint(byte[] data) {
        String HEX_DIGITS = "0123456789abcdef";
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xff;
            buf.append(HEX_DIGITS.charAt(v >> 4));
            buf.append(HEX_DIGITS.charAt(v & 0xf));
            buf.append(" ");
        }
        return buf.toString();
    }
}
