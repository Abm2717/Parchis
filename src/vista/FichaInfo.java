package vista;

/**
 * Clase auxiliar para representar información de una ficha
 * para renderizado en TableroVista
 */
public class FichaInfo {
    
    private int id;
    private int jugadorId;
    private String color;
    private int posicionCasilla; // Índice de la casilla (1-96, o -1 si está en casa)
    private int indiceEnCasilla; // Posición dentro de la casilla (0-3)
    private String estado; // EN_CASA, EN_TABLERO, EN_META
    
    public FichaInfo(int id, int jugadorId, String color, int posicionCasilla, String estado) {
        this.id = id;
        this.jugadorId = jugadorId;
        this.color = color;
        this.posicionCasilla = posicionCasilla;
        this.estado = estado;
        this.indiceEnCasilla = 0; // Se calculará después
    }
    
    // Getters y setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getJugadorId() {
        return jugadorId;
    }
    
    public void setJugadorId(int jugadorId) {
        this.jugadorId = jugadorId;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getPosicionCasilla() {
        return posicionCasilla;
    }
    
    public void setPosicionCasilla(int posicionCasilla) {
        this.posicionCasilla = posicionCasilla;
    }
    
    public int getIndiceEnCasilla() {
        return indiceEnCasilla;
    }
    
    public void setIndiceEnCasilla(int indiceEnCasilla) {
        this.indiceEnCasilla = indiceEnCasilla;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    /**
     * Verifica si la ficha está en casa (no visible en tablero)
     */
    public boolean estaEnCasa() {
        return "EN_CASA".equals(estado) || posicionCasilla == -1;
    }
    
    /**
     * Verifica si la ficha está en meta
     */
    public boolean estaEnMeta() {
        return "EN_META".equals(estado);
    }
    
    @Override
    public String toString() {
        return String.format("Ficha[id=%d, color=%s, casilla=%d, indice=%d, estado=%s]",
            id, color, posicionCasilla, indiceEnCasilla, estado);
    }
}