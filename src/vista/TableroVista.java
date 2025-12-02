package vista;

import controlador.ClienteControlador;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Vista del tablero de Parchís.
 * ✅ FASE 2: Dibuja fichas reales desde el estado del servidor
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
    
    // ✅ Estado del juego
    private List<FichaInfo> fichasEnJuego;
    private Map<String, String> nombresJugadores; // color -> nombre
    
    /**
     * Constructor que recibe el controlador del juego
     * @param controlador Controlador que maneja la lógica y comunicación
     */
    public TableroVista(ClienteControlador controlador) {
        this.controlador = controlador;
        this.fichasEnJuego = new ArrayList<>();
        this.nombresJugadores = new HashMap<>();
        
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
    
    /**
     * Carga todos los recursos gráficos (imágenes)
     */
    private void cargarRecursos() {
        try {
            // ✅ Cargar fondo
            fondo = new ImageIcon(getClass().getResource("/vista/recursos/fondo.jpg")).getImage();
            
            // ✅ Cargar tablero
            tablero = new ImageIcon(getClass().getResource("/vista/recursos/TAB.png")).getImage();
            
            // ✅ Cargar fichas
            fichaRoja = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FR.png")).getImage();
            fichaAzul = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FAZ.png")).getImage();
            fichaVerde = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FV.png")).getImage();
            fichaAmarilla = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FA.png")).getImage();
            
            // ✅ Cargar dados
            imagenesDados = new Image[7]; // índice 0 no se usa
            for (int i = 1; i <= 6; i++) {
                imagenesDados[i] = new ImageIcon(getClass().getResource("/vista/recursos/DADOS_D" + i + ".png")).getImage();
            }
            
            // ✅ Cargar logo para perfil
            perfilJugador = new ImageIcon(getClass().getResource("/vista/recursos/logoP.png")).getImage();
            
            System.out.println("[TableroVista] Recursos cargados exitosamente");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error cargando recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicializa los componentes de la interfaz (botones, labels, etc.)
     */
    private void inicializarComponentes() {
        // ✅ CORREGIDO: Panel para los dados (ARRIBA A LA DERECHA, fuera del tablero)
        JPanel panelDados = new JPanel();
        panelDados.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelDados.setOpaque(false);
        panelDados.setBounds(1050, 100, 300, 150); // Más a la derecha
        
        // Labels para los dados
        lblDado1 = new JLabel();
        lblDado2 = new JLabel();
        
        lblDado1.setPreferredSize(new Dimension(80, 80));
        lblDado2.setPreferredSize(new Dimension(80, 80));
        
        lblDado1.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        lblDado2.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        
        panelDados.add(lblDado1);
        panelDados.add(lblDado2);
        
        // ✅ CORREGIDO: Botón "Tirar Dados" - fuera del tablero
        btnTirar = new JButton("TIRAR");
        btnTirar.setBounds(1100, 280, 200, 60);
        btnTirar.setFont(new Font("Arial", Font.BOLD, 24));
        btnTirar.setBackground(new Color(34, 139, 34));
        btnTirar.setForeground(Color.WHITE);
        btnTirar.setFocusPainted(false);
        btnTirar.setBorderPainted(false);
        btnTirar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Efecto hover
        btnTirar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnTirar.setBackground(new Color(46, 184, 46));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btnTirar.setBackground(new Color(34, 139, 34));
            }
        });
        
        btnTirar.addActionListener(e -> tirarDados());
        
        // ✅ CORREGIDO: Botón "Salir" - fuera del tablero
        btnSalir = new JButton("SALIR");
        btnSalir.setBounds(1100, 600, 200, 60);
        btnSalir.setFont(new Font("Arial", Font.BOLD, 24));
        btnSalir.setBackground(new Color(220, 20, 60));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFocusPainted(false);
        btnSalir.setBorderPainted(false);
        btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Efecto hover
        btnSalir.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnSalir.setBackground(new Color(255, 60, 90));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btnSalir.setBackground(new Color(220, 20, 60));
            }
        });
        
        btnSalir.addActionListener(e -> salir());
        
        // Agregar componentes al panel
        add(panelDados);
        add(btnTirar);
        add(btnSalir);
    }
    
    /**
     * Maneja el evento de tirar los dados
     */
    private void tirarDados() {
        System.out.println("[TableroVista] Tirando dados...");
        
        if (controlador == null) {
            System.err.println("[ERROR] Controlador es null");
            JOptionPane.showMessageDialog(this, 
                "Error: No hay conexión con el controlador", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Verificar si es el turno del jugador
        if (!controlador.esmiTurno()) {
            JOptionPane.showMessageDialog(this, 
                "No es tu turno", 
                "Espera tu turno", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Llamar al controlador para tirar los dados
        controlador.tirarDados();
    }
    
    /**
     * ✅ Actualiza visualmente los dados con los valores recibidos del servidor
     * @param dado1 Valor del primer dado (1-6)
     * @param dado2 Valor del segundo dado (1-6)
     */
    public void mostrarResultadoDados(int dado1, int dado2) {
        SwingUtilities.invokeLater(() -> {
            if (dado1 >= 1 && dado1 <= 6 && imagenesDados[dado1] != null) {
                ImageIcon icon1 = new ImageIcon(imagenesDados[dado1].getScaledInstance(80, 80, Image.SCALE_SMOOTH));
                lblDado1.setIcon(icon1);
            }
            
            if (dado2 >= 1 && dado2 <= 6 && imagenesDados[dado2] != null) {
                ImageIcon icon2 = new ImageIcon(imagenesDados[dado2].getScaledInstance(80, 80, Image.SCALE_SMOOTH));
                lblDado2.setIcon(icon2);
            }
            
            System.out.println("[TableroVista] Dados actualizados visualmente: [" + dado1 + "][" + dado2 + "]");
        });
    }
    
    /**
     * Maneja el evento de salir del juego
     */
    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this, 
            "¿Estás seguro de que quieres salir?", 
            "Confirmar salida", 
            JOptionPane.YES_NO_OPTION);
        
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
    
    /**
     * ✅ Actualiza el estado del tablero desde el JSON del servidor
     */
    public void actualizarEstadoTablero(JsonObject tableroJson) {
        SwingUtilities.invokeLater(() -> {
            try {
                fichasEnJuego.clear();
                
                if (!tableroJson.has("casillas")) {
                    System.err.println("[ERROR] JSON no tiene campo 'casillas'");
                    return;
                }
                
                JsonArray casillas = tableroJson.getAsJsonArray("casillas");
                
                // Recorrer todas las casillas
                for (int i = 0; i < casillas.size(); i++) {
                    JsonObject casilla = casillas.get(i).getAsJsonObject();
                    
                    if (!casilla.has("fichas")) continue;
                    
                    JsonArray fichas = casilla.getAsJsonArray("fichas");
                    int indiceCasilla = casilla.get("indice").getAsInt();
                    
                    // Procesar fichas en esta casilla
                    for (int j = 0; j < fichas.size(); j++) {
                        JsonObject fichaJson = fichas.get(j).getAsJsonObject();
                        
                        int fichaId = fichaJson.get("id").getAsInt();
                        int jugadorId = fichaJson.get("jugadorId").getAsInt();
                        String color = fichaJson.get("color").getAsString();
                        String estado = fichaJson.get("estado").getAsString();
                        
                        FichaInfo ficha = new FichaInfo(fichaId, jugadorId, color, indiceCasilla, estado);
                        ficha.setIndiceEnCasilla(j);
                        
                        fichasEnJuego.add(ficha);
                    }
                }
                
                System.out.println("[TableroVista] Estado actualizado: " + fichasEnJuego.size() + " fichas en juego");
                
                // Redibujar el tablero
                repaint();
                
            } catch (Exception e) {
                System.err.println("[ERROR] Error actualizando estado del tablero: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * ✅ Actualiza los nombres de los jugadores
     */
    public void actualizarNombresJugadores(Map<String, String> jugadores) {
        SwingUtilities.invokeLater(() -> {
            this.nombresJugadores.clear();
            this.nombresJugadores.putAll(jugadores);
            System.out.println("[TableroVista] Nombres actualizados: " + nombresJugadores);
            repaint();
        });
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
        
        // 5. Dibujar fichas REALES
        dibujarFichas(g);
    }
    
    /**
     * ✅ CORREGIDO: Dibuja la información de los jugadores en las posiciones correctas
     */
    private void dibujarJugadores(Graphics g, int x, int y, int tabW, int tabH) {
        // Configuración de texto
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        
        // ✅ CORREGIDO: Obtener nombres reales o usar placeholders
        String nombreRojo = nombresJugadores.getOrDefault("ROJO", "Jugador Rojo");
        String nombreAzul = nombresJugadores.getOrDefault("AZUL", "Jugador Azul");
        String nombreVerde = nombresJugadores.getOrDefault("VERDE", "Jugador Verde");
        String nombreAmarillo = nombresJugadores.getOrDefault("AMARILLO", "Jugador Amarillo");
        
        // ✅ ROJO - Abajo izquierda
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 150, y + tabH - 150, 100, 100, this);
        }
        int rojoX = x - 150 + 50 - fm.stringWidth(nombreRojo) / 2;
        g.drawString(nombreRojo, rojoX, y + tabH - 30);
        
        // ✅ AZUL - Arriba izquierda
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 150, y + 50, 100, 100, this);
        }
        int azulX = x - 150 + 50 - fm.stringWidth(nombreAzul) / 2;
        g.drawString(nombreAzul, azulX, y + 170);
        
        // ✅ AMARILLO - Arriba derecha
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 50, y + 50, 100, 100, this);
        }
        int amarilloX = x + tabW + 50 + 50 - fm.stringWidth(nombreAmarillo) / 2;
        g.drawString(nombreAmarillo, amarilloX, y + 170);
        
        // ✅ VERDE - Abajo derecha
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 50, y + tabH - 150, 100, 100, this);
        }
        int verdeX = x + tabW + 50 + 50 - fm.stringWidth(nombreVerde) / 2;
        g.drawString(nombreVerde, verdeX, y + tabH - 30);
    }
    
    /**
     * ✅ Dibuja las fichas usando datos REALES del servidor
     */
    private void dibujarFichas(Graphics g) {
        if (mapaCasillas == null) return;
        
        // Dibujar fichas en CASA
        dibujarFichasEnCasa(g);
        
        // Dibujar fichas EN JUEGO
        for (FichaInfo ficha : fichasEnJuego) {
            if (ficha.estaEnCasa()) {
                continue;
            }
            
            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(ficha.getPosicionCasilla());
            
            if (coord == null) {
                System.err.println("[ERROR] No hay coordenadas para casilla " + ficha.getPosicionCasilla());
                continue;
            }
            
            Image imagenFicha = obtenerImagenFicha(ficha.getColor());
            
            if (imagenFicha == null) {
                System.err.println("[ERROR] No hay imagen para color " + ficha.getColor());
                continue;
            }
            
            int fichaX = tableroOffsetX + coord.getX(ficha.getIndiceEnCasilla());
            int fichaY = tableroOffsetY + coord.getY(ficha.getIndiceEnCasilla());
            
            g.drawImage(imagenFicha, fichaX - 30, fichaY - 30, 60, 60, this);
        }
    }
    
    /**
     * ✅ Dibuja las fichas en CASA (hardcodeadas por ahora)
     */
    private void dibujarFichasEnCasa(Graphics g) {
        // Casa ROJA (abajo-izquierda)
        if (fichaRoja != null) {
            g.drawImage(fichaRoja, tableroOffsetX + 75, tableroOffsetY + 510, 60, 60, this);
            g.drawImage(fichaRoja, tableroOffsetX + 75, tableroOffsetY + 580, 60, 60, this);
            g.drawImage(fichaRoja, tableroOffsetX + 145, tableroOffsetY + 510, 60, 60, this);
            g.drawImage(fichaRoja, tableroOffsetX + 145, tableroOffsetY + 580, 60, 60, this);
        }
        
        // Casa AZUL (arriba-izquierda)
        if (fichaAzul != null) {
            g.drawImage(fichaAzul, tableroOffsetX + 75, tableroOffsetY + 75, 60, 60, this);
            g.drawImage(fichaAzul, tableroOffsetX + 75, tableroOffsetY + 145, 60, 60, this);
            g.drawImage(fichaAzul, tableroOffsetX + 145, tableroOffsetY + 75, 60, 60, this);
            g.drawImage(fichaAzul, tableroOffsetX + 145, tableroOffsetY + 145, 60, 60, this);
        }
        
        // Casa AMARILLA (arriba-derecha)
        if (fichaAmarilla != null) {
            g.drawImage(fichaAmarilla, tableroOffsetX + 510, tableroOffsetY + 75, 60, 60, this);
            g.drawImage(fichaAmarilla, tableroOffsetX + 510, tableroOffsetY + 145, 60, 60, this);
            g.drawImage(fichaAmarilla, tableroOffsetX + 580, tableroOffsetY + 75, 60, 60, this);
            g.drawImage(fichaAmarilla, tableroOffsetX + 580, tableroOffsetY + 145, 60, 60, this);
        }
        
        // Casa VERDE (abajo-derecha)
        if (fichaVerde != null) {
            g.drawImage(fichaVerde, tableroOffsetX + 510, tableroOffsetY + 510, 60, 60, this);
            g.drawImage(fichaVerde, tableroOffsetX + 510, tableroOffsetY + 580, 60, 60, this);
            g.drawImage(fichaVerde, tableroOffsetX + 580, tableroOffsetY + 510, 60, 60, this);
            g.drawImage(fichaVerde, tableroOffsetX + 580, tableroOffsetY + 580, 60, 60, this);
        }
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
     * Actualiza la vista
     */
    public void actualizarVista() {
        repaint();
    }
}