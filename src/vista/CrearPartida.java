package vista;

import controlador.ClienteControlador;
import controlador.servidor.ServidorCentral;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class CrearPartida extends JFrame {
    
    private JTextField txtNombrePartida;
    private JComboBox<String> comboJugadores;
    private JTextField txtPuerto;
    
    // Datos del jugador
    private String nombreJugador;
    private String rutaAvatar;

    /**
     * Constructor que recibe nombre y avatar del jugador
     */
    public CrearPartida(String nombreJugador, String rutaAvatar) {
        this.nombreJugador = nombreJugador;
        this.rutaAvatar = rutaAvatar;

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        // LAYERS
        JLayeredPane layers = new JLayeredPane();
        layers.setBounds(0, 0, screenW, screenH);
        setContentPane(layers);

        // Fondo
        ImageIcon imgFondo = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg"));
        Image fondoEscalado = imgFondo.getImage().getScaledInstance(screenW, screenH, Image.SCALE_SMOOTH);

        JLabel fondo = new JLabel(new ImageIcon(fondoEscalado));
        fondo.setBounds(0, 0, screenW, screenH);
        layers.add(fondo, Integer.valueOf(0));

        // Panel central translúcido
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(0, 0, 0, 160));

        int w = 480;
        int h = 520;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        panel.setBounds(x, y, w, h);
        layers.add(panel, Integer.valueOf(1));

        //------------------------------
        // ESTILOS
        //------------------------------
        Font labelFont = new Font("Arial", Font.BOLD, 22);
        Font inputFont = new Font("Arial", Font.PLAIN, 20);

        Color grisClaro = new Color(220, 220, 220);
        Color amarillo = new Color(255, 207, 64);
        Color amarilloHover = new Color(255, 225, 110);

        //------------------------------
        // TITULO
        //------------------------------
        JLabel titulo = new JLabel("Crear Partida");
        titulo.setBounds(0, 20, w, 45);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 34));
        titulo.setForeground(Color.WHITE);
        panel.add(titulo);

        //------------------------------
        // MOSTRAR NOMBRE DEL JUGADOR
        //------------------------------
        JLabel lblJugador = new JLabel("Jugador: " + nombreJugador);
        lblJugador.setFont(new Font("Arial", Font.BOLD, 18));
        lblJugador.setForeground(new Color(255, 235, 59));
        lblJugador.setBounds(40, 75, 400, 25);
        panel.add(lblJugador);

        //------------------------------
        // CAMPOS
        //------------------------------

        // Nombre partida
        JLabel lblNombrePartida = new JLabel("Nombre Partida:");
        lblNombrePartida.setFont(labelFont);
        lblNombrePartida.setForeground(grisClaro);
        lblNombrePartida.setBounds(40, 115, 250, 30);
        panel.add(lblNombrePartida);

        txtNombrePartida = new JTextField("Partida de " + nombreJugador);
        txtNombrePartida.setBounds(40, 150, 400, 40);
        txtNombrePartida.setBackground(new Color(0, 0, 0, 100));
        txtNombrePartida.setForeground(Color.WHITE);
        txtNombrePartida.setFont(inputFont);
        txtNombrePartida.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(txtNombrePartida);

        // Jugadores
        JLabel lblJugadores = new JLabel("Jugadores:");
        lblJugadores.setFont(labelFont);
        lblJugadores.setForeground(grisClaro);
        lblJugadores.setBounds(40, 205, 200, 30);
        panel.add(lblJugadores);

        String[] numJugadores = {"2", "3", "4"};
        comboJugadores = new JComboBox<>(numJugadores);
        comboJugadores.setBounds(200, 205, 80, 35);
        comboJugadores.setBackground(new Color(0, 0, 0, 120));
        comboJugadores.setForeground(Color.WHITE);
        comboJugadores.setFont(inputFont);
        comboJugadores.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(comboJugadores);

        // Puerto
        JLabel lblPuerto = new JLabel("Puerto Servidor:");
        lblPuerto.setFont(labelFont);
        lblPuerto.setForeground(grisClaro);
        lblPuerto.setBounds(40, 255, 250, 30);
        panel.add(lblPuerto);

        txtPuerto = new JTextField("8000");
        txtPuerto.setBounds(40, 290, 150, 40);
        txtPuerto.setBackground(new Color(0, 0, 0, 100));
        txtPuerto.setForeground(Color.WHITE);
        txtPuerto.setFont(inputFont);
        txtPuerto.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(txtPuerto);

        //------------------------------
        // BOTONES
        //------------------------------
        int btnY = 360;

        // Botón Volver
        JButton btnVolver = new JButton("Volver");
        btnVolver.setBounds(60, btnY, 160, 50);
        btnVolver.setBackground(new Color(200, 200, 200));
        btnVolver.setForeground(Color.BLACK);
        btnVolver.setFont(new Font("Arial", Font.BOLD, 20));
        btnVolver.setFocusPainted(false);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
                RegistrarJugador seleccionar = new RegistrarJugador(true);
                seleccionar.setVisible(true);
                dispose();
            }
        });

        panel.add(btnVolver);

        // Botón Crear
        JButton btnCrear = new JButton("Crear");
        btnCrear.setBounds(260, btnY, 160, 50);
        btnCrear.setBackground(amarillo);
        btnCrear.setForeground(Color.BLACK);
        btnCrear.setFont(new Font("Arial", Font.BOLD, 22));
        btnCrear.setFocusPainted(false);
        btnCrear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCrear.setBorder(BorderFactory.createBevelBorder(
                javax.swing.border.BevelBorder.RAISED,
                Color.WHITE,
                new Color(200, 150, 0)
        ));

        btnCrear.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnCrear.setBackground(amarilloHover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnCrear.setBackground(amarillo);
            }
        });

        btnCrear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                crearYUnirseAPartida();
            }
        });

        panel.add(btnCrear);

        setVisible(true);
    }

    /**
     * Valida los datos y crea la partida
     */
    private void crearYUnirseAPartida() {
        String nombrePartida = txtNombrePartida.getText().trim();
        String puertoStr = txtPuerto.getText().trim();

        // Validar nombre de partida
        if (nombrePartida.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa el nombre de la partida",
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

        int maxJugadores = Integer.parseInt((String) comboJugadores.getSelectedItem());

        // Desactivar botón para evitar doble click
        Component[] components = ((JPanel) getContentPane().getComponent(0)).getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton && ((JButton) comp).getText().equals("Crear")) {
                comp.setEnabled(false);
                ((JButton) comp).setText("Creando...");
            }
        }

        // Iniciar servidor en hilo separado
        System.out.println("[CrearPartida] Iniciando servidor...");

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
                            JOptionPane.showMessageDialog(CrearPartida.this,
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

        // Esperar a que servidor inicie
        System.out.println("[CrearPartida] Esperando a que el servidor inicie...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Conectar como cliente
        conectarYAbrir(nombrePartida, maxJugadores, puerto);
    }

    /**
     * Conecta al servidor y abre PantallaCarga
     */
    private void conectarYAbrir(String nombrePartida, int maxJugadores, int puerto) {
        Thread hiloConexion = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[CrearPartida] Conectando a localhost:" + puerto);

                    ClienteControlador controlador = new ClienteControlador(null);

                    boolean conectado = controlador.conectar("localhost", puerto);

                    if (!conectado) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(CrearPartida.this,
                                        "No se pudo conectar al servidor",
                                        "Error de Conexión",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }

                    System.out.println("[CrearPartida] Conectado exitosamente");
                    Thread.sleep(500);

                    // Registrar jugador
                    System.out.println("[CrearPartida] Registrando jugador: " + nombreJugador);
                    boolean registrado = controlador.registrar(nombreJugador);

                    if (!registrado) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(CrearPartida.this,
                                        "No se pudo registrar el jugador",
                                        "Error de Registro",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return;
                    }

                    System.out.println("[CrearPartida] Jugador registrado");

                    // Unirse a partida
                    System.out.println("[CrearPartida] Uniéndose a partida...");
                    controlador.unirseAPartidaDisponible();
                    Thread.sleep(500);

                    // Abrir pantalla de carga
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("[CrearPartida] Abriendo pantalla de carga");

                            PantallaCarga pantallaCarga = new PantallaCarga(
                                    controlador,
                                    nombreJugador,
                                    nombrePartida,
                                    maxJugadores,
                                    puerto,
                                    rutaAvatar
                            );

                            pantallaCarga.setVisible(true);
                            dispose();
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(CrearPartida.this,
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
                new CrearPartida("Abraham", "/vista/recursos/pp.png");
            }
        });
    }
}