package controlador.peer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vista.VistaCliente;
import vista.TableroVista;
import controlador.ClienteControlador;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ CORREGIDO: ClientePeer calcula movimientos localmente
 * - Recibe movimientos de otros peers (10ms)
 * - Actualiza UI inmediatamente (optimista)
 * - NO espera confirmación del servidor
 */
public class ClientePeer {
    
    private final int miPuertoPeer;
    private int miJugadorId; // ✅ NUEVO: ID del jugador local
    private ServerSocket servidorPeer;
    private final Map<Integer, ConexionPeer> peerConectados;
    private boolean activo;
    private Thread hiloServidor;
    private VistaCliente vista;
    private TableroVista tableroVista; // ✅ NUEVO
    private ClienteControlador controlador; // ✅ NUEVO
    
    public ClientePeer(int puerto, ClienteControlador controlador) {
        this.miPuertoPeer = puerto;
        this.miJugadorId = -1; // Se asignará después
        this.peerConectados = new ConcurrentHashMap<>();
        this.activo = false;
        this.vista = null;
        this.tableroVista = null;
        this.controlador = controlador;
    }
    
    /**
     * ✅ NUEVO: Asigna el ID del jugador local
     */
    public void setMiJugadorId(int jugadorId) {
        this.miJugadorId = jugadorId;
        System.out.println("[ClientePeer] Mi ID asignado: " + jugadorId);
    }
    
    /**
     * ✅ Asigna la vista de consola
     */
    public void setVista(VistaCliente vista) {
        this.vista = vista;
    }
    
    /**
     * ✅ NUEVO: Asigna la vista del tablero GUI
     */
    public void setTableroVista(TableroVista tableroVista) {
        this.tableroVista = tableroVista;
        System.out.println("[ClientePeer] TableroVista conectado");
    }
    
    public boolean iniciarServidorPeer() {
        try {
            servidorPeer = new ServerSocket(miPuertoPeer);
            activo = true;
            
            System.out.println(">>> Servidor P2P iniciado en puerto: " + miPuertoPeer);
            
            hiloServidor = new Thread(this::aceptarConexionesPeer);
            hiloServidor.setDaemon(true);
            hiloServidor.start();
            
            return true;
            
        } catch (IOException e) {
            System.err.println("[ERROR P2P] No se pudo iniciar servidor: " + e.getMessage());
            return false;
        }
    }
    
    private void aceptarConexionesPeer() {
        while (activo) {
            try {
                Socket socketPeer = servidorPeer.accept();
                
                Thread hiloPeer = new Thread(() -> manejarConexionPeer(socketPeer));
                hiloPeer.setDaemon(true);
                hiloPeer.start();
                
            } catch (IOException e) {
                if (activo) {
                    System.err.println("[ERROR P2P] Error aceptando conexión: " + e.getMessage());
                }
            }
        }
    }
    
    private void manejarConexionPeer(Socket socket) {
        try {
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8")
            );
            PrintWriter salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                true
            );
            
            // ✅ CRÍTICO: Esperar mensaje de handshake con ID del peer
            String primerMensaje = entrada.readLine();
            if (primerMensaje != null) {
                try {
                    JsonObject json = JsonParser.parseString(primerMensaje).getAsJsonObject();
                    if (json.has("tipo") && "handshake_peer".equals(json.get("tipo").getAsString())) {
                        int peerId = json.get("jugadorId").getAsInt();
                        
                        // Registrar conexión entrante
                        ConexionPeer conexion = new ConexionPeer(peerId, socket, entrada, salida);
                        peerConectados.put(peerId, conexion);
                        
                        System.out.println("[P2P] Peer " + peerId + " conectado (entrante)");
                        
                        // Continuar escuchando mensajes de este peer
                        String mensaje;
                        while ((mensaje = entrada.readLine()) != null) {
                            procesarMensajePeer(mensaje);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[P2P] Error en handshake: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            // Conexión cerrada
        }
    }
    
    public boolean conectarAPeer(int jugadorId, String ip, int puerto) {
        if (peerConectados.containsKey(jugadorId)) {
            return true;
        }
        
        try {
            Socket socket = new Socket(ip, puerto);
            
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8")
            );
            PrintWriter salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                true
            );
            
            // ✅ CRÍTICO: Enviar handshake con mi ID
            JsonObject handshake = new JsonObject();
            handshake.addProperty("tipo", "handshake_peer");
            handshake.addProperty("jugadorId", miJugadorId);
            salida.println(handshake.toString());
            salida.flush();
            System.out.println("[P2P] Handshake enviado a peer " + jugadorId);
            
            ConexionPeer conexion = new ConexionPeer(jugadorId, socket, entrada, salida);
            peerConectados.put(jugadorId, conexion);
            
            System.out.println("[P2P] Conectado a peer " + jugadorId + " en " + ip + ":" + puerto);
            
            Thread hiloEscucha = new Thread(() -> escucharPeer(conexion));
            hiloEscucha.setDaemon(true);
            hiloEscucha.start();
            
            return true;
            
        } catch (IOException e) {
            System.err.println("[ERROR P2P] No se pudo conectar a peer " + jugadorId + ": " + e.getMessage());
            return false;
        }
    }
    
