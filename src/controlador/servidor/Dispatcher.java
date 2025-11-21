package controlador;

import controlador.servidor.ClienteHandler;
import controlador.juego.CtrlUnirse;
import controlador.juego.CtrlTirarDado;
import controlador.juego.CtrlMoverFicha;
import modelo.servicios.GestorMotores;
import modelo.partida.MotorJuego;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Dispatcher - Enrutador de mensajes para arquitectura híbrida.
 * 
 * Recibe mensajes JSON del ClienteHandler y los redirige
 * al controlador apropiado según el tipo de acción.
 * 
 * En arquitectura híbrida:
 * - Valida acciones en el servidor
 * - Retorna respuestas para broadcast a peers
 * - Mantiene sincronización del estado oficial
 */
public class Dispatcher {
    
    private final ClienteHandler clienteHandler;
    private final Gson gson;
    private final CtrlUnirse ctrlUnirse;
    private CtrlTirarDado ctrlTirarDado;
    private CtrlMoverFicha ctrlMoverFicha;
    private final GestorMotores gestorMotores;
    
    // Flag para modo debug
    private static final boolean MODO_DEBUG = false;
    
    public Dispatcher(ClienteHandler clienteHandler) {
        this.clienteHandler = clienteHandler;
        this.gson = new Gson();
        this.ctrlUnirse = new CtrlUnirse();
        this.gestorMotores = GestorMotores.getInstancia();
        
        // Los controladores de juego se inicializan cuando hay un motor disponible
        this.ctrlTirarDado = null;
        this.ctrlMoverFicha = null;
    }
    
    /**
     * Procesa un mensaje JSON y retorna la respuesta.
     */
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
    
    /**
     * Enruta la acción al controlador apropiado.
     */
    private String enrutarAccion(String tipo, JsonObject datos) {
        switch (tipo.toLowerCase()) {
            // ============================
            // SESION Y CONEXION
            // ============================
            case "registrar":
            case "login":
                return manejarRegistro(datos);
            
            case "ping":
                return manejarPing();
            
            case "desconectar":
                return manejarDesconexion();
            
            // ============================
            // GESTION DE PARTIDA
            // ============================
            case "unirse":
            case "unirse_partida":
                return manejarUnirse(datos);
            
            case "listo":
            case "marcar_listo":
                return manejarMarcarListo();
            
            case "obtener_estado":
            case "estado_sala":
                return manejarObtenerEstado();
            
            // ============================
            // ACCIONES DE JUEGO
            // ============================
            case "tirar_dado":
            case "tirar_dados":
                return manejarTirarDados();
            
            case "mover_ficha":
                return manejarMoverFicha(datos);
            
            case "sacar_ficha":
                return manejarSacarFicha(datos);
            
            case "usar_bonus":
            case "aplicar_bonus":
                return manejarUsarBonus(datos);
            
            case "saltar_turno":
                return manejarSaltarTurno();
            
            case "obtener_fichas":
                return manejarObtenerFichas();
            
            // ============================
            // CASOS NO RECONOCIDOS
            // ============================
            default:
                return crearRespuestaError("Accion no reconocida: " + tipo);
        }
    }
    
    // ============================
    // HANDLERS DE SESION
    // ============================
    
    private String manejarRegistro(JsonObject datos) {
        try {
            if (!datos.has("nombre")) {
                return crearRespuestaError("Falta campo 'nombre'");
            }
            
            String nombreJugador = datos.get("nombre").getAsString();
            JsonObject respuesta = ctrlUnirse.registrarJugador(nombreJugador, clienteHandler);
            
            // Si el registro fue exitoso, asociar jugador con el handler
            if (respuesta.get("exito").getAsBoolean()) {
                int jugadorId = respuesta.get("jugadorId").getAsInt();
                
                // Obtener motor y jugador
                MotorJuego motor = gestorMotores.getMotorPrincipal();
                if (motor != null) {
                    modelo.Jugador.Jugador jugador = motor.getPartida().getJugadorPorId(jugadorId);
                    if (jugador != null) {
                        clienteHandler.setJugador(jugador);
                    }
                    
                    // Inicializar controladores de juego si aún no existen
                    if (ctrlTirarDado == null) {
                        ctrlTirarDado = new CtrlTirarDado(motor);
                        ctrlMoverFicha = new CtrlMoverFicha(motor);
                    }
                }
            }
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error en registro: " + e.getMessage());
        }
    }
    
