package modelo.Jugador;

import java.io.Serializable;

/**
 * Objeto de transferencia de datos para Jugador.
 * Contiene solo la información necesaria para mostrar o enviar.
 */
public class JugadorDTO implements Serializable {
    private int id;
    private String nombre;
    private ColorJugador color;
    private String avatar;
    private int puntos;
    private boolean listo;

    public JugadorDTO(int id, String nombre, ColorJugador color, String avatar, int puntos, boolean listo) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.avatar = avatar;
        this.puntos = puntos;
        this.listo = listo;
    }

    // Constructor a partir de un Jugador del modelo
    public JugadorDTO(Jugador jugador) {
        this(jugador.getId(), jugador.getNombre(), jugador.getColor(),
             jugador.getAvatar(), jugador.getPuntos(), jugador.isListo());
    }

    public int getId() { 
        return id; 
    }
    
    public String getNombre() {
        return nombre; 
    }
    
    public ColorJugador getColor() { 
        return color; 
    }
    
    public String getAvatar() { 
        return avatar; 
    }
    
    public int getPuntos() { 
        return puntos; 
    }
    
    public boolean isListo() { 
        return listo; 
    }
}
