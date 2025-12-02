package vista;

import controlador.ClienteControlador;
import javax.swing.*;
import java.awt.*;

/**
 * Ventana principal del juego que contiene el TableroVista
 * ✅ NUEVA CLASE para integrar TableroVista con el controlador
 * 
 * Esta clase sirve como contenedor del TableroVista y facilita
 * la comunicación bidireccional entre el controlador y la vista.
 */
public class VentanaJuego extends JFrame {
    
    private ClienteControlador controlador;
    private TableroVista tableroVista;
    
    /**
     * Constructor que recibe el controlador del juego
     * 
     * @param controlador ClienteControlador con la conexión al servidor
     */
    public VentanaJuego(ClienteControlador controlador) {
        this.controlador = controlador;
        
        setTitle("Parchís - Juego en Curso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        
        // Crear TableroVista con el controlador
        tableroVista = new TableroVista(controlador);
        setContentPane(tableroVista);
        
        System.out.println("[VentanaJuego] Ventana creada con TableroVista");
    }
    
    /**
     * Obtiene la vista del tablero
     * Útil para que otros componentes puedan acceder al tablero
     * 
     * @return TableroVista actual
     */
    public TableroVista getTableroVista() {
        return tableroVista;
    }
    
    /**
     * Actualiza el tablero visualmente
     * Llama al método repaint del TableroVista
     */
    public void actualizarTablero() {
        if (tableroVista != null) {
            tableroVista.actualizarVista();
        }
    }
}