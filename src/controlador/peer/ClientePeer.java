package controlador.peer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vista.VistaCliente;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ ACTUALIZADO: Procesamiento completo de mensajes P2P de juego
 * - Recibe movimientos directamente de otros peers (10ms)
 * - Actualiza UI de forma optimista
 * - El servidor valida en paralelo (100ms)
 */
public class ClientePeer {
    
    private final int miPuertoPeer;
    private ServerSocket servidorPeer;
    private final Map<Integer, ConexionPeer> peerConectados;
    private boolean activo;
    private Thread hiloServidor;
    private VistaCliente vista; // ✅ NUEVO: Referencia a la vista
    
    public ClientePeer(int puerto) {
        this.miPuertoPeer = puerto;
        this.peerConectados = new ConcurrentHashMap<>();
        this.activo = false;
        this.vista = null;
    }
    
    /**
     * ✅ NUEVO: Asigna la vista para actualizar UI con mensajes P2P
     */
    public void setVista(VistaCliente vista) {
        this.vista = vista;
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
            
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                procesarMensajePeer(mensaje);
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
        for (ConexionPeer conexion : peerConectados.values()) {
            try {
                conexion.salida.println(mensaje);
            } catch (Exception e) {
                // Ignorar errores individuales
            }
        }
    }
    
    /**
     * ✅ ACTUALIZADO: Procesa mensajes P2P de juego en tiempo real
     */
    private void procesarMensajePeer(String mensajeJson) {
        try {
            JsonObject json = JsonParser.parseString(mensajeJson).getAsJsonObject();
            String tipo = json.has("tipo") ? json.get("tipo").getAsString() : "";
            
            switch (tipo) {
                case "tirada_dados_peer":
                    // Otro jugador tiró dados (notificación P2P)
                    if (vista != null) {
                        String nombre = json.get("jugadorNombre").getAsString();
                        int dado1 = json.get("dado1").getAsInt();
                        int dado2 = json.get("dado2").getAsInt();
                        
                        System.out.println("[P2P] " + nombre + " tiró dados: [" + dado1 + "][" + dado2 + "]");
                        vista.mostrarDadosOtroJugador(nombre, dado1, dado2);
                    }
                    break;
                    
                case "movimiento_peer":
                    // Movimiento de ficha (actualización optimista)
                    if (vista != null) {
                        String nombre = json.get("jugadorNombre").getAsString();
                        int fichaId = json.get("fichaId").getAsInt();
                        int desde = json.get("desde").getAsInt();
                        int hasta = json.get("hasta").getAsInt();
                        
                        System.out.println("[P2P] " + nombre + " movió ficha #" + fichaId + 
                                         " (" + desde + " → " + hasta + ")");
                        vista.mostrarMovimientoOtroJugador(nombre, fichaId, desde, hasta);
                    }
                    break;
                    
                case "captura_peer":
                    // Captura de ficha
                    if (vista != null) {
                        String capturador = json.get("capturadorNombre").getAsString();
                        System.out.println("[P2P] " + capturador + " capturó una ficha!");
                        vista.mostrarCaptura(capturador);
                    }
                    break;
                    
                case "meta_peer":
                    // Llegada a meta
                    if (vista != null) {
                        String jugador = json.get("jugadorNombre").getAsString();
                        System.out.println("[P2P] " + jugador + " llegó a la meta!");
                        vista.mostrarLlegadaMeta(jugador);
                    }
                    break;
                    
                case "cambio_turno_peer":
                    // Cambio de turno
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
    
    /**
     * ✅ NUEVO: Obtiene número de peers conectados
     */
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