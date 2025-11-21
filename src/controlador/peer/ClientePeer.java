package controlador.peer;

import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.cache.CachePartida;
import modelo.cache.CachePartida.JugadorCache;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * ClientePeer - Cliente híbrido para arquitectura P2P + Servidor.
 * 
 * Responsabilidades:
 * - Conectarse al servidor central (para validación)
 * - Conectarse a otros peers (para notificaciones rápidas P2P)
 * - Enviar acciones al servidor
 * - Notificar a peers directamente
 * - Sincronizar estado local con servidor
 * 
 * Flujo típico:
 * 1. Usuario hace acción
 * 2. Notifica P2P a otros peers (10ms - optimista)
 * 3. Envía al servidor para validación (100ms)
 * 4. Recibe confirmación del servidor
 * 5. Sincroniza estado con servidor
 */
public class ClientePeer {
    
    // Conexión al servidor central
    private Socket socketServidor;
    private BufferedReader entradaServidor;
    private PrintWriter salidaServidor;
    private String sessionId;
    
    // Información del jugador
    private Jugador jugadorLocal;
    private int jugadorId;
    
    // Conexiones P2P con otros peers
    private final Map<Integer, ConexionPeer> conexionesPeers;
    private ServerSocket serverSocketP2P;
    private int puertoP2P;
    
    // Sincronización
    private SincronizadorEstado sincronizador;
    private CachePartida cache;
    
    // Estado
    private boolean conectado;
    private boolean enPartida;
    
    // Threads
    private Thread threadEscuchaServidor;
    private Thread threadEscuchaPeers;
    
    public ClientePeer() {
        this.conexionesPeers = new ConcurrentHashMap<>();
        this.conectado = false;
        this.enPartida = false;
        this.sincronizador = new SincronizadorEstado();
        this.cache = new CachePartida();
    }
    
    public void setCache(CachePartida cache) {
        this.cache = cache;
    }
    
    public CachePartida getCache() {
        return this.cache;
    }
    
    // ============================
    // CONEXIÓN AL SERVIDOR
    // ============================
    
