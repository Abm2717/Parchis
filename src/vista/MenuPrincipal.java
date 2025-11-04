package vista;

import controlador.servidor.ServidorCentral;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Menú principal del juego - Punto de entrada único.
 * Permite elegir entre ser HOST (crear partida) o CLIENTE (unirse a partida).
 */
public class MenuPrincipal {
    
    private static final String SEPARADOR = "************************************************";
    private static final String SEPARADOR_DOBLE = "================================================";
    
    private final Scanner scanner;
    private ServidorCentral servidor;
    
    public MenuPrincipal() {
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Inicia el menú principal.
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
        System.out.println("*                  v1.0                        *");
        System.out.println("*                                              *");
        System.out.println(SEPARADOR_DOBLE);
        System.out.println();
    }
    
    /**
     * Muestra el menú principal y captura la opción.
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
    // OPCIÓN 1: CREAR PARTIDA (HOST)
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
        
        // Obtener puerto
        System.out.print("Puerto del servidor (Enter para 5000): ");
        String puertoStr = scanner.nextLine().trim();
        int puerto = puertoStr.isEmpty() ? 5000 : Integer.parseInt(puertoStr);
        
        // Iniciar servidor en thread separado
        System.out.println("\n" + SEPARADOR);
        System.out.println("  Iniciando servidor...");
        
        servidor = new ServidorCentral(puerto);
        Thread hiloServidor = new Thread(() -> {
            servidor.iniciar();
        });
        hiloServidor.setDaemon(false); // Mantener servidor vivo
        hiloServidor.start();
        
        // Esperar a que el servidor esté listo
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Mostrar información de conexión
        mostrarInformacionConexion(puerto);
        
        // Ahora conectarse como cliente al servidor local
        System.out.println("\n  Conectandote a tu servidor...");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Iniciar cliente y conectarse automáticamente
        VistaCliente vistaCliente = new VistaCliente();
        
        // Conectar a localhost
        if (vistaCliente.conectarAlServidor("localhost", puerto)) {
            // Registrar jugador
            if (vistaCliente.registrarJugadorAutomatico(nombreJugador)) {
                // Crear sala automáticamente
                if (vistaCliente.crearSalaAutomatica(nombrePartida, maxJugadores)) {
                    System.out.println("\n  Partida creada exitosamente!");
                    System.out.println("  Esperando a que otros jugadores se unan...");
                    
                    // Iniciar loop del cliente
                    vistaCliente.iniciarLoopCliente();
                }
            }
        }
        
        // Al terminar, detener servidor
        if (servidor != null) {
            servidor.detener();
        }
    }
    
    /**
     * Muestra información para que otros se conecten.
     */
    private void mostrarInformacionConexion(int puerto) {
        System.out.println(SEPARADOR);
        System.out.println("*         SERVIDOR INICIADO                    *");
        System.out.println(SEPARADOR);
        
        try {
            // Obtener IP local
            String ipLocal = InetAddress.getLocalHost().getHostAddress();
            
            System.out.println("\n  INFORMACION PARA UNIRSE:");
            System.out.println("  " + SEPARADOR);
            System.out.println("    IP:     " + ipLocal);
            System.out.println("    Puerto: " + puerto);
            System.out.println("  " + SEPARADOR);
            System.out.println("\n  Comparte esta informacion con otros jugadores");
            System.out.println("  para que puedan unirse a tu partida.");
            
        } catch (UnknownHostException e) {
            System.out.println("  No se pudo obtener la IP local.");
            System.out.println("  Puerto: " + puerto);
        }
        
        System.out.println(SEPARADOR);
    }
    
    // ============================
    // OPCIÓN 2: UNIRSE A PARTIDA
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
        
        // Solicitar datos de conexión
        System.out.println("\n  Ingresa los datos de conexion:");
        System.out.print("  IP del servidor: ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) {
            ip = "localhost";
        }
        
        System.out.print("  Puerto (Enter para 5000): ");
        String puertoStr = scanner.nextLine().trim();
        int puerto = puertoStr.isEmpty() ? 5000 : Integer.parseInt(puertoStr);
        
        // Conectar al servidor
        System.out.println("\n  Conectando a " + ip + ":" + puerto + "...");
        
        VistaCliente vistaCliente = new VistaCliente();
        
        if (vistaCliente.conectarAlServidor(ip, puerto)) {
            if (vistaCliente.registrarJugadorAutomatico(nombreJugador)) {
                // Mostrar opciones de unión
                vistaCliente.menuUnirsePartida();
                
                // Iniciar loop del cliente
                vistaCliente.iniciarLoopCliente();
            }
        } else {
            System.out.println("\n  No se pudo conectar al servidor.");
            System.out.println("  Verifica la IP y el puerto.");
        }
    }
    
    // ============================
    // MAIN
    // ============================
    
    /**
     * Punto de entrada del programa.
     */
    public static void main(String[] args) {
        MenuPrincipal menu = new MenuPrincipal();
        menu.iniciar();
    }
}
