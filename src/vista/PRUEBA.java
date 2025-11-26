import javax.swing.*;

public class PRUEBA {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Tablero");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Cargar la imagen desde recursos/vista
        ImageIcon originalIcon = new ImageIcon(
                PRUEBA.class.getResource("/vista/recursos/TAB.png")
        );

        // Redimensionar a 400x400
        ImageIcon resizedIcon = new ImageIcon(
                originalIcon.getImage().getScaledInstance(700, 700, java.awt.Image.SCALE_SMOOTH)
        );

        JLabel label = new JLabel(resizedIcon);

        frame.add(label);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
