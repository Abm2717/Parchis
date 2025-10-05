/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.partida;

import modelo.Jugador.Jugador;

/**
 *
 * @author a5581
 */
public class Turno {
    private Jugador jugadorActual;
    private int dado1;
    private int dado2;
    private boolean dadosLanzados;
    private boolean turnoTerminado;

    // Constructor
    public Turno(Jugador jugadorActual) {
        this.jugadorActual = jugadorActual;
        this.dadosLanzados = false;
        this.turnoTerminado = false;
    }

    public Jugador getJugadorActual() {
        return jugadorActual;
    }

    public int getDado1() {
        return dado1;
    }

    public int getDado2() {
        return dado2;
    }

    public boolean isDadosLanzados() {
        return dadosLanzados;
    }

    public boolean isTurnoTerminado() {
        return turnoTerminado;
    }
    
    public void setDados(int dado1, int dado2) {
        this.dado1 = dado1;
        this.dado2 = dado2;
        this.dadosLanzados = true;
    }
    
    public void terminarTurno() {
        this.turnoTerminado = true;
    }
}
