/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package anserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Guacha
 */
public class Jugador implements Runnable{
    
    private final Socket playerSocket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final String name;
    private boolean isReady;
    private boolean inGame;
    private final ANServer server;

    public Jugador(Socket playerSocket, DataInputStream input, DataOutputStream output, String name, ANServer server) {
        this.playerSocket = playerSocket;
        this.input = input;
        this.output = output;
        this.name = name;
        this.isReady = false;
        this.server = server;
        inGame = true;
    }
    
    @Override
    public void run() {
        String respuestaJugador;
        while (inGame) { 
            try {
            respuestaJugador = input.readUTF(); //Leer la respuesta del jugador
            Jugador.this.interpretarRespuesta(respuestaJugador);
            } catch (IOException ex) {
                if (server.gameRunning) {
                    System.out.println(name + " ha abandonado la partida");
                    Jugador.this.abandono();
                }
            }
        }
        
    }
    
    /**
     * Subrutina que interpreta la respuesta recibida por el cliente remoto y 
     * realiza una función correspondiente con base en la cadena recibida.
     * @param respuesta La respuesta del cliente remoto
     */
    private void interpretarRespuesta(String respuesta) {
        if (respuesta.startsWith("CLIENTE#")) {
            String comando = respuesta.substring(8);
            System.out.println(name + ": " + comando);
            if (comando.startsWith("DISPARO$")) {
                int i,j;
                i = Integer.parseInt(comando.substring(8,9));
                j = Integer.parseInt(comando.substring(10));
                hitQuery(i, j);
            } else if (comando.startsWith("CONFIRMHIT$")) {
                int i = Integer.parseInt(comando.substring(11,12));
                int j = Integer.parseInt(comando.substring(13));
                playerHit(true, i, j);
            } else if (comando.startsWith("DENYHIT$")) {
                int i = Integer.parseInt(comando.substring(8,9));
                int j = Integer.parseInt(comando.substring(10));
                playerHit(false, i, j);
            } else if (comando.startsWith("ISREADY")) {
                isReady = true;
            } else if (comando.contains("FINTURNO")) {
                server.cambioTurno(this);
            } else if(comando.equals("DERROTA")) {
                inGame = false;
                server.derrota(this);
            }
        }
    }

    void iniciarTurno() {
        try {
            output.writeUTF("SERVER#INICIOTURNO$");
            System.out.println("Inicio de turno para: " + name);
        } catch (IOException ex) {
            Logger.getLogger(Jugador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void hitQuery(int i, int j) {
        server.hitQuery(i,j, this);
    }

    void sendShot(int i, int j) {
        try {
            output.writeUTF("SERVER#DISPARO$" + i + "," + j);
        } catch (IOException ex) {
            Logger.getLogger(Jugador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void playerHit(boolean hitConfirmed, int i, int j) {
        if (hitConfirmed) {
            server.playerCommunication(("SERVER#CONFIRMHIT$" + i + "," + j), this);
        } else {
            server.playerCommunication(("SERVER#DENYHIT$" + i + "," + j), this);
        }
    }

    void sendMessage(String message) {
        try {
            output.writeUTF(message);
        } catch (IOException ex) {
            Logger.getLogger(Jugador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    boolean isReady() {
        return isReady;
    }

    void triggerVictoria() {
        try {
            output.writeUTF("SERVER#VICTORIA");
        } catch (IOException ex) {
            Logger.getLogger(Jugador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void abandono() {
        inGame = false;
        server.declareAbandono(this);
    }
    
    void triggerDefault() {
        try {
            output.writeUTF("SERVER#DEFAULT");
            inGame = false;
        } catch (IOException ex) {
            Logger.getLogger(Jugador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void closeSocket() {
        try {
            this.playerSocket.close();
            server.searchPlayers();
        } catch (IOException ex) {
            Logger.getLogger(Jugador.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.inGame = false;
    }
    
}