    /**
     * Conecta al servidor central.
     */
    public boolean conectarAlServidor(String host, int puerto) {
        try {
            socketServidor = new Socket(host, puerto);
            entradaServidor = new BufferedReader(
                new InputStreamReader(socketServidor.getInputStream(), "UTF-8")
            );
            salidaServidor = new PrintWriter(
                new OutputStreamWriter(socketServidor.getOutputStream(), "UTF-8"),
                true
            );
            
            conectado = true;
            
            // Iniciar thread para escuchar mensajes del servidor
            threadEscuchaServidor = new Thread(this::escucharServidor);
            threadEscuchaServidor.start();
            
            System.out.println(">>> Conectado al servidor: " + host + ":" + puerto);
            
            // Esperar mensaje de bienvenida
            String bienvenida = entradaServidor.readLine();
            if (bienvenida != null) {
                procesarMensajeServidor(bienvenida);
            }
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Error conectando al servidor: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registra el jugador en el servidor.
     */
    public boolean registrarJugador(String nombre) {
        if (!conectado) {
            System.err.println("No estas conectado al servidor");
            return false;
        }
        
        try {
            JsonObject mensaje = new JsonObject();
            mensaje.addProperty("tipo", "registrar");
            mensaje.addProperty("nombre", nombre);
            
            enviarAlServidor(mensaje);
            
            // La respuesta se procesará en escucharServidor()
            return true;
            
        } catch (Exception e) {
            System.err.println("Error registrando jugador: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Marca el jugador como listo.
     */
    public void marcarListo() {
        if (!conectado) return;
        
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "listo");
        enviarAlServidor(mensaje);
    }
    
    // ============================
    // CONEXIONES P2P
    // ============================
    
    /**
     * Inicia el servidor P2P para recibir conexiones de otros peers.
     */
    public boolean iniciarServidorP2P(int puerto) {
        try {
            serverSocketP2P = new ServerSocket(puerto);
            this.puertoP2P = puerto;
            
            // Thread para aceptar conexiones P2P entrantes
            threadEscuchaPeers = new Thread(() -> {
                while (conectado) {
                    try {
                        Socket socketPeer = serverSocketP2P.accept();
                        System.out.println(">>> Nueva conexion P2P desde: " + 
                            socketPeer.getInetAddress().getHostAddress());
                        
                        // Crear conexión peer (se identificará después)
                        ConexionPeer conexion = new ConexionPeer(socketPeer, this);
                        conexion.iniciar();
                        
                    } catch (IOException e) {
                        if (conectado) {
                            System.err.println("Error aceptando conexion P2P: " + e.getMessage());
                        }
                    }
                }
            });
            threadEscuchaPeers.start();
            
            System.out.println(">>> Servidor P2P iniciado en puerto: " + puerto);
            return true;
            
        } catch (IOException e) {
            System.err.println("Error iniciando servidor P2P: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Conecta a otro peer directamente.
     */
    public boolean conectarAPeer(int peerJugadorId, String host, int puerto) {
        try {
            Socket socketPeer = new Socket(host, puerto);
            ConexionPeer conexion = new ConexionPeer(socketPeer, this);
            conexion.setPeerJugadorId(peerJugadorId);
            conexion.iniciar();
            
            conexionesPeers.put(peerJugadorId, conexion);
            
            System.out.println(">>> Conectado a peer: " + peerJugadorId + " (" + host + ":" + puerto + ")");
            
            // Enviar identificación
            JsonObject identificacion = new JsonObject();
            identificacion.addProperty("tipo", "IDENTIFICACION_PEER");
            identificacion.addProperty("jugadorId", jugadorId);
            conexion.enviarMensaje(identificacion);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Error conectando a peer: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registra una conexión peer entrante.
     */
    public void registrarConexionPeer(int peerJugadorId, ConexionPeer conexion) {
        conexionesPeers.put(peerJugadorId, conexion);
        System.out.println(">>> Peer registrado: " + peerJugadorId);
    }
    
    // ============================
    // ACCIONES DE JUEGO
    // ============================
    
    /**
     * Tira los dados.
     * 
     * Flujo híbrido:
     * 1. Notifica a peers (rápido)
     * 2. Envía al servidor (validación)
     */
    public void tirarDados() {
        if (!enPartida) {
            System.err.println("No estas en una partida");
            return;
        }
        
        // 1. Notificar a peers inmediatamente (optimista)
        JsonObject notificacionPeer = new JsonObject();
        notificacionPeer.addProperty("tipo", "NOTIFICACION_TIRADA");
        notificacionPeer.addProperty("jugadorId", jugadorId);
        notificacionPeer.addProperty("mensaje", "Tirando dados...");
        notificacionPeer.addProperty("timestamp", System.currentTimeMillis());
        
        broadcastAPeers(notificacionPeer);
        
        // 2. Enviar al servidor para validación oficial
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "tirar_dados");
        
        enviarAlServidor(mensajeServidor);
        
        System.out.println(">>> Tirando dados... (esperando servidor)");
    }
    
    /**
     * Mueve una ficha.
     */
    public void moverFicha(int fichaId, int dado1, int dado2) {
        if (!enPartida) {
            System.err.println("No estas en una partida");
            return;
        }
        
        // 1. Notificar a peers (optimista)
        JsonObject notificacionPeer = new JsonObject();
        notificacionPeer.addProperty("tipo", "NOTIFICACION_MOVIMIENTO");
        notificacionPeer.addProperty("jugadorId", jugadorId);
        notificacionPeer.addProperty("fichaId", fichaId);
        notificacionPeer.addProperty("mensaje", "Moviendo ficha...");
        
        broadcastAPeers(notificacionPeer);
        
        // 2. Enviar al servidor para validación
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "mover_ficha");
        mensajeServidor.addProperty("fichaId", fichaId);
        mensajeServidor.addProperty("dado1", dado1);
        mensajeServidor.addProperty("dado2", dado2);
        
        enviarAlServidor(mensajeServidor);
        
        System.out.println(">>> Moviendo ficha " + fichaId + "... (esperando servidor)");
    }
    
    /**
     * Saca una ficha de casa.
     */
    public void sacarFicha(int fichaId, int dado1, int dado2) {
        if (!enPartida) return;
        
        // Notificación P2P
        JsonObject notificacionPeer = new JsonObject();
        notificacionPeer.addProperty("tipo", "NOTIFICACION_SACAR");
        notificacionPeer.addProperty("jugadorId", jugadorId);
        notificacionPeer.addProperty("fichaId", fichaId);
        
        broadcastAPeers(notificacionPeer);
        
        // Envío al servidor
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "sacar_ficha");
        mensajeServidor.addProperty("fichaId", fichaId);
        mensajeServidor.addProperty("dado1", dado1);
        mensajeServidor.addProperty("dado2", dado2);
        
        enviarAlServidor(mensajeServidor);
    }
    
    /**
     * Salta el turno.
     */
    public void saltarTurno() {
        if (!enPartida) return;
        
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "saltar_turno");
        
        enviarAlServidor(mensaje);
    }
    
    // ============================
    // COMUNICACIÓN
    // ============================
    
    /**
     * Envía un mensaje al servidor.
     */
    private synchronized void enviarAlServidor(JsonObject mensaje) {
        if (!conectado || salidaServidor == null) {
            System.err.println("No conectado al servidor");
            return;
        }
        
        try {
            salidaServidor.println(mensaje.toString());
            salidaServidor.flush();
        } catch (Exception e) {
            System.err.println("Error enviando al servidor: " + e.getMessage());
        }
    }
    
    /**
     * Hace broadcast de un mensaje a todos los peers.
     */
    private void broadcastAPeers(JsonObject mensaje) {
        for (ConexionPeer conexion : conexionesPeers.values()) {
            if (conexion.isConectado()) {
                conexion.enviarMensaje(mensaje);
            }
        }
    }
    
    /**
     * Escucha mensajes del servidor (thread).
     */
    private void escucharServidor() {
        try {
            String linea;
            while (conectado && (linea = entradaServidor.readLine()) != null) {
                procesarMensajeServidor(linea);
            }
        } catch (IOException e) {
            if (conectado) {
                System.err.println("Error escuchando servidor: " + e.getMessage());
            }
        }
    }
    
    /**
     * Procesa un mensaje recibido del servidor.
     */
    private void procesarMensajeServidor(String mensajeJson) {
        try {
            JsonObject mensaje = JsonParser.parseString(mensajeJson).getAsJsonObject();
            String tipo = mensaje.get("tipo").getAsString();
            
            switch (tipo) {
                case "bienvenida":
                    manejarBienvenida(mensaje);
                    break;
                    
                case "JUGADOR_REGISTRADO":
                    manejarRegistroExitoso(mensaje);
                    break;
                    
                case "NUEVO_JUGADOR":
                    manejarNuevoJugador(mensaje);
                    break;
                    
                case "JUGADOR_LISTO":
                    manejarJugadorListo(mensaje);
                    break;
                    
                case "PARTIDA_INICIADA":
                    manejarInicioPartida(mensaje);
                    break;
                    
                case "DADOS_TIRADOS":
                    manejarDadosTirados(mensaje);
                    break;
                    
                case "FICHA_MOVIDA":
                    manejarFichaMovida(mensaje);
                    break;
                    
                case "FICHA_SACADA":
                    manejarFichaSacada(mensaje);
                    break;
                    
                case "TURNO_SALTADO":
                    manejarTurnoSaltado(mensaje);
                    break;
                    
                case "error":
                    manejarError(mensaje);
                    break;
                    
                default:
                    System.out.println("[SERVIDOR] " + tipo + ": " + mensaje);
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje servidor: " + e.getMessage());
        }
    }
    
    /**
     * Procesa un mensaje recibido de un peer.
     */
    public void procesarMensajePeer(JsonObject mensaje, int peerJugadorId) {
        try {
            String tipo = mensaje.get("tipo").getAsString();
            
            switch (tipo) {
                case "IDENTIFICACION_PEER":
                    // Ya manejado en ConexionPeer
                    break;
                    
                case "NOTIFICACION_TIRADA":
                case "NOTIFICACION_MOVIMIENTO":
                case "NOTIFICACION_SACAR":
                    // Mostrar notificación rápida al usuario
                    String jugadorIdStr = mensaje.has("jugadorId") ? 
                        mensaje.get("jugadorId").getAsString() : "?";
                    String mensajeTexto = mensaje.has("mensaje") ? 
                        mensaje.get("mensaje").getAsString() : tipo;
                    System.out.println("[PEER " + jugadorIdStr + "] " + mensajeTexto);
                    break;
                    
                default:
                    System.out.println("[PEER] " + tipo + ": " + mensaje);
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje peer: " + e.getMessage());
        }
    }
    
    // ============================
    // HANDLERS DE MENSAJES DEL SERVIDOR
    // ============================
    
    private void manejarBienvenida(JsonObject mensaje) {
        if (mensaje.has("sessionId")) {
            this.sessionId = mensaje.get("sessionId").getAsString();
            System.out.println(">>> Session ID: " + sessionId);
        }
    }
    
    private void manejarRegistroExitoso(JsonObject mensaje) {
        this.jugadorId = mensaje.get("jugadorId").getAsInt();
        String nombre = mensaje.get("nombre").getAsString();
        String color = mensaje.get("color").getAsString();
        
        System.out.println("\n================================================");
        System.out.println("   REGISTRO EXITOSO");
        System.out.println("================================================");
        System.out.println("Jugador ID: " + jugadorId);
        System.out.println("Nombre: " + nombre);
        System.out.println("Color: " + color);
        System.out.println("================================================\n");
        
        // Actualizar cache con jugador local
        if (cache != null) {
            cache.agregarJugador(jugadorId, nombre, color);
            cache.actualizarJugador(jugadorId, nombre, color, 0, false);
        }
        
        // Obtener información de otros jugadores para conectar P2P
        if (mensaje.has("jugadores")) {
            JsonArray jugadores = mensaje.getAsJsonArray("jugadores");
            System.out.println("Jugadores en la sala:");
            for (int i = 0; i < jugadores.size(); i++) {
                JsonObject jug = jugadores.get(i).getAsJsonObject();
                int id = jug.get("id").getAsInt();
                String nom = jug.get("nombre").getAsString();
                String col = jug.get("color").getAsString();
                System.out.println("  - " + nom + " (ID: " + id + ")");
                
                // Actualizar cache con todos los jugadores
                if (cache != null && id != jugadorId) {
                    cache.agregarJugador(id, nom, col);
                    cache.actualizarJugador(id, nom, col, 0, false);
                }
            }
        }
    }
    
    private void manejarNuevoJugador(JsonObject mensaje) {
        int nuevoId = mensaje.get("jugadorId").getAsInt();
        String nombre = mensaje.get("nombre").getAsString();
        String color = mensaje.get("color").getAsString();
        
        System.out.println("\n>>> Nuevo jugador se unio: " + nombre + " (ID: " + nuevoId + ")");
        
        // Actualizar cache
        if (cache != null) {
            cache.agregarJugador(nuevoId, nombre, color);
            cache.actualizarJugador(nuevoId, nombre, color, 0, false);
        }
        
        // Aquí podrías iniciar conexión P2P con el nuevo jugador
        // (necesitarías su IP y puerto P2P)
    }
    
    private void manejarJugadorListo(JsonObject mensaje) {
        int jugadorIdMsg = mensaje.get("jugadorId").getAsInt();
        String nombre = mensaje.has("nombre") ? mensaje.get("nombre").getAsString() : "Jugador " + jugadorIdMsg;
        
        System.out.println("\n>>> " + nombre + " esta listo!");
        
        // Actualizar cache - buscar jugador existente para mantener su info
        if (cache != null) {
            CachePartida.JugadorCache jugador = cache.getJugador(jugadorIdMsg);
            if (jugador != null) {
                // Actualizar solo el estado "listo"
                cache.actualizarJugador(jugadorIdMsg, jugador.nombre, jugador.color, jugador.puntos, true);
            } else {
                // Si no existe, crear con info básica
                cache.actualizarJugador(jugadorIdMsg, nombre, null, 0, true);
            }
        }
    }
    
    private void manejarInicioPartida(JsonObject mensaje) {
        enPartida = true;
        System.out.println("\n================================================");
        System.out.println("   LA PARTIDA HA COMENZADO!");
        System.out.println("================================================");
        
        // Actualizar estado en cache
        if (cache != null) {
            cache.actualizarEstado(modelo.partida.EstadoPartida.EN_PROGRESO);
        }
        
        if (mensaje.has("primerTurno")) {
            int primerTurno = mensaje.get("primerTurno").getAsInt();
            String nombrePrimero = mensaje.has("primerJugadorNombre") ? 
                mensaje.get("primerJugadorNombre").getAsString() : "?";
            
            // Actualizar turno en cache
            if (cache != null) {
                cache.actualizarTurno(primerTurno, nombrePrimero);
            }
            
            if (primerTurno == jugadorId) {
                System.out.println("** ES TU TURNO **");
            } else {
                System.out.println("Turno de: " + nombrePrimero);
            }
        }
        System.out.println("================================================\n");
    }
    
    private void manejarDadosTirados(JsonObject mensaje) {
        int jugadorIdMsg = mensaje.get("jugadorId").getAsInt();
        JsonArray dados = mensaje.getAsJsonArray("dados");
        int dado1 = dados.get(0).getAsInt();
        int dado2 = dados.get(1).getAsInt();
        
        boolean esDoble = mensaje.has("esDoble") && mensaje.get("esDoble").getAsBoolean();
        int contadorDobles = mensaje.has("contadorDobles") ? 
            mensaje.get("contadorDobles").getAsInt() : 0;
        
        if (jugadorIdMsg == this.jugadorId) {
            System.out.println("\n>>> Tiraste: [" + dado1 + "] [" + dado2 + "]");
            if (esDoble) {
                System.out.println("*** DOBLE *** - Puedes tirar de nuevo!");
            }
        } else {
            System.out.println("\n[SERVIDOR] Jugador " + jugadorIdMsg + " tiro: [" + dado1 + "] [" + dado2 + "]");
        }
        
        // Actualizar cache
        if (cache != null) {
            cache.actualizarDados(dado1, dado2, esDoble, contadorDobles);
        }
        
        // Sincronizar con estado local
        sincronizador.actualizarDados(dado1, dado2);
    }
    
    private void manejarFichaMovida(JsonObject mensaje) {
        int jugadorIdMsg = mensaje.get("jugadorId").getAsInt();
        int fichaId = mensaje.get("fichaId").getAsInt();
        int origen = mensaje.get("posicionOrigen").getAsInt();
        int destino = mensaje.get("posicionDestino").getAsInt();
        
        System.out.println("\n>>> Ficha " + fichaId + " movida: " + origen + " -> " + destino);
        
        if (mensaje.has("huboCaptura") && mensaje.get("huboCaptura").getAsBoolean()) {
            int bonus = mensaje.get("bonusCaptura").getAsInt();
            System.out.println("*** CAPTURA *** +" + bonus + " casillas de bonus!");
        }
        
        if (mensaje.has("llegoAMeta") && mensaje.get("llegoAMeta").getAsBoolean()) {
            System.out.println("*** LLEGO A META ***");
        }
    }
    
    private void manejarFichaSacada(JsonObject mensaje) {
        int fichaId = mensaje.get("fichaId").getAsInt();
        System.out.println("\n>>> Ficha " + fichaId + " sacada de casa!");
        
        if (mensaje.has("dadoDisponible")) {
            int dadoDisp = mensaje.get("dadoDisponible").getAsInt();
            System.out.println("Tienes un dado disponible: " + dadoDisp);
        }
    }
    
    private void manejarTurnoSaltado(JsonObject mensaje) {
        System.out.println("\n>>> Turno saltado");
    }
    
    private void manejarError(JsonObject mensaje) {
        String error = mensaje.get("mensaje").getAsString();
        System.err.println("\n[ERROR] " + error);
    }
    
    // ============================
    // DESCONEXIÓN
    // ============================
    
    /**
     * Desconecta del servidor y de todos los peers.
     */
    public void desconectar() {
        conectado = false;
        enPartida = false;
        
        // Cerrar conexiones peer
        for (ConexionPeer conexion : conexionesPeers.values()) {
            conexion.desconectar();
        }
        conexionesPeers.clear();
        
        // Cerrar servidor P2P
        try {
            if (serverSocketP2P != null) {
                serverSocketP2P.close();
            }
        } catch (IOException e) {
            // Ignorar
        }
        
        // Cerrar conexión al servidor
        try {
            if (salidaServidor != null) salidaServidor.close();
            if (entradaServidor != null) entradaServidor.close();
            if (socketServidor != null) socketServidor.close();
        } catch (IOException e) {
            // Ignorar
        }
        
        System.out.println(">>> Desconectado");
    }
    
    // ============================
    // GETTERS
    // ============================
    
    public boolean isConectado() {
        return conectado;
    }
    
    public boolean isEnPartida() {
        return enPartida;
    }
    
    public int getJugadorId() {
        return jugadorId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public int getPuertoP2P() {
        return puertoP2P;
    }
}