package vista;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import controlador.ClienteControlador;

public class PantallaCarga extends JFrame {
    
    private ClienteControlador controlador;
    private JLabel textoEspera;
    private JPanel panelInfo;
    private JLabel lblJugadores;
    private int maxJugadores;

    /**
     * Constructor VACÍO - Para jugadores que se unen
     */
    public PantallaCarga() {
        inicializarPantalla(false, null, 0, 0, 0);
    }

    /**
     * Constructor COMPLETO - Para creador y jugadores que se unen
     */
    public PantallaCarga(ClienteControlador controlador, String nombreJugador, 
                         String nombrePartida, int maxJugadores, int puerto, String rutaAvatar) {
        this.controlador = controlador;
        this.maxJugadores = maxJugadores;
        
        // ✅ CONECTAR LA VISTA AL CONTROLADOR
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

        // Panel de información (solo para quien ve la info)
        if (esCreador) {
            panelInfo = new JPanel(null);
            panelInfo.setBackground(new Color(0, 0, 0, 160));
            panelInfo.setBounds(50, 320, w - 100, 250);
            panelInfo.setBorder(BorderFactory.createLineBorder(new Color(255, 207, 64), 3));
            
            Font fontTitulo = new Font("Arial", Font.BOLD, 24);
            Font fontInfo = new Font("Arial", Font.PLAIN, 20);
            Color grisClaro = new Color(220, 220, 220);
            Color amarillo = new Color(255, 207, 64);

            // Título
            JLabel lblTitulo = new JLabel("Información de la Partida");
            lblTitulo.setBounds(0, 10, w - 100, 35);
            lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
            lblTitulo.setFont(fontTitulo);
            lblTitulo.setForeground(amarillo);
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

            // ✅ Jugadores - GUARDAR REFERENCIA
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
     * ✅ MÉTODO PÚBLICO: Actualiza el contador de jugadores
     * Llamado desde ClienteControlador cuando llega "jugador_unido"
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PantallaCarga(null, "Abraham", "Partida Epica", 2, 8000, null);
            }
        });
    }
}