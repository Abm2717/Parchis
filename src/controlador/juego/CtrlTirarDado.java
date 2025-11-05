
package controlador.juego;

import controlador.servidor.ClienteHandler;
import modelo.Jugador.Jugador;
import modelo.partida.Partida;
import modelo.partida.EstadoPartida;
import modelo.partida.MotorJuego;
import modelo.servicios.PersistenciaServicio;
import modelo.servicios.SalaServicio;
import com.google.gson.JsonObject;
import java.util.Optional;
import modelo.servicios.GestorMotores;
import vista.VistaServidor;

/**
 * Controlador para manejar la tirada de dados.
 * 
 */
public class CtrlTirarDado {
    
    private final PersistenciaServicio persistencia;
    private final SalaServicio salaServicio;
    
    public CtrlTirarDado() {
        this.persistencia = PersistenciaServicio.getInstancia();
        this.salaServicio = SalaServicio.getInstancia();
    }
    
    
    /**
     * Ejecuta la tirada de dados para un jugador.
     * 
     * @param cliente ClienteHandler del jugador
     * @param datos Datos adicionales del mensaje (puede ser null)
     * @return Respuesta JSON
     */
    public String ejecutar(ClienteHandler cliente, JsonObject datos) {
        try {
            // Validar jugador
            Jugador jugador = cliente.getJugador();
            if (jugador == null) {
                return crearError("Debes registrarte primero");
            }
            
            // Obtener partida del jugador
            Optional<Partida> partidaOpt = persistencia.obtenerPartidaDeJugador(jugador.getId());
            if (!partidaOpt.isPresent()) {
                return crearError("No estas en ninguna partida");
            }
            
            Partida partida = partidaOpt.get();
            
            // Validar estado de la partida
            if (partida.getEstado() != EstadoPartida.EN_PROGRESO) {
                return crearError("La partida no ha iniciado");
            }
            
            // Validar turno
            if (!partida.esTurnoDeJugador(jugador.getId())) {
                Jugador jugadorTurno = partida.getJugadorActual();
                String nombreTurno = jugadorTurno != null ? jugadorTurno.getNombre() : "desconocido";
                return crearError("No es tu turno. Turno de: " + nombreTurno);
            }
            
            // Obtener motor de juego (asumiendo que esta en la partida o crear uno)
            MotorJuego motor = obtenerMotorJuego(partida);
            
            // Tirar dados
            MotorJuego.ResultadoDados resultado = motor.tirarDados(jugador.getId());
            
            VistaServidor.mostrarTiradaDados(
                jugador, 
                resultado.dado1, 
                resultado.dado2, 
                resultado.esDoble
            );

            if (resultado.fichaPerdida) {
                VistaServidor.mostrarFichaPerdida(jugador);
            }

            if (resultado.bloqueoRoto) {
                VistaServidor.mostrarBloqueoRoto(jugador);
            }
            
            // Notificar a todos los jugadores
            notificarResultadoDados(partida, jugador, resultado, cliente);
            
            // Crear respuesta para el jugador que tiro
            JsonObject respuesta = crearRespuestaTirada(resultado);
            
            // Si saco doble, puede volver a tirar
            if (resultado.esDoble && !resultado.fichaPerdida) {
                respuesta.addProperty("puedeVolverATirar", true);
                respuesta.addProperty("mensaje", "¡Doble! Puedes tirar de nuevo");
            }
            
            // Si perdio ficha por 3 dobles
            if (resultado.fichaPerdida) {
                respuesta.addProperty("fichaPerdida", true);
                respuesta.addProperty("mensaje", "3 dobles seguidos - Pierdes una ficha");
                notificarFichaPerdida(partida, jugador, cliente);
            }
            
            // Si rompio bloqueo
            if (resultado.bloqueoRoto) {
                respuesta.addProperty("bloqueoRoto", true);
                notificarBloqueoRoto(partida, jugador, cliente);
            }
            
            return respuesta.toString();
            
        } catch (MotorJuego.NoEsTuTurnoException e) {
            return crearError("No es tu turno");
        } catch (MotorJuego.JuegoException e) {
            return crearError("Error en el juego: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error tirando dados: " + e.getMessage());
            e.printStackTrace();
            return crearError("Error interno: " + e.getMessage());
        }
    }

    
    /**
     * Crea la respuesta JSON para el resultado de tirada.
     */
    private JsonObject crearRespuestaTirada(MotorJuego.ResultadoDados resultado) {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("tipo", "resultado_dados");
        respuesta.addProperty("exito", true);
        
        // Datos de los dados
        JsonObject dados = new JsonObject();
        dados.addProperty("dado1", resultado.dado1);
        dados.addProperty("dado2", resultado.dado2);
        dados.addProperty("suma", resultado.getSuma());
        dados.addProperty("esDoble", resultado.esDoble);
        dados.addProperty("contadorDobles", resultado.contadorDobles);
        
        respuesta.add("dados", dados);
        
        // Estado adicional
        respuesta.addProperty("bloqueoRoto", resultado.bloqueoRoto);
        respuesta.addProperty("fichaPerdida", resultado.fichaPerdida);
        
        // Mensaje informativo
        String mensaje = "Tiraste " + resultado.dado1 + " y " + resultado.dado2;
        if (resultado.esDoble) {
            mensaje += " - ¡Doble!";
        }
        respuesta.addProperty("mensaje", mensaje);
        
        return respuesta;
    }
    
    
    /**
     * Notifica el resultado de los dados a todos los jugadores de la partida.
     */
    private void notificarResultadoDados(Partida partida, Jugador jugador, 
                                     MotorJuego.ResultadoDados resultado, 
                                     ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "jugador_tiro_dados");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("dado1", resultado.dado1);
        notificacion.addProperty("dado2", resultado.dado2);
        notificacion.addProperty("suma", resultado.getSuma());
        notificacion.addProperty("esDoble", resultado.esDoble);

        // Broadcast a otros jugadores
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            cliente.getSessionId()
        );
    }
    
    /**
     * Notifica que se perdio una ficha por 3 dobles.
     */
    private void notificarFichaPerdida(Partida partida, Jugador jugador, ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "ficha_perdida");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("razon", "3 dobles consecutivos");
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            null // Enviar a todos incluyendo el jugador
        );
    }
    
    /**
     * Notifica que se rompio un bloqueo.
     */
    private void notificarBloqueoRoto(Partida partida, Jugador jugador, ClienteHandler cliente) {
        JsonObject notificacion = new JsonObject();
        notificacion.addProperty("tipo", "bloqueo_roto");
        notificacion.addProperty("jugadorId", jugador.getId());
        notificacion.addProperty("jugadorNombre", jugador.getNombre());
        notificacion.addProperty("mensaje", jugador.getNombre() + " rompio su bloqueo");
        
        cliente.getServidor().broadcastAPartida(
            partida.getId(), 
            notificacion.toString(), 
            null
        );
    }
    

    
    /**
     * Obtiene o crea el MotorJuego para una partida.
     */
    private MotorJuego obtenerMotorJuego(Partida partida) {
        GestorMotores gestorMotores = GestorMotores.getInstancia();
        return gestorMotores.obtenerMotor(partida);
    }
    
    /**
     * Crea una respuesta de error.
     */
    private String crearError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo", "error");
        error.addProperty("exito", false);
        error.addProperty("mensaje", mensaje);
        return error.toString();
    }
}