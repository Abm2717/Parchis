package modelo.servicios;

import modelo.partida.Partida;
import modelo.partida.MotorJuego;
import modelo.partida.EstadoPartida;
import controlador.servidor.ClienteHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor centralizado de instancias de MotorJuego para arquitectura híbrida.
 * 
 * Responsabilidades:
 * - Crear y almacenar motores de juego
 * - Asociar ClienteHandlers con jugadores para broadcast
 * - Mantener estado de motores entre llamadas
 * - Limpiar motores de partidas finalizadas
 * 
 * Patrón: Singleton + Factory
 * Thread-safe mediante ConcurrentHashMap
 */
public class GestorMotores {
    
    private static GestorMotores instancia;
    
    // Mapa principal: clave de sala -> MotorJuego
    private final Map<String, MotorJuego> motoresPorSala;
    
    // Mapa de handlers: jugadorId -> ClienteHandler (para broadcast)
    private final Map<Integer, ClienteHandler> handlersPorJugador;
    
    // Motor principal de la partida actual (simplificado para híbrida)
    private MotorJuego motorPrincipal;
    
    // ============================
    // CONSTRUCTOR PRIVADO (SINGLETON)
    // ============================
    
    private GestorMotores() {
        this.motoresPorSala = new ConcurrentHashMap<>();
        this.handlersPorJugador = new ConcurrentHashMap<>();
        this.motorPrincipal = null;
    }
    
    /**
     * Obtiene la instancia única del gestor (Singleton).
     */
    public static synchronized GestorMotores getInstancia() {
        if (instancia == null) {
            instancia = new GestorMotores();
        }
        return instancia;
    }
    
    // ============================
    // GESTIÓN DE MOTORES
    // ============================
    
    /**
     * Obtiene o crea el motor principal de la partida.
     * En arquitectura híbrida simplificada, manejamos una sola partida principal.
     * 
     * @return MotorJuego principal
     */
    public MotorJuego getMotorPrincipal() {
        return motorPrincipal;
    }
    
    /**
     * Crea un nuevo motor de juego principal.
     * 
     * @param salaId Identificador de la sala
     * @return Nuevo MotorJuego creado
     */
    public MotorJuego crearNuevoMotor(String salaId) {
        // Crear nueva partida
        Partida nuevaPartida = new Partida(salaId.hashCode(), salaId);
        
        // Crear motor
        MotorJuego nuevoMotor = new MotorJuego(nuevaPartida);
        
        // Guardar como motor principal
        motorPrincipal = nuevoMotor;
        motoresPorSala.put(salaId, nuevoMotor);
        
        System.out.println(">>> Nuevo MotorJuego creado para sala: " + salaId);
        return nuevoMotor;
    }
    
    /**
     * Obtiene un motor por ID de sala.
     * 
     * @param salaId ID de la sala
     * @return MotorJuego o null si no existe
     */
    public MotorJuego obtenerMotorPorSala(String salaId) {
        return motoresPorSala.get(salaId);
    }
    
    /**
     * Verifica si existe un motor para una sala.
     * 
     * @param salaId ID de la sala
     * @return true si existe, false en caso contrario
     */
    public boolean existeMotor(String salaId) {
        return motoresPorSala.containsKey(salaId);
    }
    
    /**
     * Remueve un motor de juego.
     * 
     * @param salaId ID de la sala
     * @return true si se removió, false si no existía
     */
    public boolean removerMotor(String salaId) {
        MotorJuego removido = motoresPorSala.remove(salaId);
        if (removido != null) {
            System.out.println(">>> MotorJuego removido para sala: " + salaId);
            
            // Si era el motor principal, limpiarlo
            if (removido == motorPrincipal) {
                motorPrincipal = null;
            }
            
            return true;
        }
        return false;
    }
    
    // ============================
    // GESTIÓN DE HANDLERS (para broadcast)
    // ============================
    
    /**
     * Registra un ClienteHandler asociado a un jugador.
     * Esto permite hacer broadcast de mensajes a jugadores específicos.
     * 
     * @param jugadorId ID del jugador
     * @param handler ClienteHandler del jugador
     */
    public void registrarHandler(int jugadorId, ClienteHandler handler) {
        handlersPorJugador.put(jugadorId, handler);
        System.out.println(">>> Handler registrado para jugador: " + jugadorId);
    }
    
