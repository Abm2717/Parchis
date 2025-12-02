package modelo.Tablero;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import modelo.Ficha.Ficha;
import modelo.Ficha.EstadoFicha;
import modelo.Jugador.Jugador;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Tablero de Parchis con metodos extendidos para el MotorJuego.
 * ✅ CORREGIDO: Constantes de casillas alineadas con MapaCasillas.java
 * ✅ CORREGIDO: AZUL y VERDE ahora tienen las posiciones correctas
 * 
 * Estructura del tablero (68 casillas normales + 28 casillas de pasillos + 4 metas):
 * - Casillas 1-68: recorrido principal circular (antihorario)
 * - Casillas 69-75: pasillo ROJO (7 casillas)
 * - Casillas 76-82: pasillo VERDE (7 casillas)
 * - Casillas 83-89: pasillo AMARILLO (7 casillas)
 * - Casillas 90-96: pasillo AZUL (7 casillas)
 */
public class Tablero {
    
    private static final int CASILLAS_NORMALES = 68;
    private static final int CASILLAS_POR_PASILLO = 7; // ✅ CORREGIDO: Son 7 casillas por pasillo
    
    // ✅ CORREGIDO: Casillas de salida alineadas con MapaCasillas
    private static final Map<ColorCasilla, Integer> CASILLAS_SALIDA = new HashMap<>() {{
        put(ColorCasilla.ROJO, 1);       // Casilla 1 (arriba-izquierda)
        put(ColorCasilla.VERDE, 18);     // ✅ CORREGIDO (antes: 52)
        put(ColorCasilla.AMARILLO, 35);  // Casilla 35 (abajo-derecha)
        put(ColorCasilla.AZUL, 52);      // ✅ CORREGIDO (antes: 18)
    }};
    
    // ✅ CORREGIDO: Entrada a pasillos alineadas con MapaCasillas
    private static final Map<ColorCasilla, Integer> ENTRADA_PASILLO = new HashMap<>() {{
        put(ColorCasilla.ROJO, 69);      // Pasillo después de casilla 68
        put(ColorCasilla.VERDE, 76);     // ✅ CORREGIDO (antes: 81)
        put(ColorCasilla.AMARILLO, 83);  // ✅ CORREGIDO (antes: 77)
        put(ColorCasilla.AZUL, 90);      // ✅ CORREGIDO (antes: 73)
    }};
    
    // ✅ CORREGIDO: Última casilla antes del pasillo
    private static final Map<ColorCasilla, Integer> CASILLA_ENTRADA_META = new HashMap<>() {{
        put(ColorCasilla.ROJO, 68);      // Última casilla antes del pasillo rojo
        put(ColorCasilla.VERDE, 17);     // ✅ CORREGIDO (antes: 51)
        put(ColorCasilla.AMARILLO, 34);  // Última casilla antes del pasillo amarillo
        put(ColorCasilla.AZUL, 51);      // ✅ CORREGIDO (antes: 17)
    }};
    
