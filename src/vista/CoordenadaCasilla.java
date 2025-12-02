package vista;
public class CoordenadaCasilla {
    public int x1, y1;  // Primera posición
    public int x2, y2;  // Segunda posición
    public int x3, y3;  // Tercera posición (solo para META)
    public int x4, y4;  // Cuarta posición (solo para META)

    private boolean esMeta;

    /**
     * Constructor para casillas normales (2 posiciones)
     */
    public CoordenadaCasilla(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.esMeta = false;
    }

    /**
     * Constructor para casillas META (4 posiciones)
     */
    public CoordenadaCasilla(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
        this.x4 = x4;
        this.y4 = y4;
        this.esMeta = true;
    }

    /**
     * Obtiene la posición X según el número de ficha (0, 1, 2, 3)
     */
    public int getX(int numeroFicha) {
        switch(numeroFicha) {
            case 0: return x1;
            case 1: return x2;
            case 2: return x3;
            case 3: return x4;
            default: return x1;
        }
    }

    /**
     * Obtiene la posición Y según el número de ficha (0, 1, 2, 3)
     */
    public int getY(int numeroFicha) {
        switch(numeroFicha) {
            case 0: return y1;
            case 1: return y2;
            case 2: return y3;
            case 3: return y4;
            default: return y1;
        }
    }

    public boolean esMeta() {
        return esMeta;
    }
}