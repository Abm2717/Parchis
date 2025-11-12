package vista;

import modelo.partida.*;
import modelo.Jugador.*;
import modelo.Ficha.*;
import modelo.Tablero.*;
import java.util.Scanner;

/**
 * Test interactivo para sistema de bonus en Parchis
 * Simula una partida real donde el jugador tira dados (con valores predefinidos)
 * y los bonus de 20 movimientos se aplican completos a UNA ficha seleccionada
 */
public class TestBonusParchis {
    
    private static Scanner scanner = new Scanner(System.in);
    private static MotorJuego motor;
    private static Partida partida;
    private static Jugador jugador1;
    private static Jugador jugador2;
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  TEST: SISTEMA DE BONUS - PARCHIS");
        System.out.println("===========================================\n");
        
        try {
            // Inicializar partida
            inicializarPartida();
            
            // Configurar escenario (fichas en posiciones especificas)
            configurarEscenario();
            
            // Turno 1: Capturar ficha rival
            turnoCaptura();
            
            // Turno 2: Usar bonus de 20 movimientos
            turnoUsarBonus();
            
            // Resumen final
            mostrarResumen();
            
            System.out.println("\nTEST COMPLETADO EXITOSAMENTE");
            
        } catch (Exception e) {
            System.err.println("\nERROR EN EL TEST:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicializa la partida con 2 jugadores
     */
    private static void inicializarPartida() {
        System.out.println("PASO 1: Inicializando partida...\n");
        
        // Crear partida
        partida = new Partida(1, "Partida Test");
        Tablero tablero = new Tablero();
        partida.setTablero(tablero);
        
        // Crear jugadores
        jugador1 = new Jugador(1, "Abraham", ColorJugador.ROJO, "avatar1.png");
        jugador2 = new Jugador(2, "Rival", ColorJugador.AZUL, "avatar2.png");
        
        // Inicializar fichas usando el metodo de Jugador
        jugador1.inicializarFichas(4);
        jugador2.inicializarFichas(4);
        
        // Agregar jugadores a partida
        partida.agregarJugador(jugador1);
        partida.agregarJugador(jugador2);
        
        // Registrar en tablero
        tablero.registrarJugador(jugador1);
        tablero.registrarJugador(jugador2);
        
        // Configurar estado
        partida.setEstado(EstadoPartida.EN_PROGRESO);
        partida.setTurnoActual(0);
        
        // Crear motor con dados fijos
        Dado dadoFijo = new Dado() {
            @Override
            public int tirar() {
                return 1; // Siempre retorna 1
            }
        };
        motor = new MotorJuego(partida, dadoFijo, dadoFijo);
        
        System.out.println("Partida creada: " + partida.getNombre());
        System.out.println("Jugador 1: " + jugador1.getNombre() + " (" + jugador1.getColor() + ")");
        System.out.println("Jugador 2: " + jugador2.getNombre() + " (" + jugador2.getColor() + ")");
        System.out.println();
    }
    
    /**
     * Configura el escenario inicial colocando fichas en posiciones especificas
     */
    private static void configurarEscenario() {
        System.out.println("PASO 2: Configurando escenario...\n");
        
        Tablero tablero = partida.getTablero();
        
        // Ficha de Abraham en casilla 9
        Ficha fichaAbraham = jugador1.getFichas().get(0);
        Casilla casilla9 = tablero.getCasilla(9);
        fichaAbraham.moverA(casilla9);
        fichaAbraham.setEstado(EstadoFicha.EN_TABLERO);
        
        // Ficha rival en casilla 10 (sera capturada)
        Ficha fichaRival = jugador2.getFichas().get(0);
        Casilla casilla10 = tablero.getCasilla(10);
        fichaRival.moverA(casilla10);
        fichaRival.setEstado(EstadoFicha.EN_TABLERO);
        
        // Otra ficha de Abraham en casilla 20 (para usar bonus)
        Ficha fichaAbraham2 = jugador1.getFichas().get(1);
        Casilla casilla20 = tablero.getCasilla(20);
        fichaAbraham2.moverA(casilla20);
        fichaAbraham2.setEstado(EstadoFicha.EN_TABLERO);
        
        System.out.println("Escenario configurado:");
        System.out.println("  - Tu ficha #1 (ROJA): casilla 9");
        System.out.println("  - Ficha rival #1 (AZUL): casilla 10");
        System.out.println("  - Tu ficha #2 (ROJA): casilla 20");
        System.out.println();
    }
    

    private static void turnoCaptura() {
        System.out.println("===========================================");
        System.out.println("  TURNO 1: CAPTURAR FICHA RIVAL");
        System.out.println("===========================================\n");
        
        System.out.println("Estado actual:");
        mostrarEstadoFichas();
        System.out.println();
        
        System.out.print("Presiona ENTER para tirar dados...");
        scanner.nextLine();
        
        MotorJuego.ResultadoDados dados = motor.tirarDados(jugador1.getId());
        System.out.println("\nDados: [" + dados.dado1 + "] [" + dados.dado2 + "] = " + dados.getSuma());
        
        if (dados.esDoble) {
            System.out.println("DOBLE! Puedes volver a tirar despues.");
        }
        System.out.println();
        
        System.out.println("Tus fichas:");
        System.out.println("  1. Ficha #1 (ROJA) - Casilla 9");
        System.out.println("  2. Ficha #2 (ROJA) - Casilla 20");
        System.out.println();
        
        System.out.print("Selecciona ficha a mover (1-2): ");
        int opcion = Integer.parseInt(scanner.nextLine().trim());
        
        Ficha fichaSeleccionada = jugador1.getFichas().get(opcion - 1);
        int casillaActual = fichaSeleccionada.getCasillaActual().getIndice();
        
        System.out.println("\nMoviendo ficha #" + fichaSeleccionada.getId() + " desde casilla " + casillaActual + "...");
        
        MotorJuego.ResultadoMovimiento resultado = motor.moverFichaConUnDado(
            jugador1.getId(),
            fichaSeleccionada.getId(),
            1
        );
        
        System.out.println("Movimiento: casilla " + resultado.casillaSalida + " -> " + resultado.casillaLlegada);
        
        if (resultado.capturaRealizada) {
            System.out.println("\n*** CAPTURA! ***");
            System.out.println("Capturaste la ficha #" + resultado.fichaCapturadaId + " del rival");
            System.out.println("BONUS GANADO: +" + resultado.bonusGanado + " movimientos");
            System.out.println("BONUS TOTAL: " + resultado.bonusTotal + " movimientos");
        }
        
        System.out.println();
    }
    
    /**
     * Turno donde se usan los 20 movimientos de bonus en UNA ficha
     */
    private static void turnoUsarBonus() {
        System.out.println("===========================================");
        System.out.println("  TURNO 2: USAR BONUS DE 20 MOVIMIENTOS");
        System.out.println("===========================================\n");
        
        int bonusDisponible = motor.getBonusDisponible(jugador1.getId());
        
        if (bonusDisponible == 0) {
            System.out.println("No tienes bonus disponibles.");
            return;
        }
        
        System.out.println("Tienes " + bonusDisponible + " movimientos de bonus disponibles");
        System.out.println("Los 20 movimientos se aplicaran completos a UNA ficha\n");
        
        System.out.println("Estado actual:");
        mostrarEstadoFichas();
        System.out.println();
        
        System.out.println("Tus fichas en tablero:");
        int contador = 1;
        for (Ficha f : jugador1.getFichas()) {
            if (f.estaEnTablero()) {
                int casilla = f.getCasillaActual().getIndice();
                System.out.println("  " + contador + ". Ficha #" + f.getId() + " (" + f.getColor() + ") - Casilla " + casilla + 
                                 " (puede avanzar a casilla " + (casilla + bonusDisponible) + ")");
                contador++;
            }
        }
        System.out.println();
        
        // Seleccionar ficha
        System.out.print("Selecciona ficha para aplicar los " + bonusDisponible + " movimientos (1-" + (contador-1) + "): ");
        int opcion = Integer.parseInt(scanner.nextLine().trim());
        
        Ficha fichaSeleccionada = null;
        contador = 1;
        for (Ficha f : jugador1.getFichas()) {
            if (f.estaEnTablero()) {
                if (contador == opcion) {
                    fichaSeleccionada = f;
                    break;
                }
                contador++;
            }
        }
        
        if (fichaSeleccionada == null) {
            System.out.println("Opcion invalida");
            return;
        }
        
        int casillaAntes = fichaSeleccionada.getCasillaActual().getIndice();
        
        System.out.println("\nAplicando " + bonusDisponible + " movimientos a ficha #" + fichaSeleccionada.getId() + "...");
        
        try {
            // Usar TODOS los movimientos bonus en la ficha seleccionada
            MotorJuego.ResultadoMovimiento resultado = motor.usarBonus(
                jugador1.getId(),
                fichaSeleccionada.getId(),
                bonusDisponible
            );
            
            System.out.println("\nBONUS APLICADO!");
            System.out.println("Movimiento: casilla " + resultado.casillaSalida + " -> " + resultado.casillaLlegada);
            System.out.println("Bonus consumido: " + resultado.bonusConsumido);
            System.out.println("Bonus restante: " + resultado.bonusRestante);
            
            if (resultado.llegadaMeta) {
                System.out.println("\n*** LLEGO A LA META! ***");
                System.out.println("Bonus de puntos: +" + resultado.bonusPuntosMeta);
            }
            
        } catch (Exception e) {
            System.out.println("\nError al usar bonus: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Muestra el estado actual de las fichas en el tablero
     */
    private static void mostrarEstadoFichas() {
        System.out.println("Fichas de " + jugador1.getNombre() + ":");
        for (Ficha f : jugador1.getFichas()) {
            if (f.estaEnTablero()) {
                System.out.println("  - Ficha #" + f.getId() + " (" + f.getColor() + "): casilla " + f.getCasillaActual().getIndice());
            } else if (f.estaEnCasa()) {
                System.out.println("  - Ficha #" + f.getId() + " (" + f.getColor() + "): EN CASA");
            } else if (f.estaEnMeta()) {
                System.out.println("  - Ficha #" + f.getId() + " (" + f.getColor() + "): EN META");
            }
        }
        
        System.out.println("\nFichas del rival:");
        for (Ficha f : jugador2.getFichas()) {
            if (f.estaEnTablero()) {
                System.out.println("  - Ficha #" + f.getId() + " (" + f.getColor() + "): casilla " + f.getCasillaActual().getIndice());
            } else if (f.estaEnCasa()) {
                System.out.println("  - Ficha #" + f.getId() + " (" + f.getColor() + "): EN CASA");
            }
        }
    }
    
    /**
     * Muestra el resumen final del test
     */
    private static void mostrarResumen() {
        System.out.println("===========================================");
        System.out.println("  RESUMEN FINAL");
        System.out.println("===========================================\n");
        
        System.out.println("Jugador: " + jugador1.getNombre());
        System.out.println("  - Puntos: " + jugador1.getPuntos());
        System.out.println("  - Bonus restantes: " + motor.getBonusDisponible(jugador1.getId()));
        System.out.println("  - Fichas en meta: " + jugador1.contarFichasEnMeta());
        System.out.println();
        
        System.out.println("Estado final de fichas:");
        for (Ficha f : jugador1.getFichas()) {
            String ubicacion;
            if (f.estaEnCasa()) {
                ubicacion = "EN CASA";
            } else if (f.estaEnMeta()) {
                ubicacion = "EN META";
            } else {
                ubicacion = "Casilla " + f.getCasillaActual().getIndice();
            }
            System.out.println("  - Ficha #" + f.getId() + " (" + f.getColor() + "): " + ubicacion);
        }
    }
}