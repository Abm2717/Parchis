package vista;

import controlador.ClienteControlador;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Vista del tablero de Parchís.
 * ✅ MODIFICADO - Ahora recibe y guarda referencia al ClienteControlador
 */
public class TableroVista extends JPanel {
    
    // Referencia al controlador
    private ClienteControlador controlador;
    
    // Recursos visuales
    private Image fondo;
    private Image tablero;
    private Image fichaRoja, fichaAzul, fichaVerde, fichaAmarilla;
    private Image[] imagenesDados;
    private Image perfilJugador;
    
    // Componentes UI
    private JLabel lblDado1;
    private JLabel lblDado2;
    private JButton btnTirar;
    private JButton btnSalir;
    
    // Sistema de coordenadas
    private MapaCasillas mapaCasillas;
    private int tableroOffsetX;
    private int tableroOffsetY;
    
    /**
     * Constructor que recibe el controlador del juego
     * @param controlador Controlador que maneja la lógica y comunicación
     */
    public TableroVista(ClienteControlador controlador) {
        this.controlador = controlador;
        
        setLayout(null);
        cargarRecursos();
        inicializarComponentes();
        
        System.out.println("[TableroVista] TableroVista inicializado con controlador");
        
        // MouseListener para debug de coordenadas
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (tableroOffsetX == 0 || tableroOffsetY == 0) return;
                
                int tabW = 700, tabH = 700;
                
                // Coordenadas relativas al tablero
                int relX = e.getX() - tableroOffsetX;
                int relY = e.getY() - tableroOffsetY;
                
                System.out.println("CLICK EN TABLERO → X=" + relX + " Y=" + relY);
            }
        });
    }
    
    private void cargarRecursos() {
        try {
            fondo = new ImageIcon(getClass().getResource("/vista/recursos/fondo.jpg")).getImage();
            tablero = new ImageIcon(getClass().getResource("/vista/recursos/TAB.png")).getImage();
            
            fichaRoja = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FR.png")).getImage();
            fichaAzul = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FAZ.png")).getImage();
            fichaVerde = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FV.png")).getImage();
            fichaAmarilla = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FA.png")).getImage();
            
            perfilJugador = new ImageIcon(getClass().getResource("/vista/recursos/pp.png")).getImage();
            
            imagenesDados = new Image[6];
            for (int i = 0; i < 6; i++) {
                imagenesDados[i] = new ImageIcon(getClass().getResource("/vista/recursos/DADOS_D" + (i + 1) + ".png")).getImage();
            }
            
            System.out.println("[TableroVista] Recursos visuales cargados correctamente");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error cargando recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void inicializarComponentes() {
        // Panel de dados (izquierda)
        JPanel panelDados = new JPanel(null);
        panelDados.setBackground(new Color(0, 0, 0, 150));
        panelDados.setBorder(BorderFactory.createLineBorder(new Color(255, 207, 64), 2));
        panelDados.setBounds(50, 0, 200, 260);
        
        // Dados
        lblDado1 = new JLabel();
        lblDado1.setBounds(20, 20, 80, 80);
        if (imagenesDados != null && imagenesDados[0] != null) {
            lblDado1.setIcon(new ImageIcon(imagenesDados[0].getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        }
        panelDados.add(lblDado1);
        
        lblDado2 = new JLabel();
        lblDado2.setBounds(100, 20, 80, 80);
        if (imagenesDados != null && imagenesDados[0] != null) {
            lblDado2.setIcon(new ImageIcon(imagenesDados[0].getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        }
        panelDados.add(lblDado2);
        
        // Botón Tirar
        btnTirar = new JButton("Tirar");
        btnTirar.setBounds(50, 120, 100, 40);
        btnTirar.setFont(new Font("Arial", Font.BOLD, 16));
        btnTirar.setBackground(new Color(30, 30, 30));
        btnTirar.setForeground(Color.WHITE);
        btnTirar.setFocusPainted(false);
        btnTirar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTirar.addActionListener(e -> tirarDados());
        panelDados.add(btnTirar);
        
        add(panelDados);
        
        // Posicionar panel dados centrado verticalmente
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int yPos = (getHeight() - 260) / 2;
                panelDados.setLocation(50, yPos);
            }
        });
        
        // Botón Salir (arriba-izquierda)
        btnSalir = new JButton("Salir");
        btnSalir.setBounds(20, 20, 90, 35);
        btnSalir.setFont(new Font("Arial", Font.BOLD, 14));
        btnSalir.setBackground(new Color(0, 0, 0, 170));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFocusPainted(false);
        btnSalir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> salir());
        add(btnSalir);
    }
    
    /**
     * Acción al tirar los dados
     * TODO: En siguiente fase, integrar con controlador.tirarDados()
     */
    private void tirarDados() {
        // Por ahora, solo genera valores aleatorios para visualizar
        int dado1 = (int) (Math.random() * 6);
        int dado2 = (int) (Math.random() * 6);
        
        // Actualizar imágenes
        if (imagenesDados != null && imagenesDados[dado1] != null) {
            lblDado1.setIcon(new ImageIcon(imagenesDados[dado1].getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        }
        
        if (imagenesDados != null && imagenesDados[dado2] != null) {
            lblDado2.setIcon(new ImageIcon(imagenesDados[dado2].getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        }
        
        System.out.println("[TableroVista] Dados lanzados (visual): [" + (dado1 + 1) + "][" + (dado2 + 1) + "]");
        
        // TODO FASE 2: Llamar a controlador.tirarDados() cuando esté implementado
        /*
        if (controlador != null) {
            controlador.tirarDados();
        }
        */
    }
    
    /**
     * Acción al salir del juego
     */
    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que quieres salir?",
            "Confirmar salida",
            JOptionPane.YES_NO_OPTION
        );
        
        if (opcion == JOptionPane.YES_OPTION) {
            // TODO: Desconectar del servidor si está conectado
            if (controlador != null) {
                // controlador.desconectar();
            }
            System.exit(0);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 1. Dibujar fondo
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        }
        
        // 2. Calcular posición del tablero (centrado)
        int tabW = 700, tabH = 700;
        tableroOffsetX = (getWidth() - tabW) / 2;
        tableroOffsetY = (getHeight() - tabH) / 2;
        
        // Inicializar MapaCasillas con los offsets calculados
        if (mapaCasillas == null) {
            mapaCasillas = new MapaCasillas(tableroOffsetX, tableroOffsetY);
            System.out.println("[TableroVista] MapaCasillas inicializado con offset X=" + tableroOffsetX + " Y=" + tableroOffsetY);
        }
        
        // 3. Dibujar tablero
        if (tablero != null) {
            g.drawImage(tablero, tableroOffsetX, tableroOffsetY, tabW, tabH, this);
        }
        
        // 4. Dibujar perfiles y nombres de jugadores
        dibujarJugadores(g, tableroOffsetX, tableroOffsetY, tabW, tabH);
        
        // 5. Dibujar fichas
        dibujarFichas(g);
    }
    
    /**
     * Dibuja la información de los jugadores alrededor del tablero
     * Orden de asignación: ROJO, AMARILLO, VERDE, AZUL
     */
    private void dibujarJugadores(Graphics g, int x, int y, int tabW, int tabH) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        
        // TODO FASE 1 - Paso 4: Obtener nombres reales desde el controlador
        // Orden correcto: ROJO (1), AMARILLO (2), VERDE (3), AZUL (4)
        String[] nombresJugadores = {"Jugador 1 (Rojo)", "Jugador 2 (Amarillo)", "Jugador 3 (Verde)", "Jugador 4 (Azul)"};
        
        // Jugador 1 - Arriba izquierda (ROJO)
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 120, y + 50, 100, 100, this);
        }
        g.drawString(nombresJugadores[0], x - 120, y + 170);
        
        // Jugador 2 - Abajo derecha (AMARILLO)
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 20, y + tabH - 150, 100, 100, this);
        }
        g.drawString(nombresJugadores[1], x + tabW + 20, y + tabH - 30);
        
        // Jugador 3 - Abajo izquierda (VERDE)
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 120, y + tabH - 150, 100, 100, this);
        }
        g.drawString(nombresJugadores[2], x - 120, y + tabH - 30);
        
        // Jugador 4 - Arriba derecha (AZUL)
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 20, y + 50, 100, 100, this);
        }
        g.drawString(nombresJugadores[3], x + tabW + 20, y + 170);
    }
    
    /**
     * Dibuja las fichas en el tablero
     * TODO FASE 1 - Paso 3: Obtener fichas desde el modelo real
     */
    private void dibujarFichas(Graphics g) {
        if (mapaCasillas == null) return;
        
        // Por ahora, dibujar fichas hardcodeadas en sus casas
        // TODO: Reemplazar con fichas del modelo real
        
        // Casa roja (ejemplo estático)
        if (fichaRoja != null) {
            g.drawImage(fichaRoja, tableroOffsetX + 75, tableroOffsetY + 510, 60, 60, this);
            g.drawImage(fichaRoja, tableroOffsetX + 75, tableroOffsetY + 580, 60, 60, this);
            g.drawImage(fichaRoja, tableroOffsetX + 145, tableroOffsetY + 510, 60, 60, this);
            g.drawImage(fichaRoja, tableroOffsetX + 145, tableroOffsetY + 580, 60, 60, this);
        }
        
        // TODO FASE 1 - Paso 3: Implementar con MapaCasillas
        /*
        // Ejemplo de cómo se usará en Paso 3:
        List<FichaInfo> fichas = obtenerFichasDesdeModelo();
        
        for (FichaInfo ficha : fichas) {
            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(ficha.posicion);
            
            if (coord != null) {
                Image imagenFicha = obtenerImagenFicha(ficha.color);
                int fichaX = tableroOffsetX + coord.getX(ficha.indiceEnCasilla);
                int fichaY = tableroOffsetY + coord.getY(ficha.indiceEnCasilla);
                g.drawImage(imagenFicha, fichaX, fichaY, 60, 60, this);
            }
        }
        */
    }
    
    /**
     * Obtiene la imagen de una ficha según su color
     */
    private Image obtenerImagenFicha(String color) {
        switch (color.toUpperCase()) {
            case "ROJO": return fichaRoja;
            case "AZUL": return fichaAzul;
            case "VERDE": return fichaVerde;
            case "AMARILLO": return fichaAmarilla;
            default: return fichaRoja;
        }
    }
    
    /**
     * Actualiza la vista (para cuando cambie el estado del juego)
     */
    public void actualizarVista() {
        repaint();
    }
    
    /**
     * Main para pruebas visuales
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Parchís - Tablero");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(screen.width, screen.height);
            frame.setLocation(0, 0);
            
            // Crear TableroVista sin controlador para pruebas
            TableroVista tablero = new TableroVista(null);
            frame.setContentPane(tablero);
            
            frame.setVisible(true);
        });
    }
}