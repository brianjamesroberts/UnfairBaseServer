package com.unfairtools;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.json.Json;

/**
 * Created by brianroberts on 9/15/16.
 */
 public class PongGame {

    public String user1 = "1";
    public String user2 = "2";

    public DatagramSocket s1;
    public DatagramSocket s2;

    public String s1Host;
    public int s1Port;
    public String s2Host;
    public int s2Port;

    volatile String id;

    public volatile float ballPosX = .5f;
    public volatile float ballPosY = 0;

    public volatile float ballXV;

    public volatile float ballYV;

    public float paddleHeight = .13f;

    public volatile float paddle1 = .8f;
    public volatile float paddle2 = .92f;


    public volatile String winner = null;


    public volatile boolean started = false;

    private Vertx vertx;

    public PongGame(Vertx vertx1, String idd){
        id = idd;
        vertx = vertx1;
    }

    private RemoveRunnable  removeRunnable = null;



    public void updatePaddle(int playerNumber, float position){
        if(playerNumber==1)
            paddle1 = position;
        else
            paddle2 = position;
    }



    public InfoObject getGameInfo(int playerNum){
        InfoObject inf = new InfoObject();
        inf.action = "GAME_INFO";
        if(playerNum ==1){
            inf.vals = new String[]{"1", ballPosX + "", ballPosY + "", paddle2 + ""};
        }else{
            inf.vals = new String[]{"2", ballPosX + "", ballPosY + "", paddle1 + ""};
        }
        return inf;
    }


    public void run(){
        new Thread(new Runnable() {
            public void run() {
                ballXV = .013f;
                ballYV = .008f;
                float time = 0;
                int step = 0;
                started = true;

                double t2 = 0.0d;
                double t1 = 0.0d;

                while (winner == null) {


                    //System.out.println("Game running...");
                    //step++;

                    try {
                        t1 = System.currentTimeMillis();
                        Thread.sleep(35);
                        t2 = System.currentTimeMillis();
                        t1 = (t2 - t1) / 50.0f;
                        //System.out.println("t1 = " + t1);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    try {






                        if(s1!=null && s2!= null){
                            String s1String = Json.encode(getGameInfo(1));
                            String s2String = Json.encode(getGameInfo(2));
                           s1.send(Buffer.buffer(s1String.getBytes()), s1Port, s1Host, asyncResult2 -> {
                                if(asyncResult2.succeeded()){
                                    //System.out.println("Sending game data to p1");
                                }else{
                                    System.out.println(asyncResult2.cause().getMessage());
                                }
                            });

                            s2.send( Buffer.buffer(s2String.getBytes()), s2Port, s2Host, asyncResult2 -> {
                                if(asyncResult2.succeeded()){
                                    //System.out.println("Sending game data to p2");
                                }else{
                                    System.out.println(asyncResult2.cause().getMessage());
                                }
                            });


                        }else{
                            continue;
                        }
                    } catch (Exception e) {
                    }
                    ballPosX += ballXV * t1;
                    ballPosY += ballYV * t1;


                    //System.out.println("BALL POS:" + ballPosX + ", " + ballPosY);

                    if (ballXV < 0 && ballPosX < -.05) {
                        if (Math.abs(ballPosY - paddle1) < paddleHeight) {
                            //ballPosX = -.05f;
                            ballXV = -ballXV;
                        } else {
                            winner = user2;
                        }
                    }


                    if (ballXV > 0 && ballPosX > 1.05) {
                        if(Math.abs(ballPosY-paddle2) < paddleHeight){
                            //ballPosX = 1.05f;
                            ballXV = -ballXV;
                        }else {
                            winner = user1;
                        }
                    }

                    if (ballXV < 0 && ballPosX < .05 && Math.abs(ballPosY - paddle1) < paddleHeight) {
                        ballXV = -ballXV;
                        float factor =  (ballPosY - paddle1)/20;
                        ballYV += factor;

                        System.out.println("REBOUND OFF PADDLE 1");
                        ballXV = ballXV * 1.07f;
                    }
                    if (ballXV > 0 && ballPosX > .95 && Math.abs(ballPosY - paddle2) < paddleHeight) {
                        ballXV = -ballXV;
                        System.out.println("REBOUND OFF PADDLE 2");
                        float factor =  (ballPosY - paddle2)/20;
                        ballYV += factor;
                        ballXV = ballXV * 1.07f;
                    }

                    //traveling down, and is at the bottom of the screen
                    if (ballPosY < 0 && ballYV < 0) {
                        ballYV = -ballYV;
                        ballPosY = 0;
                    }

                    //traveling up, and its at the top of the screen
                    if (ballPosY > 1 && ballYV > 0) {
                        ballYV = -ballYV;
                        ballPosY = 1;
                    }


                    time++;
                }

                System.out.println("Winner is " + winner);
                System.out.println("Paddle1: " + paddle1);
                System.out.println("Paddle2: " + paddle2);
                System.out.println("Ball pos x,y: " + ballPosX + "," + ballPosY);
                removeRunnable = new RemoveRunnable(id);
                new Thread(removeRunnable).start();
                //AccountVerticle.gameHashMap.remove(id + "");
            }
        }).start();



    }



    class RemoveRunnable implements Runnable{
        String id;
        public RemoveRunnable(String idd){
            id = idd;
        }

        public RemoveRunnable() throws IllegalAccessException{
            throw new IllegalAccessException("You need to call the constructor with (int) as the sole paramater");
        }
        public void run(){
            try{
                vertx.executeBlocking(future -> {
                    UDPVerticle.deleteInvite(vertx,id);
                }, res -> {
                    if(res.succeeded()){
                        System.out.println("Deleted " + id + " game from PongGame class.");
                    }else{
                        System.out.println("FAILED to delete " + id + " game from PongGame class.");
                    }

                });
                Thread.sleep(1500);
                //note, DO NOT CLOSE UDP SOCKETS s1 and s2!!!
                AccountVerticle.gameHashMap.remove(id);
                System.out.println("Game " + id + "removed.");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


}
