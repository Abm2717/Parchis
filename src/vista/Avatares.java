package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import controlador.ClienteControlador;
import controlador.servidor.ServidorCentral;

public class Avatares extends JFrame {

    private JLabel avatar1, avatar2, avatar3, avatar4;
    private JLabel seleccionado = null;
    private String rutaAvatarSeleccionado = null;
    
    // Datos - modo CREAR
    private String nombreJugador;
    private String nombrePartida;
    private int maxJugadores;
    private int puerto;
    
    // Datos - modo UNIRSE
    private String ipServidor;
    private boolean esCreador; // Identifica si es creador o si se une

    /**
     * Constructor para CREAR PARTIDA
     */
    public Avatares(String nombreJugador, String nombrePartida, int maxJugadores, int puerto) {
        this.nombreJugador = nombreJugador;
        this.nombrePartida = nombrePartida;
        this.maxJugadores = maxJugadores;
        this.puerto = puerto;
        this.esCreador = true;
        this.ipServidor = "localhost";
        
        inicializarInterfaz();
    }

    /**
     * Constructor para UNIRSE A PARTIDA
     */
    public Avatares(String nombreJugador, String ipServidor, int puerto) {
        this.nombreJugador = nombreJugador;
        this.ipServidor = ipServidor;
        this.puerto = puerto;
        this.esCreador = false;
        this.nombrePartida = null;
        this.maxJugadores = 0;
        
        inicializarInterfaz();
    }

    private void inicializarInterfaz() {
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

        // Panel central
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(0, 0, 0, 160));

        int w = 600;
        int h = 500;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        panel.setBounds(x, y, w, h);
        layers.add(panel, Integer.valueOf(1));

        Font tituloFont = new Font("Arial", Font.BOLD, 32);
        Color amarillo = new Color(255, 235, 59);

        // Título
        JLabel titulo = new JLabel("Selecciona tu Avatar");
        titulo.setBounds(0, 20, w, 40);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(tituloFont);
        titulo.setForeground(Color.WHITE);
        panel.add(titulo);

        // Rutas de avatares
        String rutaAvatar1 = "/vista/recursos/pp.png";
        String rutaAvatar2 = "/vista/recursos/pp.png";
        String rutaAvatar3 = "/vista/recursos/pp.png";
        String rutaAvatar4 = "/vista/recursos/pp.png";

        // Crear avatares
        avatar1 = crearAvatar(rutaAvatar1);
        avatar2 = crearAvatar(rutaAvatar2);
        avatar3 = crearAvatar(rutaAvatar3);
        avatar4 = crearAvatar(rutaAvatar4);

        int cuadroW = 120;
        int cuadroH = 120;
        int baseX = (w - (cuadroW * 4 + 40 * 3)) / 2;
        int baseY = 120;

        avatar1.setBounds(baseX, baseY, cuadroW, cuadroH);
        avatar2.setBounds(baseX + 160, baseY, cuadroW, cuadroH);
        avatar3.setBounds(baseX + 320, baseY, cuadroW, cuadroH);
        avatar4.setBounds(baseX + 480, baseY, cuadroW, cuadroH);

        panel.add(avatar1);
        panel.add(avatar2);
        panel.add(avatar3);
        panel.add(avatar4);

