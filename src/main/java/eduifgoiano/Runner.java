package eduifgoiano;

import ij.IJ;
import ij.ImageJ;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Runner extends JFrame {

    private JTextField txtVideoPath;
    private JTextField txtOutputDir;
    private JComboBox<String> cbFilter;
    private JCheckBox chkSaveFiltered;
    private JButton btnRun;
    private JProgressBar progressBar;
    private JLabel lblStatus;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (args.length >= 2) {
            runBatchMode(args);
        } else {
            SwingUtilities.invokeLater(() -> new Runner().setVisible(true));
        }
    }

    public Runner() {
        setTitle("Image Analyzer - IF Goiano");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        mainPanel.add(new JLabel("Arquivo de Vídeo:"), gbc);

        txtVideoPath = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        mainPanel.add(txtVideoPath, gbc);

        JButton btnBrowseVideo = new JButton("...");
        gbc.gridx = 2; gbc.weightx = 0;
        mainPanel.add(btnBrowseVideo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        mainPanel.add(new JLabel("Pasta de Saída:"), gbc);

        txtOutputDir = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        mainPanel.add(txtOutputDir, gbc);

        JButton btnBrowseOutput = new JButton("...");
        gbc.gridx = 2; gbc.weightx = 0;
        mainPanel.add(btnBrowseOutput, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        mainPanel.add(new JLabel("Filtro de Borda:"), gbc);

        cbFilter = new JComboBox<>(new String[]{"Laplace", "Sobel"});
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
        mainPanel.add(cbFilter, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2;
        chkSaveFiltered = new JCheckBox("Salvar imagens do filtro (Sobel/Laplace)?");
        chkSaveFiltered.setSelected(false);
        mainPanel.add(chkSaveFiltered, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        gbc.insets = new Insets(20, 5, 5, 5);
        btnRun = new JButton("Iniciar Processamento");
        btnRun.setPreferredSize(new Dimension(200, 40));
        mainPanel.add(btnRun, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(10, 5, 0, 5);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        mainPanel.add(progressBar, gbc);

        gbc.gridy = 6;
        lblStatus = new JLabel("Aguardando início...");
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblStatus, gbc);

        add(mainPanel, BorderLayout.CENTER);

        btnBrowseVideo.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (!txtVideoPath.getText().isEmpty()) {
                fc.setCurrentDirectory(new File(txtVideoPath.getText()).getParentFile());
            }
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtVideoPath.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        btnBrowseOutput.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (!txtOutputDir.getText().isEmpty()) {
                fc.setCurrentDirectory(new File(txtOutputDir.getText()));
            }
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtOutputDir.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        btnRun.addActionListener(e -> startProcessing());
    }

    private void startProcessing() {
        String videoPath = txtVideoPath.getText();
        String outputDir = txtOutputDir.getText();

        if (videoPath.isEmpty() || outputDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione o vídeo e a pasta de saída.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setComponentsEnabled(false);
        progressBar.setValue(0);
        
        new Thread(() -> {
            try {
                new ImageJ(ImageJ.NO_SHOW);

                Path basePath = Paths.get(outputDir);
                Path framesDir = basePath.resolve("Frames_Extraidos");
                Path resultsDir = basePath.resolve("Resultados_Analise");

                double blurThreshold = 10;
                double diffThreshold = 30;
                
                int filtroIndex = cbFilter.getSelectedIndex(); 
                int filtroEscolhido = filtroIndex + 1; 
                boolean saveFiltered = chkSaveFiltered.isSelected();

                updateStatus("Extraindo frames...");
                
                VideoFrameExtractor extractor = new VideoFrameExtractor(
                        blurThreshold,
                        diffThreshold,
                        filtroEscolhido,
                        saveFiltered
                );

                extractor.setProgressCallback((current, total, message) -> {
                    SwingUtilities.invokeLater(() -> {
                        int percent = (int) ((current / (double) total) * 100);
                        progressBar.setValue(percent);
                        progressBar.setString(current + " / " + total);
                        lblStatus.setText(message);
                    });
                });

                int savedFrames = extractor.extractFrames(videoPath, framesDir);
                
                if (savedFrames > 0) {
                    updateStatus("Executando Macro...");
                    progressBar.setIndeterminate(true);
                    
                    File resDirFile = resultsDir.toFile();
                    if (!resDirFile.exists()) resDirFile.mkdirs();

                    String inputMacroPath = framesDir.toAbsolutePath().toString().replace("\\", "/");
                    String outputMacroPath = resultsDir.toAbsolutePath().toString().replace("\\", "/");
                    
                    if (!inputMacroPath.endsWith("/")) inputMacroPath += "/";
                    if (!outputMacroPath.endsWith("/")) outputMacroPath += "/";

                    String macroScript = buildMacroString(inputMacroPath, outputMacroPath);
                    
                    IJ.runMacro(macroScript);
                    
                    updateStatus("Concluído!");
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    
                    int choice = JOptionPane.showOptionDialog(this,
                        "Processamento finalizado!\nSalvo em: " + resultsDir.toString() + "\nO que deseja fazer?",
                        "Sucesso",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[]{"Processar Outro", "Sair"},
                        "Processar Outro");
                        
                    if (choice == 1) {
                        System.exit(0);
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            setComponentsEnabled(true);
                            progressBar.setValue(0);
                            progressBar.setString("0%");
                            lblStatus.setText("Aguardando início...");
                            IJ.log("\\Clear");
                        });
                    }
                } else {
                    updateStatus("Erro: Nenhum frame extraído.");
                    JOptionPane.showMessageDialog(this, "Nenhum frame foi extraído.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    SwingUtilities.invokeLater(() -> setComponentsEnabled(true));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                updateStatus("Erro fatal: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> setComponentsEnabled(true));
            }
        }).start();
    }

    private void updateStatus(String msg) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(msg));
    }

    private void setComponentsEnabled(boolean enabled) {
        txtVideoPath.setEnabled(enabled);
        txtOutputDir.setEnabled(enabled);
        cbFilter.setEnabled(enabled);
        chkSaveFiltered.setEnabled(enabled);
        btnRun.setEnabled(enabled);
    }

    private static void runBatchMode(String[] args) {
        new ImageJ(ImageJ.NO_SHOW);
        String videoFile = args[0];
        String outputBaseDir = args[1];
        
        try {
            Path basePath = Paths.get(outputBaseDir);
            Path framesDir = basePath.resolve("Frames_Extraidos");
            Path resultsDir = basePath.resolve("Resultados_Analise");

            VideoFrameExtractor extractor = new VideoFrameExtractor(10, 30, 1, false);
            int savedFrames = extractor.extractFrames(videoFile, framesDir);

            if (savedFrames > 0) {
                File resDirFile = resultsDir.toFile();
                if (!resDirFile.exists()) resDirFile.mkdirs();

                String inputMacroPath = framesDir.toAbsolutePath().toString().replace("\\", "/");
                String outputMacroPath = resultsDir.toAbsolutePath().toString().replace("\\", "/");
                
                if (!inputMacroPath.endsWith("/")) inputMacroPath += "/";
                if (!outputMacroPath.endsWith("/")) outputMacroPath += "/";

                String macroScript = buildMacroString(inputMacroPath, outputMacroPath);
                IJ.runMacro(macroScript);
            }
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
}