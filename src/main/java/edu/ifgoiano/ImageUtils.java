package edu.ifgoiano;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.plugin.ImageCalculator;

public class ImageUtils {

    /**
     * Converte um ImageProcessor para escala de cinza.
     *
     * @param processor O ImageProcessor original.
     * @return Um novo ImageProcessor em escala de cinza, ou o original se já for 8bit.
     */
	//usa os metodos nativos do IMAGEJ
    public static ImageProcessor convertToGrayscale(ImageProcessor processor) {
        if (processor == null) return null;
        // convertToByteProcessor - true garante que a imagem seja convertida para 8-bit grayscale
        // 'true' habilita o scaling se necessário
        return processor.convertToByteProcessor(true);
    }

    /**
     * Verifica se uma imagem está desfocada com base na variância do Laplaciano.
     * Este método usa Sobel (por enquanto).
     *
     * @param processor O ImageProcessor.
     * @param threshold O limiar de variância. Abaixo disso, é considerado desfocado.
     * @return true se a imagem for considerada desfocada, false caso contrário.
     */
    public static boolean isBlurry(ImageProcessor processor, double threshold) {
        if (processor == null) return true;

        // Duplicar para não alterar o processador original
        ImageProcessor laplacianProcessor = processor.duplicate();
        laplacianProcessor.findEdges();

        // Calcular a variância da imagem resultante
        // Uma imagem nítida terá bordas mais proeminentes, resultando em maior variância
        ImageStatistics stats = laplacianProcessor.getStatistics();
        double variance = stats.stdDev * stats.stdDev; // Variância = desvio padrão ao quadrado
        return variance < threshold;
    }

    /**
     * Compara dois ImageProcessors para verificar se são muito similares.
     * A comparação é baseada na média da diferença absoluta dos pixels.
     *
     * @param current O ImageProcessor atual (espera em escala de cinza).
     * @param previous O ImageProcessor anterior (espera em escala de cinza).
     * @param threshold O limiar de diferença. Abaixo disso, são considerados muito similares.
     * @return true se as imagens forem consideradas muito similares, false caso contrário.
     */
    public static boolean isTooSimilar(ImageProcessor current, ImageProcessor previous, double threshold) {
        if (current == null || previous == null) {
            return false;
        }
        if (current.getWidth() != previous.getWidth() || current.getHeight() != previous.getHeight()) {
            // Se os tamanhos forem diferentes, considera-se que não são similares (ou um erro)
            System.err.println("Tentativa de comparar imagens de tamanhos diferentes.");
            return false;
        }

        // Criar ImagePlus temporários para usar ImageCalculator
        ImagePlus currentImp = new ImagePlus("Current", current);
        ImagePlus previousImp = new ImagePlus("Previous", previous);

        ImageCalculator ic = new ImageCalculator();
        // "Difference" calcula a diferença absoluta: |currentImp - previousImp|
        ImagePlus diffImp = ic.run("Difference create", currentImp, previousImp);

        if (diffImp == null) {
            System.err.println("Falha ao calcular a diferença entre as imagens para verificação de similaridade.");
            return false; // Considerar como não similar para evitar erro
        }

        // Calcular a média dos valores de pixel da imagem de diferença
        ImageStatistics stats = diffImp.getProcessor().getStatistics();
        return stats.mean < threshold;
    }
}