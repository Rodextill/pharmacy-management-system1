import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class kayit extends JFrame {

    // Gmail üzerinden e-posta gönderme
    public static void sendEmail(String recipientEmail, String subject, String body) {
        String host = "smtp.gmail.com";
        String from = "bayspace85@gmail.com";  // Buraya kendi e-posta adresinizi yazın
        String password = "jnwv nfep grcb yncl";  // Buraya e-posta hesabınızın şifresini yazın

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        // Oturum açma
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            // E-posta mesajını oluştur
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject(subject);
            message.setText(body);

            // E-posta gönder
            Transport.send(message);
            System.out.println("E-posta başarıyla gönderildi.");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    final private Font mainFont = new Font("Mirava", Font.BOLD, 18);
    JTextField tfFirstName, tfLastName, tfEmail;
    JPasswordField pfPassword;
    JLabel lbwelcome;
    JButton btnShowPassword;

    // Veritabanı bağlantı bilgileri
    private final String url = "jdbc:mysql://localhost:3306/deneme?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private final String dbUsername = "root";
    private final String dbPassword = "";

    public void initialize() {
        // Form Panel
        JLabel lbFirstName = new JLabel("Adiniz: ");
        lbFirstName.setFont(mainFont);

        tfFirstName = new JTextField();
        tfFirstName.setFont(mainFont);

        JLabel lbLastName = new JLabel("Soyadiniz: ");
        lbLastName.setFont(mainFont);

        tfLastName = new JTextField();
        tfLastName.setFont(mainFont);

        JLabel lbEmail = new JLabel("E-posta Adresiniz: ");
        lbEmail.setFont(mainFont);

        tfEmail = new JTextField();
        tfEmail.setFont(mainFont);

        JLabel lbPassword = new JLabel("Şifre: ");
        lbPassword.setFont(mainFont);

        // Şifre alanı, JPasswordField ile oluşturuluyor
        pfPassword = new JPasswordField();
        pfPassword.setFont(mainFont);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(5, 1, 5, 5));
        formPanel.setOpaque(false);
        formPanel.add(lbFirstName);
        formPanel.add(tfFirstName);
        formPanel.add(lbLastName);
        formPanel.add(tfLastName);
        formPanel.add(lbEmail);
        formPanel.add(tfEmail);
        formPanel.add(lbPassword);
        formPanel.add(pfPassword);

        lbwelcome = new JLabel();
        lbwelcome.setFont(mainFont);

        // Şifreyi gösterme butonu
        btnShowPassword = new JButton("Şifreyi Göster");
        btnShowPassword.setFont(mainFont);
        btnShowPassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pfPassword.getEchoChar() == '*') {
                    pfPassword.setEchoChar((char) 0);  // Şifreyi göster
                    btnShowPassword.setText("Şifreyi Gizle");
                } else {
                    pfPassword.setEchoChar('*');  // Şifreyi gizle
                    btnShowPassword.setText("Şifreyi Göster");
                }
            }
        });

        // Buttons Panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 3, 5, 5));  // 3 buton olduğu için 1 satır, 3 sütun
        buttonsPanel.setOpaque(false);

        JButton btnRegister = new JButton("Kayıt Ol");
        btnRegister.setFont(mainFont);
        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Kullanıcıdan alınan bilgileri al
                String firstName = tfFirstName.getText().trim();
                String lastName = tfLastName.getText().trim();
                String email = tfEmail.getText().trim();
                String password = new String(pfPassword.getPassword()).trim();  // Şifreyi al

                // Ad ve soyad boş olamaz
                if (firstName.isEmpty() || lastName.isEmpty()) {
                    lbwelcome.setText("Ad ve soyad boş bırakılamaz!");
                    return;
                }

                // Şifre en fazla 8 karakter olabilir
                if (password.length() > 8) {
                    lbwelcome.setText("Şifre en fazla 8 karakter olmalıdır!");
                    return;
                }

                // Geçerli bir e-posta formatı olup olmadığını kontrol et
                if (!isValidEmail(email)) {
                    lbwelcome.setText("Geçerli bir e-posta adresi girin!");
                    return;
                }

                // Veritabanına yeni kullanıcı kaydet
                try {
                    // MySQL sürücüsünü yükle
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    
                    // Bağlantıyı oluştur
                    Connection connection = DriverManager.getConnection(url, dbUsername, dbPassword);
                    String query = "INSERT INTO kullanicilar (ad, soyad, email, sifre, type, bakiye) VALUES (?, ?, ?, ?, '0', 0)";
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.setString(1, firstName);
                        stmt.setString(2, lastName);
                        stmt.setString(3, email);
                        stmt.setString(4, password);

                        int rowsAffected = stmt.executeUpdate();

                        if (rowsAffected > 0) {
                            // E-posta gönderme işlemini burada yapıyoruz
                            String subject = "Kayıt Başarılı!";
                            String body = "Merhaba " + firstName + " " + lastName + ",\n\n" +
                                    "Kayıt işleminiz başarıyla tamamlanmıştır. Hoş geldiniz!";
                            sendEmail(email, subject, body);  // E-posta gönder

                            JOptionPane.showMessageDialog(kayit.this, "Kayıt Başarılı! Giriş sayfasına yönlendiriliyorsunuz.");
                            dispose(); // Kayıt penceresini kapat
                            giris loginFrame = new giris(); // Giriş penceresini aç
                            loginFrame.initialize();
                        } else {
                            lbwelcome.setText("Kayıt başarısız! Lütfen tekrar deneyin.");
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                    lbwelcome.setText("MySQL sürücüsü bulunamadı! Lütfen MySQL Connector/J'yi ekleyin.");
                } catch (SQLSyntaxErrorException ex) {
                    ex.printStackTrace();
                    lbwelcome.setText("Veritabanı tablosu bulunamadı! Lütfen tabloyu oluşturun.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    if (ex.getMessage().contains("Communications link failure")) {
                        lbwelcome.setText("MySQL sunucusuna bağlanılamadı! MySQL servisinin çalıştığından emin olun.");
                    } else if (ex.getMessage().contains("Access denied")) {
                        lbwelcome.setText("Veritabanı erişim hatası! Kullanıcı adı ve şifreyi kontrol edin.");
                    } else {
                        lbwelcome.setText("Veritabanı hatası: " + ex.getMessage());
                    }
                }
            }
        });

        JButton btnClear = new JButton("Temizle");
        btnClear.setFont(mainFont);
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Tüm alanları temizle
                tfFirstName.setText("");
                tfLastName.setText("");
                tfEmail.setText("");
                pfPassword.setText("");
                lbwelcome.setText("");
                btnShowPassword.setText("Şifreyi Göster");
                pfPassword.setEchoChar('*');  // Şifreyi tekrar gizle
            }
        });

        buttonsPanel.add(btnRegister);
        buttonsPanel.add(btnShowPassword);
        buttonsPanel.add(btnClear);

        // Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(128, 128, 255));  // Pencere arka plan rengi
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Pencere kenar boşluğu
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(lbwelcome, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Ana pencereye formu ekle
        add(mainPanel);

        // Pencere ayarları
        setTitle("Kullanıcı Kayıt Formu");
        setSize(500, 600);
        setMinimumSize(new Dimension(300, 400));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // E-posta doğrulama fonksiyonu
    public boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
    
    public static void show(String type){
        switch (type){
            case "0":
            	System.out.println("Giriş kullanıcı tarafından yapıldı.");
                break;
            case "1":
                System.out.println("Giriş yönetici tarafından yapıldı.");
        }
    }

    public static void main(String[] args) {
        kayit myFrame = new kayit();
        myFrame.initialize();
    }
} 