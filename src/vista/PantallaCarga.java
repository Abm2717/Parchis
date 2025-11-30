package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import controlador.ClienteControlador;

public class PantallaCarga extends JFrame {
    
    private ClienteControlador controlador;
    private JLabel textoEspera;
    private JPanel panelInfo;
    private JLabel lblJugadores;
    private int maxJugadores;
    private JButton btnListo; // ✅ NUEVO

    /**
     * Constructor VACÍO - Para jugadores que se unen
     */
    public PantallaCarga() {
        inicializarPantalla(false, null, 0, 0, 0);
    }
    
    /**
    * ✅ NUEVO: Constructor SOLO CON CONTROLADOR - Para jugadores que se unen
    */
     public PantallaCarga(ClienteControlador controlador) {
       this.controlador = controlador;
       this.maxJugadores = 0;

       if (controlador != null) {
           controlador.setVistaCarga(this);
       }

       inicializarPantalla(false, null, 0, 0, 0);
    }

    /**
     * Constructor COMPLETO - Para creador
     */
    public PantallaCarga(ClienteControlador controlador, String nombreJugador, 
                         String nombrePartida, int maxJugadores, int puerto, String rutaAvatar) {
        this.controlador = controlador;
        this.maxJugadores = maxJugadores;
        
        if (controlador != null) {
            controlador.setVistaCarga(this);
        }
        
        inicializarPantalla(true, nombrePartida, maxJugadores, puerto, 1);
    }

