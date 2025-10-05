/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.partida;

import modelo.Jugador.Jugador;
import modelo.Tablero.Tablero;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author a5581
 */
public class Partida {
    private int id;
    private String claveAcceso;
    private List<Jugador> jugadores;
    private Tablero tablero;
    private boolean iniciada;
    private int maxJugadores;

    public Partida(int id, String claveAcceso, int maxJugadores) {
        this.id = id;
        this.claveAcceso = claveAcceso;
        this.jugadores = new ArrayList<>();
        this.tablero = new Tablero();
        this.iniciada = false;
        this.maxJugadores = maxJugadores;
    }

    public int getId() {
        return id;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public Tablero getTablero() {
        return tablero;
    }

    public boolean isIniciada() {
        return iniciada;
    }

    public void setIniciada(boolean iniciada) {
        this.iniciada = iniciada;
    }

    public void setMaxJugadores(int maxJugadores) {
        this.maxJugadores = maxJugadores;
    }
        
    public boolean agregarJugador(Jugador jugador) {
        if(jugadores.size() < maxJugadores) {
            jugadores.add(jugador);
            return true;
        }
        return false;
    }

    public boolean estaLlena() {
        return jugadores.size() >= maxJugadores;
    }

    public void iniciarPartida() {
        if(!iniciada && jugadores.stream().allMatch(Jugador::isListo)) {
            iniciada = true;
        }
    }
}
