// ========================================
// VISTASERVIDOR.JAVA
// ========================================
package vista;

import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.Ficha.Ficha;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Vista para el servidor - Muestra información en consola del servidor.
 * Utiliza solo caracteres ASCII estándar para compatibilidad.
 */
public class VistaServidor {
    
    private static final String SEPARADOR_GRUESO = "************************************************";
    private static final String SEPARADOR_FINO = "------------------------------------------------";
    private static final String SEPARADOR_DOBLE = "================================================";
    
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // ============================
    // MENSAJES DE INICIO/FIN
    // ============================
    
    /**
     * Muestra banner de inicio del servidor.
     */
    public static void mostrarBannerInicio(int puerto) {
        System.out.println("\n" + SEPARADOR_DOBLE);
        System.out.println("*                                              *");
        System.out.println("*        SERVIDOR PARCHIS - INICIADO           *");
        System.out.println("*                                              *");
        System.out.println(SEPARADOR_DOBLE);
        System.out.println("  Puerto: " + puerto);
        System.out.println("  Hora inicio: " + obtenerHoraActual());
        System.out.println("  Estado: ESPERANDO CONEXIONES...");
        System.out.println(SEPARADOR_GRUESO + "\n");
    }
    
    /**
     * Muestra mensaje de cierre del servidor.
     */
    public static void mostrarCierreServidor() {
        System.out.println("\n" + SEPARADOR_DOBLE);
        System.out.println("*        DETENIENDO SERVIDOR...                *");
        System.out.println(SEPARADOR_DOBLE);
    }
    
    // ============================
    // CONEXIONES
    // ============================
    
