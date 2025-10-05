/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package parchis;

import controlador.MotorJuego;
import java.util.Random;
import modelo.Ficha.Ficha;
import modelo.Jugador.ColorJugador;
import modelo.Jugador.Jugador;
import modelo.Tablero.Casilla;
import modelo.Tablero.Tablero;
import modelo.partida.Partida;
import modelo.partida.Turno;

/**
 *
 * @author a5581
 */
public class Parchis {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("=== Simulacion Parchis - 2 Jugadores ===");

        // CrearPartida
        Partida partida = new Partida(1, "ABC123", 2);
        MotorJuego motor = new MotorJuego(partida);

        System.out.println("Partida creada con clave: " + partida.getClaveAcceso());

        // CrearJugafores
        Jugador jugador1 = new Jugador(1, "Ana", ColorJugador.ROJO, "avatar1.png");
        Jugador jugador2 = new Jugador(2, "Luis", ColorJugador.AZUL, "avatar2.png");

        // Agregar fichas a los jugadores
        for (int i = 1; i <= 4; i++) {
            jugador1.agregarFicha(new Ficha(i, ColorJugador.ROJO));
            jugador2.agregarFicha(new Ficha(i, ColorJugador.AZUL));
        }

        // Agregar jugadores a la partida
        partida.agregarJugador(jugador1);
        partida.agregarJugador(jugador2);

        // Inicializar tablero
        Tablero tablero = partida.getTablero();
        tablero.inicializarCasillas();
        System.out.println("Numero de casillas en el tablero: " + tablero.getNumeroCasillas());

        //Asignar fichas a la casilla inicial de cada jugador
        Casilla inicioRojo = tablero.getCasillaPorIndice(1);
        Casilla inicioAzul = tablero.getCasillaPorIndice(18);

        for (Ficha f : jugador1.getFichas()) {
            inicioRojo.agregarFicha(f);
            f.setCasillaActual(inicioRojo);
        }
        for (Ficha f : jugador2.getFichas()) {
            inicioAzul.agregarFicha(f);
            f.setCasillaActual(inicioAzul);
        }

        System.out.println("Fichas inicializadas en casillas de inicio.");

        Random rand = new Random();

        // ===== Turno de Ana =====
        System.out.println("\n--- Turno de Ana ---");
        Turno turnoAna = new Turno(jugador1);
        int dado1 = rand.nextInt(6) + 1;
        int dado2 = rand.nextInt(6) + 1;
        turnoAna.setDados(dado1, dado2);
        System.out.println(jugador1.getNombre() + " lanza los dados: " + dado1 + " y " + dado2);

        // Mover primera ficha
        Ficha fichaAna = jugador1.getFichas().get(0);
        motor.moverFicha(fichaAna, dado1 + dado2);
        turnoAna.terminarTurno();

        // ===== Turno de Luis =====
        System.out.println("\n--- Turno de Luis ---");
        Turno turnoLuis = new Turno(jugador2);
        int dado3 = rand.nextInt(6) + 1;
        int dado4 = rand.nextInt(6) + 1;
        turnoLuis.setDados(dado3, dado4);
        System.out.println(jugador2.getNombre() + " lanza los dados: " + dado3 + " y " + dado4);

        // Mover primera ficha
        Ficha fichaLuis = jugador2.getFichas().get(0);
        motor.moverFicha( fichaLuis, dado3 + dado4);
        turnoLuis.terminarTurno();
        
        
        motor.mostrarEstadoTablero();
    }
    
    
}
    

