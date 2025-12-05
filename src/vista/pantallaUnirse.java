package vista;

import controlador.ClienteControlador;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PantallaUnirse extends JFrame {

    private Image backgroundImage;
    private JTextField txtIP;
    private JTextField txtPuerto;
    
    // Datos del jugador
    private String nombreJugador;
    private String rutaAvatar;

    /**
     * Constructor que recibe nombre y avatar del jugador
     */
    public PantallaUnirse(String nombreJugador, String rutaAvatar) {
        this.nombreJugador = nombreJugador;
        this.rutaAvatar = rutaAvatar;
        
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
        int h = 380;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        JPanel panel = new JPanel(null);
        panel.setBounds(x, y, w, h);
        panel.setBackground(new Color(0, 0, 0, 140));
        panel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        mainPanel.add(panel);

        // ======= ESTILOS REUTILIZABLES =======
        Font tituloFont = new Font("Arial", Font.BOLD, 34);
        Font labelFont = new Font("Arial", Font.BOLD, 18);
        Font inputFont = new Font("Arial", Font.PLAIN, 16);

        Color grisClaro = new Color(220, 220, 220);
        Color inputBg = new Color(25, 25, 25);

        Color amarillo = new Color(255, 207, 64);
        Color amarilloHover = new Color(255, 225, 110);

        // ======= TÍTULO =======
        JLabel titulo = new JLabel("Unirse a Partida");
        titulo.setBounds(0, 20, w, 45);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(tituloFont);
        titulo.setForeground(Color.WHITE);
        panel.add(titulo);

        // ======= MOSTRAR NOMBRE DEL JUGADOR =======
        JLabel lblJugador = new JLabel("Jugador: " + nombreJugador);
        lblJugador.setFont(new Font("Arial", Font.BOLD, 16));
        lblJugador.setForeground(new Color(255, 235, 59));
        lblJugador.setBounds(40, 75, 400, 25);
        panel.add(lblJugador);

        // ======= LABELS =======
        JLabel lblIP = new JLabel("IP del Servidor:");
        lblIP.setForeground(Color.WHITE);
        lblIP.setFont(labelFont);
        lblIP.setBounds(40, 115, 150, 30);
        panel.add(lblIP);

        JLabel lblPuerto = new JLabel("Puerto:");
        lblPuerto.setForeground(Color.WHITE);
        lblPuerto.setFont(labelFont);
        lblPuerto.setBounds(40, 175, 150, 30);
        panel.add(lblPuerto);

        // ======= INPUTS =======
        txtIP = new JTextField("localhost");
        txtIP.setBounds(200, 115, 220, 30);
        estiloInput(txtIP, inputBg, inputFont);
        panel.add(txtIP);

        txtPuerto = new JTextField("8000");
        txtPuerto.setBounds(200, 175, 220, 30);
        estiloInput(txtPuerto, inputBg, inputFont);
        panel.add(txtPuerto);

        // ======= BOTONES =======
        int btnY = 240;

        // Botón Volver
        JButton btnVolver = new JButton("Volver");
        btnVolver.setBounds(60, btnY, 160, 50);
        btnVolver.setFont(new Font("Arial", Font.BOLD, 20));
        btnVolver.setFocusable(false);
        btnVolver.setBackground(new Color(200, 200, 200));
        btnVolver.setForeground(Color.BLACK);
        btnVolver.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVolver.setOpaque(true);
        btnVolver.setContentAreaFilled(true);
        btnVolver.setFocusPainted(false);

        btnVolver.setBorder(BorderFactory.createBevelBorder(
                javax.swing.border.BevelBorder.RAISED,
                Color.WHITE,
                new Color(150, 150, 150)
        ));

        btnVolver.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnVolver.setBackground(new Color(220, 220, 220));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnVolver.setBackground(new Color(200, 200, 200));
            }
        });

        btnVolver.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RegistrarJugador seleccionar = new RegistrarJugador(false);
                seleccionar.setVisible(true);
                dispose();
            }
        });

        panel.add(btnVolver);

        // Botón Aceptar
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBounds(260, btnY, 160, 50);
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
     * Valida los campos y conecta al servidor
     */
    private void validarYContinuar() {
        String ip = txtIP.getText().trim();
        String puertoStr = txtPuerto.getText().trim();

        // Validar IP
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa la IP del servidor",
                    "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar puerto
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

        // Desactivar botón para evitar doble click
        Component[] components = ((JPanel) getContentPane()).getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                for (Component subComp : ((JPanel) comp).getComponents()) {
                    if (subComp instanceof JButton && ((JButton) subComp).getText().equals("Aceptar")) {
                        subComp.setEnabled(false);
                        ((JButton) subComp).setText("Conectando...");
                    }
                }
            }
        }

        // Conectar al servidor
        conectarYAbrir(ip, puerto);
    }

    /**
     * Conecta al servidor y abre PantallaCarga
     */
    private void conectarYAbrir(String ip, int puerto) {
        Thread hiloConexion = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[PantallaUnirse] Conectando a " + ip + ":" + puerto);

                    ClienteControlador controlador = new ClienteControlador(null);

                    boolean conectado = controlador.conectar(ip, puerto);

                    if (!conectado) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(PantallaUnirse.this,
                                        "No se pudo conectar al servidor",
                                        "Error de Conexión",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }

                    System.out.println("[PantallaUnirse] Conectado exitosamente");
                    Thread.sleep(500);

                    // Registrar jugador
                    System.out.println("[PantallaUnirse] Registrando jugador: " + nombreJugador);
                    boolean registrado = controlador.registrar(nombreJugador);

                    if (!registrado) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(PantallaUnirse.this,
                                        "No se pudo registrar el jugador",
                                        "Error de Registro",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }

                    System.out.println("[PantallaUnirse] Jugador registrado");

                    // Unirse a partida
                    System.out.println("[PantallaUnirse] Uniéndose a partida...");
                    controlador.unirseAPartidaDisponible();
                    Thread.sleep(500);

                    // Abrir pantalla de carga
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("[PantallaUnirse] Abriendo pantalla de carga");

                            // Constructor para quien se une (sin información de partida)
                            PantallaCarga pantallaCarga = new PantallaCarga(controlador);

                            pantallaCarga.setVisible(true);
                            dispose();
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(PantallaUnirse.this,
                                    "Error: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }
        });

        hiloConexion.start();
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
                new PantallaUnirse("Carlos", "/vista/recursos/pp.png");
            }
        });
    }
}