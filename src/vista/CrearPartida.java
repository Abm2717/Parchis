package vista;

import controlador.ClienteControlador;
import controlador.servidor.ServidorCentral;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class CrearPartida extends JFrame {
    
    private JTextField txtNombreJugador;
    private JTextField txtNombrePartida;
    private JComboBox<String> comboJugadores;
    private JTextField txtPuerto;

    public CrearPartida() {

        // Pantalla completa
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
        panel.setBackground(new Color(0, 0, 0, 160)); // un poco más suave

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
        Color negroTrans = new Color(0, 0, 0, 140);

        // Botón estilo PantallaInicio
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
        // CAMPOS
        //------------------------------

        // Nombre jugador
        JLabel lblNombreJugador = new JLabel("Tu Nombre:");
        lblNombreJugador.setFont(labelFont);
        lblNombreJugador.setForeground(grisClaro);
        lblNombreJugador.setBounds(40, 85, 200, 30);
        panel.add(lblNombreJugador);

        txtNombreJugador = new JTextField();
        txtNombreJugador.setBounds(40, 120, 400, 40);
        txtNombreJugador.setBackground(new Color(0, 0, 0, 100));
        txtNombreJugador.setForeground(Color.WHITE);
        txtNombreJugador.setFont(inputFont);
        txtNombreJugador.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(txtNombreJugador);

        // Nombre partida
        JLabel lblNombrePartida = new JLabel("Nombre Partida:");
        lblNombrePartida.setFont(labelFont);
        lblNombrePartida.setForeground(grisClaro);
        lblNombrePartida.setBounds(40, 170, 250, 30);
        panel.add(lblNombrePartida);

        txtNombrePartida = new JTextField();
        txtNombrePartida.setBounds(40, 205, 400, 40);
        txtNombrePartida.setBackground(new Color(0, 0, 0, 100));
        txtNombrePartida.setForeground(Color.WHITE);
        txtNombrePartida.setFont(inputFont);
        txtNombrePartida.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(txtNombrePartida);

        // Jugadores
        JLabel lblJugadores = new JLabel("Jugadores:");
        lblJugadores.setFont(labelFont);
        lblJugadores.setForeground(grisClaro);
        lblJugadores.setBounds(40, 255, 200, 30);
        panel.add(lblJugadores);

        String[] numJugadores = {"2", "3", "4"};
        comboJugadores = new JComboBox<>(numJugadores);
        comboJugadores.setBounds(200, 255, 80, 35);
        comboJugadores.setBackground(new Color(0, 0, 0, 120));
        comboJugadores.setForeground(Color.WHITE);
        comboJugadores.setFont(inputFont);
        comboJugadores.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(comboJugadores);

        // Puerto
        JLabel lblPuerto = new JLabel("Puerto Servidor:");
        lblPuerto.setFont(labelFont);
        lblPuerto.setForeground(grisClaro);
        lblPuerto.setBounds(40, 305, 250, 30);
        panel.add(lblPuerto);

        txtPuerto = new JTextField("8000");
        txtPuerto.setBounds(40, 340, 150, 40);
        txtPuerto.setBackground(new Color(0, 0, 0, 100));
        txtPuerto.setForeground(Color.WHITE);
        txtPuerto.setFont(inputFont);
        txtPuerto.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(txtPuerto);

        //------------------------------
        // BOTÓN ACEPTAR ESTILO "PANTALLA INICIO"
        //------------------------------
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBounds(140, 410, 200, 50);
        btnAceptar.setBackground(amarillo);
        btnAceptar.setForeground(Color.BLACK);
        btnAceptar.setFont(new Font("Arial", Font.BOLD, 24));
        btnAceptar.setFocusPainted(false);
        btnAceptar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Borde con relieve 3D
        btnAceptar.setBorder(BorderFactory.createBevelBorder(
                javax.swing.border.BevelBorder.RAISED,
                Color.WHITE,              // luz
                new Color(200, 150, 0)    // sombra
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

        // ===============================
        //   EVENTO DEL BOTÓN ACEPTAR
        // ===============================
        btnAceptar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                crearYUnirseAPartida();
            }
        });

        panel.add(btnAceptar);

        setVisible(true);
    }

    /**
    * Abre la pantalla de selección de avatares
    */
   private void crearYUnirseAPartida() {
       // 1. VALIDAR CAMPOS
       String nombreJugador = txtNombreJugador.getText().trim();
       String nombrePartida = txtNombrePartida.getText().trim();
       String puertoStr = txtPuerto.getText().trim();

       if (nombreJugador.isEmpty()) {
           JOptionPane.showMessageDialog(this, 
               "Por favor ingresa tu nombre", 
               "Campo requerido", 
               JOptionPane.WARNING_MESSAGE);
           return;
       }

       if (nombrePartida.isEmpty()) {
           JOptionPane.showMessageDialog(this, 
               "Por favor ingresa el nombre de la partida", 
               "Campo requerido", 
               JOptionPane.WARNING_MESSAGE);
           return;
       }

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

       // 2. ABRIR PANTALLA DE AVATARES (pasando los datos)
       Avatares avatares = new Avatares(nombreJugador, nombrePartida, maxJugadores, puerto);
       avatares.setVisible(true);

       // 3. CERRAR ESTA VENTANA
       dispose();
   }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CrearPartida();
            }
        });
    }
}