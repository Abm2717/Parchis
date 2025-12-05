package vista;

import controlador.ClienteControlador;
import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;

/**
 * Pantalla de sala de espera antes de iniciar la partida.
 * ✅ MODIFICADO - Ahora pasa el ClienteControlador a VentanaJuego
 */
public class PantallaCarga extends JFrame {
    
    private JLabel lblEspera;
    private JLabel lblGif;
    private JButton btnListo;
    private ClienteControlador controlador;
    
    // Variables para el creador
    private String nombreJugador;
    private String nombrePartida;
    private int maxJugadores;
    private int puerto;
    private String rutaAvatar;
    private boolean esCreador;
    
    // Labels para información (solo creador)
    private JLabel lblJugadores;
    
    // Constructor vacío (deprecated)
    public PantallaCarga() {
        this.esCreador = false;
        inicializar();
    }
    
    // Constructor para quien se une (sin información)
    public PantallaCarga(ClienteControlador controlador) {
        this.controlador = controlador;
        this.esCreador = false;
        
        // Configurar referencia a esta vista en el controlador
        if (controlador != null) {
            controlador.setVistaCarga(this);
        }
        
        inicializar();
    }
    
    // Constructor para creador (con toda la información)
    public PantallaCarga(ClienteControlador controlador, String nombreJugador, String nombrePartida, 
                         int maxJugadores, int puerto, String rutaAvatar) {
        this.controlador = controlador;
        this.nombreJugador = nombreJugador;
        this.nombrePartida = nombrePartida;
        this.maxJugadores = maxJugadores;
        this.puerto = puerto;
        this.rutaAvatar = rutaAvatar;
        this.esCreador = true;
        
        // Configurar referencia a esta vista en el controlador
        if (controlador != null) {
            controlador.setVistaCarga(this);
        }
        
        inicializar();
    }
    
    private void inicializar() {
        setTitle("Sala de Espera - Parchís");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        
        inicializarComponentes();
        
        setVisible(true);
    }
    
    private void inicializarComponentes() {
        // Panel principal con fondo
        JPanel panelPrincipal = new JPanel() {
            private Image fondo;
            
            {
                try {
                    fondo = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg")).getImage();
                } catch (Exception e) {
                    System.err.println("Error cargando fondo: " + e.getMessage());
                }
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (fondo != null) {
                    g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        panelPrincipal.setLayout(null);
        
        // ✅ Panel contenedor transparente (sin fondo negro)
        int panelWidth = esCreador ? 600 : 600;
        int panelHeight = esCreador ? 600 : 400;
        
        JPanel panelCentral = new JPanel(null);
        panelCentral.setOpaque(false); // ✅ Completamente transparente
        
        // Centrar panel
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int x = (getWidth() - panelWidth) / 2;
                int y = (getHeight() - panelHeight) / 2;
                panelCentral.setBounds(x, y, panelWidth, panelHeight);
            }
        });
        
        // ✅ CENTRADO VERTICAL - Calcular posición base
        int contentHeight = esCreador ? 650 : 450;
        int startY = (panelHeight - contentHeight) / 2;
        
        // ✅ GIF DE CARGA directamente sin panel
        try {
            ImageIcon gifIcon = new ImageIcon(getClass().getResource("/vista/recursos/loading.gif"));
            Image gifImage = gifIcon.getImage().getScaledInstance(250, 250, Image.SCALE_DEFAULT);
            lblGif = new JLabel(new ImageIcon(gifImage));
            lblGif.setBounds((panelWidth - 300) / 2, startY, 300, 300);
            panelCentral.add(lblGif);
        } catch (Exception e) {
            System.err.println("Error cargando GIF: " + e.getMessage());
        }
        
        // Texto de espera - centrado verticalmente
        lblEspera = new JLabel("Esperando jugadores...");
        lblEspera.setFont(new Font("Arial", Font.BOLD, 28));
        lblEspera.setForeground(Color.WHITE);
        lblEspera.setHorizontalAlignment(SwingConstants.CENTER);
        lblEspera.setBounds(0, startY + 310, panelWidth, 40);
        panelCentral.add(lblEspera);
        
        // Botón Listo - centrado verticalmente
        btnListo = new JButton("Listo");
        btnListo.setBounds((panelWidth - 200) / 2, startY + 370, 200, 50);
        btnListo.setFont(new Font("Arial", Font.BOLD, 22));
        btnListo.setBackground(new Color(255, 235, 59));
        btnListo.setForeground(Color.BLACK);
        btnListo.setFocusPainted(false);
        btnListo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnListo.addActionListener(e -> marcarListo());
        panelCentral.add(btnListo);
        
        // Panel de información (solo para creador) - centrado verticalmente
        if (esCreador) {
            JPanel panelInfo = new JPanel(null);
            panelInfo.setBackground(new Color(0, 0, 0, 180));
            panelInfo.setBorder(BorderFactory.createLineBorder(new Color(255, 207, 64), 2));
            panelInfo.setBounds(50, startY + 440, 500, 180);
            
            // Título del panel
            JLabel lblTituloInfo = new JLabel("Información de la Partida");
            lblTituloInfo.setFont(new Font("Arial", Font.BOLD, 24));
            lblTituloInfo.setForeground(new Color(255, 207, 64));
            lblTituloInfo.setBounds(0, 10, 500, 30);
            lblTituloInfo.setHorizontalAlignment(SwingConstants.CENTER);
            panelInfo.add(lblTituloInfo);
            
            // Información
            int yPos = 50;
            int spacing = 30;
            
            JLabel lblNombrePartida = crearLabelInfo("Partida: " + nombrePartida);
            lblNombrePartida.setBounds(20, yPos, 460, 25);
            panelInfo.add(lblNombrePartida);
            
            lblJugadores = crearLabelInfo("Jugadores: 1 / " + maxJugadores);
            lblJugadores.setBounds(20, yPos + spacing, 460, 25);
            panelInfo.add(lblJugadores);
            
            // Obtener IP local
            String ipLocal = "N/A";
            try {
                ipLocal = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                System.err.println("Error obteniendo IP: " + e.getMessage());
            }
            
            JLabel lblIP = crearLabelInfo("IP: " + ipLocal);
            lblIP.setBounds(20, yPos + spacing * 2, 460, 25);
            panelInfo.add(lblIP);
            
            JLabel lblPuerto = crearLabelInfo("Puerto: " + puerto);
            lblPuerto.setBounds(20, yPos + spacing * 3, 460, 25);
            panelInfo.add(lblPuerto);
            
            panelCentral.add(panelInfo);
        }
        
        panelPrincipal.add(panelCentral);
        setContentPane(panelPrincipal);
    }
    
