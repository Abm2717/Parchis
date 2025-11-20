package controlador;

import vista.VistaCliente;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;

/**
 * ✅ CORREGIDO: Soporte para dados independientes
 * - Guarda dado disponible después de sacar con 5
 * - Método moverFichaConUnDado() para mover con un solo dado
 */
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
    private int ultimoResultadoDados;
    private int[] ultimosDados = new int[2];
    private boolean debeVolverATirar = false;
    private int dadoDisponible = 0;  // ✅ NUEVO: Dado disponible después de sacar con 5
    private boolean tieneFichasEnJuego = false;  // ✅ NUEVO: Si tiene fichas fuera de casa
    
    private JsonObject ultimoEstadoTablero = null;
    
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

    
    private void procesarMensajeServidor(String mensajeJson) {
        try {
            JsonObject json = JsonParser.parseString(mensajeJson).getAsJsonObject();
            String tipo = json.has("tipo") ? json.get("tipo").getAsString() : "";
            
            switch (tipo) {
                case "bienvenida":
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
                    // ✅ CORREGIDO: Guardar ambos dados y verificar dado disponible
                    JsonObject dados = json.getAsJsonObject("dados");
                    int dado1 = dados.get("dado1").getAsInt();
                    int dado2 = dados.get("dado2").getAsInt();
                    boolean esDoble = dados.get("esDoble").getAsBoolean();
                    
                    ultimosDados[0] = dado1;
                    ultimosDados[1] = dado2;
                    ultimoResultadoDados = dado1 + dado2;
                    
                    // ✅ Verificar si debe volver a tirar
                    if (json.has("debeVolverATirar")) {
                        debeVolverATirar = json.get("debeVolverATirar").getAsBoolean();
                    } else {
                        debeVolverATirar = false;
                    }
                    
                    // ✅ NUEVO: Verificar si hay un dado disponible
                    if (json.has("dadoDisponible")) {
                        dadoDisponible = json.get("dadoDisponible").getAsInt();
                        System.out.println("[DEBUG] Dado disponible recibido: " + dadoDisponible);
                    } else {
                        dadoDisponible = 0;
                    }
                    
                    // ✅ NUEVO: Verificar si tiene fichas en juego
                    if (json.has("tieneFichasEnJuego")) {
                        tieneFichasEnJuego = json.get("tieneFichasEnJuego").getAsBoolean();
                        System.out.println("[DEBUG] Tiene fichas en juego: " + tieneFichasEnJuego);
                    } else {
                        tieneFichasEnJuego = false;
                    }
                    
                    vista.mostrarResultadoDados(dado1, dado2, esDoble);
                    break;
                    
                case "jugador_tiro_dados":
                    String nombreJugador = json.get("jugadorNombre").getAsString();
                    int d1 = json.get("dado1").getAsInt();
                    int d2 = json.get("dado2").getAsInt();
                    vista.mostrarDadosOtroJugador(nombreJugador, d1, d2);
                    break;
                    
                case "movimiento_exitoso":
                    // ✅ Verificar si el turno terminó
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
                    
                    // ✅ NUEVO: Verificar si hay dado disponible después de sacar
                    if (json.has("dadoDisponible")) {
                        dadoDisponible = json.get("dadoDisponible").getAsInt();
                        System.out.println("[DEBUG] Dado disponible después de sacar: " + dadoDisponible);
                    }
                    
                    if (automatico) {
                        vista.mostrarMovimientoAutomatico(nombreMov, fichaId, desde, hasta);
                    } else {
                        vista.mostrarMovimientoOtroJugador(nombreMov, fichaId, desde, hasta);
                    }
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
    
    private void responderPong() {
        JsonObject pong = new JsonObject();
        pong.addProperty("tipo", "pong");
        enviarMensaje(pong);
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
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "tirar_dado");
        return enviarMensaje(mensaje);
    }
    
    /**
     * ✅ MANTENER: Mueve ficha usando ambos dados (compatible con código anterior)
     */
    public boolean moverFicha(int fichaId, int dado1, int dado2) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "mover_ficha");
        mensaje.addProperty("fichaId", fichaId);
        mensaje.addProperty("dado1", dado1);
        mensaje.addProperty("dado2", dado2);
        return enviarMensaje(mensaje);
    }
    
    /**
     * ✅ NUEVO: Mueve ficha usando UN SOLO dado
     * 
     * @param fichaId ID de la ficha a mover (1-4)
     * @param valorDado Valor del dado a usar
     * @param pasarTurno Si true, pasa el turno después de mover
     * @return true si el mensaje se envió correctamente
     */
    public boolean moverFichaConUnDado(int fichaId, int valorDado, boolean pasarTurno) {
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("tipo", "mover_ficha_un_dado");
        mensaje.addProperty("fichaId", fichaId);
        mensaje.addProperty("valorDado", valorDado);
        mensaje.addProperty("pasarTurno", pasarTurno);
        return enviarMensaje(mensaje);
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
    
    /**
     * ✅ Retorna ambos dados por separado
     */
    public int[] getUltimosDados() {
        return ultimosDados;
    }
    
    /**
     * ✅ Indica si el jugador sacó doble y debe volver a tirar
     */
    public boolean debeVolverATirar() {
        return debeVolverATirar;
    }
    
    /**
     * ✅ NUEVO: Limpia el estado de volver a tirar después de usarlo
     */
    public void limpiarDebeVolverATirar() {
        debeVolverATirar = false;
    }
    
    /**
     * ✅ NUEVO: Indica si el jugador tiene fichas en juego (fuera de casa)
     */
    public boolean tieneFichasEnJuego() {
        return tieneFichasEnJuego;
    }
    
    /**
     * ✅ NUEVO: Retorna el dado disponible después de sacar con 5
     * @return Valor del dado disponible, o 0 si no hay ninguno
     */
    public int getDadoDisponible() {
        return dadoDisponible;
    }
    
    /**
     * ✅ NUEVO: Limpia el dado disponible después de usarlo
     */
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