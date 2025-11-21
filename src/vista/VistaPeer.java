package vista;

import controlador.peer.ClientePeer;
import modelo.cache.CachePartida;
import modelo.cache.CachePartida.JugadorCache;
import modelo.cache.CachePartida.FichaCache;
import java.util.Scanner;
import java.util.List;

/**
 * VistaPeer - Vista de consola para el cliente peer.
 * 
 * Muestra:
 * - Menú principal
 * - Estado de la partida
 * - Información de jugadores
 * - Opciones de juego
 * 
 * Interfaz basada en texto ASCII sin caracteres especiales.
 */
public class VistaPeer {
    
    private final ClientePeer clientePeer;
    private final CachePartida cache;
    private final Scanner scanner;
    private int jugadorLocalId;
    
    public VistaPeer(ClientePeer clientePeer, CachePartida cache) {
        this.clientePeer = clientePeer;
        this.cache = cache;
        this.scanner = new Scanner(System.in);
        this.jugadorLocalId = -1;
    }
    
    // ============================
    // MENÚ PRINCIPAL
    // ============================
    
    /**
     * Muestra el menú principal.
     */
    public void mostrarMenuPrincipal() {
        limpiarPantalla();
        System.out.println("************************************************");
        System.out.println("*          PARCHIS MULTIJUGADOR               *");
        System.out.println("*        ARQUITECTURA HIBRIDA P2P             *");
        System.out.println("************************************************");
        System.out.println();
        System.out.println("1. Conectar al servidor");
        System.out.println("2. Salir");
        System.out.println();
        System.out.print("Elige una opcion: ");
    }
    
    /**
     * Muestra el menú de configuración inicial.
     */
    public void mostrarMenuConexion() {
        System.out.println();
        System.out.println("************************************************");
        System.out.println("*           CONFIGURACION                     *");
        System.out.println("************************************************");
    }
    
    /**
     * Solicita el host del servidor.
     */
    public String solicitarHost() {
        System.out.print("Host del servidor (localhost): ");
        String host = scanner.nextLine().trim();
        return host.isEmpty() ? "localhost" : host;
    }
    
