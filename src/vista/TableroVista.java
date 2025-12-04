package vista;

import com.google.gson.JsonArray;
import controlador.ClienteControlador;
import controlador.ReglasValidador;  // ← IMPORT NECESARIO
import com.google.gson.JsonObject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * ✅ ACTUALIZADO: Soporte para bonus de captura
 * - Muestra mensaje cuando se captura ficha
 * - Marca fichas que pueden usar bonus (borde dorado)
 * - Pierde bonus si ninguna ficha puede usarlo
 */
public class TableroVista extends JPanel {
    
    // ==================== CAMPOS BÁSICOS ====================
    
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
    
    // Nombres de jugadores
    private String nombreRojo = "Jugador Rojo";
    private String nombreAzul = "Jugador Azul";
    private String nombreVerde = "Jugador Verde";
    private String nombreAmarillo = "Jugador Amarillo";
    
    // ==================== GESTIÓN DE FICHAS ====================
    
    private List<FichaVisual> fichasEnTablero;
    private Map<Integer, FichaVisual> mapaFichas;
    private String miColor = "ROJO";
    
    
    // ==================== SISTEMA DE SELECCIÓN ====================
    
    private int[] dadosActuales = {0, 0};
    private boolean[] dadosUsados = {false, false};
    private FichaVisual fichaSeleccionada = null;
    private List<FichaVisual> fichasMovibles = new ArrayList<>();
    
    // ✅ NUEVO: Sistema de bonus de captura
    private int bonusActivoCantidad = 0;  // 0 = sin bonus, >0 = bonus disponible
    
    // Panel de selección de dados
    private JPanel panelSeleccionDado;
    private JButton btnUsarDado1;
    private JButton btnUsarDado2;
    private JLabel lblPregunta;
    
    // ==================== CONSTRUCTOR ====================
    
