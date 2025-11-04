package modelo.Jugador;

import java.util.ArrayList;
import java.util.List;
import modelo.Ficha.Ficha;
import modelo.Tablero.ColorCasilla;

public class Jugador {
    private int id;
    private String nombre;
    private ColorJugador color;
    private String avatar;
    private int puntos;
    private boolean listo;
    private List<Ficha> fichas;
    private String sessionId;
    private boolean conectado;

    public Jugador(int id, String nombre, ColorJugador color, String avatar) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.avatar = avatar;
        this.puntos = 0;
        this.listo = false;
        this.fichas = new ArrayList<>();
        this.sessionId = null;
        this.conectado = false;
    }

    // Getters y setters originales
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public ColorJugador getColor() { return color; }
    public void setColor(ColorJugador color) { this.color = color; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public boolean isListo() { return listo; }
    public void setListo(boolean listo) { this.listo = listo; }
    public List<Ficha> getFichas() { return fichas; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public boolean isConectado() { return conectado; }
    public void setConectado(boolean conectado) { this.conectado = conectado; }

    // ✅ NUEVO: Convertir ColorJugador a ColorCasilla (necesario para Tablero)
    /**
     * Convierte el color del jugador al color de casilla correspondiente.
     * Necesario para que Tablero pueda identificar casillas de salida y pasillos.
     */
    public ColorCasilla getColorCasilla() {
        if (color == null) return ColorCasilla.NINGUNO;
        
        switch (color) {
            case ROJO: return ColorCasilla.ROJO;
            case AZUL: return ColorCasilla.AZUL;
            case AMARILLO: return ColorCasilla.AMARILLO;
            case VERDE: return ColorCasilla.VERDE;
            default: return ColorCasilla.NINGUNO;
        }
    }

    // Gestión de fichas
    public void agregarFicha(Ficha ficha) {
        if (ficha != null) {
            fichas.add(ficha);
            ficha.setIdJugador(this.id); // Asegurar que la ficha conoce a su dueño
        }
    }

    public Ficha getFichaPorId(int fichaId) {
        return fichas.stream()
            .filter(f -> f.getId() == fichaId)
            .findFirst()
            .orElse(null);
    }

    public boolean eliminarFicha(int fichaId) {
        return fichas.removeIf(f -> f.getId() == fichaId);
    }

    /**
    * Inicializa las fichas del jugador.
    */
    public void inicializarFichas(int cantidad) {
        fichas.clear();
        for (int i = 0; i < Math.min(cantidad, 4); i++) {
            Ficha ficha = new Ficha(i + 1, this.id, this.color);
            agregarFicha(ficha);
        }
    }

    /**
     * ✅ NUEVO: Cuenta cuántas fichas están en meta.
     */
    public int contarFichasEnMeta() {
        return (int) fichas.stream()
            .filter(Ficha::estaEnMeta)
            .count();
    }

   /**
    * Verifica si todas las fichas llegaron a meta.
    */
    public boolean haGanado() {
        if (fichas.isEmpty()) return false;
        return fichas.stream().allMatch(Ficha::estaEnMeta);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jugador)) return false;
        Jugador j = (Jugador) o;
        return this.id == j.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Jugador[id=%d, nombre=%s, color=%s, puntos=%d, fichasEnMeta=%d]",
            id, nombre, color, puntos, contarFichasEnMeta());
    }
}