    /**
     * Muestra nueva conexión de cliente.
     */
    public static void mostrarNuevaConexion(String sessionId, String ip, int totalClientes) {
        System.out.println("\n[" + obtenerHoraActual() + "] NUEVA CONEXION");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Session ID: " + sessionId);
        System.out.println("  IP: " + ip);
        System.out.println("  Clientes conectados: " + totalClientes);
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra desconexión de cliente.
     */
    public static void mostrarDesconexion(String sessionId, String nombreJugador, int totalClientes) {
        System.out.println("\n[" + obtenerHoraActual() + "] DESCONEXION");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Session ID: " + sessionId);
        if (nombreJugador != null) {
            System.out.println("  Jugador: " + nombreJugador);
        }
        System.out.println("  Clientes restantes: " + totalClientes);
        System.out.println(SEPARADOR_FINO);
    }
    
    // ============================
    // REGISTRO Y SALAS
    // ============================
    
    /**
     * Muestra registro de nuevo jugador.
     */
    public static void mostrarRegistroJugador(Jugador jugador) {
        System.out.println("\n[" + obtenerHoraActual() + "] NUEVO JUGADOR REGISTRADO");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  ID: " + jugador.getId());
        System.out.println("  Nombre: " + jugador.getNombre());
        System.out.println("  Session: " + jugador.getSessionId());
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra creación de nueva sala.
     */
    public static void mostrarCreacionSala(Partida partida, Jugador creador) {
        System.out.println("\n[" + obtenerHoraActual() + "] SALA CREADA");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  ID Partida: " + partida.getId());
        System.out.println("  Nombre: " + partida.getNombre());
        System.out.println("  Max Jugadores: " + partida.getMaxJugadores());
        System.out.println("  Creador: " + creador.getNombre());
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra cuando un jugador se une a una sala.
     */
    public static void mostrarUnionSala(Jugador jugador, Partida partida) {
        System.out.println("\n[" + obtenerHoraActual() + "] JUGADOR SE UNIO A SALA");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Jugador: " + jugador.getNombre() + " (ID: " + jugador.getId() + ")");
        System.out.println("  Sala: " + partida.getNombre() + " (ID: " + partida.getId() + ")");
        System.out.println("  Jugadores en sala: " + partida.getJugadores().size() + "/" + partida.getMaxJugadores());
        System.out.println("  Color asignado: " + jugador.getColor());
        System.out.println(SEPARADOR_FINO);
    }
    
    // ============================
    // INICIO DE PARTIDA
    // ============================
    
    /**
     * Muestra inicio de partida con todos los jugadores.
     */
    public static void mostrarInicioPartida(Partida partida) {
        System.out.println("\n" + SEPARADOR_DOBLE);
        System.out.println("[" + obtenerHoraActual() + "] PARTIDA INICIADA");
        System.out.println(SEPARADOR_DOBLE);
        System.out.println("  Partida ID: " + partida.getId());
        System.out.println("  Nombre: " + partida.getNombre());
        System.out.println("  Estado: " + partida.getEstado());
        System.out.println("");
        System.out.println("  JUGADORES:");
        System.out.println("  " + SEPARADOR_FINO);
        
        for (Jugador j : partida.getJugadores()) {
            System.out.println("    [" + j.getColor() + "] " + j.getNombre() + " (ID: " + j.getId() + ")");
        }
        
        System.out.println("  " + SEPARADOR_FINO);
        
        Jugador primerJugador = partida.getJugadorActual();
        if (primerJugador != null) {
            System.out.println("  Primer turno: " + primerJugador.getNombre());
        }
        
        System.out.println(SEPARADOR_DOBLE + "\n");
    }
    
    // ============================
    // ACCIONES DE JUEGO
    // ============================
    
    /**
     * Muestra tirada de dados.
     */
    public static void mostrarTiradaDados(Jugador jugador, int dado1, int dado2, boolean esDoble) {
        System.out.println("\n[" + obtenerHoraActual() + "] TIRADA DE DADOS");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Jugador: " + jugador.getNombre());
        System.out.println("  Dados: [" + dado1 + "] [" + dado2 + "] = " + (dado1 + dado2));
        
        if (esDoble) {
            System.out.println("  ** DOBLE ** - Puede volver a tirar");
        }
        
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra movimiento de ficha.
     */
    public static void mostrarMovimientoFicha(Jugador jugador, int fichaId, int desde, int hasta) {
        System.out.println("\n[" + obtenerHoraActual() + "] MOVIMIENTO DE FICHA");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Jugador: " + jugador.getNombre());
        System.out.println("  Ficha: #" + fichaId);
        System.out.println("  Movimiento: Casilla " + desde + " --> Casilla " + hasta);
        System.out.println("  Distancia: " + (hasta - desde) + " casillas");
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra captura de ficha.
     */
    public static void mostrarCaptura(Jugador capturador, Jugador capturado, int fichaId, int bonusGanado) {
        System.out.println("\n[" + obtenerHoraActual() + "] ** CAPTURA DE FICHA **");
        System.out.println(SEPARADOR_GRUESO);
        System.out.println("  " + capturador.getNombre() + " capturo ficha de " + capturado.getNombre());
        System.out.println("  Ficha capturada: #" + fichaId);
        System.out.println("  Bonus ganado: +" + bonusGanado + " casillas");
        System.out.println("  Ficha capturada vuelve a CASA");
        System.out.println(SEPARADOR_GRUESO);
    }
    
    /**
     * Muestra llegada a meta.
     */
    public static void mostrarLlegadaMeta(Jugador jugador, int fichaId, int fichasEnMeta) {
        System.out.println("\n[" + obtenerHoraActual() + "] ** FICHA EN META **");
        System.out.println(SEPARADOR_GRUESO);
        System.out.println("  Jugador: " + jugador.getNombre());
        System.out.println("  Ficha: #" + fichaId + " llego a la META");
        System.out.println("  Fichas en meta: " + fichasEnMeta + "/4");
        System.out.println("  Puntos ganados: +10");
        System.out.println(SEPARADOR_GRUESO);
    }
    
    /**
     * Muestra uso de bonus.
     */
    public static void mostrarUsoBonus(Jugador jugador, int fichaId, int bonusUsado, int bonusRestante) {
        System.out.println("\n[" + obtenerHoraActual() + "] USO DE BONUS");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Jugador: " + jugador.getNombre());
        System.out.println("  Ficha: #" + fichaId);
        System.out.println("  Bonus usado: " + bonusUsado + " casillas");
        System.out.println("  Bonus restante: " + bonusRestante + " casillas");
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra bloqueo roto.
     */
    public static void mostrarBloqueoRoto(Jugador jugador) {
        System.out.println("\n[" + obtenerHoraActual() + "] BLOQUEO ROTO");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Jugador: " + jugador.getNombre());
        System.out.println("  Accion: Rompio su propio bloqueo");
        System.out.println("  Razon: Saco DOBLE");
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra pérdida de ficha por 3 dobles.
     */
    public static void mostrarFichaPerdida(Jugador jugador) {
        System.out.println("\n[" + obtenerHoraActual() + "] ** PENALIZACION **");
        System.out.println(SEPARADOR_GRUESO);
        System.out.println("  Jugador: " + jugador.getNombre());
        System.out.println("  Penalizacion: FICHA PERDIDA");
        System.out.println("  Razon: 3 DOBLES CONSECUTIVOS");
        System.out.println("  Una ficha vuelve a CASA");
        System.out.println(SEPARADOR_GRUESO);
    }
    
    // ============================
    // CAMBIO DE TURNO
    // ============================
    
    /**
     * Muestra cambio de turno.
     */
    public static void mostrarCambioTurno(Jugador jugadorAnterior, Jugador jugadorActual) {
        System.out.println("\n[" + obtenerHoraActual() + "] CAMBIO DE TURNO");
        System.out.println(SEPARADOR_FINO);
        if (jugadorAnterior != null) {
            System.out.println("  Turno anterior: " + jugadorAnterior.getNombre());
        }
        System.out.println("  Turno actual: " + jugadorActual.getNombre() + " [" + jugadorActual.getColor() + "]");
        System.out.println(SEPARADOR_FINO);
    }
    
    // ============================
    // FIN DE PARTIDA
    // ============================
    
    /**
     * Muestra ganador de la partida.
     */
    public static void mostrarGanador(Partida partida, Jugador ganador) {
        System.out.println("\n" + SEPARADOR_DOBLE);
        System.out.println("*                                              *");
        System.out.println("*           ** PARTIDA FINALIZADA **           *");
        System.out.println("*                                              *");
        System.out.println(SEPARADOR_DOBLE);
        System.out.println("");
        System.out.println("  GANADOR: " + ganador.getNombre());
        System.out.println("  Color: " + ganador.getColor());
        System.out.println("  Puntos: " + ganador.getPuntos());
        System.out.println("  Fichas en meta: " + ganador.contarFichasEnMeta() + "/4");
        System.out.println("");
        System.out.println("  CLASIFICACION FINAL:");
        System.out.println("  " + SEPARADOR_FINO);
        
        // Ordenar jugadores por fichas en meta y puntos
        java.util.List<Jugador> ranking = new java.util.ArrayList<>(partida.getJugadores());
        ranking.sort((j1, j2) -> {
            int compareFichas = Integer.compare(j2.contarFichasEnMeta(), j1.contarFichasEnMeta());
            if (compareFichas != 0) return compareFichas;
            return Integer.compare(j2.getPuntos(), j1.getPuntos());
        });
        
        int posicion = 1;
        for (Jugador j : ranking) {
            System.out.println("    " + posicion + ". " + j.getNombre() + 
                             " - Fichas: " + j.contarFichasEnMeta() + "/4" +
                             " - Puntos: " + j.getPuntos());
            posicion++;
        }
        
        System.out.println("  " + SEPARADOR_FINO);
        System.out.println("");
        System.out.println("  Partida ID: " + partida.getId());
        System.out.println("  Hora fin: " + obtenerHoraActual());
        System.out.println(SEPARADOR_DOBLE + "\n");
    }
    
    // ============================
    // ESTADISTICAS
    // ============================
    
    /**
     * Muestra estadísticas del servidor.
     */
    public static void mostrarEstadisticas(int clientes, int partidas, int jugadores) {
        System.out.println("\n" + SEPARADOR_DOBLE);
        System.out.println("*        ESTADISTICAS DEL SERVIDOR             *");
        System.out.println(SEPARADOR_DOBLE);
        System.out.println("  Clientes conectados:  " + clientes);
        System.out.println("  Partidas activas:     " + partidas);
        System.out.println("  Jugadores registrados: " + jugadores);
        System.out.println("  Hora: " + obtenerHoraActual());
        System.out.println(SEPARADOR_DOBLE + "\n");
    }
    
    /**
     * Muestra estado de una partida.
     */
    public static void mostrarEstadoPartida(Partida partida) {
        System.out.println("\n[" + obtenerHoraActual() + "] ESTADO DE PARTIDA");
        System.out.println(SEPARADOR_FINO);
        System.out.println("  Partida ID: " + partida.getId());
        System.out.println("  Nombre: " + partida.getNombre());
        System.out.println("  Estado: " + partida.getEstado());
        System.out.println("  Turno actual: " + partida.getTurnoActual());
        System.out.println("");
        System.out.println("  Jugadores (" + partida.getJugadores().size() + "/" + partida.getMaxJugadores() + "):");
        
        for (Jugador j : partida.getJugadores()) {
            String turno = partida.esTurnoDeJugador(j.getId()) ? " <-- TURNO" : "";
            System.out.println("    - " + j.getNombre() + " [" + j.getColor() + "]" +
                             " | Meta: " + j.contarFichasEnMeta() + "/4" +
                             " | Pts: " + j.getPuntos() + turno);
        }
        
        System.out.println(SEPARADOR_FINO);
    }
    
    // ============================
    // ERRORES
    // ============================
    
    /**
     * Muestra error genérico.
     */
    public static void mostrarError(String contexto, String mensaje) {
        System.err.println("\n[" + obtenerHoraActual() + "] ** ERROR **");
        System.err.println(SEPARADOR_FINO);
        System.err.println("  Contexto: " + contexto);
        System.err.println("  Mensaje: " + mensaje);
        System.err.println(SEPARADOR_FINO);
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Obtiene la hora actual formateada.
     */
    private static String obtenerHoraActual() {
        return LocalDateTime.now().format(formatter);
    }
    
    /**
     * Muestra mensaje genérico con formato.
     */
    public static void mostrarMensaje(String titulo, String mensaje) {
        System.out.println("\n[" + obtenerHoraActual() + "] " + titulo);
        System.out.println(SEPARADOR_FINO);
        System.out.println("  " + mensaje);
        System.out.println(SEPARADOR_FINO);
    }
    
    /**
     * Muestra línea simple con timestamp.
     */
    public static void log(String mensaje) {
        System.out.println("[" + obtenerHoraActual() + "] " + mensaje);
    }
}