    private List<Casilla> casillas;
    private Map<Integer, Jugador> jugadorPorId; 
    
    
    public Tablero() {
        casillas = new ArrayList<>();
        jugadorPorId = new HashMap<>();
        inicializarCasillas();
    }
    
   
    public void inicializarCasillas() {
        casillas = new ArrayList<>();
        
        // Casillas 1-68: recorrido principal
        for (int i = 1; i <= CASILLAS_NORMALES; i++) {
            TipoCasilla tipo = determinarTipoCasilla(i);
            ColorCasilla color = determinarColorCasilla(i);
            int capacidad = tipo == TipoCasilla.SEGURA ? 2 : 2; // Todas permiten 2 fichas
            
            casillas.add(new Casilla(i, i, color, tipo, capacidad));
        }
        
        // Casillas 69-75: pasillos/metas ROJO (7 casillas)
        for (int i = 69; i <= 75; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.ROJO, TipoCasilla.META, 1));
        }
        
        // Casillas 76-82: pasillos/metas VERDE (7 casillas)
        for (int i = 76; i <= 82; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.VERDE, TipoCasilla.META, 1));
        }
        
        // Casillas 83-89: pasillos/metas AMARILLO (7 casillas)
        for (int i = 83; i <= 89; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.AMARILLO, TipoCasilla.META, 1));
        }
        
        // Casillas 90-96: pasillos/metas AZUL (7 casillas)
        for (int i = 90; i <= 96; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.AZUL, TipoCasilla.META, 1));
        }
  
    }
    
    /**
     * Determina el tipo de casilla segun su posicion.
     * Casillas seguras tipicamente cada 12 posiciones + casillas de salida.
     */
    /**
    * ✅ CORREGIDO: Casillas seguras correctas del Parchís
    * - 8 casillas seguras: 8, 13, 25, 30, 42, 47, 59, 64
    * - 4 casillas de salida (también seguras): 1, 18, 35, 52
    */  
    private TipoCasilla determinarTipoCasilla(int posicion) {
        // Casillas de inicio/salida (son seguras pero con tipo especial)
        if (CASILLAS_SALIDA.containsValue(posicion)) {
            return TipoCasilla.INICIO;
        }

        // ✅ Casillas seguras (8 en total)
        if (posicion == 8 || posicion == 13 || posicion == 25 || posicion == 30 ||
            posicion == 42 || posicion == 47 || posicion == 59 || posicion == 64) {
            return TipoCasilla.SEGURA;
        }

        return TipoCasilla.NORMAL;
    }
    
    /**
     * Determina el color de una casilla (para pasillos y casillas especiales).
     */
    private ColorCasilla determinarColorCasilla(int posicion) {
  
        for (Map.Entry<ColorCasilla, Integer> entry : CASILLAS_SALIDA.entrySet()) {
            if (entry.getValue() == posicion) {
                return entry.getKey();
            }
        }
        return ColorCasilla.NINGUNO;
    }
    
    /**
     * ✅ CORREGIDO: Calcula la casilla destino tras mover N pasos desde origen.
     * Maneja el recorrido circular y entrada a pasillos de meta.
     * 
     * REGLA CRÍTICA: Solo entra al pasillo si viene del RECORRIDO NORMAL,
     * no si sale directamente de la casilla de INICIO del mismo color.
     */
    public int calcularDestino(int indiceOrigen, int pasos, int jugadorId) {
        Jugador jugador = jugadorPorId.get(jugadorId);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + jugadorId);
        }
        
        ColorCasilla colorJugador = jugador.getColorCasilla();
        Integer casillaSalida = CASILLAS_SALIDA.get(colorJugador);
        Integer entradaPasillo = CASILLA_ENTRADA_META.get(colorJugador);
        
        // Si esta en pasillo, moverse dentro del pasillo
        if (indiceOrigen >= 69) {
            return calcularEnPasillo(indiceOrigen, pasos, colorJugador);
        }
        
        // ✅ CRÍTICO: Si está en su propia casilla de INICIO, NO puede entrar directamente al pasillo
        // Debe dar toda la vuelta primero
        if (casillaSalida != null && indiceOrigen == casillaSalida) {
            // Movimiento normal circular desde INICIO
            int nuevaPosicion = indiceOrigen + pasos;
            
            if (nuevaPosicion > CASILLAS_NORMALES) {
                nuevaPosicion = nuevaPosicion - CASILLAS_NORMALES;
            }
            
            return nuevaPosicion;
        }
        
        // Movimiento en tablero principal (NO desde INICIO)
        int nuevaPosicion = indiceOrigen + pasos;
        
        // ✅ CORREGIDO: Solo entra al pasillo si:
        // 1. Está ANTES de la entrada (indiceOrigen < entradaPasillo)
        // 2. El movimiento lo lleva A o DESPUÉS de la entrada (nuevaPosicion >= entradaPasillo)
        // 3. No se pasa del tablero circular (nuevaPosicion <= CASILLAS_NORMALES)
        if (entradaPasillo != null && 
            indiceOrigen < entradaPasillo && 
            nuevaPosicion >= entradaPasillo && 
            nuevaPosicion <= CASILLAS_NORMALES) {
            
            int pasosEnPasillo = nuevaPosicion - entradaPasillo;
            int primerCasillaPasillo = ENTRADA_PASILLO.get(colorJugador);
            return primerCasillaPasillo + pasosEnPasillo;
        }
        
        // Movimiento circular normal
        if (nuevaPosicion > CASILLAS_NORMALES) {
            nuevaPosicion = nuevaPosicion - CASILLAS_NORMALES;
        }
        
        return nuevaPosicion;
    }
    
    /**
     * Calcula movimiento dentro del pasillo de meta.
     */
    private int calcularEnPasillo(int indiceActual, int pasos, ColorCasilla color) {
        int primerCasillaPasillo = ENTRADA_PASILLO.get(color);
        int ultimaCasillaPasillo = primerCasillaPasillo + CASILLAS_POR_PASILLO - 1;
        
        int nuevaPosicion = indiceActual + pasos;
        
        // No puede pasar de la ultima casilla del pasillo
        if (nuevaPosicion > ultimaCasillaPasillo) {
            throw new IllegalArgumentException(
                "Movimiento excede el final del pasillo. Necesitas valor exacto."
            );
        }
        
        return nuevaPosicion;
    }
    
    /**
     * Obtiene la casilla de salida para un jugador.
     */
    public Casilla getCasillaSalidaParaJugador(int jugadorId) {
        Jugador jugador = jugadorPorId.get(jugadorId);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + jugadorId);
        }

        ColorCasilla colorCasilla = jugador.getColorCasilla();
        Integer indiceSalida = CASILLAS_SALIDA.get(colorCasilla);

        if (indiceSalida == null) {
            throw new IllegalStateException("No hay casilla de salida para color: " + colorCasilla);
        }

        return getCasilla(indiceSalida);
    }
    
    /**
     * Verifica si una casilla es la meta final para un jugador.
     */
    public boolean esMeta(Casilla casilla, int jugadorId) {
        if (casilla == null || casilla.getTipo() != TipoCasilla.META) {
            return false;
        }

        Jugador jugador = jugadorPorId.get(jugadorId);
        if (jugador == null) return false;

        ColorCasilla colorCasilla = jugador.getColorCasilla();
        Integer primerCasillaPasillo = ENTRADA_PASILLO.get(colorCasilla);

        if (primerCasillaPasillo == null) return false;

        int ultimaCasillaMeta = primerCasillaPasillo + CASILLAS_POR_PASILLO - 1;
        return casilla.getIndice() == ultimaCasillaMeta;
    }
    
    /**
     * Busca una ficha rival en una casilla especifica.
     */
    public Ficha buscarFichaRivalEnCasilla(int indiceCasilla, int jugadorId) {
        Casilla casilla = getCasilla(indiceCasilla);
        if (casilla == null) return null;
        
        List<Ficha> fichas = casilla.getFichas();
        if (fichas == null || fichas.isEmpty()) return null;
        
        // Buscar primera ficha que no sea del jugador actual
        return fichas.stream()
            .filter(f -> f.getIdJugador() != jugadorId)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Verifica si hay una barrera en la ruta entre dos casillas.
     * Una barrera se forma cuando 2 fichas del mismo jugador ocupan la misma casilla.
     */
    public boolean rutaContieneBarrera(int indiceOrigen, int indiceDestino) {
        // Recorrer la ruta (sin incluir origen, incluyendo destino)
        int actual = indiceOrigen;
        
        while (actual != indiceDestino) {
            actual = siguienteCasilla(actual);
            
            if (esBarrera(actual)) {
                return true;
            }
            
            // Prevenir bucle infinito
            if (actual == indiceOrigen) {
                break;
            }
        }
        
        return false;
    }
    
    /**
     * ✅ NUEVO: Verifica si hay una barrera en la ruta EXCLUYENDO la casilla de origen
     * Esto permite que una ficha pueda salir de su propia barrera
     * 
     * @param indiceOrigen Casilla desde donde se mueve (se EXCLUYE de la verificación)
     * @param indiceDestino Casilla de destino
     * @return true si hay barrera en el camino (sin contar el origen)
     */
    public boolean rutaContieneBarreraExcluyendoOrigen(int indiceOrigen, int indiceDestino) {
        int actual = indiceOrigen;
        int contador = 0;
        int maxIteraciones = 70; // Prevenir bucles infinitos
        
        while (actual != indiceDestino && contador < maxIteraciones) {
            actual = siguienteCasilla(actual);
            contador++;
            
            // ✅ CRÍTICO: NO verificar la casilla de origen
            if (actual == indiceOrigen) {
                break; // Dio la vuelta completa
            }
            
            // ✅ Verificar barrera en esta casilla (que NO es el origen)
            if (actual != indiceDestino && esBarrera(actual)) {
                return true; // Hay barrera bloqueando el camino
            }
            
            // Si llegamos al destino, verificar si ahí hay barrera
            if (actual == indiceDestino) {
                break;
            }
        }
        
        return false;
    }
    
    /**
     * Obtiene el indice de la siguiente casilla en el recorrido.
     */
    private int siguienteCasilla(int actual) {
        // Si esta en pasillos, avanzar dentro del pasillo
        if (actual >= 69) {
            return actual + 1;
        }
        
        // Casillas normales: circular
        int siguiente = actual + 1;
        if (siguiente > CASILLAS_NORMALES) {
            siguiente = 1;
        }
        return siguiente;
    }
    
    /**
     * Verifica si una casilla tiene una barrera (2+ fichas del mismo jugador).
     */
    public boolean esBarrera(int indiceCasilla) {
        Casilla casilla = getCasilla(indiceCasilla);
        if (casilla == null) return false;
        
        List<Ficha> fichas = casilla.getFichas();
        if (fichas == null || fichas.size() < 2) return false;
        
        // Agrupar fichas por jugador
        Map<Integer, Long> fichasPorJugador = fichas.stream()
            .collect(Collectors.groupingBy(Ficha::getIdJugador, Collectors.counting()));
        
        // Hay barrera si algun jugador tiene 2+ fichas
        return fichasPorJugador.values().stream().anyMatch(count -> count >= 2);
    }
    
    /**
     * Verifica si hay una barrera rival en una casilla.
     */
    public boolean esBarreraRival(int indiceCasilla, int jugadorId) {
        Casilla casilla = getCasilla(indiceCasilla);
        if (casilla == null) return false;
        
        List<Ficha> fichas = casilla.getFichas();
        if (fichas == null || fichas.size() < 2) return false;
        
        // Contar fichas por jugador (excluyendo al jugador actual)
        Map<Integer, Long> fichasPorJugador = fichas.stream()
            .filter(f -> f.getIdJugador() != jugadorId)
            .collect(Collectors.groupingBy(Ficha::getIdJugador, Collectors.counting()));
        
        // Hay barrera rival si otro jugador tiene 2+ fichas
        return fichasPorJugador.values().stream().anyMatch(count -> count >= 2);
    }
    
    /**
     * Intenta romper un bloqueo propio del jugador.
     * Mueve UNA de las fichas del bloqueo a la siguiente casilla disponible.
     * 
     * @return true si se rompio un bloqueo, false si no habia o no se pudo romper
     */
    public boolean romperBloqueoPropio(int jugadorId) {
        Jugador jugador = jugadorPorId.get(jugadorId);
        if (jugador == null) return false;
        
        // Buscar casillas con 2+ fichas propias (bloqueo)
        Map<Integer, List<Ficha>> fichasPorCasilla = new HashMap<>();
        
        for (Ficha ficha : jugador.getFichas()) {
            if (ficha.getEstado() == EstadoFicha.EN_TABLERO && ficha.getCasillaActual() != null) {
                int idx = ficha.getCasillaActual().getIndice();
                fichasPorCasilla.computeIfAbsent(idx, k -> new ArrayList<>()).add(ficha);
            }
        }
        
        // Encontrar primer bloqueo (casilla con 2+ fichas)
        for (Map.Entry<Integer, List<Ficha>> entry : fichasPorCasilla.entrySet()) {
            if (entry.getValue().size() >= 2) {
                // Intentar mover una ficha del bloqueo
                Ficha fichaAMover = entry.getValue().get(0);
                int casillaActual = entry.getKey();
                int siguienteCasilla = siguienteCasilla(casillaActual);
                
                Casilla destino = getCasilla(siguienteCasilla);
                if (destino != null && !esBarrera(siguienteCasilla)) {
                    // Mover la ficha
                    fichaAMover.moverA(destino);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Registra un jugador en el tablero (necesario para mapeos de color).
     */
    public void registrarJugador(Jugador jugador) {
        if (jugador != null) {
            jugadorPorId.put(jugador.getId(), jugador);
        }
    }
    
    /**
     * Registra multiples jugadores.
     */
    public void registrarJugadores(List<Jugador> jugadores) {
        if (jugadores != null) {
            jugadores.forEach(this::registrarJugador);
        }
    }
    
    
    public Casilla getCasilla(int indice) {
        if (indice < 1 || indice > casillas.size()) {
            return null;
        }
        return casillas.get(indice - 1); // Convertir indice a posicion en lista
    }
    
    public Casilla getCasillaPorIndice(int indice) {
        return casillas.stream()
            .filter(c -> c.getIndice() == indice)
            .findFirst()
            .orElse(null);
    }
    
    public Casilla getCasillaPorPosicion(int posicion) {
        return casillas.stream()
            .filter(c -> c.getPosicion() == posicion)
            .findFirst()
            .orElse(null);
    }
    
    public int getNumeroCasillas() {
        return casillas.size();
    }
    
    public List<Casilla> getCasillas() {
        return new ArrayList<>(casillas); // Retornar copia defensiva
    }
    
    /**
     * Obtiene todas las fichas en una casilla especifica.
     */
    public List<Ficha> getFichasEnCasilla(int indiceCasilla) {
        Casilla casilla = getCasilla(indiceCasilla);
        if (casilla == null) {
            return new ArrayList<>();
        }
        
        List<Ficha> fichas = casilla.getFichas();
        return fichas != null ? new ArrayList<>(fichas) : new ArrayList<>();
    }
    
    /**
    * ✅ NUEVO: Verifica si la casilla de salida está bloqueada por fichas propias
    */
   public boolean salidaBloqueadaPorPropias(int jugadorId) {
       Casilla salida = getCasillaSalidaParaJugador(jugadorId);
       if (salida == null) return false;

       List<Ficha> fichas = salida.getFichas();
       if (fichas == null || fichas.size() < 2) return false;

       // Contar fichas propias
       long fichasPropias = fichas.stream()
           .filter(f -> f.getIdJugador() == jugadorId)
           .count();

       return fichasPropias >= 2;
   }

   /**
    * ✅ NUEVO: Verifica si hay fichas rivales en la salida
    */
   public List<Ficha> obtenerFichasRivalesEnSalida(int jugadorId) {
       Casilla salida = getCasillaSalidaParaJugador(jugadorId);
       if (salida == null) return new ArrayList<>();

       List<Ficha> fichas = salida.getFichas();
       if (fichas == null) return new ArrayList<>();

       return fichas.stream()
           .filter(f -> f.getIdJugador() != jugadorId)
           .collect(Collectors.toList());
   }

   /**
    * ✅ NUEVO: Cuenta fichas propias en la salida
    */
   public int contarFichasPropiasEnSalida(int jugadorId) {
       Casilla salida = getCasillaSalidaParaJugador(jugadorId);
       if (salida == null) return 0;

       List<Ficha> fichas = salida.getFichas();
       if (fichas == null) return 0;

       return (int) fichas.stream()
           .filter(f -> f.getIdJugador() == jugadorId)
           .count();
   }
    
    /**
     * Limpia el tablero.
     */
    public void limpiar() {
        for (Casilla casilla : casillas) {
            casilla.limpiarFichas();
        }
    }
    
    public JsonObject generarEstadoJSON() {
        JsonObject json = new JsonObject();
        JsonArray casillasArr = new JsonArray();

        for (Casilla c : casillas) {
            JsonObject cObj = new JsonObject();
            cObj.addProperty("indice", c.getIndice());
            cObj.addProperty("tipo", c.getTipo().name());
            cObj.addProperty("color", c.getColor().name());
            cObj.addProperty("bloqueada", c.isBloqueada());

            JsonArray fichasArr = new JsonArray();
            for (Ficha f : c.getFichas()) {
                JsonObject fObj = new JsonObject();
                fObj.addProperty("id", f.getId());  
                fObj.addProperty("jugadorId", f.getIdJugador());
                fObj.addProperty("color", f.getColor().name());
                fObj.addProperty("estado", f.getEstado().name());
                fichasArr.add(fObj);
            }
            cObj.add("fichas", fichasArr);
            casillasArr.add(cObj);
        }

        json.add("casillas", casillasArr);
        return json;
    }
    
    @Override
    public String toString() {
        return String.format("Tablero[%d casillas, %d jugadores registrados]", 
            casillas.size(), jugadorPorId.size());
    }
}