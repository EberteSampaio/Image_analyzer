package edu.ifgoiano;

import java.io.IOException;

public class Main {
     private static final double DEFAULT_BLUR_THRESHOLD = 10.0;
    private static final double DEFAULT_DIFF_THRESHOLD = 30.0;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        String videoPath = "C:\\Users\\ebert\\OneDrive\\Imagens\\projeto-deev-laura\\video-teste.mp4";
        Path outputDir = Paths.get("C:\\Users\\ebert\\OneDrive\\Imagens\\projeto-deev-laura\\output");

        double blurThreshold = DEFAULT_BLUR_THRESHOLD;
        double diffThreshold = DEFAULT_DIFF_THRESHOLD;
        try {
            ensureDirectory(outputDir);
            int saved = extractFrames(videoPath, outputDir, blurThreshold, diffThreshold);
            System.out.println("Concluído! " + saved + " frames válidos foram salvos em: " + outputDir);
        } catch (IOException e) {
            System.err.println("Falha ao criar ou acessar a pasta de saída: " + outputDir);
            e.printStackTrace();
        }

    }

    private static int extractFrames(String videoPath, Path outputDir, double blurThreshold, double diffThreshold) {
        VideoCapture capture = new VideoCapture(videoPath);
        if (!capture.isOpened()) {
            System.err.println("Não foi possível abrir o vídeo: " + videoPath);
            return 0;
        }

        Mat frame = new Mat();
        Mat gray = new Mat();
        Mat laplacian = new Mat();
        Mat lastSaved = null;
        int savedCount = 0;

        try {
            while (capture.read(frame)) {
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

                if (isBlurry(gray, laplacian, blurThreshold)) {
                    continue;
                }

                if (lastSaved != null && isTooSimilar(gray, lastSaved, diffThreshold)) {
                    continue;
                }

                Path outputFile = outputDir.resolve(String.format("frame_%05d.png", savedCount));
                Imgcodecs.imwrite(outputFile.toString(), gray);
                System.out.println("Frame salvo: " + outputFile);
                savedCount++;

                if (lastSaved != null) {
                    lastSaved.release();
                }
                lastSaved = gray.clone();
            }
        } finally {
            frame.release();
            gray.release();
            laplacian.release();
            if (lastSaved != null) {
                lastSaved.release();
            }
            capture.release();
        }

        return savedCount;
    }
    private static boolean isBlurry(Mat gray, Mat laplacian, double threshold) {
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
        MatOfDouble mu = new MatOfDouble();
        MatOfDouble sigma = new MatOfDouble();
        Core.meanStdDev(laplacian, mu, sigma);
        double variance = Math.pow(sigma.get(0, 0)[0], 2);
        mu.release();
        sigma.release();
        return variance < threshold;
    }

    private static boolean isTooSimilar(Mat current, Mat last, double threshold) {
        Mat diff = new Mat();
        Core.absdiff(current, last, diff);
        Scalar sum = Core.sumElems(diff);
        double avgDiff = sum.val[0] / (current.rows() * current.cols());
        diff.release();
        return avgDiff < threshold;
    }

    private static void ensureDirectory(Path dir) throws IOException {
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
            System.out.println("Diretório criado: " + dir);
        }
    }

    private static double parseDoubleOrDefault(String text, double defaultValue) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            System.err.println("Valor inválido '" + text + "', usando padrão " + defaultValue);
            return defaultValue;
        }
    }
}
