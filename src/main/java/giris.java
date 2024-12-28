import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.sql.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class giris extends JFrame {

    kayit main = new kayit(); // Ana ekran sınıfı örneği

    // E-posta gönderim metodu
    public static void sendEmail(String recipientEmail, String subject, String body) throws MessagingException {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new MessagingException("E-posta adresi boş olamaz!");
        }

        String host = "smtp.gmail.com";
        String from = "bayspace85@gmail.com";  // E-posta adresiniz
        String password = "jnwv nfep grcb yncl";  // E-posta şifreniz

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Oturum açma
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        // E-posta mesajını oluştur
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
        message.setSubject(subject);
        message.setText(body);

        // E-posta gönder
        Transport.send(message);
        System.out.println("E-posta başarıyla gönderildi: " + recipientEmail);
    }

    final private Font mainFont = new Font("Mirava", Font.BOLD, 18);
    JTextField tfUsername;
    JPasswordField pfPassword;
    JLabel lbWelcome;
    int loginAttempts = 0;
    String userEmail = ""; // Kullanıcının e-posta adresini saklamak için
    JButton btnShowPassword; // Şifreyi göster butonu

    // Veritabanı bağlantı bilgileri
    private final String url = "jdbc:mysql://localhost:3306/deneme?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private final String dbUsername = "root";
    private final String dbPassword = "";

    public void initialize() {
        /********** Form Panel **********/
        JLabel lbUsername = new JLabel("Kullanıcı Adınız: ");
        lbUsername.setFont(mainFont);

        tfUsername = new JTextField();
        tfUsername.setFont(mainFont);

        JLabel lbPassword = new JLabel("Şifreniz: ");
        lbPassword.setFont(mainFont);

        pfPassword = new JPasswordField();
        pfPassword.setFont(mainFont);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(3, 1, 5, 5));
        formPanel.setOpaque(false);
        formPanel.add(lbUsername);
        formPanel.add(tfUsername);
        formPanel.add(lbPassword);
        formPanel.add(pfPassword);

        /********** Şifreyi Göster Butonu **********/
        btnShowPassword = new JButton("Şifreyi Göster");
        btnShowPassword.setFont(mainFont);
        btnShowPassword.addActionListener(e -> {
            if (pfPassword.getEchoChar() == '*') {
                pfPassword.setEchoChar((char) 0); // Şifreyi göster
                btnShowPassword.setText("Şifreyi Gizle");
            } else {
                pfPassword.setEchoChar('*'); // Şifreyi gizle
                btnShowPassword.setText("Şifreyi Göster");
            }
        });

        /********** Welcome Label **********/
        lbWelcome = new JLabel();
        lbWelcome.setFont(mainFont);

        /********** Buttons Panel **********/
        JButton btnLogin = new JButton("Giriş Yap");
        btnLogin.setFont(mainFont);
        btnLogin.addActionListener(e -> {
            String username = tfUsername.getText().trim();
            String password = new String(pfPassword.getPassword()).trim();

            if (username.isEmpty()) {
                lbWelcome.setText("Kullanıcı adı boş bırakılamaz!");
                return;
            }

            if (password.length() > 8) {
                lbWelcome.setText("Şifre en fazla 8 karakter olmalıdır!");
                return;
            }

            try (Connection connection = DriverManager.getConnection(url, dbUsername, dbPassword)) {
                String query = "SELECT type, email FROM kullanicilar WHERE ad = ? AND sifre = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);

                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String userType = rs.getString("type");
                        userEmail = rs.getString("email"); // Kullanıcı e-posta adresini al
                        JOptionPane.showMessageDialog(null, "Giriş Başarılı.");
                        dispose(); // Giriş penceresini kapat
                        
                        // Kullanıcı tipine göre yönlendirme
                        if ("1".equals(userType)) {
                            yönetici adminPanel = new yönetici(userEmail);
                            adminPanel.setVisible(true);
                        } else {
                            KullaniciPaneli userPanel = new KullaniciPaneli(userEmail);
                            userPanel.setVisible(true);
                        }
                    } else {
                        loginAttempts++;
                        if (loginAttempts >= 3) {
                            try {
                                // Önce veritabanından e-posta adresini alalım
                                String emailQuery = "SELECT email FROM kullanicilar WHERE ad = ?";
                                try (PreparedStatement emailStmt = connection.prepareStatement(emailQuery)) {
                                    emailStmt.setString(1, username);
                                    ResultSet emailRs = emailStmt.executeQuery();
                                    
                                    if (emailRs.next()) {
                                        String userEmail = emailRs.getString("email");
                                        if (userEmail != null && !userEmail.isEmpty()) {
                                            String subject = "Hesap Güvenliği: Şifre Yanlış Giriş Uyarısı";
                                            String body = "Merhaba,\n\n" +
                                                    "Hesabınızda 3 kez yanlış giriş denemesi yapılmıştır. " +
                                                    "Eğer bu girişleri siz yapmadıysanız, lütfen hemen şifrenizi değiştirin.\n\n" +
                                                    "Saygılarımızla,\nSistem Yönetimi";
                                            try {
                                                sendEmail(userEmail, subject, body);
                                                lbWelcome.setText("Hatalı giriş 3 kez yapıldı! E-posta gönderildi: " + userEmail);
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                                lbWelcome.setText("E-posta gönderirken hata oluştu!");
                                            }
                                        } else {
                                            lbWelcome.setText("Kullanıcının e-posta adresi bulunamadı!");
                                        }
                                    } else {
                                        lbWelcome.setText("Kullanıcı bulunamadı!");
                                    }
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                lbWelcome.setText("Veritabanı sorgulama hatası!");
                            }
                            loginAttempts = 0;
                        } else {
                            lbWelcome.setText("Hatalı giriş! Kalan hak: " + (3 - loginAttempts));
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                lbWelcome.setText("Veritabanı bağlantı hatası!");
            }
        });

        JButton btnClear = new JButton("Temizle");
        btnClear.setFont(mainFont);
        btnClear.addActionListener(e -> {
            tfUsername.setText("");
            pfPassword.setText("");
            lbWelcome.setText("");
            btnShowPassword.setText("Şifreyi Göster");
            pfPassword.setEchoChar('*'); // Şifreyi tekrar gizle
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 3, 5, 5));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(btnLogin);
        buttonsPanel.add(btnShowPassword);
        buttonsPanel.add(btnClear);

        /********** Main Panel **********/
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(128, 128, 255));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(lbWelcome, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(mainPanel);

        setTitle("Kullanıcı Giriş Formu");
        setSize(500, 600);
        setMinimumSize(new Dimension(300, 400));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        giris myFrame = new giris();
        myFrame.initialize();
    }
}
