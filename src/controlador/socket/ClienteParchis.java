package controlador.socket;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteParchis {
    private String host;
    private int port;

    public ClienteParchis(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        Socket socket = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        Scanner sc = new Scanner(System.in);

        // Hilo para recibir mensajes del servidor
        new Thread(() -> {
            try {
                String line;
                while((line = in.readLine()) != null){
                    System.out.println(line);
                }
            } catch(IOException e){}
        }).start();

        // Enviar mensajes al servidor
        while(true){
            String msg = sc.nextLine();
            out.println(msg);
        }
    }

    public static void main(String[] args) throws IOException {
        ClienteParchis cliente = new ClienteParchis("localhost", 5000);
        cliente.start();
    }
}
