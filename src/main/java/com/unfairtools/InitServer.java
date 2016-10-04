package com.unfairtools;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Created by brianroberts on 9/13/16.
 */
public class InitServer {


    //dear hackers, these are only for my postgres, and will not work for ssh.
    public static String dbOwner = "brianroberts";
    public static String dbPassword = "password1";
    public static String ServerIP = "158.69.207.153";
    public static int UDPPort = 8086;
    public static int TCPPort = 8079;


    public InitServer(){

        Vertx vertx;
        vertx = Vertx.vertx();
        Handler<AsyncResult<String>> handler = new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if(event.cause()==null){
                    System.out.println("result: Successfully deployed TSL/TCP server");
                }else{
                    System.out.println(event.cause());
                }
            }
        };

        Handler<AsyncResult<String>> handler2 = new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if(event.cause()==null){
                    System.out.println("result: Successfully deployed UDP server");
                }else{
                    System.out.println(event.cause());
                }
            }
        };

        try {
            vertx.deployVerticle(AccountVerticle.class.getName(), handler);
            vertx.deployVerticle(UDPVerticle.class.getName(),handler2);
        }catch(Exception e){
            e.printStackTrace();
        }


    }


    public static void main(String[] args){
        new InitServer();
    }

}
