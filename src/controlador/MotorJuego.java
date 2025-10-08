/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controlador;

import java.util.List;
import java.util.Random;
import modelo.Ficha.Ficha;
import modelo.Jugador.Jugador;
import modelo.Tablero.Casilla;
import modelo.Tablero.Tablero;
import modelo.partida.Partida;
import modelo.partida.Turno;

/**
 *
 * @author a5581
 */
public class MotorJuego {
    private Partida partida;
    private Turno turnoActual;
    private Random random;

    // Constructor
    public MotorJuego(Partida partida) {
        this.partida = partida;
        this.random = new Random();
    }

    // Inicializar la partida
    public void iniciarPartida() {
        if (!partida.estaLlena()) {
            throw new IllegalStateException("No se puede iniciar: la partida no esta completa.");
        }
        partida.setIniciada(true);
        iniciarTurno(partida.getJugadores().get(0));
    }

    // Iniciar un turno
    public void iniciarTurno(Jugador jugador) {
        this.turnoActual = new Turno(jugador);
        System.out.println("Turno de: " + jugador.getNombre());
    }

    // Lanzar los dados
    public void lanzarDados() {
        if (turnoActual == null) throw new IllegalStateException("No hay turno activo.");
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        turnoActual.setDados(d1, d2);
        System.out.println("Dados: " + d1 + " y " + d2);
    }

    // Cambiar de turno
    public void cambiarTurno() {
        turnoActual.terminarTurno();
        Jugador siguiente = obtenerSiguienteJugador();
        iniciarTurno(siguiente);
    }

    // Metodos auxiliares
    private Casilla calcularDestino(Casilla origen, int pasos) {
        // TODO: logica para encontrar la siguiente casilla (segun tablero)
        return null;
    }

    private void verificarEventos(Casilla destino, Ficha ficha) {
        // TODO: validar si la ficha comio otra, si es casilla segura, si llego a meta, etc.
    }

    private Jugador obtenerSiguienteJugador() {
        List<Jugador> jugadores = partida.getJugadores();
        int index = jugadores.indexOf(turnoActual.getJugadorActual());
        return jugadores.get((index + 1) % jugadores.size());
    }
    
    public void moverFicha(Ficha ficha, int avance) {
        Tablero tablero = partida.getTablero();
        Casilla actual = ficha.getCasillaActual();
        int nuevaPos = actual.getIndice() + avance;
        if (nuevaPos > tablero.getNumeroCasillas()) {
            nuevaPos -= tablero.getNumeroCasillas();
        }
        Casilla destino = tablero.getCasillaPorIndice(nuevaPos);
        actual.removerFicha(ficha);
        destino.agregarFicha(ficha);
        ficha.setCasillaActual(destino);
    }

    // ===== Nuevo metodo: mostrar estado del tablero =====
    public void mostrarEstadoTablero() {
        Tablero tablero = partida.getTablero();
        System.out.println("\n--- Estado del Tablero ---");
        for (Casilla c : tablero.getCasillas()) {
            if (c.getFichas().size() > 0) {
                System.out.print("Casilla " + c.getIndice() + " [" + c.getTipo() + "]: ");
                for (Ficha f : c.getFichas()) {
                    System.out.print(f.getColor() + " ");
                }
                System.out.println("(" + c.getFichas().size() + " fichas)");
            }
        }
        System.out.println("---------------------------\n");
    }
    
     public Ficha buscarFicha(int fichaId){
        for(Jugador jugador : partida.getJugadores()){
            for(Ficha f : jugador.getFichas()){
                if(f.getId() == fichaId){
                    return f;
                }
            }
        }
        return null; // si no se encuentra
    }

}
