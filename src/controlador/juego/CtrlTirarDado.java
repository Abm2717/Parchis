package controlador.juego;

import modelo.partida.MotorJuego;
import modelo.partida.MotorJuego.ResultadoDados;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.Jugador.Jugador;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Controlador para tirar dados en arquitectura híbrida P2P + Servidor.
 * 
 * El servidor valida y ejecuta la tirada oficial, luego hace broadcast
 * a todos los peers para sincronización.
 */
public class CtrlTirarDado {
    
    private final MotorJuego motor;
    
    public CtrlTirarDado(MotorJuego motor) {
        this.motor = motor;
    }
    
    /**
     * Procesa la tirada de dados de un jugador.
     * 
     * @param jugadorId ID del jugador que tira
     * @return JsonObject con resultado para broadcast
     */
    public JsonObject tirarDados(int jugadorId) {
        JsonObject respuesta = new JsonObject();
        
        try {
            // Validar que sea el turno del jugador
            Partida partida = motor.getPartida();
            
            if (!esElTurnoDelJugador(jugadorId)) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_ES_TU_TURNO");
                respuesta.addProperty("mensaje", "No es tu turno");
                return respuesta;
            }
            
            // Validar que la partida esté EN_PROGRESO
            if (partida.getEstado() != EstadoPartida.EN_PROGRESO) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "PARTIDA_NO_INICIADA");
                respuesta.addProperty("mensaje", "La partida no está en progreso");
                return respuesta;
            }
            
            // Tirar dados (el motor maneja la lógica completa)
            ResultadoDados resultado = motor.tirarDados(jugadorId);
            
            // Construir respuesta exitosa
            respuesta.addProperty("exito", true);
            respuesta.addProperty("tipo", "DADOS_TIRADOS");
            respuesta.addProperty("jugadorId", jugadorId);
            
            // Agregar los dados
            JsonArray dadosArray = new JsonArray();
            dadosArray.add(resultado.dado1);
            dadosArray.add(resultado.dado2);
            respuesta.add("dados", dadosArray);
            
            // Información del turno
            respuesta.addProperty("esDoble", resultado.esDoble);
            respuesta.addProperty("bloqueoRoto", resultado.bloqueoRoto);
            respuesta.addProperty("fichaPerdida", resultado.fichaPerdida);
            respuesta.addProperty("contadorDobles", resultado.contadorDobles);
            
            // Mensajes según el resultado
            if (resultado.fichaPerdida) {
                respuesta.addProperty("mensaje", "¡Tres dobles consecutivos! Pierdes una ficha");
                // El turno pasa al siguiente
                partida.avanzarTurno();
                Jugador siguienteJugador = partida.getJugadorActual();
                respuesta.addProperty("siguienteTurno", siguienteJugador.getId());
            } else if (resultado.esDoble) {
                respuesta.addProperty("mensaje", "¡Doble! Puedes tirar de nuevo");
            } else {
                respuesta.addProperty("mensaje", "Dados tirados. Selecciona ficha para mover");
            }
            
            // Timestamp para sincronización
            respuesta.addProperty("timestamp", System.currentTimeMillis());
            
            return respuesta;
            
        } catch (MotorJuego.NoEsTuTurnoException e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "NO_ES_TU_TURNO");
            respuesta.addProperty("mensaje", e.getMessage());
            return respuesta;
        } catch (Exception e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "ERROR_SERVIDOR");
            respuesta.addProperty("mensaje", "Error al tirar dados: " + e.getMessage());
            e.printStackTrace();
            return respuesta;
        }
    }
    
    /**
     * Valida que sea el turno del jugador especificado.
     */
    private boolean esElTurnoDelJugador(int jugadorId) {
        Partida partida = motor.getPartida();
        if (partida == null) {
            return false;
        }
        
        Jugador jugadorActual = partida.getJugadorActual();
        return jugadorActual != null && jugadorActual.getId() == jugadorId;
    }
    
    /**
     * Obtiene información del turno actual para sincronización.
     */
    public JsonObject getInfoTurnoActual() {
        JsonObject info = new JsonObject();
        
        try {
            Partida partida = motor.getPartida();
            if (partida == null) {
                info.addProperty("error", "NO_HAY_PARTIDA");
                return info;
            }
            
            Jugador jugadorActual = partida.getJugadorActual();
            if (jugadorActual == null) {
                info.addProperty("error", "NO_HAY_TURNO");
                return info;
            }
            
            info.addProperty("jugadorActualId", jugadorActual.getId());
            info.addProperty("jugadorActualNombre", jugadorActual.getNombre());
            info.addProperty("doblesConsecutivos", motor.getContadorDobles(jugadorActual.getId()));
            
            return info;
            
        } catch (Exception e) {
            info.addProperty("error", "ERROR_OBTENER_INFO");
            return info;
        }
    }
}