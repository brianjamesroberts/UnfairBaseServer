package com.unfairtools;

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

    public PongGame(String idd){
        id = idd;
    }

    private RemoveRunnable  removeRunnable = null;
    public void updatePaddle(int playerNumber, float position){
        if(playerNumber==1)
            paddle1 = position;
        else
            paddle2 = position;
    }

    class RemoveRunnable implements Runnable{
        String id;
        public RemoveRunnable(String idd){
            id = idd;
        }
        private RemoveRunnable(){

        }
        public void run(){
            try{
                Thread.sleep(1500);
                AccountVerticle.gameHashMap.remove(id);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
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
                ballXV = .01f;
                ballYV = .008f;
                float time = 0;
                int step = 0;
                started = true;
                while (winner == null) {
                    //System.out.println("Game running...");
                    //step++;
                    try {
                        Thread.sleep(50);
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
                    ballPosX += ballXV * 1;
                    ballPosY += ballYV * 1;

                    //System.out.println("BALL POS:" + ballPosX + ", " + ballPosY);

                    if (ballXV < 0 && ballPosX < -.05)
                        winner = user2;


                    if (ballXV > 0 && ballPosX > 1.05)
                        winner = user1;

                    if (ballXV < 0 && ballPosX < .05 && Math.abs(ballPosY - paddle1) < paddleHeight) {
                        ballXV = -ballXV;
                        float factor =  (ballPosY - paddle1)/30;
                        ballYV += factor;

                        System.out.println("REBOUND OFF PADDLE 1");
                        ballXV = ballXV * 1.07f;
                    }
                    if (ballXV > 0 && ballPosX > .95 && Math.abs(ballPosY - paddle2) < paddleHeight) {
                        ballXV = -ballXV;
                        System.out.println("REBOUND OFF PADDLE 2");
                        float factor =  (ballPosY - paddle2)/30;
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


}
