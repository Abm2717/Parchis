// ========================================
// CTRLUNIRSE.JAVA
// ========================================
package controlador.juego;

import controlador.servidor.ClienteHandler;
import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.servicios.PersistenciaServicio;
import modelo.servicios.SalaServicio;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.List;
import java.util.Optional;
import vista.VistaServidor;

/**
 * Controlador para gestionar registro de jugadores y unión a salas.
 * 
 * Responsabilidades:
 * - Registrar nuevos jugadores
 * - Crear salas/partidas
 * - Unir jugadores a partidas
 * - Listar salas disponibles
 * - Marcar jugadores como listos
 * - Salir de partidas
 */
public class CtrlUnirse {
    
    private final PersistenciaServicio persistencia;
    private final SalaServicio salaServicio;
    
    public CtrlUnirse() {
        this.persistencia = PersistenciaServicio.getInstancia();
        this.salaServicio = SalaServicio.getInstancia();
    }
    
    // ============================
    // REGISTRO DE JUGADOR
    // ============================
    
    /**
     * Registra un nuevo jugador en el sistema.
     * 
     * @param cliente ClienteHandler del jugador
     * @param nombre Nombre del jugador
     * @return Respuesta JSON
     */
    public String registrarJugador(ClienteHandler cliente, String nombre) {
        try {
            // Validar nombre
            if (nombre == null || nombre.trim().isEmpty()) {
                return crearError("El nombre no puede estar vacío");
            }
            
            nombre = nombre.trim();
            if (nombre.length() > 20) {
                nombre = nombre.substring(0, 20);
            }
            
            // Verificar si ya tiene un jugador asignado
            if (cliente.getJugador() != null) {
                return crearError("Ya estás registrado como: " + cliente.getJugador().getNombre());
            }
            
            // Crear jugador
            Jugador jugador = persistencia.crearJugador(nombre, cliente.getSessionId());
            cliente.setJugador(jugador);
            
            VistaServidor.mostrarRegistroJugador(jugador);
            
            // Crear respuesta
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "registro_exitoso");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Bienvenido, " + nombre);
            
            JsonObject datosJugador = new JsonObject();
            datosJugador.addProperty("id", jugador.getId());
            datosJugador.addProperty("nombre", jugador.getNombre());
            datosJugador.addProperty("sessionId", cliente.getSessionId());
            
            respuesta.add("jugador", datosJugador);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error registrando jugador: " + e.getMessage());
            return crearError("Error en el registro: " + e.getMessage());
        }
    }
    
    // ============================
    // CREAR SALA
    // ============================
    
    /**
     * Crea una nueva sala/partida.
     * 
     * @param cliente ClienteHandler del jugador
     * @param nombreSala Nombre de la sala
     * @param maxJugadores Número máximo de jugadores (2-4)
     * @return Respuesta JSON
     */
    public String crearSala(ClienteHandler cliente, String nombreSala, int maxJugadores) {
        try {
            // Validar que el jugador esté registrado
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            // Verificar que no esté en otra partida
            Optional<Partida> partidaActual = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (partidaActual.isPresent()) {
                return crearError("Ya estás en una partida. Sal primero.");
            }
            
            // Validar parámetros
            if (maxJugadores < 2 || maxJugadores > 4) {
                return crearError("El número de jugadores debe ser entre 2 y 4");
            }
            
            // Crear sala
            Partida partida = salaServicio.crearSala(nombreSala, maxJugadores);
            
            // Unir al creador automáticamente
            boolean unido = salaServicio.unirJugadorAPartida(jugador.getId(), partida.getId());
            
            if (!unido) {
                return crearError("Error uniéndose a la sala creada");
            }
            
            VistaServidor.mostrarCreacionSala(partida, jugador);
            
            // Crear respuesta
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "sala_creada");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Sala creada exitosamente");
            
            JsonObject datosPartida = serializarPartida(partida);
            respuesta.add("partida", datosPartida);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error creando sala: " + e.getMessage());
            return crearError("Error creando sala: " + e.getMessage());
        }
    }
    
    // ============================
    // UNIRSE A PARTIDA
    // ============================
    
    /**
     * Une un jugador a una partida específica.
     * 
     * @param cliente ClienteHandler del jugador
     * @param partidaId ID de la partida
     * @return Respuesta JSON
     */
    public String unirseAPartida(ClienteHandler cliente, int partidaId) {
        try {
            // Validar jugador registrado
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            // Verificar que no esté en otra partida
            Optional<Partida> partidaActual = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (partidaActual.isPresent()) {
                return crearError("Ya estás en una partida");
            }
            
            // Verificar que la partida exista
            Partida partida = persistencia.obtenerPartida(partidaId);
            if (partida == null) {
                return crearError("La partida no existe");
            }
            
            // Intentar unirse
            boolean unido = salaServicio.unirJugadorAPartida(jugador.getId(), partidaId);
            
            if (!unido) {
                return crearError("No se pudo unir a la partida (puede estar llena o iniciada)");
            }
            
            VistaServidor.mostrarUnionSala(jugador, partida);
            
            // Notificar a otros jugadores
            notificarNuevoJugador(partida, jugador, cliente);
            
            // Crear respuesta
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "union_exitosa");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Te has unido a la partida");
            
            JsonObject datosPartida = serializarPartida(partida);
            respuesta.add("partida", datosPartida);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error uniéndose a partida: " + e.getMessage());
            return crearError("Error: " + e.getMessage());
        }
    }
    
    /**
     * Une un jugador a cualquier partida disponible (o crea una nueva).
     * 
     * @param cliente ClienteHandler del jugador
     * @return Respuesta JSON
     */
    public String unirseAPartidaDisponible(ClienteHandler cliente) {
        try {
            // Validar jugador
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            // Verificar que no esté en partida
            Optional<Partida> partidaActual = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (partidaActual.isPresent()) {
                return crearError("Ya estás en una partida");
            }
            
            // Buscar o crear partida
            Partida partida = salaServicio.unirJugadorAPartidaDisponible(jugador.getId());
            
            if (partida == null) {
                return crearError("No se pudo unir a ninguna partida");
            }
            
            System.out.println("✓ " + jugador.getNombre() + " se unió a partida " + partida.getId());
            
            // Notificar a otros jugadores
            notificarNuevoJugador(partida, jugador, cliente);
            
            // Crear respuesta
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "union_exitosa");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Te has unido a una partida");
            
            JsonObject datosPartida = serializarPartida(partida);
            respuesta.add("partida", datosPartida);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error uniéndose a partida: " + e.getMessage());
            return crearError("Error: " + e.getMessage());
        }
    }
    
    // ============================
    // LISTAR SALAS
    // ============================
    
    /**
     * Lista todas las salas disponibles.
     * 
     * @param cliente ClienteHandler del jugador
     * @return Respuesta JSON con lista de salas
     */
    public String listarSalasDisponibles(ClienteHandler cliente) {
        try {
            List<Partida> disponibles = salaServicio.obtenerPartidasDisponibles();
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "lista_salas");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("total", disponibles.size());
            
            JsonArray salas = new JsonArray();
            for (Partida p : disponibles) {
                JsonObject sala = new JsonObject();
                sala.addProperty("id", p.getId());
                sala.addProperty("nombre", p.getNombre());
                sala.addProperty("jugadores", p.getJugadores().size());
                sala.addProperty("maxJugadores", p.getMaxJugadores());
                sala.addProperty("estado", p.getEstado().toString());
                salas.add(sala);
            }
            
            respuesta.add("salas", salas);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearError("Error listando salas: " + e.getMessage());
        }
    }
    
    // ============================
    // MARCAR LISTO
    // ============================
    
    /**
     * Marca al jugador como listo para iniciar la partida.
     * 
     * @param cliente ClienteHandler del jugador
     * @return Respuesta JSON
     */
    public String marcarListo(ClienteHandler cliente) {
        try {
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            // Verificar que esté en una partida
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) {
                return crearError("No estás en ninguna partida");
            }
            
            Partida partida = partidaOpt.get();
            
            // Marcar como listo
            boolean marcado = salaServicio.marcarJugadorListo(jugador.getId());
            
            if (!marcado) {
                return crearError("No se pudo marcar como listo");
            }
            
            System.out.println("✓ " + jugador.getNombre() + " está listo");
            
            // Notificar a otros jugadores
            notificarJugadorListo(partida, jugador, cliente);
            
            // Verificar si la partida debe iniciar
            if (partida.getEstado() == EstadoPartida.EN_PROGRESO) {
                notificarInicioPartida(partida);
            }
            
            // Crear respuesta
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "listo_confirmado");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Esperando a otros jugadores...");
            respuesta.addProperty("partidaIniciada", partida.getEstado() == EstadoPartida.EN_PROGRESO);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearError("Error: " + e.getMessage());
        }
    }
    
    // ============================
    // SALIR DE PARTIDA
    // ============================
    
    /**
     * Saca al jugador de su partida actual.
     * 
     * @param cliente ClienteHandler del jugador
     * @return Respuesta JSON
     */
    public String salirDePartida(ClienteHandler cliente) {
        try {
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            // Verificar que esté en una partida
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) {
                return crearError("No estás en ninguna partida");
            }
            
            Partida partida = partidaOpt.get();
            int partidaId = partida.getId();
            
            // Remover de la partida
            boolean removido = salaServicio.removerJugadorDePartida(jugador.getId());
            
            if (!removido) {
                return crearError("No se pudo salir de la partida");
            }
            
            System.out.println("✓ " + jugador.getNombre() + " salió de partida " + partidaId);
            
            // Notificar a otros jugadores
            notificarJugadorSalio(partidaId, jugador, cliente);
            
            // Crear respuesta
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "salida_exitosa");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Has salido de la partida");
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearError("Error: " + e.getMessage());
        }
    }
    
    // ============================
    // NOTIFICACIONES
    // ============================
    
    /**
     * Notifica a otros jugadores que alguien nuevo se unió.
     */
    private void notificarNuevoJugador(Partida partida, Jugador nuevoJugador, ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "jugador_unido");
        notificacion.addProperty("jugadorId", nuevoJugador.getId());
        notificacion.addProperty("nombre", nuevoJugador.getNombre());
        notificacion.addProperty("totalJugadores", partida.getJugadores().size());
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            cliente.getSessionId()
        );
    }
    
    /**
     * Notifica que un jugador está listo.
     */
    private void notificarJugadorListo(Partida partida, Jugador jugador, ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "jugador_listo");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("nombre", jugador.getNombre());
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            cliente.getSessionId()
        );
    }
    
    /**
     * Notifica que un jugador salió.
     */
    private void notificarJugadorSalio(int partidaId, Jugador jugador, ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "jugador_salio");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("nombre", jugador.getNombre());
        
        cliente.getServidor().broadcastAPartida(
            partidaId, 
            notificacion.toString(), 
            cliente.getSessionId()
        );
    }
    
    /**
     * Notifica que la partida ha iniciado.
     */
    private void notificarInicioPartida(Partida partida) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "partida_iniciada");
        notificacion.addProperty("mensaje", "¡La partida ha comenzado!");
        notificacion.addProperty("turnoInicial", partida.getTurnoActual());
        VistaServidor.mostrarInicioPartida(partida);
        
        Jugador jugadorTurno = partida.getJugadorActual();
        if (jugadorTurno != null) {
            notificacion.addProperty("turnoJugadorId", jugadorTurno.getId());
            notificacion.addProperty("turnoJugadorNombre", jugadorTurno.getNombre());
        }
        
        // Broadcast a todos en la partida
        for (Jugador j : partida.getJugadores()) {
            ClienteHandler handler = persistencia.obtenerJugadorPorSession(j.getSessionId()) != null ?
                getClienteHandler(j.getSessionId()) : null;
            
            if (handler != null) {
                handler.enviarMensaje(notificacion.toString());
            }
        }
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Serializa una partida a JSON.
     */
    private JsonObject serializarPartida(Partida partida) {
        JsonObject datos = new JsonObject();
        datos.addProperty("id", partida.getId());
        datos.addProperty("nombre", partida.getNombre());
        datos.addProperty("estado", partida.getEstado().toString());
        datos.addProperty("maxJugadores", partida.getMaxJugadores());
        datos.addProperty("turnoActual", partida.getTurnoActual());
        
        JsonArray jugadores = new JsonArray();
        for (Jugador j : partida.getJugadores()) {
            JsonObject jugadorObj = new JsonObject();
            jugadorObj.addProperty("id", j.getId());
            jugadorObj.addProperty("nombre", j.getNombre());
            jugadorObj.addProperty("color", j.getColor() != null ? j.getColor().toString() : "null");
            jugadorObj.addProperty("listo", j.isListo());
            jugadorObj.addProperty("puntos", j.getPuntos());
            jugadores.add(jugadorObj);
        }
        
        datos.add("jugadores", jugadores);
        
        return datos;
    }
    
    /**
     * Crea una respuesta de error.
     */
    private String crearError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error.toString();
    }
    
    /**
     * Obtiene ClienteHandler por sessionId (helper).
     */
    private ClienteHandler getClienteHandler(String sessionId) {
        // Necesitarías acceso al servidor, por simplicidad retorno null
        // En producción, pasar referencia al servidor en constructor
        return null;
    }
}
