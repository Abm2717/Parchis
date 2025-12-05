/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package vista;

/**
 *
 * @author jpdl2
 */
import controlador.ClienteControlador;
import javax.swing.*;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Crear ventana principal
            JFrame frame = new JFrame("Parchís - Tablero de Juego");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setLocationRelativeTo(null);

            
            // Crear controlador (puedes usar null temporalmente si no lo tienes)
            ClienteControlador controlador = null; // Cambia esto cuando tengas el controlador
            
            // Nombres de ejemplo para los jugadores
            String[] nombres = {
                "Jugador Rojo",
                "Jugador Azul", 
                "Jugador Verde",
                "Jugador Amarillo"
            };
            
            // Crear y agregar TableroVista
            TableroVista tableroVista = new TableroVista(controlador, nombres);
            frame.add(tableroVista);
            
            // Mostrar ventana
            frame.setVisible(true);
            
            System.out.println("[Main] Aplicación iniciada");
        });
    }
}