    private void inicializarPantalla(boolean esCreador, String nombrePartida, 
                                      Integer maxJugadores, Integer puerto, Integer jugadoresActuales) {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        JLayeredPane layers = new JLayeredPane();
        layers.setBounds(0, 0, screenW, screenH);
        setContentPane(layers);

        // Fondo
        ImageIcon imgFondo = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg"));
        Image fondoEscalado = imgFondo.getImage().getScaledInstance(screenW, screenH, Image.SCALE_SMOOTH);
        
        JLabel fondo = new JLabel(new ImageIcon(fondoEscalado));
        fondo.setBounds(0, 0, screenW, screenH);
        layers.add(fondo, Integer.valueOf(0));

        // Panel translúcido
        JPanel contenedor = new JPanel(null);
        contenedor.setOpaque(false);
        
        int w = 600;
        int h = esCreador ? 600 : 400;
        contenedor.setBounds((screenW - w) / 2, (screenH - h) / 2, w, h);
        layers.add(contenedor, Integer.valueOf(1));

        // GIF
        ImageIcon gifOriginal = new ImageIcon(getClass().getResource("/vista/recursos/loading.gif"));
        int nuevoAncho = 190;
        int nuevoAlto = (gifOriginal.getIconHeight() * nuevoAncho) / gifOriginal.getIconWidth();
        Image gifEscalado = gifOriginal.getImage().getScaledInstance(nuevoAncho, nuevoAlto, Image.SCALE_DEFAULT);
        ImageIcon gifFinal = new ImageIcon(gifEscalado);
        
        JLabel gif = new JLabel(gifFinal);
        gif.setBounds((w - nuevoAncho) / 2, 40, nuevoAncho, nuevoAlto);
        contenedor.add(gif);

        // Texto de espera
        textoEspera = new JLabel("Esperando jugadores...");
        textoEspera.setHorizontalAlignment(SwingConstants.CENTER);
        textoEspera.setForeground(Color.WHITE);
        textoEspera.setFont(new Font("Arial", Font.BOLD, 28));
        textoEspera.setBounds(0, 250, w, 50);
        contenedor.add(textoEspera);

        // ✅ BOTÓN LISTO
        Color amarillo = new Color(255, 235, 59);
        Color amarilloHover = new Color(255, 245, 120);
        
        btnListo = new JButton("Listo");
        btnListo.setBounds((w - 200) / 2, 310, 200, 50);
        btnListo.setBackground(amarillo);
        btnListo.setForeground(Color.BLACK);
        btnListo.setFont(new Font("Arial", Font.BOLD, 22));
        btnListo.setFocusPainted(false);
        btnListo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btnListo.setBorder(BorderFactory.createBevelBorder(
            javax.swing.border.BevelBorder.RAISED,
            Color.WHITE,
            new Color(200, 150, 0)
        ));
        
        // Hover
        btnListo.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnListo.setBackground(amarilloHover);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnListo.setBackground(amarillo);
            }
        });
        
        // ✅ EVENTO: Marcar listo
        btnListo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                marcarListo();
            }
        });
        
        contenedor.add(btnListo);

        // Panel de información (solo para creador)
        if (esCreador) {
            panelInfo = new JPanel(null);
            panelInfo.setBackground(new Color(0, 0, 0, 160));
            panelInfo.setBounds(50, 380, w - 100, 250); // ✅ Movido más abajo
            panelInfo.setBorder(BorderFactory.createLineBorder(new Color(255, 207, 64), 3));
            
            Font fontTitulo = new Font("Arial", Font.BOLD, 24);
            Font fontInfo = new Font("Arial", Font.PLAIN, 20);
            Color grisClaro = new Color(220, 220, 220);
            Color amarilloInfo = new Color(255, 207, 64);

            // Título
            JLabel lblTitulo = new JLabel("Información de la Partida");
            lblTitulo.setBounds(0, 10, w - 100, 35);
            lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
            lblTitulo.setFont(fontTitulo);
            lblTitulo.setForeground(amarilloInfo);
            panelInfo.add(lblTitulo);

            String ipLocal = obtenerIPLocal();

            int yPos = 60;
            int espaciado = 40;

            // Nombre de la partida
            JLabel lblNombre = new JLabel("Partida: " + nombrePartida);
            lblNombre.setBounds(30, yPos, 440, 30);
            lblNombre.setFont(fontInfo);
            lblNombre.setForeground(grisClaro);
            panelInfo.add(lblNombre);
            yPos += espaciado;

            // Jugadores
            lblJugadores = new JLabel("Jugadores: " + jugadoresActuales + " / " + maxJugadores);
            lblJugadores.setBounds(30, yPos, 440, 30);
            lblJugadores.setFont(fontInfo);
            lblJugadores.setForeground(grisClaro);
            panelInfo.add(lblJugadores);
            yPos += espaciado;

            // IP
            JLabel lblIP = new JLabel("IP: " + ipLocal);
            lblIP.setBounds(30, yPos, 440, 30);
            lblIP.setFont(fontInfo);
            lblIP.setForeground(grisClaro);
            panelInfo.add(lblIP);
            yPos += espaciado;

            // Puerto
            JLabel lblPuerto = new JLabel("Puerto: " + puerto);
            lblPuerto.setBounds(30, yPos, 440, 30);
            lblPuerto.setFont(fontInfo);
            lblPuerto.setForeground(grisClaro);
            panelInfo.add(lblPuerto);

            contenedor.add(panelInfo);
        }

        setVisible(true);
    }

    private String obtenerIPLocal() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            return ip.getHostAddress();
        } catch (UnknownHostException e) {
            return "No disponible";
        }
    }

    /**
     * ✅ NUEVO: Marca al jugador como listo
     */
    private void marcarListo() {
        if (controlador == null) {
            System.err.println("[ERROR] No hay controlador conectado");
            return;
        }
        
        System.out.println("[PANTALLA CARGA] Marcando como listo...");
        
        // Llamar al controlador para marcar listo
        boolean exito = controlador.marcarListo();
        
        if (exito) {
            // Deshabilitar el botón
            btnListo.setEnabled(false);
            btnListo.setText("Esperando...");
            btnListo.setBackground(new Color(150, 150, 150));
            
            // Actualizar texto
            actualizarTextoEspera("Esperando a otros jugadores...");
            
            System.out.println("[PANTALLA CARGA] Marcado como listo exitosamente");
        } else {
            System.err.println("[PANTALLA CARGA] Error al marcar listo");
        }
    }

    /**
     * Actualiza el contador de jugadores
     */
    public void actualizarJugadores(int jugadoresActuales) {
        if (lblJugadores != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    lblJugadores.setText("Jugadores: " + jugadoresActuales + " / " + maxJugadores);
                    System.out.println("[PANTALLA CARGA] Actualizado UI: " + jugadoresActuales + "/" + maxJugadores);
                }
            });
        }
    }

    public void actualizarTextoEspera(String nuevoTexto) {
        if (textoEspera != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    textoEspera.setText(nuevoTexto);
                }
            });
        }
    }

    /**
     * ✅ Inicia la partida
     * - Espera 2 segundos
     * - Abre VentanaJuego
     * - Cierra esta ventana
     */
    public void iniciarPartida() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Cambiar texto
                actualizarTextoEspera("¡Iniciando partida!");
                
                // Deshabilitar botón si existe
                if (btnListo != null) {
                    btnListo.setEnabled(false);
                }
                
                // Esperar 2 segundos y abrir el juego
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            System.out.println("[PANTALLA CARGA] Iniciando partida en 2 segundos...");
                            Thread.sleep(2000);
                            
                            // Abrir ventana del juego
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("[PANTALLA CARGA] Abriendo tablero de juego");
                                    TableroVista tableroVista = new TableroVista();
                                    tableroVista.setVisible(true);
                                    
                                    // Cerrar pantalla de carga
                                    dispose();
                                }
                            });
                            
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PantallaCarga(null, "Abraham", "Partida Epica", 2, 8000, null);
            }
        });
    }
}