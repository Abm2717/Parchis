package modelo.partida;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import modelo.Jugador.Jugador;
import modelo.Ficha.Ficha;
import modelo.Tablero.Tablero;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representa una partida de Parchis.
 * Maneja jugadores, turno actual, tablero y estado general.
 * Thread-safe mediante ReentrantLock.
 */
public class Partida {
    
    private int id;
    private String nombre;
    private List<Jugador> jugadores;
    private int maxJugadores;
    private EstadoPartida estadoActual;
    private int turnoActual;  
    private Tablero tablero;
    private final ReentrantLock lock;
    private MotorJuego motorJuego;
    
  
    
    public Partida(int id, String nombre) {
        this(id, nombre, 4);
    }
    
    public Partida(int id, String nombre, int maxJugadores) {
        this.id = id;
        this.nombre = nombre;
        this.maxJugadores = Math.min(Math.max(maxJugadores, 2), 4); // Entre 2 y 4
        this.jugadores = new ArrayList<>();
        this.estadoActual = EstadoPartida.ESPERANDO;
        this.turnoActual = 0;
        this.tablero = null;
        this.motorJuego = null;
        this.lock = new ReentrantLock();
    }
    
  
    public MotorJuego getMotorJuego() {
        return motorJuego;
    }


    public void setMotorJuego(MotorJuego motorJuego) {
        this.motorJuego = motorJuego;
    }
    
