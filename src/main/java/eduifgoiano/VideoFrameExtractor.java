package eduifgoiano;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.IJ;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class VideoFrameExtractor {

    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
    }

    private final double blurThreshold;
    private final double diffThreshold;
    private final Java2DFrameConverter frameConverter;
    private ImageProcessor previousGrayProcessor;
    private final int filtroEscolhido;
    private ProgressCallback progressCallback;
    private boolean saveFilterImages;

    public VideoFrameExtractor(double blurThreshold, double diffThreshold, int filtroEscolhido, boolean saveFilterImages) {
        this.blurThreshold = blurThreshold;
        this.diffThreshold = diffThreshold;
        this.frameConverter = new Java2DFrameConverter();
        this.previousGrayProcessor = null;
        this.filtroEscolhido = filtroEscolhido;
        this.progressCallback = null;
        this.saveFilterImages = saveFilterImages;
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    public int extractFrames(String videoPath, Path outputDir) {
        int savedCount = 0;
        int frameNumber = 0;
        
        IJ.log("Iniciando extracao...");
        
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath);
            frameGrabber.setFormat("mp4");
            frameGrabber.start();
            
            int totalFrames = frameGrabber.getLengthInFrames();
            
            IJ.log("Video: " + videoPath);
            IJ.log("Total: " + totalFrames);
            
            if (progressCallback != null) {
                progressCallback.onProgress(0, totalFrames, "Iniciando...");
            }
            
            while (true) {
                Frame currentFrame;
                try {
                    currentFrame = frameGrabber.grabImage();
                } catch (FrameGrabber.Exception e) {
                    IJ.log("Erro frame: " + e.getMessage());
                    break;
                }

                if (currentFrame == null || currentFrame.image == null) {
                    break;
                }
                
                frameNumber++;
               
                if (frameNumber % 10 == 0) {
                    if (progressCallback != null) {
                        progressCallback.onProgress(frameNumber, totalFrames, "Processando frame " + frameNumber + "/" + totalFrames);
                    }
                }
                
                BufferedImage bufferedImage = frameConverter.convert(currentFrame);
                if (bufferedImage == null) {
                    continue;
                }
                
                ImagePlus impColor = new ImagePlus("Frame", bufferedImage);
                ImageProcessor currentProcessor = impColor.getProcessor();       
               
                ImageProcessor grayProcessor = ImageUtils.convertToGrayscale(currentProcessor);
               
                ImageProcessor edgeProcessor;
                if (filtroEscolhido == 2) {
                    edgeProcessor = grayProcessor.duplicate();
                    edgeProcessor.findEdges();
                    
                    if (saveFilterImages) {
                        Path outputDirSobel = outputDir.resolve("filtro/sobel");
                        if (!Files.exists(outputDirSobel)) Files.createDirectories(outputDirSobel);
                        File sobelFile = outputDirSobel.resolve(String.format("sobel_%05d.png", frameNumber)).toFile();
                        IJ.save(new ImagePlus("", edgeProcessor), sobelFile.getAbsolutePath());
                    }
                } else {
                    edgeProcessor = ImageUtils.applyLaplace(grayProcessor);
                    if (saveFilterImages) {
                        Path outputDirLaplace = outputDir.resolve("filtro/laplace");
                        if (!Files.exists(outputDirLaplace)) Files.createDirectories(outputDirLaplace);
                        File laplaceFile = outputDirLaplace.resolve(String.format("laplace_%05d.png", frameNumber)).toFile();
                        IJ.save(new ImagePlus("", edgeProcessor), laplaceFile.getAbsolutePath());
                    }
                }
                
                ImageStatistics stats = edgeProcessor.getStatistics();
                double variance = stats.stdDev * stats.stdDev;
                if (variance < this.blurThreshold) {
                    continue;
                }

                if (previousGrayProcessor != null) {
                    if (ImageUtils.isTooSimilar(grayProcessor, previousGrayProcessor, this.diffThreshold)) {
                        continue;
                    }
                }

                ImagePlus grayImpToSave = new ImagePlus(String.format("frame_%05d", savedCount + 1), grayProcessor);
                File outputFile = outputDir.resolve(String.format("frame_%05d.png", savedCount + 1)).toFile();

                IJ.saveAs(grayImpToSave, "PNG", outputFile.getAbsolutePath());
                if (outputFile.exists()) {
                    savedCount++;
                }

                previousGrayProcessor = grayProcessor.duplicate();
            }
            
            if (progressCallback != null) {
                progressCallback.onProgress(totalFrames, totalFrames, "Concluido: " + savedCount + " frames.");
            }
            
            frameGrabber.stop();
            frameGrabber.release();

        } catch (Throwable e) {
            IJ.handleException(e);
            if (progressCallback != null) {
                progressCallback.onProgress(0, 100, "Erro: " + e.getMessage());
            }
        }
        
        return savedCount;
    }
}