import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleTextEditor extends JFrame implements ActionListener {
    private Map<JTextArea, UndoManager> undoManagers;
    private JTextArea currentTextArea;
    private JTabbedPane tabbedPane;
    private JButton buttonUndo, buttonRedo, buttonDelete, buttonRename, buttonClose, tabClose, buttonSearchReplace, buttonNew, buttonOpen, buttonSave;

    public SimpleTextEditor() {
        setTitle("MAD Simple Text Editor - (by DAM Inc.)");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Open in full-screen mode
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        undoManagers = new HashMap<>();

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // initial tab
        addNewTab("Untitled", "");

        JPanel buttonPanel = new JPanel();
        add(buttonPanel, BorderLayout.NORTH);

        // Initialize all buttons and add them to the panel

buttonNew = new JButton("New");
buttonNew.addActionListener(this);
buttonPanel.add(buttonNew);

buttonOpen = new JButton("Open");
buttonOpen.addActionListener(this);
buttonPanel.add(buttonOpen);

buttonSave = new JButton("Save");
buttonSave.addActionListener(this);
buttonPanel.add(buttonSave);

buttonUndo = new JButton("Undo");
buttonUndo.addActionListener(this);
buttonPanel.add(buttonUndo);

buttonRedo = new JButton("Redo");
buttonRedo.addActionListener(this);
buttonPanel.add(buttonRedo);

buttonDelete = new JButton("Delete");
buttonDelete.addActionListener(this);
buttonPanel.add(buttonDelete);

buttonRename = new JButton("Rename");
buttonRename.addActionListener(this);
buttonPanel.add(buttonRename);

buttonClose = new JButton("Close");
buttonClose.addActionListener(this);
buttonPanel.add(buttonClose);

tabClose = new JButton("Close Tab");
tabClose.addActionListener(this);
buttonPanel.add(tabClose);

buttonSearchReplace = new JButton("Search & Replace");
buttonSearchReplace.addActionListener(this);
buttonPanel.add(buttonSearchReplace);

        // Add listener for tab selection changes
        tabbedPane.addChangeListener(e -> {
            currentTextArea = (JTextArea) ((JScrollPane) tabbedPane.getSelectedComponent()).getViewport().getView();
            updateCurrentUndoManager();
        });

        setVisible(true);

        /*
         * Purpose: To add a timer set to autosave all open current files in the tabs to
         * separate text files automatically titled "autosave[iteration].txt" for
         * clarity and ease of finding
         * It will save to a folder titled "autosaves" within this current working
         * directory
         */
        Timer autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    JTextArea tabTextArea = (JTextArea) ((JScrollPane) tabbedPane.getComponentAt(i)).getViewport()
                            .getView();
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    saveFile("autosaves" + File.separator + "autosave_" + timestamp + "_" + i + ".txt",
                            tabTextArea.getText());
                }
            }
        }, 60000, 60000); // Save every minute
    }

    private void updateCurrentUndoManager() {
        if (currentTextArea != null) {
            if (!undoManagers.containsKey(currentTextArea)) {
                UndoManager undoManager = new UndoManager();
                currentTextArea.getDocument().addUndoableEditListener(undoManager);
                undoManagers.put(currentTextArea, undoManager);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonUndo) {
            if (currentTextArea != null && undoManagers.containsKey(currentTextArea) && undoManagers.get(currentTextArea).canUndo()) {
                undoManagers.get(currentTextArea).undo();
            }
        } else if (e.getSource() == buttonRedo) {
            if (currentTextArea != null && undoManagers.containsKey(currentTextArea) && undoManagers.get(currentTextArea).canRedo()) {
                undoManagers.get(currentTextArea).redo();
            }
        } else if (e.getSource() == buttonDelete) {
            deleteFile();
        } else if (e.getSource() == buttonRename) {
            renameFile();
        } else if (e.getSource() == buttonClose) {
            dispose();
        } else if (e.getSource() == tabClose) {
            closeTab();
        } else if (e.getSource() == buttonSearchReplace) {
            searchAndReplace();
        } else if (e.getSource() == buttonOpen) {
            openFile();
        }
        else if (e.getSource() == buttonNew) {
            addNewTab("Untitled", "");
        }
        else if (e.getSource() == buttonSave) {
            saveFile(null, null); // Pass null to use default behavior
        }
    }

    public void addNewTab(String title, String initialText) {
        JTextArea newTextArea = new JTextArea(initialText);
        JScrollPane scrollPane = new JScrollPane(newTextArea);
        tabbedPane.addTab(title, scrollPane);
        currentTextArea = newTextArea;
        updateCurrentUndoManager();
    }

    public void closeTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex >= 0) {
            tabbedPane.remove(selectedIndex);
        }
    }

    public JTextArea getCurrentTextArea() {
        JScrollPane scrollPane = (JScrollPane) tabbedPane.getSelectedComponent();
        return (JTextArea) scrollPane.getViewport().getView();
    }

    public void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.getName().toLowerCase().endsWith(".txt")) {
                addNewTab(file.getName(), readFile(file));
            } else {
                JOptionPane.showMessageDialog(this, "Only .txt files can be opened", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public String readFile(File file) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "File could not be read!", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return content.toString();
    }

    public void saveFile(String filePath, String content) {
        if (filePath == null) {
            // If filePath is null, get the content from the current tab
            JTextArea currentTextArea = getCurrentTextArea();
            if (currentTextArea != null) {
                content = currentTextArea.getText();
            } else {
                // No tab is selected, cannot save
                JOptionPane.showMessageDialog(this, "No file selected to save", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Choose file path based on user input or default
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
            int option = fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                filePath = file.getAbsolutePath();
            } else {
                // User canceled the save operation
                return;
            }
        }

        // Append .txt extension if not already present
        if (!filePath.toLowerCase().endsWith(".txt")) {
            filePath += ".txt";
        }

        // Check if the directory exists, if not, create it
        File directory = new File("autosaves");
        if (!directory.exists()) {
            directory.mkdir();
        }

        // Save the file
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(content);
                JOptionPane.showMessageDialog(this, "File saved successfully");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to save file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to create directory", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showDialog(this, "Delete");
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.getName().toLowerCase().endsWith(".txt")) {
                if (file.delete()) {
                    JOptionPane.showMessageDialog(this, "File deleted successfully");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete file", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Only .txt files can be deleted", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void renameFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showDialog(this, "Rename");
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.getName().toLowerCase().endsWith(".txt")) {
                String newName = JOptionPane.showInputDialog(this, "New name:", file.getName());
                
                // Append .txt extension if not already present
        if (!newName.toLowerCase().endsWith(".txt")) {
            newName += ".txt";}

                if (newName != null && !newName.isEmpty()) {
                    File newFile = new File(file.getParent(), newName);
                    if (file.renameTo(newFile)) {
                        JOptionPane.showMessageDialog(this, "File renamed successfully");
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to rename file", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Only .txt files can be renamed", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void searchAndReplace() {
        JTextField searchField = new JTextField(20);
        JTextField replaceField = new JTextField(20);
        Object[] message = {
                "Search for:", searchField,
                "Replace with:", replaceField
        };
        int option = JOptionPane.showConfirmDialog(null, message, "Search & Replace", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String searchText = searchField.getText();
            String replaceText = replaceField.getText();
            if (!searchText.isEmpty()) {
                Matcher matcher = Pattern.compile(Pattern.quote(searchText)).matcher(currentTextArea.getText());
                while (matcher.find()) {
                    currentTextArea.replaceRange(replaceText, matcher.start(), matcher.end());
                    matcher = Pattern.compile(Pattern.quote(searchText)).matcher(currentTextArea.getText());
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleTextEditor::new);
    }
}
