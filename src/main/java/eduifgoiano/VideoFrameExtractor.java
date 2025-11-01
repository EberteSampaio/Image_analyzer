package eduifgoiano;

//Capturar e converter frames de vídeo
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

//Manipulação de imagens
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.IJ;

//Importa classes do Java AWT para trabalhar com imagens em BufferedImage
import java.awt.image.BufferedImage;

//Arquivos e caminhos
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class VideoFrameExtractor {

    private final double blurThreshold;
    private final double diffThreshold;
    private final Java2DFrameConverter frameConverter;
    private ImageProcessor previousGrayProcessor;
    private final int filtroEscolhido;

    public VideoFrameExtractor(double blurThreshold, double diffThreshold, int filtroEscolhido) {
        this.blurThreshold = blurThreshold;
        this.diffThreshold = diffThreshold;
        this.frameConverter = new Java2DFrameConverter();
        this.previousGrayProcessor = null;
        this.filtroEscolhido = filtroEscolhido;
    }

    
    public int extractFrames(String videoPath, Path outputDir) {
    	 int savedCount = 0;
         int frameNumber = 0;
        IJ.log("Dentro do metodo extractFrames...");
        IJ.log("Dentro do metodo extractFrames...");
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath);
            frameGrabber.setFormat("mp4");
            frameGrabber.start();
            int totalFrames = frameGrabber.getLengthInFrames();
            
            // Log inicial apenas
            IJ.log("Video: " + videoPath);
            IJ.log("Total de frames: " + totalFrames);
            IJ.log("Processando...");
            
            while (true) {
                Frame currentFrame;
                try {
                    currentFrame = frameGrabber.grabImage();
                } catch (FrameGrabber.Exception e) {
                    IJ.log("Erro ao capturar frame: " + e.getMessage());
                    break;
                }

                if (currentFrame == null || currentFrame.image == null) {
                    break;
                }
                
                frameNumber++;
                
                // Atualizar progresso a cada 10 frames
                if (frameNumber % 10 == 0) {
                    IJ.showProgress(frameNumber, totalFrames);
                    IJ.log("Processando frame " + frameNumber + "/" + totalFrames);
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
                    Path outputDirSobel = outputDir.resolve("filtro/sobel");
                    Files.createDirectories(outputDirSobel);
                    File sobelFile = outputDirSobel.resolve(String.format("sobel_%05d.png", frameNumber)).toFile();
                    IJ.save(new ImagePlus(String.format("sobel_%05d", frameNumber), edgeProcessor), sobelFile.getAbsolutePath());
                } else {
                    edgeProcessor = ImageUtils.applyLaplace(grayProcessor);
                    Path outputDirLaplace = outputDir.resolve("filtro/laplace");
                    Files.createDirectories(outputDirLaplace);
                    File laplaceFile = outputDirLaplace.resolve(String.format("laplace_%05d.png", frameNumber)).toFile();
                    IJ.save(new ImagePlus(String.format("laplace_%05d", frameNumber), edgeProcessor), laplaceFile.getAbsolutePath());
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
                    // Log apenas frames salvos, a cada 5
                    if (savedCount % 5 == 0) {
                        IJ.log("Salvos: " + savedCount + " frames");
                    }
                }

                previousGrayProcessor = grayProcessor.duplicate();
            }
            
            IJ.showProgress(1.0);
            IJ.log("Concluído!");

        } catch (UnsatisfiedLinkError e) {
            IJ.log("Erro de link nativo (FFmpeg não encontrado): " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            IJ.log("Classe faltando: " + e.getMessage());
        } catch (Throwable e) {
            IJ.handleException(e);
        
        } 
        return savedCount;
    }
}