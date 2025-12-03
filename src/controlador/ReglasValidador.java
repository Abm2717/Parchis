package controlador;

import vista.FichaVisual;
import java.util.List;

/**
 * Validador de reglas de Parchís
 * 
 * Contiene todas las validaciones de reglas del juego para uso en el cliente.
 * Esta clase es STATELESS - no guarda estado, solo valida.
 * 
 * El servidor (MotorJuego) tiene la autoridad final, esta clase solo
 * proporciona feedback inmediato al usuario.
 */
public class ReglasValidador {
    
    // ==================== CONSTANTES DE CASILLAS ====================
    
    // Casillas de entrada a pasillos
    private static final int ROJO_ENTRADA = 64;
    private static final int VERDE_ENTRADA = 13;
    private static final int AMARILLO_ENTRADA = 30;
    private static final int AZUL_ENTRADA = 47;
    
    // Primera casilla de cada pasillo
    private static final int ROJO_PASILLO_INICIO = 69;
    private static final int VERDE_PASILLO_INICIO = 76;
    private static final int AMARILLO_PASILLO_INICIO = 83;
    private static final int AZUL_PASILLO_INICIO = 90;
    
    // Última casilla de cada pasillo (meta)
    private static final int ROJO_META = 75;
    private static final int VERDE_META = 82;
    private static final int AMARILLO_META = 89;
    private static final int AZUL_META = 96;
    
    // ==================== VALIDACIÓN PRINCIPAL ====================
    
