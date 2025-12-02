package controlador;

import vista.VistaCliente;
import vista.TableroVista;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import controlador.peer.ClientePeer;
import java.io.*;
import java.net.Socket;

/**
 * ✅ ACTUALIZADO: Comunicación P2P real + Conexión con TableroVista
 * - Movimientos van directamente a peers (10ms)
 * - Servidor valida en paralelo (100ms)
 * - Actualiza TableroVista en tiempo real
 */
public class ClienteControlador {
    
    private final VistaCliente vista;
    private vista.PantallaCarga vistaCarga;
    private TableroVista tableroVista;
    
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Thread hiloEscucha;
    private boolean conectado;
    private int jugadorId;
    private String nombreJugador;
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
    
    // ✅ Cache de nombres [rojo, azul, verde, amarillo]
    private String[] nombresGuardados = null;
    
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
        this.tableroVista = null;
        this.nombresGuardados = null;
    }
    
    /**
     * ✅ Conecta la vista de carga para recibir actualizaciones
     */
    public void setVistaCarga(vista.PantallaCarga vistaCarga) {
        this.vistaCarga = vistaCarga;
    }
    
    /**
     * ✅ Asigna la vista del tablero para actualizar la UI
     */
    public void setTableroVista(TableroVista tableroVista) {
        this.tableroVista = tableroVista;
        System.out.println("[ClienteControlador] TableroVista conectado");
    }
    
    /**
     * ✅ NUEVO: Obtiene los nombres guardados
     * @return Array [rojo, azul, verde, amarillo]
     */
    public String[] getNombresGuardados() {
        return nombresGuardados;
    }
    
    public boolean conectar(String ip, int puerto) {
        try {
            miPuertoPeer = asignarPuertoP2PAutomatico();
            System.out.println("  Puerto P2P asignado automaticamente: " + miPuertoPeer);
            
            System.out.println("[DEBUG] Iniciando cliente peer...");
            clientePeer = new ClientePeer(miPuertoPeer);
            clientePeer.setVista(vista);
            
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

                    // ✅ PASO 1: Extraer y GUARDAR nombres PRIMERO
                    if (json.has("jugadores")) {
                        JsonArray jugadores = json.getAsJsonArray("jugadores");

                        // Inicializar array con valores por defecto
                        String[] nombres = new String[] { 
                            "Jugador Rojo", 
                            "Jugador Azul", 
                            "Jugador Verde", 
                            "Jugador Amarillo" 
                        };

                        // Actualizar solo los jugadores que existen
                        for (int i = 0; i < jugadores.size(); i++) {
                            JsonObject jugador = jugadores.get(i).getAsJsonObject();
                            String color = jugador.get("color").getAsString();
                            String nombreJug = jugador.get("nombre").getAsString();

                            switch (color) {
                                case "ROJO": nombres[0] = nombreJug; break;
                                case "AZUL": nombres[1] = nombreJug; break;
                                case "VERDE": nombres[2] = nombreJug; break;
                                case "AMARILLO": nombres[3] = nombreJug; break;
                            }

                            System.out.println("[DEBUG] Jugador: " + nombreJug + " -> " + color);
                        }

                        // ✅ GUARDAR nombres
                        nombresGuardados = nombres;
                        System.out.println("[ClienteControlador] Nombres guardados: " + jugadores.size() + " jugadores");
                        System.out.println("  Rojo=" + nombres[0] + " Azul=" + nombres[1] + " Verde=" + nombres[2] + " Amarillo=" + nombres[3]);
                    }

                    // ✅ PASO 2: DESPUÉS de guardar nombres, notificar a PantallaCarga
                    if (vistaCarga != null) {
                        System.out.println("[ClienteControlador] Nombres listos, iniciando partida en GUI");
                        vistaCarga.iniciarPartida();
                    }
                    break;
                    
                case "tu_turno":
                    esmiTurno = true;
                    System.out.println("[DEBUG] Recibido 'tu_turno' - Activando turno");
                    if (vista != null) {
                        vista.notificarTurno();
                    }
                    break;

                case "cambio_turno":
                    esmiTurno = false;
                    String nombreTurno = json.get("jugadorNombre").getAsString();
                    int jugadorTurnoId = json.get("jugadorId").getAsInt();

                    if (jugadorTurnoId == jugadorId) {
                        System.out.println("[DEBUG] Cambio de turno dice que es MI turno");
                        esmiTurno = true;
                    }

                    if (vista != null) {
                        vista.mostrarCambioTurno(nombreTurno);
                    }
                    break;

                case "estado_tablero":
                    JsonObject tableroJson = json.getAsJsonObject("tablero");
                    ultimoEstadoTablero = tableroJson;

                    // ✅ Actualizar TableroVista
                    if (tableroVista != null) {
                        tableroVista.actualizarFichas(tableroJson);
                    }

                    if (vista != null) {
                        vista.mostrarEstadoTablero(tableroJson);
                    }
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

                    // ✅ Actualizar TableroVista con los dados
                    if (tableroVista != null) {
                        tableroVista.mostrarResultadoDados(dado1, dado2);
                    }

                    if (vista != null) {
                        vista.mostrarResultadoDados(dado1, dado2, esDoble);
                    }
                    break;

                case "jugador_tiro_dados":
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
                        System.out.println("[DEBUG] Dado disponible después de mover: " + dadoDisponible);
                    }

                    // ✅ NUEVO: Actualizar TableroVista según el movimiento
                    if (tableroVista != null) {
                        if (desde == -1) {
                            // Sacó ficha de casa
                            System.out.println("[ClienteControlador] Sacando ficha #" + fichaId);
                            tableroVista.sacarFicha(fichaId);
                        } else {
                            // Movió ficha en tablero
                            int distancia = hasta - desde;
                            System.out.println("[ClienteControlador] Moviendo ficha #" + fichaId + " " + distancia + " casillas");

                            // Llamar avanzarCasilla() tantas veces como la distancia
                            for (int i = 0; i < distancia; i++) {
                                tableroVista.avanzarCasilla(fichaId);

                                // Pequeña pausa para ver la animación (opcional)
                                try {
                                    Thread.sleep(100); // 100ms entre cada casilla
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    if (automatico) {
                        if (vista != null) {
                            vista.mostrarMovimientoAutomatico(nombreMov, fichaId, desde, hasta);
                        }
                    }
                    break;

                case "ficha_capturada":
                    break;

                case "ficha_en_meta":
                    break;

                case "bonus_disponible":
                case "bonus_disponibles":
                    if (json.has("mensaje")) {
                        System.out.println("\n[BONUS] " + json.get("mensaje").getAsString());
                    }
                    break;

                case "partida_ganada":
                    String ganador = json.get("ganadorNombre").getAsString();
                    if (vista != null) {
                        vista.mostrarGanador(ganador);
                    }
                    break;

                case "penalizacion_tres_dobles":
                    String jugadorPenalizado = json.get("jugadorNombre").getAsString();
                    String mensajePenalizacion = json.get("mensaje").getAsString();
                    if (vista != null) {
                        vista.mostrarPenalizacionTresDobles(jugadorPenalizado, mensajePenalizacion);
                    }
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
            e.printStackTrace();
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
    // MÉTODOS DE JUEGO
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
    
    public boolean tirarDados() {
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "tirar_dado");
        enviarMensaje(mensajeServidor);
        
        return true;
    }
    
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
    
    public boolean moverFicha(int fichaId, int dado1, int dado2) {
        enviarMovimientoAPeers(fichaId, -1, -1);
        
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "mover_ficha");
        mensajeServidor.addProperty("fichaId", fichaId);
        mensajeServidor.addProperty("dado1", dado1);
        mensajeServidor.addProperty("dado2", dado2);
        
        return enviarMensaje(mensajeServidor);
    }
    
    public boolean moverFichaConUnDado(int fichaId, int valorDado, boolean pasarTurno) {
        enviarMovimientoAPeers(fichaId, -1, -1);
        
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "mover_ficha_un_dado");
        mensajeServidor.addProperty("fichaId", fichaId);
        mensajeServidor.addProperty("valorDado", valorDado);
        mensajeServidor.addProperty("pasarTurno", pasarTurno);
        
        return enviarMensaje(mensajeServidor);
    }
    
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
    
    public void notificarCapturaAPeers() {
        if (clientePeer == null) return;
        
        JsonObject mensajeP2P = new JsonObject();
        mensajeP2P.addProperty("tipo", "captura_peer");
        mensajeP2P.addProperty("capturadorNombre", nombreJugador);
        
        System.out.println("[P2P OUT] Notificando captura a peers");
        clientePeer.broadcastAPeers(mensajeP2P.toString());
    }
    
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
        if (vista != null) {
            vista.mostrarEstadoCompleto(jugadorId, ultimoEstadoTablero);
        }
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