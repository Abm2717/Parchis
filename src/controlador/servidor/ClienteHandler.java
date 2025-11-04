package controlador.servidor;

import controlador.Dispatcher;
import modelo.Jugador.Jugador;
import modelo.servicios.PersistenciaServicio;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClienteHandler implements Runnable {
    
    private final Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private final String sessionId;
    private Jugador jugador;
    private final ServidorCentral servidor;
    private final Dispatcher dispatcher;
    private final PersistenciaServicio persistencia;
    private boolean conectado;
    
    // ✅ NUEVO: Flag para ocultar mensajes de debug
    private static final boolean MODO_DEBUG = false;
    
    public ClienteHandler(Socket socket, String sessionId, ServidorCentral servidor) {
        this.socket = socket;
        this.sessionId = sessionId;
        this.servidor = servidor;
        this.dispatcher = new Dispatcher(this);
        this.persistencia = PersistenciaServicio.getInstancia();
        this.conectado = true;
        this.jugador = null;
        inicializarStreams();
    }
    
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
                    System.out.println("<- [" + sessionId + "] Mensaje recibido: " + 
                                     truncar(mensajeRecibido, 100));
                }
                
                procesarMensaje(mensajeRecibido);
            }
            
        } catch (SocketException e) {
            if (MODO_DEBUG) {
                System.out.println("X [" + sessionId + "] Conexion cerrada");
            }
        } catch (IOException e) {
            System.err.println("X [" + sessionId + "] Error de I/O: " + e.getMessage());
        } finally {
            desconectar();
        }
    }
    
    private void procesarMensaje(String mensajeJson) {
        try {
            String respuesta = dispatcher.procesarMensaje(mensajeJson);
            
            if (respuesta != null && !respuesta.isEmpty()) {
                enviarMensaje(respuesta);
            }
            
        } catch (Exception e) {
            System.err.println("X Error procesando mensaje: " + e.getMessage());
            enviarError("Error procesando solicitud: " + e.getMessage());
        }
    }
    
    private void enviarBienvenida() {
        String bienvenida = String.format(
            "{\"tipo\":\"bienvenida\",\"sessionId\":\"%s\",\"mensaje\":\"Conectado al servidor Parchis\"}",
            sessionId
        );
        enviarMensaje(bienvenida);
    }
    
    public synchronized void enviarMensaje(String mensajeJson) {
        if (!conectado || salida == null) return;
        
        try {
            salida.println(mensajeJson);
            
            if (MODO_DEBUG) {
                System.out.println("-> [" + sessionId + "] Mensaje enviado: " + 
                                 truncar(mensajeJson, 100) + "...");
            }
            
            if (salida.checkError()) {
                desconectar();
            }
            
        } catch (Exception e) {
            System.err.println("X Error enviando mensaje: " + e.getMessage());
            desconectar();
        }
    }
    
    public void enviarError(String mensajeError) {
        String errorJson = String.format(
            "{\"tipo\":\"error\",\"mensaje\":\"%s\"}",
            escaparJson(mensajeError)
        );
        enviarMensaje(errorJson);
    }
    
    public void desconectar() {
        if (!conectado) return;
        conectado = false;
        
        cerrarStreams();
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignorar
        }
        
        if (jugador != null) {
            persistencia.actualizarConexion(jugador.getId(), false);
        }
        
        servidor.removerCliente(sessionId);
    }
    
    private void cerrarStreams() {
        try {
            if (entrada != null) entrada.close();
        } catch (IOException e) { }
        
        if (salida != null) salida.close();
    }
    
    private String truncar(String texto, int maxLength) {
        if (texto == null) return "null";
        if (texto.length() <= maxLength) return texto;
        return texto.substring(0, maxLength);
    }
    
    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public boolean isConectado() { return conectado; }
    public ServidorCentral getServidor() { return servidor; }
    public Socket getSocket() { return socket; }
}