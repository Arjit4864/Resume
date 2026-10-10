import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

class Book implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String title;
    private String author;
    private boolean isIssued;

    public Book(String id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isIssued = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public boolean isIssued() { return isIssued; }
    public void setIssued(boolean issued) { isIssued = issued; }
    
    @Override
    public String toString() {
        return isIssued ? "Issued" : "Available";
    }
}

public class LibrarySystem extends JFrame {

    private List<Book> bookList;
    private static final String FILE_NAME = "library_data.ser";
    
    private DefaultTableModel tableModel;
    private JTable bookTable;
    private JTextField titleField, authorField, idField;

    public LibrarySystem() {
        
        loadData();

        setTitle("Library Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }

        initComponents();
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel viewPanel = new JPanel(new BorderLayout());
        
        String[] columns = {"ID", "Title", "Author", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        bookTable = new JTable(tableModel);
        refreshTable();

        JPanel actionPanel = new JPanel();
        JButton issueBtn = new JButton("Issue/Return Book");
        JButton deleteBtn = new JButton("Delete Book");

        styleButton(issueBtn, new Color(70, 130, 180));
        styleButton(deleteBtn, new Color(220, 53, 69));

        actionPanel.add(issueBtn);
        actionPanel.add(deleteBtn);

        viewPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        viewPanel.add(actionPanel, BorderLayout.SOUTH);

        JPanel addPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        addPanel.add(new JLabel("Book ID:"), gbc);
        gbc.gridx = 1;
        idField = new JTextField(20);
        addPanel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        addPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField(20);
        addPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        addPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        addPanel.add(authorField, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        JButton addBtn = new JButton("Add Book to Library");
        styleButton(addBtn, new Color(40, 167, 69));
        addPanel.add(addBtn, gbc);

        tabbedPane.addTab("View Library", viewPanel);
        tabbedPane.addTab("Add New Book", addPanel);
        
        add(tabbedPane);

        addBtn.addActionListener(e -> {
            String id = idField.getText();
            String title = titleField.getText();
            String author = authorField.getText();

            if (id.isEmpty() || title.isEmpty() || author.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
                return;
            }

            Book newBook = new Book(id, title, author);
            bookList.add(newBook);
            saveData();
            refreshTable();
            
            idField.setText("");
            titleField.setText("");
            authorField.setText("");
            JOptionPane.showMessageDialog(this, "Book Added Successfully!");
            tabbedPane.setSelectedIndex(0); 
        });

        deleteBtn.addActionListener(e -> {
            int selectedRow = bookTable.getSelectedRow();
            if (selectedRow >= 0) {
                bookList.remove(selectedRow);
                saveData();
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Select a book to delete.");
            }
        });

        issueBtn.addActionListener(e -> {
            int selectedRow = bookTable.getSelectedRow();
            if (selectedRow >= 0) {
                Book book = bookList.get(selectedRow);
                book.setIssued(!book.isIssued());
                saveData();
                refreshTable();
            } else {
                JOptionPane.showMessageDialog(this, "Select a book to update status.");
            }
        });
    }


    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Book b : bookList) {
            tableModel.addRow(new Object[]{
                b.getId(), 
                b.getTitle(), 
                b.getAuthor(), 
                b.toString()
            });
        }
    }

    
    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                bookList = (List<Book>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                bookList = new ArrayList<>();
                e.printStackTrace();
            }
        } else {
            bookList = new ArrayList<>();
        }
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(bookList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            new LibrarySystem().setVisible(true);
        });
    }
}