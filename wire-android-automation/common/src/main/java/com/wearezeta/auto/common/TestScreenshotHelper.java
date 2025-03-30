package com.wearezeta.auto.common;

import com.wearezeta.auto.common.log.ZetaLogger;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Helper for saving screenshots to the right place for jenkins cucumber report plugin. This class should actually reside on the
 * test execution implementation but for testing purposes it's included in PickleJar for now.
 *
 */
public class TestScreenshotHelper {

    private static final Logger log = ZetaLogger.getLog(TestScreenshotHelper.class.getSimpleName());

    private static final int MAX_SCREENSHOT_WIDTH = 1000;
    private static final int MAX_SCREENSHOT_HEIGHT = 800;

    public String encodeToBase64(byte[] screenshot) throws IOException {
        screenshot = reduceScreenshotSize(screenshot, MAX_SCREENSHOT_WIDTH, MAX_SCREENSHOT_HEIGHT, "jpeg");
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(screenshot);
    }

    public String encodeToBase64(byte[] screenshot, int width, int height) throws IOException {
        screenshot = reduceScreenshotSize(screenshot, width, height, "jpeg");
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(screenshot);
    }

    public String encodeToBase64(BufferedImage screenshot) throws IOException {
        BufferedImage image = scaleTo(screenshot, MAX_SCREENSHOT_WIDTH, MAX_SCREENSHOT_HEIGHT);
        Base64.Encoder encoder = Base64.getEncoder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return encoder.encodeToString(baos.toByteArray());
    }

    private byte[] reduceScreenshotSize(byte[] screenshot, final int maxWidth, final int maxHeight, String format)
            throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(screenshot);
        BufferedImage imgScreenshot = ImageIO.read(in);
        if (imgScreenshot != null) {
            try {
                imgScreenshot = scaleTo(imgScreenshot, maxWidth, maxHeight);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if(format.equals("jpeg")) {
                    ImageOutputStream outputStream = ImageIO.createImageOutputStream(baos);
                    BufferedImage newImage = new BufferedImage(imgScreenshot.getWidth(), imgScreenshot.getHeight(),
                            BufferedImage.TYPE_INT_RGB);
                    newImage.createGraphics().drawImage(imgScreenshot, 0, 0, Color.WHITE, null);
                    ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
                    ImageWriteParam jpegParams = writer.getDefaultWriteParam();
                    jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    jpegParams.setCompressionQuality(0.4f);
                    writer.setOutput(outputStream);
                    writer.write(null, new IIOImage(newImage, null, null), jpegParams);
                    writer.dispose();
                } else {
                    ImageIO.write(imgScreenshot, format, baos);
                }
                return baos.toByteArray();
            } catch (Exception e) {
                log.warning("Could not resize image: " + e.getMessage());
                return screenshot;
            }
        }
        return screenshot;
    }

    private static BufferedImage scaleTo(BufferedImage originalImage, final int maxWidth, final int maxHeight) {
        final int height = originalImage.getHeight();
        final int width = originalImage.getWidth();
        float resizeRatio = 1;
        if (width > maxWidth || height > maxHeight) {
            final float resizeRatioW1 = (float) maxWidth / width;
            final float resizeRatioW2 = (float) maxWidth / height;
            final float resizeRatioH1 = (float) maxHeight / width;
            final float resizeRatioH2 = (float) maxHeight / height;
            float resizeRatioH = (resizeRatioH1 < resizeRatioH2) ? resizeRatioH1 : resizeRatioH2;
            float resizeRatioW = (resizeRatioW1 < resizeRatioW2) ? resizeRatioW1 : resizeRatioW2;
            final float resizeRatioLimitedW = (resizeRatioH > resizeRatioW) ? resizeRatioH : resizeRatioW;
            resizeRatioH = (resizeRatioH1 > resizeRatioH2) ? resizeRatioH1 : resizeRatioH2;
            resizeRatioW = (resizeRatioW1 > resizeRatioW2) ? resizeRatioW1 : resizeRatioW2;
            final float resizeRatioLimitedH = (resizeRatioH < resizeRatioW) ? resizeRatioH : resizeRatioW;
            resizeRatio = (resizeRatioLimitedW < resizeRatioLimitedH) ? resizeRatioLimitedW : resizeRatioLimitedH;
        }
        final int scaledW = Math.round(width * resizeRatio);
        final int scaledH = Math.round(height * resizeRatio);
        BufferedImage resizedImage = new BufferedImage(scaledW, scaledH, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g2d.drawImage(originalImage, 0, 0, scaledW, scaledH, null);
        return resizedImage;
    }

}
