/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.*;
import java.awt.*;

public class CrearPartida extends JFrame {

    public CrearPartida() {

        // Pantalla completa
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenW = screen.width;
        int screenH = screen.height;

        // LAYERS
        JLayeredPane layers = new JLayeredPane();
        layers.setBounds(0, 0, screenW, screenH);
        setContentPane(layers);

        // Fondo
        ImageIcon imgFondo = new ImageIcon(getClass().getResource("/vista/recursos/fondoInicio.jpg"));
        Image fondoEscalado = imgFondo.getImage().getScaledInstance(screenW, screenH, Image.SCALE_SMOOTH);

        JLabel fondo = new JLabel(new ImageIcon(fondoEscalado));
        fondo.setBounds(0, 0, screenW, screenH);
        layers.add(fondo, Integer.valueOf(0));

        // Panel central translúcido
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(0, 0, 0, 150)); // translúcido

        int w = 450;
        int h = 480;
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;

        panel.setBounds(x, y, w, h);
        layers.add(panel, Integer.valueOf(1));

        //------------------------------
        // ESTILOS
        //------------------------------
        Font labelFont = new Font("Arial", Font.BOLD, 22);
        Font inputFont = new Font("Arial", Font.PLAIN, 20);

        Color grisClaro = new Color(200, 200, 200);
        Color negroTrans = new Color(0, 0, 0, 140);
        Color amarillo = new Color(255, 235, 59);

        //------------------------------
        // CAMPOS
        //------------------------------

        JLabel titulo = new JLabel("Crear Partida");
        titulo.setBounds(0, 20, w, 40);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 30));
        titulo.setForeground(Color.WHITE);
        panel.add(titulo);

        // Nombre jugador
        JLabel lblNombreJugador = new JLabel("Tu Nombre:");
        lblNombreJugador.setFont(labelFont);
        lblNombreJugador.setForeground(grisClaro);
        lblNombreJugador.setBounds(40, 80, 200, 30);
        panel.add(lblNombreJugador);

        JTextField txtNombreJugador = new JTextField();
        txtNombreJugador.setBounds(40, 115, 360, 40);
        txtNombreJugador.setBackground(negroTrans);
        txtNombreJugador.setForeground(Color.WHITE);
        txtNombreJugador.setFont(inputFont);
        txtNombreJugador.setBorder(null);
        panel.add(txtNombreJugador);

        // Nombre partida
        JLabel lblNombrePartida = new JLabel("Nombre Partida:");
        lblNombrePartida.setFont(labelFont);
        lblNombrePartida.setForeground(grisClaro);
        lblNombrePartida.setBounds(40, 165, 250, 30);
        panel.add(lblNombrePartida);

        JTextField txtNombrePartida = new JTextField();
        txtNombrePartida.setBounds(40, 200, 360, 40);
        txtNombrePartida.setBackground(negroTrans);
        txtNombrePartida.setForeground(Color.WHITE);
        txtNombrePartida.setFont(inputFont);
        txtNombrePartida.setBorder(null);
        panel.add(txtNombrePartida);

        // Numero de jugadores
        JLabel lblJugadores = new JLabel("Jugadores:");
        lblJugadores.setFont(labelFont);
        lblJugadores.setForeground(grisClaro);
        lblJugadores.setBounds(40, 255, 200, 30);
        panel.add(lblJugadores);

        String[] numJugadores = {"2", "3", "4"};
        JComboBox<String> comboJugadores = new JComboBox<>(numJugadores);
        comboJugadores.setBounds(220, 255, 80, 35);
        comboJugadores.setBackground(negroTrans);
        comboJugadores.setForeground(Color.WHITE);
        comboJugadores.setFont(inputFont);
        panel.add(comboJugadores);

        // Puerto del servidor
        JLabel lblPuerto = new JLabel("Puerto Servidor:");
        lblPuerto.setFont(labelFont);
        lblPuerto.setForeground(grisClaro);
        lblPuerto.setBounds(40, 305, 250, 30);
        panel.add(lblPuerto);

        JTextField txtPuerto = new JTextField("");
        txtPuerto.setBounds(40, 340, 150, 40);
        txtPuerto.setBackground(negroTrans);
        txtPuerto.setForeground(Color.WHITE);
        txtPuerto.setFont(inputFont);
        txtPuerto.setBorder(null);
        panel.add(txtPuerto);

        // Botón Aceptar
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBounds(120, 400, 200, 45);
        btnAceptar.setBackground(amarillo);
        btnAceptar.setFont(new Font("Arial", Font.BOLD, 22));
        btnAceptar.setFocusPainted(false);
        panel.add(btnAceptar);

        setVisible(true);
    }

    public static void main(String[] args) {
        new CrearPartida();
    }
}
