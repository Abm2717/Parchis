package controlador.servidor;

import modelo.servicios.GestorMotores;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Servidor central del juego de Parchís - Arquitectura Híbrida.
 * 
 * En la arquitectura híbrida:
 * - El servidor mantiene el estado oficial del juego
 * - Valida todas las acciones de los jugadores
 * - Los peers se comunican entre sí para notificaciones rápidas
 * - El servidor hace broadcast del estado validado
 * 
 * Thread-safe y soporta múltiples clientes concurrentes.
 */
public class ServidorCentral {
    
    private static final int PUERTO_DEFAULT = 5000;
    private static final int MAX_CLIENTES = 50;
    
    private ServerSocket serverSocket;
    private final int puerto;
    private boolean ejecutando;
    private final ExecutorService poolClientes;
    private final Map<String, ClienteHandler> clientesConectados;
    private final GestorMotores gestorMotores;
    private int contadorSesiones;
    
    public ServidorCentral() {
        this(PUERTO_DEFAULT);
    }
    
    public ServidorCentral(int puerto) {
        this.puerto = puerto;
        this.ejecutando = false;
        this.poolClientes = Executors.newFixedThreadPool(MAX_CLIENTES);
        this.clientesConectados = new ConcurrentHashMap<>();
        this.gestorMotores = GestorMotores.getInstancia();
        this.contadorSesiones = 0;
    }
    
    /**
     * Inicia el servidor y comienza a aceptar conexiones.
     */
    public void iniciar() {
        try {
            serverSocket = new ServerSocket(puerto);
            ejecutando = true;
            
            System.out.println("================================================");
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
            System.err.println("Error iniciando servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            detenerInterno();
        }
    }
    
    /**
     * Maneja una nueva conexión de cliente.
     */
    private void manejarNuevaConexion(Socket socketCliente) {
        String sessionId = generarSessionId();
        String ipCliente = socketCliente.getInetAddress().getHostAddress();
        
        System.out.println(">>> Nueva conexion desde " + ipCliente + " [Session: " + sessionId + "]");
        
        ClienteHandler handler = new ClienteHandler(socketCliente, sessionId, this);
        clientesConectados.put(sessionId, handler);
        poolClientes.execute(handler);
        
        System.out.println(">>> Clientes conectados: " + clientesConectados.size());
    }
    
    /**
     * Detiene el servidor.
     */
    public void detener() {
        System.out.println("\n>>> Deteniendo servidor...");
        ejecutando = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando servidor: " + e.getMessage());
        }
        
        detenerInterno();
    }
    
    /**
     * Detiene recursos internos.
     */
    private void detenerInterno() {
        // Cerrar todas las conexiones
        for (ClienteHandler handler : clientesConectados.values()) {
            handler.desconectar();
        }
        clientesConectados.clear();
        
        // Detener pool de threads
        poolClientes.shutdown();
        
        // Limpiar gestor de motores
        gestorMotores.limpiarTodosLosHandlers();
        
        System.out.println(">>> Servidor detenido");
    }
    
    /**
     * Remueve un cliente del servidor.
     */
    public void removerCliente(String sessionId) {
        ClienteHandler handler = clientesConectados.remove(sessionId);
        if (handler != null) {
            System.out.println(">>> Cliente removido: " + sessionId);
            System.out.println(">>> Clientes conectados: " + clientesConectados.size());
            
            // Remover handler del gestor de motores
            if (handler.getJugador() != null) {
                gestorMotores.removerHandler(handler.getJugador().getId());
            }
        }
    }
    
    /**
     * Obtiene un cliente por session ID.
     */
    public ClienteHandler getCliente(String sessionId) {
        return clientesConectados.get(sessionId);
    }
    
    /**
     * Obtiene lista de todos los clientes conectados.
     */
    public List<ClienteHandler> getClientesConectados() {
        return new ArrayList<>(clientesConectados.values());
    }
    
    // ============================
    // BROADCAST DE MENSAJES
    // ============================
    
    /**
     * Envía un mensaje a todos los clientes conectados.
     */
    public void broadcastATodos(String mensaje) {
        for (ClienteHandler handler : clientesConectados.values()) {
            if (handler.isConectado()) {
                handler.enviarMensaje(mensaje);
            }
        }
    }
    
