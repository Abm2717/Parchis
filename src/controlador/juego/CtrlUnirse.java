package controlador.juego;

import controlador.servidor.ClienteHandler;
import modelo.Jugador.Jugador;
import modelo.Jugador.ColorJugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.partida.MotorJuego;
import modelo.servicios.GestorMotores;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.List;
import java.util.ArrayList;

/**
 * ✅ COMPATIBLE CON TU DISPATCHER
 */
public class CtrlUnirse {
    
    private final GestorMotores gestorMotores;
    private static int contadorJugadores = 1;
    
    public CtrlUnirse() {
        this.gestorMotores = GestorMotores.getInstancia();
    }
    
    /**
     * ✅ Firma compatible con Dispatcher: registrarJugador(String nombre, ClienteHandler cliente)
     */
    public JsonObject registrarJugador(String nombre, ClienteHandler cliente) {
        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                return crearErrorJson("El nombre no puede estar vacio");
            }
            
            nombre = nombre.trim();
            if (nombre.length() > 20) {
                nombre = nombre.substring(0, 20);
            }
            
            if (cliente.getJugador() != null) {
                return crearErrorJson("Ya estas registrado como: " + cliente.getJugador().getNombre());
            }
            
            int jugadorId = contadorJugadores++;
            Jugador jugador = new Jugador(jugadorId, nombre, null, "default");
            jugador.setSessionId(cliente.getSessionId());
            
            cliente.setJugador(jugador);
            gestorMotores.registrarHandler(jugadorId, cliente);
            
            System.out.println("Jugador registrado: " + nombre + " [ID: " + jugadorId + "]");
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "registro_exitoso");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("jugadorId", jugadorId);
            respuesta.addProperty("mensaje", "Bienvenido, " + nombre);
            
            return respuesta;
            
        } catch (Exception e) {
            System.err.println("Error registrando jugador: " + e.getMessage());
            return crearErrorJson("Error en el registro: " + e.getMessage());
        }
    }
    
    /**
     * ✅ Firma compatible con Dispatcher: marcarListo(int jugadorId)
     */
    public JsonObject marcarListo(int jugadorId) {
        try {
            MotorJuego motor = gestorMotores.obtenerMotorDeJugador(jugadorId);
            if (motor == null) {
                return crearErrorJson("No estas en ninguna partida");
            }

            Partida partida = motor.getPartida();
            Jugador jugador = partida.getJugadorPorId(jugadorId);
            
            if (jugador == null) {
                return crearErrorJson("Jugador no encontrado");
            }

            jugador.setListo(true);
            System.out.println(jugador.getNombre() + " esta listo");

            // ✅ Verificar si todos están listos para iniciar
            if (partida.todosJugadoresListos() && partida.tieneMinJugadores()) {
                iniciarPartida(partida, motor);
            }

            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "listo_confirmado");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("mensaje", "Esperando a otros jugadores...");
            respuesta.addProperty("partidaIniciada", partida.getEstado() == EstadoPartida.EN_PROGRESO);

            return respuesta;

        } catch (Exception e) {
            e.printStackTrace();
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    public JsonObject getEstadoSala() {
        try {
            MotorJuego motor = gestorMotores.getMotorPrincipal();
            if (motor == null) {
                return crearErrorJson("No hay partida activa");
            }
            
            Partida partida = motor.getPartida();
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "estado_sala");
            respuesta.addProperty("exito", true);
            respuesta.add("partida", serializarPartida(partida));
            
            return respuesta;
            
        } catch (Exception e) {
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    private void iniciarPartida(Partida partida, MotorJuego motor) {
        partida.setEstado(EstadoPartida.EN_PROGRESO);
        partida.setTurnoActual(0);
        
        // ✅ Inicializar tablero
        modelo.Tablero.Tablero tablero = new modelo.Tablero.Tablero();
        tablero.registrarJugadores(partida.getJugadores());
        partida.setTablero(tablero);
        
        // ✅ Inicializar fichas para cada jugador
        for (Jugador j : partida.getJugadores()) {
            j.inicializarFichas(4);
        }
        
        System.out.println("Partida iniciada!");
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
    
    private JsonObject crearErrorJson(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error;
    }
}