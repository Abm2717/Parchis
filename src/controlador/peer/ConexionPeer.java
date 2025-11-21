package controlador.peer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;

/**
 * ConexionPeer - Maneja la conexión P2P con un peer específico.
 * 
 * Responsabilidades:
 * - Mantener socket abierto con otro peer
 * - Enviar/recibir mensajes P2P
 * - Notificar al ClientePeer de mensajes recibidos
 * 
 * Cada ConexionPeer se ejecuta en su propio thread para escuchar mensajes.
 */
public class ConexionPeer {
    
    private final Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private final ClientePeer clientePeer;
    
    private int peerJugadorId;
    private boolean conectado;
    private Thread threadEscucha;
    
    public ConexionPeer(Socket socket, ClientePeer clientePeer) {
        this.socket = socket;
        this.clientePeer = clientePeer;
        this.peerJugadorId = -1; // Se asignará después
        this.conectado = false;
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
            conectado = true;
        } catch (IOException e) {
            System.err.println("Error inicializando streams P2P: " + e.getMessage());
            conectado = false;
        }
    }
    
    /**
     * Inicia el thread de escucha de mensajes.
     */
    public void iniciar() {
        if (!conectado) {
            System.err.println("No se pudo iniciar conexion P2P");
            return;
        }
        
        threadEscucha = new Thread(this::escucharMensajes);
        threadEscucha.start();
    }
    
    /**
     * Escucha mensajes del peer (thread).
     */
    private void escucharMensajes() {
        try {
            String linea;
            while (conectado && (linea = entrada.readLine()) != null) {
                procesarMensaje(linea);
            }
        } catch (IOException e) {
            if (conectado) {
                System.err.println("Error escuchando peer: " + e.getMessage());
            }
        } finally {
            desconectar();
        }
    }
    
    /**
     * Procesa un mensaje recibido del peer.
     */
    private void procesarMensaje(String mensajeJson) {
        try {
            JsonObject mensaje = JsonParser.parseString(mensajeJson).getAsJsonObject();
            String tipo = mensaje.get("tipo").getAsString();
            
            // Si es identificación, registrar el peer
            if (tipo.equals("IDENTIFICACION_PEER")) {
                this.peerJugadorId = mensaje.get("jugadorId").getAsInt();
                clientePeer.registrarConexionPeer(peerJugadorId, this);
                return;
            }
            
            // Delegar al ClientePeer
            clientePeer.procesarMensajePeer(mensaje, peerJugadorId);
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje P2P: " + e.getMessage());
        }
    }
    
    /**
     * Envía un mensaje al peer.
     */
    public synchronized void enviarMensaje(JsonObject mensaje) {
        if (!conectado || salida == null) {
            return;
        }
        
        try {
            salida.println(mensaje.toString());
            salida.flush();
        } catch (Exception e) {
            System.err.println("Error enviando mensaje P2P: " + e.getMessage());
            conectado = false;
        }
    }
    
    /**
     * Desconecta del peer.
     */
    public void desconectar() {
        if (!conectado) {
            return;
        }
        
        conectado = false;
        
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignorar
        }
        
        if (peerJugadorId != -1) {
            System.out.println(">>> Desconectado de peer: " + peerJugadorId);
        }
    }
    
    // ============================
    // GETTERS Y SETTERS
    // ============================
    
    public boolean isConectado() {
        return conectado;
    }
    
    public int getPeerJugadorId() {
        return peerJugadorId;
    }
    
    public void setPeerJugadorId(int peerJugadorId) {
        this.peerJugadorId = peerJugadorId;
    }
    
    public Socket getSocket() {
        return socket;
    }
}