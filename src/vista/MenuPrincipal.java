package vista;

import controlador.servidor.ServidorCentral;
import controlador.peer.ClientePeer;
import modelo.cache.CachePartida;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.io.IOException;

/**
 * Menu principal del juego - Arquitectura Híbrida.
 * Permite crear o unirse a una partida P2P + Servidor.
 */
public class MenuPrincipal {    
    
    private static final String SEPARADOR = "************************************************";
    private static final String SEPARADOR_DOBLE = "================================================";
    
    // Contador para asignación automática de puertos P2P
    private static int contadorPuertosP2P = 6000 + (int)(Math.random() * 100);
    
    private final Scanner scanner;
    private ServidorCentral servidor;
    
    public MenuPrincipal() {
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Busca un puerto P2P disponible.
     */
    private int buscarPuertoP2PDisponible() {
        int puertoInicial = contadorPuertosP2P;
        int intentos = 0;
        
        while (intentos < 50) {
            int puerto = puertoInicial + intentos;
            
            if (esPuertoDisponible(puerto)) {
                contadorPuertosP2P = puerto + 1; // Siguiente puerto para el próximo jugador
                return puerto;
            }
            
            intentos++;
        }
        
        // Si no encontró puerto disponible, usar uno aleatorio alto
        return 6000 + (int)(Math.random() * 1000);
    }
    
    /**
     * Verifica si un puerto está disponible.
     */
    private boolean esPuertoDisponible(int puerto) {
        try {
            java.net.ServerSocket testSocket = new java.net.ServerSocket(puerto);
            testSocket.close();
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }
    
    /**
     * Inicia el menu principal.
     */
    public void iniciar() {
        mostrarBanner();
        
        boolean continuar = true;
        while (continuar) {
            int opcion = mostrarMenuPrincipal();
            
            switch (opcion) {
                case 1:
                    crearPartida();
                    continuar = false;
                    break;
                    
                case 2:
                    unirseAPartida();
                    continuar = false;
                    break;
                    
                case 3:
                    System.out.println("\nGracias por jugar. Hasta pronto!");
                    continuar = false;
                    break;
                    
                default:
                    System.out.println("\nOpcion invalida. Intenta de nuevo.");
            }
        }
        
        scanner.close();
    }
    
    /**
     * Muestra el banner de bienvenida.
     */
    private void mostrarBanner() {
        System.out.println("\n" + SEPARADOR_DOBLE);
        System.out.println("*                                              *");
        System.out.println("*              JUEGO DE PARCHIS                *");
        System.out.println("*         ARQUITECTURA HIBRIDA P2P            *");
        System.out.println("*                  v2.0                        *");
        System.out.println("*                                              *");
        System.out.println(SEPARADOR_DOBLE);
        System.out.println();
    }
    
    /**
     * Muestra el menu principal y captura la opcion.
     */
    private int mostrarMenuPrincipal() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("               MENU PRINCIPAL");
        System.out.println(SEPARADOR);
        System.out.println("  1. Crear Partida (Ser HOST)");
        System.out.println("  2. Unirse a Partida");
        System.out.println("  3. Salir");
        System.out.println(SEPARADOR);
        System.out.print("\nElige una opcion: ");
        
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    // ============================
    // OPCION 1: CREAR PARTIDA
    // ============================
    
    /**
     * Crea una partida (inicia servidor y se conecta automáticamente).
     */
    private void crearPartida() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("            CREAR PARTIDA (HOST)");
        System.out.println(SEPARADOR);
        
        // Solicitar datos de la partida
        System.out.print("Nombre de tu jugador: ");
        String nombreJugador = scanner.nextLine().trim();
        if (nombreJugador.isEmpty()) {
            System.out.println("El nombre no puede estar vacio.");
            return;
        }
        
        System.out.print("Nombre de la partida: ");
        String nombrePartida = scanner.nextLine().trim();
        if (nombrePartida.isEmpty()) {
            nombrePartida = "Partida de " + nombreJugador;
        }
        
        System.out.print("Numero de jugadores (2-4, Enter para 4): ");
        String maxStr = scanner.nextLine().trim();
        int maxJugadores = maxStr.isEmpty() ? 4 : Integer.parseInt(maxStr);
        
        if (maxJugadores < 2 || maxJugadores > 4) {
            System.out.println("Numero invalido. Debe ser entre 2 y 4.");
            return;
        }
        
        // Puerto del servidor
        System.out.print("Puerto del servidor (Enter para 5000): ");
        String puertoStr = scanner.nextLine().trim();
        int puertoServidor = puertoStr.isEmpty() ? 5000 : Integer.parseInt(puertoStr);
        
        // Puerto P2P automático (buscar disponible)
        int puertoP2P = buscarPuertoP2PDisponible();
        
        // Iniciar servidor en thread separado
        System.out.println("\n" + SEPARADOR);
        System.out.println("  Iniciando servidor...");
        
        servidor = new ServidorCentral(puertoServidor);
        Thread hiloServidor = new Thread(() -> {
            servidor.iniciar();
        });
        hiloServidor.setDaemon(false); 
        hiloServidor.start();
        
        // Esperar a que el servidor esté listo
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Mostrar información de conexión
        mostrarInformacionConexion(puertoServidor, puertoP2P, nombrePartida, maxJugadores);
        
        System.out.println("\n  Conectandote como HOST...");
        System.out.println("  Puerto P2P asignado automaticamente: " + puertoP2P);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Crear cliente peer e iniciar
        iniciarClientePeer(nombreJugador, "localhost", puertoServidor, puertoP2P, true);
    }
    
    /**
     * Muestra informacion para que otros se conecten.
     */
    private void mostrarInformacionConexion(int puertoServidor, int puertoP2P, 
                                            String nombrePartida, int maxJugadores) {
        System.out.println(SEPARADOR);
        System.out.println("*         SERVIDOR INICIADO                    *");
        System.out.println(SEPARADOR);
        
        try {
            // Obtener IP local
            String ipLocal = InetAddress.getLocalHost().getHostAddress();
            
            System.out.println("\n  INFORMACION DE LA PARTIDA:");
            System.out.println("  " + SEPARADOR);
            System.out.println("    Nombre:          " + nombrePartida);
            System.out.println("    Max. Jugadores:  " + maxJugadores);
            System.out.println("  " + SEPARADOR);
            
            System.out.println("\n  PARA UNIRSE, COMPARTE ESTA INFO:");
            System.out.println("  " + SEPARADOR);
            System.out.println("    IP del servidor: " + ipLocal);
            System.out.println("    Puerto:          " + puertoServidor);
            System.out.println("  " + SEPARADOR);
            System.out.println("\n  NOTA: Los puertos P2P se asignan automaticamente");
            
        } catch (UnknownHostException e) {
            System.out.println("  No se pudo obtener la IP local.");
            System.out.println("  Puerto Servidor: " + puertoServidor);
        }
        
        System.out.println(SEPARADOR);
    }
    
    // ============================
    // OPCION 2: UNIRSE A PARTIDA
    // ============================
    
    /**
     * Une a un jugador a una partida existente.
     */
    private void unirseAPartida() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("           UNIRSE A PARTIDA");
        System.out.println(SEPARADOR);
        
        // Solicitar datos del jugador
        System.out.print("Tu nombre: ");
        String nombreJugador = scanner.nextLine().trim();
        if (nombreJugador.isEmpty()) {
            System.out.println("El nombre no puede estar vacio.");
            return;
        }
        
        // Solicitar datos de conexion al servidor
        System.out.println("\n  Ingresa los datos de conexion:");
        System.out.print("  IP del servidor: ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) {
            ip = "localhost";
        }
        
        System.out.print("  Puerto del servidor (Enter para 5000): ");
        String puertoStr = scanner.nextLine().trim();
        int puertoServidor = puertoStr.isEmpty() ? 5000 : Integer.parseInt(puertoStr);
        
        // Puerto P2P automático (buscar disponible)
        int puertoP2P = buscarPuertoP2PDisponible();
        
        // Conectar al servidor
        System.out.println("\n  Conectando a " + ip + ":" + puertoServidor + "...");
        System.out.println("  Puerto P2P asignado automaticamente: " + puertoP2P);
        
        iniciarClientePeer(nombreJugador, ip, puertoServidor, puertoP2P, false);
    }
    
