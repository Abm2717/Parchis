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

public class CtrlUnirse {
    
    private final PersistenciaServicio persistencia;
    private final SalaServicio salaServicio;
    
    public CtrlUnirse() {
        this.persistencia = PersistenciaServicio.getInstancia();
        this.salaServicio = SalaServicio.getInstancia();
    }
    
     
    public String registrarJugador(ClienteHandler cliente, String nombre) {
        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                return crearError("El nombre no puede estar vacio");
            }
            
            nombre = nombre.trim();
            if (nombre.length() > 20) {
                nombre = nombre.substring(0, 20);
            }
            
            if (cliente.getJugador() != null) {
                return crearError("Ya estas registrado como: " + cliente.getJugador().getNombre());
            }
            
            Jugador jugador = persistencia.crearJugador(nombre, cliente.getSessionId());
            cliente.setJugador(jugador);
            
            System.out.println("Jugador registrado: " + nombre + " [ID: " + jugador.getId() + "]");
            
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
    
    public String crearSala(ClienteHandler cliente, String nombreSala, int maxJugadores) {
        try {
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            Optional<Partida> partidaActual = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (partidaActual.isPresent()) {
                return crearError("Ya estas en una partida. Sal primero.");
            }
            
            if (maxJugadores < 2 || maxJugadores > 4) {
                return crearError("El numero de jugadores debe ser entre 2 y 4");
            }
            
            Partida partida = salaServicio.crearSala(nombreSala, maxJugadores);
            
            boolean unido = salaServicio.unirJugadorAPartida(jugador.getId(), partida.getId());
            
            if (!unido) {
                return crearError("Error uniendose a la sala creada");
            }
            
            System.out.println("✓ Sala creada: " + nombreSala + " [ID: " + partida.getId() + "]");
            
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

    public String unirseAPartida(ClienteHandler cliente, int partidaId) {
        try {
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            Optional<Partida> partidaActual = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (partidaActual.isPresent()) {
                return crearError("Ya estas en una partida");
            }
            
            Partida partida = persistencia.obtenerPartida(partidaId);
            if (partida == null) {
                return crearError("La partida no existe");
            }
            
            boolean unido = salaServicio.unirJugadorAPartida(jugador.getId(), partidaId);
            
            if (!unido) {
                return crearError("No se pudo unir a la partida (puede estar llena o iniciada)");
            }
            
            System.out.println("✓ " + jugador.getNombre() + " se unio a partida " + partidaId);
            
            notificarNuevoJugador(partida, jugador, cliente);
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "union_exitosa");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Te has unido a la partida");
            
            JsonObject datosPartida = serializarPartida(partida);
            respuesta.add("partida", datosPartida);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error uniendose a partida: " + e.getMessage());
            return crearError("Error: " + e.getMessage());
        }
    }
    
    public String unirseAPartidaDisponible(ClienteHandler cliente) {
        try {
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            Optional<Partida> partidaActual = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (partidaActual.isPresent()) {
                return crearError("Ya estas en una partida");
            }
            
            Partida partida = salaServicio.unirJugadorAPartidaDisponible(jugador.getId());
            
            if (partida == null) {
                return crearError("No se pudo unir a ninguna partida");
            }
            
            System.out.println("✓ " + jugador.getNombre() + " se unio a partida " + partida.getId());
            
            notificarNuevoJugador(partida, jugador, cliente);
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "union_exitosa");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Te has unido a una partida");
            
            JsonObject datosPartida = serializarPartida(partida);
            respuesta.add("partida", datosPartida);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            System.err.println("Error uniendose a partida: " + e.getMessage());
            return crearError("Error: " + e.getMessage());
        }
    }

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

    
    public String marcarListo(ClienteHandler clienteHandler) {
        try {
            Jugador jugador = clienteHandler.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }

            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) {
                return crearError("No estas en ninguna partida");
            }

            Partida partida = partidaOpt.get();

            boolean marcado = salaServicio.marcarJugadorListo(jugador.getId());

            if (!marcado) {
                return crearError("No se pudo marcar como listo");
            }

            System.out.println(jugador.getNombre() + " esta listo");

            notificarJugadorListo(partida, jugador, clienteHandler);

            if (partida.getEstado() == EstadoPartida.EN_PROGRESO) {
                notificarInicioPartida(partida, clienteHandler);
            }

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

    
    public String salirDePartida(ClienteHandler cliente) {
        try {
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) {
                return crearError("No estas en ninguna partida");
            }
            
            Partida partida = partidaOpt.get();
            int partidaId = partida.getId();
            
            boolean removido = salaServicio.removerJugadorDePartida(jugador.getId());
            
            if (!removido) {
                return crearError("No se pudo salir de la partida");
            }
            
            System.out.println("✓ " + jugador.getNombre() + " salio de partida " + partidaId);
            
            notificarJugadorSalio(partidaId, jugador, cliente);
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "salida_exitosa");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Has salido de la partida");
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearError("Error: " + e.getMessage());
        }
    }

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
    
   
    private void notificarInicioPartida(Partida partida, ClienteHandler clienteHandler) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "partida_iniciada");
        notificacion.addProperty("mensaje", "¡La partida ha comenzado!");
        notificacion.addProperty("turnoInicial", partida.getTurnoActual());
        
        Jugador jugadorTurno = partida.getJugadorActual();
        if (jugadorTurno != null) {
            notificacion.addProperty("turnoJugadorId", jugadorTurno.getId());
            notificacion.addProperty("turnoJugadorNombre", jugadorTurno.getNombre());
        }
        
               clienteHandler.getServidor().broadcastAPartida(
            partida.getId(),
            notificacion.toString()
        );
    }
    
    
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
    
    private String crearError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error.toString();
    }
}