    /**
     * Envía un mensaje a todos excepto uno.
     */
    public void broadcastExcepto(String mensaje, String sessionIdExcluido) {
        for (Map.Entry<String, ClienteHandler> entry : clientesConectados.entrySet()) {
            if (!entry.getKey().equals(sessionIdExcluido) && entry.getValue().isConectado()) {
                entry.getValue().enviarMensaje(mensaje);
            }
        }
    }
    
    /**
     * Envía un mensaje a todos los jugadores de una partida.
     */
    public void broadcastAPartida(int partidaId, String mensaje) {
        broadcastAPartida(partidaId, mensaje, null);
    }
    
    /**
     * Envía un mensaje a todos los jugadores de una partida excepto uno.
     */
    public void broadcastAPartida(int partidaId, String mensaje, String sessionIdExcluido) {
        for (ClienteHandler handler : clientesConectados.values()) {
            if (handler.isConectado() && 
                handler.getJugador() != null) {
                
                // Verificar si el jugador está en la partida
                boolean estaEnPartida = false; // Aquí deberías verificar con el MotorJuego
                
                if (estaEnPartida && 
                    (sessionIdExcluido == null || !handler.getSessionId().equals(sessionIdExcluido))) {
                    handler.enviarMensaje(mensaje);
                }
            }
        }
    }
    
    // ============================
    // UTILIDADES
    // ============================
    
    /**
     * Genera un ID de sesión único.
     */
    private synchronized String generarSessionId() {
        return "session_" + (++contadorSesiones) + "_" + System.currentTimeMillis();
    }
    
    /**
     * Verifica si el servidor está ejecutando.
     */
    public boolean estaEjecutando() {
        return ejecutando;
    }
    
    /**
     * Obtiene el número de clientes conectados.
     */
    public int getNumeroClientesConectados() {
        return clientesConectados.size();
    }
    
    /**
     * Obtiene el estado del servidor.
     */
    public String getEstado() {
        return String.format("Servidor[puerto=%d, ejecutando=%s, clientes=%d]",
            puerto, ejecutando, clientesConectados.size());
    }
    
    /**
     * Muestra estadísticas del servidor.
     */
    public void mostrarEstadisticas() {
        System.out.println("\n================================================");
        System.out.println("   ESTADISTICAS DEL SERVIDOR");
        System.out.println("================================================");
        System.out.println("Puerto: " + puerto);
        System.out.println("Estado: " + (ejecutando ? "EJECUTANDO" : "DETENIDO"));
        System.out.println("Clientes conectados: " + clientesConectados.size());
        System.out.println("Motores activos: " + gestorMotores.getCantidadMotores());
        System.out.println("Handlers registrados: " + gestorMotores.getCantidadHandlers());
        
        if (!clientesConectados.isEmpty()) {
            System.out.println("\nClientes:");
            for (Map.Entry<String, ClienteHandler> entry : clientesConectados.entrySet()) {
                ClienteHandler handler = entry.getValue();
                String jugadorInfo = handler.getJugador() != null ? 
                    handler.getJugador().getNombre() : "No registrado";
                System.out.println("  - " + entry.getKey() + " -> " + jugadorInfo);
            }
        }
        
        System.out.println("================================================\n");
    }
    
    // ============================
    // MAIN
    // ============================
    
    public static void main(String[] args) {
        int puerto = PUERTO_DEFAULT;
        
        // Permitir especificar puerto por argumento
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto invalido, usando default: " + PUERTO_DEFAULT);
            }
        }
        
        ServidorCentral servidor = new ServidorCentral(puerto);
        
        // Thread para comandos del servidor
        Thread hiloComandos = new Thread(() -> {
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            while (servidor.estaEjecutando()) {
                String comando = scanner.nextLine().trim().toLowerCase();
                
                switch (comando) {
                    case "stats":
                        servidor.mostrarEstadisticas();
                        break;
                    case "stop":
                        System.out.println("Deteniendo servidor...");
                        servidor.detener();
                        System.exit(0);
                        break;
                    case "help":
                        System.out.println("\nComandos disponibles:");
                        System.out.println("  stats - Muestra estadisticas");
                        System.out.println("  stop  - Detiene el servidor");
                        System.out.println("  help  - Muestra esta ayuda\n");
                        break;
                    default:
                        if (!comando.isEmpty()) {
                            System.out.println("Comando desconocido. Escribe 'help' para ver comandos.");
                        }
                }
            }
            scanner.close();
        });
        hiloComandos.setDaemon(true);
        hiloComandos.start();
        
        // Iniciar servidor (bloquea hasta que se detenga)
        servidor.iniciar();
    }
}