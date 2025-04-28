import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

public class PDFMergerApp extends JFrame {
    private JButton btnSelect, btnMerge;
    private JTextArea txtAreaFiles;
    private Map<String, File> selectedFiles;
    private JComboBox<String>[] fileTypeComboBoxes;
    private Set<String> usedFiles;
    private Map<String, File> fileMap;

    private static final String[] REQUIRED_TYPES = {
        "Tapa Pasta", "Cubierta", "Portada",
        "Cesión de Derecho de Publicación",
        "Antiplagio", "Documento Escrito"
    };

    public PDFMergerApp() {
        setTitle("PDF Merger para Trabajo de Titulación");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        btnSelect = new JButton("Seleccionar Archivos");
        btnMerge = new JButton("Unir PDFs");
        txtAreaFiles = new JTextArea(5, 40);
        txtAreaFiles.setEditable(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnSelect);
        buttonPanel.add(btnMerge);

        selectedFiles = new HashMap<String, File>();
        fileTypeComboBoxes = new JComboBox[REQUIRED_TYPES.length];
        usedFiles = new HashSet<String>();
        fileMap = new HashMap<String, File>();

        JPanel filePanel = new JPanel(new GridLayout(REQUIRED_TYPES.length, 2, 5, 5));
        for (int i = 0; i < REQUIRED_TYPES.length; i++) {
            filePanel.add(new JLabel(REQUIRED_TYPES[i]));
            fileTypeComboBoxes[i] = new JComboBox<String>();
            fileTypeComboBoxes[i].addItemListener(new ComboBoxListener(i));
            filePanel.add(fileTypeComboBoxes[i]);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(filePanel, BorderLayout.CENTER);
        mainPanel.add(new JScrollPane(txtAreaFiles), BorderLayout.SOUTH);

        add(buttonPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        btnSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectFiles();
            }
        });

        btnMerge.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mergeFiles();
            }
        });
    }

    private class ComboBoxListener implements ItemListener {
        private int index;

        public ComboBoxListener(int index) {
            this.index = index;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedFile = (String) e.getItem();
                if (!"-- Seleccione un archivo --".equals(selectedFile)) {
                    for (int i = 0; i < fileTypeComboBoxes.length; i++) {
                        if (i != index && selectedFile.equals(fileTypeComboBoxes[i].getSelectedItem())) {
                            fileTypeComboBoxes[i].setSelectedIndex(0);
                        }
                    }
                }
            }
        }
    }

    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF", "pdf"));
        fileChooser.setDialogTitle("Seleccione los archivos PDF");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            updateFileList(files);
            updateComboBoxes(files);
        }
    }

    private void updateFileList(File[] files) {
        txtAreaFiles.setText("");
        for (File file : files) {
            txtAreaFiles.append(file.getName() + "\n");
        }
    }

    private void updateComboBoxes(File[] files) {
        fileMap.clear();
        for (JComboBox<String> comboBox : fileTypeComboBoxes) {
            String currentSelection = (String) comboBox.getSelectedItem();
            comboBox.removeAllItems();
            comboBox.addItem("-- Seleccione un archivo --");
            for (File file : files) {
                String fileName = file.getName();
                comboBox.addItem(fileName);
                fileMap.put(fileName, file);
            }
            if (currentSelection != null) {
                boolean found = false;
                for (File file : files) {
                    if (file.getName().equals(currentSelection)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    comboBox.setSelectedItem(currentSelection);
                } else {
                    comboBox.setSelectedIndex(0);
                }
            }
        }
    }

    private void mergeFiles() {
        if (!validateSelection()) {
            JOptionPane.showMessageDialog(this, "Por favor, asigne un archivo a cada tipo de documento.");
            return;
        }

        try {
            String outputPath = chooseOutputFile();
            if (outputPath == null) return;

            List<File> sortedFiles = new ArrayList<File>();
            for (String type : REQUIRED_TYPES) {
                sortedFiles.add(selectedFiles.get(type));
            }

            mergePDFs(sortedFiles, outputPath);
            JOptionPane.showMessageDialog(this, "PDF creado: " + outputPath);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al unir PDFs: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private boolean validateSelection() {
        selectedFiles.clear();
        usedFiles.clear();
        for (int i = 0; i < REQUIRED_TYPES.length; i++) {
            String selectedFileName = (String) fileTypeComboBoxes[i].getSelectedItem();
            if (selectedFileName == null || "-- Seleccione un archivo --".equals(selectedFileName)) {
                return false;
            }
            if (usedFiles.contains(selectedFileName)) {
                JOptionPane.showMessageDialog(this, "El archivo " + selectedFileName + " está seleccionado más de una vez.");
                return false;
            }
            File selectedFile = fileMap.get(selectedFileName);
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, "Error: No se pudo encontrar el archivo " + selectedFileName);
                return false;
            }
            selectedFiles.put(REQUIRED_TYPES[i], selectedFile);
            usedFiles.add(selectedFileName);
        }
        return true;
    }

    private String chooseOutputFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar PDF unificado");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF", "pdf"));
        fileChooser.setSelectedFile(new File(generateSequentialName()));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
            }
            return fileToSave.getAbsolutePath();
        }
        return null;
    }

    private String generateSequentialName() {
        String baseName = "Trabajo_Titulacion_";
        int counter = 1;
        while (new File(baseName + counter + ".pdf").exists()) {
            counter++;
        }
        return baseName + counter + ".pdf";
    }

    private void mergePDFs(List<File> files, String outputPath) throws Exception {
        Document document = new Document();
        PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputPath));
        document.open();

        for (File file : files) {
            PdfReader reader = new PdfReader(file.getAbsolutePath());
            copy.addDocument(reader);
            copy.freeReader(reader);
            reader.close();
        }

        document.close();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PDFMergerApp().setVisible(true);
            }
        });
    }
}
