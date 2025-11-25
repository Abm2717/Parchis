package vista;

import controlador.ClienteControlador;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * ✅ NUEVA VERSIÓN: Dados COMPLETAMENTE independientes
 * - Puedes usar cada dado por separado con diferentes fichas
 * - Preguntas interactivas para elegir qué dado usar
 * - Solo automático: Doble 5 (saca 2 fichas)
 */
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

        // ✅ MEJORADO: Loop hasta que escriba "listo" correctamente
        boolean listoMarcado = false;
        while (!listoMarcado) {
            String respuesta = scanner.nextLine().trim().toLowerCase();

            if (respuesta.equals("listo")) {
                boolean marcado = controlador.marcarListo();

                if (marcado) {
                    System.out.println("Esperando a otros jugadores...");
                    esperandoInicioPartida = true;
                    listoMarcado = true;
                }
            } else {
                // ✅ Si no escribió "listo", volver a preguntar
                System.out.println("\n[ERROR] Debes escribir 'listo' para continuar");
                System.out.print("Escribe 'listo' cuando estes preparado: ");
            }
        }
    }
    
    public void mostrarSalidaBloqueada() {
        System.out.println("\n" + SEPARADOR_FINO);
        System.out.println("  [INFO] Tu salida está bloqueada por tus fichas");
        System.out.println("  Mueve una ficha del bloqueo para poder sacar");
        System.out.println(SEPARADOR_FINO);
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
        
        if (controlador.esmiTurno()) {
            System.out.println("\n** Eres el primero en jugar! **");
        } else {
            System.out.println("\n** Esperando turno de otros jugadores... **");
        }
        
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
    
    /**
     * ✅ CORREGIDO: Dados totalmente independientes con dobles usables
     * 
     * FLUJO:
     * 1. Tirar dados
     * 2. Si es DOBLE 5 → Servidor saca 2 fichas automáticamente, NO usar dados, volver al menú
     * 3. Si es DOBLE normal y turno terminó ANTES de usar dados → Penalización (3er doble)
     * 4. Si es DOBLE normal y turno sigue → Usar dados y volver al menú
     * 5. Si NO es doble → Preguntar qué hacer con cada dado
     */
    /**
    * ✅ ACTUALIZADO: Manejo completo de dobles según los 3 casos
    */
    private void tirarDadosYMover() {
       System.out.println("\nTirando dados...");

       controlador.tirarDados();

       // Esperar respuesta del servidor
       try {
           Thread.sleep(1500);
       } catch (InterruptedException e) { }

       int[] dados = controlador.getUltimosDados();

       // ✅ Notificar a peers que tiraste dados
       controlador.notificarDadosAPeers(dados[0], dados[1]);

       boolean esDoble5 = (dados[0] == 5 && dados[1] == 5);

       // ========================================
       // VERIFICACIÓN #1: ¿ES DOBLE 5?
       // ========================================
       if (esDoble5) {
           System.out.println("\n** ¡DOBLE 5! Sacaste 2 fichas. Vuelve a tirar dados **");
           controlador.limpiarDebeVolverATirar();
           return;
       }

       // ========================================
       // VERIFICACIÓN #2: ¿TURNO TERMINÓ? (Penalización o turno perdido)
       // ========================================
       if (!controlador.esmiTurno()) {
           System.out.println("\n[INFO] Tu turno ha terminado.");
           return;
       }

       // ========================================
       // VERIFICACIÓN #3: ¿ES DOBLE?
       // ========================================
       boolean esDoble = controlador.debeVolverATirar();

       if (esDoble) {
           System.out.println("\n[INFO] Sacaste DOBLE");
           controlador.limpiarDebeVolverATirar();

           // ✅ CASO 1: Doble SIN fichas fuera → Volver a tirar directamente
           if (!controlador.tieneFichasEnJuego()) {
               System.out.println("[INFO] No tienes fichas fuera. Vuelve a tirar dados");
               return; // Volver al menú para tirar de nuevo
           }

           // ✅ CASO 2: Doble CON fichas fuera → Usar dados, luego volver a tirar
           System.out.println("[INFO] Usa tus dados y podrás volver a tirar");
           usarDadosIndependientes(true); // true = es doble, no pasa turno

           // Esperar respuesta del servidor
           try {
               Thread.sleep(500);
           } catch (InterruptedException e) { }

           // Verificar si el turno sigue siendo nuestro
           if (!controlador.esmiTurno()) {
               System.out.println("\n[INFO] Tu turno ha terminado.");
               return;
           }

           System.out.println("\n** ¡DOBLE! Vuelve a tirar dados **");
           return;
       }

       // ========================================
       // NO ES DOBLE: Usar dados normalmente
       // ========================================
       usarDadosIndependientes(false); // false = NO es doble, sí pasa turno
   }
    
    /**
     * ✅ CORREGIDO: Permite usar cada dado de manera independiente
     * Si hay dadoDisponible, solo usa ese. Si no, usa ambos dados normalmente.
     * 
     * @param esDoble Si es true, NO pasa turno al final (permite volver a tirar)
     */
    private void usarDadosIndependientes(boolean esDoble) {
        int dadoDisp = controlador.getDadoDisponible();
        
        // ✅ Si hay dado disponible (porque ya se usó el otro para sacar)
        if (dadoDisp > 0) {
            System.out.println("\n" + SEPARADOR_FINO);
            System.out.println("  Dado disponible: [" + dadoDisp + "]");
            System.out.println(SEPARADOR_FINO);
            
            // ✅ CRÍTICO: Si es doble, NO pasar turno
            boolean pasarTurno = !esDoble;
            boolean usado = usarUnDadoConPasarTurno(dadoDisp, "DADO DISPONIBLE", pasarTurno);
            
            if (!usado) {
                // Pasa el turno solo si NO es doble
                if (!esDoble) {
                    controlador.saltarTurno();
                    controlador.marcarTurnoTerminado();
                }
            }
            
            controlador.limpiarDadoDisponible();
            return;
        }
        
        // ✅ Si NO hay dado disponible, usar ambos dados normalmente
        int[] dados = controlador.getUltimosDados();
        
        System.out.println("\n" + SEPARADOR_FINO);
        System.out.println("  Tus dados: [" + dados[0] + "] [" + dados[1] + "]");
        System.out.println(SEPARADOR_FINO);
        
        // Usar primer dado (NO pasa turno)
        boolean usado1 = usarUnDadoConPasarTurno(dados[0], "DADO 1", false);
        
        if (!usado1) {
            // Saltó el primer dado
            // Preguntar por el segundo
            System.out.println("\n" + SEPARADOR_FINO);
            System.out.println("  Te queda el dado: [" + dados[1] + "]");
            System.out.println(SEPARADOR_FINO);
            
            boolean pasarTurno = !esDoble;
            boolean usado2 = usarUnDadoConPasarTurno(dados[1], "DADO 2", pasarTurno);
            
            if (!usado2) {
                // Saltó ambos dados
                if (!esDoble) {
                    controlador.saltarTurno();
                    controlador.marcarTurnoTerminado();
                }
            }
            return;
        }
        
        // Esperar respuesta del servidor
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) { }
        
        // Verificar si el turno terminó después del primer movimiento
        if (!controlador.esmiTurno() && !esDoble) {
            System.out.println("\n[INFO] Tu turno ha terminado.");
            return;
        }
        
        // Usar segundo dado (SÍ pasa turno SOLO si NO es doble)
        System.out.println("\n" + SEPARADOR_FINO);
        System.out.println("  Te queda el dado: [" + dados[1] + "]");
        System.out.println(SEPARADOR_FINO);
        
        boolean pasarTurno = !esDoble;
        boolean usado2 = usarUnDadoConPasarTurno(dados[1], "DADO 2", pasarTurno);
        
        if (!usado2) {
            // Pasa el turno solo si NO es doble
            if (!esDoble) {
                controlador.saltarTurno();
                controlador.marcarTurnoTerminado();
            }
        }
    }
    
    /**
     * ✅ NUEVO: Usa un solo dado con control explícito de pasar turno
     * @param valorDado Valor del dado a usar
     * @param nombreDado Nombre descriptivo del dado
     * @param pasarTurno Si true, pasa el turno después de mover
     * @return true si se usó, false si se saltó
     */
    private boolean usarUnDadoConPasarTurno(int valorDado, String nombreDado, boolean pasarTurno) {
        System.out.print("Que ficha mueves con " + nombreDado + " [" + valorDado + "]? (1-4, 0 para saltar): ");
        
        try {
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty() || input.equals("0")) {
                System.out.println("Dado saltado.");
                return false;
            }
            
            int fichaId = Integer.parseInt(input);
            
            if (fichaId < 1 || fichaId > 4) {
                System.out.println("ID debe ser entre 1 y 4.");
                return false;
            }
            
            System.out.println("Moviendo ficha #" + fichaId + " (" + valorDado + " casillas)...");
            
            // Mover con un solo dado usando el parámetro pasarTurno
            boolean movido = controlador.moverFichaConUnDado(fichaId, valorDado, pasarTurno);
            
            if (movido) {
                System.out.println("Ficha movida exitosamente!");
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { }
                
                return true;
            } else {
                System.out.println("No se pudo mover la ficha.");
                return false;
            }
            
        } catch (NumberFormatException e) {
            System.out.println("ID invalido.");
            return false;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return false;
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
            
            if (fichas.size() > 0) {
                int indice = casilla.get("indice").getAsInt();
                String tipo = casilla.get("tipo").getAsString();
                
                System.out.printf("Casilla %2d: ", indice);
                
                if (tipo.equals("SEGURA")) {
                    System.out.print("[SEGURA] ");
                } else if (tipo.equals("META")) {
                    System.out.print("[META]   ");
                } else if (tipo.equals("INICIO")) {
                    System.out.print("[INICIO] ");
                }
                
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
        System.out.println("  Resultado: [" + dado1 + "] [" + dado2 + "]");
        if (esDoble) {
            System.out.println("  ** DOBLE ** - Puedes volver a tirar!");
        }
        System.out.println(SEPARADOR_FINO);
    }
    
    public void mostrarDadosOtroJugador(String nombre, int d1, int d2) {
        System.out.println("\n[INFO] " + nombre + " tiro dados: [" + d1 + "] [" + d2 + "]");
    }
    
    public void mostrarMovimientoOtroJugador(String nombre, int ficha, int desde, int hasta) {
        System.out.println("[INFO] " + nombre + " movio ficha #" + ficha + 
                         " (casilla " + desde + " -> " + hasta + ")");
    }
    
    public void mostrarMovimientoAutomatico(String nombre, int ficha, int desde, int hasta) {
        System.out.println("\n[AUTO] " + nombre + " saco automáticamente ficha #" + ficha + " a la casilla " + hasta);
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
    
    public void mostrarPenalizacionTresDobles(String jugador, String mensaje) {
        System.out.println("\n" + SEPARADOR);
        System.out.println("*         ¡PENALIZACION!                       *");
        System.out.println(SEPARADOR);
        System.out.println("  " + mensaje);
        System.out.println(SEPARADOR);
    }
    
    public void mostrarEstadoCompleto(int jugadorId, JsonObject tablero) {
        System.out.println("\n" + SEPARADOR);
        System.out.println("          ESTADO DE LA PARTIDA");
        System.out.println(SEPARADOR);
        System.out.println("  Jugador: " + nombreJugador + " (ID: " + jugadorId + ")");
        System.out.println("  Estado: " + (controlador.esmiTurno() ? "ES TU TURNO" : "Esperando turno"));
        
        int[] ultimosDados = controlador.getUltimosDados();
        if (ultimosDados[0] > 0 || ultimosDados[1] > 0) {
            System.out.println("  Ultimos dados: [" + ultimosDados[0] + "] [" + ultimosDados[1] + "]");
        }
        
        System.out.println(SEPARADOR);
        
        if (tablero != null) {
            mostrarEstadoTablero(tablero);
        } else {
            System.out.println("\n  [INFO] Aun no hay movimientos en el tablero.");
            System.out.println("  El estado se actualizara despues del primer movimiento.\n");
        }
    }
    
    /**
     * ✅ CORREGIDO: Verifica si el jugador puede jugar con los dados actuales
     * PRIORIDAD: Si tiene fichas en juego, SIEMPRE puede intentar mover
     */
    private boolean puedeJugarConDados() {
        int[] dados = controlador.getUltimosDados();
        int dadoDisp = controlador.getDadoDisponible();
        
        // ✅ PRIORIDAD #1: Si tiene fichas en juego, SIEMPRE puede jugar
        if (controlador.tieneFichasEnJuego()) {
            return true;
        }
        
        // ✅ Si NO tiene fichas en juego, verificar si puede sacar
        
        // Si hay dado disponible (ya sacó con 5)
        if (dadoDisp > 0) {
            return true;
        }
        
        // Si algún dado es 5, puede sacar ficha
        if (dados[0] == 5 || dados[1] == 5) {
            return true;
        }
        
        // Si la suma es 5, puede sacar ficha
        if (dados[0] + dados[1] == 5) {
            return true;
        }
        
        // No tiene fichas y no puede sacar
        return false;
    }
    
    public void desconectar() {
        controlador.desconectar();
    }
}