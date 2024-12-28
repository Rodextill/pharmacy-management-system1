import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class KullaniciPaneli extends JFrame {
    private final String url = "jdbc:mysql://localhost:3306/deneme?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private final String dbUsername = "root";
    private final String dbPassword = "";

    private int loginAttempts = 0;
    private double userBalance = 0.0; // Başlangıç değeri 0

    public KullaniciPaneli(String email) {
        // Veritabanından bakiyeyi çek
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            String query = "SELECT bakiye FROM kullanicilar WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    userBalance = rs.getDouble("bakiye");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Bakiye bilgisi alınırken hata oluştu!");
        }
        showUserPanel(email);
    }

    private void sendEmail(String toEmail) {
        final String fromEmail = "bayspace85@gmail.com";
        final String fromPassword = "jnwv nfep grcb yncl";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, fromPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Başarısız Giriş Denemesi");
            message.setText("Hesabınıza 3 kez yanlış giriş yapılmıştır.");

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void showUserPanel(String email) {
        // Kullanıcı bilgilerini veritabanından al
        String userName = "";
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            String query = "SELECT ad, soyad, bakiye FROM kullanicilar WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    userName = rs.getString("ad") + " " + rs.getString("soyad");
                    userBalance = rs.getDouble("bakiye");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        JFrame userFrame = new JFrame("Kullanıcı Paneli - " + userName);
        userFrame.setSize(800, 600);
        userFrame.setLocationRelativeTo(null);
        userFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();

        // İlaç Satın Alma Tab
        JPanel buyDrugPanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Kullanıcı adı etiketi
        JLabel userLabel = new JLabel("Hoş geldiniz, " + userName);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // Bakiye etiketi
        JLabel balanceLabel = new JLabel("Bakiye: " + userBalance + " TL");
        balanceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        topPanel.add(userLabel);
        topPanel.add(balanceLabel);

        JPanel buyPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField drugIdField = new JTextField();
        JLabel drugPriceLabel = new JLabel("Fiyat: -");
        JTextField paymentField = new JTextField();

        buyPanel.add(new JLabel("İlaç ID:"));
        buyPanel.add(drugIdField);
        buyPanel.add(new JLabel("Fiyat:"));
        buyPanel.add(drugPriceLabel);
        buyPanel.add(new JLabel("Ödeme Miktarı:"));
        buyPanel.add(paymentField);

        JButton fetchPriceButton = new JButton("Fiyatı Getir");
        fetchPriceButton.addActionListener(e -> {
            String drugId = drugIdField.getText().trim();
            if (!drugId.isEmpty()) {
                try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                    String query = "SELECT ilacfiyat FROM ilaclar WHERE ilacid = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setInt(1, Integer.parseInt(drugId));
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            double price = rs.getDouble("ilacfiyat");
                            drugPriceLabel.setText("Fiyat: " + price + " TL");
                        } else {
                            JOptionPane.showMessageDialog(userFrame, "İlaç bulunamadı.");
                        }
                    }
                } catch (SQLException | NumberFormatException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(userFrame, "Hata oluştu. Lütfen geçerli bir ID girin.");
                }
            } else {
                JOptionPane.showMessageDialog(userFrame, "Lütfen bir ilaç ID girin.");
            }
        });

        JButton buyButton = new JButton("Satın Al");
        buyButton.addActionListener(e -> {
            String drugId = drugIdField.getText().trim();
            String paymentText = paymentField.getText().trim();
            if (!drugId.isEmpty() && !paymentText.isEmpty()) {
                try {
                    double payment = Double.parseDouble(paymentText);
                    double price = Double.parseDouble(drugPriceLabel.getText().replace("Fiyat: ", "").replace(" TL", ""));

                    if (payment >= price && userBalance >= payment) {
                        userBalance -= price;
                        balanceLabel.setText("Bakiye: " + userBalance + " TL");

                        // Veritabanında bakiye güncelleme ve sipariş kaydetme
                        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                            conn.setAutoCommit(false);
                            try {
                                // Bakiye güncelleme
                                String updateBalanceQuery = "UPDATE kullanicilar SET bakiye = ? WHERE email = ?";
                                try (PreparedStatement stmt = conn.prepareStatement(updateBalanceQuery)) {
                                    stmt.setDouble(1, userBalance);
                                    stmt.setString(2, email);
                                    stmt.executeUpdate();
                                }

                                // Stok güncelleme
                                String updateStockQuery = "UPDATE ilaclar SET stokmik = stokmik - 1 WHERE ilacid = ?";
                                try (PreparedStatement stmt = conn.prepareStatement(updateStockQuery)) {
                                    stmt.setInt(1, Integer.parseInt(drugId));
                                    stmt.executeUpdate();
                                }

                                // Sipariş kaydetme
                                String insertOrderQuery = "INSERT INTO siparisler (kullanici_id, ilac_id, miktar, fiyat, durum) " +
                                                        "SELECT k.id, ?, 1, ?, 'Tamamlandı' FROM kullanicilar k WHERE k.email = ?";
                                try (PreparedStatement stmt = conn.prepareStatement(insertOrderQuery)) {
                                    stmt.setInt(1, Integer.parseInt(drugId));
                                    stmt.setDouble(2, price);
                                    stmt.setString(3, email);
                                    stmt.executeUpdate();
                                }

                                conn.commit();
                                
                                // Aktivite kaydı
                                logActivity(email, "İlaç Satın Alma", "İlaç ID: " + drugId + ", Fiyat: " + price + " TL");
                                
                                JOptionPane.showMessageDialog(userFrame, "İlaç başarıyla satın alındı.");
                            } catch (SQLException ex) {
                                conn.rollback();
                                throw ex;
                            } finally {
                                conn.setAutoCommit(true);
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(userFrame, "Yetersiz bakiye veya ödeme tutarı.");
                    }
                } catch (NumberFormatException | SQLException ex) {
                    JOptionPane.showMessageDialog(userFrame, "Hata oluştu. Lütfen geçerli bir değer girin.");
                }
            } else {
                JOptionPane.showMessageDialog(userFrame, "Lütfen tüm alanları doldurun.");
            }
        });

        buyDrugPanel.add(topPanel, BorderLayout.NORTH);
        buyDrugPanel.add(buyPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(fetchPriceButton);
        buttonPanel.add(buyButton);

        buyDrugPanel.add(buttonPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("İlaç Satın Alma", buyDrugPanel);

        // Bakiye Yükleme Tab
        JPanel balancePanel = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField balanceField = new JTextField();
        JButton loadBalanceButton = new JButton("Bakiye Yükle");

        loadBalanceButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(balanceField.getText().trim());
                if (amount > 0) {
                    userBalance += amount;
                    balanceLabel.setText("Bakiye: " + userBalance + " TL");

                    // Veritabanında bakiye güncelleme
                    try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                        String updateBalanceQuery = "UPDATE kullanicilar SET bakiye = ? WHERE email = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(updateBalanceQuery)) {
                            stmt.setDouble(1, userBalance);
                            stmt.setString(2, email);
                            stmt.executeUpdate();
                        }
                    }

                    JOptionPane.showMessageDialog(userFrame, "Bakiye başarıyla yüklendi.");
                } else {
                    JOptionPane.showMessageDialog(userFrame, "Pozitif bir tutar girin.");
                }
            } catch (NumberFormatException | SQLException ex) {
                JOptionPane.showMessageDialog(userFrame, "Geçerli bir tutar girin.");
            }
        });

        balancePanel.add(new JLabel("Yüklenecek Bakiye:"));
        balancePanel.add(balanceField);
        balancePanel.add(loadBalanceButton);
        tabbedPane.addTab("Bakiye Yükleme", balancePanel);

        // Profil Düzenleme Tab
        JPanel profilePanel = new JPanel(new GridLayout(4, 2, 10, 10));
        JTextField nameField = new JTextField();
        JTextField surnameField = new JTextField();
        JPasswordField newPasswordField = new JPasswordField();

        JButton updateProfileButton = new JButton("Güncelle");
        updateProfileButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newSurname = surnameField.getText().trim();
            String newPassword = new String(newPasswordField.getPassword()).trim();

            if (!newName.isEmpty() && !newSurname.isEmpty() && !newPassword.isEmpty()) {
                try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                    String updateQuery = "UPDATE kullanicilar SET ad = ?, soyad = ?, sifre = ? WHERE email = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                        stmt.setString(1, newName);
                        stmt.setString(2, newSurname);
                        stmt.setString(3, newPassword);
                        stmt.setString(4, email);
                        stmt.executeUpdate();
                        JOptionPane.showMessageDialog(userFrame, "Profil başarıyla güncellendi.");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(userFrame, "Veritabanı güncelleme hatası!");
                }
            } else {
                JOptionPane.showMessageDialog(userFrame, "Lütfen tüm alanları doldurun.");
            }
        });

        profilePanel.add(new JLabel("Ad:"));
        profilePanel.add(nameField);
        profilePanel.add(new JLabel("Soyad:"));
        profilePanel.add(surnameField);
        profilePanel.add(new JLabel("Yeni Şifre:"));
        profilePanel.add(newPasswordField);
        profilePanel.add(updateProfileButton);
        tabbedPane.addTab("Profil Düzenleme", profilePanel);

        // İlaç Stokları Görüntüleme Tab
        JPanel stockPanel = new JPanel(new BorderLayout());
        JTable stockTable = new JTable(new DefaultTableModel(new Object[]{"İlaç ID", "İlaç Adı", "Stok"}, 0));
        JScrollPane stockScrollPane = new JScrollPane(stockTable);
        stockPanel.add(stockScrollPane, BorderLayout.CENTER);

        JButton loadStockButton = new JButton("Stokları Yükle");
        loadStockButton.addActionListener(e -> {
            DefaultTableModel model = (DefaultTableModel) stockTable.getModel();
            model.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                String query = "SELECT ilacid, ilacadi, stokmik FROM ilaclar";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        model.addRow(new Object[]{rs.getInt("ilacid"), rs.getString("ilacadi"), rs.getInt("stokmik")});
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(userFrame, "Stokları yüklerken bir hata oluştu.");
            }
        });

        stockPanel.add(loadStockButton, BorderLayout.SOUTH);
        tabbedPane.addTab("İlaç Stokları", stockPanel);

        userFrame.add(tabbedPane);
        userFrame.setVisible(true);
    }

    // Aktivite kaydetme metodu
    private void logActivity(String email, String aktiviteTipi, String detay) {
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            // Önce kullanıcı ID'sini al
            String userQuery = "SELECT id FROM kullanicilar WHERE email = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userQuery)) {
                userStmt.setString(1, email);
                ResultSet rs = userStmt.executeQuery();
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    
                    // Aktiviteyi kaydet
                    String logQuery = "INSERT INTO kullanici_aktivite (kullanici_id, aktivite_tipi, detay) VALUES (?, ?, ?)";
                    try (PreparedStatement logStmt = conn.prepareStatement(logQuery)) {
                        logStmt.setInt(1, userId);
                        logStmt.setString(2, aktiviteTipi);
                        logStmt.setString(3, detay);
                        logStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        });
    }
}
