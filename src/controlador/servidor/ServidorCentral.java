// ========================================
// SERVIDORCENTRAL.JAVA (SIN MAIN)
// ========================================
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

/**
 * Servidor central del juego de Parchis.
 * 
 * Responsabilidades:
 * Aceptar conexiones de clientes
 * Crear y gestionar ClienteHandlers
 * Coordinar broadcast de mensajes
 * Gestionar desconexiones
 * 
 */
public class ServidorCentral {
    
    // Configuracion del servidor
    private static final int PUERTO_DEFAULT = 5000;
    private static final int MAX_CLIENTES = 50;
    
    // Componentes del servidor
    private ServerSocket serverSocket;
    private final int puerto;
    private boolean ejecutando;
    
    // Pool de threads para manejar clientes
    private ExecutorService poolClientes;
    
    // Lista de clientes conectados
    private final Map<String, ClienteHandler> clientesConectados;
    
    // Servicios
    private final PersistenciaServicio persistencia;
    private final SalaServicio salaServicio;
    
    // Contador para IDs de sesion
    private int contadorSesiones;
    
    // Limpieza automatica
    private ScheduledExecutorService schedulerLimpieza;
    

    public ServidorCentral() {
        this(PUERTO_DEFAULT);
    }
    
    public ServidorCentral(int puerto) {
        this.puerto = puerto;
        this.ejecutando = false;
        this.clientesConectados = new ConcurrentHashMap<>();
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
    
    
    
    /**
     * Inicia el servidor y comienza a aceptar conexiones.
     */
    public void iniciar() {
        try {
            serverSocket = new ServerSocket(puerto);
            ejecutando = true;
            
            VistaServidor.mostrarBannerInicio(puerto);
            
            // Loop principal - aceptar clientes
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
    
    /**
     * Detiene el servidor y cierra todas las conexiones.
     */
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
    
    /**
     * Detencion interna con limpieza completa.
     */
    private void detenerInterno() {
        VistaServidor.mostrarCierreServidor();
        
        ejecutando = false;
        
        // Cerrar todas las conexiones de clientes
        for (ClienteHandler cliente : clientesConectados.values()) {
            cliente.desconectar();
        }
        clientesConectados.clear();
        
        // Cerrar el pool de threads
        if (poolClientes != null) {
            poolClientes.shutdown();
        }
        
        // Cerrar scheduler de limpieza
        if (schedulerLimpieza != null) {
            schedulerLimpieza.shutdown();
        }
        
        // Cerrar el servidor socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando servidor: " + e.getMessage());
        }
        
        System.out.println("Servidor detenido.");
    }

    /**
     * Maneja una nueva conexion de cliente.
     */
    private void manejarNuevaConexion(Socket socketCliente) {
        try {
            // Generar session ID unico
            String sessionId = generarSessionId();
            
            // Crear el handler para este cliente
            ClienteHandler cliente = new ClienteHandler(socketCliente, sessionId, this);
            
            // Registrar el cliente
            clientesConectados.put(sessionId, cliente);
            
            // Ejecutar el handler en el pool de threads
            poolClientes.execute(cliente);
            
            VistaServidor.mostrarNuevaConexion(
                sessionId, 
                socketCliente.getInetAddress().getHostAddress(),
                clientesConectados.size()
            );
            
        } catch (Exception e) {
            System.err.println("Error manejando nueva conexion: " + e.getMessage());
        }
    }
    
    /**
     * Remueve un cliente desconectado.
     */
    public void removerCliente(String sessionId) {
        ClienteHandler cliente = clientesConectados.remove(sessionId);
        
        if (cliente != null) {
            // Actualizar estado del jugador en persistencia
            modelo.Jugador.Jugador jugador = persistencia.obtenerJugadorPorSession(sessionId);
            if (jugador != null) {
                persistencia.actualizarConexion(jugador.getId(), false);
                
                // Notificar a otros jugadores en la misma partida
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
     * Obtiene un cliente por su session ID.
     */
    public ClienteHandler getCliente(String sessionId) {
        return clientesConectados.get(sessionId);
    }
    
    /**
     * Obtiene todos los clientes conectados.
     */
    public List<ClienteHandler> getClientesConectados() {
        return new ArrayList<>(clientesConectados.values());
    }

    /**
     * Envia un mensaje a todos los clientes conectados.
     */
    public void broadcastATodos(String mensaje) {
        for (ClienteHandler cliente : clientesConectados.values()) {
            cliente.enviarMensaje(mensaje);
        }
    }
    
    /**
     * Envia un mensaje a todos los clientes excepto uno.
     */
    public void broadcastExcepto(String mensaje, String sessionIdExcluido) {
        for (Map.Entry<String, ClienteHandler> entry : clientesConectados.entrySet()) {
            if (!entry.getKey().equals(sessionIdExcluido)) {
                entry.getValue().enviarMensaje(mensaje);
            }
        }
    }
    
    /**
     * Envia un mensaje a todos los jugadores de una partida.
     */
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
    
    /**
     * Envia un mensaje a todos los jugadores de una partida (sin excluir ninguno).
     */
    public void broadcastAPartida(int partidaId, String mensaje) {
        broadcastAPartida(partidaId, mensaje, null);
    }

    /**
     * Genera un session ID unico.
     */
    private synchronized String generarSessionId() {
        contadorSesiones++;
        return "SESSION_" + System.currentTimeMillis() + "_" + contadorSesiones;
    }
    
    /**
     * Crea un mensaje JSON de desconexion.
     */
    private String crearMensajeDesconexion(modelo.Jugador.Jugador jugador) {
        return String.format(
            "{\"tipo\":\"jugador_desconectado\",\"jugadorId\":%d,\"nombre\":\"%s\"}",
            jugador.getId(),
            jugador.getNombre()
        );
    }
    
    /**
     * Verifica si el servidor esta ejecutando.
     */
    public boolean estaEjecutando() {
        return ejecutando;
    }
    
    /**
     * Obtiene el numero de clientes conectados.
     */
    public int getNumeroClientesConectados() {
        return clientesConectados.size();
    }
    
    /**
     * Obtiene informacion del servidor.
     */
    public String getEstado() {
        return String.format(
            "Servidor[puerto=%d, clientes=%d, partidas=%d, ejecutando=%s]",
            puerto,
            clientesConectados.size(),
            persistencia.getTotalPartidas(),
            ejecutando
        );
    }
    
    /**
     * Muestra estadisticas del servidor.
     */
    public void mostrarEstadisticas() {
        VistaServidor.mostrarEstadisticas(
            clientesConectados.size(),
            persistencia.getTotalPartidas(),
            persistencia.getTotalJugadores()
        );
    }
    
    /**
     * Inicia limpieza automatica de motores (opcional).
     */
    private void iniciarLimpiezaAutomatica() {
        schedulerLimpieza = Executors.newScheduledThreadPool(1);
        schedulerLimpieza.scheduleAtFixedRate(() -> {
            GestorMotores.getInstancia().limpiarMotoresFinalizados();
        }, 10, 10, TimeUnit.MINUTES);
    }
}