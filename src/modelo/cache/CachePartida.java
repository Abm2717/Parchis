package modelo.cache;

import modelo.partida.EstadoPartida;
import modelo.Jugador.Jugador;
import modelo.Ficha.Ficha;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CachePartida - Copia local de solo lectura del estado de la partida.
 * 
 * En arquitectura híbrida:
 * - La UI consulta esta cache para mostrar datos sin esperar al servidor
 * - Se actualiza cuando llegan broadcasts del servidor
 * - Es de SOLO LECTURA para la UI (no modifica el estado oficial)
 * 
 * Propósito:
 * - Reducir latencia en la UI
 * - Mostrar estado consistente mientras se espera confirmación del servidor
 * - Permitir consultas frecuentes sin saturar la red
 */
public class CachePartida {
    
    // Estado básico
    private int partidaId;
    private String nombrePartida;
    private EstadoPartida estado;
    
    // Jugadores
    private List<JugadorCache> jugadores;
    private int turnoActual; // Índice del jugador actual
    
    // Dados actuales
    private int dado1;
    private int dado2;
    private boolean dadosTirados;
    
    // Info del turno
    private int jugadorTurnoId;
    private String jugadorTurnoNombre;
    private boolean esDoble;
    private int contadorDobles;
    
    // Timestamps
    private long timestampUltimaActualizacion;
    
    // Thread-safety
    private final ReentrantLock lock;
    
    public CachePartida() {
        this.jugadores = new ArrayList<>();
        this.estado = EstadoPartida.ESPERANDO;
        this.dadosTirados = false;
        this.dado1 = -1;
        this.dado2 = -1;
        this.turnoActual = 0;
        this.jugadorTurnoId = -1;
        this.esDoble = false;
        this.contadorDobles = 0;
        this.lock = new ReentrantLock();
        this.timestampUltimaActualizacion = System.currentTimeMillis();
    }
    
    // ============================
    // ACTUALIZACIONES (desde servidor)
    // ============================
    