    /**
     * Verifica si una ficha puede moverse con un valor de dado
     * 
     * @param ficha La ficha a validar
     * @param valorDado Valor del dado (1-6)
     * @param todasLasFichas Lista de todas las fichas en el tablero
     * @return true si el movimiento es válido
     */
    public static boolean puedeMoverse(FichaVisual ficha, int valorDado, 
                                       List<FichaVisual> todasLasFichas) {
        if (ficha == null || ficha.estaEnCasa() || ficha.estaEnMeta()) {
            return false;
        }
        
        int posicionActual = ficha.getPosicionCasilla();
        String color = ficha.getColor();
        
        // Validar movimiento en pasillo (movimiento exacto a meta)
        if (estaEnPasillo(posicionActual, color)) {
            return validarMovimientoEnPasillo(posicionActual, color, valorDado);
        }
        
        // Verificar barreras en el camino
        if (hayBarreraEnCamino(posicionActual, valorDado, color, todasLasFichas)) {
            return false;
        }
        
        // Verificar que el destino no tenga 2 fichas propias
        int destino = calcularDestino(posicionActual, valorDado, color);
        if (destinoTieneDosFichasPropias(destino, color, todasLasFichas)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Verifica si una ficha puede salir de casa
     */
    public static boolean puedeSalirDeCasa(int casillaSalida, String color, 
                                           List<FichaVisual> todasLasFichas) {
        return !destinoTieneDosFichasPropias(casillaSalida, color, todasLasFichas);
    }
    
    // ==================== VALIDACIONES DE PASILLO ====================
    
    /**
     * Verifica si una casilla está en un pasillo
     */
    public static boolean estaEnPasillo(int casilla, String color) {
        switch (color.toUpperCase()) {
            case "ROJO":
                return casilla >= ROJO_PASILLO_INICIO && casilla <= ROJO_META;
            case "VERDE":
                return casilla >= VERDE_PASILLO_INICIO && casilla <= VERDE_META;
            case "AMARILLO":
                return casilla >= AMARILLO_PASILLO_INICIO && casilla <= AMARILLO_META;
            case "AZUL":
                return casilla >= AZUL_PASILLO_INICIO && casilla <= AZUL_META;
            default:
                return false;
        }
    }
    
    /**
     * Valida movimiento exacto a meta en pasillo
     */
    public static boolean validarMovimientoEnPasillo(int posicionActual, String color, int valorDado) {
        int casillaMetaFin = obtenerCasillaMeta(color);
        int nuevaPosicion = posicionActual + valorDado;
        
        // No puede pasarse de la meta
        return nuevaPosicion <= casillaMetaFin;
    }
    
    /**
     * Verifica si llegó a meta
     */
    public static boolean llegoAMeta(int casilla, String color) {
        return casilla == obtenerCasillaMeta(color);
    }
    
    // ==================== CÁLCULO DE DESTINO ====================
    
    /**
     * Calcula el destino de una ficha considerando entrada a pasillos
     */
    public static int calcularDestino(int posicionActual, int valorDado, String color) {
        // Si está en pasillo, movimiento normal
        if (estaEnPasillo(posicionActual, color)) {
            return posicionActual + valorDado;
        }
        
        // Simular movimiento paso a paso
        int posicion = posicionActual;
        for (int i = 0; i < valorDado; i++) {
            posicion = avanzarUnaCasilla(posicion, color);
        }
        
        return posicion;
    }
    
    /**
     * Avanza una casilla considerando entrada a pasillos y wrap-around
     */
    public static int avanzarUnaCasilla(int posicionActual, String color) {
        int casillaEntrada = obtenerCasillaEntrada(color);
        int primeraCasillaPasillo = obtenerPrimeraCasillaPasillo(color);
        
        // Entrada a pasillo
        if (posicionActual == casillaEntrada) {
            return primeraCasillaPasillo;
        }
        
        // Ya está en pasillo
        if (estaEnPasillo(posicionActual, color)) {
            return posicionActual + 1;
        }
        
        // Tablero normal con wrap-around
        if (posicionActual >= 68) {
            return 1;
        }
        
        return posicionActual + 1;
    }
    
    // ==================== VALIDACIONES DE BARRERAS ====================
    
    /**
     * Verifica si hay barrera en el camino
     */
    public static boolean hayBarreraEnCamino(int posicionActual, int valorDado, 
                                             String color, List<FichaVisual> todasLasFichas) {
        int posicion = posicionActual;
        
        for (int i = 0; i < valorDado; i++) {
            posicion = avanzarUnaCasilla(posicion, color);
            
            // No verificar barrera en el destino final
            if (i < valorDado - 1 && hayBarrera(posicion, todasLasFichas)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Verifica si hay barrera en una casilla (2 fichas del mismo color)
     */
    public static boolean hayBarrera(int casilla, List<FichaVisual> todasLasFichas) {
        int fichasEnCasilla = 0;
        String colorEnCasilla = null;
        
        for (FichaVisual ficha : todasLasFichas) {
            if (ficha.estaEnCasa() || ficha.estaEnMeta()) continue;
            
            if (ficha.getPosicionCasilla() == casilla) {
                if (colorEnCasilla == null) {
                    colorEnCasilla = ficha.getColor();
                    fichasEnCasilla = 1;
                } else if (colorEnCasilla.equals(ficha.getColor())) {
                    fichasEnCasilla++;
                }
            }
        }
        
        return fichasEnCasilla >= 2;
    }
    
    /**
     * Verifica si el destino tiene 2 fichas propias (máximo permitido)
     */
    public static boolean destinoTieneDosFichasPropias(int destino, String color, 
                                                        List<FichaVisual> todasLasFichas) {
        int fichasPropias = 0;
        
        for (FichaVisual ficha : todasLasFichas) {
            if (ficha.estaEnCasa() || ficha.estaEnMeta()) continue;
            
            if (ficha.getPosicionCasilla() == destino && ficha.getColor().equals(color)) {
                fichasPropias++;
            }
        }
        
        return fichasPropias >= 2;
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private static int obtenerCasillaEntrada(String color) {
        switch (color.toUpperCase()) {
            case "ROJO": return ROJO_ENTRADA;
            case "VERDE": return VERDE_ENTRADA;
            case "AMARILLO": return AMARILLO_ENTRADA;
            case "AZUL": return AZUL_ENTRADA;
            default: return -1;
        }
    }
    
    private static int obtenerPrimeraCasillaPasillo(String color) {
        switch (color.toUpperCase()) {
            case "ROJO": return ROJO_PASILLO_INICIO;
            case "VERDE": return VERDE_PASILLO_INICIO;
            case "AMARILLO": return AMARILLO_PASILLO_INICIO;
            case "AZUL": return AZUL_PASILLO_INICIO;
            default: return -1;
        }
    }
    
    private static int obtenerCasillaMeta(String color) {
        switch (color.toUpperCase()) {
            case "ROJO": return ROJO_META;
            case "VERDE": return VERDE_META;
            case "AMARILLO": return AMARILLO_META;
            case "AZUL": return AZUL_META;
            default: return -1;
        }
    }
    
    /**
     * Obtiene la casilla de salida de un color
     */
    public static int obtenerCasillaSalida(String color) {
        switch (color.toUpperCase()) {
            case "ROJO": return 1;
            case "AZUL": return 18;
            case "AMARILLO": return 35;
            case "VERDE": return 52;
            default: return -1;
        }
    }
}