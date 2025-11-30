package vista;

import java.util.HashMap;
import java.util.Map;

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
        coordenadas.put(1, new CoordenadaCasilla(157,409, 157, 441));
        
        // CASILLA 2
        coordenadas.put(2, new CoordenadaCasilla(187, 409, 187, 441));
        
        // CASILLA 3
        coordenadas.put(3, new CoordenadaCasilla(217, 409, 217, 441));
        
        // CASILLA 4
        coordenadas.put(4, new CoordenadaCasilla(248, 401, 247, 428));
        
        // CASILLA 5
        coordenadas.put(5, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 6
        coordenadas.put(6, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 7
        coordenadas.put(7, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 8: SEGURA ⭐
        coordenadas.put(8, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 9
        coordenadas.put(9, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 10
        coordenadas.put(10, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 11
        coordenadas.put(11, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 12
        coordenadas.put(12, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 13: SEGURA ⭐
        coordenadas.put(13, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 14
        coordenadas.put(14, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 15
        coordenadas.put(15, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 16
        coordenadas.put(16, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 17
        coordenadas.put(17, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 18: SALIDA VERDE ⭐
        coordenadas.put(18, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 19
        coordenadas.put(19, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 20
        coordenadas.put(20, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 21
        coordenadas.put(21, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 22
        coordenadas.put(22, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 23
        coordenadas.put(23, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 24
        coordenadas.put(24, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 25: SEGURA ⭐
        coordenadas.put(25, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 26
        coordenadas.put(26, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 27
        coordenadas.put(27, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 28
        coordenadas.put(28, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 29
        coordenadas.put(29, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 30: SEGURA ⭐
        coordenadas.put(30, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 31
        coordenadas.put(31, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 32
        coordenadas.put(32, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 33
        coordenadas.put(33, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 34
        coordenadas.put(34, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 35: SALIDA AMARILLA ⭐
        coordenadas.put(35, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 36
        coordenadas.put(36, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 37
        coordenadas.put(37, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 38
        coordenadas.put(38, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 39
        coordenadas.put(39, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 40
        coordenadas.put(40, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 41
        coordenadas.put(41, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 42: SEGURA ⭐
        coordenadas.put(42, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 43
        coordenadas.put(43, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 44
        coordenadas.put(44, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 45
        coordenadas.put(45, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 46
        coordenadas.put(46, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 47: SEGURA ⭐
        coordenadas.put(47, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 48
        coordenadas.put(48, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 49
        coordenadas.put(49, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 50
        coordenadas.put(50, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 51
        coordenadas.put(51, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 52: SALIDA AZUL ⭐
        coordenadas.put(52, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 53
        coordenadas.put(53, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 54
        coordenadas.put(54, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 55
        coordenadas.put(55, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 56
        coordenadas.put(56, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 57
        coordenadas.put(57, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 58
        coordenadas.put(58, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 59: SEGURA ⭐
        coordenadas.put(59, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 60
        coordenadas.put(60, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 61
        coordenadas.put(61, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 62
        coordenadas.put(62, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 63
        coordenadas.put(63, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 64: SEGURA ⭐
        coordenadas.put(64, new CoordenadaCasilla(0, 0, 0, 0));
        
        // CASILLA 65
        coordenadas.put(65, new CoordenadaCasilla(37, 409, 37, 441));
        
        // CASILLA 66
        coordenadas.put(66, new CoordenadaCasilla(67, 409, 67, 441));
        
        // CASILLA 67
        coordenadas.put(67, new CoordenadaCasilla(97, 409, 125, 441));
        
        // CASILLA 68
        coordenadas.put(68, new CoordenadaCasilla(128, 409, 128, 441));
        
        // ================================================================
        // METAS (4 fichas cada una)
        // ================================================================
        
        // META ROJA (4 fichas en formación 2x2)
        coordenadas.put(META_ROJA, new CoordenadaCasilla(
            0, 0,    // Ficha 1
            0, 0,    // Ficha 2
            0, 0,    // Ficha 3
            0, 0     // Ficha 4
        ));
        
        // META VERDE (4 fichas en formación 2x2)
        coordenadas.put(META_VERDE, new CoordenadaCasilla(
            0, 0,    // Ficha 1
            0, 0,    // Ficha 2
            0, 0,    // Ficha 3
            0, 0     // Ficha 4
        ));
        
        // META AMARILLA (4 fichas en formación 2x2)
        coordenadas.put(META_AMARILLA, new CoordenadaCasilla(
            0, 0,    // Ficha 1
            0, 0,    // Ficha 2
            0, 0,    // Ficha 3
            0, 0     // Ficha 4
        ));
        
        // META AZUL (4 fichas en formación 2x2)
        coordenadas.put(META_AZUL, new CoordenadaCasilla(
            0, 0,    // Ficha 1
            0, 0,    // Ficha 2
            0, 0,    // Ficha 3
            0, 0     // Ficha 4
        ));
    }
    
    public CoordenadaCasilla obtenerCoordenadas(int indiceCasilla) {
        return coordenadas.get(indiceCasilla);
    }
    
    public boolean existeCasilla(int indiceCasilla) {
        return coordenadas.containsKey(indiceCasilla);
    }
}