    /**
     * Solicita el puerto del servidor.
     */
    public int solicitarPuertoServidor() {
        System.out.print("Puerto del servidor (5000): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return 5000;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Puerto invalido, usando 5000");
            return 5000;
        }
    }
    
    /**
     * Solicita el puerto P2P local.
     */
    public int solicitarPuertoP2P() {
        System.out.print("Puerto P2P local (6000): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return 6000;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Puerto invalido, usando 6000");
            return 6000;
        }
    }
    
    /**
     * Solicita el nombre del jugador.
     */
    public String solicitarNombreJugador() {
        System.out.println();
        System.out.print("Ingresa tu nombre: ");
        return scanner.nextLine().trim();
    }
    
    // ============================
    // MENÚ DE SALA DE ESPERA
    // ============================
    
    /**
     * Muestra la sala de espera.
     */
    public void mostrarSalaEspera() {
        limpiarPantalla();
        System.out.println("************************************************");
        System.out.println("*           SALA DE ESPERA                    *");
        System.out.println("************************************************");
        System.out.println();
        
        List<JugadorCache> jugadores = cache.getJugadores();
        System.out.println("Jugadores conectados (" + jugadores.size() + "/4):");
        for (JugadorCache jugador : jugadores) {
            String listo = jugador.listo ? "[LISTO]" : "[ESPERANDO]";
            System.out.println("  * " + jugador.nombre + " (" + jugador.color + ") " + listo);
        }
        
        System.out.println();
        System.out.println("1. Marcar como listo");
        System.out.println("2. Ver estado de la sala");
        System.out.println("3. Salir");
        System.out.println();
        System.out.print("Elige una opcion: ");
    }
    
    // ============================
    // MENÚ DE JUEGO
    // ============================
    
    /**
     * Muestra el menú durante el juego.
     */
    public void mostrarMenuJuego() {
        System.out.println();
        System.out.println("************************************************");
        
        // Verificar de quién es el turno
        int turnoActualId = cache.getJugadorTurnoId();
        if (turnoActualId == jugadorLocalId) {
            System.out.println("*            TU TURNO                          *");
        } else {
            JugadorCache jugadorTurno = cache.getJugadorTurnoActual();
            String nombreTurno = jugadorTurno != null ? jugadorTurno.nombre : "?";
            System.out.println("*     TURNO DE: " + nombreTurno);
        }
        
        System.out.println("************************************************");
        System.out.println();
        System.out.println("1. Tirar dados");
        System.out.println("2. Mover ficha");
        System.out.println("3. Sacar ficha");
        System.out.println("4. Ver estado de la partida");
        System.out.println("5. Saltar turno");
        System.out.println("6. Salir");
        System.out.println();
        System.out.print("Elige una opcion: ");
    }
    
    /**
     * Muestra el estado completo de la partida.
     */
    public void mostrarEstadoPartida() {
        limpiarPantalla();
        System.out.println("************************************************");
        System.out.println("*         ESTADO DE LA PARTIDA                *");
        System.out.println("************************************************");
        System.out.println();
        
        // Turno actual
        JugadorCache turnoActual = cache.getJugadorTurnoActual();
        if (turnoActual != null) {
            System.out.println("Turno actual: " + turnoActual.nombre + " (" + turnoActual.color + ")");
        }
        
        // Dados
        if (cache.isDadosTirados()) {
            int[] dados = cache.getDados();
            System.out.print("Dados: [" + dados[0] + "] [" + dados[1] + "]");
            if (cache.isDoble()) {
                System.out.print(" ** DOBLE **");
            }
            System.out.println();
        }
        
        System.out.println();
        System.out.println("------------------------------------------------");
        System.out.println("JUGADORES:");
        System.out.println("------------------------------------------------");
        
        List<JugadorCache> jugadores = cache.getJugadores();
        for (JugadorCache jugador : jugadores) {
            System.out.println();
            System.out.println(jugador.nombre + " (" + jugador.color + ")");
            System.out.println("  Puntos: " + jugador.puntos);
            
            if (!jugador.fichas.isEmpty()) {
                System.out.println("  Fichas:");
                for (FichaCache ficha : jugador.fichas) {
                    System.out.println("    Ficha #" + ficha.id + ": " + 
                        ficha.estado + " (casilla " + ficha.posicion + ")");
                }
            }
        }
        
        System.out.println();
        System.out.println("************************************************");
        System.out.println();
        esperarEnter();
    }
    
    /**
     * Solicita el ID de una ficha.
     */
    public int solicitarIdFicha() {
        System.out.println();
        System.out.print("Ingresa el ID de la ficha (1-4, 0 para cancelar): ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("ID invalido");
            return 0;
        }
    }
    
    /**
     * Muestra las fichas del jugador local.
     */
    public void mostrarMisFichas() {
        System.out.println();
        System.out.println("Tus fichas:");
        
        JugadorCache jugadorLocal = cache.getJugador(jugadorLocalId);
        if (jugadorLocal != null && !jugadorLocal.fichas.isEmpty()) {
            for (FichaCache ficha : jugadorLocal.fichas) {
                System.out.println("  " + ficha.id + ". Ficha #" + ficha.id + ": " + 
                    ficha.estado + " (casilla " + ficha.posicion + ")");
            }
        } else {
            System.out.println("  (Sin fichas registradas)");
        }
    }
    
    // ============================
    // NOTIFICACIONES
    // ============================
    
    /**
     * Muestra una notificación de dados tirados.
     */
    public void notificarDadosTirados(int dado1, int dado2, boolean esDoble) {
        System.out.println();
        System.out.println("************************************************");
        System.out.print("Dados tirados: [" + dado1 + "] [" + dado2 + "]");
        if (esDoble) {
            System.out.print(" ** DOBLE **");
        }
        System.out.println();
        System.out.println("************************************************");
    }
    
    /**
     * Muestra una notificación de ficha movida.
     */
    public void notificarFichaMovida(int fichaId, int origen, int destino) {
        System.out.println();
        System.out.println(">>> Ficha #" + fichaId + " movida: casilla " + 
            origen + " -> " + destino);
    }
    
    /**
     * Muestra una notificación de captura.
     */
    public void notificarCaptura(int bonusCasillas) {
        System.out.println();
        System.out.println("************************************************");
        System.out.println("*          CAPTURA!                           *");
        System.out.println("*      +" + bonusCasillas + " casillas de bonus                 *");
        System.out.println("************************************************");
    }
    
    /**
     * Muestra una notificación de llegada a meta.
     */
    public void notificarLlegadaMeta(int bonusPuntos) {
        System.out.println();
        System.out.println("************************************************");
        System.out.println("*       FICHA EN META!                        *");
        System.out.println("*      +" + bonusPuntos + " puntos                            *");
        System.out.println("************************************************");
    }
    
    /**
     * Muestra un mensaje de error.
     */
    public void mostrarError(String mensaje) {
        System.out.println();
        System.out.println("[ERROR] " + mensaje);
    }
    
    /**
     * Muestra un mensaje informativo.
     */
    public void mostrarMensaje(String mensaje) {
        System.out.println();
        System.out.println(mensaje);
    }
    
    /**
     * Muestra mensaje de inicio de partida.
     */
    public void notificarInicioPartida() {
        limpiarPantalla();
        System.out.println();
        System.out.println("************************************************");
        System.out.println("*                                              *");
        System.out.println("*        LA PARTIDA HA COMENZADO!             *");
        System.out.println("*                                              *");
        System.out.println("************************************************");
        System.out.println();
        esperarEnter();
    }
    
    /**
     * Muestra mensaje de cambio de turno.
     */
    public void notificarCambioTurno(String nombreJugador, boolean esMiTurno) {
        System.out.println();
        System.out.println("------------------------------------------------");
        if (esMiTurno) {
            System.out.println("*            ES TU TURNO!                     *");
        } else {
            System.out.println("  Turno de: " + nombreJugador);
        }
        System.out.println("------------------------------------------------");
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Limpia la pantalla (simulado con líneas).
     */
    private void limpiarPantalla() {
        for (int i = 0; i < 3; i++) {
            System.out.println();
        }
    }
    
    /**
     * Espera que el usuario presione Enter.
     */
    private void esperarEnter() {
        System.out.print("Presiona Enter para continuar...");
        scanner.nextLine();
    }
    
    /**
     * Lee una opción del menú.
     */
    public int leerOpcion() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Cierra el scanner.
     */
    public void cerrar() {
        scanner.close();
    }
    
    /**
     * Establece el ID del jugador local.
     */
    public void setJugadorLocalId(int jugadorId) {
        this.jugadorLocalId = jugadorId;
    }
    
    /**
     * Muestra mensaje de despedida.
     */
    public void mostrarDespedida() {
        System.out.println();
        System.out.println("************************************************");
        System.out.println("*         Gracias por jugar!                  *");
        System.out.println("*              Hasta pronto!                  *");
        System.out.println("************************************************");
    }
}