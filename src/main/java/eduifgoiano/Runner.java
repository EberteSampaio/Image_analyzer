package eduifgoiano;

import ij.IJ;
import ij.ImageJ;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Runner {
    
    private static ImageJ imageJInstance = null;

    public static void main(String[] args) {
        // Configura o Look and Feel do sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Verifica se está em modo batch (linha de comando)
        if (args.length >= 2) {
            runBatchMode(args);
        } else {
            // Mostra a tela inicial
            SwingUtilities.invokeLater(() -> showMainWindow());
        }
    }

    private static void initializeImageJ() {
        if (imageJInstance == null) {
            imageJInstance = new ImageJ(ImageJ.NO_SHOW);
            imageJInstance.exitWhenQuitting(false);
        }
    }

    private static void showMainWindow() {
        JFrame frame = new JFrame("Analisador de Vídeos - IF Goiano");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 350);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // Painel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 240, 245));

        // Painel superior com logo/título
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(240, 240, 245));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Analisador de Vídeos");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Extração e Análise de Frames");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(20));

        // Painel central com botões
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(240, 240, 245));

        // Botão Processar Vídeo
        JButton processButton = createStyledButton("Processar Vídeo", new Color(70, 130, 180));
        processButton.addActionListener(e -> {
            frame.setVisible(false);
            processVideo(frame);
        });

        // Botão Sobre
        JButton aboutButton = createStyledButton("Sobre", new Color(100, 149, 237));
        aboutButton.addActionListener(e -> showAboutDialog(frame));

        // Botão Sair
        JButton exitButton = createStyledButton("Sair", new Color(169, 169, 169));
        exitButton.addActionListener(e -> System.exit(0));

        centerPanel.add(processButton);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(aboutButton);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(exitButton);

        // Rodapé
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(new Color(240, 240, 245));
        JLabel footerLabel = new JLabel("IF Goiano - 2025");
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        footerLabel.setForeground(Color.GRAY);
        footerPanel.add(footerLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(300, 45));
        button.setMaximumSize(new Dimension(300, 45));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Efeito hover
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }

    private static void showAboutDialog(JFrame parent) {
        String message = "Analisador de Vídeos\n\n" +
                        "Versão 1.0\n\n" +
                        "Sistema para extração e análise\n" +
                        "automática de frames de vídeos.\n\n" +
                        "IF Goiano - 2025";
        
        JOptionPane.showMessageDialog(parent, message, "Sobre", 
                                     JOptionPane.INFORMATION_MESSAGE);
    }

    private static void processVideo(JFrame mainFrame) {
        OpenDialog od = new OpenDialog("Selecione o Vídeo para processar...");
        String dir = od.getDirectory();
        String name = od.getFileName();
        
        if (dir == null || name == null) {
            IJ.log("Seleção cancelada.");
            mainFrame.setVisible(true);
            return;
        }
        String videoFile = dir + name;

        DirectoryChooser dc = new DirectoryChooser("Selecione onde salvar os resultados...");
        String outputBaseDir = dc.getDirectory();
        
        if (outputBaseDir == null) {
            IJ.log("Seleção de pasta cancelada.");
            mainFrame.setVisible(true);
            return;
        }

        // Cria janela de progresso detalhada
        ProgressWindow progressWindow = new ProgressWindow();
        
        // Executa processamento em thread separada
        new Thread(() -> {
            try {
                // Inicializa ImageJ em background (sem mostrar)
                initializeImageJ();
                
                executeProcessing(videoFile, outputBaseDir, progressWindow);
                
                SwingUtilities.invokeLater(() -> {
                    progressWindow.dispose();
                    showCompletionDialog(mainFrame);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    progressWindow.dispose();
                    JOptionPane.showMessageDialog(mainFrame, 
                        "Erro no processamento:\n" + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
                    mainFrame.setVisible(true);
                });
            }
        }).start();
    }

    private static void showCompletionDialog(JFrame mainFrame) {
        Object[] options = {"Processar Outro Vídeo", "Fechar Programa"};
        
        int choice = JOptionPane.showOptionDialog(null,
            "A análise foi concluída com sucesso!\nO que deseja fazer agora?",
            "Processo Finalizado",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

        if (choice == 0) {
            // Processar outro vídeo
            IJ.log("\\Clear");
            processVideo(mainFrame);
        } else {
            // Fechar programa
            System.exit(0);
        }
    }

    private static void executeProcessing(String videoFile, String outputBaseDir, 
                                          ProgressWindow progressWindow) throws Exception {
        System.out.println("------------------------------------------------");
        System.out.println("Iniciando novo processamento...");
        System.out.println("Vídeo: " + videoFile);
        System.out.println("Pasta Base: " + outputBaseDir);

        Path basePath = Paths.get(outputBaseDir);
        Path framesDir = basePath.resolve("Frames_Extraidos");
        Path resultsDir = basePath.resolve("Resultados_Analise");

        double blurThreshold = 10;
        double diffThreshold = 30;
        int filtroEscolhido = 1;

        VideoFrameExtractor extractor = new VideoFrameExtractor(
                blurThreshold, diffThreshold, filtroEscolhido);

        // Define o callback de progresso
        extractor.setProgressCallback((current, total, message) -> {
            SwingUtilities.invokeLater(() -> {
                progressWindow.updateProgress(current, total, message);
            });
        });

        System.out.println("Extraindo frames...");
        progressWindow.setStage("Extraindo Frames");
        
        int savedFrames = extractor.extractFrames(videoFile, framesDir);
        System.out.println(savedFrames + " frames válidos extraídos.");

        if (savedFrames > 0) {
            System.out.println("Executando análise (Macro)...");
            progressWindow.setStage("Analisando Partículas");
            
            File resDirFile = resultsDir.toFile();
            if (!resDirFile.exists()) {
                resDirFile.mkdirs();
            }

            String inputMacroPath = framesDir.toAbsolutePath().toString().replace("\\", "/");
            String outputMacroPath = resultsDir.toAbsolutePath().toString().replace("\\", "/");
            
            if (!inputMacroPath.endsWith("/")) inputMacroPath += "/";
            if (!outputMacroPath.endsWith("/")) outputMacroPath += "/";

            String macroScript = buildMacroString(inputMacroPath, outputMacroPath);
            IJ.runMacro(macroScript);
            
            System.out.println("Processo completo!");
        } else {
            IJ.log("Nenhum frame salvo. Pulando etapa do Macro.");
        }
    }

    private static void runBatchMode(String[] args) {
        initializeImageJ();
        String videoFile = args[0];
        String outputBaseDir = args[1];
        
        try {
            executeProcessing(videoFile, outputBaseDir, new ProgressWindow());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String buildMacroString(String inputPath, String outputPath) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("print('\\\\Clear');\n");
        sb.append("run('Clear Results');\n");
        sb.append("dir_input = '" + inputPath + "';\n");
        sb.append("dir_output = '" + outputPath + "';\n");
        sb.append("list = getFileList(dir_input);\n");
        sb.append("setBatchMode(true);\n");
        sb.append("for (i = 0; i < list.length; i++) {\n"); 
        sb.append("    if (endsWith(list[i], '.png')) {\n");
        sb.append("        open(dir_input + list[i]);\n");
        sb.append("        image_title = getTitle();\n");
        sb.append("        n_before = nResults;\n");
        sb.append("        run('8-bit');\n");
        sb.append("        setAutoThreshold('Default');\n");
        sb.append("        run('Convert to Mask');\n");
        sb.append("        run('Fill Holes');\n");
        sb.append("        run('Watershed');\n");
        sb.append("        run('Set Measurements...', 'area mean min perimeter feret\\'s redirect=None decimal=3');\n");
        sb.append("        run('Analyze Particles...', 'size=30-Infinity circularity=0.00-1.00 show=Nothing display add');\n");
        sb.append("        n_after = nResults;\n");
        sb.append("        if (n_after > n_before) {\n");
        sb.append("            for (j = n_before; j < n_after; j++) {\n");
        sb.append("                setResult('Image', j, image_title);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        updateResults();\n");
        sb.append("        close();\n");
        sb.append("    }\n");
        sb.append("}\n");
        sb.append("output_filename = 'Resultados_Particulas.csv';\n");
        sb.append("saveAs('Results', dir_output + output_filename);\n");
        sb.append("setBatchMode('exit and display');\n");
        sb.append("showMessage('Macro Concluido!', 'CSV salvo em: ' + dir_output);\n");
        
        return sb.toString();
    }

    // Classe interna para a janela de progresso
    static class ProgressWindow extends JFrame {
        private JProgressBar progressBar;
        private JLabel stageLabel;
        private JLabel detailLabel;
        private JLabel percentLabel;

        public ProgressWindow() {
            setTitle("Processando...");
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setSize(500, 200);
            setLocationRelativeTo(null);
            setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
            panel.setBackground(Color.WHITE);

            stageLabel = new JLabel("Iniciando...");
            stageLabel.setFont(new Font("Arial", Font.BOLD, 16));
            stageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            detailLabel = new JLabel("Preparando processamento");
            detailLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            detailLabel.setForeground(Color.GRAY);
            detailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            progressBar = new JProgressBar(0, 100);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            progressBar.setPreferredSize(new Dimension(440, 30));
            progressBar.setMaximumSize(new Dimension(440, 30));

            percentLabel = new JLabel("0%");
            percentLabel.setFont(new Font("Arial", Font.BOLD, 14));
            percentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(stageLabel);
            panel.add(Box.createVerticalStrut(10));
            panel.add(detailLabel);
            panel.add(Box.createVerticalStrut(15));
            panel.add(progressBar);
            panel.add(Box.createVerticalStrut(10));
            panel.add(percentLabel);

            add(panel);
            setVisible(true);
        }

        public void setStage(String stage) {
            SwingUtilities.invokeLater(() -> stageLabel.setText(stage));
        }

        public void updateProgress(int current, int total, String message) {
            SwingUtilities.invokeLater(() -> {
                int percent = (int) ((current / (double) total) * 100);
                progressBar.setValue(percent);
                progressBar.setString(current + " / " + total);
                percentLabel.setText(percent + "%");
                detailLabel.setText(message);
            });
        }
    }
}