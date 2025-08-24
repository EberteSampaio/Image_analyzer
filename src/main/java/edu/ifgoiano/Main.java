package edu.ifgoiano;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String videoPath = "C:/Users/randolfo/Documents/projetodev/source/videos/fazjatoba.mp4"; 
        Path outputDir = Paths.get("C:/Users/randolfo/Documents/projetodev/output");
        
        double blurThreshold = 10; 
        double diffThreshold = 30;  
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Escolha o filtro: ");
        System.out.println("1 - Laplace");
        System.out.println("2 - Sobel");
        int filtroEscolhido = scanner.nextInt();
        scanner.close();
        
        VideoFrameExtractor extractor = new VideoFrameExtractor(blurThreshold, diffThreshold, filtroEscolhido);
        int savedFrames = extractor.extractFrames(videoPath, outputDir);

        System.out.println("Concluído! " + savedFrames + " frames válidos salvos em: " + outputDir);
    }
}