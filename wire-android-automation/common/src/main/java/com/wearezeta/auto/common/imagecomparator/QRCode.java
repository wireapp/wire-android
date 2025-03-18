package com.wearezeta.auto.common.imagecomparator;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QRCode {

    public static String readCode(BufferedImage image) throws FormatException, ChecksumException, NotFoundException {
        QRCodeReader reader = new QRCodeReader();
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Hashtable<DecodeHintType, Object> decodeHints = new Hashtable<>();
        decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        final Result decode = reader.decode(bitmap, decodeHints);
        return decode.getText();
    }

    public static List<String> readMultipleCodes(BufferedImage image) throws NotFoundException {
        LuminanceSource ls = new BufferedImageLuminanceSource(image);
        BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(ls));
        Hashtable<DecodeHintType, Object> decodeHints = new Hashtable<>();
        decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        return Stream.of(new QRCodeMultiReader().decodeMultiple(bmp, decodeHints))
                .map(Result::getText)
                .collect(Collectors.toList());
    }

    public static BufferedImage generateCode(String text, Color fgColor, Color bgColor, int size, int borderSize) {
        Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        // Now with zxing version 3.2.1 you could change border size (white border size to just 1)
        hintMap.put(EncodeHintType.MARGIN, borderSize); /* default = 4 */
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix byteMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size,
                    size, hintMap);
            int CrunchifyWidth = byteMatrix.getWidth();
            BufferedImage image = new BufferedImage(CrunchifyWidth, CrunchifyWidth,
                    BufferedImage.TYPE_INT_RGB);
            image.createGraphics();

            Graphics2D graphics = (Graphics2D) image.getGraphics();
            if (bgColor != null) {
                graphics.setColor(bgColor);
                graphics.fillRect(0, 0, CrunchifyWidth, CrunchifyWidth);
            }
            graphics.setColor(fgColor);

            for (int i = 0; i < CrunchifyWidth; i++) {
                for (int j = 0; j < CrunchifyWidth; j++) {
                    if (byteMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }
            return image;
        } catch (WriterException e) {
            throw new IllegalStateException(e);
        }
    }
}
