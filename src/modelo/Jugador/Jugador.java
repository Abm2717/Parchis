/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.Jugador;

import java.util.ArrayList;
import java.util.List;
import modelo.Ficha.Ficha;
import modelo.Tablero.ColorCasilla;

/**
 *
 * @author a5581
 */
public class Jugador {
    // Atributos
    private int id;               // identificador unico del jugador
    private String nombre;        // nombre del jugador
    private ColorJugador color;   // color de sus fichas
    private String avatar;        // ruta o identificador del avatar
    private int puntos;           // puntaje acumulado
    private boolean listo;        // si esta listo para iniciar la partida
    private List<Ficha> fichas;

    // Constructor
    public Jugador(int id, String nombre, ColorJugador color, String avatar) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.avatar = avatar;
        this.puntos = 0;
        this.listo = false;
        this.fichas = new ArrayList<>();
        
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public ColorJugador getColor() {
        return color;
    }

    public void setColor(ColorJugador color) {
        this.color = color;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public boolean isListo() {
        return listo;
    }

    public void setListo(boolean listo) {
        this.listo = listo;
    }
    
     // Metodo para agregar ficha
    public void agregarFicha(Ficha ficha) {
        fichas.add(ficha);
    }

    // Getter de fichas
    public List<Ficha> getFichas() {
        return fichas;
    }
    
    
}
