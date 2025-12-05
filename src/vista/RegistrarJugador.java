package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RegistrarJugador extends JFrame {

    private JTextField txtNombre;
    private JLabel avatar1, avatar2, avatar3, avatar4;
    private JLabel seleccionado = null;
    private String rutaAvatarSeleccionado = null;
    private boolean esCreador; // true = Crear Partida, false = Unirse

    /**
     * Constructor
     * @param esCreador true si viene de "Crear Sala", false si viene de "Unirse"
     */
    public RegistrarJugador(boolean esCreador) {
        this.esCreador = esCreador;

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        // LayeredPane
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
        int h = 580;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        panel.setBounds(x, y, w, h);
        layers.add(panel, Integer.valueOf(1));

        // ====== ESTILOS ======
        Font tituloFont = new Font("Arial", Font.BOLD, 32);
        Font labelFont = new Font("Arial", Font.BOLD, 22);
        Font inputFont = new Font("Arial", Font.PLAIN, 20);

        Color grisClaro = new Color(220, 220, 220);
        Color amarillo = new Color(255, 207, 64);
        Color amarilloHover = new Color(255, 225, 110);

        // ====== TÍTULO ======
        JLabel titulo = new JLabel("Configura tu Jugador");
        titulo.setBounds(0, 20, w, 40);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(tituloFont);
        titulo.setForeground(Color.WHITE);
        panel.add(titulo);

        // ====== NOMBRE ======
        JLabel lblNombre = new JLabel("Tu Nombre:");
        lblNombre.setFont(labelFont);
        lblNombre.setForeground(grisClaro);
        lblNombre.setBounds(40, 80, 200, 30);
        panel.add(lblNombre);

        txtNombre = new JTextField();
        txtNombre.setBounds(40, 115, 520, 40);
        txtNombre.setBackground(new Color(0, 0, 0, 100));
        txtNombre.setForeground(Color.WHITE);
        txtNombre.setFont(inputFont);
        txtNombre.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2));
        panel.add(txtNombre);

        // ====== AVATARES ======
        JLabel lblAvatar = new JLabel("Selecciona tu Avatar:");
        lblAvatar.setFont(labelFont);
        lblAvatar.setForeground(grisClaro);
        lblAvatar.setBounds(40, 175, 300, 30);
        panel.add(lblAvatar);

        // Rutas de avatares (puedes cambiarlas)
        String rutaAvatar1 = "/vista/recursos/tung.jpg";
        String rutaAvatar2 = "/vista/recursos/boneca.jpg";
        String rutaAvatar3 = "/vista/recursos/banana.jpg";
        String rutaAvatar4 = "/vista/recursos/brbr.jpg";

        avatar1 = crearAvatar(rutaAvatar1);
        avatar2 = crearAvatar(rutaAvatar2);
        avatar3 = crearAvatar(rutaAvatar3);
        avatar4 = crearAvatar(rutaAvatar4);

        int cuadroW = 100;
        int cuadroH = 100;
        int baseX = (w - (cuadroW * 4 + 30 * 3)) / 2;
        int baseY = 220;

        avatar1.setBounds(baseX, baseY, cuadroW, cuadroH);
        avatar2.setBounds(baseX + 130, baseY, cuadroW, cuadroH);
        avatar3.setBounds(baseX + 260, baseY, cuadroW, cuadroH);
        avatar4.setBounds(baseX + 390, baseY, cuadroW, cuadroH);

        panel.add(avatar1);
        panel.add(avatar2);
        panel.add(avatar3);
        panel.add(avatar4);

        // ====== BOTONES ======
        int btnY = 360;

        // Botón Volver
        JButton btnVolver = new JButton("Volver");
        btnVolver.setBounds(100, btnY, 180, 50);
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
                PantallaInicio inicio = new PantallaInicio();
                inicio.setVisible(true);
                dispose();
            }
        });

        panel.add(btnVolver);

        // Botón Siguiente
        JButton btnSiguiente = new JButton("Siguiente");
        btnSiguiente.setBounds(320, btnY, 180, 50);
        btnSiguiente.setBackground(amarillo);
        btnSiguiente.setForeground(Color.BLACK);
        btnSiguiente.setFont(new Font("Arial", Font.BOLD, 20));
        btnSiguiente.setFocusPainted(false);
        btnSiguiente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSiguiente.setBorder(BorderFactory.createBevelBorder(
                javax.swing.border.BevelBorder.RAISED,
                Color.WHITE,
                new Color(200, 150, 0)
        ));

        btnSiguiente.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSiguiente.setBackground(amarilloHover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSiguiente.setBackground(amarillo);
            }
        });

        btnSiguiente.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validarYContinuar();
            }
        });

        panel.add(btnSiguiente);

        setVisible(true);
    }

    private JLabel crearAvatar(String rutaImagen) {
        JLabel lbl = new JLabel();

        try {
            java.net.URL imgURL = getClass().getResource(rutaImagen);

            if (imgURL != null) {
                ImageIcon img = new ImageIcon(imgURL);
                Image esc = img.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
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
        // Deseleccionar anterior
        if (seleccionado != null) {
            seleccionado.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        }

        // Seleccionar nuevo
        avatar.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
        seleccionado = avatar;
        rutaAvatarSeleccionado = rutaImagen;

        System.out.println("[AVATAR] Seleccionado: " + rutaImagen);
    }

    private void validarYContinuar() {
        String nombre = txtNombre.getText().trim();

        // Validar nombre
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa tu nombre",
                    "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (nombre.length() < 3) {
            JOptionPane.showMessageDialog(this,
                    "El nombre debe tener al menos 3 caracteres",
                    "Nombre muy corto",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar avatar
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Por favor selecciona un avatar",
                    "Avatar requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Continuar según el modo
        if (esCreador) {
            // Ir a CrearPartida
            CrearPartida crearPartida = new CrearPartida(nombre, rutaAvatarSeleccionado);
            crearPartida.setVisible(true);
            dispose();
        } else {
            // Ir a PantallaUnirse
            PantallaUnirse pantallaUnirse = new PantallaUnirse(nombre, rutaAvatarSeleccionado);
            pantallaUnirse.setVisible(true);
            dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Prueba modo CREAR
                new RegistrarJugador(true);

                // Prueba modo UNIRSE
                // new SeleccionarJugador(false);
            }
        });
    }
}