    /**
     * Verifica si se puede unir un jugador a la partida.
     */
    public boolean puedeUnirseJugador() {
        lock.lock();
        try {
            return jugadores.size() < maxJugadores && 
                   estadoActual == EstadoPartida.ESPERANDO;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Agrega un jugador a la partida.
     * @return true si se agrego exitosamente
     */
    public boolean agregarJugador(Jugador jugador) {
        lock.lock();
        try {
            if (!puedeUnirseJugador()) {
                return false;
            }
            
            // Verificar que no este ya en la partida
            if (jugadores.stream().anyMatch(j -> j.getId() == jugador.getId())) {
                return false;
            }
            
            jugadores.add(jugador);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Remueve un jugador de la partida.
     * @return true si se removio exitosamente
     */
    public boolean removerJugador(int jugadorId) {
        lock.lock();
        try {
            boolean removido = jugadores.removeIf(j -> j.getId() == jugadorId);
            
            // Si se removio el jugador del turno actual, ajustar turno
            if (removido && turnoActual >= jugadores.size() && !jugadores.isEmpty()) {
                turnoActual = turnoActual % jugadores.size();
            }
            
            return removido;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene un jugador por su ID.
     */
    public Jugador getJugadorPorId(int jugadorId) {
        lock.lock();
        try {
            return jugadores.stream()
                .filter(j -> j.getId() == jugadorId)
                .findFirst()
                .orElse(null);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el jugador del turno actual.
     */
    public Jugador getJugadorActual() {
        lock.lock();
        try {
            if (jugadores.isEmpty() || turnoActual < 0 || turnoActual >= jugadores.size()) {
                return null;
            }
            return jugadores.get(turnoActual);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene una ficha especifica de un jugador.
     */
    public Ficha getFicha(int jugadorId, int fichaId) {
        Jugador jugador = getJugadorPorId(jugadorId);
        if (jugador == null) {
            return null;
        }
        return jugador.getFichaPorId(fichaId);
    }
    
    // ============================
    // GESTIoN DE TURNOS
    // ============================
    
    /**
     * Avanza al siguiente turno.
     */
    public void avanzarTurno() {
        lock.lock();
        try {
            if (jugadores.isEmpty()) {
                return;
            }
            
            turnoActual = (turnoActual + 1) % jugadores.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si es el turno de un jugador especifico.
     */
    public boolean esTurnoDeJugador(int jugadorId) {
        Jugador actual = getJugadorActual();
        return actual != null && actual.getId() == jugadorId;
    }
    

    
    /**
     * Verifica si la partida ha terminado.
     */
    public boolean haTerminado() {
        lock.lock();
        try {
            if (estadoActual != EstadoPartida.EN_PROGRESO) {
                return false;
            }
            
            // Verificar si algun jugador tiene todas sus fichas en meta
            for (Jugador j : jugadores) {
                if (j.haGanado()) {
                    return true;
                }
            }
            
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene el ganador de la partida (si existe).
     */
    public Jugador getGanador() {
        lock.lock();
        try {
            if (!haTerminado()) {
                return null;
            }
            
            return jugadores.stream()
                .filter(Jugador::haGanado)
                .findFirst()
                .orElse(null);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si todos los jugadores estan listos.
     */
    public boolean todosJugadoresListos() {
        lock.lock();
        try {
            if (jugadores.isEmpty()) {
                return false;
            }
            return jugadores.stream().allMatch(Jugador::isListo);
        } finally {
            lock.unlock();
        }
    }
    
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public List<Jugador> getJugadores() {
        lock.lock();
        try {
            return new ArrayList<>(jugadores); // Retornar copia defensiva
        } finally {
            lock.unlock();
        }
    }
    
    public int getMaxJugadores() {
        return maxJugadores;
    }
    
    public void setMaxJugadores(int maxJugadores) {
        this.maxJugadores = Math.min(Math.max(maxJugadores, 2), 4);
    }
    
    public EstadoPartida getEstado() {
        return estadoActual;
    }
    
    public void setEstado(EstadoPartida estado) {
        lock.lock();
        try {
            this.estadoActual = estado;
        } finally {
            lock.unlock();
        }
    }
    
    public int getTurnoActual() {
        return turnoActual;
    }
    
    public void setTurnoActual(int turno) {
        lock.lock();
        try {
            if (turno >= 0 && turno < jugadores.size()) {
                this.turnoActual = turno;
            }
        } finally {
            lock.unlock();
        }
    }
    
    public Tablero getTablero() {
        return tablero;
    }
    
    public void setTablero(Tablero tablero) {
        this.tablero = tablero;
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Obtiene el numero actual de jugadores.
     */
    public int getNumeroJugadores() {
        lock.lock();
        try {
            return jugadores.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si la partida esta llena.
     */
    public boolean estaLlena() {
        lock.lock();
        try {
            return jugadores.size() >= maxJugadores;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica si la partida tiene el minimo de jugadores para comenzar.
     */
    public boolean tieneMinJugadores() {
        lock.lock();
        try {
            return jugadores.size() >= 2;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Reinicia la partida (util para jugar de nuevo).
     */
    public void reiniciar() {
        lock.lock();
        try {
            estadoActual = EstadoPartida.ESPERANDO;
            turnoActual = 0;
            
            // Reiniciar jugadores
            for (Jugador j : jugadores) {
                j.setListo(false);
                j.setPuntos(0);
                j.inicializarFichas(4);
            }
            
            // Reiniciar tablero si existe
            if (tablero != null) {
                tablero.limpiar();
                tablero.registrarJugadores(jugadores);
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Obtiene informacion resumida de la partida.
     */
    public String getResumen() {
        lock.lock();
        try {
            return String.format("Partida[id=%d, nombre=%s, jugadores=%d/%d, estado=%s, turno=%d]",
                id, nombre, jugadores.size(), maxJugadores, estadoActual, turnoActual);
        } finally {
            lock.unlock();
        }
    }
    
    public JsonObject generarEstadoJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("partidaId", id);
        json.addProperty("nombre", nombre);
        json.addProperty("estado", estadoActual.name());
        json.addProperty("turnoActual", turnoActual);
        json.addProperty("maxJugadores", maxJugadores);

        // Jugadores
        JsonArray jugadoresArr = new JsonArray();
        for (Jugador j : jugadores) {
            JsonObject jObj = new JsonObject();
            jObj.addProperty("id", j.getId());
            jObj.addProperty("nombre", j.getNombre());
            jObj.addProperty("color", j.getColor() != null ? j.getColor().name() : "NINGUNO");
            jObj.addProperty("puntos", j.getPuntos());
            jObj.addProperty("listo", j.isListo());
            jObj.addProperty("fichasEnMeta", j.contarFichasEnMeta());
            jugadoresArr.add(jObj);
        }
        json.add("jugadores", jugadoresArr);

        // Tablero
        if (tablero != null) {
            json.add("tablero", tablero.generarEstadoJSON());
        }

        return json;
    }
    
    @Override
    public String toString() {
        return getResumen();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Partida)) return false;
        Partida p = (Partida) o;
        return this.id == p.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

