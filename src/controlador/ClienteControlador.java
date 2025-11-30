package controlador;

import vista.VistaCliente;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import controlador.peer.ClientePeer;
import java.io.*;
import java.net.Socket;

/**
 * ✅ ACTUALIZADO: Comunicación P2P real
 * - Movimientos van directamente a peers (10ms)
 * - Servidor valida en paralelo (100ms)
 */
public class ClienteControlador {
    
    private final VistaCliente vista;
    private vista.PantallaCarga vistaCarga;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Thread hiloEscucha;
    private boolean conectado;
    private int jugadorId;
    private String nombreJugador; // ✅ NUEVO
    private int partidaActualId;
    private boolean esmiTurno;
    private int ultimoResultadoDados;
    private int[] ultimosDados = new int[2];
    private boolean debeVolverATirar = false;
    private int dadoDisponible = 0;
    private boolean tieneFichasEnJuego = false;
    
    private JsonObject ultimoEstadoTablero = null;
    
    private ClientePeer clientePeer;
    private int miPuertoPeer;
    
    public ClienteControlador(VistaCliente vista) {
        this.vista = vista;
        this.conectado = false;
        this.jugadorId = -1;
        this.nombreJugador = "";
        this.partidaActualId = -1;
        this.esmiTurno = false;
        this.ultimoResultadoDados = 0;
        this.clientePeer = null;
        this.miPuertoPeer = -1;
    }
    
    /**
    * ✅ NUEVO: Conecta la vista de carga para recibir actualizaciones
    */
    public void setVistaCarga(vista.PantallaCarga vistaCarga) {
       this.vistaCarga = vistaCarga;
    }
    
    public boolean conectar(String ip, int puerto) {
        try {
            miPuertoPeer = asignarPuertoP2PAutomatico();
            System.out.println("  Puerto P2P asignado automaticamente: " + miPuertoPeer);
            
            System.out.println("[DEBUG] Iniciando cliente peer...");
            clientePeer = new ClientePeer(miPuertoPeer);
            clientePeer.setVista(vista); // ✅ NUEVO: Conectar vista
            
            System.out.println("[DEBUG] Conectando al servidor " + ip + ":" + puerto);
            socket = new Socket(ip, puerto);
            entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8")
            );
            salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                true
            );
            
            conectado = true;
            System.out.println(">>> Conectado al servidor: " + ip + ":" + puerto);
            
            iniciarHiloEscucha();
            
            System.out.println("[DEBUG] Iniciando servidor P2P en puerto " + miPuertoPeer);
            if (!clientePeer.iniciarServidorPeer()) {
                System.err.println("[ERROR] No se pudo iniciar servidor P2P");
                return false;
            }
            
