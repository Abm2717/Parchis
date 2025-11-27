/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.*;
import java.awt.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Avatares extends JFrame {

    private JLabel avatar1, avatar2, avatar3, avatar4;
    private JLabel seleccionado = null;

    public Avatares() {

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
        // PANEL CENTRAL
        //----------------------------------
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(0, 0, 0, 160));

        int w = 600;
        int h = 500;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        panel.setBounds(x, y, w, h);
        layers.add(panel, Integer.valueOf(1));

        //----------------------------------
        // ESTILOS
        //----------------------------------
        Font tituloFont = new Font("Arial", Font.BOLD, 32);
        Color amarillo = new Color(255, 235, 59);

        //----------------------------------
        // TITULO
        //----------------------------------
        JLabel titulo = new JLabel("Selecciona tu Avatar");
        titulo.setBounds(0, 20, w, 40);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(tituloFont);
        titulo.setForeground(Color.WHITE);
        panel.add(titulo);

        //----------------------------------
        // RUTAS DE LOS AVATARES (CÁMBIALAS AQUÍ)
        //----------------------------------
        String rutaAvatar1 = "/vista/recursos/pp.png";
        String rutaAvatar2 = "/vista/recursos/pp.png";
        String rutaAvatar3 = "/vista/recursos/pp.png";
        String rutaAvatar4 = "/vista/recursos/pp.png";

        //----------------------------------
        // CREAR AVATARES
        //----------------------------------
        avatar1 = crearAvatar(rutaAvatar1);
        avatar2 = crearAvatar(rutaAvatar2);
        avatar3 = crearAvatar(rutaAvatar3);
        avatar4 = crearAvatar(rutaAvatar4);

        int cuadroW = 120;
        int cuadroH = 120;
        int baseX = (w - (cuadroW * 4 + 40 * 3)) / 2;
        int baseY = 120;

        avatar1.setBounds(baseX, baseY, cuadroW, cuadroH);
        avatar2.setBounds(baseX + 160, baseY, cuadroW, cuadroH);
        avatar3.setBounds(baseX + 320, baseY, cuadroW, cuadroH);
        avatar4.setBounds(baseX + 480, baseY, cuadroW, cuadroH);

        panel.add(avatar1);
        panel.add(avatar2);
        panel.add(avatar3);
        panel.add(avatar4);

        //----------------------------------
        // BOTÓN LISTO
        //----------------------------------
        JButton btnListo = new JButton("Listo");
        btnListo.setBounds((w - 200) / 2, 380, 200, 50);
        btnListo.setBackground(amarillo);
        btnListo.setFont(new Font("Arial", Font.BOLD, 22));
        btnListo.setFocusPainted(false);
        panel.add(btnListo);

        setVisible(true);
    }

    //----------------------------------
    // CREAR UN AVATAR
    //----------------------------------
    private JLabel crearAvatar(String rutaImagen) {

        ImageIcon img = new ImageIcon(getClass().getResource(rutaImagen));
        Image esc = img.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        JLabel lbl = new JLabel(new ImageIcon(esc));

        lbl.setOpaque(true);
        lbl.setBackground(new Color(0, 0, 0, 120));
        lbl.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarAvatar(lbl);
                System.out.println("Avatar seleccionado: " + rutaImagen);
            }
        });

        return lbl;
    }

    //----------------------------------
    // MARCAR AVATAR SELECCIONADO
    //----------------------------------
    private void seleccionarAvatar(JLabel avatar) {

        // Quitar selección anterior
        if (seleccionado != null) {
            seleccionado.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        }

        // Nueva selección
        avatar.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));
        seleccionado = avatar;
    }

    public static void main(String[] args) {
        new Avatares();
    }
}
