package controlador;

import controlador.servidor.ClienteHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import controlador.juego.CtrlMoverFicha;
import controlador.juego.CtrlTirarDado;
import controlador.juego.CtrlUnirse;
import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.servicios.PersistenciaServicio;
import java.util.Optional;

public class Dispatcher {
    
    private final ClienteHandler clienteHandler;
    private final Gson gson;
    private final CtrlUnirse ctrlUnirse;
    private final CtrlTirarDado ctrlTirarDado;
    private final CtrlMoverFicha ctrlMoverFicha;
    
    private static final boolean MODO_DEBUG = false;
    
    public Dispatcher(ClienteHandler clienteHandler) {
        this.clienteHandler = clienteHandler;
        this.gson = new Gson();
        this.ctrlUnirse = new CtrlUnirse();
        this.ctrlTirarDado = new CtrlTirarDado();
        this.ctrlMoverFicha = new CtrlMoverFicha();
    }
    
    public String procesarMensaje(String mensajeJson) {
        try {
            JsonObject jsonObject = JsonParser.parseString(mensajeJson).getAsJsonObject();
            
            if (!jsonObject.has("tipo")) {
                return crearRespuestaError("Mensaje sin campo 'tipo'");
            }
            
            String tipo = jsonObject.get("tipo").getAsString();
            
            if (MODO_DEBUG) {
                System.out.println("  -> Procesando accion: " + tipo);
            }
            
            return enrutarAccion(tipo, jsonObject);
            
        } catch (JsonSyntaxException e) {
            return crearRespuestaError("JSON invalido: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("X Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
            return crearRespuestaError("Error interno: " + e.getMessage());
        }
    }
    
    private String enrutarAccion(String tipo, JsonObject datos) {
        switch (tipo.toLowerCase()) {
            case "registrar":
            case "login":
                return manejarRegistro(datos);
            case "ping":
                return manejarPing();
            case "desconectar":
                return manejarDesconexion();
            case "crear_sala":
                return manejarCrearSala(datos);
            case "unirse":
            case "unirse_sala":
                return manejarUnirse(datos);
            case "listar_salas":
                return manejarListarSalas();
            case "salir_sala":
                return manejarSalirSala();
            case "listo":
            case "marcar_listo":
                return manejarMarcarListo();
            case "tirar_dado":
            case "tirar_dados":
                return manejarTirarDado(datos);
            case "mover_ficha":
                return manejarMoverFicha(datos);
            case "usar_bonus":
                return manejarUsarBonus(datos);
            case "obtener_estado":
            case "estado_partida":
                return manejarObtenerEstado();
            case "saltar_turno":
                return manejarSaltarTurno();
            default:
                return crearRespuestaError("Accion no reconocida: " + tipo);
        }
    }
    
    private String manejarRegistro(JsonObject datos) {
        try {
            String nombre = datos.get("nombre").getAsString();
            return ctrlUnirse.registrarJugador(clienteHandler, nombre);
        } catch (Exception e) {
            return crearRespuestaError("Error en registro: " + e.getMessage());
        }
    }
    
    private String manejarPing() {
        return "{\"tipo\":\"pong\",\"timestamp\":" + System.currentTimeMillis() + "}";
    }
    
    private String manejarDesconexion() {
        clienteHandler.desconectar();
        return null;
    }
    
    private String manejarCrearSala(JsonObject datos) {
        try {
            String nombreSala = datos.has("nombre") ? datos.get("nombre").getAsString() : "Sala Nueva";
            int maxJugadores = datos.has("maxJugadores") ? datos.get("maxJugadores").getAsInt() : 4;
            return ctrlUnirse.crearSala(clienteHandler, nombreSala, maxJugadores);
        } catch (Exception e) {
            return crearRespuestaError("Error creando sala: " + e.getMessage());
        }
    }
    
    private String manejarUnirse(JsonObject datos) {
        try {
            if (datos.has("partidaId")) {
                int partidaId = datos.get("partidaId").getAsInt();
                return ctrlUnirse.unirseAPartida(clienteHandler, partidaId);
            } else {
                return ctrlUnirse.unirseAPartidaDisponible(clienteHandler);
            }
        } catch (Exception e) {
            return crearRespuestaError("Error uniendose: " + e.getMessage());
        }
    }
    
    private String manejarListarSalas() {
        try {
            return ctrlUnirse.listarSalasDisponibles(clienteHandler);
        } catch (Exception e) {
            return crearRespuestaError("Error listando salas: " + e.getMessage());
        }
    }
    
    private String manejarSalirSala() {
        try {
            return ctrlUnirse.salirDePartida(clienteHandler);
        } catch (Exception e) {
            return crearRespuestaError("Error saliendo: " + e.getMessage());
        }
    }
    
    private String manejarMarcarListo() {
        try {
            return ctrlUnirse.marcarListo(clienteHandler);
        } catch (Exception e) {
            return crearRespuestaError("Error marcando listo: " + e.getMessage());
        }
    }
    
    private String manejarTirarDado(JsonObject datos) {
        try {
            return ctrlTirarDado.ejecutar(clienteHandler, datos);
        } catch (Exception e) {
            return crearRespuestaError("Error tirando dado: " + e.getMessage());
        }
    }
    
    private String manejarMoverFicha(JsonObject datos) {
        try {
            if (!datos.has("fichaId") || !datos.has("pasos")) {
                return crearRespuestaError("Faltan parametros: fichaId y pasos");
            }
            int fichaId = datos.get("fichaId").getAsInt();
            int pasos = datos.get("pasos").getAsInt();
            return ctrlMoverFicha.ejecutar(clienteHandler, fichaId, pasos);
        } catch (Exception e) {
            return crearRespuestaError("Error moviendo ficha: " + e.getMessage());
        }
    }
    
    private String manejarUsarBonus(JsonObject datos) {
        try {
            if (!datos.has("fichaId") || !datos.has("pasos")) {
                return crearRespuestaError("Faltan parametros: fichaId y pasos");
            }
            int fichaId = datos.get("fichaId").getAsInt();
            int pasos = datos.get("pasos").getAsInt();
            return ctrlMoverFicha.usarBonus(clienteHandler, fichaId, pasos);
        } catch (Exception e) {
            return crearRespuestaError("Error usando bonus: " + e.getMessage());
        }
    }
    
    private String manejarObtenerEstado() {
        return crearRespuestaError("Funcion no implementada");
    }
    
    private String manejarSaltarTurno() {
        try {
            Jugador jugador = clienteHandler.getJugador();
            if (jugador == null) return crearRespuestaError("Debes registrarte primero");
            
            PersistenciaServicio persistencia = PersistenciaServicio.getInstancia();
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) return crearRespuestaError("No estas en ninguna partida");
            
            Partida partida = partidaOpt.get();
            if (!partida.esTurnoDeJugador(jugador.getId())) return crearRespuestaError("No es tu turno");
            
            partida.avanzarTurno();
            
            Jugador siguienteJugador = partida.getJugadorActual();
            if (siguienteJugador != null) {
                JsonObject tuTurno = new JsonObject();
                tuTurno.addProperty("tipo", "tu_turno");
                tuTurno.addProperty("jugadorId", siguienteJugador.getId());
                tuTurno.addProperty("jugadorNombre", siguienteJugador.getNombre());
                
                ClienteHandler handlerTurno = clienteHandler.getServidor().getCliente(siguienteJugador.getSessionId());
                if (handlerTurno != null) handlerTurno.enviarMensaje(tuTurno.toString());
                
                JsonObject cambioTurno = new JsonObject();
                cambioTurno.addProperty("tipo", "cambio_turno");
                cambioTurno.addProperty("jugadorId", siguienteJugador.getId());
                cambioTurno.addProperty("jugadorNombre", siguienteJugador.getNombre());
                
                clienteHandler.getServidor().broadcastAPartida(partida.getId(), cambioTurno.toString(), siguienteJugador.getSessionId());
            }
            return crearRespuestaExito("Turno saltado");
        } catch (Exception e) {
            return crearRespuestaError("Error saltando turno: " + e.getMessage());
        }
    }
    
    private String crearRespuestaError(String mensaje) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "error");
        respuesta.addProperty("exito", false);
        respuesta.addProperty("mensaje", mensaje);
        return gson.toJson(respuesta);
    }
    
    public String crearRespuestaExito(String mensaje) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "exito");
        respuesta.addProperty("exito", true);
        respuesta.addProperty("mensaje", mensaje);
        return gson.toJson(respuesta);
    }
    
    public Gson getGson() { return gson; }
    public ClienteHandler getClienteHandler() { return clienteHandler; }
}