        // Botón Listo
        JButton btnListo = new JButton("Listo");
        btnListo.setBounds((w - 200) / 2, 380, 200, 50);
        btnListo.setBackground(amarillo);
        btnListo.setFont(new Font("Arial", Font.BOLD, 22));
        btnListo.setFocusPainted(false);
        btnListo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btnListo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (esCreador) {
                    iniciarPartida(); // CREAR servidor + conectar
                } else {
                    unirseAPartida(); // SOLO conectar
                }
            }
        });
        
        panel.add(btnListo);

        setVisible(true);
    }

    private JLabel crearAvatar(String rutaImagen) {
        JLabel lbl = new JLabel();
        
        try {
            java.net.URL imgURL = getClass().getResource(rutaImagen);
            
            if (imgURL != null) {
                ImageIcon img = new ImageIcon(imgURL);
                Image esc = img.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                lbl.setIcon(new ImageIcon(esc));
            } else {
                lbl.setText("?");
                lbl.setFont(new Font("Arial", Font.BOLD, 48));
                lbl.setForeground(Color.WHITE);
            }
        } catch (Exception e) {
            lbl.setText("?");
            lbl.setFont(new Font("Arial", Font.BOLD, 48));
            lbl.setForeground(Color.WHITE);
        }

        lbl.setOpaque(true);
        lbl.setBackground(new Color(0, 0, 0, 120));
        lbl.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarAvatar(lbl, rutaImagen);
            }
        });

        return lbl;
    }

    private void seleccionarAvatar(JLabel avatar, String rutaImagen) {
        if (seleccionado != null) {
            seleccionado.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        }

        avatar.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
        seleccionado = avatar;
        rutaAvatarSeleccionado = rutaImagen;
        
        System.out.println("[AVATAR] Seleccionado: " + rutaImagen);
    }

    /**
     * MODO CREADOR: Inicia servidor y se conecta
     */
    private void iniciarPartida() {
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this,
                "Por favor selecciona un avatar",
                "Avatar requerido",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        System.out.println("[AVATARES] Iniciando proceso de creación de partida...");

        // 1. Iniciar servidor
        Thread hiloServidor = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServidorCentral servidor = new ServidorCentral(puerto);
                    System.out.println("[SERVIDOR] Iniciando en puerto " + puerto);
                    servidor.iniciar();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(Avatares.this,
                                "Error al iniciar servidor: " + ex.getMessage(),
                                "Error del Servidor",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }
        });
        hiloServidor.setDaemon(true);
        hiloServidor.start();

        // 2. Esperar
        System.out.println("[AVATARES] Esperando a que el servidor inicie...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // 3. Conectar como cliente
        conectarYAbrir();
    }

    /**
     * MODO UNIRSE: Solo se conecta al servidor existente
     */
    private void unirseAPartida() {
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this,
                "Por favor selecciona un avatar",
                "Avatar requerido",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        System.out.println("[AVATARES] Uniéndose a partida en " + ipServidor + ":" + puerto);
        
        // Conectar directamente (sin iniciar servidor)
        conectarYAbrir();
    }

    /**
     * Método común: conecta al servidor y abre PantallaCarga
     */
    private void conectarYAbrir() {
        Thread hiloConexion = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[AVATARES] Conectando a " + ipServidor + ":" + puerto);
                    
                    ClienteControlador controlador = new ClienteControlador(null);
                    
                    boolean conectado = controlador.conectar(ipServidor, puerto);
                    
                    if (!conectado) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(Avatares.this,
                                    "No se pudo conectar al servidor",
                                    "Error de Conexión",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }
                    
                    System.out.println("[AVATARES] Conectado exitosamente");
                    Thread.sleep(500);
                    
                    // Registrar jugador
                    System.out.println("[AVATARES] Registrando jugador: " + nombreJugador);
                    boolean registrado = controlador.registrar(nombreJugador);
                    
                    if (!registrado) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(Avatares.this,
                                    "No se pudo registrar el jugador",
                                    "Error de Registro",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }
                    
                    System.out.println("[AVATARES] Jugador registrado");
                    
                    // Unirse a partida
                    System.out.println("[AVATARES] Uniéndose a partida...");
                    controlador.unirseAPartidaDisponible();
                    Thread.sleep(500);
                    
                    // Abrir pantalla de carga
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("[AVATARES] Abriendo pantalla de carga");
                            
                            // SI ES CREADOR: Constructor con toda la info
                            // SI SE UNE: Constructor vacío (sin info)
                            PantallaCarga pantallaCarga;
                            
                            if (esCreador) {
                                pantallaCarga = new PantallaCarga(
                                    controlador,
                                    nombreJugador,
                                    nombrePartida,
                                    maxJugadores,
                                    puerto,
                                    rutaAvatarSeleccionado
                                );
                            } else {
                                // Constructor vacío para quien se une
                                pantallaCarga = new PantallaCarga(controlador);
                            }
                            
                            pantallaCarga.setVisible(true);
                            dispose();
                        }
                    });
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(Avatares.this,
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Prueba CREAR
                new Avatares("Abraham", "Partida Test", 2, 8000);
                
                // Prueba UNIRSE
                // new Avatares("Carlos", "192.168.1.67", 8000);
            }
        });
    }
}