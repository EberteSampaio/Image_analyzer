package edu.ifgoiano;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FrameSplitter {

    public static void validate(String videoPath, String outputPath) throws IOException, FrameGrabber.Exception {
        File outputDirectory = new File(outputPath);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
        grabber.start();

        int frameNumber = 0;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        Frame frame;
        while ((frame = grabber.grab()) != null) {
            BufferedImage bufferedImage = converter.convert(frame);
            if (bufferedImage != null) {
                BufferedImage grayImage = new BufferedImage(
                        bufferedImage.getWidth(),
                        bufferedImage.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY
                );
                grayImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
                File outputFile = new File(outputPath + "/frame_" + frameNumber + ".jpg");
                ImageIO.write(grayImage, "jpg", outputFile);
                frameNumber++;
            }
        }
        grabber.stop();
        grabber.release();
    }
}