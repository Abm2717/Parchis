package controlador.servidor;

import modelo.servicios.PersistenciaServicio;
import modelo.servicios.SalaServicio;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import modelo.servicios.GestorMotores;
import vista.VistaServidor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * ✅ ACTUALIZADO: Servidor central híbrido P2P
 * - Mantiene el estado oficial del juego (MotorJuego)
 * - Valida todas las jugadas (100ms latencia)
 * - Coordina conexiones P2P entre jugadores
 * - Los movimientos se comunican P2P (10ms latencia)
 */
public class ServidorCentral {
    
    private static final int PUERTO_DEFAULT = 5000;
    private static final int MAX_CLIENTES = 50;
    
    private ServerSocket serverSocket;
    private final int puerto;
    private boolean ejecutando;
    
    private ExecutorService poolClientes;
    
    private final Map<String, ClienteHandler> clientesConectados;
    
    // ✅ NUEVO: Mapeo de jugadores a puertos P2P
    private final Map<Integer, InfoPeer> infosPeers;
    
    private final PersistenciaServicio persistencia;
    private final SalaServicio salaServicio;
    
    private int contadorSesiones;
    
    private ScheduledExecutorService schedulerLimpieza;
    
    public ServidorCentral() {
        this(PUERTO_DEFAULT);
    }
    