    /**
     * Actualiza el estado de la partida.
     */
    public void actualizarEstado(EstadoPartida nuevoEstado) {
        lock.lock();
        try {
            this.estado = nuevoEstado;
            this.timestampUltimaActualizacion = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Actualiza la información de un jugador.
     */
    public void actualizarJugador(int jugadorId, String nombre, String color, int puntos, boolean listo) {
        lock.lock();
        try {
            JugadorCache jugador = buscarJugador(jugadorId);
            if (jugador == null) {
                jugador = new JugadorCache(jugadorId, nombre, color != null ? color : "AZUL");
                jugadores.add(jugador);
            }
            
            // Actualizar solo campos no nulos
            if (nombre != null) jugador.nombre = nombre;
            if (color != null) jugador.color = color;
            jugador.puntos = puntos;
            jugador.listo = listo;
            
            this.timestampUltimaActualizacion = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Actualiza los dados tirados.
     */
    public void actualizarDados(int dado1, int dado2, boolean esDoble, int contadorDobles) {
        lock.lock();
        try {
            this.dado1 = dado1;
            this.dado2 = dado2;
            this.dadosTirados = true;
            this.esDoble = esDoble;
            this.contadorDobles = contadorDobles;
            this.timestampUltimaActualizacion = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Actualiza el turno actual.
     */
    public void actualizarTurno(int jugadorId, String nombreJugador) {
        lock.lock();
        try {
            this.jugadorTurnoId = jugadorId;
            this.jugadorTurnoNombre = nombreJugador;
            this.dadosTirados = false; // Nuevo turno, dados no tirados
            this.dado1 = -1;
            this.dado2 = -1;
            this.timestampUltimaActualizacion = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Actualiza la posición de una ficha.
     */
    public void actualizarPosicionFicha(int jugadorId, int fichaId, int nuevaPosicion) {
        lock.lock();
        try {
            JugadorCache jugador = buscarJugador(jugadorId);
            if (jugador != null) {
                FichaCache ficha = jugador.buscarFicha(fichaId);
                if (ficha != null) {
                    ficha.posicion = nuevaPosicion;
                } else {
                    jugador.fichas.add(new FichaCache(fichaId, nuevaPosicion));
                }
            }
            this.timestampUltimaActualizacion = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Actualiza el estado de una ficha.
     */
    public void actualizarEstadoFicha(int jugadorId, int fichaId, String estado, int posicion) {
        lock.lock();
        try {
            JugadorCache jugador = buscarJugador(jugadorId);
            if (jugador != null) {
                FichaCache ficha = jugador.buscarFicha(fichaId);
                if (ficha != null) {
                    ficha.estado = estado;
                    ficha.posicion = posicion;
                }
            }
            this.timestampUltimaActualizacion = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Agrega un nuevo jugador a la cache.
     */
    public void agregarJugador(int jugadorId, String nombre, String color) {
        lock.lock();
        try {
            if (buscarJugador(jugadorId) == null) {
                jugadores.add(new JugadorCache(jugadorId, nombre, color));
                this.timestampUltimaActualizacion = System.currentTimeMillis();
            }
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // CONSULTAS (solo lectura para UI)
    // ============================
    
    /**
     * Obtiene el estado actual de la partida.
     */
    public EstadoPartida getEstado() {
        lock.lock();
        try {
            return estado;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene la lista de jugadores.
     */
    public List<JugadorCache> getJugadores() {
        lock.lock();
        try {
            return new ArrayList<>(jugadores);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el jugador del turno actual.
     */
    public JugadorCache getJugadorTurnoActual() {
        lock.lock();
        try {
            return buscarJugador(jugadorTurnoId);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el ID del jugador del turno actual.
     */
    public int getJugadorTurnoId() {
        lock.lock();
        try {
            return jugadorTurnoId;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si es el turno de un jugador específico.
     */
    public boolean esTurnoDeJugador(int jugadorId) {
        lock.lock();
        try {
            return this.jugadorTurnoId == jugadorId;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene los dados actuales.
     */
    public int[] getDados() {
        lock.lock();
        try {
            return new int[] { dado1, dado2 };
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si los dados fueron tirados.
     */
    public boolean isDadosTirados() {
        lock.lock();
        try {
            return dadosTirados;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si se sacó doble.
     */
    public boolean isDoble() {
        lock.lock();
        try {
            return esDoble;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el contador de dobles.
     */
    public int getContadorDobles() {
        lock.lock();
        try {
            return contadorDobles;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene un jugador por ID.
     */
    public JugadorCache getJugador(int jugadorId) {
        lock.lock();
        try {
            return buscarJugador(jugadorId);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el timestamp de la última actualización.
     */
    public long getTimestampUltimaActualizacion() {
        return timestampUltimaActualizacion;
    }
    
    // ============================
    // UTILIDADES PRIVADAS
    // ============================
    
    /**
     * Busca un jugador en la cache.
     */
    private JugadorCache buscarJugador(int jugadorId) {
        for (JugadorCache jugador : jugadores) {
            if (jugador.id == jugadorId) {
                return jugador;
            }
        }
        return null;
    }
    
    /**
     * Limpia toda la cache.
     */
    public void limpiar() {
        lock.lock();
        try {
            jugadores.clear();
            estado = EstadoPartida.ESPERANDO;
            dadosTirados = false;
            dado1 = -1;
            dado2 = -1;
            jugadorTurnoId = -1;
            jugadorTurnoNombre = null;
            esDoble = false;
            contadorDobles = 0;
        } finally {
            lock.unlock();
        }
    }
    
    // ============================
    // CLASES INTERNAS
    // ============================
    
    /**
     * Información cached de un jugador.
     */
    public static class JugadorCache {
        public int id;
        public String nombre;
        public String color;
        public int puntos;
        public boolean listo;
        public List<FichaCache> fichas;
        
        public JugadorCache(int id, String nombre, String color) {
            this.id = id;
            this.nombre = nombre;
            this.color = color;
            this.puntos = 0;
            this.listo = false;
            this.fichas = new ArrayList<>();
        }
        
        public FichaCache buscarFicha(int fichaId) {
            for (FichaCache ficha : fichas) {
                if (ficha.id == fichaId) {
                    return ficha;
                }
            }
            return null;
        }
        
        @Override
        public String toString() {
            return nombre + " (" + color + ") - " + puntos + " pts";
        }
    }
    
    /**
     * Información cached de una ficha.
     */
    public static class FichaCache {
        public int id;
        public int posicion;
        public String estado;
        
        public FichaCache(int id, int posicion) {
            this.id = id;
            this.posicion = posicion;
            this.estado = "EN_CASA";
        }
        
        @Override
        public String toString() {
            return "Ficha#" + id + " [" + estado + "] casilla:" + posicion;
        }
    }
}