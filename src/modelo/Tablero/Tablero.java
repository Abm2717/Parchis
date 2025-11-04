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
 * Tablero de Parchís con métodos extendidos para el MotorJuego.
 * 
 * Estructura del tablero (68 casillas normales + 8 metas):
 * - Casillas 1-68: recorrido principal circular
 * - Casillas 69-72: pasillo/meta ROJO
 * - Casillas 73-76: pasillo/meta AZUL
 * - (Agregar 77-80 AMARILLO y 81-84 VERDE según necesites)
 */
public class Tablero {
    
    // Constantes del tablero
    private static final int CASILLAS_NORMALES = 68;
    private static final int CASILLAS_POR_PASILLO = 4;
    
    // Puntos de inicio para cada color
    private static final Map<ColorCasilla, Integer> CASILLAS_SALIDA = new HashMap<>() {{
        put(ColorCasilla.ROJO, 1);
        put(ColorCasilla.AZUL, 18);
        put(ColorCasilla.AMARILLO, 35);  // Ajustar según tu diseño
        put(ColorCasilla.VERDE, 52);     // Ajustar según tu diseño
    }};
    
    // Primer casilla del pasillo a meta para cada color
    private static final Map<ColorCasilla, Integer> ENTRADA_PASILLO = new HashMap<>() {{
        put(ColorCasilla.ROJO, 69);
        put(ColorCasilla.AZUL, 73);
        put(ColorCasilla.AMARILLO, 77);
        put(ColorCasilla.VERDE, 81);
    }};
    
    // Casilla segura antes de entrar al pasillo
    private static final Map<ColorCasilla, Integer> CASILLA_ENTRADA_META = new HashMap<>() {{
        put(ColorCasilla.ROJO, 68);   // Última casilla antes del pasillo rojo
        put(ColorCasilla.AZUL, 17);   // Última casilla antes del pasillo azul
        put(ColorCasilla.AMARILLO, 34);
        put(ColorCasilla.VERDE, 51);
    }};
    
    private List<Casilla> casillas;
    private Map<Integer, Jugador> jugadorPorId;  // Para lookup rápido
    
    // Constructor
    public Tablero() {
        casillas = new ArrayList<>();
        jugadorPorId = new HashMap<>();
        inicializarCasillas();
    }
    
    // Inicializar casillas
    public void inicializarCasillas() {
        casillas = new ArrayList<>();
        
        // Casillas 1-68: recorrido principal
        for (int i = 1; i <= CASILLAS_NORMALES; i++) {
            TipoCasilla tipo = determinarTipoCasilla(i);
            ColorCasilla color = determinarColorCasilla(i);
            int capacidad = tipo == TipoCasilla.SEGURA ? 2 : 2; // Todas permiten 2 fichas
            
            casillas.add(new Casilla(i, i, color, tipo, capacidad));
        }
        
        // Casillas 69-76: pasillos/metas (rojo y azul)
        for (int i = 69; i <= 72; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.ROJO, TipoCasilla.META, 1));
        }
        for (int i = 73; i <= 76; i++) {
            casillas.add(new Casilla(i, 0, ColorCasilla.AZUL, TipoCasilla.META, 1));
        }
        
        // Si tienes 4 jugadores, agregar pasillos amarillo y verde
        // for (int i = 77; i <= 80; i++) {
        //     casillas.add(new Casilla(i, 0, ColorCasilla.AMARILLO, TipoCasilla.META, 1));
        // }
        // for (int i = 81; i <= 84; i++) {
        //     casillas.add(new Casilla(i, 0, ColorCasilla.VERDE, TipoCasilla.META, 1));
        // }
    }
    
    /**
     * Determina el tipo de casilla según su posición.
     * Casillas seguras típicamente cada 12 posiciones + casillas de salida.
     */
    private TipoCasilla determinarTipoCasilla(int posicion) {
        // Casillas de inicio/salida
        if (CASILLAS_SALIDA.containsValue(posicion)) {
            return TipoCasilla.INICIO;
        }
        
        // Casillas seguras (ejemplo: cada 12 casillas hay una segura)
        // Ajusta según tu diseño específico
        if (posicion % 12 == 5) {  // Por ejemplo: 5, 17, 29, 41, 53, 65
            return TipoCasilla.SEGURA;
        }
        
        return TipoCasilla.NORMAL;
    }
    
    /**
     * Determina el color de una casilla (para pasillos y casillas especiales).
     */
    private ColorCasilla determinarColorCasilla(int posicion) {
        // Las casillas normales no tienen color específico
        // Solo las de salida y pasillos tienen color
        for (Map.Entry<ColorCasilla, Integer> entry : CASILLAS_SALIDA.entrySet()) {
            if (entry.getValue() == posicion) {
                return entry.getKey();
            }
        }
        return ColorCasilla.NINGUNO;
    }
    
    // ============================
    // MÉTODOS REQUERIDOS POR MOTORJUEGO
    // ============================
    
    /**
     * Calcula la casilla destino tras mover N pasos desde origen.
     * Maneja el recorrido circular y entrada a pasillos de meta.
     */
    public int calcularDestino(int indiceOrigen, int pasos, int jugadorId) {
        Jugador jugador = jugadorPorId.get(jugadorId);
        if (jugador == null) {
            throw new IllegalArgumentException("Jugador no encontrado: " + jugadorId);
        }
        
        ColorCasilla colorJugador = jugador.getColorCasilla();
        
        // Si está en pasillo, moverse dentro del pasillo
        if (indiceOrigen >= 69) {
            return calcularEnPasillo(indiceOrigen, pasos, colorJugador);
        }
        
        // Movimiento en tablero principal
        int nuevaPosicion = indiceOrigen + pasos;
        Integer entradaPasillo = CASILLA_ENTRADA_META.get(colorJugador);
        
        // Si pasa o llega a su entrada de pasillo, entrar
        if (entradaPasillo != null && nuevaPosicion >= entradaPasillo) {
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
        
        // No puede pasar de la última casilla del pasillo
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

        // ✅ USAR getColorCasilla() en lugar de getColor()
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

        // ✅ USAR getColorCasilla()
        ColorCasilla colorCasilla = jugador.getColorCasilla();
        Integer primerCasillaPasillo = ENTRADA_PASILLO.get(colorCasilla);

        if (primerCasillaPasillo == null) return false;

        int ultimaCasillaMeta = primerCasillaPasillo + CASILLAS_POR_PASILLO - 1;
        return casilla.getIndice() == ultimaCasillaMeta;
    }
    
    /**
     * Busca una ficha rival en una casilla específica.
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
     * Obtiene el índice de la siguiente casilla en el recorrido.
     */
    private int siguienteCasilla(int actual) {
        // Si está en pasillos, avanzar dentro del pasillo
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
        
        // Hay barrera si algún jugador tiene 2+ fichas
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
     * @return true si se rompió un bloqueo, false si no había o no se pudo romper
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
     * Registra múltiples jugadores.
     */
    public void registrarJugadores(List<Jugador> jugadores) {
        if (jugadores != null) {
            jugadores.forEach(this::registrarJugador);
        }
    }
    
    // ============================
    // MÉTODOS ORIGINALES (mantenidos)
    // ============================
    
    public Casilla getCasilla(int indice) {
        if (indice < 1 || indice > casillas.size()) {
            return null;
        }
        return casillas.get(indice - 1); // Convertir índice a posición en lista
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
     * Obtiene todas las fichas en una casilla específica.
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
     * Limpia el tablero (útil para reiniciar partida).
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