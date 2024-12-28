import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class yönetici extends JFrame {
    private final String url = "jdbc:mysql://localhost:3306/deneme?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private final String dbUsername = "root";
    private final String dbPassword = "";
    private String adminName = "";
    private String adminEmail = "";

    public yönetici(String email) {
        // Yönetici bilgilerini veritabanından al
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            String query = "SELECT ad, soyad FROM kullanicilar WHERE email = ? AND type = '1'";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    adminName = rs.getString("ad") + " " + rs.getString("soyad");
                    adminEmail = email;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı bağlantı hatası!");
        }

        setTitle("Yönetici Paneli - " + adminName);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane mainTabbedPane = new JTabbedPane();

        // Üst panel - Yönetici adı gösterimi
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 20));
        
        JLabel adminLabel = new JLabel("Yönetici: " + adminName);
        adminLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(adminLabel);
        add(topPanel, BorderLayout.NORTH);

        // Profil Düzenleme Tab
        JPanel profilePanel = new JPanel();
        profilePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(20);
        JTextField surnameField = new JTextField(20);
        JPasswordField currentPasswordField = new JPasswordField(20);
        JPasswordField newPasswordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);

        // Mevcut bilgileri getir
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            String query = "SELECT ad, soyad FROM kullanicilar WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, adminEmail);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    nameField.setText(rs.getString("ad"));
                    surnameField.setText(rs.getString("soyad"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı bağlantı hatası!");
        }

        gbc.gridx = 0; gbc.gridy = 0;
        profilePanel.add(new JLabel("Ad:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        profilePanel.add(new JLabel("Soyad:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(surnameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        profilePanel.add(new JLabel("Mevcut Şifre:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(currentPasswordField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        profilePanel.add(new JLabel("Yeni Şifre:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(newPasswordField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        profilePanel.add(new JLabel("Yeni Şifre (Tekrar):"), gbc);
        gbc.gridx = 1;
        profilePanel.add(confirmPasswordField, gbc);

        JButton updateProfileButton = new JButton("Profili Güncelle");
        updateProfileButton.addActionListener(e -> {
            String currentPassword = new String(currentPasswordField.getPassword());
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            // Şifre kontrolü
            if (!newPassword.isEmpty() && !newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Yeni şifreler eşleşmiyor!");
                return;
            }
            
            try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                // Önce mevcut şifreyi kontrol et
                String checkQuery = "SELECT sifre FROM kullanicilar WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, adminEmail);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next() && rs.getString("sifre").equals(currentPassword)) {
                        // Şifre doğru, güncelleme yapılabilir
                        String updateQuery = "UPDATE kullanicilar SET ad = ?, soyad = ?" +
                                (newPassword.isEmpty() ? "" : ", sifre = ?") +
                                " WHERE email = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, nameField.getText());
                            updateStmt.setString(2, surnameField.getText());
                            if (!newPassword.isEmpty()) {
                                updateStmt.setString(3, newPassword);
                                updateStmt.setString(4, adminEmail);
                            } else {
                                updateStmt.setString(3, adminEmail);
                            }
                            updateStmt.executeUpdate();
                            
                            // Güncelleme başarılı
                            adminName = nameField.getText() + " " + surnameField.getText();
                            adminLabel.setText("Yönetici: " + adminName);
                            setTitle("Yönetici Paneli - " + adminName);
                            
                            JOptionPane.showMessageDialog(this, "Profil başarıyla güncellendi.");
                            currentPasswordField.setText("");
                            newPasswordField.setText("");
                            confirmPasswordField.setText("");
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Mevcut şifre yanlış!");
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Güncelleme sırasında bir hata oluştu!");
            }
        });

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        profilePanel.add(updateProfileButton, gbc);

        // Kullanıcı Listesi Tab
        JPanel userPanel = new JPanel(new BorderLayout());

        String[] columnNames = {"Ad", "Soyad", "Email", "Şifre", "Tip"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable userTable = new JTable(tableModel);
        userTable.setFont(new Font("Arial", Font.PLAIN, 12));
        userTable.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(userTable);
        userPanel.add(scrollPane, BorderLayout.CENTER);

        JButton loadUsersButton = new JButton("Kullanıcıları Yükle");
        loadUsersButton.setFont(new Font("Arial", Font.PLAIN, 12));
        loadUsersButton.addActionListener(e -> loadUsers(tableModel));
        userPanel.add(loadUsersButton, BorderLayout.SOUTH);
        mainTabbedPane.addTab("Kullanıcı Listesi", userPanel);

        // Stok Kontrolü Tab
        JPanel stockPanel = new JPanel(new BorderLayout());
        String[] stockColumnNames = {"İlaç ID", "İlaç Adı", "Stok"};
        DefaultTableModel stockTableModel = new DefaultTableModel(stockColumnNames, 0);
        JTable stockTable = new JTable(stockTableModel);
        stockTable.setFont(new Font("Arial", Font.PLAIN, 12));
        stockTable.setRowHeight(25);

        JScrollPane stockScrollPane = new JScrollPane(stockTable);
        stockPanel.add(stockScrollPane, BorderLayout.CENTER);

        JButton loadStockButton = new JButton("Stokları Yükle");
        loadStockButton.setFont(new Font("Arial", Font.PLAIN, 12));
        loadStockButton.addActionListener(e -> loadStock(stockTableModel));
        stockPanel.add(loadStockButton, BorderLayout.SOUTH);
        mainTabbedPane.addTab("Stok Kontrolü", stockPanel);

        // İlaç İşlemleri Tab
        JTabbedPane drugTabbedPane = new JTabbedPane();

        // İlaç Ekleme Paneli
        JPanel addDrugPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        JTextField tfDrugId = new JTextField();
        JTextField tfDrugName = new JTextField();
        JTextField tfDrugPrice = new JTextField();
        JTextField tfDrugStock = new JTextField();

        addDrugPanel.add(new JLabel("İlaç ID:"));
        addDrugPanel.add(tfDrugId);
        addDrugPanel.add(new JLabel("İlaç Adı:"));
        addDrugPanel.add(tfDrugName);
        addDrugPanel.add(new JLabel("İlaç Fiyatı:"));
        addDrugPanel.add(tfDrugPrice);
        addDrugPanel.add(new JLabel("Stok Miktarı:"));
        addDrugPanel.add(tfDrugStock);

        JButton addDrugButton = new JButton("İlaç Ekle");
        addDrugButton.setFont(new Font("Arial", Font.PLAIN, 12));
        addDrugButton.addActionListener(e -> {
            String id = tfDrugId.getText().trim();
            String name = tfDrugName.getText().trim();
            String price = tfDrugPrice.getText().trim();
            String stock = tfDrugStock.getText().trim();

            // Veri Girişi Kontrolleri
            if (id.isEmpty() || name.isEmpty() || price.isEmpty() || stock.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Tüm alanlar doldurulmalıdır!");
            } else {
                try {
                    Integer.parseInt(id);  // ID'nin sayısal bir değer olduğunu kontrol et
                    Double.parseDouble(price); // Fiyatın sayısal olduğunu kontrol et
                    Integer.parseInt(stock);  // Stok miktarının sayısal olduğunu kontrol et

                    addDrug(id, name, price, stock);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "ID, Fiyat ve Stok sayısal değerler olmalıdır!");
                }
            }
        });

        JButton clearFieldsButton = new JButton("Alanları Temizle");
        clearFieldsButton.setFont(new Font("Arial", Font.PLAIN, 12));
        clearFieldsButton.addActionListener(e -> {
            tfDrugId.setText("");
            tfDrugName.setText("");
            tfDrugPrice.setText("");
            tfDrugStock.setText("");
        });

        JPanel addDrugWrapper = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addDrugButton);
        buttonPanel.add(clearFieldsButton);

        addDrugWrapper.add(addDrugPanel, BorderLayout.CENTER);
        addDrugWrapper.add(buttonPanel, BorderLayout.SOUTH);

        drugTabbedPane.addTab("İlaç Ekleme", addDrugWrapper);

        // İlaç Fiyatı Değiştirme Paneli
        JPanel changePricePanel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField tfDrugIdPrice = new JTextField();
        JTextField tfNewPrice = new JTextField();

        changePricePanel.add(new JLabel("İlaç ID:"));
        changePricePanel.add(tfDrugIdPrice);
        changePricePanel.add(new JLabel("Yeni Fiyat:"));
        changePricePanel.add(tfNewPrice);

        JButton changePriceButton = new JButton("Fiyatı Değiştir");
        changePriceButton.setFont(new Font("Arial", Font.PLAIN, 12));
        changePriceButton.addActionListener(e -> changePrice(tfDrugIdPrice.getText(), tfNewPrice.getText()));

        JButton clearPriceFieldsButton = new JButton("Alanları Temizle");
        clearPriceFieldsButton.setFont(new Font("Arial", Font.PLAIN, 12));
        clearPriceFieldsButton.addActionListener(e -> {
            tfDrugIdPrice.setText("");
            tfNewPrice.setText("");
        });

        JPanel changePriceWrapper = new JPanel(new BorderLayout());
        JPanel priceButtonPanel = new JPanel(new FlowLayout());
        priceButtonPanel.add(changePriceButton);
        priceButtonPanel.add(clearPriceFieldsButton);

        changePriceWrapper.add(changePricePanel, BorderLayout.CENTER);
        changePriceWrapper.add(priceButtonPanel, BorderLayout.SOUTH);

        drugTabbedPane.addTab("Fiyat Değiştirme", changePriceWrapper);

        mainTabbedPane.addTab("İlaç İşlemleri", drugTabbedPane);

        // Satış Raporları Tab
        JPanel salesPanel = new JPanel(new BorderLayout());
        String[] salesColumns = {"Sipariş ID", "Kullanıcı", "İlaç", "Miktar", "Fiyat", "Tarih", "Durum"};
        DefaultTableModel salesTableModel = new DefaultTableModel(salesColumns, 0);
        JTable salesTable = new JTable(salesTableModel);
        salesTable.setFont(new Font("Arial", Font.PLAIN, 12));
        salesTable.setRowHeight(25);

        JScrollPane salesScrollPane = new JScrollPane(salesTable);
        salesPanel.add(salesScrollPane, BorderLayout.CENTER);

        JPanel salesControlPanel = new JPanel(new FlowLayout());
        JButton loadSalesButton = new JButton("Satışları Yükle");
        JComboBox<String> dateFilterCombo = new JComboBox<>(new String[]{"Tümü", "Bugün", "Bu Hafta", "Bu Ay"});
        
        loadSalesButton.addActionListener(e -> {
            loadSalesReport(salesTableModel, dateFilterCombo.getSelectedItem().toString());
        });

        salesControlPanel.add(new JLabel("Tarih Filtresi:"));
        salesControlPanel.add(dateFilterCombo);
        salesControlPanel.add(loadSalesButton);
        salesPanel.add(salesControlPanel, BorderLayout.SOUTH);

        // Kullanıcı Aktiviteleri Tab
        JPanel activityPanel = new JPanel(new BorderLayout());
        String[] activityColumns = {"Kullanıcı", "Aktivite Tipi", "Detay", "Tarih"};
        DefaultTableModel activityTableModel = new DefaultTableModel(activityColumns, 0);
        JTable activityTable = new JTable(activityTableModel);
        activityTable.setFont(new Font("Arial", Font.PLAIN, 12));
        activityTable.setRowHeight(25);

        JScrollPane activityScrollPane = new JScrollPane(activityTable);
        activityPanel.add(activityScrollPane, BorderLayout.CENTER);

        JPanel activityControlPanel = new JPanel(new FlowLayout());
        JButton loadActivityButton = new JButton("Aktiviteleri Yükle");
        JTextField userFilterField = new JTextField(15);
        
        loadActivityButton.addActionListener(e -> {
            loadActivityReport(activityTableModel, userFilterField.getText().trim());
        });

        activityControlPanel.add(new JLabel("Kullanıcı Filtresi:"));
        activityControlPanel.add(userFilterField);
        activityControlPanel.add(loadActivityButton);
        activityPanel.add(activityControlPanel, BorderLayout.SOUTH);

        // Tüm tabları ekle
        mainTabbedPane.addTab("Profil Düzenle", profilePanel);
        mainTabbedPane.addTab("Kullanıcı Listesi", userPanel);
        mainTabbedPane.addTab("Stok Kontrolü", stockPanel);
        mainTabbedPane.addTab("İlaç İşlemleri", drugTabbedPane);
        mainTabbedPane.addTab("Satış Raporları", salesPanel);
        mainTabbedPane.addTab("Kullanıcı Aktiviteleri", activityPanel);

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    private void loadUsers(DefaultTableModel tableModel) {
        tableModel.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            String query = "SELECT ad, soyad, email, sifre, type FROM kullanicilar";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getString("ad"),
                            rs.getString("soyad"),
                            rs.getString("email"),
                            rs.getString("sifre"),
                            rs.getString("type")
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadStock(DefaultTableModel stockTableModel) {
        stockTableModel.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            String query = "SELECT ilacid, ilacadi, stokmik FROM ilaclar";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    stockTableModel.addRow(new Object[]{
                            rs.getInt("ilacid"),
                            rs.getString("ilacadi"),
                            rs.getInt("stokmik")
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void addDrug(String id, String name, String price, String stock) {
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            String query = "INSERT INTO ilaclar (ilacid, ilacadi, ilacfiyat, stokmik) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, Integer.parseInt(id));
                stmt.setString(2, name);
                stmt.setDouble(3, Double.parseDouble(price));
                stmt.setInt(4, Integer.parseInt(stock));
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(null, "İlaç başarıyla eklendi.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "İlaç eklerken hata oluştu.");
        }
    }

    private void changePrice(String drugId, String newPrice) {
        if (drugId.isEmpty() || newPrice.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Lütfen tüm alanları doldurun.");
            return;
        }

        try {
            int id = Integer.parseInt(drugId);
            double price = Double.parseDouble(newPrice);

            try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                String query = "UPDATE ilaçlar SET ilacfiyat = ? WHERE ilacid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setDouble(1, price);
                    stmt.setInt(2, id);

                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(null, "İlaç fiyatı başarıyla değiştirildi.");
                    } else {
                        JOptionPane.showMessageDialog(null, "İlaç ID bulunamadı.");
                    }
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "ID ve fiyat sayısal değerler olmalıdır.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Fiyat değiştirilirken bir hata oluştu.");
        }
    }

    // Satış raporu yükleme metodu
    private void loadSalesReport(DefaultTableModel model, String dateFilter) {
        model.setRowCount(0);
        String query = "SELECT s.id, k.ad, k.soyad, i.ilacadi, s.miktar, s.fiyat, s.tarih, s.durum " +
                      "FROM siparisler s " +
                      "JOIN kullanicilar k ON s.kullanici_id = k.id " +
                      "JOIN ilaclar i ON s.ilac_id = i.ilacid ";

        switch (dateFilter) {
            case "Bugün":
                query += "WHERE DATE(s.tarih) = CURDATE()";
                break;
            case "Bu Hafta":
                query += "WHERE YEARWEEK(s.tarih) = YEARWEEK(CURDATE())";
                break;
            case "Bu Ay":
                query += "WHERE MONTH(s.tarih) = MONTH(CURDATE()) AND YEAR(s.tarih) = YEAR(CURDATE())";
                break;
        }
        query += " ORDER BY s.tarih DESC";

        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("ad") + " " + rs.getString("soyad"),
                    rs.getString("ilacadi"),
                    rs.getInt("miktar"),
                    rs.getDouble("fiyat"),
                    rs.getTimestamp("tarih"),
                    rs.getString("durum")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Satış raporu yüklenirken hata oluştu!");
        }
    }

    // Aktivite raporu yükleme metodu
    private void loadActivityReport(DefaultTableModel model, String userFilter) {
        model.setRowCount(0);
        String query = "SELECT k.ad, k.soyad, ka.aktivite_tipi, ka.detay, ka.tarih " +
                      "FROM kullanici_aktivite ka " +
                      "JOIN kullanicilar k ON ka.kullanici_id = k.id ";

        if (!userFilter.isEmpty()) {
            query += "WHERE k.ad LIKE ? OR k.soyad LIKE ? ";
        }
        query += "ORDER BY ka.tarih DESC";

        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (!userFilter.isEmpty()) {
                String searchPattern = "%" + userFilter + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("ad") + " " + rs.getString("soyad"),
                    rs.getString("aktivite_tipi"),
                    rs.getString("detay"),
                    rs.getTimestamp("tarih")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Aktivite raporu yüklenirken hata oluştu!");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            yönetici adminPanel = new yönetici("admin@example.com"); // Test için örnek e-posta
            adminPanel.setVisible(true);
        });
    }
} 