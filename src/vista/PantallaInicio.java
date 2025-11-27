/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.*;
import java.awt.*;

public class PantallaInicio extends JFrame {

    public PantallaInicio() {

        // Pantalla completa
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        // --- LAYERED PANE ---
        JLayeredPane layers = new JLayeredPane();
        layers.setBounds(0, 0, screenW, screenH);
        setContentPane(layers);

        // -------------------------
        //  FONDO (CAPA 0)
        // -------------------------
        ImageIcon imgFondo = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg"));
        Image fondoEscalado = imgFondo.getImage().getScaledInstance(screenW, screenH, Image.SCALE_SMOOTH);

        JLabel fondo = new JLabel(new ImageIcon(fondoEscalado));
        fondo.setBounds(0, 0, screenW, screenH);

        layers.add(fondo, Integer.valueOf(0)); // capa baja



        // -------------------------
        //  PANEL CENTRAL (CAPA 1)
        // -------------------------
        JPanel panelCentro = new JPanel(null);
        panelCentro.setOpaque(false);

        int w = 400;
        int h = 500;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        panelCentro.setBounds(x, y, w, h);

        // -------- LOGO --------
        ImageIcon logoOriginal = new ImageIcon(getClass().getResource("/vista/recursos/logoP.png"));
        Image logoImg = logoOriginal.getImage().getScaledInstance(220, 220, Image.SCALE_SMOOTH);

        JLabel logo = new JLabel(new ImageIcon(logoImg));
        logo.setBounds(90, 10, 220, 220);
        panelCentro.add(logo);

        // -------- TEXTO --------
        JLabel titulo = new JLabel("Parchis Royale");
        titulo.setBounds(0, 240, 400, 40);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 32));
        titulo.setForeground(Color.WHITE);
        panelCentro.add(titulo);

        // -------- BOTÓN CREAR --------
        JButton btnCrear = new JButton("Crear Sala");
        btnCrear.setBounds(90, 300, 220, 50);
        panelCentro.add(btnCrear);

        // -------- BOTÓN UNIRSE --------
        JButton btnUnirse = new JButton("Unirse");
        btnUnirse.setBounds(90, 370, 220, 50);
        panelCentro.add(btnUnirse);

        // Se agrega el panel en la capa superior
        layers.add(panelCentro, Integer.valueOf(1));

        setVisible(true);
    }

    public static void main(String[] args) {
        new PantallaInicio();
    }
}