    private JLabel crearLabelInfo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.PLAIN, 20));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }
    
    private void marcarListo() {
        if (controlador == null) {
            System.err.println("[ERROR] Controlador es null");
            return;
        }
        
        boolean exito = controlador.marcarListo();
        
        if (exito) {
            btnListo.setEnabled(false);
            btnListo.setText("Esperando...");
            btnListo.setBackground(new Color(150, 150, 150));
            actualizarTextoEspera("Esperando a otros jugadores...");
        }
    }
    
    public void actualizarTextoEspera(String texto) {
        if (lblEspera != null) {
            SwingUtilities.invokeLater(() -> lblEspera.setText(texto));
        }
    }
    
    public void actualizarJugadores(int jugadoresActuales) {
        if (lblJugadores != null) {
            SwingUtilities.invokeLater(() -> {
                lblJugadores.setText("Jugadores: " + jugadoresActuales + " / " + maxJugadores);
            });
        }
    }
    
    public void iniciarPartida() {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[PantallaCarga] iniciarPartida() llamado");
            
            // Intentar obtener nombres con reintentos
            String[] nombres = null;
            int intentos = 0;
            int maxIntentos = 20;
            
            while (nombres == null && intentos < maxIntentos) {
                nombres = controlador.getNombresGuardados();
                
                if (nombres == null) {
                    System.out.println("[PantallaCarga] Esperando nombres... intento " + (intentos + 1));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    intentos++;
                }
            }
            
            if (nombres == null) {
                System.err.println("[ERROR PantallaCarga] No se pudieron obtener nombres después de " + maxIntentos + " intentos");
                nombres = new String[] { "Jugador Rojo", "Jugador Azul", "Jugador Verde", "Jugador Amarillo" };
            } else {
                System.out.println("[PantallaCarga] Nombres obtenidos después de " + intentos + " intentos:");
                System.out.println("  Rojo=" + nombres[0]);
                System.out.println("  Azul=" + nombres[1]);
                System.out.println("  Verde=" + nombres[2]);
                System.out.println("  Amarillo=" + nombres[3]);
            }
            
            VentanaJuego ventanaJuego = new VentanaJuego(controlador, nombres);
            TableroVista tablero = ventanaJuego.getTableroVista();
            controlador.setTableroVista(tablero);
            ventanaJuego.setVisible(true);
            
            Window ventanaCarga = SwingUtilities.getWindowAncestor(this);
            if (ventanaCarga != null) {
                ventanaCarga.dispose();
            }
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PantallaCarga(null, "Abraham", "Partida Épica", 4, 8000, "/vista/recursos/pp.png");
        });
    }
}