    private void escucharPeer(ConexionPeer conexion) {
        try {
            String mensaje;
            while ((mensaje = conexion.entrada.readLine()) != null) {
                procesarMensajePeer(mensaje);
            }
        } catch (IOException e) {
            peerConectados.remove(conexion.jugadorId);
        }
    }
    
    public boolean enviarAPeer(int jugadorId, String mensaje) {
        ConexionPeer conexion = peerConectados.get(jugadorId);
        
        if (conexion == null) {
            return false;
        }
        
        try {
            conexion.salida.println(mensaje);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void broadcastAPeers(String mensaje) {
        int mensajesEnviados = 0;
        for (ConexionPeer conexion : peerConectados.values()) {
            // ✅ CRÍTICO: NO enviar mensaje a uno mismo
            if (conexion.jugadorId == miJugadorId) {
                System.out.println("[P2P] Saltando envío a mí mismo (ID: " + miJugadorId + ")");
                continue;
            }
            
            try {
                conexion.salida.println(mensaje);
                conexion.salida.flush(); // ✅ CRÍTICO: Forzar envío inmediato
                mensajesEnviados++;
            } catch (Exception e) {
                System.err.println("[P2P] Error enviando a peer " + conexion.jugadorId + ": " + e.getMessage());
            }
        }
        System.out.println("[P2P] Broadcast enviado a " + mensajesEnviados + " peers (de " + peerConectados.size() + " conectados)");
    }
    
    /**
     * ✅ CORREGIDO: Procesa mensajes P2P inmediatamente (sin esperar servidor)
     */
    private void procesarMensajePeer(String mensajeJson) {
        try {
            JsonObject json = JsonParser.parseString(mensajeJson).getAsJsonObject();
            String tipo = json.has("tipo") ? json.get("tipo").getAsString() : "";
            
            switch (tipo) {
                case "tirada_dados_peer":
                    // Notificación de dados (solo visual)
                    if (vista != null) {
                        String nombre = json.get("jugadorNombre").getAsString();
                        int dado1 = json.get("dado1").getAsInt();
                        int dado2 = json.get("dado2").getAsInt();
                        
                        System.out.println("[P2P] " + nombre + " tiró dados: [" + dado1 + "][" + dado2 + "]");
                        vista.mostrarDadosOtroJugador(nombre, dado1, dado2);
                    }
                    break;
                    
                case "movimiento_peer":
                    // ✅ CRÍTICO: Procesar movimiento INMEDIATAMENTE
                    if (controlador != null) {
                        String nombre = json.get("jugadorNombre").getAsString();
                        int fichaId = json.get("fichaId").getAsInt();
                        String accion = json.get("accion").getAsString();
                        int valorDado = json.get("valorDado").getAsInt();
                        
                        System.out.println("[P2P RECV] " + nombre + " - " + accion + " ficha #" + fichaId + 
                                         " con dado " + valorDado);
                        
                        // Delegar a ClienteControlador para actualizar UI
                        controlador.procesarMovimientoPeerRecibido(nombre, fichaId, accion, valorDado);
                    }
                    break;
                    
                case "captura_peer":
                    // Notificación de captura
                    if (vista != null) {
                        String capturador = json.get("capturadorNombre").getAsString();
                        System.out.println("[P2P] " + capturador + " capturó una ficha!");
                        vista.mostrarCaptura(capturador);
                    }
                    break;
                    
                case "meta_peer":
                    // Notificación de llegada a meta
                    if (vista != null) {
                        String jugador = json.get("jugadorNombre").getAsString();
                        System.out.println("[P2P] " + jugador + " llegó a la meta!");
                        vista.mostrarLlegadaMeta(jugador);
                    }
                    break;
                    
                case "cambio_turno_peer":
                    // Notificación de cambio de turno
                    if (vista != null) {
                        String jugador = json.get("jugadorNombre").getAsString();
                        System.out.println("[P2P] Turno de: " + jugador);
                        vista.mostrarCambioTurno(jugador);
                    }
                    break;
                    
                default:
                    System.out.println("[P2P] Mensaje desconocido: " + tipo);
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("[ERROR P2P] Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void cerrar() {
        activo = false;
        
        for (ConexionPeer conexion : peerConectados.values()) {
            try {
                conexion.socket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }
        peerConectados.clear();
        
        try {
            if (servidorPeer != null && !servidorPeer.isClosed()) {
                servidorPeer.close();
            }
        } catch (IOException e) {
            // Ignorar
        }
    }
    
    public int getMiPuertoPeer() {
        return miPuertoPeer;
    }
    
    public boolean estaActivo() {
        return activo;
    }
    
    public int getNumeroPeersConectados() {
        return peerConectados.size();
    }
    
    private static class ConexionPeer {
        final int jugadorId;
        final Socket socket;
        final BufferedReader entrada;
        final PrintWriter salida;
        
        ConexionPeer(int jugadorId, Socket socket, BufferedReader entrada, PrintWriter salida) {
            this.jugadorId = jugadorId;
            this.socket = socket;
            this.entrada = entrada;
            this.salida = salida;
        }
    }
}