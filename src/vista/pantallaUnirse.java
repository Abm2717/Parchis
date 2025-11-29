package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PantallaUnirse extends JFrame {

    private Image backgroundImage;
    private JTextField txtNombre;
    private JTextField txtIP;
    private JTextField txtPuerto;

    public PantallaUnirse() {
        setTitle("Unirse a Partida");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        // Cargar imagen de fondo
        backgroundImage = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg")).getImage();

        // Panel principal con fondo
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        mainPanel.setLayout(null);
        setContentPane(mainPanel);

        // Dimensiones de pantalla
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        // Panel translúcido centrado 
        int w = 480;
        int h = 320;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        JPanel panel = new JPanel(null);
        panel.setBounds(x, y, w, h);
        panel.setBackground(new Color(0, 0, 0, 140));
        panel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        mainPanel.add(panel);

        // ======= ESTILOS REUTILIZABLES =======
        Font labelFont = new Font("Arial", Font.BOLD, 18);
        Font inputFont = new Font("Arial", Font.PLAIN, 16);

        Color grisClaro = new Color(220, 220, 220);
        Color inputBg = new Color(25, 25, 25);

        Color amarillo = new Color(255, 207, 64);
        Color amarilloHover = new Color(255, 225, 110);

        // ======= LABELS =======
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setForeground(Color.WHITE);
        lblNombre.setFont(labelFont);
        lblNombre.setBounds(40, 40, 150, 30);
        panel.add(lblNombre);

        JLabel lblIP = new JLabel("IP del Servidor:");
        lblIP.setForeground(Color.WHITE);
        lblIP.setFont(labelFont);
        lblIP.setBounds(40, 95, 150, 30);
        panel.add(lblIP);

        JLabel lblPuerto = new JLabel("Puerto:");
        lblPuerto.setForeground(Color.WHITE);
        lblPuerto.setFont(labelFont);
        lblPuerto.setBounds(40, 150, 150, 30);
        panel.add(lblPuerto);

        // ======= INPUTS =======
        txtNombre = new JTextField();
        txtNombre.setBounds(200, 40, 220, 30);
        estiloInput(txtNombre, inputBg, inputFont);
        panel.add(txtNombre);

        txtIP = new JTextField("localhost");
        txtIP.setBounds(200, 95, 220, 30);
        estiloInput(txtIP, inputBg, inputFont);
        panel.add(txtIP);

        txtPuerto = new JTextField("8000");
        txtPuerto.setBounds(200, 150, 220, 30);
        estiloInput(txtPuerto, inputBg, inputFont);
        panel.add(txtPuerto);

        // ======= BOTÓN ACEPTAR =======
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBounds((w - 260) / 2, 210, 260, 55);
        btnAceptar.setFont(new Font("Arial", Font.BOLD, 22));
        btnAceptar.setFocusable(false);
        btnAceptar.setBackground(amarillo);
        btnAceptar.setForeground(Color.BLACK);
        btnAceptar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAceptar.setOpaque(true);
        btnAceptar.setContentAreaFilled(true);
        btnAceptar.setFocusPainted(false);

        btnAceptar.setBorder(BorderFactory.createBevelBorder(
                javax.swing.border.BevelBorder.RAISED,
                Color.WHITE,
                new Color(200, 150, 0)
        ));

        // Hover
        btnAceptar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnAceptar.setBackground(amarilloHover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnAceptar.setBackground(amarillo);
            }
        });

        // ✅ EVENTO DEL BOTÓN
        btnAceptar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validarYContinuar();
            }
        });

        panel.add(btnAceptar);

        setVisible(true);
    }

    
    /**
    * Valida los campos y abre la pantalla de avatares
    */
    private void validarYContinuar() {
       // Validación de campos...
       String nombre = txtNombre.getText().trim();
       String ip = txtIP.getText().trim();
       String puertoStr = txtPuerto.getText().trim();

       // ... validaciones ...

       int puerto;
       try {
           puerto = Integer.parseInt(puertoStr);
           if (puerto < 1024 || puerto > 65535) {
               throw new NumberFormatException();
           }
       } catch (NumberFormatException ex) {
           JOptionPane.showMessageDialog(this,
               "Puerto inválido. Debe ser un número entre 1024 y 65535",
               "Error de puerto",
               JOptionPane.ERROR_MESSAGE);
           return;
       }

       // ✅ Abrir Avatares con constructor de UNIRSE
       Avatares avatares = new Avatares(nombre, ip, puerto);
       avatares.setVisible(true);

       dispose();
    }

    private void estiloInput(JTextField txt, Color bg, Font font) {
        txt.setOpaque(true);
        txt.setBackground(bg);
        txt.setForeground(Color.WHITE);
        txt.setFont(font);
        txt.setCaretColor(Color.WHITE);
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 90), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PantallaUnirse();
            }
        });
    }
}