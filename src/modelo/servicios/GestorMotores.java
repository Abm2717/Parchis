
package modelo.servicios;

import modelo.partida.Partida;
import modelo.partida.MotorJuego;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor centralizado de instancias de MotorJuego.
 * 
 * Responsabilidades:
 * - Crear y almacenar motores de juego por partida
 * - Reutilizar motores existentes (mantiene estado)
 * - Limpiar motores de partidas finalizadas
 * 
 */
public class GestorMotores {
    
    private static GestorMotores instancia;
    
    // Mapa de motores: partidaId -> MotorJuego
    private final Map<Integer, MotorJuego> motoresPorPartida;
    
       
    private GestorMotores() {
        this.motoresPorPartida = new ConcurrentHashMap<>();
    }
    
    /**
     * Obtiene la instancia unica del gestor (Singleton).
     */
    public static synchronized GestorMotores getInstancia() {
        if (instancia == null) {
            instancia = new GestorMotores();
        }
        return instancia;
    }
    
    /**
     * Obtiene el motor de juego para una partida.
     * Si no existe, lo crea automaticamente.
     * 
     * @param partida Partida para la cual obtener el motor
     * @return MotorJuego asociado a la partida
     */
    public MotorJuego obtenerMotor(Partida partida) {
        if (partida == null) {
            throw new IllegalArgumentException("La partida no puede ser null");
        }
        
        // Usar computeIfAbsent para crear solo si no existe (thread-safe)
        return motoresPorPartida.computeIfAbsent(
            partida.getId(), 
            id -> {
                System.out.println("Creando nuevo MotorJuego para partida " + id);
                return new MotorJuego(partida);
            }
        );
    }
    
    /**
     * Obtiene un motor por ID de partida.
     * 
     * @param partidaId ID de la partida
     * @return MotorJuego o null si no existe
     */
    public MotorJuego obtenerMotorPorId(int partidaId) {
        return motoresPorPartida.get(partidaId);
    }
    
    /**
     * Verifica si existe un motor para una partida.
     * 
     * @param partidaId ID de la partida
     * @return true si existe un motor, false en caso contrario
     */
    public boolean existeMotor(int partidaId) {
        return motoresPorPartida.containsKey(partidaId);
    }
    
    /**
     * Crea o reemplaza un motor para una partida especifica.
     * util para reiniciar el estado del juego.
     * 
     * @param partida Partida para la cual crear el motor
     * @return Nuevo MotorJuego creado
     */
    public MotorJuego crearNuevoMotor(Partida partida) {
        if (partida == null) {
            throw new IllegalArgumentException("La partida no puede ser null");
        }
        
        System.out.println("Creando/reemplazando MotorJuego para partida " + partida.getId());
        
        MotorJuego nuevoMotor = new MotorJuego(partida);
        motoresPorPartida.put(partida.getId(), nuevoMotor);
        
        return nuevoMotor;
    }
    
    // ============================
    // LIMPIEZA
    // ============================
    
    /**
     * Remueve el motor de una partida.
     * Debe llamarse cuando una partida termina.
     * 
     * @param partidaId ID de la partida
     * @return true si se removio, false si no existia
     */
    public boolean removerMotor(int partidaId) {
        MotorJuego removido = motoresPorPartida.remove(partidaId);
        
        if (removido != null) {
            System.out.println("Motor removido para partida " + partidaId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Limpia todos los motores.
     * util para reiniciar el servidor o en testing.
     */
    public synchronized void limpiarTodos() {
        int cantidad = motoresPorPartida.size();
        motoresPorPartida.clear();
        System.out.println("Limpiados " + cantidad + " motores de juego");
    }
    
    /**
     * Limpia motores de partidas finalizadas.
     * Debe ejecutarse periodicamente para liberar memoria.
     */
    public void limpiarMotoresFinalizados() {
        PersistenciaServicio persistencia = PersistenciaServicio.getInstancia();
        
        motoresPorPartida.entrySet().removeIf(entry -> {
            int partidaId = entry.getKey();
            Partida partida = persistencia.obtenerPartida(partidaId);
            
            // Remover si la partida no existe o esta finalizada
            if (partida == null || 
                partida.getEstado() == modelo.partida.EstadoPartida.FINALIZADA ||
                partida.getEstado() == modelo.partida.EstadoPartida.CANCELADA) {
                
                System.out.println("Limpiando motor de partida finalizada: " + partidaId);
                return true;
            }
            
            return false;
        });
    }
    
  
    /**
     * Obtiene el numero de motores activos.
     */
    public int getTotalMotores() {
        return motoresPorPartida.size();
    }
    
    /**
     * Obtiene informacion de un motor especifico.
     */
    public String getInfoMotor(int partidaId) {
        MotorJuego motor = motoresPorPartida.get(partidaId);
        if (motor == null) {
            return "Motor no encontrado para partida " + partidaId;
        }
        
        // Aqui podrias agregar mas detalles del motor si MotorJuego tiene metodos de info
        return String.format("Motor[partidaId=%d, existe=true]", partidaId);
    }
    
    /**
     * Lista todos los IDs de partidas con motor activo.
     */
    public java.util.Set<Integer> getPartidasConMotor() {
        return new java.util.HashSet<>(motoresPorPartida.keySet());
    }
    
 
    /**
     * Imprime estadisticas del gestor.
     */
    public void imprimirEstadisticas() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   ESTADISTICAS GESTOR DE MOTORES      ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("Motores activos:     " + motoresPorPartida.size());
        System.out.println("Partidas con motor:  " + motoresPorPartida.keySet());
        System.out.println();
    }
    
    @Override
    public String toString() {
        return String.format("GestorMotores[motores=%d]", motoresPorPartida.size());
    }
}
