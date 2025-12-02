package vista;

import com.google.gson.JsonArray;
import controlador.ClienteControlador;
import com.google.gson.JsonObject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Vista del tablero de Parchís.
 * Conectada con ClienteControlador para mostrar el juego en tiempo real
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
    
    // ✅ NUEVO: Nombres de jugadores
    private String nombreRojo = "Jugador Rojo";
    private String nombreAzul = "Jugador Azul";
    private String nombreVerde = "Jugador Verde";
    private String nombreAmarillo = "Jugador Amarillo";
    
    //Fichas 
    // ✅ NUEVO: Gestión de fichas reales
    private List<FichaVisual> fichasEnTablero;        // Todas las fichas del juego
    private Map<Integer, FichaVisual> mapaFichas;     // Mapa rápido: fichaId -> FichaVisual
    
    /**
    * Constructor que recibe el controlador y los nombres
    */
    public TableroVista(ClienteControlador controlador, String[] nombres) {
        this.controlador = controlador;
        this.fichasEnTablero = new ArrayList<>();
        this.mapaFichas = new HashMap<>();

       // ✅ Asignar nombres de forma segura (pueden ser 2, 3 o 4 jugadores)
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
       } else {
           System.out.println("[TableroVista] No se recibieron nombres, usando valores por defecto");
       }

       setLayout(null);
       cargarRecursos();
       inicializarComponentes();

       System.out.println("[TableroVista] TableroVista inicializado con controlador");

       // MouseListener...
       addMouseListener(new MouseAdapter() {
           @Override
           public void mouseClicked(MouseEvent e) {
               if (tableroOffsetX == 0 || tableroOffsetY == 0) return;
               int relX = e.getX() - tableroOffsetX;
               int relY = e.getY() - tableroOffsetY;
               System.out.println("CLICK EN TABLERO → X=" + relX + " Y=" + relY);
           }
       });
       inicializarFichas();
      }
    
    /**
    * ✅ NUEVO: Inicializa las 16 fichas del juego (4 por jugador)
    */
     private void inicializarFichas() {
       fichasEnTablero.clear();
       mapaFichas.clear();

       int fichaId = 1;

       // 4 fichas ROJAS (jugador 1)
       for (int i = 0; i < 4; i++) {
           FichaVisual ficha = new FichaVisual(fichaId, 1, "ROJO");
           fichasEnTablero.add(ficha);
           mapaFichas.put(fichaId, ficha);
           fichaId++;
       }

       // 4 fichas AZULES (jugador 2)
       for (int i = 0; i < 4; i++) {
           FichaVisual ficha = new FichaVisual(fichaId, 2, "AZUL");
           fichasEnTablero.add(ficha);
           mapaFichas.put(fichaId, ficha);
           fichaId++;
       }

       // 4 fichas VERDES (jugador 3)
       for (int i = 0; i < 4; i++) {
           FichaVisual ficha = new FichaVisual(fichaId, 3, "VERDE");
           fichasEnTablero.add(ficha);
           mapaFichas.put(fichaId, ficha);
           fichaId++;
       }

       // 4 fichas AMARILLAS (jugador 4)
       for (int i = 0; i < 4; i++) {
           FichaVisual ficha = new FichaVisual(fichaId, 4, "AMARILLO");
           fichasEnTablero.add(ficha);
           mapaFichas.put(fichaId, ficha);
           fichaId++;
       }

       System.out.println("[TableroVista] Inicializadas " + fichasEnTablero.size() + " fichas");
    }
    
    /**
     * Carga todos los recursos gráficos
     */
    private void cargarRecursos() {
        try {
            // Cargar fondo
            fondo = new ImageIcon(getClass().getResource("/vista/recursos/fondo.jpg")).getImage();
            
            // Cargar tablero
            tablero = new ImageIcon(getClass().getResource("/vista/recursos/TAB.png")).getImage();
            
            // Cargar fichas
            fichaRoja = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FR.png")).getImage();
            fichaAzul = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FAZ.png")).getImage();
            fichaVerde = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FV.png")).getImage();
            fichaAmarilla = new ImageIcon(getClass().getResource("/vista/recursos/FICHAS_FA.png")).getImage();
            
            // Cargar dados
            imagenesDados = new Image[7];
            for (int i = 1; i <= 6; i++) {
                imagenesDados[i] = new ImageIcon(getClass().getResource("/vista/recursos/DADOS_D" + i + ".png")).getImage();
            }
            
            // Cargar perfil
            perfilJugador = new ImageIcon(getClass().getResource("/vista/recursos/logoP.png")).getImage();
            
            System.out.println("[TableroVista] Recursos cargados exitosamente");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error cargando recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicializa los componentes de la interfaz
     */
    private void inicializarComponentes() {
        // Panel para los dados
        JPanel panelDados = new JPanel();
        panelDados.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelDados.setOpaque(false);
        panelDados.setBounds(1050, 100, 300, 150);
        
        // Labels para los dados
        lblDado1 = new JLabel();
        lblDado2 = new JLabel();
        
        lblDado1.setPreferredSize(new Dimension(80, 80));
        lblDado2.setPreferredSize(new Dimension(80, 80));
        
        lblDado1.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        lblDado2.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        
        panelDados.add(lblDado1);
        panelDados.add(lblDado2);
        
        // Botón "Tirar Dados"
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
        
        // Botón "Salir"
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
        
        // Agregar componentes
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
        
        if (!controlador.esmiTurno()) {
            JOptionPane.showMessageDialog(this, 
                "No es tu turno", 
                "Espera tu turno", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        controlador.tirarDados();
    }
    
    /**
     * Actualiza visualmente los dados
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
     * ✅ NUEVO: Actualiza los nombres de los jugadores
     */
    public void actualizarNombresJugadores(String nombreRojo, String nombreAzul, String nombreVerde, String nombreAmarillo) {
        SwingUtilities.invokeLater(() -> {
            this.nombreRojo = nombreRojo;
            this.nombreAzul = nombreAzul;
            this.nombreVerde = nombreVerde;
            this.nombreAmarillo = nombreAmarillo;
            
            System.out.println("[TableroVista] Nombres actualizados - Rojo:" + nombreRojo + " Azul:" + nombreAzul + " Verde:" + nombreVerde + " Amarillo:" + nombreAmarillo);
            repaint();
        });
    }
    
   /**
    * ✅ MODIFICADO: Actualiza las fichas desde el JSON del servidor
    */
   public void actualizarFichas(JsonObject tableroJson) {
       SwingUtilities.invokeLater(() -> {
           try {
               System.out.println("[TableroVista] Actualizando fichas desde servidor");

               if (!tableroJson.has("casillas")) {
                   System.err.println("[ERROR] JSON no tiene campo 'casillas'");
                   return;
               }

               JsonArray casillas = tableroJson.getAsJsonArray("casillas");

               // ✅ PASO 1: Resetear todas las fichas a casa
               for (FichaVisual ficha : fichasEnTablero) {
                   ficha.setEstaEnCasa(true);
                   ficha.setPosicionCasilla(-1);
                   ficha.setEstaEnMeta(false);
               }

               // ✅ PASO 2: Actualizar posiciones desde el servidor
               for (int i = 0; i < casillas.size(); i++) {
                   JsonObject casilla = casillas.get(i).getAsJsonObject();

                   if (!casilla.has("fichas")) continue;

                   JsonArray fichas = casilla.getAsJsonArray("fichas");
                   int indiceCasilla = casilla.get("indice").getAsInt();

                   // Procesar fichas en esta casilla
                   for (int j = 0; j < fichas.size(); j++) {
                       JsonObject fichaJson = fichas.get(j).getAsJsonObject();

                       int fichaId = fichaJson.get("id").getAsInt();
                       String estado = fichaJson.get("estado").getAsString();
                       String colorServidor = fichaJson.get("color").getAsString(); // ✅ NUEVO

                       FichaVisual ficha = mapaFichas.get(fichaId);

                       if (ficha == null) {
                           System.err.println("[ERROR] Ficha ID " + fichaId + " no existe en mapaFichas");
                           continue;
                       }

                       // ✅ NUEVO: Actualizar color desde servidor
                       if (!ficha.getColor().equals(colorServidor)) {
                           System.out.println("[TableroVista] Actualizando color de ficha #" + fichaId + ": " + ficha.getColor() + " -> " + colorServidor);
                           ficha.setColor(colorServidor);
                       }

                       // Actualizar estado según lo que dice el servidor
                       switch (estado) {
                           case "EN_CASA":
                               ficha.setEstaEnCasa(true);
                               ficha.setPosicionCasilla(-1);
                               break;

                           case "EN_TABLERO":
                               ficha.setEstaEnCasa(false);
                               ficha.setPosicionCasilla(indiceCasilla);
                               break;

                           case "EN_META":
                               ficha.setEstaEnMeta(true);
                               ficha.setEstaEnCasa(false);
                               // Posición de meta según color
                               int posMeta = 0;
                               switch (ficha.getColor()) {
                                   case "ROJO": posMeta = 1000; break;
                                   case "VERDE": posMeta = 2000; break;
                                   case "AZUL": posMeta = 3000; break;
                                   case "AMARILLO": posMeta = 4000; break;
                               }
                               ficha.setPosicionCasilla(posMeta);
                               break;
                       }
                   }
               }

               // Contar fichas en juego
               int fichasEnJuego = 0;
               for (FichaVisual ficha : fichasEnTablero) {
                   if (!ficha.estaEnCasa()) fichasEnJuego++;
               }

               System.out.println("[TableroVista] Estado actualizado: " + fichasEnJuego + " fichas en juego");

               // ✅ PASO 3: Redibujar
               repaint();

           } catch (Exception e) {
               System.err.println("[ERROR] Error actualizando fichas: " + e.getMessage());
               e.printStackTrace();
           }
       });
   }
    
    /**
     * Maneja el evento de salir
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
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 1. Dibujar fondo
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        }
        
        // 2. Calcular posición del tablero
        int tabW = 700, tabH = 700;
        tableroOffsetX = (getWidth() - tabW) / 2;
        tableroOffsetY = (getHeight() - tabH) / 2;
        
        // Inicializar MapaCasillas
        if (mapaCasillas == null) {
            mapaCasillas = new MapaCasillas(tableroOffsetX, tableroOffsetY);
            System.out.println("[TableroVista] MapaCasillas inicializado con offset X=" + tableroOffsetX + " Y=" + tableroOffsetY);
        }
        
        // 3. Dibujar tablero
        if (tablero != null) {
            g.drawImage(tablero, tableroOffsetX, tableroOffsetY, tabW, tabH, this);
        }
        
        // 4. Dibujar jugadores
        dibujarJugadores(g, tableroOffsetX, tableroOffsetY, tabW, tabH);
        
        // 5. Dibujar fichas
        dibujarFichas(g);
    }
    
    /**
     * Dibuja los nombres de los jugadores
     */
    private void dibujarJugadores(Graphics g, int x, int y, int tabW, int tabH) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        
        // ROJO - Abajo izquierda
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 150, y + tabH - 150, 100, 100, this);
        }
        int rojoX = x - 150 + 50 - fm.stringWidth(nombreRojo) / 2;
        g.drawString(nombreRojo, rojoX, y + tabH - 30);
        
        // AZUL - Arriba izquierda
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x - 150, y + 50, 100, 100, this);
        }
        int azulX = x - 150 + 50 - fm.stringWidth(nombreAzul) / 2;
        g.drawString(nombreAzul, azulX, y + 170);
        
        // AMARILLO - Arriba derecha
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 50, y + 50, 100, 100, this);
        }
        int amarilloX = x + tabW + 50 + 50 - fm.stringWidth(nombreAmarillo) / 2;
        g.drawString(nombreAmarillo, amarilloX, y + 170);
        
        // VERDE - Abajo derecha
        if (perfilJugador != null) {
            g.drawImage(perfilJugador, x + tabW + 50, y + tabH - 150, 100, 100, this);
        }
        int verdeX = x + tabW + 50 + 50 - fm.stringWidth(nombreVerde) / 2;
        g.drawString(nombreVerde, verdeX, y + tabH - 30);
    }
    
   /**
    * ✅ MODIFICADO: Dibuja las fichas usando FichaVisual (posiciones reales)
    */
    private void dibujarFichas(Graphics g) {
        if (mapaCasillas == null) return;

        // Contar fichas en cada casilla para posicionarlas correctamente
        Map<Integer, Integer> contadorPorCasilla = new HashMap<>();

        for (FichaVisual ficha : fichasEnTablero) {
            // Ignorar fichas en casa (se dibujan en las casas visuales)
            if (ficha.estaEnCasa()) {
                continue;
            }

            int casilla = ficha.getPosicionCasilla();

            // Obtener coordenadas de la casilla
            CoordenadaCasilla coord = mapaCasillas.obtenerCoordenadas(casilla);

            if (coord == null) {
                System.err.println("[ERROR] No hay coordenadas para casilla " + casilla);
                continue;
            }

            // Obtener imagen según el color
            Image imagenFicha = obtenerImagenFicha(ficha.getColor());

            if (imagenFicha == null) {
                System.err.println("[ERROR] No hay imagen para color " + ficha.getColor());
                continue;
            }

            // Determinar índice dentro de la casilla (si hay varias fichas)
            int indiceEnCasilla = contadorPorCasilla.getOrDefault(casilla, 0);
            contadorPorCasilla.put(casilla, indiceEnCasilla + 1);

            // Calcular posición en pantalla
            int fichaX = tableroOffsetX + coord.getX(indiceEnCasilla);
            int fichaY = tableroOffsetY + coord.getY(indiceEnCasilla);

            // Dibujar la ficha
            g.drawImage(imagenFicha, fichaX - 30, fichaY - 30, 60, 60, this);
        }

        // Dibujar fichas en CASA (las que tienen estaEnCasa = true)
        dibujarFichasEnCasa(g);
    }

    /**
     * ✅ MODIFICADO: Dibuja las fichas que están en CASA usando FichaVisual
     */
    private void dibujarFichasEnCasa(Graphics g) {
        // Contar cuántas fichas de cada color están en casa
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
                    // Casa ROJA (abajo-izquierda) - posiciones fijas para 4 fichas
                    switch (contadorRojo) {
                        case 0: x = 75; y = 510; break;
                        case 1: x = 75; y = 580; break;
                        case 2: x = 145; y = 510; break;
                        case 3: x = 145; y = 580; break;
                    }
                    contadorRojo++;
                    break;

                case "AZUL":
                    // Casa AZUL (arriba-izquierda)
                    switch (contadorAzul) {
                        case 0: x = 75; y = 75; break;
                        case 1: x = 75; y = 145; break;
                        case 2: x = 145; y = 75; break;
                        case 3: x = 145; y = 145; break;
                    }
                    contadorAzul++;
                    break;

                case "AMARILLO":
                    // Casa AMARILLA (arriba-derecha)
                    switch (contadorAmarillo) {
                        case 0: x = 510; y = 75; break;
                        case 1: x = 510; y = 145; break;
                        case 2: x = 580; y = 75; break;
                        case 3: x = 580; y = 145; break;
                    }
                    contadorAmarillo++;
                    break;

                case "VERDE":
                    // Casa VERDE (abajo-derecha)
                    switch (contadorVerde) {
                        case 0: x = 510; y = 510; break;
                        case 1: x = 510; y = 580; break;
                        case 2: x = 580; y = 510; break;
                        case 3: x = 580; y = 580; break;
                    }
                    contadorVerde++;
                    break;
            }

            // Dibujar ficha en casa
            g.drawImage(imagenFicha, tableroOffsetX + x, tableroOffsetY + y, 60, 60, this);
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
    * ✅ NUEVO: Saca una ficha de casa a su casilla de salida
    * @param fichaId ID de la ficha (1-16)
    */
   public void sacarFicha(int fichaId) {
       SwingUtilities.invokeLater(() -> {
           FichaVisual ficha = mapaFichas.get(fichaId);
           if (ficha == null) {
               System.err.println("[ERROR] No existe ficha con ID " + fichaId);
               return;
           }

           // Cambiar estado
           ficha.setEstaEnCasa(false);
           ficha.setPosicionCasilla(ficha.getCasillaSalida());

           System.out.println("[TableroVista] Ficha #" + fichaId + " (" + ficha.getColor() + ") sacada a casilla " + ficha.getPosicionCasilla());

           // Redibujar
           repaint();
       });
   }

   /**
    * ✅ NUEVO: Avanza una ficha UNA casilla
    * @param fichaId ID de la ficha
    */
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

           // Avanzar una casilla
           int nuevaPosicion = ficha.getPosicionCasilla() + 1;

           // TODO: Manejar lógica de pasillos y vuelta al tablero (casilla 68 → 1)
           if (nuevaPosicion > 68) {
               nuevaPosicion = 1; // Por ahora, volver al inicio
           }

           ficha.setPosicionCasilla(nuevaPosicion);

           System.out.println("[TableroVista] Ficha #" + fichaId + " avanzó a casilla " + nuevaPosicion);

           // Redibujar
           repaint();
       });
   }

   /**
    * ✅ NUEVO: Manda una ficha a casa (cuando es capturada)
    * @param fichaId ID de la ficha
    */
   public void mandarACasa(int fichaId) {
       SwingUtilities.invokeLater(() -> {
           FichaVisual ficha = mapaFichas.get(fichaId);
           if (ficha == null) {
               System.err.println("[ERROR] No existe ficha con ID " + fichaId);
               return;
           }

           // Cambiar estado
           ficha.setEstaEnCasa(true);
           ficha.setPosicionCasilla(-1);

           System.out.println("[TableroVista] Ficha #" + fichaId + " (" + ficha.getColor() + ") enviada a casa");

           // Redibujar
           repaint();
       });
   }

   /**
    * ✅ NUEVO: Mete una ficha en la meta
    * @param fichaId ID de la ficha
    */
   public void meterEnMeta(int fichaId) {
       SwingUtilities.invokeLater(() -> {
           FichaVisual ficha = mapaFichas.get(fichaId);
           if (ficha == null) {
               System.err.println("[ERROR] No existe ficha con ID " + fichaId);
               return;
           }

           // Cambiar estado
           ficha.setEstaEnMeta(true);

           // Posición de meta según color
           int posicionMeta = 0;
           switch (ficha.getColor()) {
               case "ROJO": posicionMeta = 1000; break;
               case "VERDE": posicionMeta = 2000; break;
               case "AZUL": posicionMeta = 3000; break;
               case "AMARILLO": posicionMeta = 4000; break;
           }
           ficha.setPosicionCasilla(posicionMeta);

           System.out.println("[TableroVista] Ficha #" + fichaId + " (" + ficha.getColor() + ") llegó a META");

           // Redibujar
           repaint();
       });
   }
    
    /**
     * Actualiza la vista
     */
    public void actualizarVista() {
        repaint();
    }
}