    package modelo.Tablero;

import java.util.ArrayList;
import java.util.List;

public class Tablero {
    // Atributos
    private List<Casilla> casillas;

    // Constructor
    public Tablero() {
        casillas = new ArrayList<>(); // 🔹 inicializar la lista
        inicializarCasillas();
    }  

    // Inicializar casillas (ejemplo básico, puedes ajustar tipos y colores)
    public void inicializarCasillas() {
        for (int i = 1; i <= 68; i++) {
            // Puedes cambiar color o tipo según el número de casilla
            Casilla casilla = new Casilla(
                i,                  // indice
                i,                  // posición
                modelo.Tablero.ColorCasilla.NINGUNO,  // color (ajustar según camino)
                modelo.Tablero.TipoCasilla.NORMAL, // tipo
                2                   // capacidad de fichas
            );
            casillas.add(casilla);
        }
    }

    // Obtener casilla por indice
    public Casilla getCasillaPorIndice(int indice) {
        for (Casilla c : casillas) {
            if (c.getIndice() == indice) return c;
        }
        return null;
    }

    // Obtener casilla por posición
    public Casilla getCasillaPorPosicion(int posicion) {
        for (Casilla c : casillas) {
            if (c.getPosicion() == posicion) return c;
        }
        return null;
    }

    // Número total de casillas
    public int getNumeroCasillas() {
        return casillas.size();
    }

    // Getter
    public List<Casilla> getCasillas() {
        return casillas;
    }
}
