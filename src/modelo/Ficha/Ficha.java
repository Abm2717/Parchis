/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.Ficha;

import modelo.Jugador.ColorJugador;
import modelo.Tablero.ColorCasilla;
import modelo.Tablero.Casilla;

/**
 *
 * @author a5581
 */
public class Ficha {
    // Atributos
    private int id;
    private ColorJugador color;
    private EstadoFicha estado;    // EN_CASA, EN_TABLERO, EN_META
    private Casilla casillaActual;

    public Ficha(int id, ColorJugador color) {
        this.id = id;
        this.color = color;
        this.estado = estado;
        this.casillaActual = casillaActual;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ColorJugador getColor() {
        return color;
    }

    public void setColor(ColorJugador color) {
        this.color = color;
    }

    public EstadoFicha getEstado() {
        return estado;
    }

    public void setEstado(EstadoFicha estado) {
        this.estado = estado;
    }

    public Casilla getCasillaActual() {
        return casillaActual;
    }

    public void setCasillaActual(Casilla casillaActual) {
        this.casillaActual = casillaActual;
    }
    
    
    
}
