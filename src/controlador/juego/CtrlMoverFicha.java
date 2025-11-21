package controlador.juego;

import modelo.partida.MotorJuego;
import modelo.partida.MotorJuego.ResultadoMovimiento;
import modelo.partida.MotorJuego.ResultadoSacar;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.Jugador.Jugador;
import modelo.Ficha.Ficha;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Controlador para mover fichas en arquitectura híbrida P2P + Servidor.
 * 
 * El servidor valida y ejecuta el movimiento oficial, maneja bonus
 * (20 por captura, 10 por meta) y hace broadcast a todos los peers.
 */
public class CtrlMoverFicha {
    
    private final MotorJuego motor;
    
    public CtrlMoverFicha(MotorJuego motor) {
        this.motor = motor;
    }
    
    /**
     * Procesa el movimiento de una ficha con ambos dados (suma).
     * 
     * @param jugadorId ID del jugador
     * @param fichaId ID de la ficha (1-4)
     * @param dado1 Valor del primer dado
     * @param dado2 Valor del segundo dado
     * @return JsonObject con resultado para broadcast
     */
    public JsonObject moverFicha(int jugadorId, int fichaId, int dado1, int dado2) {
        JsonObject respuesta = new JsonObject();
        
        try {
            // Validar que sea el turno del jugador
            if (!esElTurnoDelJugador(jugadorId)) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_ES_TU_TURNO");
                respuesta.addProperty("mensaje", "No es tu turno");
                return respuesta;
            }
            
            // Validar partida
            Partida partida = motor.getPartida();
            if (partida.getEstado() != EstadoPartida.EN_PROGRESO) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "PARTIDA_NO_INICIADA");
                return respuesta;
            }
            
            // Obtener la ficha antes del movimiento
            Jugador jugador = partida.getJugadorPorId(jugadorId);
            if (jugador == null) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "JUGADOR_NO_ENCONTRADO");
                return respuesta;
            }
            
            Ficha ficha = jugador.getFichaPorId(fichaId);
            if (ficha == null) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "FICHA_NO_ENCONTRADA");
                return respuesta;
            }
            
            int posicionOriginal = ficha.getCasillaActual() != null ? 
                ficha.getCasillaActual().getIndice() : -1;
            
            // Ejecutar movimiento (el motor maneja lógica completa)
            ResultadoMovimiento resultado = motor.moverFicha(jugadorId, fichaId, dado1, dado2);
            
            if (!resultado.movimientoExitoso) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "MOVIMIENTO_INVALIDO");
                respuesta.addProperty("mensaje", "No se pudo realizar el movimiento");
                return respuesta;
            }
            
            // Construir respuesta exitosa
            respuesta.addProperty("exito", true);
            respuesta.addProperty("tipo", "FICHA_MOVIDA");
            respuesta.addProperty("jugadorId", jugadorId);
            respuesta.addProperty("fichaId", fichaId);
            respuesta.addProperty("dado1", dado1);
            respuesta.addProperty("dado2", dado2);
            respuesta.addProperty("posicionOrigen", posicionOriginal);
            respuesta.addProperty("posicionDestino", resultado.casillaLlegada);
            
            // Información de captura
            if (resultado.capturaRealizada) {
                respuesta.addProperty("huboCaptura", true);
                respuesta.addProperty("fichaCapturadaId", resultado.fichaCapturadaId);
                respuesta.addProperty("jugadorCapturadoId", resultado.jugadorCapturadoId);
                respuesta.addProperty("bonusCaptura", resultado.bonusGanado);
                respuesta.addProperty("bonusTotal", resultado.bonusTotal);
                respuesta.addProperty("mensaje", "¡Captura! +" + resultado.bonusGanado + " casillas de bonus");
            }
            
            // Información de llegada a meta
            if (resultado.llegadaMeta) {
                respuesta.addProperty("llegoAMeta", true);
                respuesta.addProperty("bonusMeta", resultado.bonusPuntosMeta);
                respuesta.addProperty("puntosTotal", resultado.puntosTotal);
                respuesta.addProperty("mensaje", "¡Ficha en meta! +" + resultado.bonusPuntosMeta + " puntos");
            }
            
            // El turno termina después de mover
            partida.avanzarTurno();
            Jugador siguienteJugador = partida.getJugadorActual();
            respuesta.addProperty("turnoTerminado", true);
            respuesta.addProperty("siguienteTurno", siguienteJugador.getId());
            
            // Timestamp para sincronización
            respuesta.addProperty("timestamp", System.currentTimeMillis());
            
            return respuesta;
            
        } catch (MotorJuego.MovimientoInvalidoException e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "MOVIMIENTO_INVALIDO");
            respuesta.addProperty("mensaje", e.getMessage());
            return respuesta;
        } catch (Exception e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "ERROR_SERVIDOR");
            respuesta.addProperty("mensaje", "Error al mover ficha: " + e.getMessage());
            e.printStackTrace();
            return respuesta;
        }
    }
    
    /**
     * Saca una ficha de casa con la regla del 5.
     * 
     * @param jugadorId ID del jugador
     * @param fichaId ID de la ficha a sacar
     * @param dado1 Valor del primer dado
     * @param dado2 Valor del segundo dado
     * @return JsonObject con resultado para broadcast
     */
    public JsonObject sacarFicha(int jugadorId, int fichaId, int dado1, int dado2) {
        JsonObject respuesta = new JsonObject();
        
        try {
            if (!esElTurnoDelJugador(jugadorId)) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_ES_TU_TURNO");
                return respuesta;
            }
            
            // Ejecutar sacar ficha
            ResultadoSacar resultado = motor.sacarFichaDeCasa(jugadorId, fichaId, dado1, dado2);
            
            if (!resultado.movimientoExitoso) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_SE_PUDO_SACAR");
                return respuesta;
            }
            
            // Construir respuesta
            respuesta.addProperty("exito", true);
            respuesta.addProperty("tipo", "FICHA_SACADA");
            respuesta.addProperty("jugadorId", jugadorId);
            respuesta.addProperty("fichaId", fichaId);
            respuesta.addProperty("casillaLlegada", resultado.casillaLlegada);
            respuesta.addProperty("mensaje", resultado.mensajeExtra);
            
            // Información de dados usados
            respuesta.addProperty("dado1Usado", resultado.dado1Usado);
            respuesta.addProperty("dado2Usado", resultado.dado2Usado);
            
            if (resultado.hayDadoDisponible()) {
                respuesta.addProperty("dadoDisponible", resultado.getDadoDisponible());
                respuesta.addProperty("mensaje", resultado.mensajeExtra);
            }
            
            // Captura al sacar
            if (resultado.capturaRealizada) {
                respuesta.addProperty("huboCaptura", true);
                respuesta.addProperty("bonusCaptura", resultado.bonusGanado);
            }
            
            respuesta.addProperty("timestamp", System.currentTimeMillis());
            
            return respuesta;
            
        } catch (Exception e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "ERROR_SACAR_FICHA");
            respuesta.addProperty("mensaje", e.getMessage());
            return respuesta;
        }
    }
    
    /**
     * Aplicar bonus a una ficha específica.
     * 
     * @param jugadorId ID del jugador
     * @param fichaId ID de la ficha que recibirá el bonus
     * @param pasos Cantidad de pasos a aplicar
     * @return JsonObject con resultado para broadcast
     */
    public JsonObject aplicarBonus(int jugadorId, int fichaId, int pasos) {
        JsonObject respuesta = new JsonObject();
        
        try {
            if (!esElTurnoDelJugador(jugadorId)) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_ES_TU_TURNO");
                return respuesta;
            }
            
            // Ejecutar aplicación de bonus
            ResultadoMovimiento resultado = motor.usarBonus(jugadorId, fichaId, pasos);
            
            if (!resultado.movimientoExitoso) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_SE_PUDO_APLICAR_BONUS");
                return respuesta;
            }
            
            // Construir respuesta
            respuesta.addProperty("exito", true);
            respuesta.addProperty("tipo", "BONUS_APLICADO");
            respuesta.addProperty("jugadorId", jugadorId);
            respuesta.addProperty("fichaId", fichaId);
            respuesta.addProperty("pasos", pasos);
            respuesta.addProperty("posicionFinal", resultado.casillaLlegada);
            respuesta.addProperty("bonusRestante", resultado.bonusRestante);
            respuesta.addProperty("timestamp", System.currentTimeMillis());
            
            return respuesta;
            
        } catch (Exception e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "ERROR_APLICAR_BONUS");
            respuesta.addProperty("mensaje", e.getMessage());
            return respuesta;
        }
    }
    
    /**
     * Saltar turno (no mover ninguna ficha).
     */
    public JsonObject saltarTurno(int jugadorId) {
        JsonObject respuesta = new JsonObject();
        
        try {
            if (!esElTurnoDelJugador(jugadorId)) {
                respuesta.addProperty("exito", false);
                respuesta.addProperty("error", "NO_ES_TU_TURNO");
                return respuesta;
            }
            
            // Pasar al siguiente turno
            Partida partida = motor.getPartida();
            partida.avanzarTurno();
            
            Jugador siguienteJugador = partida.getJugadorActual();
            
            respuesta.addProperty("exito", true);
            respuesta.addProperty("tipo", "TURNO_SALTADO");
            respuesta.addProperty("jugadorId", jugadorId);
            respuesta.addProperty("siguienteTurno", siguienteJugador.getId());
            respuesta.addProperty("mensaje", "Turno saltado");
            respuesta.addProperty("timestamp", System.currentTimeMillis());
            
            return respuesta;
            
        } catch (Exception e) {
            respuesta.addProperty("exito", false);
            respuesta.addProperty("error", "ERROR_SALTAR_TURNO");
            return respuesta;
        }
    }
    
    /**
     * Obtiene las fichas movibles del jugador actual.
     */
    public JsonObject getFichasMovibles(int jugadorId) {
        JsonObject respuesta = new JsonObject();
        
        try {
            Partida partida = motor.getPartida();
            Jugador jugador = partida.getJugadorPorId(jugadorId);
            if (jugador == null) {
                respuesta.addProperty("error", "JUGADOR_NO_ENCONTRADO");
                return respuesta;
            }
            
            JsonArray fichasArray = new JsonArray();
            for (Ficha ficha : jugador.getFichas()) {
                JsonObject fichaObj = new JsonObject();
                fichaObj.addProperty("id", ficha.getId());
                int posicion = ficha.getCasillaActual() != null ? 
                    ficha.getCasillaActual().getIndice() : -1;
                fichaObj.addProperty("posicion", posicion);
                fichaObj.addProperty("estado", ficha.getEstado().toString());
                fichasArray.add(fichaObj);
            }
            
            respuesta.add("fichas", fichasArray);
            return respuesta;
            
        } catch (Exception e) {
            respuesta.addProperty("error", "ERROR_OBTENER_FICHAS");
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
}