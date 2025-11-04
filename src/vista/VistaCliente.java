package vista;

import controlador.cliente.ClienteControlador;
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
    
    // ============================
    // MÉTODOS AUTOMÁTICOS
    // ============================
    
    public boolean conectarAlServidor(String ip, int puerto) {
        return controlador.conectar(ip, puerto);
    }
    
    public boolean registrarJugadorAutomatico(String nombre) {
        this.nombreJugador = nombre;
        boolean registrado = controlador.registrar(nombre);
        
        if (registrado) {
            try {
                Thread.sleep(200); // Esperar respuesta
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
    
    // ============================
    // MENÚ UNIRSE
    // ============================
    
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
    
    // ============================
    // SALA DE ESPERA
    // ============================
    
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
    
    // ============================
    // LOOP PRINCIPAL
    // ============================
    
    public void iniciarLoopCliente() {
        esperarEnSala();
        
        // Esperar hasta que la partida inicie
        while (esperandoInicioPartida) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        }
    }
    
    // ============================
    // JUEGO EN PROGRESO
    // ============================
    
    public void iniciarJuego() {
        esperandoInicioPartida = false;
        enPartida = true;
        
        System.out.println("\n" + SEPARADOR);
        System.out.println("*         LA PARTIDA HA COMENZADO!            *");
        System.out.println(SEPARADOR);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { }
        
        // Loop del juego
        while (enPartida) {
            if (controlador.esmiTurno()) {
                enPartida = menuTurno();
            } else {
                esperarTurno();
            }
        }
        
        System.out.println("\nPartida finalizada. Presiona Enter para continuar...");
        scanner.nextLine();
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
        
        // Esperar a que llegue la respuesta
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) { }
        
        // El resultado se mostrará vía notificación
        // Luego preguntar qué ficha mover
        System.out.print("\nIngresa el ID de la ficha a mover (1-4): ");
        
        try {
            int fichaId = Integer.parseInt(scanner.nextLine().trim());
            
            if (fichaId < 1 || fichaId > 4) {
                System.out.println("ID debe ser entre 1 y 4.");
                return;
            }
            
            int pasos = controlador.getUltimoResultadoDados();
            
            if (pasos <= 0) {
                System.out.println("Error: No hay resultado de dados.");
                return;
            }
            
            System.out.println("Moviendo ficha #" + fichaId + " (" + pasos + " casillas)...");
            
            boolean movido = controlador.moverFicha(fichaId, pasos);
            
            if (movido) {
                System.out.println("Ficha movida exitosamente!");
                
                // Esperar un momento
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { }
            } else {
                System.out.println("No se pudo mover la ficha.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("ID invalido.");
        }
    }
    
    private void esperarTurno() {
        // Simplemente esperar sin imprimir mucho
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
    
    // ============================
    // NOTIFICACIONES
    // ============================
    
    public void notificarTurno() {
        System.out.println("\n" + SEPARADOR);
        System.out.println("*            ES TU TURNO!                     *");
        System.out.println(SEPARADOR);
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