            System.out.println("  Servidor P2P iniciado en puerto " + miPuertoPeer);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Error conectando: " + e.getMessage());
            return false;
        }
    }
    
    private int asignarPuertoP2PAutomatico() {
        int puertoBase = 6000;
        int intentos = 0;
        int maxIntentos = 100;
        
        while (intentos < maxIntentos) {
            int puerto = puertoBase + (int)(Math.random() * 1000);
            
            try (java.net.ServerSocket test = new java.net.ServerSocket(puerto)) {
                return puerto;
            } catch (IOException e) {
                intentos++;
            }
        }
        
        return puertoBase + (int)(Math.random() * 1000);
    }
    
    private void iniciarHiloEscucha() {
        hiloEscucha = new Thread(() -> {
            try {
                String mensaje;
                while (conectado && (mensaje = entrada.readLine()) != null) {
                    procesarMensajeServidor(mensaje);
                }
            } catch (IOException e) {
                if (conectado) {
                    System.err.println("Error en comunicacion: " + e.getMessage());
                }
            }
        });
        
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }
    
    public void desconectar() {
        conectado = false;
        
        if (clientePeer != null) {
            clientePeer.cerrar();
        }
        
        try {
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            if (socket != null) socket.close();
        } catch (IOException e) { }
    }
    
    private boolean enviarMensaje(JsonObject mensaje) {
        if (!conectado || salida == null) {
            return false;
        }
        
        try {
            salida.println(mensaje.toString());
            return !salida.checkError();
        } catch (Exception e) {
            return false;
        }
    }

    private void procesarMensajeServidor(String mensajeJson) {
        try {
            JsonObject json = JsonParser.parseString(mensajeJson).getAsJsonObject();
            String tipo = json.has("tipo") ? json.get("tipo").getAsString() : "";

            switch (tipo) {
                case "bienvenida":
                    if (json.has("sessionId")) {
                        System.out.println(">>> Session ID: " + json.get("sessionId").getAsString());
                    }
                    break;

                case "registro_exitoso":
                    if (json.has("jugador")) {
                        JsonObject jugador = json.getAsJsonObject("jugador");
                        jugadorId = jugador.get("id").getAsInt();
                        nombreJugador = jugador.get("nombre").getAsString();

                        notificarPuertoP2PAlServidor();
                    }
                    break;

                case "info_peers":
                    conectarAPeers(json.getAsJsonArray("peers"));
                    break;

                case "sala_creada":
                case "union_exitosa":
                    if (json.has("partida")) {
                        JsonObject partida = json.getAsJsonObject("partida");
                        partidaActualId = partida.get("id").getAsInt();
                    }
                    break;

                case "jugador_unido":
                    String nombre = json.get("nombre").getAsString();
                    int total = json.get("totalJugadores").getAsInt();
                    System.out.println("\n[INFO] " + nombre + " se unio a la partida (" + total + " jugadores)");
                    break;

                case "jugador_listo":
                    String nombreListo = json.get("nombre").getAsString();
                    System.out.println("[INFO] " + nombreListo + " esta listo");
                    break;

                case "partida_iniciada":
                    if (json.has("turnoJugadorId")) {
                        int turnoId = json.get("turnoJugadorId").getAsInt();
                        esmiTurno = (turnoId == jugadorId);
                    }
                    
                    // ✅ Notificar a PantallaCarga que inicie el juego
                    if (vistaCarga != null) {
                        vistaCarga.iniciarPartida();
                    }


                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            vista.iniciarJuego();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    break;

                case "tu_turno":
                    esmiTurno = true;
                    System.out.println("[DEBUG] Recibido 'tu_turno' - Activando turno");
                    vista.notificarTurno();
                    break;

                case "cambio_turno":
                    esmiTurno = false;
                    String nombreTurno = json.get("jugadorNombre").getAsString();
                    int jugadorTurnoId = json.get("jugadorId").getAsInt();

                    if (jugadorTurnoId == jugadorId) {
                        System.out.println("[DEBUG] Cambio de turno dice que es MI turno");
                        esmiTurno = true;
                    }

                    vista.mostrarCambioTurno(nombreTurno);
                    break;

                case "estado_tablero":
                    JsonObject tableroJson = json.getAsJsonObject("tablero");
                    ultimoEstadoTablero = tableroJson;
                    vista.mostrarEstadoTablero(tableroJson);
                    break;

                case "resultado_dados":
                    JsonObject dados = json.getAsJsonObject("dados");
                    int dado1 = dados.get("dado1").getAsInt();
                    int dado2 = dados.get("dado2").getAsInt();
                    boolean esDoble = dados.get("esDoble").getAsBoolean();

                    ultimosDados[0] = dado1;
                    ultimosDados[1] = dado2;
                    ultimoResultadoDados = dado1 + dado2;

                    if (json.has("debeVolverATirar")) {
                        debeVolverATirar = json.get("debeVolverATirar").getAsBoolean();
                    } else {
                        debeVolverATirar = false;
                    }

                    if (json.has("dadoDisponible")) {
                        dadoDisponible = json.get("dadoDisponible").getAsInt();
                        System.out.println("[DEBUG] Dado disponible recibido: " + dadoDisponible);
                    } else {
                        dadoDisponible = 0;
                    }

                    if (json.has("tieneFichasEnJuego")) {
                        tieneFichasEnJuego = json.get("tieneFichasEnJuego").getAsBoolean();
                        System.out.println("[DEBUG] Tiene fichas en juego: " + tieneFichasEnJuego);
                    } else {
                        tieneFichasEnJuego = false;
                    }

                    vista.mostrarResultadoDados(dado1, dado2, esDoble);
                    break;

                case "jugador_tiro_dados":
                    // Los peers ya recibieron el mensaje P2P antes
                    break;

                case "movimiento_exitoso":
                    if (json.has("turnoTerminado") && json.get("turnoTerminado").getAsBoolean()) {
                        esmiTurno = false;
                    }
                    break;

                case "ficha_movida":
                    String nombreMov = json.get("jugadorNombre").getAsString();
                    int fichaId = json.get("fichaId").getAsInt();
                    int desde = json.get("desde").getAsInt();
                    int hasta = json.get("hasta").getAsInt();
                    boolean automatico = json.has("automatico") && json.get("automatico").getAsBoolean();

                    if (json.has("dadoDisponible")) {
                        dadoDisponible = json.get("dadoDisponible").getAsInt();
                        System.out.println("[DEBUG] Dado disponible después de sacar: " + dadoDisponible);
                    }

                    if (automatico) {
                        vista.mostrarMovimientoAutomatico(nombreMov, fichaId, desde, hasta);
                    }
                    break;

                case "ficha_capturada":
                    // Confirmación del servidor (ya se mostró por P2P)
                    break;

                case "ficha_en_meta":
                    // Confirmación del servidor (ya se mostró por P2P)
                    break;

                // ✅ NUEVO: Manejo de bonus de captura
                case "bonus_disponible":
                case "bonus_disponibles":
                    if (json.has("mensaje")) {
                        System.out.println("\n[BONUS] " + json.get("mensaje").getAsString());
                    }
                    break;

                case "partida_ganada":
                    String ganador = json.get("ganadorNombre").getAsString();
                    vista.mostrarGanador(ganador);
                    break;

                case "penalizacion_tres_dobles":
                    String jugadorPenalizado = json.get("jugadorNombre").getAsString();
                    String mensajePenalizacion = json.get("mensaje").getAsString();
                    vista.mostrarPenalizacionTresDobles(jugadorPenalizado, mensajePenalizacion);
                    break;

                case "error":
                    String mensaje = json.get("mensaje").getAsString();
                    System.err.println("\n[ERROR] " + mensaje);
                    break;

                case "ping":
                    responderPong();
                    break;

                case "listo_confirmado":
                case "exito":
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
        }
    }
    
    private void notificarPuertoP2PAlServidor() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "registrar_puerto_peer");
        mensaje.addProperty("puertoP2P", miPuertoPeer);
        enviarMensaje(mensaje);
    }
    
    private void conectarAPeers(JsonArray peers) {
        if (clientePeer == null) return;
        
        for (int i = 0; i < peers.size(); i++) {
            JsonObject peer = peers.get(i).getAsJsonObject();
            int peerId = peer.get("id").getAsInt();
            String peerIp = peer.get("ip").getAsString();
            int peerPort = peer.get("puerto").getAsInt();
            
            if (peerId != jugadorId) {
                clientePeer.conectarAPeer(peerId, peerIp, peerPort);
            }
        }
        
        System.out.println("[P2P] Conectado a " + clientePeer.getNumeroPeersConectados() + " peers");
    }
    
    private void responderPong() {
        JsonObject pong = new JsonObject();
        pong.addProperty("tipo", "pong");
        enviarMensaje(pong);
    }
    
    // ========================================
    // MÉTODOS DE JUEGO (ACTUALIZADOS CON P2P)
    // ========================================
    
    public boolean registrar(String nombre) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "registrar");
        mensaje.addProperty("nombre", nombre);
        return enviarMensaje(mensaje);
    }
    
    public boolean crearPartida(String nombrePartida, int maxJugadores) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "crear_sala");
        mensaje.addProperty("nombre", nombrePartida);
        mensaje.addProperty("maxJugadores", maxJugadores);
        return enviarMensaje(mensaje);
    }
    
    public boolean unirseAPartida(int partidaId) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "unirse");
        mensaje.addProperty("partidaId", partidaId);
        return enviarMensaje(mensaje);
    }
    
    public boolean unirseAPartidaDisponible() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "unirse");
        return enviarMensaje(mensaje);
    }
    
    public void listarPartidas() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "listar_salas");
        enviarMensaje(mensaje);
    }
    
    public boolean marcarListo() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "listo");
        return enviarMensaje(mensaje);
    }
    
    /**
     * ✅ ACTUALIZADO: Tirar dados con comunicación P2P
     */
    public boolean tirarDados() {
        // 1. Enviar al servidor (validación)
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "tirar_dado");
        enviarMensaje(mensajeServidor);
        
        return true;
    }
    
    /**
     * ✅ NUEVO: Notifica a peers que tiraste dados (después de que el servidor confirme)
     */
    public void notificarDadosAPeers(int dado1, int dado2) {
        if (clientePeer == null) return;
        
        JsonObject mensajeP2P = new JsonObject();
        mensajeP2P.addProperty("tipo", "tirada_dados_peer");
        mensajeP2P.addProperty("jugadorNombre", nombreJugador);
        mensajeP2P.addProperty("dado1", dado1);
        mensajeP2P.addProperty("dado2", dado2);
        
        System.out.println("[P2P OUT] Notificando tirada de dados a peers");
        clientePeer.broadcastAPeers(mensajeP2P.toString());
    }
    
    /**
     * ✅ ACTUALIZADO: Mover ficha con comunicación P2P
     */
    public boolean moverFicha(int fichaId, int dado1, int dado2) {
        // 1. Enviar a PEERS primero (optimista - 10ms)
        enviarMovimientoAPeers(fichaId, -1, -1); // desde/hasta se actualizan después
        
        // 2. Enviar al servidor (validación - 100ms)
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "mover_ficha");
        mensajeServidor.addProperty("fichaId", fichaId);
        mensajeServidor.addProperty("dado1", dado1);
        mensajeServidor.addProperty("dado2", dado2);
        
        return enviarMensaje(mensajeServidor);
    }
    
    /**
     * ✅ ACTUALIZADO: Mover con un dado con comunicación P2P
     */
    public boolean moverFichaConUnDado(int fichaId, int valorDado, boolean pasarTurno) {
        // 1. Enviar a PEERS primero (optimista - 10ms)
        enviarMovimientoAPeers(fichaId, -1, -1);
        
        // 2. Enviar al servidor (validación - 100ms)
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "mover_ficha_un_dado");
        mensajeServidor.addProperty("fichaId", fichaId);
        mensajeServidor.addProperty("valorDado", valorDado);
        mensajeServidor.addProperty("pasarTurno", pasarTurno);
        
        return enviarMensaje(mensajeServidor);
    }
    
    /**
     * ✅ NUEVO: Envía movimiento a todos los peers
     */
    private void enviarMovimientoAPeers(int fichaId, int desde, int hasta) {
        if (clientePeer == null) return;
        
        JsonObject mensajeP2P = new JsonObject();
        mensajeP2P.addProperty("tipo", "movimiento_peer");
        mensajeP2P.addProperty("jugadorNombre", nombreJugador);
        mensajeP2P.addProperty("fichaId", fichaId);
        mensajeP2P.addProperty("desde", desde);
        mensajeP2P.addProperty("hasta", hasta);
        
        System.out.println("[P2P OUT] Enviando movimiento a peers");
        clientePeer.broadcastAPeers(mensajeP2P.toString());
    }
    
    /**
     * ✅ NUEVO: Notifica captura a peers
     */
    public void notificarCapturaAPeers() {
        if (clientePeer == null) return;
        
        JsonObject mensajeP2P = new JsonObject();
        mensajeP2P.addProperty("tipo", "captura_peer");
        mensajeP2P.addProperty("capturadorNombre", nombreJugador);
        
        System.out.println("[P2P OUT] Notificando captura a peers");
        clientePeer.broadcastAPeers(mensajeP2P.toString());
    }
    
    /**
     * ✅ NUEVO: Notifica llegada a meta a peers
     */
    public void notificarMetaAPeers() {
        if (clientePeer == null) return;
        
        JsonObject mensajeP2P = new JsonObject();
        mensajeP2P.addProperty("tipo", "meta_peer");
        mensajeP2P.addProperty("jugadorNombre", nombreJugador);
        
        System.out.println("[P2P OUT] Notificando meta a peers");
        clientePeer.broadcastAPeers(mensajeP2P.toString());
    }
    
    public boolean saltarTurno() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "saltar_turno");
        return enviarMensaje(mensaje);
    }
    
    public boolean salirDePartida() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "salir_sala");
        return enviarMensaje(mensaje);
    }
    
    public void mostrarEstadoPartida() {
        vista.mostrarEstadoCompleto(jugadorId, ultimoEstadoTablero);
    }
    
    public boolean esmiTurno() {
        return esmiTurno;
    }
    
    public int getUltimoResultadoDados() {
        return ultimoResultadoDados;
    }
    
    public int[] getUltimosDados() {
        return ultimosDados;
    }
    
    public boolean debeVolverATirar() {
        return debeVolverATirar;
    }
    
    public void limpiarDebeVolverATirar() {
        debeVolverATirar = false;
    }
    
    public boolean tieneFichasEnJuego() {
        return tieneFichasEnJuego;
    }
    
    public int getDadoDisponible() {
        return dadoDisponible;
    }
    
    public void limpiarDadoDisponible() {
        dadoDisponible = 0;
    }
    
    public void mostrarJugadoresEnSala() {
        System.out.println("  Jugadores en sala: (esperando info del servidor)");
    }
    
    public void marcarTurnoTerminado() {
        esmiTurno = false;
    }
}