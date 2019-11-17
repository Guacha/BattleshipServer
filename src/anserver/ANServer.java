/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package anserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Guacha
 */
public class ANServer {
    /** 
    **ServerSocket: Socket que recibe conexiones foráneas
     */
    private ServerSocket serverSocket;
    
    /**
     * PlayerSocket: Socket Temporal que maneja la conexión con el jugador
     */
    private Socket playerSocket;
    
    /**
     * Vector que será de dos posiciones que contiene a los dos jugadores
     */
    private Jugador[] jugadores;
    
    boolean gameRunning = true;
    
    /**
     * Hilo que se encarga de obtener los dos jugadores en el vector.<p>
     * Busca jugadores por el ServerSocket hasta que el vector esté lleno
     */
    public Thread findPlayersThread = new Thread(new Runnable() {
        @Override
        public void run() {
            gameRunning = true;
            System.out.println("Buscando jugadores!");
            int jugadoresEnSala = 0;
            while (jugadoresEnSala < 2) {
                try {
                    playerSocket = serverSocket.accept();
                    DataInputStream input = new DataInputStream(playerSocket.getInputStream());     //Genera el canal de entrada para el jugador
                    DataOutputStream output = new DataOutputStream(playerSocket.getOutputStream()); //Genera el canal de salida para el jugador
                    String nombre = input.readUTF();
                    jugadores[jugadoresEnSala] = new Jugador(playerSocket, input, output, nombre, ANServer.this);      //Crea un nuevo jugador, y lo asigna a la posición correspondiente del vector
                    jugadoresEnSala++;                                                                                       //El primer jugador se denomina "Lider de sala", y es el que determina cuando se inicia la partida
                    System.out.println("Jugador añadido: " + nombre);
                } catch (IOException ex) {
                    Logger.getLogger(ANServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            for (Jugador jugador : jugadores) {
                if (jugador != null) {
                    Thread t = new Thread(jugador);
                    t.start();
                }
            }
            while(!ANServer.this.playersReady()) {
                
            }
            jugadores[0].iniciarTurno();
        }
    });
    

    public ANServer() {
        try {
            
            serverSocket = new ServerSocket(14999);
            jugadores = new Jugador[2];
            InetAddress address = InetAddress.getLocalHost();
            System.out.println("SERVIDOR INICIADO EXITOSAMENTE EN: " + address.getHostAddress());
            findPlayersThread.start();

        } catch (IOException ex) {
            Logger.getLogger(ANServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void cambioTurno(Jugador jugador) {
        if (jugadores[0].equals(jugador)) {
            jugadores[1].iniciarTurno();
        } else {
            jugadores[0].iniciarTurno();
        }
    }
    
    public static void main(String[] args) {
        ANServer s = new ANServer();
        while (true) {
            if (!s.gameRunning) {
                System.out.println("Iniciar servidor nuevo");
                s = new ANServer();
            } else {
                
            }
        }
    }

    void hitQuery(int i, int j, Jugador jugador) {
        if (jugador.equals(jugadores[0])) {
            jugadores[1].sendShot(i,j);
        } else {
            jugadores[0].sendShot(i, j);
        }
    }
    
    void playerCommunication(String message, Jugador emisor) {
        if (jugadores[0].equals(emisor)) {
            jugadores[1].sendMessage(message);
        } else {
            jugadores[0].sendMessage(message);
        }
    }

    private boolean playersReady() {
        boolean retorno = true;
        for (Jugador jugador : jugadores) {
            if (!jugador.isReady()) {
                retorno = false;
            }
        }
        return retorno;
    }
 
    public void derrota(Jugador jugador) {
        gameRunning = false;
        if (jugadores[0].equals(jugador)) {
            jugadores[1].triggerVictoria();
        } else {
            jugadores[0].triggerVictoria();
        }
    }

    void declareAbandono(Jugador aThis) {
        if (gameRunning) {
            if (jugadores[0].equals(aThis)) {
                jugadores[1].triggerDefault();
                jugadores[1].closeSocket();
            } else {
                jugadores[0].triggerDefault();
                jugadores[0].closeSocket();
            }
            System.out.println("Fin de partida: Cerrando Servidor");
            gameRunning = false;
            try {
                System.out.println("Fin de partida: Cerrando socket");
                serverSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(ANServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Hola");
        }
    }

    void searchPlayers() {
        findPlayersThread.run();
    }


}
