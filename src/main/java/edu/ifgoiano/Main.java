package edu.ifgoiano;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String videoPath = "C:/Users/randolfo/Documents/projetodev/source/videos/fazjatoba.mp4"; 
        Path outputDir = Paths.get("C:/Users/randolfo/Documents/projetodev/output");
        
        double blurThreshold = 10; 
        double diffThreshold = 30;  

        VideoFrameExtractor extractor = new VideoFrameExtractor(blurThreshold, diffThreshold);
        int savedFrames = extractor.extractFrames(videoPath, outputDir);

        System.out.println("Concluído! " + savedFrames + " frames válidos salvos em: " + outputDir);
    }
}