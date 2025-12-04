package vista;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MapaCasillas {
    
    private int offsetX;
    private int offsetY;
    private Map<Integer, CoordenadaCasilla> coordenadas;
    
    // Índices especiales para METAS
    public static final int META_ROJA = 1000;
    public static final int META_VERDE = 2000;
    public static final int META_AZUL = 3000;
    public static final int META_AMARILLA = 4000;
    
    public MapaCasillas(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.coordenadas = new HashMap<>();
        
        inicializarCoordenadas();
    }
    
    private void inicializarCoordenadas() {
        
        // ================================================================
        // CASILLAS NORMALES (1-68)
        // ================================================================
        
        // CASILLA 1: SALIDA ROJA ⭐
        coordenadas.put(1, new CoordenadaCasilla(129 ,382, 129, 420));
        
        // CASILLA 2
        coordenadas.put(2, new CoordenadaCasilla(159, 382, 159, 420));
        
        // CASILLA 3
        coordenadas.put(3, new CoordenadaCasilla(189, 382, 189, 420));
        
        // CASILLA 4
        coordenadas.put(4, new CoordenadaCasilla(219, 382, 219, 420));
        
        // CASILLA 5
        coordenadas.put(5, new CoordenadaCasilla(222, 422, 266, 422));
        
        // CASILLA 6
        coordenadas.put(6, new CoordenadaCasilla(222, 452, 266, 452));
        
        // CASILLA 7
        coordenadas.put(7, new CoordenadaCasilla(222, 482, 266, 482));
        
        // CASILLA 8: SEGURA ⭐  
        coordenadas.put(8, new CoordenadaCasilla(222, 512, 266, 512)); //44
        
        // CASILLA 9
        coordenadas.put(9, new CoordenadaCasilla(222, 542, 266, 542));
        
        // CASILLA 10
        coordenadas.put(10, new CoordenadaCasilla(222, 572, 266, 572));
        
        // CASILLA 11
        coordenadas.put(11, new CoordenadaCasilla(222, 602, 266, 602));
        
        // CASILLA 12
        coordenadas.put(12, new CoordenadaCasilla(222, 632, 266, 632));
        
        // CASILLA 13: SEGURA ⭐
        coordenadas.put(13, new CoordenadaCasilla(302, 632, 342, 632));
        
        // CASILLA 14
        coordenadas.put(14, new CoordenadaCasilla(378, 632, 419, 632));
        
        // CASILLA 15
        coordenadas.put(15, new CoordenadaCasilla(378, 602, 419, 602));
        
        // CASILLA 16
        coordenadas.put(16, new CoordenadaCasilla(378, 572, 419, 572));
        
        // CASILLA 17
        coordenadas.put(17, new CoordenadaCasilla(378, 542, 419, 542));
        
        // CASILLA 18: SALIDA VERDE ⭐
        coordenadas.put(18, new CoordenadaCasilla(378, 512, 419, 512));
        
        // CASILLA 19
        coordenadas.put(19, new CoordenadaCasilla(378, 482, 419, 482));
        
        // CASILLA 20
        coordenadas.put(20, new CoordenadaCasilla(378, 452, 419, 452));
        
        // CASILLA 21
        coordenadas.put(21, new CoordenadaCasilla(378, 422, 419, 422));
        
        // CASILLA 22
        coordenadas.put(22, new CoordenadaCasilla(422, 382, 422, 420));
        
        // CASILLA 23
        coordenadas.put(23, new CoordenadaCasilla(452, 382, 452, 420));
        
        // CASILLA 24
        coordenadas.put(24, new CoordenadaCasilla(482, 382, 482, 420));
        
        // CASILLA 25: SEGURA ⭐
        coordenadas.put(25, new CoordenadaCasilla(512, 382, 512, 420));
        
        // CASILLA 26
        coordenadas.put(26, new CoordenadaCasilla(542, 382, 542, 420));
        
        // CASILLA 27
        coordenadas.put(27, new CoordenadaCasilla(572, 382, 572, 420));
        
        // CASILLA 28
        coordenadas.put(28, new CoordenadaCasilla(602, 382, 602, 420));
        
        // CASILLA 29
        coordenadas.put(29, new CoordenadaCasilla(632, 382, 632, 420));
        
        // CASILLA 30: SEGURA ⭐
        coordenadas.put(30, new CoordenadaCasilla(632, 300, 632, 338));
        
        // CASILLA 31
        coordenadas.put(31, new CoordenadaCasilla(632, 266, 632, 228));
        
        // CASILLA 32
        coordenadas.put(32, new CoordenadaCasilla(602, 266, 602, 228));
        
        // CASILLA 33
        coordenadas.put(33, new CoordenadaCasilla(572, 266, 572, 228));
        
        // CASILLA 34
        coordenadas.put(34, new CoordenadaCasilla(542, 266, 542, 0));
        
        // CASILLA 35: SALIDA AMARILLA ⭐
        coordenadas.put(35, new CoordenadaCasilla(512, 266, 512, 228));
        
        // CASILLA 36
        coordenadas.put(36, new CoordenadaCasilla(482, 266, 482, 228));
        
        // CASILLA 37
        coordenadas.put(37, new CoordenadaCasilla(452, 266, 452, 228));
        
        // CASILLA 38
        coordenadas.put(38, new CoordenadaCasilla(422, 266, 422, 228));
        
        // CASILLA 39
        coordenadas.put(39, new CoordenadaCasilla(378, 219, 415, 219));
        
        // CASILLA 40
        coordenadas.put(40, new CoordenadaCasilla(378, 189, 419, 189));
        
        // CASILLA 41
        coordenadas.put(41, new CoordenadaCasilla(378, 159, 419, 159));
        
        // CASILLA 42: SEGURA ⭐
        coordenadas.put(42, new CoordenadaCasilla(378, 129, 419, 219));
        
        // CASILLA 43
        coordenadas.put(43, new CoordenadaCasilla(378, 99, 419, 99));
        
        // CASILLA 44
        coordenadas.put(44, new CoordenadaCasilla(378, 69, 419, 69));
        
        // CASILLA 45
        coordenadas.put(45, new CoordenadaCasilla(378, 39, 419, 39));
        
        // CASILLA 46
        coordenadas.put(46, new CoordenadaCasilla(378, 9, 419, 9));
        
        // CASILLA 47: SEGURA ⭐
        coordenadas.put(47, new CoordenadaCasilla(302, 9, 342, 9));
        
        // CASILLA 48
        coordenadas.put(48, new CoordenadaCasilla(222, 9, 263, 9));
        
        // CASILLA 49
        coordenadas.put(49, new CoordenadaCasilla(222, 39, 263, 39));
        
        // CASILLA 50
        coordenadas.put(50, new CoordenadaCasilla(222, 69, 263, 69));
        
        // CASILLA 51
        coordenadas.put(51, new CoordenadaCasilla(222, 99, 263, 99));
        
        // CASILLA 52: SALIDA AZUL ⭐
        coordenadas.put(52, new CoordenadaCasilla(222, 129, 263, 129));
        
        // CASILLA 53
        coordenadas.put(53, new CoordenadaCasilla(222, 159, 263, 159));
        
        // CASILLA 54
        coordenadas.put(54, new CoordenadaCasilla(222, 189, 263, 189));
        
        // CASILLA 55
        coordenadas.put(55, new CoordenadaCasilla(228, 219, 263, 219));
        
        // CASILLA 56
        coordenadas.put(56, new CoordenadaCasilla(219, 266, 219, 228));
        
        // CASILLA 57
        coordenadas.put(57, new CoordenadaCasilla(189, 266, 189, 228));
        
        // CASILLA 58
        coordenadas.put(58, new CoordenadaCasilla(159, 266, 159, 228));
        
        // CASILLA 59: SEGURA ⭐
        coordenadas.put(59, new CoordenadaCasilla(129, 266, 129, 228));
        
        // CASILLA 60
        coordenadas.put(60, new CoordenadaCasilla(99, 266, 99, 228));
        
        // CASILLA 61
        coordenadas.put(61, new CoordenadaCasilla(69, 266, 69, 228));
        
        // CASILLA 62
        coordenadas.put(62, new CoordenadaCasilla(39, 266, 39, 228));
        
        // CASILLA 63
        coordenadas.put(63, new CoordenadaCasilla(9, 266, 9, 228));
        
        // CASILLA 64: SEGURA ⭐
        coordenadas.put(64, new CoordenadaCasilla(9, 300, 9, 338));
        
        // CASILLA 65
        coordenadas.put(65, new CoordenadaCasilla(9, 382, 9, 420));
        
        // CASILLA 66
        coordenadas.put(66, new CoordenadaCasilla(39, 382, 39, 420));
        
        // CASILLA 67
        coordenadas.put(67, new CoordenadaCasilla(69, 382, 69, 420));
        
        // CASILLA 68
        coordenadas.put(68, new CoordenadaCasilla(99, 382, 99, 420));
        
        // ================================================================
        // PASILLOS HACIA META (7 casillas cada uno)
        // ================================================================

        // ----------------------------------------------------------------
        // PASILLO ROJO (69-75): De izquierda hacia el CENTRO (horizontal →)
        // Entrada: después de casilla 68
        // ----------------------------------------------------------------

        // CASILLA 69: Primera del pasillo rojo
        coordenadas.put(69, new CoordenadaCasilla(29 ,300, 29, 338));

        // CASILLA 70
        coordenadas.put(70, new CoordenadaCasilla(69, 300, 69, 338));

        // CASILLA 71
        coordenadas.put(71, new CoordenadaCasilla(99, 300, 99, 338));

        // CASILLA 72
        coordenadas.put(72, new CoordenadaCasilla(129 ,300, 129, 338));

        // CASILLA 73
        coordenadas.put(73, new CoordenadaCasilla(159, 300, 159, 338));

        // CASILLA 74
        coordenadas.put(74, new CoordenadaCasilla(189, 300, 189, 338));

        // CASILLA 75: Última del pasillo rojo (antes de meta)
        coordenadas.put(75, new CoordenadaCasilla(219, 300, 219, 338));

        // ----------------------------------------------------------------
        // PASILLO VERDE (76-82): De abajo hacia el CENTRO (vertical ↑)
        // Entrada: después de casilla 17
        // ----------------------------------------------------------------

        // CASILLA 76: Primera del pasillo verde
        coordenadas.put(76, new CoordenadaCasilla(302, 602, 342, 602));

        // CASILLA 77
        coordenadas.put(77, new CoordenadaCasilla(302, 572, 342, 572));

        // CASILLA 78
        coordenadas.put(78, new CoordenadaCasilla(302, 542, 342, 542));

        // CASILLA 79
        coordenadas.put(79, new CoordenadaCasilla(302, 512, 342, 512));

        // CASILLA 80
        coordenadas.put(80, new CoordenadaCasilla(302, 482, 342, 482));

        // CASILLA 81
        coordenadas.put(81, new CoordenadaCasilla(302, 452, 342, 452));

        // CASILLA 82: Última del pasillo verde (antes de meta)
        coordenadas.put(82, new CoordenadaCasilla(302, 422, 342, 422));

        // ----------------------------------------------------------------
        // PASILLO AMARILLO (83-89): De derecha hacia el CENTRO (vertical ↓)
        // Entrada: después de casilla 34
        // ----------------------------------------------------------------

        // CASILLA 83: Primera del pasillo amarillo
        coordenadas.put(83, new CoordenadaCasilla(602, 300, 452, 338));

        // CASILLA 84
        coordenadas.put(84, new CoordenadaCasilla(572, 300, 422, 338));

        // CASILLA 85
        coordenadas.put(85, new CoordenadaCasilla(542, 300, 392, 338));

        // CASILLA 86
        coordenadas.put(86, new CoordenadaCasilla(512, 300, 362, 338));

        // CASILLA 87
        coordenadas.put(87, new CoordenadaCasilla(482, 300, 332, 338));

        // CASILLA 88
        coordenadas.put(88, new CoordenadaCasilla(452, 300, 302, 338));

        // CASILLA 89: Última del pasillo amarillo (antes de meta)
        coordenadas.put(89, new CoordenadaCasilla(422, 300, 272, 338));

        // ----------------------------------------------------------------
        // PASILLO AZUL (90-96): De arriba hacia el CENTRO (horizontal ←)
        // Entrada: después de casilla 51
        // ----------------------------------------------------------------

        // CASILLA 90: Primera del pasillo azul
        coordenadas.put(90, new CoordenadaCasilla(300, 39, 341, 39));

        // CASILLA 91
        coordenadas.put(91, new CoordenadaCasilla(300, 69, 341, 69));

        // CASILLA 92
        coordenadas.put(92, new CoordenadaCasilla(300, 99, 341, 99));

        // CASILLA 93
        coordenadas.put(93, new CoordenadaCasilla(300, 129, 341, 129));

        // CASILLA 94
        coordenadas.put(94, new CoordenadaCasilla(300, 159, 341, 159));

        // CASILLA 95
        coordenadas.put(95, new CoordenadaCasilla(300, 189, 341, 189));

        // CASILLA 96: Última del pasillo azul (antes de meta)
        coordenadas.put(96, new CoordenadaCasilla(300, 219, 341, 219));
        
        // ================================================================
        // METAS (4 fichas cada una)
        // ================================================================
        
        // META ROJA (4 fichas en formación 2x2)
        coordenadas.put(META_ROJA, new CoordenadaCasilla(
            295, 319,    // Ficha 1
            274, 295,    // Ficha 2
            274, 343,    // Ficha 3
            254, 319     // Ficha 4
        ));
        
        // META VERDE (4 fichas en formación 2x2)
        coordenadas.put(META_VERDE, new CoordenadaCasilla(
            324, 343,    // Ficha 1
            292, 368,    // Ficha 2
            352, 368,    // Ficha 3
            324, 390     // Ficha 4
        ));
        
        // META AMARILLA (4 fichas en formación 2x2)
        coordenadas.put(META_AMARILLA, new CoordenadaCasilla(
            345, 319,    // Ficha 1
            372, 295,    // Ficha 2
            372, 343,    // Ficha 3
            390, 319     // Ficha 4
        ));
        
        // META AZUL (4 fichas en formación 2x2)
        coordenadas.put(META_AZUL, new CoordenadaCasilla(
            324, 295,    // Ficha 1
            292, 265,    // Ficha 2
            352, 265,    // Ficha 3
            324, 250     // Ficha 4
        ));
    }
    
    public CoordenadaCasilla obtenerCoordenadas(int indiceCasilla) {
        return coordenadas.get(indiceCasilla);
    }
    
    public boolean existeCasilla(int indiceCasilla) {
        return coordenadas.containsKey(indiceCasilla);
    }
    public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        JFrame frame = new JFrame("Probador de MapaCasillas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 900);

        JPanel panel = new JPanel() {
            MapaCasillas mapa = new MapaCasillas(0, 0);
            int casillaActual = 1;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                g.setFont(new Font("Arial", Font.BOLD, 12));

                // Dibujar TODAS las casillas
                for (int i = 1; i <= 96; i++) {
                    CoordenadaCasilla c = mapa.obtenerCoordenadas(i);
                    if (c == null) continue;

                    int x = c.getX(0);
                    int y = c.getY(0);

                    g.setColor(Color.LIGHT_GRAY);
                    g.fillRect(x - 10, y - 10, 20, 20);

                    g.setColor(Color.BLACK);
                    g.drawRect(x - 10, y - 10, 20, 20);
                    g.drawString(String.valueOf(i), x - 5, y - 15);
                }

                // Dibujar casilla ACTUAL en ROJO
                CoordenadaCasilla c = mapa.obtenerCoordenadas(casillaActual);
                if (c != null) {
                    int x = c.getX(0);
                    int y = c.getY(0);

                    g.setColor(Color.RED);
                    g.fillOval(x - 8, y - 8, 16, 16);

                    g.drawString("CASILLA " + casillaActual + 
                                 " → (" + x + "," + y + ")", 
                                 20, 20);
                }
            }

            {
                // Teclas para navegar casillas
                setFocusable(true);
                addKeyListener(new java.awt.event.KeyAdapter() {
                    @Override
                    public void keyPressed(java.awt.event.KeyEvent e) {
                        if (e.getKeyCode() == java.awt.event.KeyEvent.VK_RIGHT) {
                            if (casillaActual < 96) casillaActual++;
                        } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_LEFT) {
                            if (casillaActual > 1) casillaActual--;
                        }
                        CoordenadaCasilla c = mapa.obtenerCoordenadas(casillaActual);
                        if (c != null) {
                            System.out.println("Casilla " + casillaActual +
                                ": X=" + c.getX(0) + "  Y=" + c.getY(0));
                        }
                        repaint();
                    }
                });
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    });
}

}