/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package parchis;

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
        // 1️⃣ Crear jugadores
        Jugador jugador1 = new Jugador(1, "Ana", ColorJugador.ROJO, "avatar1.png");
        Jugador jugador2 = new Jugador(2, "Luis", ColorJugador.AZUL, "avatar2.png");

        // 2️⃣ Crear fichas para cada jugador
        for (int i = 1; i <= 4; i++) {
            jugador1.agregarFicha(new Ficha(i, ColorJugador.ROJO));
            jugador2.agregarFicha(new Ficha(i, ColorJugador.AZUL));
        }

        // 3️⃣ Crear la partida y agregar jugadores
        Partida partida = new Partida(1, "clave123", 2);
        partida.agregarJugador(jugador1);
        partida.agregarJugador(jugador2);

        System.out.println("Partida creada con " + partida.getJugadores().size() + " jugadores.");

        // 4️⃣ Inicializar tablero
        Tablero tablero = partida.getTablero();
        System.out.println("Número de casillas en el tablero: " + tablero.getCasillas().size());

        // 5️⃣ Asignar fichas a la casilla inicial de cada jugador
        Casilla inicioRojo = tablero.getCasillaPorIndice(1); // casilla de inicio rojo
        for (Ficha f : jugador1.getFichas()) {
            inicioRojo.agregarFicha(f);
            f.setCasillaActual(inicioRojo);
        }

        Casilla inicioAzul = tablero.getCasillaPorIndice(9); // casilla de inicio azul
        for (Ficha f : jugador2.getFichas()) {
            inicioAzul.agregarFicha(f);
            f.setCasillaActual(inicioAzul);
        }

        System.out.println("Fichas inicializadas en sus casillas de salida.");

        // 6️⃣ Crear un turno para el jugador 1
        Turno turno = new Turno(jugador1);
        System.out.println("Turno creado para: " + turno.getJugadorActual().getNombre());

        // 7️⃣ Simular asignación de dados
        turno.setDados(3, 5);
        System.out.println("Dados lanzados: " + turno.getDado1() + " y " + turno.getDado2());

        // 8️⃣ Mostrar estado de una casilla
        System.out.println("Casilla 1 tiene " + inicioRojo.getFichas().size() + " fichas.");
    }
}
    

