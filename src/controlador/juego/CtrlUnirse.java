package controlador.juego;

import controlador.servidor.ClienteHandler;
import modelo.partida.MotorJuego;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.Jugador.Jugador;
import modelo.Jugador.ColorJugador;
import modelo.servicios.GestorMotores;
import modelo.Tablero.Tablero;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.List;
import java.util.ArrayList;

/**
 * Controlador para unirse a partidas en arquitectura híbrida P2P + Servidor.
 * 
 * Gestiona el registro de jugadores, creación de salas y preparación
 * de partidas antes de iniciar el juego.
 */
public class CtrlUnirse {
    
    private final GestorMotores gestorMotores;
    private static int contadorJugadores = 1; // Para generar IDs únicos (estático para compartir entre instancias)
    
    public CtrlUnirse() {
        this.gestorMotores = GestorMotores.getInstancia();
    }
    
    /**
     * Registra un nuevo jugador y lo agrega a la partida.
     * 
     * @param nombreJugador Nombre del jugador
     * @param handler ClienteHandler del jugador (para comunicación)
     * @return JsonObject con resultado del registro
     */
    public JsonObject registrarJugador(String nombreJugador, ClienteHandler handler) {
        JsonObject respuesta = new JsonObject();
        
        try {
            // Validar nombre
            if (nombreJugador == null || nombreJugador.trim().isEmpty()) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NOMBRE_INVALIDO");
                respuesta.addProperty("mensaje", "El nombre no puede estar vacío");
                return respuesta;
            }
            
            // Obtener o crear partida
            MotorJuego motor = gestorMotores.getMotorPrincipal();
            if (motor == null) {
                motor = gestorMotores.crearNuevoMotor("partida-principal");
            }
            
            Partida partida = motor.getPartida();
            
            // Validar que la partida no esté llena (máximo 4 jugadores)
            if (partida.getJugadores().size() >= 4) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "PARTIDA_LLENA");
                respuesta.addProperty("mensaje", "La partida ya tiene 4 jugadores");
                return respuesta;
            }
            
            // Validar que la partida no haya iniciado
            if (partida.getEstado() == EstadoPartida.EN_PROGRESO) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "PARTIDA_EN_CURSO");
                respuesta.addProperty("mensaje", "La partida ya está en curso");
                return respuesta;
            }
            
            // Validar que el nombre no esté duplicado
            for (Jugador j : partida.getJugadores()) {
                if (j.getNombre().equalsIgnoreCase(nombreJugador)) {
                    respuesta.addProperty("exito", false);
                    respuesta.addProperty("error", "NOMBRE_DUPLICADO");
                    respuesta.addProperty("mensaje", "Ya existe un jugador con ese nombre");
                    return respuesta;
                }
            }
            
            // Asignar color automáticamente
            ColorJugador color = asignarColorDisponible(partida);
            if (color == null) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_HAY_COLORES");
                return respuesta;
            }
            
            // Crear y agregar jugador
            int jugadorId = generarIdJugador();
            Jugador nuevoJugador = new Jugador(jugadorId, nombreJugador, color, "default");
            nuevoJugador.inicializarFichas(4); // Inicializar 4 fichas
            
            partida.agregarJugador(nuevoJugador);
            
            // Asociar handler con jugador en el gestor
            gestorMotores.registrarHandler(jugadorId, handler);
            
            // Construir respuesta
            respuesta.addProperty("exito", true);
            respuesta.addProperty("tipo", "JUGADOR_REGISTRADO");
            respuesta.addProperty("jugadorId", jugadorId);
            respuesta.addProperty("nombre", nombreJugador);
            respuesta.addProperty("color", color.name());
            respuesta.addProperty("mensaje", "Registro exitoso. Esperando otros jugadores...");
            
            // Información de la sala
            respuesta.addProperty("cantidadJugadores", partida.getJugadores().size());
            respuesta.addProperty("minimoJugadores", 2);
            respuesta.addProperty("maximoJugadores", 4);
            
            // Lista de jugadores actuales
            JsonArray jugadoresArray = new JsonArray();
            for (Jugador j : partida.getJugadores()) {
                JsonObject jugadorObj = new JsonObject();
                jugadorObj.addProperty("id", j.getId());
                jugadorObj.addProperty("nombre", j.getNombre());
                jugadorObj.addProperty("color", j.getColor().name());
                jugadoresArray.add(jugadorObj);
            }
            respuesta.add("jugadores", jugadoresArray);
            
            respuesta.addProperty("timestamp", System.currentTimeMillis());
            
            // Notificar a otros jugadores que alguien se unió
            notificarNuevoJugador(motor, nuevoJugador);
            
            return respuesta;
            
        } catch (Exception e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "ERROR_REGISTRO");
            respuesta.addProperty("mensaje", "Error al registrar: " + e.getMessage());
            e.printStackTrace();
            return respuesta;
        }
    }
    
    /**
     * Marca un jugador como listo para iniciar la partida.
     * 
     * @param jugadorId ID del jugador
     * @return JsonObject con resultado
     */
    public JsonObject marcarListo(int jugadorId) {
        JsonObject respuesta = new JsonObject();
        
        try {
            MotorJuego motor = gestorMotores.getMotorPrincipal();
            if (motor == null) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_HAY_PARTIDA");
                return respuesta;
            }
            
            Partida partida = motor.getPartida();
            Jugador jugador = partida.getJugadorPorId(jugadorId);
            
            if (jugador == null) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "JUGADOR_NO_ENCONTRADO");
                return respuesta;
            }
            
            // Marcar como listo
            jugador.setListo(true);
            
            respuesta.addProperty("exito", true);
            respuesta.addProperty("tipo", "JUGADOR_LISTO");
            respuesta.addProperty("jugadorId", jugadorId);
            respuesta.addProperty("mensaje", jugador.getNombre() + " está listo");
            
            // Notificar a todos
            notificarJugadorListo(motor, jugador);
            
            // Verificar si todos están listos para iniciar
            if (todosListosParaIniciar(partida)) {
                iniciarPartida(motor);
            }
            
            return respuesta;
            
        } catch (Exception e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "ERROR_MARCAR_LISTO");
            return respuesta;
        }
    }
    
    /**
     * Inicia la partida cuando todos los jugadores están listos.
     */
    private void iniciarPartida(MotorJuego motor) {
        try {
            Partida partida = motor.getPartida();
            
            // Validar que haya al menos 2 jugadores
            if (partida.getJugadores().size() < 2) {
                return;
            }
            
            // Crear y configurar tablero
            Tablero tablero = new Tablero();
            tablero.registrarJugadores(partida.getJugadores());
            partida.setTablero(tablero);
            
            // Cambiar estado a EN_PROGRESO
            partida.setEstado(EstadoPartida.EN_PROGRESO);
            
            // Establecer primer turno
            partida.setTurnoActual(0);
            
            // Notificar a todos que la partida inició
            notificarInicioPartida(motor);
            
        } catch (Exception e) {
            System.err.println("Error al iniciar partida: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Notifica a todos los jugadores que se unió uno nuevo.
     */
    private void notificarNuevoJugador(MotorJuego motor, Jugador nuevoJugador) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "NUEVO_JUGADOR");
        notificacion.addProperty("jugadorId", nuevoJugador.getId());
        notificacion.addProperty("nombre", nuevoJugador.getNombre());
        notificacion.addProperty("color", nuevoJugador.getColor().name());
        notificacion.addProperty("cantidadJugadores", motor.getPartida().getJugadores().size());
        
        broadcast(motor, notificacion);
    }
    
    /**
     * Notifica que un jugador está listo.
     */
    private void notificarJugadorListo(MotorJuego motor, Jugador jugador) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "JUGADOR_LISTO");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("nombre", jugador.getNombre());
        
        broadcast(motor, notificacion);
    }
    
    /**
     * Notifica a todos que la partida inició.
     */
    private void notificarInicioPartida(MotorJuego motor) {
        Partida partida = motor.getPartida();
        
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "PARTIDA_INICIADA");
        notificacion.addProperty("mensaje", "¡La partida ha comenzado!");
        
        // Información del primer turno
        Jugador primerJugador = partida.getJugadorActual();
        if (primerJugador != null) {
            notificacion.addProperty("primerTurno", primerJugador.getId());
            notificacion.addProperty("primerJugadorNombre", primerJugador.getNombre());
        }
        
        // Lista completa de jugadores
        JsonArray jugadoresArray = new JsonArray();
        for (Jugador j : partida.getJugadores()) {
            JsonObject jugadorObj = new JsonObject();
            jugadorObj.addProperty("id", j.getId());
            jugadorObj.addProperty("nombre", j.getNombre());
            jugadorObj.addProperty("color", j.getColor().name());
            jugadoresArray.add(jugadorObj);
        }
        notificacion.add("jugadores", jugadoresArray);
        
        broadcast(motor, notificacion);
    }
    
    /**
     * Obtiene el estado actual de la sala/partida.
     */
    public JsonObject getEstadoSala() {
        JsonObject respuesta = new JsonObject();
        
        try {
            MotorJuego motor = gestorMotores.getMotorPrincipal();
            if (motor == null) {
                respuesta.addProperty("error", "NO_HAY_PARTIDA");
                return respuesta;
            }
            
            Partida partida = motor.getPartida();
            respuesta.addProperty("estado", partida.getEstado().name());
            respuesta.addProperty("cantidadJugadores", partida.getJugadores().size());
            
            // Jugadores
            JsonArray jugadoresArray = new JsonArray();
            for (Jugador j : partida.getJugadores()) {
                JsonObject jugadorObj = new JsonObject();
                jugadorObj.addProperty("id", j.getId());
                jugadorObj.addProperty("nombre", j.getNombre());
                jugadorObj.addProperty("color", j.getColor().name());
                jugadorObj.addProperty("listo", j.isListo());
                jugadoresArray.add(jugadorObj);
            }
            respuesta.add("jugadores", jugadoresArray);
            
            return respuesta;
            
        } catch (Exception e) {
            respuesta.addProperty("error", "ERROR_OBTENER_ESTADO");
            return respuesta;
        }
    }
    
    /**
     * Verifica si todos los jugadores están listos para iniciar.
     */
    private boolean todosListosParaIniciar(Partida partida) {
        if (partida.getJugadores().size() < 2) {
            return false;
        }
        return partida.todosJugadoresListos();
    }
    
    /**
     * Asigna un color disponible automáticamente.
     */
    private ColorJugador asignarColorDisponible(Partida partida) {
        List<ColorJugador> coloresUsados = new ArrayList<>();
        for (Jugador j : partida.getJugadores()) {
            coloresUsados.add(j.getColor());
        }
        
        for (ColorJugador color : ColorJugador.values()) {
            if (!coloresUsados.contains(color)) {
                return color;
            }
        }
        
        return null; // No hay colores disponibles
    }
    
    /**
     * Genera un ID único para el jugador.
     */
    private synchronized int generarIdJugador() {
        return contadorJugadores++;
    }
    
    /**
     * Envía un mensaje a todos los jugadores conectados.
     */
    private void broadcast(MotorJuego motor, JsonObject mensaje) {
        Partida partida = motor.getPartida();
        for (Jugador jugador : partida.getJugadores()) {
            ClienteHandler handler = gestorMotores.getHandler(jugador.getId());
            if (handler != null) {
                handler.enviarMensaje(mensaje);
            }
        }
    }
}