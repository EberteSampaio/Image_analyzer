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
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath);
        int savedCount = 0;
        int frameNumber = 0;

        try {
        	//se nao existir, mandei criar o diretorio fds
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            frameGrabber.start();
IJ.log("Processando video: " + videoPath);
IJ.log("Total de frames no video: " + frameGrabber.getLengthInFrames());

            while (true) {
                Frame currentFrame;
                try {
                    currentFrame = frameGrabber.grabImage();
                } catch (FrameGrabber.Exception e) {
                    IJ.log("Erro ao pegar frame: " + e.getMessage());
                    break;
                }

                if (currentFrame == null || currentFrame.image == null) { System.out.println("Fim do vídeo ou frame nulo alcançado."); break; }
                frameNumber++;
    IJ.log("Processando frame: " + frameNumber);

                // Conversão do Frame para BufferedImage e depois para ImagePlus
                /* A conversão intermediária para BufferedImage é necessária porque
				Não existe uma conversão direta de Framepara ImagePlus que é suporta no ImageJJ
                OU SEJA É UMA PONTE ENTRE O JAVAC E O IMAGEJ*/
                
                BufferedImage bufferedImage = frameConverter.convert(currentFrame);
                if (bufferedImage == null) { IJ.log("Não foi possível converter o frame " + frameNumber + " para BufferedImage."); continue; }
                
                //Converte para IMAGE plus pra gente conseguir usar no imageJ
                ImagePlus impColor = new ImagePlus("Frame", bufferedImage);
                ImageProcessor currentProcessor = impColor.getProcessor();       
               
                // Converter para escala de cinza
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
                    edgeProcessor = ImageUtils.applyLaplace(grayProcessor); // Laplace
                    Path outputDirLaplace = outputDir.resolve("filtro/laplace");
                    Files.createDirectories(outputDirLaplace);
                    File laplaceFile = outputDirLaplace.resolve(String.format("laplace_%05d.png", frameNumber)).toFile();
                    IJ.save(new ImagePlus(String.format("laplace_%05d", frameNumber), edgeProcessor), laplaceFile.getAbsolutePath());
                }
                
                ImageStatistics stats = edgeProcessor.getStatistics();
                double variance = stats.stdDev * stats.stdDev;
                if (variance < this.blurThreshold) {
        IJ.log("Frame " + frameNumber + " descartado: Desfocado (Variância: " + variance + ")");
                    continue;
                }

                if (previousGrayProcessor != null) {
                    if (ImageUtils.isTooSimilar(grayProcessor, previousGrayProcessor, this.diffThreshold)) {
            IJ.log("Frame " + frameNumber + " descartado: Similar ao anterior");
                        continue;
                    }
                }

                // Salvar o frame
                ImagePlus grayImpToSave = new ImagePlus(String.format("frame_%05d", savedCount + 1), grayProcessor);
                File outputFile = outputDir.resolve(String.format("frame_%05d.png", savedCount + 1)).toFile();

                IJ.saveAs(grayImpToSave, "PNG", outputFile.getAbsolutePath());
                if (outputFile.exists()) {
        IJ.log("Frame " + frameNumber + " salvo como: " + outputFile.getAbsolutePath());
                    savedCount++;
                } else {
                    IJ.log("Falha ao salvar o frame: " + outputFile.getAbsolutePath());
                }

                previousGrayProcessor = grayProcessor.duplicate();
            }

        } catch (FrameGrabber.Exception e) {
            IJ.log("Exceção do FrameGrabber: " + e.getMessage());
        } catch (Exception e) {
            IJ.log("Ocorreu um erro inesperado: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (frameGrabber != null) {
                    frameGrabber.stop();
                    frameGrabber.release();
        IJ.log("FrameGrabber parado e liberado.");
                }
            } catch (FrameGrabber.Exception e) {
                IJ.log("Erro ao parar/liberar o FrameGrabber: " + e.getMessage());
            }
        }
        return savedCount;
    }
}