    /**
     * Obtiene el ClienteHandler de un jugador.
     * 
     * @param jugadorId ID del jugador
     * @return ClienteHandler o null si no existe
     */
    public ClienteHandler getHandler(int jugadorId) {
        return handlersPorJugador.get(jugadorId);
    }
    
    /**
     * Remueve el handler de un jugador (cuando se desconecta).
     * 
     * @param jugadorId ID del jugador
     */
    public void removerHandler(int jugadorId) {
        ClienteHandler removido = handlersPorJugador.remove(jugadorId);
        if (removido != null) {
            System.out.println(">>> Handler removido para jugador: " + jugadorId);
        }
    }
    
    /**
     * Obtiene todos los handlers registrados.
     * Útil para hacer broadcast a todos los jugadores.
     * 
     * @return Mapa de jugadorId -> ClienteHandler
     */
    public Map<Integer, ClienteHandler> getTodosLosHandlers() {
        return new ConcurrentHashMap<>(handlersPorJugador);
    }
    
    // ============================
    // LIMPIEZA Y MANTENIMIENTO
    // ============================
    
    /**
     * Limpia motores de partidas finalizadas.
     * Se puede ejecutar periódicamente para liberar memoria.
     * 
     * @return Cantidad de motores limpiados
     */
    public int limpiarMotoresFinalizados() {
        int limpiados = 0;
        
        for (Map.Entry<String, MotorJuego> entry : motoresPorSala.entrySet()) {
            MotorJuego motor = entry.getValue();
            Partida partida = motor.getPartida();
            
            if (partida.getEstado() == EstadoPartida.FINALIZADA) {
                motoresPorSala.remove(entry.getKey());
                limpiados++;
                System.out.println(">>> Motor finalizado limpiado: " + entry.getKey());
            }
        }
        
        if (limpiados > 0) {
            System.out.println(">>> Total de motores limpiados: " + limpiados);
        }
        
        return limpiados;
    }
    
    /**
     * Limpia todos los handlers (útil al cerrar el servidor).
     */
    public void limpiarTodosLosHandlers() {
        int cantidad = handlersPorJugador.size();
        handlersPorJugador.clear();
        System.out.println(">>> Todos los handlers limpiados: " + cantidad);
    }
    
    /**
     * Reinicia completamente el gestor (útil para testing o reinicio del servidor).
     */
    public void reiniciar() {
        motoresPorSala.clear();
        handlersPorJugador.clear();
        motorPrincipal = null;
        System.out.println(">>> GestorMotores reiniciado");
    }
    
    // ============================
    // INFORMACIÓN Y DEBUG
    // ============================
    
    /**
     * Obtiene la cantidad de motores activos.
     * 
     * @return Número de motores
     */
    public int getCantidadMotores() {
        return motoresPorSala.size();
    }
    
    /**
     * Obtiene la cantidad de handlers registrados.
     * 
     * @return Número de handlers
     */
    public int getCantidadHandlers() {
        return handlersPorJugador.size();
    }
    
    /**
     * Imprime información de debug del gestor.
     */
    public void imprimirEstado() {
        System.out.println("========================================");
        System.out.println("ESTADO DE GESTORMOTORES");
        System.out.println("========================================");
        System.out.println("Motores activos: " + getCantidadMotores());
        System.out.println("Handlers registrados: " + getCantidadHandlers());
        System.out.println("Motor principal: " + (motorPrincipal != null ? "SI" : "NO"));
        
        if (!motoresPorSala.isEmpty()) {
            System.out.println("\nSalas activas:");
            for (Map.Entry<String, MotorJuego> entry : motoresPorSala.entrySet()) {
                Partida partida = entry.getValue().getPartida();
                System.out.println("  - " + entry.getKey() + 
                    " [" + partida.getEstado() + "] " +
                    "(" + partida.getJugadores().size() + " jugadores)");
            }
        }
        
        if (!handlersPorJugador.isEmpty()) {
            System.out.println("\nJugadores conectados:");
            for (int jugadorId : handlersPorJugador.keySet()) {
                System.out.println("  - " + jugadorId);
            }
        }
        
        System.out.println("========================================");
    }
}