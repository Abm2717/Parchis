package modelo.Tablero;

import java.util.ArrayList;
import java.util.List;
import modelo.Ficha.Ficha;

public class Casilla {
    private int indice;
    private int posicion;
    private ColorCasilla color;
    private TipoCasilla tipo;
    private int capacidad;
    private boolean bloqueada;
    private List<Ficha> fichas;

    public Casilla(int indice, int posicion, ColorCasilla color, TipoCasilla tipo, int capacidad) {
        this.indice = indice;
        this.posicion = posicion;
        this.color = color;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.bloqueada = false;
        this.fichas = new ArrayList<>();
    }

    // Getters y setters originales
    public int getIndice() { return indice; }
    public void setIndice(int indice) { this.indice = indice; }
    public int getPosicion() { return posicion; }
    public void setPosicion(int posicion) { this.posicion = posicion; }
    public ColorCasilla getColor() { return color; }
    public TipoCasilla getTipo() { return tipo; }
    public int getCapacidad() { return capacidad; }
    public boolean isBloqueada() { return bloqueada; }
    public void setBloqueada(boolean bloqueada) { this.bloqueada = bloqueada; }
    public List<Ficha> getFichas() { return fichas; }

    // ✅ Métodos requeridos por MotorJuego
    
    /**
     * Verifica si esta casilla es segura (no permite capturas).
     */
    public boolean isSegura() {
        return tipo == TipoCasilla.SEGURA || tipo == TipoCasilla.INICIO;
    }

    /**
     * Agrega una ficha a esta casilla.
     */
    public void agregarFicha(Ficha ficha) {
        if (ficha == null) return;
        
        fichas.add(ficha);
        
        // Actualizar estado de bloqueo si hay 2+ fichas del mismo jugador
        actualizarBloqueo();
    }

    /**
     * Remueve una ficha de esta casilla.
     */
    public void removerFicha(Ficha ficha) {
        fichas.remove(ficha);
        actualizarBloqueo();
    }

    /**
     * Limpia todas las fichas de esta casilla.
     */
    public void limpiarFichas() {
        fichas.clear();
        bloqueada = false;
    }

    /**
     * Actualiza el estado de bloqueo según las fichas presentes.
     */
    private void actualizarBloqueo() {
        if (fichas.size() < 2) {
            bloqueada = false;
            return;
        }

        // Verificar si hay 2+ fichas del mismo jugador
        java.util.Map<Integer, Long> fichasPorJugador = fichas.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Ficha::getIdJugador, 
                java.util.stream.Collectors.counting()
            ));

        bloqueada = fichasPorJugador.values().stream().anyMatch(count -> count >= 2);
    }

    /**
     * Obtiene el número de fichas de un jugador específico en esta casilla.
     */
    public int contarFichasDeJugador(int jugadorId) {
        return (int) fichas.stream()
            .filter(f -> f.getIdJugador() == jugadorId)
            .count();
    }

    /**
     * Verifica si esta casilla tiene capacidad para más fichas.
     */
    public boolean tieneEspacio() {
        return fichas.size() < capacidad;
    }

    @Override
    public String toString() {
        return String.format("Casilla[idx=%d, tipo=%s, fichas=%d, bloqueada=%s]",
            indice, tipo, fichas.size(), bloqueada);
    }
}
