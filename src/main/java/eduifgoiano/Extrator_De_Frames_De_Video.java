package eduifgoiano;

import ij.IJ;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.DirectoryChooser;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Extrator_De_Frames_De_Video implements PlugIn {
    
    @Override
    public void run(String arg) {
        // --- 1. Selecionar arquivo de vídeo ---
        OpenDialog od = new OpenDialog("Selecione o arquivo de vídeo...");
        String diretorioPai = od.getDirectory();
        String nomeArquivo = od.getFileName();
        
        if (nomeArquivo == null) {
            IJ.log("Seleção de vídeo cancelada.");
            return;
        }
        
        String videoPath = diretorioPai + nomeArquivo;
        
        // --- 2. Obter o diretório de saída ---
        DirectoryChooser dc = new DirectoryChooser("Selecione o diretório para salvar os frames...");
        String outputDirString = dc.getDirectory();
        
        if (outputDirString == null) {
            IJ.log("Nenhum diretório de saída selecionado. Plugin cancelado.");
            return;
        }
        
        Path outputDir = Paths.get(outputDirString);
        
        // --- 3. Configurações do usuário ---
        GenericDialog gd = new GenericDialog("Configurações do Extrator de Frames");
        gd.addNumericField("Limiar de Desfoque (Variância):", 10, 2);
        gd.addNumericField("Limiar de Similaridade (Diferença Média):", 30, 2);
        
        String[] filtros = {"Laplace", "Sobel"};
        gd.addChoice("Filtro de Borda:", filtros, filtros[0]);
        gd.showDialog();
        
        if (gd.wasCanceled()) {
            IJ.log("Plugin cancelado pelo usuário.");
            return;
        }
        
        double blurThreshold = gd.getNextNumber();
        double diffThreshold = gd.getNextNumber();
        String filtroSelecionado = gd.getNextChoice();
        
        // Converte a escolha do filtro (1 para Laplace, 2 para Sobel)
        int filtroEscolhido = filtroSelecionado.equals("Laplace") ? 1 : 2;
        
        // --- 4. Executar o processo em uma nova Thread ---
        final String finalVideoPath = videoPath;
        final Path finalOutputDir = outputDir;
        
        Thread thread = new Thread(() -> {
            IJ.log("Iniciando extração de frames...");
            IJ.log("Vídeo: " + finalVideoPath);
            IJ.log("Diretório de saída: " + finalOutputDir);
            
            VideoFrameExtractor extractor = new VideoFrameExtractor(
                blurThreshold, 
                diffThreshold, 
                filtroEscolhido,
                false
            );
            
            // Define callback de progresso (opcional para o plugin)
            // Como o plugin usa IJ.log, você pode adicionar um callback que mostre progresso
            extractor.setProgressCallback((current, total, message) -> {
                // Atualiza a barra de status do ImageJ
                IJ.showProgress(current, total);
                // Opcional: log apenas a cada 50 frames para não poluir
                if (current % 50 == 0 || current == total) {
                    IJ.log(message);
                }
            });
            
            IJ.log("Extraindo frames...");
            int savedFrames = extractor.extractFrames(finalVideoPath, finalOutputDir);
            
            IJ.showProgress(1.0); // Completa a barra de progresso
            IJ.log("========================================");
            IJ.log("Processo Concluído!");
            IJ.log(savedFrames + " frames válidos salvos em: " + finalOutputDir);
            IJ.log("========================================");
        });
        
        thread.start();
    }
}