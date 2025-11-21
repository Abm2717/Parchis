package controlador.juego;

import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.partida.MotorJuego;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * ✅ COMPATIBLE CON TU DISPATCHER
 */
public class CtrlMoverFicha {
    
    private final MotorJuego motor;
    
    public CtrlMoverFicha(MotorJuego motor) {
        this.motor = motor;
    }
    
    public JsonObject moverFicha(int jugadorId, int fichaId, int dado1, int dado2) {
        try {
            Partida partida = motor.getPartida();
            
            if (partida.getEstado() != EstadoPartida.EN_PROGRESO) {
                return crearErrorJson("La partida no esta en progreso");
            }
            
            if (!partida.esTurnoDeJugador(jugadorId)) {
                return crearErrorJson("No es tu turno");
            }
            
            MotorJuego.ResultadoMovimiento resultado = motor.moverFicha(jugadorId, fichaId, dado1, dado2);
            
            partida.avanzarTurno();
            
            JsonObject respuesta = crearRespuestaMovimiento(resultado);
            return respuesta;
            
        } catch (MotorJuego.MovimientoInvalidoException e) {
            return crearErrorJson("Movimiento invalido: " + e.getMessage());
        } catch (Exception e) {
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    public JsonObject sacarFicha(int jugadorId, int fichaId, int dado1, int dado2) {
        try {
            MotorJuego.ResultadoSacar resultado = motor.sacarFichaDeCasa(jugadorId, fichaId, dado1, dado2);
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "ficha_sacada");
            respuesta.addProperty("exito", true);
            respuesta.addProperty("fichaId", fichaId);
            respuesta.addProperty("casillaLlegada", resultado.casillaLlegada);
            
            if (resultado.hayDadoDisponible()) {
                respuesta.addProperty("dadoDisponible", resultado.getDadoDisponible());
            }
            
            return respuesta;
            
        } catch (Exception e) {
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    public JsonObject aplicarBonus(int jugadorId, int fichaId, int pasos) {
        try {
            MotorJuego.ResultadoMovimiento resultado = motor.usarBonus(jugadorId, fichaId, pasos);
            return crearRespuestaMovimiento(resultado);
        } catch (Exception e) {
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    public JsonObject saltarTurno(int jugadorId) {
        try {
            Partida partida = motor.getPartida();
            
            if (!partida.esTurnoDeJugador(jugadorId)) {
                return crearErrorJson("No es tu turno");
            }
            
            partida.avanzarTurno();
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "turno_saltado");
            respuesta.addProperty("exito", true);
            
            return respuesta;
            
        } catch (Exception e) {
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    public JsonObject getFichasMovibles(int jugadorId) {
        try {
            Partida partida = motor.getPartida();
            Jugador jugador = partida.getJugadorPorId(jugadorId);
            
            if (jugador == null) {
                return crearErrorJson("Jugador no encontrado");
            }
            
            JsonObject respuesta = new JsonObject();
            respuesta.addProperty("tipo", "fichas_movibles");
            respuesta.addProperty("exito", true);
            
            JsonArray fichas = new JsonArray();
            for (modelo.Ficha.Ficha ficha : jugador.getFichas()) {
                JsonObject fichaObj = new JsonObject();
                fichaObj.addProperty("id", ficha.getId());
                fichaObj.addProperty("enCasa", ficha.estaEnCasa());
                fichaObj.addProperty("enMeta", ficha.estaEnMeta());
                if (ficha.getCasillaActual() != null) {
                    fichaObj.addProperty("casilla", ficha.getCasillaActual().getIndice());
                }
                fichas.add(fichaObj);
            }
            
            respuesta.add("fichas", fichas);
            
            return respuesta;
            
        } catch (Exception e) {
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    private JsonObject crearRespuestaMovimiento(MotorJuego.ResultadoMovimiento resultado) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "movimiento_exitoso");
        respuesta.addProperty("exito", true);
        respuesta.addProperty("desde", resultado.casillaSalida);
        respuesta.addProperty("hasta", resultado.casillaLlegada);
        
        if (resultado.capturaRealizada) {
            respuesta.addProperty("captura", true);
            respuesta.addProperty("bonusGanado", resultado.bonusGanado);
        }
        
        if (resultado.llegadaMeta) {
            respuesta.addProperty("meta", true);
            respuesta.addProperty("puntos", resultado.bonusPuntosMeta);
        }
        
        return respuesta;
    }
    
    private JsonObject crearErrorJson(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error;
    }
}