package vista;

import controlador.servidor.ServidorCentral;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ✅ ACTUALIZADO: Menú con información de arquitectura híbrida
 */
public class MenuPrincipal {    
    
    private static final String SEPARADOR = "************************************************";
    private static final String SEPARADOR_DOBLE = "================================================";
    
    private final Scanner scanner;
    private ServidorCentral servidor;
    
    public MenuPrincipal() {
        this.scanner = new Scanner(System.in);
    }
    
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
    
    private void crearPartida() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("            CREAR PARTIDA (HOST)");
        System.out.println(SEPARADOR);
        
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
        
        System.out.print("Puerto del servidor (Enter para 5000): ");
        String puertoStr = scanner.nextLine().trim();
        int puerto = puertoStr.isEmpty() ? 5000 : Integer.parseInt(puertoStr);
        
        System.out.println("\n" + SEPARADOR);
        System.out.println("  Iniciando servidor...");
        
        servidor = new ServidorCentral(puerto);
        Thread hiloServidor = new Thread(() -> {
            servidor.iniciar();
        });
        hiloServidor.setDaemon(false); 
        hiloServidor.start();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        mostrarInformacionConexion(nombrePartida, maxJugadores, puerto);
        
        System.out.println("\n  Conectandote como HOST...");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        VistaCliente vistaCliente = new VistaCliente();
        
        if (vistaCliente.conectarAlServidor("localhost", puerto)) {
            if (vistaCliente.registrarJugadorAutomatico(nombreJugador)) {
                if (vistaCliente.crearSalaAutomatica(nombrePartida, maxJugadores)) {
                    System.out.println("\n  Partida creada exitosamente!");
                    System.out.println("  Esperando a que otros jugadores se unan...");
                    
                    vistaCliente.iniciarLoopCliente();
                }
            }
        }
    }
    
    private void mostrarInformacionConexion(String nombrePartida, int maxJugadores, int puerto) {
        System.out.println(SEPARADOR);
        System.out.println("*         SERVIDOR INICIADO                    *");
        System.out.println(SEPARADOR);
        
        try {
            String ipLocal = InetAddress.getLocalHost().getHostAddress();
            
            System.out.println("\n  INFORMACION DE LA PARTIDA:");
            System.out.println("  " + SEPARADOR);
            System.out.println("    Nombre:          " + nombrePartida);
            System.out.println("    Max. Jugadores:  " + maxJugadores);
            System.out.println("  " + SEPARADOR);
            
            System.out.println("\n  PARA UNIRSE, COMPARTE ESTA INFO:");
            System.out.println("  " + SEPARADOR);
            System.out.println("    IP del servidor: " + ipLocal);
            System.out.println("    Puerto:          " + puerto);
            System.out.println("  " + SEPARADOR);
            
            System.out.println("\n  NOTA: Los puertos P2P se asignan automaticamente");
            
        } catch (UnknownHostException e) {
            System.out.println("  No se pudo obtener la IP local.");
            System.out.println("  Puerto: " + puerto);
        }
        
        System.out.println(SEPARADOR);
    }
    
    private void unirseAPartida() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("           UNIRSE A PARTIDA");
        System.out.println(SEPARADOR);
        
        System.out.print("Tu nombre: ");
        String nombreJugador = scanner.nextLine().trim();
        if (nombreJugador.isEmpty()) {
            System.out.println("El nombre no puede estar vacio.");
            return;
        }
        
        System.out.println("\n  Ingresa los datos de conexion:");
        System.out.print("  IP del servidor: ");
        String ip = scanner.nextLine().trim();
        if (ip.isEmpty()) {
            ip = "localhost";
        }
        
        System.out.print("  Puerto del servidor (Enter para 5000): ");
        String puertoStr = scanner.nextLine().trim();
        int puerto = puertoStr.isEmpty() ? 5000 : Integer.parseInt(puertoStr);
        
        System.out.println("\n  Conectando a " + ip + ":" + puerto + "...");
        
        VistaCliente vistaCliente = new VistaCliente();
        
        if (vistaCliente.conectarAlServidor(ip, puerto)) {
            if (vistaCliente.registrarJugadorAutomatico(nombreJugador)) {
                vistaCliente.menuUnirsePartida();
                vistaCliente.iniciarLoopCliente();
            }
        } else {
            System.out.println("\n  No se pudo conectar al servidor.");
            System.out.println("  Verifica la IP y el puerto.");
        }
    }
    
    public static void main(String[] args) {
        MenuPrincipal menu = new MenuPrincipal();
        menu.iniciar();
    }
}