package controlador.socket;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClienteParchis {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private String nombre;
    private String color;
    private boolean recibiendoTablero = false;
    private List<String> lineasTablero = new ArrayList<>();

    public ClienteParchis(String host, int puerto) throws IOException {
        socket = new Socket(host, puerto);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        scanner = new Scanner(System.in);
    }

    public void iniciar() throws IOException {
        // Thread para recibir mensajes del servidor
        Thread escucha = new Thread(() -> {
            try {
                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    procesarMensaje(mensaje);
                }
            } catch (IOException e) {
                System.out.println("Desconectado del servidor.");
            }
        });
        escucha.start();

        // Esperar mensaje inicial
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // Enviar nombre
        nombre = scanner.nextLine();
        out.println(nombre);

        try { Thread.sleep(300); } catch (InterruptedException e) {}

        // Enviar color
        color = scanner.nextLine().toUpperCase();
        out.println(color);

        System.out.println("\n********************************");
        System.out.println("*   CONECTADO AL JUEGO PARCHÍS   *");
        System.out.println("**********************************");
        System.out.println("* Jugador: " + String.format("%-20s", nombre) + " ║");
        System.out.println("* Color:   " + String.format("%-20s", color) + " ║");
        System.out.println("***********************************\n");

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Loop principal del juego
        while (true) {
            mostrarMenu();
            String opcion = scanner.nextLine();

            switch (opcion) {
                case "1":
                    realizarMovimiento();
                    break;
                case "2":
                    solicitarEstadoTablero();
                    break;
                case "3":
                    System.out.println("Saliendo del juego...");
                    cerrar();
                    return;
                default:
                    System.out.println("OpciOn invalida");
            }
        }
    }

    private void mostrarMenu() {
        System.out.println("\n*************************");
        System.out.println("*      MENÚ DE JUEGO      *");
        System.out.println("***************************");
        System.out.println("* 1. Tirar dado y mover   *");
        System.out.println("* 2. Ver estado tablero   *");
        System.out.println("* 3. Salir                *");
        System.out.println("***************************");
        System.out.print("Selecciona una opción: ");
    }

    private void procesarMensaje(String mensaje) {
        // Detectar inicio del tablero
        if (mensaje.contains("--- Estado del Tablero ---")) {
            recibiendoTablero = true;
            lineasTablero.clear();
            lineasTablero.add(mensaje);
            return;
        }

        // Detectar fin del tablero
        if (mensaje.contains("--------------------------")) {
            recibiendoTablero = false;
            lineasTablero.add(mensaje);
            mostrarTableroFormateado();
            return;
        }

        // Acumular líneas del tablero
        if (recibiendoTablero) {
            lineasTablero.add(mensaje);
        } else {
            // Mensajes normales
            System.out.println( mensaje);
        }
    }

    private void mostrarTableroFormateado() {
        System.out.println("\n" + "═".repeat(50));
        for (String linea : lineasTablero) {
            // Mostrar solo casillas ocupadas para mejor legibilidad
            if (!linea.contains("VACIA") || linea.contains("---") || linea.contains("Estado")) {
                System.out.println(linea);
            }
        }
        System.out.println("═".repeat(50) + "\n");
    }

    private void solicitarEstadoTablero() {
        System.out.println("Solicitando estado del tablero...");
        out.println("ESTADO");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }

    private void realizarMovimiento() {
        // Tirar dado
        int dado = (int) (Math.random() * 6) + 1;
        System.out.println("\n¡Tiraste el dado: " + dado + "!");

        // Mostrar fichas disponibles
        System.out.println("\nTus fichas (" + color + "):");
        System.out.println("  Ficha 1");
        System.out.println("  Ficha 2");
        System.out.println("  Ficha 3");
        System.out.println("  Ficha 4");
        System.out.print("\nSelecciona la ficha a mover (1-4): ");

        try {
            int fichaId = Integer.parseInt(scanner.nextLine());
            
            if (fichaId < 1 || fichaId > 4) {
                System.out.println(" ID invalido. Debe ser entre 1 y 4.");
                return;
            }

            // Enviar comando al servidor
            String comando = "MOVE " + fichaId + " " + dado;
            out.println(comando);
            System.out.println(" Moviendo ficha " + fichaId + " → +" + dado + " casillas");

            // Esperar respuesta del servidor
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

        } catch (NumberFormatException e) {
            System.out.println("Entrada invalida. Ingresa un numero.");
        }
    }

    private void cerrar() throws IOException {
        scanner.close();
        in.close();
        out.close();
        socket.close();
    }

    public static void main(String[] args) {
        try {
            ClienteParchis cliente = new ClienteParchis("localhost", 5000);
            cliente.iniciar();
        } catch (IOException e) {
            System.err.println("Error de conexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}