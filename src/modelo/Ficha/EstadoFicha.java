/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package modelo.Ficha;

/**
 *
 * @author a5581
 */
public enum EstadoFicha {
    EN_CASA,    // todavia no salio al tablero
    EN_TABLERO, // esta en el circuito principal o rampa
    EN_META,    // llego a la meta
    BLOQUEADA //en caso de que no pueda avanzar por bloqueo
}
