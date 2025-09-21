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
        // --- 1. Obter o arquivo de vídeo de entrada ---
        OpenDialog od = new OpenDialog("Selecione o arquivo de vídeo...");
        String diretorioPai = od.getDirectory();
        String nomeArquivo = od.getFileName();
        if (nomeArquivo == null) {
            IJ.log("Nenhum vídeo selecionado. Plugin cancelado.");
            return; // Usuário cancelou
        }
        String videoPath = diretorioPai + nomeArquivo;

        // --- 2. Obter o diretório de saída ---
        DirectoryChooser dc = new DirectoryChooser("Selecione o diretório para salvar os frames...");
        String outputDirString = dc.getDirectory();
        if (outputDirString == null) {
            IJ.log("Nenhum diretório de saída selecionado. Plugin cancelado.");
            return; // Usuário cancelou
        }
        Path outputDir = Paths.get(outputDirString);

        // --- 3. Criar a caixa de diálogo para os parâmetros ---
        GenericDialog gd = new GenericDialog("Configurações do Extrator de Frames");
        gd.addNumericField("Limiar de Desfoque (Variância):", 10, 2); // (rótulo, valor padrão, casas decimais)
        gd.addNumericField("Limiar de Similaridade (Diferença Média):", 30, 2);
        
        String[] filtros = {"Laplace", "Sobel"};
        gd.addChoice("Filtro de Borda:", filtros, filtros[0]); // (rótulo, opções, padrão)

        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.log("Plugin cancelado pelo usuário.");
            return;
        }

        // --- 4. Coletar os valores da caixa de diálogo ---
        double blurThreshold = gd.getNextNumber();
        double diffThreshold = gd.getNextNumber();
        String filtroSelecionado = gd.getNextChoice();
        
        // Converte a escolha do filtro para o número que sua classe espera (1 para Laplace, 2 para Sobel)
        int filtroEscolhido = filtroSelecionado.equals("Laplace") ? 1 : 2;

        // --- 5. Executar o processo em uma nova Thread ---
        // Isso é MUITO IMPORTANTE para não travar a interface do ImageJ!
        final String finalVideoPath = videoPath;
        final Path finalOutputDir = outputDir;

        Thread thread = new Thread(() -> {
            IJ.log("--- Iniciando a extração de frames ---");
            VideoFrameExtractor extractor = new VideoFrameExtractor(blurThreshold, diffThreshold, filtroEscolhido);
            int savedFrames = extractor.extractFrames(finalVideoPath, finalOutputDir);
            IJ.log("--- Processo Concluído! ---");
            IJ.log(savedFrames + " frames válidos salvos em: " + finalOutputDir);
        });
        
        thread.start();
    }
}