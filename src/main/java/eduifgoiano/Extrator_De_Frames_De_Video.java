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
        OpenDialog od = new OpenDialog("Selecione o arquivo de vídeo...");
        String diretorioPai = od.getDirectory();
        String nomeArquivo = od.getFileName();
        if (nomeArquivo == null) {
            IJ.log("Nenhum vídeo selecionado. Plugin cancelado.");
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
        
        // Converte a escolha do filtro para o número que sua classe espera (1 para Laplace, 2 para Sobel)
        int filtroEscolhido = filtroSelecionado.equals("Laplace") ? 1 : 2;

        // --- 5. Executar o processo em uma nova Thread pra nao travaar
        final String finalVideoPath = videoPath;
        final Path finalOutputDir = outputDir;

        Thread thread = new Thread(() -> {
            IJ.log("--- Iniciando a extração de frames ---");
            VideoFrameExtractor extractor = new VideoFrameExtractor(blurThreshold, diffThreshold, filtroEscolhido);
            int savedFrames = extractor.extractFrames(finalVideoPath, finalOutputDir);
            IJ.log("Processo Concluído!");
            IJ.log(savedFrames + " frames válidos salvos em: " + finalOutputDir);
        });
        
        thread.start();
    }
}