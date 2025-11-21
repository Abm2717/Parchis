package controlador.juego;

import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.partida.MotorJuego;
import modelo.servicios.GestorMotores;
import com.google.gson.JsonObject;
import modelo.Ficha.Ficha;

/**
 * ✅ COMPATIBLE CON TU DISPATCHER
 * Constructor recibe MotorJuego
 */
public class CtrlTirarDado {
    
    private final MotorJuego motor;
    private final GestorMotores gestorMotores;
    
    public CtrlTirarDado(MotorJuego motor) {
        this.motor = motor;
        this.gestorMotores = GestorMotores.getInstancia();
    }
    
    /**
     * ✅ Firma compatible con Dispatcher: tirarDados(int jugadorId)
     */
    public JsonObject tirarDados(int jugadorId) {
        try {
            Partida partida = motor.getPartida();
            
            if (partida.getEstado() != EstadoPartida.EN_PROGRESO) {
                return crearErrorJson("La partida no esta en progreso");
            }
            
            if (!partida.esTurnoDeJugador(jugadorId)) {
                return crearErrorJson("No es tu turno");
            }
            
            Jugador jugador = partida.getJugadorPorId(jugadorId);
            if (jugador == null) {
                return crearErrorJson("Jugador no encontrado");
            }
            
            // ✅ Tirar dados
            MotorJuego.ResultadoDados resultado = motor.tirarDados(jugadorId);
            
            System.out.println("[DADOS] " + jugador.getNombre() + " tiro: " + 
                             resultado.dado1 + " y " + resultado.dado2);
            
            // ✅ Manejo automático según dados
            boolean puedeJugar = true;
            int dadoDisponible = 0;
            
            if (resultado.esDoble) {
                if (resultado.dado1 == 5 && resultado.dado2 == 5) {
                    puedeJugar = intentarSacarDosFichas(jugador, motor);
                }
            } else {
                ResultadoAuto resAuto = manejarAutomatico(jugador, motor, resultado.dado1, resultado.dado2);
                puedeJugar = resAuto.puedeJugar;
                dadoDisponible = resAuto.dadoDisponible;
            }
            
            JsonObject respuesta = crearRespuesta(resultado);
            respuesta.addProperty("puedeJugar", puedeJugar);
            
            if (resultado.esDoble) {
                respuesta.addProperty("debeVolverATirar", true);
            }
            
            if (dadoDisponible > 0) {
                respuesta.addProperty("dadoDisponible", dadoDisponible);
            }
            
            return respuesta;
            
        } catch (MotorJuego.NoEsTuTurnoException e) {
            return crearErrorJson("No es tu turno");
        } catch (Exception e) {
            System.err.println("Error tirando dados: " + e.getMessage());
            return crearErrorJson("Error: " + e.getMessage());
        }
    }
    
    private static class ResultadoAuto {
        boolean puedeJugar;
        int dadoDisponible;
        ResultadoAuto(boolean p, int d) { puedeJugar = p; dadoDisponible = d; }
    }
    
    private ResultadoAuto manejarAutomatico(Jugador jugador, MotorJuego motor, int dado1, int dado2) {
        boolean dado1Es5 = (dado1 == 5);
        boolean dado2Es5 = (dado2 == 5);
        boolean sumaEs5 = (dado1 + dado2 == 5);
        boolean puedesSacar = dado1Es5 || dado2Es5 || sumaEs5;
        
        boolean tieneFichasEnCasa = jugador.getFichas().stream().anyMatch(f -> f.estaEnCasa());
        boolean tieneFichasEnTablero = jugador.getFichas().stream()
            .anyMatch(f -> !f.estaEnCasa() && !f.estaEnMeta());
        
        if (tieneFichasEnCasa && puedesSacar) {
            return intentarSacar(jugador, motor, dado1, dado2);
        }
        
        if (tieneFichasEnTablero) {
            return new ResultadoAuto(true, 0);
        }
        
        System.out.println("[AUTO] " + jugador.getNombre() + " no puede jugar. Pasando turno...");
        motor.getPartida().avanzarTurno();
        return new ResultadoAuto(false, 0);
    }
    
    private ResultadoAuto intentarSacar(Jugador jugador, MotorJuego motor, int dado1, int dado2) {
        try {
            Ficha fichaEnCasa = jugador.getFichas().stream()
                .filter(f -> f.estaEnCasa())
                .findFirst()
                .orElse(null);
            
            if (fichaEnCasa == null) {
                return new ResultadoAuto(true, 0);
            }
            
            MotorJuego.ResultadoSacar resultado = motor.sacarFichaDeCasa(
                jugador.getId(),
                fichaEnCasa.getId(),
                dado1,
                dado2
            );
            
            if (resultado.hayDadoDisponible()) {
                return new ResultadoAuto(true, resultado.getDadoDisponible());
            } else {
                motor.getPartida().avanzarTurno();
                return new ResultadoAuto(false, 0);
            }
            
        } catch (Exception e) {
            return new ResultadoAuto(true, 0);
        }
    }
    
    private boolean intentarSacarDosFichas(Jugador jugador, MotorJuego motor) {
        try {
            Ficha ficha1 = jugador.getFichas().stream().filter(f -> f.estaEnCasa()).findFirst().orElse(null);
            if (ficha1 == null) return true;
            
            motor.sacarFichaDeCasa(jugador.getId(), ficha1.getId(), 5, 5);
            
            Ficha ficha2 = jugador.getFichas().stream().filter(f -> f.estaEnCasa()).findFirst().orElse(null);
            if (ficha2 != null) {
                motor.sacarFichaDeCasa(jugador.getId(), ficha2.getId(), 5, 0);
            }
            
            return false;
        } catch (Exception e) {
            return true;
        }
    }
    
    private JsonObject crearRespuesta(MotorJuego.ResultadoDados resultado) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "resultado_dados");
        respuesta.addProperty("exito", true);
        
        JsonObject dados = new JsonObject();
        dados.addProperty("dado1", resultado.dado1);
        dados.addProperty("dado2", resultado.dado2);
        dados.addProperty("suma", resultado.getSuma());
        dados.addProperty("esDoble", resultado.esDoble);
        
        respuesta.add("dados", dados);
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