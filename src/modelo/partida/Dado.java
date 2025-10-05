/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.partida;

import java.util.Random;

/**
 *
 * @author a5581
 */
public class Dado {
    private int valor;         // último valor obtenido
    private Random random;     // generador de números aleatorios

    // Constructor
    public Dado() {
        this.random = new Random();
        this.valor = 1; // valor inicial
    }

    // Método para tirar el dado
    public int tirar() {
        valor = random.nextInt(6) + 1; // genera número entre 1 y 6
        return valor;
    }

    // Getter del valor actual
    public int getValor() {
        return valor;
    }
}
