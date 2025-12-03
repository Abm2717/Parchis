package controlador;

import vista.VistaCliente;
import vista.TableroVista;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import controlador.peer.ClientePeer;
import java.io.*;
import java.net.Socket;
import javax.swing.SwingUtilities;

/**
 * ✅ CORREGIDO: Comunicación P2P PRIMERO, servidor solo para validación
 * 
 * FLUJO:
 * 1. Acción del jugador (tirar dados, mover ficha)
 * 2. Enviar a PEERS inmediatamente (10ms)
 * 3. Actualizar UI local
 * 4. Enviar al SERVIDOR en background (validación)
 * 5. Si servidor detecta conflicto → Corregir
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
    
    private String[] nombresGuardados = null;
    
    // ✅ NUEVO: Sistema de tracking de movimientos procesados
    private final java.util.Set<String> movimientosProcesados = 
        java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final long TIEMPO_CACHE_MOVIMIENTOS = 2000; // 2 segundos
    
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
       
    public String getNombreJugador() {
        return nombreJugador;
    }
    
    public void setVistaCarga(vista.PantallaCarga vistaCarga) {
        this.vistaCarga = vistaCarga;
    }
    
    public void setTableroVista(TableroVista tableroVista) {
        this.tableroVista = tableroVista;
        System.out.println("[ClienteControlador] TableroVista conectado");
        
        // ✅ Conectar TableroVista con ClientePeer para que procese movimientos P2P
        if (clientePeer != null) {
            clientePeer.setTableroVista(tableroVista);
        }
    }
    
    public String[] getNombresGuardados() {
        return nombresGuardados;
    }
    
    public boolean conectar(String ip, int puerto) {
        try {
            miPuertoPeer = asignarPuertoP2PAutomatico();
            System.out.println("  Puerto P2P asignado automaticamente: " + miPuertoPeer);
            
            System.out.println("[DEBUG] Iniciando cliente peer...");
            clientePeer = new ClientePeer(miPuertoPeer, this);
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

                        // ✅ CRÍTICO: Asignar ID al ClientePeer para handshake
                        if (clientePeer != null) {
                            clientePeer.setMiJugadorId(jugadorId);
                        }

                        notificarPuertoP2PAlServidor();
                    }
                    break;

                case "info_peers":
                    System.out.println("[P2P DEBUG] Recibido info_peers");
                    if (json.has("peers")) {
                        JsonArray peersArray = json.getAsJsonArray("peers");
                        System.out.println("[P2P DEBUG] Número de peers: " + peersArray.size());
                        conectarAPeers(peersArray);
                    } else {
                        System.out.println("[P2P DEBUG] ❌ No hay campo 'peers' en el mensaje");
                    }
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
                    
                    // ✅ CRÍTICO: Solicitar info de peers actualizada
                    solicitarInfoPeers();
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

                    if (json.has("jugadores")) {
                        JsonArray jugadores = json.getAsJsonArray("jugadores");

                        String[] nombres = new String[] { 
                            "Jugador Rojo", 
                            "Jugador Azul", 
                            "Jugador Verde", 
                            "Jugador Amarillo" 
                        };

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

                        nombresGuardados = nombres;
                        System.out.println("[ClienteControlador] Nombres guardados: " + jugadores.size() + " jugadores");
                        System.out.println("  Rojo=" + nombres[0] + " Azul=" + nombres[1] + " Verde=" + nombres[2] + " Amarillo=" + nombres[3]);
                    }

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
                    // ✅ Solo guardar estado, NO actualizar UI (P2P ya lo hizo)
                    JsonObject tableroJson = json.getAsJsonObject("tablero");
                    ultimoEstadoTablero = tableroJson;
                    System.out.println("[SERVIDOR] Estado tablero recibido (solo validación)");
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

                    if (tableroVista != null) {
                        tableroVista.mostrarResultadoDados(dado1, dado2);
                    }

                    if (vista != null) {
                        vista.mostrarResultadoDados(dado1, dado2, esDoble);
                    }
                    break;

                case "jugador_tiro_dados":
                    // ✅ Ignorar - P2P ya lo procesó
                    break;

                case "movimiento_exitoso":
                    // ✅ Ignorar - P2P ya lo procesó
                    if (json.has("turnoTerminado") && json.get("turnoTerminado").getAsBoolean()) {
                        esmiTurno = false;
                    }
                    break;

                case "ficha_movida":
                    // ✅ CRÍTICO: SOLO procesar movimientos AUTOMÁTICOS del servidor
                    // Los movimientos normales ya fueron procesados por P2P
                    boolean automatico = json.has("automatico") && json.get("automatico").getAsBoolean();
                    
                    if (!automatico) {
                        // ❌ Ignorar completamente - P2P ya lo procesó
                        System.out.println("[SERVIDOR] Movimiento ignorado (P2P ya procesó)");
                        break;
                    }
                    
                    // ✅ Solo movimientos automáticos (doble 5, penalizaciones)
                    String nombreMov = json.get("jugadorNombre").getAsString();
                    int fichaId = json.get("fichaId").getAsInt();
                    int desde = json.get("desde").getAsInt();
                    int hasta = json.get("hasta").getAsInt();
                    
                    int jugadorIdMov = obtenerJugadorIdDesdeNombre(nombreMov);
                    String colorFicha = obtenerColorDesdeNombre(nombreMov);
                    int fichaVisualId = ((jugadorIdMov - 1) * 4) + fichaId;

                    System.out.println("[SERVIDOR AUTO] Jugador " + nombreMov + " movió ficha #" + fichaId);

                    if (tableroVista != null) {
                        tableroVista.actualizarColorFicha(fichaVisualId, colorFicha);

                        if (desde == -1) {
                            tableroVista.sacarFicha(fichaVisualId);
                        } else if (hasta == -1) {
                            tableroVista.mandarACasa(fichaVisualId);
                        } else {
                            int distancia = Math.abs(hasta - desde);
                            for (int i = 0; i < distancia; i++) {
                                final int paso = i;
                                final int idParaAnimar = fichaVisualId;
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(paso * 150);
                                        SwingUtilities.invokeLater(() -> {
                                            tableroVista.avanzarCasilla(idParaAnimar);
                                        });
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                        }
                    }
                    break;

                case "ficha_capturada":
                    // ✅ Ignorar - P2P ya lo procesó
                    break;

                case "ficha_en_meta":
                    // ✅ Ignorar - P2P ya lo procesó
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
    
    /**
     * ✅ NUEVO: Solicita al servidor la lista actualizada de peers
     */
    private void solicitarInfoPeers() {
        JsonObject solicitud = new JsonObject();
        solicitud.addProperty("tipo", "solicitar_peers");
        solicitud.addProperty("partidaId", partidaActualId);
        enviarMensaje(solicitud);
        System.out.println("[P2P] Solicitando info actualizada de peers");
    }
    
    private void responderPong() {
        JsonObject pong = new JsonObject();
        pong.addProperty("tipo", "pong");
        enviarMensaje(pong);
    }
    
    // ========================================
    // ✅ NUEVO: Métodos públicos para que ClientePeer procese movimientos
    // ========================================
    
    /**
     * ✅ Llamado por ClientePeer cuando recibe movimiento de otro peer
     */
    public void procesarMovimientoPeerRecibido(String nombreJugador, int fichaIdRelativa, 
                                               String accion, int valorDado) {
        if (tableroVista == null) return;
        
        // ✅ CRÍTICO: Ignorar mensajes propios (evita que un jugador procese sus propios mensajes P2P)
        System.out.println("[DEBUG FILTRO] Mensaje de: '" + nombreJugador + "' | Mi nombre: '" + this.nombreJugador + "'");
        
        if (nombreJugador.equals(this.nombreJugador)) {
            System.out.println("[P2P] ✅ IGNORANDO mensaje propio de " + nombreJugador);
            return;
        }
        
        System.out.println("[P2P] ✅ PROCESANDO mensaje de " + nombreJugador + " (es otro jugador)");
        
        int jugadorIdMov = obtenerJugadorIdDesdeNombre(nombreJugador);
        String colorFicha = obtenerColorDesdeNombre(nombreJugador);
        int fichaVisualId = ((jugadorIdMov - 1) * 4) + fichaIdRelativa;
        
        System.out.println("[P2P RECV] " + nombreJugador + " - Acción: " + accion + 
                         " ficha #" + fichaVisualId + " (relativa: " + fichaIdRelativa + ") dado: " + valorDado);
        
        tableroVista.actualizarColorFicha(fichaVisualId, colorFicha);
        
        if ("sacar".equals(accion)) {
            tableroVista.sacarFicha(fichaVisualId);
            
        } else if ("mover".equals(accion)) {
            System.out.println("[ANIMACIÓN P2P] Iniciando animación de " + valorDado + " casillas para ficha #" + fichaVisualId);
            for (int i = 0; i < valorDado; i++) {
                final int paso = i;
                final int idParaAnimar = fichaVisualId;
                final int numeroPaso = i + 1;
                System.out.println("[ANIMACIÓN P2P] Programando paso " + numeroPaso + "/" + valorDado);
                new Thread(() -> {
                    try {
                        Thread.sleep(paso * 150);
                        SwingUtilities.invokeLater(() -> {
                            System.out.println("[ANIMACIÓN P2P] Ejecutando paso " + numeroPaso + "/" + valorDado + " - avanzarCasilla(#" + idParaAnimar + ")");
                            tableroVista.avanzarCasilla(idParaAnimar);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            System.out.println("[ANIMACIÓN P2P] Todos los pasos programados");
            
        } else if ("capturar".equals(accion)) {
            tableroVista.mandarACasa(fichaVisualId);
        }
    }
    
    /**
     * Limpia movimientos antiguos del cache (más de 2 segundos)
     */
    private void limpiarCacheMovimientos() {
        // Ejecutar limpieza en thread separado para no bloquear
        new Thread(() -> {
            long tiempoActual = System.currentTimeMillis();
            movimientosProcesados.removeIf(clave -> {
                try {
                    String[] partes = clave.split("_");
                    if (partes.length >= 4) {
                        long timestamp = Long.parseLong(partes[3]);
                        return (tiempoActual - timestamp) > TIEMPO_CACHE_MOVIMIENTOS;
                    }
                } catch (Exception e) {
                    // Ignorar errores de parsing
                }
                return false;
            });
        }).start();
    }
    
    private int obtenerJugadorIdDesdeNombre(String nombreJugador) {
       if (nombresGuardados == null) return 1;

       if (nombresGuardados[0].equals(nombreJugador)) {
           return 1;
       } else if (nombresGuardados[1].equals(nombreJugador)) {
           return 2;
       } else if (nombresGuardados[2].equals(nombreJugador)) {
           return 3;
       } else if (nombresGuardados[3].equals(nombreJugador)) {
           return 4;
       }

       return 1;
   }

   private String obtenerColorDesdeNombre(String nombreJugador) {
       if (nombresGuardados == null) return "ROJO";

       if (nombresGuardados[0].equals(nombreJugador)) {
           return "ROJO";
       } else if (nombresGuardados[1].equals(nombreJugador)) {
           return "AZUL";
       } else if (nombresGuardados[2].equals(nombreJugador)) {
           return "VERDE";
       } else if (nombresGuardados[3].equals(nombreJugador)) {
           return "AMARILLO";
       }

       return "ROJO";
   }
    
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
        int fichaRelativa = ((fichaId - 1) % 4) + 1;
        
        JsonObject mensajeServidor = new JsonObject();
        mensajeServidor.addProperty("tipo", "mover_ficha");
        mensajeServidor.addProperty("fichaId", fichaRelativa);
        mensajeServidor.addProperty("dado1", dado1);
        mensajeServidor.addProperty("dado2", dado2);
        
        return enviarMensaje(mensajeServidor);
    }
    
    /**
     * ✅ CORREGIDO: P2P PRIMERO, servidor después (en background)
     */
    public boolean moverFichaConUnDado(int fichaId, int valorDado, boolean pasarTurno) {
        int fichaRelativa = ((fichaId - 1) % 4) + 1;
        
        System.out.println("[DEBUG] moverFichaConUnDado: fichaVisual=" + fichaId + " → fichaRelativa=" + fichaRelativa);
        
        // Determinar acción
        String accion = "mover";
        if (tableroVista != null) {
            int posicionActual = tableroVista.obtenerPosicionFicha(fichaId);
            if (posicionActual == -1) {
                accion = "sacar";
            }
        }
        
        // ✅ CRÍTICO: Registrar este movimiento para evitar procesarlo de vuelta
        String claveMovimiento = nombreJugador + "_" + fichaRelativa + "_" + accion + "_" + System.currentTimeMillis();
        movimientosProcesados.add(claveMovimiento);
        System.out.println("[CACHE] Movimiento propio registrado: " + claveMovimiento);
        
        // Limpiar movimientos antiguos (más de 2 segundos)
        limpiarCacheMovimientos();
        
        // ✅ PASO 1: Enviar a PEERS (inmediato, 10ms)
        enviarMovimientoAPeers(fichaRelativa, accion, valorDado);
        
        // ✅ PASO 2: Actualizar UI LOCAL (optimista)
        if (tableroVista != null) {
            if ("sacar".equals(accion)) {
                tableroVista.sacarFicha(fichaId);
            } else {
                // Animar movimiento local
                System.out.println("[ANIMACIÓN] Iniciando animación de " + valorDado + " casillas para ficha #" + fichaId);
                for (int i = 0; i < valorDado; i++) {
                    final int paso = i;
                    final int numeroPaso = i + 1;
                    System.out.println("[ANIMACIÓN] Programando paso " + numeroPaso + "/" + valorDado);
                    new Thread(() -> {
                        try {
                            Thread.sleep(paso * 150);
                            SwingUtilities.invokeLater(() -> {
                                System.out.println("[ANIMACIÓN] Ejecutando paso " + numeroPaso + "/" + valorDado + " - avanzarCasilla(#" + fichaId + ")");
                                tableroVista.avanzarCasilla(fichaId);
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                System.out.println("[ANIMACIÓN] Todos los pasos programados");
            }
        }
        
        // ✅ PASO 3: Enviar al SERVIDOR (en background, solo validación)
        new Thread(() -> {
            JsonObject mensajeServidor = new JsonObject();
            mensajeServidor.addProperty("tipo", "mover_ficha_un_dado");
            mensajeServidor.addProperty("fichaId", fichaRelativa);
            mensajeServidor.addProperty("valorDado", valorDado);
            mensajeServidor.addProperty("pasarTurno", pasarTurno);
            
            enviarMensaje(mensajeServidor);
            System.out.println("[SERVIDOR] Validación enviada (background)");
        }).start();
        
        return true;
    }
    
    /**
     * ✅ Envía movimiento a peers (simplificado)
     */
    private void enviarMovimientoAPeers(int fichaIdRelativa, String accion, int valorDado) {
        if (clientePeer == null) return;
        
        JsonObject mensajeP2P = new JsonObject();
        mensajeP2P.addProperty("tipo", "movimiento_peer");
        mensajeP2P.addProperty("jugadorNombre", nombreJugador);
        mensajeP2P.addProperty("fichaId", fichaIdRelativa);
        mensajeP2P.addProperty("accion", accion);
        mensajeP2P.addProperty("valorDado", valorDado);
        
        System.out.println("[P2P OUT] " + accion + " ficha #" + fichaIdRelativa + " con dado " + valorDado);
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