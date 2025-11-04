package modelo.Ficha;

import modelo.Jugador.ColorJugador;
import modelo.Tablero.Casilla;

public class Ficha {
    private int id;
    private int idJugador;  // ✅ NUEVO: para identificar al dueño
    private ColorJugador color;
    private EstadoFicha estado;
    private Casilla casillaActual;

    public Ficha(int id, int idJugador, ColorJugador color) {
        this.id = id;
        this.idJugador = idJugador;
        this.color = color;
        this.estado = EstadoFicha.EN_CASA;
        this.casillaActual = null;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getIdJugador() { return idJugador; }  // ✅ NUEVO
    public void setIdJugador(int idJugador) { this.idJugador = idJugador; }
    
    public ColorJugador getColor() { return color; }
    public EstadoFicha getEstado() { return estado; }
    public void setEstado(EstadoFicha estado) { this.estado = estado; }
    public Casilla getCasillaActual() { return casillaActual; }
    
    /**
     * ✅ MEJORADO: Actualiza casilla actual y maneja la relación bidireccional.
     */
    public void setCasillaActual(Casilla casilla) {
        // Remover de casilla anterior si existe
        if (this.casillaActual != null) {
            this.casillaActual.removerFicha(this);
        }
        
        // Asignar nueva casilla
        this.casillaActual = casilla;
        
        // Agregar a nueva casilla si no es null
        if (casilla != null) {
            casilla.agregarFicha(this);
        }
    }

    /**
     * ✅ MEJORADO: Mueve la ficha a una nueva casilla y actualiza estado.
     */
    public void moverA(Casilla destino) {
        setCasillaActual(destino);
        
        if (destino == null) {
            this.estado = EstadoFicha.EN_CASA;
        } else {
            // Detectar si llegó a meta (casillas de tipo META)
            if (destino.getTipo() == modelo.Tablero.TipoCasilla.META) {
                this.estado = EstadoFicha.EN_META;
            } else {
                this.estado = EstadoFicha.EN_TABLERO;
            }
        }
    }

    public boolean estaEnCasa() {
        return this.estado == EstadoFicha.EN_CASA;
    }

    public boolean estaEnMeta() {
        return this.estado == EstadoFicha.EN_META;
    }

    public boolean estaEnTablero() {
        return this.estado == EstadoFicha.EN_TABLERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ficha)) return false;
        Ficha f = (Ficha) o;
        return this.id == f.id && this.idJugador == f.idJugador;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(id);
        result = 31 * result + Integer.hashCode(idJugador);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Ficha[id=%d, jugador=%d, color=%s, estado=%s]",
            id, idJugador, color, estado);
    }
}

