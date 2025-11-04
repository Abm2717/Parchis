package modelo.Ficha;

import java.io.Serializable;
import modelo.Jugador.ColorJugador;
import modelo.Tablero.Casilla;

/**
 * Objeto de transferencia de datos para Ficha.
 */
public class FichaDTO implements Serializable {
    private int id;
    private ColorJugador color;
    private EstadoFicha estado;
    private int posicionCasilla; // índice o identificador de la casilla actual

    public FichaDTO(int id, ColorJugador color, EstadoFicha estado, int posicionCasilla) {
        this.id = id;
        this.color = color;
        this.estado = estado;
        this.posicionCasilla = posicionCasilla;
    }

    // Constructor a partir de una Ficha del modelo
    public FichaDTO(Ficha ficha) {
        this(ficha.getId(), ficha.getColor(), ficha.getEstado(),
             ficha.getCasillaActual() != null ? ficha.getCasillaActual().getIndice() : -1);
    }

    public int getId() { return id; }
    public ColorJugador getColor() { return color; }
    public EstadoFicha getEstado() { return estado; }
    public int getPosicionCasilla() { return posicionCasilla; }
}