    private String manejarPing() {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "pong");
        respuesta.addProperty("exito", true);
        respuesta.addProperty("timestamp", System.currentTimeMillis());
        return respuesta.toString();
    }
    
    private String manejarDesconexion() {
        clienteHandler.desconectar();
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "desconectado");
        respuesta.addProperty("exito", true);
        return respuesta.toString();
    }
    
    // ============================
    // HANDLERS DE PARTIDA
    // ============================
    
    private String manejarUnirse(JsonObject datos) {
        // Ya manejado en manejarRegistro, pero podría ser separado
        return manejarRegistro(datos);
    }
    
    private String manejarMarcarListo() {
        try {
            if (clienteHandler.getJugador() == null) {
                return crearRespuestaError("Debes registrarte primero");
            }
            
            int jugadorId = clienteHandler.getJugador().getId();
            JsonObject respuesta = ctrlUnirse.marcarListo(jugadorId);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error al marcar listo: " + e.getMessage());
        }
    }
    
    private String manejarObtenerEstado() {
        try {
            JsonObject respuesta = ctrlUnirse.getEstadoSala();
            return respuesta.toString();
        } catch (Exception e) {
            return crearRespuestaError("Error obteniendo estado: " + e.getMessage());
        }
    }
    
    // ============================
    // HANDLERS DE JUEGO
    // ============================
    
    private String manejarTirarDados() {
        try {
            if (clienteHandler.getJugador() == null) {
                return crearRespuestaError("Debes estar registrado en una partida");
            }
            
            if (ctrlTirarDado == null) {
                return crearRespuestaError("La partida no ha iniciado");
            }
            
            int jugadorId = clienteHandler.getJugador().getId();
            JsonObject respuesta = ctrlTirarDado.tirarDados(jugadorId);
            
            // Si fue exitoso, hacer broadcast a todos los jugadores
            if (respuesta.get("exito").getAsBoolean()) {
                clienteHandler.getServidor().broadcastATodos(respuesta.toString());
                return null; // Ya se hizo broadcast
            }
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error tirando dados: " + e.getMessage());
        }
    }
    
    private String manejarMoverFicha(JsonObject datos) {
        try {
            if (clienteHandler.getJugador() == null) {
                return crearRespuestaError("Debes estar registrado en una partida");
            }
            
            if (ctrlMoverFicha == null) {
                return crearRespuestaError("La partida no ha iniciado");
            }
            
            if (!datos.has("fichaId") || !datos.has("dado1") || !datos.has("dado2")) {
                return crearRespuestaError("Faltan parametros: fichaId, dado1, dado2");
            }
            
            int jugadorId = clienteHandler.getJugador().getId();
            int fichaId = datos.get("fichaId").getAsInt();
            int dado1 = datos.get("dado1").getAsInt();
            int dado2 = datos.get("dado2").getAsInt();
            
            JsonObject respuesta = ctrlMoverFicha.moverFicha(jugadorId, fichaId, dado1, dado2);
            
            // Si fue exitoso, hacer broadcast
            if (respuesta.get("exito").getAsBoolean()) {
                clienteHandler.getServidor().broadcastATodos(respuesta.toString());
                return null; // Ya se hizo broadcast
            }
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error moviendo ficha: " + e.getMessage());
        }
    }
    
    private String manejarSacarFicha(JsonObject datos) {
        try {
            if (clienteHandler.getJugador() == null) {
                return crearRespuestaError("Debes estar registrado en una partida");
            }
            
            if (ctrlMoverFicha == null) {
                return crearRespuestaError("La partida no ha iniciado");
            }
            
            if (!datos.has("fichaId") || !datos.has("dado1") || !datos.has("dado2")) {
                return crearRespuestaError("Faltan parametros: fichaId, dado1, dado2");
            }
            
            int jugadorId = clienteHandler.getJugador().getId();
            int fichaId = datos.get("fichaId").getAsInt();
            int dado1 = datos.get("dado1").getAsInt();
            int dado2 = datos.get("dado2").getAsInt();
            
            JsonObject respuesta = ctrlMoverFicha.sacarFicha(jugadorId, fichaId, dado1, dado2);
            
            // Si fue exitoso, hacer broadcast
            if (respuesta.get("exito").getAsBoolean()) {
                clienteHandler.getServidor().broadcastATodos(respuesta.toString());
                return null;
            }
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error sacando ficha: " + e.getMessage());
        }
    }
    
    private String manejarUsarBonus(JsonObject datos) {
        try {
            if (clienteHandler.getJugador() == null) {
                return crearRespuestaError("Debes estar registrado en una partida");
            }
            
            if (ctrlMoverFicha == null) {
                return crearRespuestaError("La partida no ha iniciado");
            }
            
            if (!datos.has("fichaId") || !datos.has("pasos")) {
                return crearRespuestaError("Faltan parametros: fichaId, pasos");
            }
            
            int jugadorId = clienteHandler.getJugador().getId();
            int fichaId = datos.get("fichaId").getAsInt();
            int pasos = datos.get("pasos").getAsInt();
            
            JsonObject respuesta = ctrlMoverFicha.aplicarBonus(jugadorId, fichaId, pasos);
            
            // Si fue exitoso, hacer broadcast
            if (respuesta.get("exito").getAsBoolean()) {
                clienteHandler.getServidor().broadcastATodos(respuesta.toString());
                return null;
            }
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error usando bonus: " + e.getMessage());
        }
    }
    
    private String manejarSaltarTurno() {
        try {
            if (clienteHandler.getJugador() == null) {
                return crearRespuestaError("Debes estar registrado en una partida");
            }
            
            if (ctrlMoverFicha == null) {
                return crearRespuestaError("La partida no ha iniciado");
            }
            
            int jugadorId = clienteHandler.getJugador().getId();
            JsonObject respuesta = ctrlMoverFicha.saltarTurno(jugadorId);
            
            // Si fue exitoso, hacer broadcast
            if (respuesta.get("exito").getAsBoolean()) {
                clienteHandler.getServidor().broadcastATodos(respuesta.toString());
                return null;
            }
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error saltando turno: " + e.getMessage());
        }
    }
    
    private String manejarObtenerFichas() {
        try {
            if (clienteHandler.getJugador() == null) {
                return crearRespuestaError("Debes estar registrado en una partida");
            }
            
            if (ctrlMoverFicha == null) {
                return crearRespuestaError("La partida no ha iniciado");
            }
            
            int jugadorId = clienteHandler.getJugador().getId();
            JsonObject respuesta = ctrlMoverFicha.getFichasMovibles(jugadorId);
            
            return respuesta.toString();
            
        } catch (Exception e) {
            return crearRespuestaError("Error obteniendo fichas: " + e.getMessage());
        }
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Crea una respuesta de error estandarizada.
     */
    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error.toString();
    }
    
    /**
     * Crea una respuesta exitosa estandarizada.
     */
    public String crearRespuesta(String tipo, boolean exito, Object datos) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", tipo);
        respuesta.addProperty("exito", exito);
        respuesta.add("datos", gson.toJsonTree(datos));
        return gson.toJson(respuesta);
    }
    
    /**
     * Obtiene el Gson instance para serialización.
     */
    public Gson getGson() {
        return gson;
    }
    
    /**
     * Obtiene el ClienteHandler asociado.
     */
    public ClienteHandler getClienteHandler() {
        return clienteHandler;
    }
}