    public ServidorCentral(int puerto) {
        this.puerto = puerto;
        this.ejecutando = false;
        this.clientesConectados = new ConcurrentHashMap<>();
        this.infosPeers = new ConcurrentHashMap<>(); // ✅ NUEVO
        this.persistencia = PersistenciaServicio.getInstancia();
        this.salaServicio = SalaServicio.getInstancia();
        this.contadorSesiones = 0;
        this.poolClientes = Executors.newFixedThreadPool(MAX_CLIENTES);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (ejecutando) {
                detenerInterno();
            }
        }));
    }
    
    public void iniciar() {
        try {
            serverSocket = new ServerSocket(puerto);
            ejecutando = true;
            
            System.out.println("\n================================================");
            System.out.println("   SERVIDOR PARCHIS - ARQUITECTURA HIBRIDA");
            System.out.println("================================================");
            System.out.println("Puerto: " + puerto);
            System.out.println("Modo: P2P + Servidor de Estado");
            System.out.println("Esperando conexiones...");
            System.out.println("================================================\n");
            
            while (ejecutando) {
                try {
                    Socket socketCliente = serverSocket.accept();
                    manejarNuevaConexion(socketCliente);
                } catch (IOException e) {
                    if (ejecutando) {
                        System.err.println("Error aceptando conexion: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error iniciando servidor en puerto " + puerto);
            System.err.println(e.getMessage());
        } finally {
            detenerInterno();
        }
    }
    
    public void detener() {
        ejecutando = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignorar
        }
    }
    
    private void detenerInterno() {
        VistaServidor.mostrarCierreServidor();
        
        ejecutando = false;
        
        for (ClienteHandler cliente : clientesConectados.values()) {
            cliente.desconectar();
        }
        clientesConectados.clear();
        
        if (poolClientes != null) {
            poolClientes.shutdown();
        }
        
        if (schedulerLimpieza != null) {
            schedulerLimpieza.shutdown();
        }
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando servidor: " + e.getMessage());
        }
        
        System.out.println("Servidor detenido.");
    }

    private void manejarNuevaConexion(Socket socketCliente) {
        try {
            String sessionId = generarSessionId();
            
            ClienteHandler cliente = new ClienteHandler(socketCliente, sessionId, this);
            
            clientesConectados.put(sessionId, cliente);
            
            poolClientes.execute(cliente);
            
            System.out.println(">>> Nueva conexion desde " + 
                socketCliente.getInetAddress().getHostAddress() + 
                " [Session: " + sessionId + "]");
            System.out.println(">>> Clientes conectados: " + clientesConectados.size());
            
        } catch (Exception e) {
            System.err.println("Error manejando nueva conexion: " + e.getMessage());
        }
    }
    
    public void removerCliente(String sessionId) {
        ClienteHandler cliente = clientesConectados.remove(sessionId);
        
        if (cliente != null) {
            modelo.Jugador.Jugador jugador = persistencia.obtenerJugadorPorSession(sessionId);
            if (jugador != null) {
                persistencia.actualizarConexion(jugador.getId(), false);
                
                // ✅ NUEVO: Remover info P2P
                infosPeers.remove(jugador.getId());
                
                java.util.Optional<modelo.partida.Partida> partidaOpt = 
                    persistencia.obtenerPartidaDeJugador(jugador.getId());
                
                if (partidaOpt.isPresent()) {
                    String mensaje = crearMensajeDesconexion(jugador);
                    broadcastAPartida(partidaOpt.get().getId(), mensaje, sessionId);
                }
                
                VistaServidor.mostrarDesconexion(
                    sessionId, 
                    jugador.getNombre(), 
                    clientesConectados.size()
                );
            }
        }
    }
    
    /**
     * ✅ NUEVO: Registra la información P2P de un jugador
     */
    public void registrarInfoPeer(int jugadorId, String ip, int puertoPeer) {
        InfoPeer info = new InfoPeer(jugadorId, ip, puertoPeer);
        infosPeers.put(jugadorId, info);
        System.out.println(">>> Info P2P registrada - Jugador " + jugadorId + 
                         " en " + ip + ":" + puertoPeer);
    }
    
    /**
     * ✅ NUEVO: Envía información de peers a un jugador
     */
    public void enviarInfoPeersAJugador(int jugadorId, int partidaId) {
        modelo.partida.Partida partida = persistencia.obtenerPartida(partidaId);
        if (partida == null) return;
        
        JsonArray peersArray = new JsonArray();
        
        for (modelo.Jugador.Jugador j : partida.getJugadores()) {
            InfoPeer info = infosPeers.get(j.getId());
            if (info != null) {
                JsonObject peerObj = new JsonObject();
                peerObj.addProperty("id", info.jugadorId);
                peerObj.addProperty("ip", info.ip);
                peerObj.addProperty("puerto", info.puertoPeer);
                peersArray.add(peerObj);
            }
        }
        
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "info_peers");
        mensaje.add("peers", peersArray);
        
        modelo.Jugador.Jugador jugador = partida.getJugadorPorId(jugadorId);
        if (jugador != null) {
            ClienteHandler handler = clientesConectados.get(jugador.getSessionId());
            if (handler != null) {
                handler.enviarMensaje(mensaje.toString());
            }
        }
    }
    
    public ClienteHandler getCliente(String sessionId) {
        return clientesConectados.get(sessionId);
    }
    
    public List<ClienteHandler> getClientesConectados() {
        return new ArrayList<>(clientesConectados.values());
    }

    public void broadcastATodos(String mensaje) {
        for (ClienteHandler cliente : clientesConectados.values()) {
            cliente.enviarMensaje(mensaje);
        }
    }
    
    public void broadcastExcepto(String mensaje, String sessionIdExcluido) {
        for (Map.Entry<String, ClienteHandler> entry : clientesConectados.entrySet()) {
            if (!entry.getKey().equals(sessionIdExcluido)) {
                entry.getValue().enviarMensaje(mensaje);
            }
        }
    }
    
    public void broadcastAPartida(int partidaId, String mensaje, String sessionIdExcluido) {
        modelo.partida.Partida partida = persistencia.obtenerPartida(partidaId);
        if (partida == null) return;
        
        for (modelo.Jugador.Jugador jugador : partida.getJugadores()) {
            String sessionId = jugador.getSessionId();
            if (sessionId != null && !sessionId.equals(sessionIdExcluido)) {
                ClienteHandler cliente = clientesConectados.get(sessionId);
                if (cliente != null) {
                    cliente.enviarMensaje(mensaje);
                }
            }
        }
    }
    
    public void broadcastAPartida(int partidaId, String mensaje) {
        broadcastAPartida(partidaId, mensaje, null);
    }

    private synchronized String generarSessionId() {
        contadorSesiones++;
        return "session_" + contadorSesiones + "_" + System.currentTimeMillis();
    }
    
    private String crearMensajeDesconexion(modelo.Jugador.Jugador jugador) {
        return String.format(
            "{\"tipo\":\"jugador_desconectado\",\"jugadorId\":%d,\"nombre\":\"%s\"}",
            jugador.getId(),
            jugador.getNombre()
        );
    }
    
    public boolean estaEjecutando() {
        return ejecutando;
    }
    
    public int getNumeroClientesConectados() {
        return clientesConectados.size();
    }
    
    public String getEstado() {
        return String.format(
            "Servidor[puerto=%d, clientes=%d, partidas=%d, ejecutando=%s]",
            puerto,
            clientesConectados.size(),
            persistencia.getTotalPartidas(),
            ejecutando
        );
    }
    
    public void mostrarEstadisticas() {
        VistaServidor.mostrarEstadisticas(
            clientesConectados.size(),
            persistencia.getTotalPartidas(),
            persistencia.getTotalJugadores()
        );
    }
    
    private void iniciarLimpiezaAutomatica() {
        schedulerLimpieza = Executors.newScheduledThreadPool(1);
        schedulerLimpieza.scheduleAtFixedRate(() -> {
            GestorMotores.getInstancia().limpiarMotoresFinalizados();
        }, 10, 10, TimeUnit.MINUTES);
    }
    
    /**
     * ✅ NUEVO: Clase para almacenar información P2P de un peer
     */
    private static class InfoPeer {
        final int jugadorId;
        final String ip;
        final int puertoPeer;
        
        InfoPeer(int jugadorId, String ip, int puertoPeer) {
            this.jugadorId = jugadorId;
            this.ip = ip;
            this.puertoPeer = puertoPeer;
        }
    }
}