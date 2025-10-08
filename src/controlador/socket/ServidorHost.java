package controlador.socket;

import java.io.*;
import java.net.*;
import java.util.*;
import modelo.Ficha.Ficha;
import modelo.Jugador.ColorJugador;
import modelo.Jugador.Jugador;
import modelo.Tablero.Casilla;
import modelo.Tablero.Tablero;
import modelo.partida.Partida;
import controlador.MotorJuego;

public class ServidorHost {
    private int port;
    private Partida partida;
    private MotorJuego motor;
    private List<PrintWriter> clientes = new ArrayList<>();

    public ServidorHost(int port){
        this.port = port;
        this.partida = new Partida(1, "ABC123", 2);
        this.motor = new MotorJuego(partida);
        partida.getTablero().inicializarCasillas();
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor escuchando en puerto " + port);

        while(partida.getJugadores().size() < 2) { // Solo 2 jugadores
            Socket socket = serverSocket.accept();
            new Thread(new ClienteHandler(socket)).start();
        }
    }

    private class ClienteHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private Jugador jugador;

        public ClienteHandler(Socket socket){
            this.socket = socket;
        }

        @Override
public void run(){
    try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        clientes.add(out);

        // Pedir nombre y color
        out.println("Ingrese su nombre:");
        String nombre = in.readLine();

        out.println("Ingrese su color (ROJO o AZUL):");
        ColorJugador color = ColorJugador.valueOf(in.readLine().toUpperCase());

        jugador = new Jugador(partida.getJugadores().size()+1, nombre, color, "avatar.png");
        for(int i=1;i<=4;i++) jugador.agregarFicha(new Ficha(i, color));
        partida.agregarJugador(jugador);

        // Colocar fichas en casilla inicial
        int inicioIndice = (color==ColorJugador.ROJO)?1:18;
        Casilla inicio = partida.getTablero().getCasillaPorIndice(inicioIndice);
        for(Ficha f : jugador.getFichas()){
            inicio.agregarFicha(f);
            f.setCasillaActual(inicio);
        }

        enviarATodos("Jugador "+nombre+" se ha unido con color "+color);

        // Escuchar comandos del jugador
        String line;
        while((line = in.readLine()) != null){
            System.out.println("Comando recibido de " + nombre + ": " + line); // Debug
            
            if(line.startsWith("MOVE")){
                String[] parts = line.split(" ");
                int fichaId = Integer.parseInt(parts[1]);
                int avance = Integer.parseInt(parts[2]);
                
                // CORRECCIÓN: Buscar la ficha del jugador actual, no global
                Ficha fichaAMover = null;
                for(Ficha f : jugador.getFichas()){
                    if(f.getId() == fichaId){
                        fichaAMover = f;
                        break;
                    }
                }
                
                if(fichaAMover != null){
                    System.out.println("Moviendo ficha " + fichaId + " de " + nombre); // Debug
                    motor.moverFicha(fichaAMover, avance);
                    enviarEstadoATodos();
                } else {
                    out.println("❌ Ficha " + fichaId + " no encontrada");
                }
            } 
            else if(line.equals("ESTADO")){
                // CORRECCIÓN: Manejar comando ESTADO
                enviarEstadoATodos();
            }
        }

    } catch(Exception e){
        System.out.println("Jugador desconectado: "+(jugador!=null?jugador.getNombre():"Desconocido"));
        e.printStackTrace(); // Para ver errores
    }
}
        private void enviarATodos(String msg){
            for(PrintWriter p : clientes) p.println(msg);
        }

        private void enviarEstadoATodos(){
            StringBuilder sb = new StringBuilder();
            sb.append("--- Estado del Tablero ---\n");
            for(Casilla c : partida.getTablero().getCasillas()){
                sb.append("Casilla ").append(c.getIndice())
                  .append(" [").append(c.getTipo()).append("]: ");
                if(c.getFichas().isEmpty()) sb.append("VACIA");
                else for(Ficha f : c.getFichas()) sb.append(f.getColor()).append(" ");
                sb.append("\n");
            }
            sb.append("--------------------------");
            enviarATodos(sb.toString());
        }
    }

    public static void main(String[] args) throws IOException{
        ServidorHost servidor = new ServidorHost(5000);
        servidor.start();
    }
}
   