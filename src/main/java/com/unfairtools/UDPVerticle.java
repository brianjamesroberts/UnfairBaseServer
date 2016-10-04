package com.unfairtools;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;

/**
 * Created by brianroberts on 9/23/16.
 */
public class UDPVerticle extends AbstractVerticle {


    public static void deleteInvite(Vertx vertx, String gameID){
        System.out.println("Deleting invite for " + gameID);

        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("database", "pongonline")
                .put("username", InitServer.dbOwner)
                .put("password", InitServer.dbPassword);
        AsyncSQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);
        postgreSQLClient.getConnection(res -> {

            if (res.succeeded()) {
                SQLConnection connection = res.result();
                try {
                    connection.query("DELETE from "+AccountVerticle.INVITES_TABLE_NAME+" WHERE gameid = '"+gameID+"';", res2 -> {
                        if (res2.succeeded()) {
                            System.out.println("invite deleted deleted!!! (" + gameID + ")");
                        } else {
                            System.out.println("Failed to delete invite! (" + gameID + ")");
                            System.out.println(res2.cause().toString());
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }finally{
                    connection.close();
                }
            }else{
                System.out.println("Failed to get database connection " + res.cause().toString());
            }
        });
    }

    public void setGameInfo(Vertx vertx, InfoObject incoming, Future future, DatagramSocket sock, DatagramPacket packet){
        if(!AccountVerticle.gameHashMap.containsKey(incoming.vals[1])) {
            //System.out.println("Sending halt");

            InfoObject inf = new InfoObject();
            inf.action = "halt";

            sock.send(Buffer.buffer("halt".getBytes()), packet.sender().port(), packet.sender().host(), asyncResult2 -> {
            });
            deleteInvite(vertx,incoming.vals[1]);
            future.fail("Game not found1");
            return;
        }else {
            //user, GameNumber, FirstOrSecondPlayer, canvas.getPaddleY() + ""};

            PongGame g = AccountVerticle.gameHashMap.get(incoming.vals[1]);

            if(g.winner!=null) {
            //System.out.println("Sending halt");

                InfoObject inf = new InfoObject();
                inf.action = "halt";

                if(incoming.vals[2].equals(g.winner))
                    inf.vals = new String[]{"true"};
                else
                    inf.vals = new String[]{"false"};



                sock.send(Buffer.buffer(Json.encode(inf).getBytes()), packet.sender().port(), packet.sender().host(), asyncResult2 -> {
                });
                deleteInvite(vertx,incoming.vals[1]);
                future.fail("game not found2");
                return;
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
                                    setGameInfo(vertx, incoming,future, socket, packet);
                                }, res ->{
                                    //if(!res.succeeded())
                                        //System.out.println(res.cause().getMessage());
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