    public TableroVista(ClienteControlador controlador, String[] nombres) {
        this.controlador = controlador;
        this.fichasEnTablero = new ArrayList<>();
        this.mapaFichas = new HashMap<>();

        // Asignar nombres
        if (nombres != null) {
            if (nombres.length > 0 && nombres[0] != null) {
                this.nombreRojo = nombres[0];
            }
            if (nombres.length > 1 && nombres[1] != null) {
                this.nombreAzul = nombres[1];
            }
            if (nombres.length > 2 && nombres[2] != null) {
                this.nombreVerde = nombres[2];
            }
            if (nombres.length > 3 && nombres[3] != null) {
                this.nombreAmarillo = nombres[3];
            }
            System.out.println("[TableroVista] Nombres recibidos - Rojo:" + nombreRojo + " Azul:" + nombreAzul + " Verde:" + nombreVerde + " Amarillo:" + nombreAmarillo);
        }

        setLayout(null);
        cargarRecursos();
        inicializarComponentes();
        inicializarPanelSeleccionDado();

        System.out.println("[TableroVista] TableroVista inicializado con controlador");

        // MouseListener para clicks en fichas
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                manejarClickEnTablero(e.getX(), e.getY());
            }
        });
        
        detectarMiColor(nombres);
        inicializarFichas();
    }
    
    // ==================== PANEL DE SELECCIÓN DE DADOS ====================
    
    private void inicializarPanelSeleccionDado() {
        panelSeleccionDado = new JPanel();
        panelSeleccionDado.setLayout(new BoxLayout(panelSeleccionDado, BoxLayout.Y_AXIS));
        panelSeleccionDado.setBackground(new Color(0, 0, 0, 200));
        panelSeleccionDado.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
        panelSeleccionDado.setBounds(1050, 400, 300, 180);
        panelSeleccionDado.setVisible(false);
        
        // Pregunta
        lblPregunta = new JLabel("¿Mover con cuál dado?");
        lblPregunta.setFont(new Font("Arial", Font.BOLD, 18));
        lblPregunta.setForeground(Color.WHITE);
        lblPregunta.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelSeleccionDado.add(Box.createVerticalStrut(15));
        panelSeleccionDado.add(lblPregunta);
        panelSeleccionDado.add(Box.createVerticalStrut(20));
        
        // Botón dado 1
        btnUsarDado1 = new JButton("6");
        btnUsarDado1.setFont(new Font("Arial", Font.BOLD, 32));
        btnUsarDado1.setPreferredSize(new Dimension(120, 60));
        btnUsarDado1.setMaximumSize(new Dimension(120, 60));
        btnUsarDado1.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnUsarDado1.setBackground(new Color(46, 184, 46));
        btnUsarDado1.setForeground(Color.WHITE);
        btnUsarDado1.setFocusPainted(false);
        btnUsarDado1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUsarDado1.addActionListener(e -> usarDado(0));
        panelSeleccionDado.add(btnUsarDado1);
        
        panelSeleccionDado.add(Box.createVerticalStrut(10));
        
        // Botón dado 2
        btnUsarDado2 = new JButton("3");
        btnUsarDado2.setFont(new Font("Arial", Font.BOLD, 32));
        btnUsarDado2.setPreferredSize(new Dimension(120, 60));
        btnUsarDado2.setMaximumSize(new Dimension(120, 60));
        btnUsarDado2.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnUsarDado2.setBackground(new Color(46, 184, 46));
        btnUsarDado2.setForeground(Color.WHITE);
        btnUsarDado2.setFocusPainted(false);
        btnUsarDado2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUsarDado2.addActionListener(e -> usarDado(1));
        panelSeleccionDado.add(btnUsarDado2);
        
        add(panelSeleccionDado);
    }
    
    // ==================== MANEJO DE CLICKS ====================
    
    private void manejarClickEnTablero(int x, int y) {
        if (dadosActuales[0] == 0 && dadosActuales[1] == 0) {
            return;
        }
        
        if (!controlador.esmiTurno()) {
            return;
        }
        
        FichaVisual fichaClickeada = obtenerFichaEnPosicion(x, y);
        
        if (fichaClickeada != null && fichasMovibles.contains(fichaClickeada)) {
            seleccionarFicha(fichaClickeada);
        }
    }
    
    private FichaVisual obtenerFichaEnPosicion(int x, int y) {
        int relX = x - tableroOffsetX;
        int relY = y - tableroOffsetY;
        
        for (FichaVisual ficha : fichasEnTablero) {
            if (ficha.estaEnCasa()) continue;
            
            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(ficha.getPosicionCasilla());
            if (coord == null) continue;
            
            int fichaX = coord.getX(0);
            int fichaY = coord.getY(0);
            
            if (relX >= fichaX && relX <= fichaX + 60 &&
                relY >= fichaY && relY <= fichaY + 60) {
                return ficha;
            }
        }
        
        return null;
    }
    
    private void seleccionarFicha(FichaVisual ficha) {
        fichaSeleccionada = ficha;
        System.out.println("[TableroVista] Ficha seleccionada: #" + ficha.getId() + " (" + ficha.getColor() + ")");
        
        // ✅ Si la ficha tiene bonus activo, mostrar solo el bonus
        if (ficha.tieneBordeBonus() && bonusActivoCantidad > 0) {
            System.out.println("[BONUS] Ficha con bonus seleccionada. Mostrando panel de bonus.");
            
            lblPregunta.setText("¿Usar bonus de captura?");
            btnUsarDado1.setText("+" + bonusActivoCantidad);
            btnUsarDado1.setEnabled(true);
            btnUsarDado2.setEnabled(false);
            btnUsarDado2.setVisible(false);  // Ocultar segundo botón
            
            panelSeleccionDado.setVisible(true);
            repaint();
            return;
        }
        
        // ========== LÓGICA NORMAL DE DADOS ==========
        
        lblPregunta.setText("¿Mover con cuál dado?");
        btnUsarDado2.setVisible(true);  // Asegurar que esté visible
        
        btnUsarDado1.setEnabled(!dadosUsados[0]);
        btnUsarDado2.setEnabled(!dadosUsados[1]);
        
        btnUsarDado1.setText(String.valueOf(dadosActuales[0]));
        btnUsarDado2.setText(String.valueOf(dadosActuales[1]));
        
        panelSeleccionDado.setVisible(true);
        
        repaint();
    }
    
    private void detectarMiColor(String[] nombres) {
        if (controlador != null) {
            String miNombre = controlador.getNombreJugador();

            System.out.println("[DEBUG] Mi nombre: " + miNombre);
            System.out.println("[DEBUG] Nombres array: Rojo=" + nombres[0] + " Azul=" + nombres[1] + " Verde=" + nombres[2] + " Amarillo=" + nombres[3]);

            if (miNombre.equals(nombres[0])) {
                miColor = "ROJO";
            } else if (miNombre.equals(nombres[1])) {
                miColor = "AZUL";
            } else if (miNombre.equals(nombres[2])) {
                miColor = "VERDE";
            } else if (miNombre.equals(nombres[3])) {
                miColor = "AMARILLO";
            }

            System.out.println("[TableroVista] Mi color detectado: " + miColor);
        }
    }
    
    // ==================== USAR DADO (✅ MODIFICADO: SOPORTE PARA BONUS) ====================
    
    private void usarDado(int indiceDado) {
        if (fichaSeleccionada == null) return;
        
        // ✅ DETECCIÓN DE BONUS: Si la ficha tiene borde dorado, usar bonus
        if (fichaSeleccionada.tieneBordeBonus() && bonusActivoCantidad > 0) {
            System.out.println("[BONUS] Usando bonus de +" + bonusActivoCantidad + " casillas en ficha #" + fichaSeleccionada.getId());
            
            // Validar que puede moverse con el bonus
            if (!ReglasValidador.puedeMoverse(fichaSeleccionada, bonusActivoCantidad, fichasEnTablero)) {
                JOptionPane.showMessageDialog(this, 
                    "⚠️ No puedes usar el bonus. Hay una barrera bloqueando el camino o te pasarías de la meta.", 
                    "Movimiento inválido", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            panelSeleccionDado.setVisible(false);
            
            // ✅ Enviar movimiento con bonus al controlador
            controlador.moverFichaConUnDado(fichaSeleccionada.getId(), bonusActivoCantidad, true);  // true = pasar turno
            
            // ✅ Limpiar bonus después de usarlo
            bonusActivoCantidad = 0;
            limpiarMarcasBonus();
            
            System.out.println("[BONUS] Bonus consumido. Pasando turno.");
            
            fichaSeleccionada = null;
            actualizarFichasMovibles();
            repaint();
            return;
        }
        
        // ========== LÓGICA NORMAL DE DADOS (sin bonus) ==========
        
        if (dadosUsados[indiceDado]) return;

        int valorDado = dadosActuales[indiceDado];
        
        // ✅ VALIDAR que el movimiento es LEGAL antes de ejecutar
        
        // Si está en casa, validar que puede salir
        if (fichaSeleccionada.estaEnCasa()) {
            if (valorDado != 5) {
                JOptionPane.showMessageDialog(this, 
                    "⚠️ Necesitas un 5 para sacar la ficha de casa", 
                    "Movimiento inválido", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!ReglasValidador.puedeSalirDeCasa(fichaSeleccionada.getCasillaSalida(), miColor, fichasEnTablero)) {
                JOptionPane.showMessageDialog(this, 
                    "⚠️ No puedes sacar ficha. Tu casilla de salida está bloqueada.", 
                    "Movimiento inválido", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            // Si está en tablero, validar que puede moverse con este dado
            if (!ReglasValidador.puedeMoverse(fichaSeleccionada, valorDado, fichasEnTablero)) {
                JOptionPane.showMessageDialog(this, 
                    "⚠️ No puedes mover con este dado. Hay una barrera bloqueando el camino o te pasarías de la meta.", 
                    "Movimiento inválido", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        
        System.out.println("[TableroVista] Usando dado " + valorDado + " para mover ficha #" + fichaSeleccionada.getId());

        panelSeleccionDado.setVisible(false);
        dadosUsados[indiceDado] = true;

        boolean esDoble = (dadosActuales[0] == dadosActuales[1]);
        boolean pasarTurno = esDoble ? false : (dadosUsados[0] && dadosUsados[1]);
        
        System.out.println("[TableroVista] Es doble: " + esDoble + ", pasar turno: " + pasarTurno);

        // ✅ Delegar TODO al controlador (animación incluida)
        controlador.moverFichaConUnDado(fichaSeleccionada.getId(), valorDado, pasarTurno);

        fichaSeleccionada = null;
        actualizarFichasMovibles();
        repaint();
    }
    
    public int obtenerPosicionFicha(int fichaId) {
        FichaVisual ficha = mapaFichas.get(fichaId);
        if (ficha == null) {
            System.err.println("[ERROR] No existe ficha con ID " + fichaId);
            return -1;
        }

        if (ficha.estaEnCasa()) {
            return -1;
        }

        return ficha.getPosicionCasilla();
    }
    
    // ==================== ACTUALIZAR FICHAS MOVIBLES (USA REGLASVALIDADOR) ====================
    
    private void actualizarFichasMovibles() {
        fichasMovibles.clear();

        if (dadosUsados[0] && dadosUsados[1]) {
            System.out.println("[TableroVista] Ambos dados usados, no hay fichas movibles");
            return;
        }

        if (!controlador.esmiTurno()) {
            return;
        }

        int dado1Disponible = dadosUsados[0] ? 0 : dadosActuales[0];
        int dado2Disponible = dadosUsados[1] ? 0 : dadosActuales[1];

        System.out.println("[TableroVista] Dados disponibles: [" + dado1Disponible + "][" + dado2Disponible + "]");

        for (FichaVisual ficha : fichasEnTablero) {
            if (!ficha.getColor().equals(miColor)) {
                continue;
            }

            boolean puedeMoverse = false;

            // ========== FICHAS EN CASA ==========
            if (ficha.estaEnCasa()) {
                // Puede salir con un 5
                if (dado1Disponible == 5 || dado2Disponible == 5) {
                    if (ReglasValidador.puedeSalirDeCasa(ficha.getCasillaSalida(), miColor, fichasEnTablero)) {
                        puedeMoverse = true;
                    } else {
                        System.out.println("[VALIDACIÓN] No puede sacar ficha #" + ficha.getId() + 
                                         " - salida tiene 2 fichas");
                    }
                }
                // Puede salir con suma = 5
                if (dado1Disponible + dado2Disponible == 5 && dado1Disponible > 0 && dado2Disponible > 0) {
                    if (ReglasValidador.puedeSalirDeCasa(ficha.getCasillaSalida(), miColor, fichasEnTablero)) {
                        puedeMoverse = true;
                    }
                }
            } 
            // ========== FICHAS EN JUEGO ==========
            else if (!ficha.estaEnMeta()) {
                // Verificar con dado 1
                if (dado1Disponible > 0 && ReglasValidador.puedeMoverse(ficha, dado1Disponible, fichasEnTablero)) {
                    puedeMoverse = true;
                }
                // Verificar con dado 2
                if (!puedeMoverse && dado2Disponible > 0 && ReglasValidador.puedeMoverse(ficha, dado2Disponible, fichasEnTablero)) {
                    puedeMoverse = true;
                }
            }

            if (puedeMoverse) {
                fichasMovibles.add(ficha);
                ficha.setMovible(true);
            } else {
                ficha.setMovible(false);
            }
        }

        System.out.println("[TableroVista] Fichas movibles (" + miColor + "): " + fichasMovibles.size());
    }
    
    // ==================== ✅ NUEVOS MÉTODOS PARA BONUS ====================
    
    /**
     * ✅ Muestra mensaje de bonus captura
     */
    public void mostrarMensajeBonus(int cantidadBonus) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                "🎉 ¡Capturaste una ficha!\n\n+" + cantidadBonus + " casillas de bonus.\nSelecciona una ficha para usar el bonus.",
                "Bonus Captura",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }
    
    /**
     * ✅ Verifica qué fichas pueden usar el bonus
     * Marca con borde DORADO las fichas que pueden usarlo
     * 
     * @param cantidadBonus Cantidad de casillas del bonus
     * @return true si al menos una ficha puede usar el bonus
     */
    public boolean verificarFichasParaBonus(int cantidadBonus) {
        boolean algunaFichaPuedeUsar = false;
        
        // ✅ Activar bonus
        bonusActivoCantidad = cantidadBonus;
        System.out.println("[BONUS] Bonus activado: +" + bonusActivoCantidad + " casillas");
        
        // Limpiar marcas previas de bonus
        limpiarMarcasBonus();
        
        for (FichaVisual ficha : fichasEnTablero) {
            if (!ficha.getColor().equals(miColor)) continue;
            if (ficha.estaEnCasa() || ficha.estaEnMeta()) continue;
            
            // Verificar si puede moverse con el bonus
            if (ReglasValidador.puedeMoverse(ficha, cantidadBonus, fichasEnTablero)) {
                ficha.setMovible(true);
                ficha.setBordeBonus(true); // ✅ Marcar con borde dorado
                algunaFichaPuedeUsar = true;
                System.out.println("[BONUS] Ficha #" + ficha.getId() + " puede usar bonus");
            }
        }
        
        repaint();
        return algunaFichaPuedeUsar;
    }
    
    /**
     * ✅ Limpia marcas de bonus y desactiva el bonus
     */
    public void limpiarMarcasBonus() {
        bonusActivoCantidad = 0;  // ✅ Desactivar bonus
        for (FichaVisual ficha : fichasEnTablero) {
            ficha.setBordeBonus(false);
        }
        repaint();
    }
    
    // ==================== FIN MÉTODOS BONUS ====================
    
    private void inicializarFichas() {
        fichasEnTablero.clear();
        mapaFichas.clear();

        int fichaId = 1;

        for (int i = 0; i < 4; i++) {
            FichaVisual ficha = new FichaVisual(fichaId, 1, "ROJO");
            fichasEnTablero.add(ficha);
            mapaFichas.put(fichaId, ficha);
            fichaId++;
        }

        for (int i = 0; i < 4; i++) {
            FichaVisual ficha = new FichaVisual(fichaId, 2, "AZUL");
            fichasEnTablero.add(ficha);
            mapaFichas.put(fichaId, ficha);
            fichaId++;
        }

        for (int i = 0; i < 4; i++) {
            FichaVisual ficha = new FichaVisual(fichaId, 3, "VERDE");
            fichasEnTablero.add(ficha);
            mapaFichas.put(fichaId, ficha);
            fichaId++;
        }

        for (int i = 0; i < 4; i++) {
            FichaVisual ficha = new FichaVisual(fichaId, 4, "AMARILLO");
            fichasEnTablero.add(ficha);
            mapaFichas.put(fichaId, ficha);
            fichaId++;
        }

        System.out.println("[TableroVista] Inicializadas " + fichasEnTablero.size() + " fichas");
    }
    
    private void cargarRecursos() {
        try {
            fondo = new ImageIcon(getClass().getResource("/vista/recursos/fondo.jpg")).getImage();
            tablero = new ImageIcon(getClass().getResource("/vista/recursos/TAB.png")).getImage();
            fichaRoja = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FR.png")).getImage();
            fichaAzul = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FAZ.png")).getImage();
            fichaVerde = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FV.png")).getImage();
            fichaAmarilla = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FA.png")).getImage();
            
            imagenesDados = new Image[7];
            for (int i = 1; i <= 6; i++) {
                imagenesDados[i] = new ImageIcon(getClass().getResource("/vista/recursos/DADOS_D" + i + ".png")).getImage();
            }
            
            perfilJugador = new ImageIcon(getClass().getResource("/vista/recursos/logoP.png")).getImage();
            
            System.out.println("[TableroVista] Recursos cargados exitosamente");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error cargando recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void inicializarComponentes() {
        JPanel panelDados = new JPanel();
        panelDados.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelDados.setOpaque(false);
        panelDados.setBounds(1050, 100, 300, 150);
        
        lblDado1 = new JLabel();
        lblDado2 = new JLabel();
        
        lblDado1.setPreferredSize(new Dimension(80, 80));
        lblDado2.setPreferredSize(new Dimension(80, 80));
        
        lblDado1.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        lblDado2.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        
        panelDados.add(lblDado1);
        panelDados.add(lblDado2);
        
        btnTirar = new JButton("TIRAR");
        btnTirar.setBounds(1100, 280, 200, 60);
        btnTirar.setFont(new Font("Arial", Font.BOLD, 24));
        btnTirar.setBackground(new Color(34, 139, 34));
        btnTirar.setForeground(Color.WHITE);
        btnTirar.setFocusPainted(false);
        btnTirar.setBorderPainted(false);
        btnTirar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
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
        
        btnSalir = new JButton("SALIR");
        btnSalir.setBounds(1100, 600, 200, 60);
        btnSalir.setFont(new Font("Arial", Font.BOLD, 24));
        btnSalir.setBackground(new Color(220, 20, 60));
        btnSalir.setForeground(Color.WHITE);
        btnSalir.setFocusPainted(false);
        btnSalir.setBorderPainted(false);
        btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
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
        
        add(panelDados);
        add(btnTirar);
        add(btnSalir);
    }
    
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
        
        if (!controlador.esmiTurno()) {
            JOptionPane.showMessageDialog(this, 
                "No es tu turno", 
                "Espera tu turno", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        controlador.tirarDados();
    }
    
    public void mostrarResultadoDados(int dado1, int dado2) {
        SwingUtilities.invokeLater(() -> {
            dadosActuales[0] = dado1;
            dadosActuales[1] = dado2;
            dadosUsados[0] = false;
            dadosUsados[1] = false;

            if (dado1 == 5 && dado2 == 5) {
                System.out.println("[TableroVista] DOBLE 5-5 detectado, el servidor sacará 2 fichas automáticamente");
            }

            if (dado1 >= 1 && dado1 <= 6 && imagenesDados[dado1] != null) {
                ImageIcon icon1 = new ImageIcon(imagenesDados[dado1].getScaledInstance(80, 80, Image.SCALE_SMOOTH));
                lblDado1.setIcon(icon1);
            }

            if (dado2 >= 1 && dado2 <= 6 && imagenesDados[dado2] != null) {
                ImageIcon icon2 = new ImageIcon(imagenesDados[dado2].getScaledInstance(80, 80, Image.SCALE_SMOOTH));
                lblDado2.setIcon(icon2);
            }

            actualizarFichasMovibles();

            System.out.println("[TableroVista] Dados actualizados: [" + dado1 + "][" + dado2 + "]");

            repaint();
        });
    }
    
    public void actualizarNombresJugadores(String nombreRojo, String nombreAzul, String nombreVerde, String nombreAmarillo) {
        SwingUtilities.invokeLater(() -> {
            this.nombreRojo = nombreRojo;
            this.nombreAzul = nombreAzul;
            this.nombreVerde = nombreVerde;
            this.nombreAmarillo = nombreAmarillo;
            
            System.out.println("[TableroVista] Nombres actualizados");
            repaint();
        });
    }
    
    public void actualizarColorFicha(int fichaId, String nuevoColor) {
        FichaVisual ficha = mapaFichas.get(fichaId);
        if (ficha != null && !ficha.getColor().equals(nuevoColor)) {
            System.out.println("[TableroVista] Actualizando color de ficha #" + fichaId + ": " + ficha.getColor() + " -> " + nuevoColor);
            ficha.setColor(nuevoColor);
        }
    }
    
    public void sacarFicha(int fichaId) {
        SwingUtilities.invokeLater(() -> {
            FichaVisual ficha = mapaFichas.get(fichaId);
            if (ficha == null) {
                System.err.println("[ERROR] No existe ficha con ID " + fichaId);
                return;
            }

            ficha.setEstaEnCasa(false);
            ficha.setPosicionCasilla(ficha.getCasillaSalida());

            System.out.println("[TableroVista] Ficha #" + fichaId + " (" + ficha.getColor() + ") sacada a casilla " + ficha.getPosicionCasilla());

            repaint();
        });
    }

    // ==================== AVANZAR CASILLA (USA REGLASVALIDADOR) ====================
    
    public void avanzarCasilla(int fichaId) {
        SwingUtilities.invokeLater(() -> {
            FichaVisual ficha = mapaFichas.get(fichaId);
            if (ficha == null) {
                System.err.println("[ERROR] No existe ficha con ID " + fichaId);
                return;
            }

            if (ficha.estaEnCasa()) {
                System.err.println("[ERROR] Ficha #" + fichaId + " está en casa, no puede avanzar");
                return;
            }

            int posicionActual = ficha.getPosicionCasilla();
            String color = ficha.getColor();
            
            // ✅ Usar ReglasValidador para calcular nueva posición
            int nuevaPosicion = ReglasValidador.avanzarUnaCasilla(posicionActual, color);

            ficha.setPosicionCasilla(nuevaPosicion);

            System.out.println("[TableroVista] Ficha #" + fichaId + " avanzó a casilla " + nuevaPosicion);

            // ✅ Verificar si llegó a meta usando ReglasValidador
            if (ReglasValidador.llegoAMeta(nuevaPosicion, color)) {
                System.out.println("[TableroVista] Ficha #" + fichaId + " LLEGÓ A META!");
                ficha.setEstaEnMeta(true);
            }

            repaint();
        });
    }

    public void mandarACasa(int fichaId) {
        SwingUtilities.invokeLater(() -> {
            FichaVisual ficha = mapaFichas.get(fichaId);
            if (ficha == null) {
                System.err.println("[ERROR] No existe ficha con ID " + fichaId);
                return;
            }

            ficha.setEstaEnCasa(true);
            ficha.setPosicionCasilla(-1);

            System.out.println("[TableroVista] Ficha #" + fichaId + " (" + ficha.getColor() + ") enviada a casa");

            repaint();
        });
    }

    public void meterEnMeta(int fichaId) {
        SwingUtilities.invokeLater(() -> {
            FichaVisual ficha = mapaFichas.get(fichaId);
            if (ficha == null) {
                System.err.println("[ERROR] No existe ficha con ID " + fichaId);
                return;
            }

            ficha.setEstaEnMeta(true);

            int posicionMeta = 0;
            switch (ficha.getColor()) {
                case "ROJO": posicionMeta = 1000; break;
                case "VERDE": posicionMeta = 2000; break;
                case "AZUL": posicionMeta = 3000; break;
                case "AMARILLO": posicionMeta = 4000; break;
            }
            ficha.setPosicionCasilla(posicionMeta);

            System.out.println("[TableroVista] Ficha #" + fichaId + " (" + ficha.getColor() + ") llegó a META");

            repaint();
        });
    }
    
    private void salir() {
        int opcion = JOptionPane.showConfirmDialog(this, 
            "¿Estás seguro de que quieres salir?", 
            "Confirmar salida", 
            JOptionPane.YES_NO_OPTION);
        
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
    
    public void actualizarVista() {
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        }
        
        int tabW = 700, tabH = 700;
        tableroOffsetX = (getWidth() - tabW) / 2;
        tableroOffsetY = (getHeight() - tabH) / 2;
        
        if (mapaCasillas == null) {
            mapaCasillas = new MapaCasillas(tableroOffsetX, tableroOffsetY);
            System.out.println("[TableroVista] MapaCasillas inicializado");
        }
        
        if (tablero != null) {
            g.drawImage(tablero, tableroOffsetX, tableroOffsetY, tabW, tabH, this);
        }
        
        dibujarJugadores(g, tableroOffsetX, tableroOffsetY, tabW, tabH);
        
        dibujarFichas(g);
        
        dibujarBordesFichas(g);
    }
    
    private void dibujarBordesFichas(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Borde verde para fichas movibles (normal)
        g2d.setColor(new Color(0, 255, 0));
        g2d.setStroke(new BasicStroke(4));
        for (FichaVisual ficha : fichasMovibles) {
            if (ficha.estaEnCasa()) continue;
            if (ficha.tieneBordeBonus()) continue; // No pintar verde si tiene borde dorado
            
            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(ficha.getPosicionCasilla());
            if (coord == null) continue;
            
            int x = tableroOffsetX + coord.getX(0);
            int y = tableroOffsetY + coord.getY(0);
            
            g2d.drawOval(x - 2, y - 2, 64, 64);
        }
        
        // ✅ NUEVO: Borde dorado para fichas con bonus
        for (FichaVisual ficha : fichasEnTablero) {
            if (ficha.estaEnCasa() || ficha.estaEnMeta()) continue;
            if (!ficha.tieneBordeBonus()) continue;
            
            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(ficha.getPosicionCasilla());
            if (coord == null) continue;
            
            int x = tableroOffsetX + coord.getX(0);
            int y = tableroOffsetY + coord.getY(0);
            
            // Borde dorado principal
            g2d.setColor(new Color(255, 215, 0)); // Dorado
            g2d.setStroke(new BasicStroke(5));
            g2d.drawOval(x - 3, y - 3, 66, 66);
            
            // Borde dorado brillante (efecto)
            g2d.setColor(new Color(255, 255, 0, 150));
            g2d.setStroke(new BasicStroke(7));
            g2d.drawOval(x - 5, y - 5, 70, 70);
        }
        
        // Borde azul para ficha seleccionada
        if (fichaSeleccionada != null && !fichaSeleccionada.estaEnCasa()) {
            g2d.setColor(new Color(0, 100, 255));
            g2d.setStroke(new BasicStroke(6));
            
            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(fichaSeleccionada.getPosicionCasilla());
            if (coord != null) {
                int x = tableroOffsetX + coord.getX(0);
                int y = tableroOffsetY + coord.getY(0);
                
                g2d.drawOval(x - 4, y - 4, 68, 68);
            }
        }
    }
    
    private void dibujarJugadores(Graphics g, int x, int y, int tabW, int tabH) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 150, y + tabH - 150, 100, 100, this);
        }
        int rojoX = x - 150 + 50 - fm.stringWidth(nombreRojo) / 2;
        g.drawString(nombreRojo, rojoX, y + tabH - 30);
        
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 150, y + 50, 100, 100, this);
        }
        int azulX = x - 150 + 50 - fm.stringWidth(nombreAzul) / 2;
        g.drawString(nombreAzul, azulX, y + 170);
        
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 50, y + 50, 100, 100, this);
        }
        int amarilloX = x + tabW + 50 + 50 - fm.stringWidth(nombreAmarillo) / 2;
        g.drawString(nombreAmarillo, amarilloX, y + 170);
        
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 50, y + tabH - 150, 100, 100, this);
        }
        int verdeX = x + tabW + 50 + 50 - fm.stringWidth(nombreVerde) / 2;
        g.drawString(nombreVerde, verdeX, y + tabH - 30);
    }
    
    private void dibujarFichas(Graphics g) {
        if (mapaCasillas == null) return;

        Map<Integer, Integer> contadorPorCasilla = new HashMap<>();

        for (FichaVisual ficha : fichasEnTablero) {
            if (ficha.estaEnCasa()) {
                continue;
            }

            int casilla = ficha.getPosicionCasilla();

            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(casilla);

            if (coord == null) {
                System.err.println("[ERROR] No hay coordenadas para casilla " + casilla);
                continue;
            }

            Image imagenFicha = obtenerImagenFicha(ficha.getColor());

            if (imagenFicha == null) {
                System.err.println("[ERROR] No hay imagen para color " + ficha.getColor());
                continue;
            }

            int indiceEnCasilla = contadorPorCasilla.getOrDefault(casilla, 0);
            contadorPorCasilla.put(casilla, indiceEnCasilla + 1);

            int fichaX = tableroOffsetX + coord.getX(indiceEnCasilla);
            int fichaY = tableroOffsetY + coord.getY(indiceEnCasilla);

            g.drawImage(imagenFicha, fichaX, fichaY, 60, 60, this);
        }

        dibujarFichasEnCasa(g);
    }

    private void dibujarFichasEnCasa(Graphics g) {
        int contadorRojo = 0;
        int contadorAzul = 0;
        int contadorVerde = 0;
        int contadorAmarillo = 0;

        for (FichaVisual ficha : fichasEnTablero) {
            if (!ficha.estaEnCasa()) continue;

            Image imagenFicha = obtenerImagenFicha(ficha.getColor());
            if (imagenFicha == null) continue;

            int x = 0, y = 0;

            switch (ficha.getColor()) {
                case "ROJO":
                    switch (contadorRojo) {
                        case 0: x = 75; y = 510; break;
                        case 1: x = 75; y = 580; break;
                        case 2: x = 145; y = 510; break;
                        case 3: x = 145; y = 580; break;
                    }
                    contadorRojo++;
                    break;

                case "AZUL":
                    switch (contadorAzul) {
                        case 0: x = 75; y = 75; break;
                        case 1: x = 75; y = 145; break;
                        case 2: x = 145; y = 75; break;
                        case 3: x = 145; y = 145; break;
                    }
                    contadorAzul++;
                    break;

                case "AMARILLO":
                    switch (contadorAmarillo) {
                        case 0: x = 510; y = 75; break;
                        case 1: x = 510; y = 145; break;
                        case 2: x = 580; y = 75; break;
                        case 3: x = 580; y = 145; break;
                    }
                    contadorAmarillo++;
                    break;

                case "VERDE":
                    switch (contadorVerde) {
                        case 0: x = 510; y = 510; break;
                        case 1: x = 510; y = 580; break;
                        case 2: x = 580; y = 510; break;
                        case 3: x = 580; y = 580; break;
                    }
                    contadorVerde++;
                    break;
            }

            g.drawImage(imagenFicha, tableroOffsetX + x, tableroOffsetY + y, 60, 60, this);
        }
    }
    
    private Image obtenerImagenFicha(String color) {
        switch (color.toUpperCase()) {
            case "ROJO": return fichaRoja;
            case "AZUL": return fichaAzul;
            case "VERDE": return fichaVerde;
            case "AMARILLO": return fichaAmarilla;
            default: return fichaRoja;
        }
    }
}