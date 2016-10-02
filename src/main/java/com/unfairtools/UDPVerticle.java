package com.unfairtools;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.Json;

/**
 * Created by brianroberts on 9/23/16.
 */
public class UDPVerticle extends AbstractVerticle {

    public void setGameInfo(InfoObject incoming, Future future, DatagramSocket sock, DatagramPacket packet){
        if(!AccountVerticle.gameHashMap.containsKey(incoming.vals[1])) {
            future.fail("Game not found");
            //System.out.println("Sending halt");
            sock.send(Buffer.buffer("halt".getBytes()), packet.sender().port(), packet.sender().host(), asyncResult2 -> {
            });
        }else {
            //user, GameNumber, FirstOrSecondPlayer, canvas.getPaddleY() + ""};

            PongGame g = AccountVerticle.gameHashMap.get(incoming.vals[1]);

            if(g.winner!=null) {
            //System.out.println("Sending halt");
                sock.send(Buffer.buffer("halt".getBytes()), packet.sender().port(), packet.sender().host(), asyncResult2 -> {
                });
            }

        if(incoming.vals[2].equals("1")) {

            if (g.s1 == null) {
                System.out.println("Starting game");
                System.out.println("Player 1 has joined the game");
                g.s1 = sock;
                g.s1Host = packet.sender().host();
                g.s1Port = packet.sender().port();
                if(g.s2 != null)
                    g.run();
            }
        }else {
            if (g.s2 == null) {
                System.out.println("Player 2 has joined the game");
                g.s2 = sock;
                g.s2Host =  packet.sender().host();
                g.s2Port =  packet.sender().port();
                if(g.s1!=null)
                    g.run();
            }
        }
            g.updatePaddle(Integer.parseInt(incoming.vals[2]), Float.parseFloat(incoming.vals[3]));

            future.complete();
        }
    }

    public void start(){
        DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
        socket.listen(InitServer.UDPPort, InitServer.ServerIP, asyncResult -> {
            if (asyncResult.succeeded()) {
                socket.handler(packet -> {

                    //System.out.println("Incoming address: " + packet.sender() + ", port: " + packet.sender().port());

                    String incomingStr = packet.data().getString(0,packet.data().length());
                    //System.out.println("incoming " + incomingStr);

                    try {
                        InfoObject incoming = Json.decodeValue(incomingStr, InfoObject.class);
                        //System.out.println("Action was " + incoming.action);

                        switch (incoming.action) {
                            case "INIT_SOCKETS_GAME":
                                if(incoming.vals[1].equals("1"))
                                    AccountVerticle.gameHashMap.get(incoming.vals[0]).s1 = socket;
                                else
                                    AccountVerticle.gameHashMap.get(incoming.vals[0]).s2 = socket;
                                break;

                            //receive info from client
                            case "SEND_GAME_INFO":
                                //System.out.println("Setting game info(" + incomingStr + ")" );
                                vertx.executeBlocking(future -> {
                                    setGameInfo(incoming,future, socket, packet);
                                }, res ->{
                                    if(!res.succeeded())
                                        System.out.println(res.cause().getMessage());
                                });
                                break;
                            default:
                                System.out.println("ERROR("  +incomingStr+")");
                                break;

                        }
                    }catch(Exception e){

                    }
                });
            } else {
                System.out.println("Listen failed" + asyncResult.cause());
            }
        });
    }

}
