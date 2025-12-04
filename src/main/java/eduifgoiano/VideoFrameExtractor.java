package eduifgoiano;

// Capturar e converter frames de vídeo
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.IJ;

import java.awt.image.BufferedImage;

// Arquivos e caminhos
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class VideoFrameExtractor {

    // Interface para callback de progresso
    public interface ProgressCallback {
        void onProgress(int current, int total, String message);
    }

    private final double blurThreshold;
    private final double diffThreshold;
    private final Java2DFrameConverter frameConverter;
    private ImageProcessor previousGrayProcessor;
    private final int filtroEscolhido;
    private ProgressCallback progressCallback;
    private boolean saveFilterImages = false;

    public VideoFrameExtractor(double blurThreshold, double diffThreshold, int filtroEscolhido) {
        this.blurThreshold = blurThreshold;
        this.diffThreshold = diffThreshold;
        this.frameConverter = new Java2DFrameConverter();
        this.previousGrayProcessor = null;
        this.filtroEscolhido = filtroEscolhido;
        this.progressCallback = null;
        this.saveFilterImages = false;
    }

    // Método para definir o callback de progresso
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    public void setSaveFilterImages(boolean save) {
        this.saveFilterImages = save;
    }

    public int extractFrames(String videoPath, Path outputDir) {
        int savedCount = 0;
        int frameNumber = 0;
        
        IJ.log("Dentro do metodo extractFrames...");
        
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            
            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath);
            frameGrabber.setFormat("mp4");
            frameGrabber.start();
            
            int totalFrames = frameGrabber.getLengthInFrames();
            
            IJ.log("Video: " + videoPath);
            IJ.log("Total de frames: " + totalFrames);
            IJ.log("Processando...");
            
            // Notifica início via callback
            if (progressCallback != null) {
                progressCallback.onProgress(0, totalFrames, "Iniciando extração...");
            }
            
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
                
                // Atualizar progresso a cada 50 frames (menos overhead)
                if (frameNumber % 50 == 0) {
                    IJ.showProgress(frameNumber, totalFrames);
                    IJ.log("Processando frame " + frameNumber + "/" + totalFrames);
                    
                    // Notifica progresso via callback
                    if (progressCallback != null) {
                        progressCallback.onProgress(
                            frameNumber, 
                            totalFrames, 
                            "Processando frame " + frameNumber + "/" + totalFrames
                        );
                    }
                }
                
                BufferedImage bufferedImage = frameConverter.convert(currentFrame);
                if (bufferedImage == null) {
                    continue;
                }
                
                ImagePlus impColor = new ImagePlus("Frame", bufferedImage);
                ImageProcessor currentProcessor = impColor.getProcessor();       
               
                ImageProcessor grayProcessor = ImageUtils.convertToGrayscale(currentProcessor);
                
                // Aplica filtro escolhido
                ImageProcessor edgeProcessor;
                if (filtroEscolhido == 2) {
                    // Filtro Sobel
                    edgeProcessor = grayProcessor.duplicate();
                    edgeProcessor.findEdges();
                    
                    // OTIMIZAÇÃO: Só salva se habilitado
                    if (saveFilterImages) {
                        Path outputDirSobel = outputDir.resolve("filtro/sobel");
                        Files.createDirectories(outputDirSobel);
                        File sobelFile = outputDirSobel.resolve(String.format("sobel_%05d.png", frameNumber)).toFile();
                        IJ.save(new ImagePlus(String.format("sobel_%05d", frameNumber), edgeProcessor), sobelFile.getAbsolutePath());
                    }
                } else {
                    // Filtro Laplace
                    edgeProcessor = ImageUtils.applyLaplace(grayProcessor);
                    
                    // OTIMIZAÇÃO: Só salva se habilitado
                    if (saveFilterImages) {
                        Path outputDirLaplace = outputDir.resolve("filtro/laplace");
                        Files.createDirectories(outputDirLaplace);
                        File laplaceFile = outputDirLaplace.resolve(String.format("laplace_%05d.png", frameNumber)).toFile();
                        IJ.save(new ImagePlus(String.format("laplace_%05d", frameNumber), edgeProcessor), laplaceFile.getAbsolutePath());
                    }
                }
                
                // Verifica se está desfocado
                ImageStatistics stats = edgeProcessor.getStatistics();
                double variance = stats.stdDev * stats.stdDev;
                if (variance < this.blurThreshold) {
                    continue;
                }

                // Verifica similaridade com frame anterior
                if (previousGrayProcessor != null) {
                    if (ImageUtils.isTooSimilar(grayProcessor, previousGrayProcessor, this.diffThreshold)) {
                        continue;
                    }
                }

                // Salva frame válido
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
            
            // Notifica conclusão via callback
            if (progressCallback != null) {
                progressCallback.onProgress(
                    totalFrames, 
                    totalFrames, 
                    "Extração concluída: " + savedCount + " frames salvos"
                );
            }
            
            // Fecha o grabber
            frameGrabber.stop();
            frameGrabber.release();

        } catch (UnsatisfiedLinkError e) {
            IJ.log("Erro de link nativo (FFmpeg não encontrado): " + e.getMessage());
            if (progressCallback != null) {
                progressCallback.onProgress(0, 100, "Erro: FFmpeg não encontrado");
            }
        } catch (NoClassDefFoundError e) {
            IJ.log("Classe faltando: " + e.getMessage());
            if (progressCallback != null) {
                progressCallback.onProgress(0, 100, "Erro: Classe faltando");
            }
        } catch (Throwable e) {
            IJ.handleException(e);
            if (progressCallback != null) {
                progressCallback.onProgress(0, 100, "Erro: " + e.getMessage());
            }
        }
        
        return savedCount;
    }
}