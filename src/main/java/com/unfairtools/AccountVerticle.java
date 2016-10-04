package com.unfairtools; /**
 * Created by brianroberts on 9/13/16.
 */


import io.netty.handler.codec.http.HttpContentEncoder;
import io.vertx.core.*;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;

import io.vertx.core.net.NetSocket;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class AccountVerticle extends AbstractVerticle {

    public static int port = 8079;

    public static int gameID = 0;

    public static String LOGIN_TABLE_NAME = "LOGIN_TABLE";
    public static String INVITES_TABLE_NAME = "INVITES_TABLE";

    public static volatile ConcurrentHashMap<String, PongGame> gameHashMap = new ConcurrentHashMap<String, PongGame>();


    public static void killInvites(io.vertx.core.Vertx vertx, String gameId, Future future){
        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("database", "pongonline")
                .put("username", InitServer.dbOwner)
                .put("password", InitServer.dbPassword);
        AsyncSQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);
        postgreSQLClient.getConnection(res -> {

            if (res.succeeded()) {
                SQLConnection connection = res.result();
                try {
                    System.out.println("Attempting to delete gameid " + gameId + " invites...");
                    connection.query("DELETE FROM " +INVITES_TABLE_NAME +" WHERE gameid = '"+ gameId+"';", res2-> {
                       if(res.succeeded()){
                           future.succeeded();
                       }else{
                           future.fail(res2.cause().getMessage());
                       }
                    });

                } catch (Exception e) {
                    future.fail(e.toString());
                    e.printStackTrace();
                } finally {
                    connection.close();
                }
            }else{
                future.fail(res.cause().getMessage());
            }

    });
    }

    public static void RespondDoesExistsInvites(io.vertx.core.Vertx vertx, String requestedUser, Future future){
        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("database", "pongonline")
                .put("username", InitServer.dbOwner)
                .put("password", InitServer.dbPassword);
        AsyncSQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);
        postgreSQLClient.getConnection(res -> {

            if (res.succeeded()) {
                SQLConnection connection = res.result();


                try {
                    //System.out.println("Requested user = " + requestedUser);
                    String query = "select * from "+INVITES_TABLE_NAME+" where username = '"+requestedUser+"';";
                    //System.out.println("Query: " + query);
                    connection.query(query, res2 -> {
                        ArrayList<ArrayList<String>> map = new ArrayList<ArrayList<String>>();
                        if (res2.succeeded()) {
                            for(int i = 0; i < res2.result().getResults().size();i++) {
                                map.add(new ArrayList<String>());

                                //gameid
                                map.get(map.size()-1).add(res2.result().getResults().get(i).getString(1));

                                //from
                                map.get(map.size()-1).add(res2.result().getResults().get(i).getString(2));

                                map.get(map.size()-1).add("waiting");

                                //to user
                                map.get(map.size()-1).add(res2.result().getResults().get(i).getString(0));

                                //match = true or false.
                                map.get(map.size()-1).add(res2.result().getResults().get(i).getString(3));

                            }
                            String query3 = "select * from "+INVITES_TABLE_NAME+" where fromuser = '"+requestedUser+"';";
                            connection.query(query3,res3 ->{
                                if(res3.succeeded()){

                                    for(int i = 0; i < res3.result().getResults().size();i++) {
                                        map.add(new ArrayList<String>());
                                        //gameid
                                        map.get(map.size()-1).add(res3.result().getResults().get(i).getString(1));

                                        //from
                                        map.get(map.size()-1).add(res3.result().getResults().get(i).getString(2));
                                        //maps2[i][1] = res3.result().getResults().get(i).getString(2);

                                        //say pending.
                                        map.get(map.size()-1).add("pending");

                                        //to user
                                        map.get(map.size()-1).add(res3.result().getResults().get(i).getString(0));

                                        //match = true or false.

                                        map.get(map.size()-1).add(res3.result().getResults().get(i).getString(3));

                                    }
                                    String[][] maps = new String[map.size()][5];

                                    for(int i = 0; i < map.size(); i++){
                                        for(int c = 0; c < map.get(i).size(); c++){
                                            maps[i][c] = map.get(i).get(c);
                                        }
                                    }
                                    future.complete(maps);
                                    return;
                                }else{
                                    System.out.println("Future faileddddd" + res3.cause().toString());
                                    future.fail("second invites select failed" + res3.cause().getMessage().toString());
                                }
                            });

                            //System.out.println(requestedUser + " has " + res2.result().getResults().get(0).getString(1) + " from " + res2.result().getResults().get(0).getString(2) + " gameid invite!");
                            //future.complete(new String[]{res2.result().getResults().get(0).getString(1), res2.result().getResults().get(0).getString(2)});
                        } else {
                            future.fail(res2.cause().getMessage().toString());
                        }
                    });
                } catch (Exception e) {
                    future.fail("ERROR 223");
                } finally {
                    connection.close();
                }
            }else{
                future.fail("Couldn't connect to databse!");
            }
        });
    }

    public static void ValidateGame(io.vertx.core.Vertx vertx, String gameNum, Future future){
        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("database", "pongonline")
                .put("username", InitServer.dbOwner)
                .put("password", InitServer.dbPassword);
        AsyncSQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);

        postgreSQLClient.getConnection(res -> {

            if (res.succeeded()) {
                String query = "UPDATE " + INVITES_TABLE_NAME + " SET match = 'true' where gameid = '" + gameNum + "';";
                SQLConnection connection = res.result();

                try{
                connection.query(query, res2 -> {
                    if (res2.succeeded()) {
                        System.out.println("succeeded in validating game " + res2.result().toString());

                    } else {
                        System.out.println("Failed to validate game " + res2.cause().toString());
                    }
                });
            }catch(Exception e){
                future.fail(e.toString());
            }finally {
                    connection.close();
                }
            }else {
                future.fail(res.cause().getMessage());
            }
        });
    }

    public static void InviteUser(io.vertx.core.Vertx vertx, String name, String id, Future future, String invitingUser) {
        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("database", "pongonline")
                .put("username", InitServer.dbOwner)
                .put("password", InitServer.dbPassword);
        AsyncSQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);
        postgreSQLClient.getConnection(res -> {

            if (res.succeeded()) {
                SQLConnection connection = res.result();

                try {
                    
                    connection.query("SELECT COUNT(*) from " + LOGIN_TABLE_NAME + " WHERE username = '" + name
                            + "';", res2 -> {
                        if (res2.succeeded()) {
                            if (res2.result().getResults().get(0).getInteger(0) > 0) {
                                System.out.println("Found user " + name);
                                connection.query("DELETE FROM " + INVITES_TABLE_NAME + " WHERE username = '" + name +"' AND NOT " +
                                        "gameid = '" + id + "' AND fromuser = '" + invitingUser + "';", res4 ->{
                                    if(res4.succeeded()){
                                        gameHashMap.remove(id);
                                        System.out.println("Deleted records for " + name);
                                        connection.query("INSERT INTO " + INVITES_TABLE_NAME + "(username, gameid, fromuser, match) VALUES ('" + name + "', '" + id  + "'" +
                                        ", '" +invitingUser + "', 'false');",
                                                res3 -> {
                                                    if(res3.succeeded()){
                                                        System.out.println("Inserted new invite request for " + name + ", " + id);
                                                        PongGame game = new PongGame(id);
                                                        //game.run();
                                                        gameHashMap.put(id,game);
                                                        future.complete();
                                                    }else{
                                                        System.out.println(res3.cause().getMessage());
                                                        future.fail("Failed to send invite to " + name);
                                                        return;
                                                    }

                                                });
                                    }else{
                                        future.fail("FAILURE TO DELETE OLD SHIT??");
                                        return;
                                    }
                                });
                            } else {
                                System.out.println("Couldn't find user " + name);
                                future.fail("Couldn't find user " + name);
                                return;
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }finally{
                    connection.close();
                }
            }else{
                future.fail("Couldn't connect to databse!");
                return;
            }
        });
    }


    public void returnGameInfo(InfoObject incoming, NetSocket netSocket,Future future){


        if(!gameHashMap.containsKey(incoming.vals[1])) {
            InfoObject inf = new InfoObject();

            inf.intVals= new int[]{-1};

            inf.action = "GAME_INFO";
            inf.vals = new String[]{"1", 1+ "", 1+ "ERROR", 1 + ""};
            netSocket.write(Json.encode(inf) + "\n");

        }else {

            new Thread(new Runnable() {
                PongGame g = gameHashMap.get(incoming.vals[1]);


                public void run() {

                    //System.out.println("PongGame g is " + g.toString());
                    //player #, float position 0 -> 1
                    g.updatePaddle(Integer.parseInt(incoming.vals[2]), Float.parseFloat(incoming.vals[3]));
                    //netSocket.write(Json.encode(g.getGameInfo(incoming)) + "\n");

            }
        });
    }
    }


    public static void createNewAccount(Vertx vertx, Future future, String user, String pass){
        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("database", "pongonline")
                .put("username", InitServer.dbOwner)
                .put("password", InitServer.dbPassword);
        AsyncSQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);
        postgreSQLClient.getConnection(res -> {

            if (res.succeeded()) {
                SQLConnection connection = res.result();

                try {
                    connection.query("INSERT INTO " + LOGIN_TABLE_NAME + " (username, password) VALUES ('" + user + "', '" + pass + "');", res2 -> {
                        if (res2.succeeded()) {
                            future.complete("Account created");
                        } else {
                            future.fail(res2.cause().toString());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    connection.close();
                }
            }
        });
    }


    public static void Login(io.vertx.core.Vertx vertx, String user, String pass, String appName, Future future){

        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("database", appName)
                .put("username", InitServer.dbOwner)
                .put("password", InitServer.dbPassword);
        AsyncSQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);
        postgreSQLClient.getConnection(res -> {

            if (res.succeeded()) {
                SQLConnection connection = res.result();

                try {
                    connection.query("SELECT COUNT(*) from " + LOGIN_TABLE_NAME + " WHERE username = '" + user
                            + "' AND password = '" + pass + "';", res2 -> {
                        if (res2.succeeded()) {
                            System.out.println("Count for " + user + " : " + pass + " resulted in " +
                            res2.result().getResults().get(0).getInteger(0));
                            if(res2.result().getResults().get(0).getInteger(0)>0){
                                future.complete("login success");

                            }else{
                                future.fail("Incorrect Username or Password");
                            }
                        } else {
                            future.fail("Internal failure 9009");
                        }

                    });
                }finally{
                    connection.close();

                }
            } else {
                System.out.println("Failed to connect to postgres" + res.cause());
            }
        });
    }

    @Override
    public void start() throws Exception {
        NetServer server = vertx.createNetServer();

        server.connectHandler(new Handler<NetSocket>() {

            @Override
            public void handle(NetSocket netSocket) {
                System.out.println("Incoming connection!");

                netSocket.handler(new Handler<Buffer>() {


                    @Override
                    public void handle(Buffer buffer) {


                        String incomingStr = buffer.getString(0,buffer.length());
                        //System.out.println("Incoming: " + incomingStr);

                        //try{
                            InfoObject incoming = Json.decodeValue(incomingStr,InfoObject.class);
                            //System.out.println("Action was " + incoming.action);
                            switch(incoming.action){
                                case "NEW_ACCOUNT":
                                    vertx.executeBlocking(future -> {
                                        createNewAccount(vertx,future,incoming.vals[0],incoming.vals[1]);
                                    }, res ->{
                                            if(res.succeeded()){
                                                InfoObject inf = new InfoObject();
                                                inf.action="SNACKBAR";
                                                inf.vals = new String[]{"Account created! :)"};
                                                netSocket.write(Json.encode(inf) + "\n");
                                            }else{
                                                InfoObject inf = new InfoObject();
                                                inf.action="SNACKBAR";
                                                inf.vals = new String[]{"Failed, username taken :("};
                                                netSocket.write(Json.encode(inf) + "\n");
                                            }
                                    });
                                    break;
                                case "START_GAME":
                                    System.out.println("Recieved START_GAME from player 2 for game");
                                    gameHashMap.get(incoming.vals[0]).run();
                                    break;
                                case "LOGIN":
                                    vertx.executeBlocking(future -> {
                                        // Call some blocking API that takes a significant amount of time to return
                                        Login(vertx, incoming.vals[0],incoming.vals[1], incoming.appName, future);
                                        //future.complete("BLAH");
                                    }, res -> {
                                        if(res.succeeded()){
                                            System.out.println("Login achieved\n");
                                            InfoObject ret = new InfoObject();
                                            ret.vals = new String[]{incoming.vals[0],incoming.vals[1]};
                                            ret.action = new String("login_accepted");

                                            netSocket.write(Json.encode(ret ) + "\n");
                                        }else{
                                            System.out.println("Login Failed");
                                            InfoObject ret = new InfoObject();
                                            ret.vals = new String[]{incoming.vals[0],incoming.vals[1]};
                                            ret.action = new String("login_denied");

                                            netSocket.write(Json.encode(ret) + "\n");

                                        }
                                    });
                                    break;
                                case "KILL_INVITE":

                                    //get game id from (0)
                                    String gameId = incoming.vals[0];
                                    vertx.executeBlocking(future -> {
                                                killInvites(vertx, gameId, future);
                                            }
                                    ,res ->{
                                            if(res.succeeded()){
                                                System.out.println("Killed invite " + incoming.vals[0]);
                                            }else{
                                              System.out.println(res.cause().getMessage());
                                            }
                                        });
                                    break;
                                case "INVITES?":
                                    String requestedUser = incoming.vals[0];
                                    //System.out.println("Checking for invites for " + requestedUser);
                                    vertx.executeBlocking(future -> {
                                        RespondDoesExistsInvites(vertx, requestedUser, future);
                                    },res ->{
                                        if(res.succeeded()){
                                            InfoObject inf3 = new InfoObject();
                                            inf3.action="INVITE_RECEIVE";
                                            inf3.maps = (String[][])res.result();
                                            netSocket.write(Json.encode(inf3) + "\n");
                                        }else{
//                                            InfoObject inf3 = new InfoObject();
//                                            inf3.action = "SNACKBAR";
//                                            //res.cause().getMessage().toString()
//                                            inf3.vals=new String[]{};
                                            //netSocket.write(Json.encode(inf3) + "\n");
                                        }
                                    });
                                    break;
                                case "NEW_GAME":
                                    //thread safe
                                    int id = gameID++;
                                    InfoObject inf = new InfoObject();
                                    inf.action="DISPLAY_GAME";
                                    inf.intVals=new int[]{id};
                                    String invitedUser = incoming.vals[0];
                                    String invitingUser = incoming.vals[1];
                                    vertx.executeBlocking(future -> {
                                        InviteUser(vertx, incoming.vals[0], id+"", future,invitingUser);
                                    }, res -> {
                                        if (res.succeeded()) {
                                            InfoObject inf2 = new InfoObject();
                                            inf2.action = "JOIN_GAME";
                                            //send game id, and which player u are
                                            inf2.vals = new String[]{id+"","1",invitedUser,invitingUser};
                                            netSocket.write(Json.encode(inf2) + "\n");


                                        } else {
                                            inf.action="SNACKBAR";
                                            inf.vals = new String[]{res.cause().getMessage()};
                                            netSocket.write(Json.encode(inf) + "\n");
                                        }
                                    });

                                    break;


                                //accept button clicked. make game available to both players.
                                case "VALIDATE_GAME_INVITE":
                                    String gameNum = incoming.vals[0];
                                    vertx.executeBlocking(future -> {
                                        ValidateGame(vertx,gameNum,future);
                                    },res->{

                                    });
                                    break;
                                case "JOIN_GAME":
                                    vertx.executeBlocking(future -> {
                                        createGameIfNotExists(vertx, future, incoming.vals[2]);
                                    },res ->{

                                    });
                                    break;
                                case "SEND_GAME_INFO":
                                    //vertx.executeBlocking(future -> {
                                            //returnGameInfo(incoming, netSocket,future);
                                    //}, res ->{

                                    //});

                                    break;
                                case "ping":
                                    System.out.println("Ping recieved");
                                    InfoObject ret = new InfoObject();
                                    ret.action="ping";
                                    netSocket.write(Json.encode(ret) + "\n");
                                default:
                                    break;
                            }
                        //}catch(Exception e){};
                    }
                });


            }
        });
        server.listen(InitServer.TCPPort);
    }


    public static void createGameIfNotExists(Vertx vertx, Future future, String gameNumber){
        System.out.println("Create? game " + gameNumber + " requested...");
        if(!gameHashMap.containsKey(gameNumber)) {
            System.out.println(gameNumber + " didn't exist, creating...");
            gameHashMap.put(gameNumber, new PongGame(gameNumber));
        }
        future.complete();
    }
}

class InfoObject{
    public String action;
    public String[] vals;
    public String appName;
    public int[] intVals;
    public String[][] maps;

}