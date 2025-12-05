package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.Border;

public class PantallaInicio extends JFrame {

    public PantallaInicio() {

        // Pantalla completa
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        // --- LAYERED PANE ---
        JLayeredPane layers = new JLayeredPane();
        layers.setBounds(0, 0, screenW, screenH);
        setContentPane(layers);

        // -------------------------
        //  FONDO (CAPA 0)
        // -------------------------
        ImageIcon imgFondo = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg"));
        Image fondoEscalado = imgFondo.getImage().getScaledInstance(screenW, screenH, Image.SCALE_SMOOTH);

        JLabel fondo = new JLabel(new ImageIcon(fondoEscalado));
        fondo.setBounds(0, 0, screenW, screenH);

        layers.add(fondo, Integer.valueOf(0));

        // -------------------------
        //  PANEL CENTRAL (CAPA 1)
        // -------------------------
        JPanel panelCentro = new JPanel(null);
        panelCentro.setOpaque(false);

        int w = 450;
        int h = 600;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        panelCentro.setBounds(x, y, w, h);

 
        ImageIcon logoOriginal = new ImageIcon(getClass().getResource("/vista/recursos/logoP.png"));
        Image logoImg = logoOriginal.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);

        JLabel logo = new JLabel(new ImageIcon(logoImg));
        logo.setBounds((w - 300) / 2, 0, 300, 300);
        panelCentro.add(logo);

        // -------- TITULO --------
        JLabel titulo = new JLabel("Parchis Royale");
        titulo.setBounds(0, 310, w, 50);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 38));
        titulo.setForeground(Color.WHITE);
        panelCentro.add(titulo);

        // ===============================
        //   ESTILO PERSONALIZADO BOTONES
        // ===============================
        Color amarillo = new Color(255, 207, 64);
        Color amarilloHover = new Color(255, 225, 110);

        Font fontBoton = new Font("Arial", Font.BOLD, 22);

        JButton btnCrear = new JButton("Crear Sala");
        JButton btnUnirse = new JButton("Unirse");

        JButton[] botones = { btnCrear, btnUnirse };

        int posY = 380;

        for (JButton btn : botones) {
            btn.setBounds((w - 260) / 2, posY, 260, 55);
            btn.setFont(fontBoton);
            btn.setFocusable(false);
            btn.setBackground(amarillo);
            btn.setForeground(Color.BLACK);

            // --- Efecto relieve clásico (3D) ---
            btn.setBorder(BorderFactory.createBevelBorder(
                javax.swing.border.BevelBorder.RAISED,
                Color.WHITE,             // luz arriba
                new Color(200, 150, 0)   // sombra abajo
            ));

            // Mejor interacción visual
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setOpaque(true);
            btn.setContentAreaFilled(true);
            btn.setFocusPainted(false);

            // --- Hover: más claro ---
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    btn.setBackground(amarilloHover);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btn.setBackground(amarillo);
                }
            });

            panelCentro.add(btn);
            posY += 80;
        }

        // ===============================
        //   EVENTOS DE LOS BOTONES
        // ===============================
        
        // Evento: Crear Sala
        btnCrear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ir a SeleccionarJugador en modo CREAR (true)
                RegistrarJugador seleccionar = new RegistrarJugador(true);
                seleccionar.setVisible(true);
                dispose();
            }
        });

        // Evento: Unirse
        btnUnirse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ir a SeleccionarJugador en modo UNIRSE (false)
                RegistrarJugador seleccionar = new RegistrarJugador(false);
                seleccionar.setVisible(true);
                dispose();
            }
        });

        layers.add(panelCentro, Integer.valueOf(1));

        setVisible(true);
    }

    // CLASE PARA BORDES REDONDEADOS
    class RoundedBorder implements Border {
        private int radius;

        RoundedBorder(int r) {
            radius = r;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(radius+1, radius+1, radius+2, radius);
        }

        public boolean isBorderOpaque() {
            return false;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PantallaInicio();
            }
        });
    }
}