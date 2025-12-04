package vista;

/**
 * ✅ ACTUALIZADO: Soporte para bordes de bonus y fichas movibles
 * - bordeBonus: true cuando la ficha puede usar bonus de captura
 * - movible: true cuando la ficha puede moverse en el turno actual
 */
public class FichaVisual {
    
    private int id;                    // ID único de la ficha (1-16)
    private int jugadorId;             // ID del jugador dueño
    private String color;              // ROJO, AZUL, VERDE, AMARILLO
    private int posicionCasilla;       // Índice de casilla actual (1-96, o -1 si en casa)
    private boolean estaEnCasa;        // true si está en casa
    private boolean estaEnMeta;        // true si llegó a meta
    
    // ✅ NUEVO: Estados visuales
    private boolean movible = false;   // true si puede moverse (borde verde)
    private boolean bordeBonus = false; // true si puede usar bonus (borde dorado)
    
    public FichaVisual(int id, int jugadorId, String color) {
        this.id = id;
        this.jugadorId = jugadorId;
        this.color = color;
        this.posicionCasilla = -1;     // Empieza en casa
        this.estaEnCasa = true;
        this.estaEnMeta = false;
    }
    
    // Getters y setters básicos
    public int getId() { return id; }
    public int getJugadorId() { return jugadorId; }
    public String getColor() { return color; }
    public int getPosicionCasilla() { return posicionCasilla; }
    public boolean estaEnCasa() { return estaEnCasa; }
    public boolean estaEnMeta() { return estaEnMeta; }
    
    public void setPosicionCasilla(int posicionCasilla) {
        this.posicionCasilla = posicionCasilla;
    }
    
    public void setEstaEnCasa(boolean estaEnCasa) {
        this.estaEnCasa = estaEnCasa;
    }
    
    public void setEstaEnMeta(boolean estaEnMeta) {
        this.estaEnMeta = estaEnMeta;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    // ✅ NUEVO: Getters y setters para estados visuales
    public boolean isMovible() {
        return movible;
    }
    
    public void setMovible(boolean movible) {
        this.movible = movible;
    }
    
    public boolean tieneBordeBonus() {
        return bordeBonus;
    }
    
    public void setBordeBonus(boolean bordeBonus) {
        this.bordeBonus = bordeBonus;
    }
    
    /**
     * Obtiene la casilla de salida según el color
     */
    public int getCasillaSalida() {
        switch (color) {
            case "ROJO": return 1;
            case "AMARILLO": return 35;
            case "AZUL": return 52;
            case "VERDE": return 18;
            default: return 1;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Ficha[id=%d, color=%s, pos=%d, casa=%b, meta=%b, movible=%b, bonus=%b]",
            id, color, posicionCasilla, estaEnCasa, estaEnMeta, movible, bordeBonus);
    }
}