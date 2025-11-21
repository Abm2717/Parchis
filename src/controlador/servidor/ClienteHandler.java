package controlador.servidor;

import modelo.Jugador.Jugador;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Manejador de un cliente conectado al servidor.
 * 
 * Responsabilidades:
 * - Recibir mensajes JSON del cliente
 * - Enviar respuestas al cliente
 * - Procesar mensajes mediante Dispatcher
 * - Gestionar desconexión del cliente
 * 
 * Cada ClienteHandler se ejecuta en su propio thread.
 */
public class ClienteHandler implements Runnable {
    
    private final Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private final String sessionId;
    private Jugador jugador;
    private final ServidorCentral servidor;
    private final Dispatcher dispatcher;
    private boolean conectado;
    
    // Flag para modo debug
    private static final boolean MODO_DEBUG = false;
    
    public ClienteHandler(Socket socket, String sessionId, ServidorCentral servidor) {
        this.socket = socket;
        this.sessionId = sessionId;
        this.servidor = servidor;
        this.dispatcher = new Dispatcher(this);
        this.conectado = true;
        this.jugador = null;
        inicializarStreams();
    }
    
    /**
     * Inicializa los streams de entrada y salida.
     */
    private void inicializarStreams() {
        try {
            entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8")
            );
            salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                true
            );
        } catch (IOException e) {
            System.err.println("Error inicializando streams: " + e.getMessage());
            conectado = false;
        }
    }
    
    @Override
    public void run() {
        if (MODO_DEBUG) {
            System.out.println("-> ClienteHandler iniciado para: " + sessionId);
        }
        
        try {
            enviarBienvenida();
            
            String mensajeRecibido;
            while (conectado && (mensajeRecibido = entrada.readLine()) != null) {
                if (mensajeRecibido.trim().isEmpty()) continue;
                
                if (MODO_DEBUG) {
                    System.out.println("-> Mensaje recibido: " + truncar(mensajeRecibido, 100));
                }
                
                procesarMensaje(mensajeRecibido);
            }
            
        } catch (SocketException e) {
            if (MODO_DEBUG) {
                System.out.println("-> Conexion cerrada: " + sessionId);
            }
        } catch (IOException e) {
            System.err.println("Error en ClienteHandler [" + sessionId + "]: " + e.getMessage());
        } finally {
            desconectar();
        }
    }
    
    /**
     * Procesa un mensaje recibido del cliente.
     */
    private void procesarMensaje(String mensajeJson) {
        try {
            String respuesta = dispatcher.procesarMensaje(mensajeJson);
            
            if (respuesta != null && !respuesta.isEmpty()) {
                enviarMensaje(respuesta);
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
            enviarError("Error procesando mensaje: " + e.getMessage());
        }
    }
    
    /**
     * Envía un mensaje JSON al cliente.
     */
    public synchronized void enviarMensaje(String mensajeJson) {
        if (!conectado || salida == null) {
            return;
        }
        
        try {
            salida.println(mensajeJson);
            salida.flush();
            
            if (MODO_DEBUG) {
                System.out.println("<- Mensaje enviado: " + truncar(mensajeJson, 100));
            }
            
        } catch (Exception e) {
            System.err.println("Error enviando mensaje: " + e.getMessage());
            conectado = false;
        }
    }
    
    /**
     * Envía un mensaje JSON al cliente (sobrecarga con JsonObject).
     */
    public void enviarMensaje(JsonObject mensajeJson) {
        enviarMensaje(mensajeJson.toString());
    }
    
    /**
     * Envía un mensaje de error al cliente.
     */
    public void enviarError(String mensajeError) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensajeError);
        enviarMensaje(error.toString());
    }
    
    /**
     * Envía mensaje de bienvenida al conectarse.
     */
    private void enviarBienvenida() {
        JsonObject bienvenida = new JsonObject();
        bienvenida.addProperty("tipo", "bienvenida");
        bienvenida.addProperty("sessionId", sessionId);
        bienvenida.addProperty("mensaje", "Conectado al servidor Parchis");
        bienvenida.addProperty("version", "1.0-hibrida");
        enviarMensaje(bienvenida.toString());
    }
    
    /**
     * Desconecta el cliente y libera recursos.
     */
    public void desconectar() {
        if (!conectado) {
            return;
        }
        
        conectado = false;
        
        // Notificar desconexión si el jugador estaba registrado
        if (jugador != null) {
            if (MODO_DEBUG) {
                System.out.println("-> Jugador desconectado: " + jugador.getNombre());
            }
            
            // Aquí podrías notificar a otros jugadores de la partida
            // sobre la desconexión
        }
        
        cerrarStreams();
        servidor.removerCliente(sessionId);
        
        if (MODO_DEBUG) {
            System.out.println("-> Cliente desconectado: " + sessionId);
        }
    }
    
    /**
     * Cierra los streams de comunicación.
     */
    private void cerrarStreams() {
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando streams: " + e.getMessage());
        }
    }
    
    // ============================
    // GETTERS Y SETTERS
    // ============================
    
    public String getSessionId() {
        return sessionId;
    }
    
    public Jugador getJugador() {
        return jugador;
    }
    
    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }
    
    public boolean isConectado() {
        return conectado;
    }
    
    public ServidorCentral getServidor() {
        return servidor;
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Trunca un texto para debug.
     */
    private String truncar(String texto, int maxLength) {
        if (texto == null) return "null";
        if (texto.length() <= maxLength) return texto;
        return texto.substring(0, maxLength) + "...";
    }
    
    /**
     * Escapa caracteres especiales para JSON.
     */
    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}