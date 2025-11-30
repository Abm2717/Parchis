package vista;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class TableroVista extends JPanel {

    private Image fondo, tablero;
    private Image fichaRoja, fichaAzul, fichaVerde, fichaAmarilla;
    private Image perfilRojo, perfilAzul, perfilVerde, perfilAmarillo;

    private ImageIcon[] caras;
    private JLabel dado1Label, dado2Label;
    private int baseX1 = 20, baseY1 = 20;
    private int baseX2 = 80, baseY2 = 20;
    private final Random random = new Random();

    private JButton btnSalir;

    public TableroVista() {

        setLayout(null);

        /** --- IMÁGENES --- */
        fondo = new ImageIcon(getClass().getResource("/vista/recursos/fondo.jpg")).getImage();
        tablero = new ImageIcon(getClass().getResource("/vista/recursos/TAB.png")).getImage();

        fichaRoja = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FR.png")).getImage();
        fichaAzul = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FAZ.png")).getImage();
        fichaVerde = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FV.png")).getImage();
        fichaAmarilla = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FA.png")).getImage();

        perfilRojo = new ImageIcon(getClass().getResource("/vista/recursos/pp.png")).getImage();
        perfilAzul = new ImageIcon(getClass().getResource("/vista/recursos/pp.png")).getImage();
        perfilVerde = new ImageIcon(getClass().getResource("/vista/recursos/pp.png")).getImage();
        perfilAmarillo = new ImageIcon(getClass().getResource("/vista/recursos/pp.png")).getImage();


        /** -----------------------------------------------------
         *    BOTÓN SALIR
         ----------------------------------------------------- */
        btnSalir = new JButton("Salir");
        btnSalir.setBounds(20, 20, 90, 35);

        btnSalir.setBackground(new Color(0, 0, 0, 170));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFocusPainted(false);
        btnSalir.setFont(new Font("Arial", Font.BOLD, 14));

        btnSalir.addActionListener(e -> System.exit(0));

        add(btnSalir);


        /** -----------------------------------------------------
        *   PANEL DADOS
        ----------------------------------------------------- */
       JPanel panelNegro = new JPanel(null);

       // Tamaño del panel
       int panelW = 200;
       int panelH = 260;

       // Posición inicial (izquierda y centrado)
       int xPanel = 30;
       int yPanel = (getHeight() - panelH) / 2;

       panelNegro.setBounds(xPanel, yPanel, panelW, panelH);
       panelNegro.setBackground(new Color(0, 0, 0, 150));
       add(panelNegro);

       // Dados
       caras = new ImageIcon[6];
       for (int i = 0; i < 6; i++) {
           ImageIcon iconOriginal = new ImageIcon(getClass().getResource("/vista/recursos/DADOS_D" + (i + 1) + ".png"));
           Image img = iconOriginal.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
           caras[i] = new ImageIcon(img);
       }

        dado1Label = new JLabel(caras[0]);
        dado2Label = new JLabel(caras[0]);

        dado1Label.setBounds(20, 20, 80, 80);
        dado2Label.setBounds(100, 20, 80, 80);

        panelNegro.add(dado1Label);
        panelNegro.add(dado2Label);

        /** -----------------------------------------------------
         *   BOTÓN TIRAR
         ----------------------------------------------------- */
        JButton btnTirar = new JButton("Tirar");
        btnTirar.setBounds(50, 170, 100, 40);
        btnTirar.setBackground(new Color(30, 30, 30));
        btnTirar.setForeground(Color.WHITE);
        btnTirar.setFocusPainted(false);

        // Acción del botón
        btnTirar.addActionListener(e -> {
            int d1 = random.nextInt(6);
            int d2 = random.nextInt(6);

            dado1Label.setIcon(caras[d1]);
            dado2Label.setIcon(caras[d2]);
        });

        panelNegro.add(btnTirar);

        /** -----------------------------------------------------
         *   Reacomodar el panel al cambiar tamaño
         ----------------------------------------------------- */
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int y = (getHeight() - panelH) / 2;
                panelNegro.setBounds(30, y, panelW, panelH);
            }
        });


        /** COORDENADAS */
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int tabW = 700;
                int tabH = 700;

                int xTab = (getWidth() - tabW) / 2;
                int yTab = (getHeight() - tabH) / 2;

                int relX = e.getX() - xTab;
                int relY = e.getY() - yTab;

                System.out.println("TABLERO → X=" + relX + " Y=" + relY);
            }
        });

        setOpaque(false);
    }


    /** -----------------------------------------------------
     *     DIBUJAR TABLERO
     ----------------------------------------------------- */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);

        int tabW = 700;
        int tabH = 700;
        int x = (getWidth() - tabW) / 2;
        int y = (getHeight() - tabH) / 2;

        g.drawImage(tablero, x, y, tabW, tabH, this);

        // nombres
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Jugador 1", x - 120, y + 40);
        g.drawString("Jugador 2", x + tabW + 10, y + 40);
        g.drawString("Jugador 3", x - 130, y + tabH - 165);
        g.drawString("Jugador 4", x + tabW + 10, y + tabH - 165);

        // perfiles
        g.drawImage(perfilRojo, x - 120, y + 50, 100, 100, this);
        g.drawImage(perfilAzul, x + tabW + 20, y + 50, 100, 100, this);
        g.drawImage(perfilVerde, x - 120, y + tabH - 150, 100, 100, this);
        g.drawImage(perfilAmarillo, x + tabW + 20, y + tabH - 150, 100, 100, this);

        // fichas
        //g.drawImage(fichaRoja, x + 75, y + 510, 60, 60, this);
        g.drawImage(fichaRoja, x + 130, y + 425, 60, 60, this);
        g.drawImage(fichaRoja, x + 145, y + 510, 60, 60, this);
        g.drawImage(fichaRoja, x + 75, y + 570, 60, 60, this);
        g.drawImage(fichaRoja, x + 145, y + 570, 60, 60, this);

        g.drawImage(fichaAzul, x + 80, y + 85, 60, 60, this);
        g.drawImage(fichaAzul, x + 150, y + 85, 60, 60, this);
        g.drawImage(fichaAzul, x + 80, y + 145, 60, 60, this);
        g.drawImage(fichaAzul, x + 150, y + 145, 60, 60, this);

        g.drawImage(fichaVerde, x + 510, y + 510, 60, 60, this);
        g.drawImage(fichaVerde, x + 580, y + 510, 60, 60, this);
        g.drawImage(fichaVerde, x + 510, y + 570, 60, 60, this);
        g.drawImage(fichaVerde, x + 580, y + 570, 60, 60, this);

        g.drawImage(fichaAmarilla, x + 500, y + 76, 60, 60, this);
        g.drawImage(fichaAmarilla, x + 576, y + 76, 60, 60, this);
        g.drawImage(fichaAmarilla, x + 500, y + 150, 60, 60, this);
        g.drawImage(fichaAmarilla, x + 576, y + 150, 60, 60, this);
    }

    /** -----------------------------------------------------
     *     MÉTODO MAIN PARA PRUEBAS
     ----------------------------------------------------- */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Parchís - Tablero");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                // Pantalla completa
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                frame.setSize(screen.width, screen.height);
                frame.setLocation(0, 0);
                
                // Agregar el tablero
                frame.setContentPane(new TableroVista());
                
                frame.setVisible(true);
            }
        });
    }
}