package vista;

import controlador.servidor.ServidorCentral;
import java.util.Scanner;

/**
 * VistaServidor - Vista de consola para el servidor.
 * 
 * Muestra:
 * - Estado del servidor
 * - Clientes conectados
 * - Comandos administrativos
 * 
 * Interfaz simple basada en texto ASCII.
 */
public class VistaServidor {
    
    private final ServidorCentral servidor;
    private final Scanner scanner;
    
    public VistaServidor(ServidorCentral servidor) {
        this.servidor = servidor;
        this.scanner = new Scanner(System.in);
    }
    
    // ============================
    // PANTALLAS PRINCIPALES
    // ============================
    
    /**
     * Muestra el banner de inicio del servidor.
     */
    public void mostrarBanner() {
        System.out.println();
        System.out.println("************************************************");
        System.out.println("*                                              *");
        System.out.println("*      SERVIDOR PARCHIS MULTIJUGADOR          *");
        System.out.println("*        ARQUITECTURA HIBRIDA P2P             *");
        System.out.println("*                                              *");
        System.out.println("************************************************");
        System.out.println();
    }
    
    /**
     * Muestra la ayuda de comandos.
     */
    public void mostrarAyuda() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("   COMANDOS DISPONIBLES");
        System.out.println("================================================");
        System.out.println();
        System.out.println("  stats    - Muestra estadisticas del servidor");
        System.out.println("  clientes - Lista clientes conectados");
        System.out.println("  partidas - Lista partidas activas");
        System.out.println("  stop     - Detiene el servidor");
        System.out.println("  help     - Muestra esta ayuda");
        System.out.println();
        System.out.println("================================================");
    }
    
    /**
     * Muestra las estadísticas del servidor.
     */
    public void mostrarEstadisticas() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("   ESTADISTICAS DEL SERVIDOR");
        System.out.println("================================================");
        
        servidor.mostrarEstadisticas();
    }
    
    /**
     * Muestra los clientes conectados.
     */
    public void mostrarClientesConectados() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("   CLIENTES CONECTADOS");
        System.out.println("================================================");
        System.out.println();
        
        int cantidad = servidor.getNumeroClientesConectados();
        if (cantidad == 0) {
            System.out.println("  No hay clientes conectados");
        } else {
            System.out.println("  Total: " + cantidad + " cliente(s)");
            // Aquí podrías listar detalles si el servidor expone la info
        }
        
        System.out.println();
        System.out.println("================================================");
    }
    
    /**
     * Muestra las partidas activas.
     */
    public void mostrarPartidasActivas() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("   PARTIDAS ACTIVAS");
        System.out.println("================================================");
        System.out.println();
        
        // Aquí podrías listar partidas si el GestorMotores expone la info
        System.out.println("  Consulta implementacion en GestorMotores");
        
        System.out.println();
        System.out.println("================================================");
    }
    
    // ============================
    // NOTIFICACIONES
    // ============================
    
    /**
     * Notifica que el servidor se está iniciando.
     */
    public void notificarInicio(int puerto) {
        System.out.println();
        System.out.println(">>> Iniciando servidor en puerto " + puerto + "...");
        System.out.println(">>> Esperando conexiones...");
        System.out.println();
        System.out.println("Escribe 'help' para ver comandos disponibles");
        System.out.println();
    }
    
    /**
     * Notifica que un cliente se conectó.
     */
    public void notificarNuevaConexion(String ip, String sessionId) {
        System.out.println("[" + obtenerTimestamp() + "] Nueva conexion desde: " + ip);
        System.out.println("  Session ID: " + sessionId);
    }
    
    /**
     * Notifica que un cliente se desconectó.
     */
    public void notificarDesconexion(String sessionId) {
        System.out.println("[" + obtenerTimestamp() + "] Cliente desconectado: " + sessionId);
    }
    
    /**
     * Notifica que un jugador se registró.
     */
    public void notificarRegistroJugador(String nombre, String sessionId) {
        System.out.println("[" + obtenerTimestamp() + "] Jugador registrado: " + nombre);
        System.out.println("  Session: " + sessionId);
    }
    
    /**
     * Notifica que una partida inició.
     */
    public void notificarInicioPartida(int partidaId, int numeroJugadores) {
        System.out.println();
        System.out.println("************************************************");
        System.out.println("  PARTIDA " + partidaId + " INICIADA");
        System.out.println("  Jugadores: " + numeroJugadores);
        System.out.println("************************************************");
        System.out.println();
    }
    
    /**
     * Notifica que una partida finalizó.
     */
    public void notificarFinPartida(int partidaId) {
        System.out.println();
        System.out.println("[" + obtenerTimestamp() + "] Partida " + partidaId + " finalizada");
    }
    
    /**
     * Muestra un mensaje de error.
     */
    public void mostrarError(String mensaje) {
        System.err.println("[ERROR] " + mensaje);
    }
    
    /**
     * Muestra un mensaje informativo.
     */
    public void mostrarMensaje(String mensaje) {
        System.out.println("[INFO] " + mensaje);
    }
    
    // ============================
    // INTERACCIÓN
    // ============================
    
    /**
     * Lee un comando del administrador.
     */
    public String leerComando() {
        System.out.print("> ");
        return scanner.nextLine().trim().toLowerCase();
    }
    
    /**
     * Solicita confirmación para detener el servidor.
     */
    public boolean confirmarDetener() {
        System.out.println();
        System.out.print("¿Estas seguro de detener el servidor? (s/n): ");
        String respuesta = scanner.nextLine().trim().toLowerCase();
        return respuesta.equals("s") || respuesta.equals("si");
    }
    
    /**
     * Muestra mensaje de cierre.
     */
    public void mostrarMensajeCierre() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("   CERRANDO SERVIDOR...");
        System.out.println("================================================");
        System.out.println();
        System.out.println("  Desconectando clientes...");
    }
    
    /**
     * Muestra mensaje de servidor cerrado.
     */
    public void mostrarServidorCerrado() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("   SERVIDOR CERRADO");
        System.out.println("================================================");
        System.out.println();
        System.out.println("  Todos los recursos liberados");
        System.out.println("  Hasta pronto!");
        System.out.println();
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Obtiene un timestamp formateado.
     */
    private String obtenerTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
        return sdf.format(new java.util.Date());
    }
    
    /**
     * Cierra el scanner.
     */
    public void cerrar() {
        scanner.close();
    }
    
    /**
     * Muestra una línea separadora.
     */
    public void mostrarSeparador() {
        System.out.println("------------------------------------------------");
    }
    
    /**
     * Imprime una línea en blanco.
     */
    public void nuevaLinea() {
        System.out.println();
    }
}