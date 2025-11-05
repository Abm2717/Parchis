package vista;

import controlador.ClienteControlador;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class VistaCliente {
    
    private static final String SEPARADOR = "************************************************";
    private static final String SEPARADOR_FINO = "------------------------------------------------";
    
    private final Scanner scanner;
    private final ClienteControlador controlador;
    private String nombreJugador;
    private boolean enPartida;
    private boolean esperandoInicioPartida;
    
    public VistaCliente() {
        this.scanner = new Scanner(System.in);
        this.controlador = new ClienteControlador(this);
        this.enPartida = false;
        this.esperandoInicioPartida = false;
    }
    
    public boolean conectarAlServidor(String ip, int puerto) {
        return controlador.conectar(ip, puerto);
    }
    
    public boolean registrarJugadorAutomatico(String nombre) {
        this.nombreJugador = nombre;
        boolean registrado = controlador.registrar(nombre);
        
        if (registrado) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) { }
        }
        
        return registrado;
    }
    
    public boolean crearSalaAutomatica(String nombreSala, int maxJugadores) {
        boolean creada = controlador.crearPartida(nombreSala, maxJugadores);
        
        if (creada) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) { }
        }
        
        return creada;
    }
    
  //Menu unirse
    public void menuUnirsePartida() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("          UNIRSE A PARTIDA");
        System.out.println(SEPARADOR);
        System.out.println("1. Unirse a partida especifica (por ID)");
        System.out.println("2. Unirse a cualquier partida disponible");
        System.out.println("3. Ver partidas disponibles");
        System.out.println(SEPARADOR);
        System.out.print("\nElige una opcion: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine().trim());
            
            switch (opcion) {
                case 1:
                    unirsePartidaEspecifica();
                    break;
                case 2:
                    unirsePartidaDisponible();
                    break;
                case 3:
                    listarYUnirse();
                    break;
                default:
                    System.out.println("Opcion invalida.");
                    menuUnirsePartida();
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Numero invalido.");
            menuUnirsePartida();
        }
    }
    
    private void unirsePartidaEspecifica() {
        System.out.print("\nIngresa el ID de la partida: ");
        try {
            int partidaId = Integer.parseInt(scanner.nextLine().trim());
            System.out.println("Uniendose a partida " + partidaId + "...");
            
            boolean unido = controlador.unirseAPartida(partidaId);
            
            if (unido) {
                System.out.println("Te has unido a la partida!");
                enPartida = true;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) { }
            } else {
                System.out.println("Error al unirse.");
                menuUnirsePartida();
            }
            
        } catch (NumberFormatException e) {
            System.out.println("ID invalido.");
            menuUnirsePartida();
        }
    }
    
    private void unirsePartidaDisponible() {
        System.out.println("\nBuscando partida disponible...");
        
        boolean unido = controlador.unirseAPartidaDisponible();
        
        if (unido) {
            System.out.println("Te has unido a una partida!");
            enPartida = true;
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) { }
        } else {
            System.out.println("No hay partidas disponibles.");
            menuUnirsePartida();
        }
    }
    
    private void listarYUnirse() {
        System.out.println("\nListando partidas...");
        controlador.listarPartidas();
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) { }
        
        System.out.print("\nID de partida (0 para volver): ");
        try {
            int partidaId = Integer.parseInt(scanner.nextLine().trim());
            
            if (partidaId == 0) {
                menuUnirsePartida();
                return;
            }
            
            boolean unido = controlador.unirseAPartida(partidaId);
            
            if (unido) {
                System.out.println("Te has unido!");
                enPartida = true;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) { }
            } else {
                menuUnirsePartida();
            }
            
        } catch (NumberFormatException e) {
            menuUnirsePartida();
        }
    }
    
    
    public void esperarEnSala() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("          SALA DE ESPERA");
        System.out.println(SEPARADOR);
        System.out.println("Esperando a que mas jugadores se unan...");
        System.out.println(SEPARADOR_FINO);
        
        controlador.mostrarJugadoresEnSala();
        
        System.out.println(SEPARADOR_FINO);
        System.out.print("\nEscribe 'listo' cuando estes preparado: ");
        
        String respuesta = scanner.nextLine().trim().toLowerCase();
        
        if (respuesta.equals("listo")) {
            boolean marcado = controlador.marcarListo();
            
            if (marcado) {
                System.out.println("Esperando a otros jugadores...");
                esperandoInicioPartida = true;
            }
        }
    }
    
    
    public void iniciarLoopCliente() {
        esperarEnSala();

        System.out.println("\nEsperando que la partida inicie...");

        while (esperandoInicioPartida) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

        // Esperar a que termine el juego
        while (enPartida) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("\nPresiona Enter para salir...");
        try {
            scanner.nextLine();
        } catch (Exception e) {
            
        }
    }
    
    
    
    public void iniciarJuego() {
        esperandoInicioPartida = false;
        enPartida = true;
        
        System.out.println("\n" + SEPARADOR);
        System.out.println("*         LA PARTIDA HA COMENZADO!            *");
        System.out.println(SEPARADOR);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { }
        
        // Verificar turno inicial y mostrar mensaje
        if (controlador.esmiTurno()) {
            System.out.println("\n** Eres el primero en jugar! **");
        } else {
            System.out.println("\n** Esperando turno de otros jugadores... **");
        }
        
        // Loop del juego
        while (enPartida) {
            try {
                if (controlador.esmiTurno()) {
                    enPartida = menuTurno();
                } else {
                    esperarTurno();
                }
            } catch (Exception e) {
                System.err.println("Error en el juego: " + e.getMessage());
                break;
            }
        }
        
        System.out.println("\n" + SEPARADOR);
        System.out.println("Partida finalizada.");
        System.out.println(SEPARADOR);
    }
    
    private boolean menuTurno() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("            TU TURNO - " + nombreJugador);
        System.out.println(SEPARADOR);
        System.out.println("1. Tirar dados");
        System.out.println("2. Ver estado de la partida");
        System.out.println("3. Salir de la partida");
        System.out.println(SEPARADOR);
        System.out.print("\nElige una opcion: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine().trim());
            
            switch (opcion) {
                case 1:
                    tirarDadosYMover();
                    return true;
                    
                case 2:
                    controlador.mostrarEstadoPartida();
                    return true;
                    
                case 3:
                    return salirDePartida();
                    
                default:
                    System.out.println("Opcion invalida.");
                    return true;
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Numero invalido.");
            return true;
        }
    }
    
    private void tirarDadosYMover() {
        System.out.println("\nTirando dados...");

        controlador.tirarDados();

        // Esperar respuesta
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) { }

        int pasos = controlador.getUltimoResultadoDados();

        if (pasos <= 0) {
            System.out.println("Error: No se recibio resultado de dados.");
            System.out.println("Intenta de nuevo o sal de la partida.");
            return;
        }

        System.out.print("\nIngresa el ID de la ficha a mover (1-4, 0 para saltar turno): ");

        try {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                controlador.saltarTurno();
                controlador.marcarTurnoTerminado();
                return;
            }

            int fichaId = Integer.parseInt(input);

            if (fichaId == 0) {
                System.out.println("Turno saltado.");
                controlador.saltarTurno();
                controlador.marcarTurnoTerminado();
                return;
            }

            if (fichaId < 1 || fichaId > 4) {
                System.out.println("ID debe ser entre 1 y 4.");
                controlador.marcarTurnoTerminado();
                return;
            }

            System.out.println("Moviendo ficha #" + fichaId + " (" + pasos + " casillas)...");

            boolean movido = controlador.moverFicha(fichaId, pasos);

            if (movido) {
                System.out.println("Ficha movida exitosamente!");

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { }
            } else {
                System.out.println("No se pudo mover la ficha.");
                controlador.marcarTurnoTerminado();
            }

        } catch (NumberFormatException e) {
            System.out.println("ID invalido.");
            controlador.marcarTurnoTerminado();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            controlador.marcarTurnoTerminado();
        }
    }
    
    private void esperarTurno() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) { }
    }
    
    private boolean salirDePartida() {
        System.out.print("\nSeguro que quieres salir? (s/n): ");
        String resp = scanner.nextLine().trim().toLowerCase();
        
        if (resp.equals("s") || resp.equals("si")) {
            controlador.salirDePartida();
            return false;
        }
        
        return true;
    }
    
    
    
    public void notificarTurno() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("*            ES TU TURNO!                     *");
        System.out.println(SEPARADOR);
    }
    
    public void mostrarCambioTurno(String jugadorNombre) {
        System.out.println("\n[TURNO] Ahora es el turno de: " + jugadorNombre);
        System.out.println("** Esperando turno de otros jugadores... **");
    }
    
 
    public void mostrarEstadoTablero(JsonObject tableroJson) {
        System.out.println("\n========================================");
        System.out.println("         ESTADO DEL TABLERO");
        System.out.println("========================================\n");
        
        JsonArray casillas = tableroJson.getAsJsonArray("casillas");
        
        for (int i = 0; i < casillas.size(); i++) {
            JsonObject casilla = casillas.get(i).getAsJsonObject();
            JsonArray fichas = casilla.getAsJsonArray("fichas");
            
            // Solo mostrar casillas con fichas
            if (fichas.size() > 0) {
                int indice = casilla.get("indice").getAsInt();
                String tipo = casilla.get("tipo").getAsString();
                
                System.out.printf("Casilla %2d: ", indice);
                
                // Mostrar tipo de casilla
                if (tipo.equals("SEGURA")) {
                    System.out.print("[SEGURA] ");
                } else if (tipo.equals("META")) {
                    System.out.print("[META]   ");
                } else if (tipo.equals("INICIO")) {
                    System.out.print("[INICIO] ");
                }
                
                // Mostrar fichas
                List<String> fichasStr = new ArrayList<>();
                for (int j = 0; j < fichas.size(); j++) {
                    JsonObject ficha = fichas.get(j).getAsJsonObject();
                    String color = obtenerNombreColor(ficha.get("color").getAsString());
                    int id = ficha.get("id").getAsInt();
                    fichasStr.add(color + "-" + id);
                }
                
                System.out.println(String.join(", ", fichasStr));
            }
        }
        
        System.out.println("\n========================================\n");
    }
    
    private String obtenerNombreColor(String color) {
        switch (color) {
            case "ROJO": return "Rojo";
            case "AMARILLO": return "Amarillo";
            case "VERDE": return "Verde";
            case "AZUL": return "Azul";
            default: return "???";
        }
    }
    
    public void mostrarResultadoDados(int dado1, int dado2, boolean esDoble) {
        System.out.println("\n" + SEPARADOR_FINO);
        System.out.println("  Resultado: [" + dado1 + "] [" + dado2 + "] = " + (dado1 + dado2));
        if (esDoble) {
            System.out.println("  ** DOBLE ** - Puedes volver a tirar!");
        }
        System.out.println(SEPARADOR_FINO);
    }
    
    public void mostrarDadosOtroJugador(String nombre, int d1, int d2) {
        System.out.println("\n[INFO] " + nombre + " tiro dados: [" + d1 + "] [" + d2 + "] = " + (d1 + d2));
    }
    
    public void mostrarMovimientoOtroJugador(String nombre, int ficha, int desde, int hasta) {
        System.out.println("[INFO] " + nombre + " movio ficha #" + ficha + 
                         " (casilla " + desde + " -> " + hasta + ")");
    }
    
    public void mostrarCaptura(String capturador) {
        System.out.println("\n[CAPTURA] " + capturador + " capturo una ficha! (+20 bonus)");
    }
    
    public void mostrarLlegadaMeta(String jugador) {
        System.out.println("\n[META] " + jugador + " llego a la meta! (+10 puntos)");
    }
    
    public void mostrarGanador(String ganador) {
        System.out.println("\n" + SEPARADOR);
        System.out.println("*                                              *");
        System.out.println("*          PARTIDA FINALIZADA                  *");
        System.out.println("*                                              *");
        System.out.println(SEPARADOR);
        System.out.println("\n  GANADOR: " + ganador + "!");
        System.out.println("\n" + SEPARADOR);
        enPartida = false;
    }
    
    public void desconectar() {
        controlador.desconectar();
    }
}