    // ============================
    // INICIO DE CLIENTE PEER
    // ============================
    
    /**
     * Inicia el cliente peer y el loop de juego.
     */
    private void iniciarClientePeer(String nombreJugador, String hostServidor, 
                                     int puertoServidor, int puertoP2P, boolean esHost) {
        
        System.out.println("\n[DEBUG] Iniciando cliente peer...");
        
        // Crear cache compartida
        CachePartida cache = new CachePartida();
        
        // Crear cliente peer y asociar cache
        ClientePeer clientePeer = new ClientePeer();
        clientePeer.setCache(cache);
        
        // Crear vista con cliente y cache
        VistaPeer vista = new VistaPeer(clientePeer, cache);
        
        System.out.println("[DEBUG] Conectando al servidor " + hostServidor + ":" + puertoServidor);
        
        // Conectar al servidor
        if (!clientePeer.conectarAlServidor(hostServidor, puertoServidor)) {
            System.out.println("\n  No se pudo conectar al servidor.");
            System.out.println("  Verifica la IP y el puerto.");
            return;
        }
        
        System.out.println("  Conectado al servidor!");
        
        System.out.println("[DEBUG] Iniciando servidor P2P en puerto " + puertoP2P);
        
        // Iniciar servidor P2P local
        if (!clientePeer.iniciarServidorP2P(puertoP2P)) {
            System.out.println("\n  Advertencia: No se pudo iniciar servidor P2P");
            System.out.println("  Continuando solo con conexion al servidor...");
        } else {
            System.out.println("  Servidor P2P iniciado en puerto " + puertoP2P);
        }
        
        System.out.println("[DEBUG] Registrando jugador: " + nombreJugador);
        
        // Registrar jugador
        if (!clientePeer.registrarJugador(nombreJugador)) {
            System.out.println("\n  Error al registrar jugador.");
            return;
        }
        
        System.out.println("[DEBUG] Esperando respuesta del servidor...");
        
        // Esperar un momento para recibir la respuesta del servidor
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("[DEBUG] ID del jugador: " + clientePeer.getJugadorId());
        
        // Guardar ID del jugador local en la vista
        vista.setJugadorLocalId(clientePeer.getJugadorId());
        
        // Si es HOST, unirse automáticamente a la partida
        if (esHost) {
            System.out.println("\n  Uniendote automaticamente a la partida...");
            clientePeer.marcarListo();
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("[DEBUG] Iniciando loop de juego...");
        
        // Iniciar loop de juego
        iniciarLoopJuego(clientePeer, vista, cache);
    }
    
    /**
     * Loop principal del juego.
     */
    private void iniciarLoopJuego(ClientePeer clientePeer, VistaPeer vista, CachePartida cache) {
        boolean jugando = true;
        
        while (jugando) {
            // Verificar si está en partida
            if (!clientePeer.isEnPartida()) {
                // Mostrar sala de espera
                vista.mostrarSalaEspera();
                int opcion = vista.leerOpcion();
                
                switch (opcion) {
                    case 1: // Marcar como listo
                        clientePeer.marcarListo();
                        vista.mostrarMensaje("Marcado como listo. Esperando a otros jugadores...");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                        
                    case 2: // Ver estado
                        // Ya se muestra en el menú
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                        
                    case 3: // Salir
                        jugando = false;
                        break;
                        
                    default:
                        vista.mostrarError("Opcion invalida");
                }
            } else {
                // Mostrar menú de juego
                vista.mostrarMenuJuego();
                int opcion = vista.leerOpcion();
                
                switch (opcion) {
                    case 1: // Tirar dados
                        clientePeer.tirarDados();
                        break;
                        
                    case 2: // Mover ficha
                        vista.mostrarMisFichas();
                        int fichaId = vista.solicitarIdFicha();
                        if (fichaId > 0) {
                            // Obtener dados actuales
                            int[] dados = cache.getDados();
                            if (dados[0] > 0 && dados[1] > 0) {
                                clientePeer.moverFicha(fichaId, dados[0], dados[1]);
                            } else {
                                vista.mostrarError("Debes tirar dados primero");
                            }
                        }
                        break;
                        
                    case 3: // Sacar ficha
                        vista.mostrarMisFichas();
                        int fichaIdSacar = vista.solicitarIdFicha();
                        if (fichaIdSacar > 0) {
                            int[] dadosSacar = cache.getDados();
                            if (dadosSacar[0] > 0 && dadosSacar[1] > 0) {
                                clientePeer.sacarFicha(fichaIdSacar, dadosSacar[0], dadosSacar[1]);
                            } else {
                                vista.mostrarError("Debes tirar dados primero");
                            }
                        }
                        break;
                        
                    case 4: // Ver estado
                        vista.mostrarEstadoPartida();
                        break;
                        
                    case 5: // Saltar turno
                        clientePeer.saltarTurno();
                        break;
                        
                    case 6: // Salir
                        jugando = false;
                        break;
                        
                    default:
                        vista.mostrarError("Opcion invalida");
                }
            }
            
            // Pequeña pausa para no saturar
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Desconectar
        clientePeer.desconectar();
        vista.mostrarDespedida();
        
        // Si es el host, detener el servidor
        if (servidor != null) {
            servidor.detener();
        }
    }
    
    // ============================
    // MAIN
    // ============================
    
    public static void main(String[] args) {
        MenuPrincipal menu = new MenuPrincipal();
        menu.iniciar();
    }
}