import javax.swing.*;
import java.awt.*;

public class MainApplication extends JFrame {
    public MainApplication() {
        setTitle("Ana Menü");
        setSize(400, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Ana panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(128, 128, 255));

        // Butonlar
        JButton registerButton = new JButton("Kayıt Ol");
        JButton loginButton = new JButton("Giriş Yap");

        // Buton fontları
        Font buttonFont = new Font("Arial", Font.BOLD, 16);
        registerButton.setFont(buttonFont);
        loginButton.setFont(buttonFont);

        // Buton renkleri
        registerButton.setBackground(new Color(46, 204, 113));
        loginButton.setBackground(new Color(52, 152, 219));
        registerButton.setForeground(Color.WHITE);
        loginButton.setForeground(Color.WHITE);

        // Buton aksiyonları
        registerButton.addActionListener(e -> {
            try {
                // MySQL sürücüsünü yükle
                Class.forName("com.mysql.cj.jdbc.Driver");
                dispose(); // Ana menüyü kapat
                kayit registerFrame = new kayit();
                registerFrame.initialize();
            } catch (ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this, 
                    "MySQL sürücüsü bulunamadı! Lütfen MySQL Connector/J'yi kontrol edin.",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        loginButton.addActionListener(e -> {
            dispose(); // Ana menüyü kapat
            giris loginFrame = new giris();
            loginFrame.initialize();
        });

        // Butonları panele ekle
        mainPanel.add(registerButton);
        mainPanel.add(loginButton);

        // Paneli frame'e ekle
        add(mainPanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainApplication mainApp = new MainApplication();
            mainApp.setVisible(true);
        });
    }
}
