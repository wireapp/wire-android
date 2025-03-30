package com.wearezeta.auto.common.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogsConverter {
    public static final String ZIP_MIME_TYPE = "application/zip";

    public static byte[] toZipBytearray(Map<String, List<String>> logsMapping) {
        try {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(bos)) {
                for (Map.Entry<String, List<String>> zipRecord : logsMapping.entrySet()) {
                    if (zipRecord.getValue() == null || zipRecord.getValue().isEmpty()) {
                        continue;
                    }

                    zos.putNextEntry(new ZipEntry(zipRecord.getKey()));
                    zos.write(String.join("\n", zipRecord.getValue()).getBytes());
                    zos.closeEntry();
                }
                zos.close();
                return bos.toByteArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
