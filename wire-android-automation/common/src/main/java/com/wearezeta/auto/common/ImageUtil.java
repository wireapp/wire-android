package com.wearezeta.auto.common;

import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import nu.pattern.OpenCV;
import java.util.logging.Logger;
import org.opencv.core.*;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.SIFT;
import org.opencv.imgproc.Imgproc;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ImageUtil {

    public static final int RESIZE_NORESIZE = 0;
    public static final int RESIZE_TEMPLATE_TO_REFERENCE_RESOLUTION = 1;
    public static final int RESIZE_REFERENCE_TO_TEMPLATE_RESOLUTION = 2;
    public static final int RESIZE_TEMPLATE_TO_RESOLUTION = 5;
    public static final int RESIZE_TO_MAX_SCORE = 7;

    private static final Logger log = ZetaLogger.getLog(ImageUtil.class.getSimpleName());

    static {
        if (Config.common().isOpenCVEnabled(ImageUtil.class)) {
            log.info("Loading OpenCV libraries...");
            OpenCV.loadShared();
        }
    }

    private static Mat convertImageToOpenCVMat(BufferedImage image) {
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer())
                .getData();
        Mat imageMat = new Mat(image.getHeight(), image.getWidth(),
                CvType.CV_8UC3);
        imageMat.put(0, 0, pixels);
        return imageMat;
    }

    private static BufferedImage convertToBufferedImageOfType(
            BufferedImage original, int type) {
        if (original == null) {
            throw new IllegalArgumentException("original == null");
        }

        // Create a buffered image
        BufferedImage image = new BufferedImage(original.getWidth(),
                original.getHeight(), type);

        // Draw the image onto the new buffer
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(original, 0, 0, null);
        } finally {
            g.dispose();
        }

        return image;
    }

    public static Mat resizeFirstMatrixToSecondMatrixResolution(Mat first,
                                                                Mat second) {
        Mat result;
        if (first.width() != second.width() || first.height() != second.height()) {
            result = new Mat();
            Size sz = new Size(second.width(), second.height());
            Imgproc.resize(first, result, sz);
        } else {
            result = first;
        }
        return result;
    }

    public static Mat resizeMatrixToResolution(Mat matrix, int exWidth,
                                               int exHeight) {
        Mat result;
        if (matrix.width() != exWidth || matrix.height() != exHeight) {
            result = new Mat();
            Size sz = new Size(exWidth, exHeight);
            Imgproc.resize(matrix, result, sz);
        } else {
            result = matrix;
        }
        return result;
    }

    public static double getOverlapScore(BufferedImage refImage, BufferedImage tplImage) {
        return getOverlapScore(refImage, tplImage, RESIZE_TO_MAX_SCORE);
    }

    public static double getOverlapScore(BufferedImage refImage, BufferedImage tplImage, int resizeMode) {
        if (resizeMode == RESIZE_TO_MAX_SCORE) {
            if (getOverlapScore(refImage, tplImage,
                    RESIZE_TEMPLATE_TO_REFERENCE_RESOLUTION, 1, 1) > getOverlapScore(
                    refImage, tplImage,
                    RESIZE_REFERENCE_TO_TEMPLATE_RESOLUTION, 1, 1)) {
                return getOverlapScore(refImage, tplImage,
                        RESIZE_TEMPLATE_TO_REFERENCE_RESOLUTION, 1, 1);
            } else {
                return getOverlapScore(refImage, tplImage,
                        RESIZE_REFERENCE_TO_TEMPLATE_RESOLUTION, 2, 1);
            }
        } else {
            return getOverlapScore(refImage, tplImage, resizeMode, 1, 1);
        }
    }

    public static double getOverlapScore(BufferedImage refImage,
                                         BufferedImage tplImage, int resizeMode, int exWidth, int exHeight) {
        refImage = convertToBufferedImageOfType(refImage, BufferedImage.TYPE_3BYTE_BGR);
        tplImage = convertToBufferedImageOfType(tplImage, BufferedImage.TYPE_3BYTE_BGR);
        Mat ref = convertImageToOpenCVMat(refImage);
        Mat tpl = convertImageToOpenCVMat(tplImage);
        if (ref.empty() || tpl.empty()) {
            if (ref.empty()) {
                log.warning("ERROR: No reference image found.");
            }
            if (tpl.empty()) {
                log.warning("ERROR: No template image found.");
            }
            return Double.NaN;
        }

        switch (resizeMode) {
            case RESIZE_TEMPLATE_TO_REFERENCE_RESOLUTION:
                tpl = resizeFirstMatrixToSecondMatrixResolution(tpl, ref);
                break;
            case RESIZE_REFERENCE_TO_TEMPLATE_RESOLUTION:
                ref = resizeFirstMatrixToSecondMatrixResolution(ref, tpl);
                break;
            case RESIZE_TEMPLATE_TO_RESOLUTION:
                tpl = resizeMatrixToResolution(tpl, exWidth, exHeight);
                break;
        }

        Mat res = new Mat(ref.rows() - tpl.rows() + 1, ref.cols() - tpl.cols() + 1, CvType.CV_32FC1);
        Imgproc.matchTemplate(ref, tpl, res, Imgproc.TM_CCOEFF_NORMED);

        MinMaxLocResult minMaxLocResult = Core.minMaxLoc(res);
        return minMaxLocResult.maxVal;
    }

    /**
     * Based on http://docs.opencv.org/3.0-beta/doc/py_tutorials/py_feature2d/py_feature_homography/py_feature_homography.html
     */
    public static int getMatches(BufferedImage refImage, BufferedImage tplImage) {
        refImage = convertToBufferedImageOfType(refImage, BufferedImage.TYPE_3BYTE_BGR);
        tplImage = convertToBufferedImageOfType(tplImage, BufferedImage.TYPE_3BYTE_BGR);
        Mat ref = convertImageToOpenCVMat(refImage);
        Mat tpl = convertImageToOpenCVMat(tplImage);

        MatOfKeyPoint kpRef = new MatOfKeyPoint();
        MatOfKeyPoint kpTpl = new MatOfKeyPoint();
        SIFT featureDetector = SIFT.create();
        featureDetector.detect(ref, kpRef);
        featureDetector.detect(tpl, kpTpl);

        Mat desRef = new Mat();
        Mat desTpl = new Mat();
        featureDetector.detectAndCompute(ref, new Mat(), kpRef, desRef);
        featureDetector.detectAndCompute(tpl, new Mat(), kpTpl, desTpl);

        // no matches if REF or TPL is empty
        log.severe("Could not search for matches because either template or reference image matrix was empty");
        if (desRef.empty() || desTpl.empty()) return -1;

        List<MatOfDMatch> matches = new ArrayList<>();
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        matcher.knnMatch(desRef, desTpl, matches, 2);

        List<DMatch> good = new ArrayList<>();
        for (MatOfDMatch match : matches) {
            DMatch m = match.toList().get(0);
            DMatch n = match.toList().get(1);
            if (m.distance < 0.7 * n.distance) {
                good.add(m);
            }
        }
        log.info(good.size() + " good matches!");
        return good.size();
    }

    public static BufferedImage readImageFromFile(String filePath) throws IOException {
        return ImageIO.read(new File(filePath));
    }

    /**
     * Resizes image to the given ratio (use >1 to upscale, or <1 to downscale)
     */
    public static BufferedImage resizeImage(BufferedImage image, float resizeRatio) {
        assert resizeRatio > 0 : "Resize ratio should be positive";
        if (resizeRatio == 1f) return image;
        int w = image.getWidth(), h = image.getHeight();
        int scaledW = Math.round(w * resizeRatio);
        int scaledH = Math.round(h * resizeRatio);
        BufferedImage result = new BufferedImage(scaledW, scaledH, image.getType());
        Graphics2D g2d = result.createGraphics();
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g2d.drawImage(image, 0, 0, scaledW, scaledH, null);
        return result;
    }

    public static BufferedImage cropToSquare(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width) ? height - (height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0) ? 0 : cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0) ? 0 : cropH;
        return image.getSubimage(cropW, cropH, newWidth, newHeight);
    }

    public static BufferedImage tilt(BufferedImage image, double angle) {
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
        int w = image.getWidth(), h = image.getHeight();
        int neww = (int) Math.floor(w * cos + h * sin), newh = (int) Math.floor(h * cos + w * sin);
        BufferedImage result = new BufferedImage(neww, newh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.translate((neww - w) / 2, (newh - h) / 2);
        g.rotate(angle, w / 2, h / 2);
        g.drawRenderedImage(image, null);
        return result;
    }

    /**
     * Calculates average similarity value between 'maxFrames' image frames
     * taken with help of elementStateScreenshoter method
     *
     * @param elementStateScreenshoter the function, which implements screenshoting
     * @param maxFrames                count of frames to compare. Is recommended to set this to 3 or
     *                                 greater
     * @param interfameDelay           minimum delay value between each screenshot. This delay can be
     *                                 greater on real device, because it depends on the actual CPU
     *                                 performance
     * @return overlap value: 0 <= value <= 1
     * @throws Exception
     */
    public static double getAnimationThreshold(Supplier<Optional<BufferedImage>> elementStateScreenshoter,
                                               final int maxFrames, final Timedelta interfameDelay) {
        assert maxFrames >= 3 : "Please set maxFrames value to 3 or greater";
        final List<BufferedImage> timelineScreenshots = new ArrayList<>();
        do {
            timelineScreenshots.add(elementStateScreenshoter.get()
                    .orElseThrow(IllegalStateException::new));
            interfameDelay.sleep();
        } while (timelineScreenshots.size() < maxFrames);
        int idx = 0;
        final List<Double> thresholds = new ArrayList<>();
        while (idx < timelineScreenshots.size() - 1) {
            thresholds.add(getOverlapScore(timelineScreenshots.get(idx + 1),
                    timelineScreenshots.get(idx),
                    ImageUtil.RESIZE_REFERENCE_TO_TEMPLATE_RESOLUTION));
            idx++;
        }
        return thresholds.stream().min(Double::compare).orElse(100.0);
    }

    public static boolean isLandscape(BufferedImage bi) {
        return (bi.getWidth() > bi.getHeight());
    }

    public static BufferedImage scaleTo(BufferedImage originalImage, final int maxWidth, final int maxHeight) {
        int height = originalImage.getHeight();
        int width = originalImage.getWidth();
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
        return ImageUtil.resizeImage(originalImage, resizeRatio);
    }

    /**
     * Converts a screenshot to a byte array containing its JPEG representation with
     * qualirt loss
     *
     * @param screenshot the initial screenshot
     * @param quality    0..100, where 100 is the maximum quality
     * @return the resulting byte array
     * @throws IOException
     */
    public static byte[] toJPEGByteArray(final BufferedImage screenshot, int quality) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed);
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(quality / 100.0f);
            jpgWriter.setOutput(outputStream);
            // Remove transparency
            BufferedImage copy = new BufferedImage(screenshot.getWidth(), screenshot.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = copy.createGraphics();
            try {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
                g2d.drawImage(screenshot, 0, 0, null);
            } finally {
                g2d.dispose();
            }
            jpgWriter.write(null,
                    new IIOImage(copy, null, null), jpgWriteParam);
        } finally {
            jpgWriter.dispose();
        }
        return compressed.toByteArray();
    }

    public static void storeImage(final BufferedImage screenshot, final File outputFile) {
        try {
            if (!outputFile.getParentFile().exists()) {
                // noinspection ResultOfMethodCallIgnored
                outputFile.getParentFile().mkdirs();
            }
            ImageIO.write(screenshot, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] asByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
