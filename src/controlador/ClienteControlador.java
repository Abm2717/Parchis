package controlador.cliente;

import vista.VistaCliente;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;

public class ClienteControlador {
    
    private final VistaCliente vista;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Thread hiloEscucha;
    private boolean conectado;
    private int jugadorId;
    private int partidaActualId;
    private boolean esmiTurno;
    private int ultimoResultadoDados; // ✅ NUEVO
    
    public ClienteControlador(VistaCliente vista) {
        this.vista = vista;
        this.conectado = false;
        this.jugadorId = -1;
        this.partidaActualId = -1;
        this.esmiTurno = false;
        this.ultimoResultadoDados = 0;
    }
    
    public boolean conectar(String ip, int puerto) {
        try {
            socket = new Socket(ip, puerto);
            entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8")
            );
            salida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                true
            );
            
            conectado = true;
            iniciarHiloEscucha();
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Error conectando: " + e.getMessage());
            return false;
        }
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
    
    // ============================
    // PROCESAMIENTO DE MENSAJES
    // ============================
    
    private void procesarMensajeServidor(String mensajeJson) {
        try {
            JsonObject json = JsonParser.parseString(mensajeJson).getAsJsonObject();
            String tipo = json.has("tipo") ? json.get("tipo").getAsString() : "";
            
            switch (tipo) {
                case "bienvenida":
                    // Silencioso
                    break;
                    
                case "registro_exitoso":
                    if (json.has("jugador")) {
                        JsonObject jugador = json.getAsJsonObject("jugador");
                        jugadorId = jugador.get("id").getAsInt();
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
                    vista.iniciarJuego();
                    break;
                    
                case "tu_turno":
                    esmiTurno = true;
                    vista.notificarTurno();
                    break;
                    
                case "resultado_dados":
                    // ✅ NUESTRO RESULTADO
                    JsonObject dados = json.getAsJsonObject("dados");
                    int dado1 = dados.get("dado1").getAsInt();
                    int dado2 = dados.get("dado2").getAsInt();
                    boolean esDoble = dados.get("esDoble").getAsBoolean();
                    
                    ultimoResultadoDados = dado1 + dado2;
                    vista.mostrarResultadoDados(dado1, dado2, esDoble);
                    break;
                    
                case "jugador_tiro_dados":
                    String nombreJugador = json.get("jugadorNombre").getAsString();
                    int d1 = json.get("dado1").getAsInt();
                    int d2 = json.get("dado2").getAsInt();
                    vista.mostrarDadosOtroJugador(nombreJugador, d1, d2);
                    break;
                    
                case "movimiento_exitoso":
                    esmiTurno = false; // Ya no es nuestro turno
                    break;
                    
                case "ficha_movida":
                    String nombreMov = json.get("jugadorNombre").getAsString();
                    int fichaId = json.get("fichaId").getAsInt();
                    int desde = json.get("desde").getAsInt();
                    int hasta = json.get("hasta").getAsInt();
                    vista.mostrarMovimientoOtroJugador(nombreMov, fichaId, desde, hasta);
                    break;
                    
                case "ficha_capturada":
                    String capturador = json.get("capturadorNombre").getAsString();
                    vista.mostrarCaptura(capturador);
                    break;
                    
                case "ficha_en_meta":
                    String jugadorMeta = json.get("jugadorNombre").getAsString();
                    vista.mostrarLlegadaMeta(jugadorMeta);
                    break;
                    
                case "partida_ganada":
                    String ganador = json.get("ganadorNombre").getAsString();
                    vista.mostrarGanador(ganador);
                    break;
                    
                case "error":
                    String mensaje = json.get("mensaje").getAsString();
                    System.err.println("\n[ERROR] " + mensaje);
                    break;
                    
                case "ping":
                    responderPong();
                    break;
                    
                // Ignorar mensajes de confirmación
                case "listo_confirmado":
                case "exito":
                    break;
                    
                default:
                    // Silencioso - no mostrar mensajes desconocidos
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
        }
    }
    
    private void responderPong() {
        JsonObject pong = new JsonObject();
        pong.addProperty("tipo", "pong");
        enviarMensaje(pong);
    }
    
    // ============================
    // ACCIONES
    // ============================
    
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
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "tirar_dado");
        return enviarMensaje(mensaje);
    }
    
    public boolean moverFicha(int fichaId, int pasos) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "mover_ficha");
        mensaje.addProperty("fichaId", fichaId);
        mensaje.addProperty("pasos", pasos);
        return enviarMensaje(mensaje);
    }
    
    public boolean salirDePartida() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "salir_sala");
        return enviarMensaje(mensaje);
    }
    
    public void mostrarEstadoPartida() {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "obtener_estado");
        enviarMensaje(mensaje);
    }
    
    // ============================
    // CONSULTAS
    // ============================
    
    public boolean esmiTurno() {
        return esmiTurno;
    }
    
    public int getUltimoResultadoDados() {
        return ultimoResultadoDados;
    }
    
    public void mostrarJugadoresEnSala() {
        // Placeholder - la info real viene del servidor
        System.out.println("  Jugadores en sala: (esperando info del servidor)");
    }
}
