/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.*;
import java.awt.*;

public class PantallaCarga extends JFrame {

    public PantallaCarga() {
        // Pantalla completa
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        //----------------------------------
        // LAYERS
        //----------------------------------
        JLayeredPane layers = new JLayeredPane();
        layers.setBounds(0, 0, screenW, screenH);
        setContentPane(layers);

        //----------------------------------
        // FONDO
        //----------------------------------
        ImageIcon imgFondo = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg"));
        Image fondoEscalado = imgFondo.getImage().getScaledInstance(screenW, screenH, Image.SCALE_SMOOTH);
        JLabel fondo = new JLabel(new ImageIcon(fondoEscalado));
        fondo.setBounds(0, 0, screenW, screenH);
        layers.add(fondo, Integer.valueOf(0));

        //----------------------------------
        // PANEL TRANSLÚCIDO (opcional)
        //----------------------------------
        JPanel contenedor = new JPanel(null);
        contenedor.setOpaque(false);
        int w = 500;
        int h = 400;
        contenedor.setBounds((screenW - w) / 2, (screenH - h) / 2, w, h);
        layers.add(contenedor, Integer.valueOf(1));

        //----------------------------------
        // GIF ESCALADO
        //----------------------------------
        ImageIcon gifOriginal = new ImageIcon(getClass().getResource("/vista/recursos/loading.gif"));

        // --- AJUSTA ESTE VALOR PARA ESCALAR EL GIF ---
        int nuevoAncho = 190; 
        // ------------------------------------------------

        int nuevoAlto = (gifOriginal.getIconHeight() * nuevoAncho) / gifOriginal.getIconWidth();

        Image gifEscalado = gifOriginal.getImage().getScaledInstance(nuevoAncho, nuevoAlto, Image.SCALE_DEFAULT);
        ImageIcon gifFinal = new ImageIcon(gifEscalado);

        JLabel gif = new JLabel(gifFinal);
        gif.setBounds((w - nuevoAncho) / 2, 40, nuevoAncho, nuevoAlto);
        contenedor.add(gif);

        //----------------------------------
        // TEXTO
        //----------------------------------
        JLabel texto = new JLabel("Esperando jugadores...");
        texto.setHorizontalAlignment(SwingConstants.CENTER);
        texto.setForeground(Color.WHITE);
        texto.setFont(new Font("Arial", Font.BOLD, 28));
        texto.setBounds(0, 250, w, 50);
        contenedor.add(texto);

        setVisible(true);
    }

    public static void main(String[] args) {
        new PantallaCarga();
    }
}
