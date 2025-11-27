/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.*;
import java.awt.*;

import javax.swing.*;
import java.awt.*;

public class pantallaUnirse extends JFrame {

    private Image backgroundImage;

    public pantallaUnirse() {
        setTitle("Unirse a Partida");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setResizable(false);

        // Cargar imagen de fondo
        backgroundImage = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg")).getImage();

        // Panel principal con fondo
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        mainPanel.setLayout(null);
        add(mainPanel);

        // Panel translúcido centrado
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds((900 - 400) / 2, (600 - 280) / 2, 400, 280);
        panel.setBackground(new Color(0, 0, 0, 120)); // negro translúcido
        mainPanel.add(panel);

        // ======= LABELS =======
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setForeground(Color.WHITE);
        lblNombre.setFont(new Font("Arial", Font.PLAIN, 18));
        lblNombre.setBounds(40, 40, 150, 30);
        panel.add(lblNombre);

        JLabel lblIP = new JLabel("IP del Servidor:");
        lblIP.setForeground(Color.WHITE);
        lblIP.setFont(new Font("Arial", Font.PLAIN, 18));
        lblIP.setBounds(40, 90, 150, 30);
        panel.add(lblIP);

        JLabel lblPuerto = new JLabel("Puerto:");
        lblPuerto.setForeground(Color.WHITE);
        lblPuerto.setFont(new Font("Arial", Font.PLAIN, 18));
        lblPuerto.setBounds(40, 140, 150, 30);
        panel.add(lblPuerto);

        // ======= INPUTS =======
        JTextField txtNombre = new JTextField();
        txtNombre.setBounds(190, 40, 160, 30);
        estiloInput(txtNombre);
        panel.add(txtNombre);

        JTextField txtIP = new JTextField();
        txtIP.setBounds(190, 90, 160, 30);
        estiloInput(txtIP);
        panel.add(txtIP);

        JTextField txtPuerto = new JTextField();
        txtPuerto.setBounds(190, 140, 160, 30);
        estiloInput(txtPuerto);
        panel.add(txtPuerto);

        // ======= BOTÓN ACEPTAR =======
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBounds(130, 200, 140, 40);
        btnAceptar.setBackground(new Color(255, 215, 0));
        btnAceptar.setForeground(Color.BLACK);
        btnAceptar.setFont(new Font("Arial", Font.BOLD, 18));
        btnAceptar.setFocusPainted(false);
        btnAceptar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        panel.add(btnAceptar);
    }

    // Reusar estilo del input
    private void estiloInput(JTextField txt) {
        txt.setBackground(new Color(30, 30, 30, 180));
        txt.setForeground(Color.WHITE);
        txt.setFont(new Font("Arial", Font.PLAIN, 16));
        txt.setCaretColor(Color.WHITE);
        txt.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80), 1));
    }

    public static void main(String[] args) {
        new pantallaUnirse().setVisible(true);
    }
}
