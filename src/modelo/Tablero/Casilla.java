/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.Tablero;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import modelo.Ficha.Ficha;

/**
 *
 * @author a5581
 */
public class Casilla {
     // Atributos
    private int indice;         // numero de casilla en el tablero (1, 2, 3...)
    private int posicion;       // posicion en la interfaz gráfica (x,y o índice lineal)
    private ColorCasilla color;        // color de la casilla (o null si es neutra)
    private TipoCasilla tipo;   // tipo de casilla (NORMAL, SEGURA, INICIO, META, etc.)
    private int capacidad;        // cuántas fichas puede tener (p. ej., 1 en bloqueos)
    private boolean bloqueada;    // si está bloqueada por dos fichas iguales
    private List<Ficha> fichas;   // las fichas que están actualmente en la casilla

    // Métodos

    public Casilla(int indice, int posicion, ColorCasilla color, TipoCasilla tipo, int capacidad) {
        this.indice = indice;
        this.posicion = posicion;
        this.color = color;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.bloqueada = false;          // inicializar en false
        this.fichas = new ArrayList<>(); // 🔹 inicializar la lista
    }
    
    

    public int getIndice() {
        return indice;
    }

    public void setIndice(int indice) {
        this.indice = indice;
    }

    public int getPosicion() {
        return posicion;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }

    public ColorCasilla getColor() {
        return color;
    }

    public void setColor(ColorCasilla color) {
        this.color = color;
    }

    public TipoCasilla getTipo() {
        return tipo;
    }

    public void setTipo(TipoCasilla tipo) {
        this.tipo = tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public void setBloqueada(boolean bloqueada) {
        this.bloqueada = bloqueada;
    }

    public List<Ficha> getFichas() {
        return fichas;
    }

    public void setFichas(List<Ficha> fichas) {
        this.fichas = fichas;
    }
    
    // ✅ Método para agregar ficha
    public void agregarFicha(Ficha ficha) {
        fichas.add(ficha);
        // opcional: actualizar bloqueada si se excede capacidad
        if(fichas.size() >= capacidad) {
            bloqueada = true;
        }
    }

    // Método para quitar ficha
    public void removerFicha(Ficha ficha) {
        fichas.remove(ficha);
        // actualizar bloqueada
        bloqueada = fichas.size() >= capacidad;
    }
}
