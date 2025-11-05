package modelo.servicios;

import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.Jugador.Jugador;
import modelo.Jugador.ColorJugador;
import modelo.Tablero.Tablero;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio gestiona las salas/partidas del juego.
 * Maneja la creacion, union, inicio y finalizacion de partidas.
 * 
 */
public class SalaServicio {
    
    private static SalaServicio instancia;
    private final PersistenciaServicio persistencia;
    private final ReentrantLock lock;
    
     private static final ColorJugador[] COLORES_DISPONIBLES = {
        ColorJugador.ROJO,
        ColorJugador.AMARILLO,
        ColorJugador.VERDE,
        ColorJugador.AZUL
    };
    
       private SalaServicio() {
        this.persistencia = PersistenciaServicio.getInstancia();
        this.lock = new ReentrantLock();
    }
    
    /**
     * Obtiene la instancia unica del servicio.
     */
    public static synchronized SalaServicio getInstancia() {
        if (instancia == null) {
            instancia = new SalaServicio();
        }
        return instancia;
    }
    
    /**
     * Crea una nueva sala/partida.
     * 
     * @param nombreSala Nombre de la sala
     * @param maxJugadores Numero maximo de jugadores (2-4)
     * @return La partida creada
     */
    public Partida crearSala(String nombreSala, int maxJugadores) {
        lock.lock();
        try {
            if (maxJugadores < 2 || maxJugadores > 4) {
                throw new IllegalArgumentException("El numero de jugadores debe ser entre 2 y 4");
            }
            
            Partida partida = persistencia.crearPartida(nombreSala, maxJugadores);
            partida.setEstado(EstadoPartida.ESPERANDO);
            
            // Inicializar tablero
            Tablero tablero = new Tablero();
            partida.setTablero(tablero);
            
            return partida;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Une un jugador a una partida especifica.
     * 
     * @param jugadorId ID del jugador
     * @param partidaId ID de la partida
     * @return true si se unio exitosamente
     */
    public boolean unirJugadorAPartida(int jugadorId, int partidaId) {
        lock.lock();
        try {
            Jugador jugador = persistencia.obtenerJugador(jugadorId);
            Partida partida = persistencia.obtenerPartida(partidaId);
            
            if (jugador == null || partida == null) {
                return false;
            }
            
            // Validar que la partida acepte jugadores
            if (!partida.puedeUnirseJugador()) {
                return false;
            }
            
            // Validar que el jugador no este en otra partida
            if (persistencia.obtenerPartidaDeJugador(jugadorId).isPresent()) {
                return false;
            }
            
            // Asignar color disponible
            ColorJugador colorAsignado = asignarColorDisponible(partida);
            if (colorAsignado == null) {
                return false;
            }
            
            jugador.setColor(colorAsignado);
            
            // Inicializar fichas del jugador
            jugador.inicializarFichas(4);
            
            // Agregar jugador a la partida
            partida.agregarJugador(jugador);
            
            // Registrar en tablero
            partida.getTablero().registrarJugador(jugador);
            
            // Actualizar persistencia
            persistencia.registrarJugadorEnPartida(jugadorId, partidaId);
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Une un jugador a cualquier partida disponible.
     * 
     * @param jugadorId ID del jugador
     * @return La partida a la que se unio
     */
    public Partida unirJugadorAPartidaDisponible(int jugadorId) {
        lock.lock();
        try {
            // Buscar partida disponible
            List<Partida> disponibles = persistencia.obtenerPartidasDisponibles();
            
            Partida partida;
            if (disponibles.isEmpty()) {
                partida = crearSala("Sala " + (persistencia.getTotalPartidas() + 1), 4);
            } else {
                // Unirse a la primera disponible
                partida = disponibles.get(0);
            }
            
            // Unir jugador
            if (unirJugadorAPartida(jugadorId, partida.getId())) {
                return partida;
            }
            
            return null;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Remueve un jugador de su partida actual.
     * 
     * @param jugadorId ID del jugador
     * @return true si se removio exitosamente
     */
    public boolean removerJugadorDePartida(int jugadorId) {
        lock.lock();
        try {
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugadorId);
            if (!partidaOpt.isPresent()) {
                return false;
            }
            
            Partida partida = partidaOpt.get();
            Jugador jugador = persistencia.obtenerJugador(jugadorId);
            
            if (jugador == null) {
                return false;
            }
            
            // Remover jugador de la partida
            partida.removerJugador(jugadorId);
            persistencia.removerJugadorDePartida(jugadorId);
            
            // Si la partida quedo vacia o esta en progreso con pocos jugadores, finalizarla
            if (partida.getJugadores().isEmpty()) {
                persistencia.eliminarPartida(partida.getId());
            } else if (partida.getEstado() == EstadoPartida.EN_PROGRESO && 
                       partida.getJugadores().size() < 2) {
                finalizarPartida(partida.getId());
            }
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Marca un jugador como "listo" para iniciar la partida.
     * 
     * @param jugadorId ID del jugador
     * @return true si se marco exitosamente
     */
    public boolean marcarJugadorListo(int jugadorId) {
        lock.lock();
        try {
            Jugador jugador = persistencia.obtenerJugador(jugadorId);
            if (jugador == null) {
                return false;
            }
            
            jugador.setListo(true);
            
            // Verificar si todos los jugadores estan listos
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugadorId);
            if (partidaOpt.isPresent()) {
                Partida partida = partidaOpt.get();
                if (todosJugadoresListos(partida) && partida.getJugadores().size() >= 2) {
                    iniciarPartida(partida.getId());
                }
            }
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Inicia una partida.
     * 
     * @param partidaId ID de la partida
     * @return true si se inicio exitosamente
     */
    public boolean iniciarPartida(int partidaId) {
        lock.lock();
        try {
            Partida partida = persistencia.obtenerPartida(partidaId);
            if (partida == null) {
                return false;
            }
            
            // Validar que hay al menos 2 jugadores
            if (partida.getJugadores().size() < 2) {
                return false;
            }
            
            // Validar estado actual
            if (partida.getEstado() != EstadoPartida.ESPERANDO) {
                return false;
            }
            
            // Cambiar estado
            partida.setEstado(EstadoPartida.EN_PROGRESO);
            
            // Determinar primer turno (aleatorio o primer jugador)
            partida.setTurnoActual(0);
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Finaliza una partida.
     * 
     * @param partidaId ID de la partida
     * @return true si se finalizo exitosamente
     */
    public boolean finalizarPartida(int partidaId) {
        lock.lock();
        try {
            Partida partida = persistencia.obtenerPartida(partidaId);
            if (partida == null) {
                return false;
            }
            
            partida.setEstado(EstadoPartida.FINALIZADA);
            GestorMotores.getInstancia().removerMotor(partidaId);
            // Opcional: remover jugadores de la partida
            for (Jugador j : partida.getJugadores()) {
                persistencia.removerJugadorDePartida(j.getId());
            }
            
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Obtiene una partida por su ID.
     */
    public Partida obtenerPartida(int partidaId) {
        return persistencia.obtenerPartida(partidaId);
    }
    
    /**
     * Obtiene la partida en la que esta un jugador.
     */
    public Optional<Partida> obtenerPartidaDeJugador(int jugadorId) {
        return persistencia.obtenerPartidaDeJugador(jugadorId);
    }
    
    /**
     * Obtiene todas las partidas disponibles para unirse.
     */
    public List<Partida> obtenerPartidasDisponibles() {
        return persistencia.obtenerPartidasDisponibles();
    }
    
    /**
     * Obtiene todas las partidas activas.
     */
    public List<Partida> obtenerTodasLasPartidas() {
        return persistencia.obtenerTodasLasPartidas();
    }
    
        
    /**
     * Asigna un color disponible a un jugador en una partida.
     * Orden: Rojo,Amarillo,Verde,Azul
     */
    private ColorJugador asignarColorDisponible(Partida partida) {
        List<ColorJugador> coloresUsados = new ArrayList<>();
        for (Jugador j : partida.getJugadores()) {
            if (j.getColor() != null) {
                coloresUsados.add(j.getColor());
            }
        }
        
        for (ColorJugador color : COLORES_DISPONIBLES) {
            if (!coloresUsados.contains(color)) {
                return color;
            }
        }
        
        return null; 
    }
    
    /**
     * Verifica si todos los jugadores de una partida estan listos.
     */
    private boolean todosJugadoresListos(Partida partida) {
        if (partida.getJugadores().isEmpty()) {
            return false;
        }
        
        return partida.getJugadores().stream().allMatch(Jugador::isListo);
    }
    
    @Override
    public String toString() {
        return String.format("SalaServicio[Partidas activas: %d]",
            persistencia.getTotalPartidas());
    }
}