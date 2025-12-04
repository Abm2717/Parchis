package vista;

import controlador.ClienteControlador;
import javax.swing.*;
import java.awt.*;

public class VentanaJuego extends JFrame {
    
    private ClienteControlador controlador;
    private TableroVista tableroVista;
    
    /**
     * ✅ Constructor que recibe nombres
     */
    public VentanaJuego(ClienteControlador controlador, String[] nombres) {
        this.controlador = controlador;
        
        // Configurar ventana
        setTitle("Parchís - Juego en Curso");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // ✅ Crear TableroVista CON los nombres
        tableroVista = new TableroVista(controlador, nombres);
        setContentPane(tableroVista);
        
        System.out.println("[VentanaJuego] Ventana creada con TableroVista");
    }
    
    public TableroVista getTableroVista() {
        return tableroVista;
    }
    
    public void actualizarTablero() {
        if (tableroVista != null) {
            tableroVista.actualizarVista();
        }
    }
}   