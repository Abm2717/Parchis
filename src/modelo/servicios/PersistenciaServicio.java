package modelo.servicios;

import modelo.partida.Partida;
import modelo.Jugador.Jugador;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Servicio de persistencia en memoria para partidas y jugadores.
 * Maneja el almacenamiento temporal de datos durante la ejecución del servidor.
 * 
 * Thread-safe: usa ConcurrentHashMap para acceso concurrente.
 */
public class PersistenciaServicio {
    
    // Singleton instance
    private static PersistenciaServicio instancia;
    
    // Almacenamiento en memoria
    private final Map<Integer, Partida> partidas;           // id -> Partida
    private final Map<Integer, Jugador> jugadores;          // id -> Jugador
    private final Map<String, Jugador> jugadoresPorSession; // sessionId -> Jugador
    private final Map<Integer, Integer> jugadorEnPartida;   // jugadorId -> partidaId
    
    // Contadores para IDs autoincrementales
    private int contadorPartidas;
    private int contadorJugadores;
    
    // Constructor privado (Singleton)
    private PersistenciaServicio() {
        this.partidas = new ConcurrentHashMap<>();
        this.jugadores = new ConcurrentHashMap<>();
        this.jugadoresPorSession = new ConcurrentHashMap<>();
        this.jugadorEnPartida = new ConcurrentHashMap<>();
        this.contadorPartidas = 0;
        this.contadorJugadores = 0;
    }
    
    /**
     * Obtiene la instancia única del servicio (Singleton).
     */
    public static synchronized PersistenciaServicio getInstancia() {
        if (instancia == null) {
            instancia = new PersistenciaServicio();
        }
        return instancia;
    }
    
    // ============================
    // GESTIÓN DE PARTIDAS
    // ============================
    
    /**
     * Crea y almacena una nueva partida.
     */
    public synchronized Partida crearPartida(String nombre, int maxJugadores) {
        int id = ++contadorPartidas;
        Partida partida = new Partida(id, nombre, maxJugadores);
        partidas.put(id, partida);
        return partida;
    }
    
    /**
     * Obtiene una partida por su ID.
     */
    public Partida obtenerPartida(int partidaId) {
        return partidas.get(partidaId);
    }
    
    /**
     * Obtiene todas las partidas activas.
     */
    public List<Partida> obtenerTodasLasPartidas() {
        return new ArrayList<>(partidas.values());
    }
    
    /**
     * Obtiene partidas disponibles (que aceptan nuevos jugadores).
     */
    public List<Partida> obtenerPartidasDisponibles() {
        List<Partida> disponibles = new ArrayList<>();
        for (Partida p : partidas.values()) {
            if (p.puedeUnirseJugador()) {
                disponibles.add(p);
            }
        }
        return disponibles;
    }
    
    /**
     * Elimina una partida.
     */
    public synchronized boolean eliminarPartida(int partidaId) {
        Partida partida = partidas.remove(partidaId);
        if (partida != null) {
            // Limpiar referencias de jugadores
            for (Jugador j : partida.getJugadores()) {
                jugadorEnPartida.remove(j.getId());
            }
            return true;
        }
        return false;
    }
    
    // ============================
    // GESTIÓN DE JUGADORES
    // ============================
    
    /**
     * Crea y registra un nuevo jugador.
     */
    public synchronized Jugador crearJugador(String nombre, String sessionId) {
        int id = ++contadorJugadores;
        Jugador jugador = new Jugador(id, nombre, null, "default.png");
        jugador.setSessionId(sessionId);
        jugador.setConectado(true);
        
        jugadores.put(id, jugador);
        jugadoresPorSession.put(sessionId, jugador);
        
        return jugador;
    }
    
    /**
     * Obtiene un jugador por su ID.
     */
    public Jugador obtenerJugador(int jugadorId) {
        return jugadores.get(jugadorId);
    }
    
    /**
     * Obtiene un jugador por su session ID.
     */
    public Jugador obtenerJugadorPorSession(String sessionId) {
        return jugadoresPorSession.get(sessionId);
    }
    
    /**
     * Actualiza el estado de conexión de un jugador.
     */
    public void actualizarConexion(int jugadorId, boolean conectado) {
        Jugador jugador = jugadores.get(jugadorId);
        if (jugador != null) {
            jugador.setConectado(conectado);
        }
    }
    
    /**
     * Elimina un jugador del sistema.
     */
    public synchronized boolean eliminarJugador(int jugadorId) {
        Jugador jugador = jugadores.remove(jugadorId);
        if (jugador != null) {
            jugadoresPorSession.remove(jugador.getSessionId());
            jugadorEnPartida.remove(jugadorId);
            return true;
        }
        return false;
    }
    
    // ============================
    // RELACIÓN JUGADOR-PARTIDA
    // ============================
    
    /**
     * Registra que un jugador está en una partida.
     */
    public void registrarJugadorEnPartida(int jugadorId, int partidaId) {
        jugadorEnPartida.put(jugadorId, partidaId);
    }
    
    /**
     * Obtiene la partida en la que está un jugador.
     */
    public Optional<Partida> obtenerPartidaDeJugador(int jugadorId) {
        Integer partidaId = jugadorEnPartida.get(jugadorId);
        if (partidaId != null) {
            return Optional.ofNullable(partidas.get(partidaId));
        }
        return Optional.empty();
    }
    
    /**
     * Remueve a un jugador de su partida actual.
     */
    public void removerJugadorDePartida(int jugadorId) {
        jugadorEnPartida.remove(jugadorId);
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Obtiene el número total de partidas activas.
     */
    public int getTotalPartidas() {
        return partidas.size();
    }
    
    /**
     * Obtiene el número total de jugadores registrados.
     */
    public int getTotalJugadores() {
        return jugadores.size();
    }
    
    /**
     * Obtiene el número de jugadores conectados.
     */
    public long getJugadoresConectados() {
        return jugadores.values().stream()
            .filter(Jugador::isConectado)
            .count();
    }
    
    /**
     * Limpia todos los datos (útil para reiniciar servidor).
     */
    public synchronized void limpiarTodo() {
        partidas.clear();
        jugadores.clear();
        jugadoresPorSession.clear();
        jugadorEnPartida.clear();
        contadorPartidas = 0;
        contadorJugadores = 0;
    }
    
    @Override
    public String toString() {
        return String.format("PersistenciaServicio[Partidas: %d, Jugadores: %d (%d conectados)]",
            getTotalPartidas(), getTotalJugadores(), getJugadoresConectados());
    }
}