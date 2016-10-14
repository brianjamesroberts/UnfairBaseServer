package com.unfairtools;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by brianroberts on 10/12/16.
 */
public class RESTService {


    Vertx vertx;

    public RESTService(Vertx vert){
        vertx = vert;
        init();
    }


    public void getInvites(RoutingContext routingContext){
        String user = routingContext.request().getHeader("username");
        if(!user.matches("^[a-zA-Z0-9]*$")){
            InfoObject infoObject = new InfoObject();
            infoObject.action="SNACKBAR";
            infoObject.vals = new String[]{"user '" + user + "' not found err 8122" };
            routingContext.response().end(Json.encodePrettily(infoObject));
        }else{
            InfoObject infoObject = new InfoObject();
            vertx.executeBlocking(future -> {
                AccountVerticle.RespondDoesExistsInvites(vertx, user, future);
            },res ->{
                if(res.succeeded()){
                    infoObject.action="INVITE_RECEIVE";
                    infoObject.maps = (String[][])res.result();
                    routingContext.response().end(Json.encodePrettily(infoObject));
                    //netSocket.write(Json.encode(inf3) + "\n");
                }else{
                    routingContext.response().end();
                }
            });
        }

    }

    public void getLogin(RoutingContext routingContext){
        System.out.println("login post received");
        String user = routingContext.request().getHeader("username");
        String pass = routingContext.request().getHeader("password");
        System.out.println("REST:" + routingContext.request().getHeader("username"));
        System.out.println("REST:" + routingContext.request().getHeader("password"));

        if(!user.matches("^[a-zA-Z0-9]*$")||!pass.matches("^[a-zA-Z0-9]*$")){
            InfoObject inf = new InfoObject();
            inf.action = "SNACKBAR";
            inf.vals = new String[]{"Must use 1-9 a-z"};
            routingContext.response().end(Json.encodePrettily(inf));
            //netSocket.write(Json.encode(inf) + "\n");
        }else {
            vertx.executeBlocking(future -> {
                // Call some blocking API that takes a significant amount of time to return
                AccountVerticle.Login(vertx, user, pass, "pongonline", future);
            }, res -> {
                if (res.succeeded()) {
                    System.out.println("Login achieved\n");
                    InfoObject ret = new InfoObject();
                    ret.vals = new String[]{user, pass};
                    ret.action = new String("login_accepted");

                    routingContext.response().end(Json.encode(ret));
                } else {
                    System.out.println("Login Failed");
                    InfoObject ret = new InfoObject();
                    ret.vals = new String[]{user, pass};
                    ret.action = new String("login_denied");
                    routingContext.response().end(Json.encodePrettily(ret));

                }
            });
        }
    }

    public void init(){
        Router router = Router.router(vertx);

        router.post("/pongonline/api/login").handler(this::getLogin);
        router.post("/pongonline/api/invites").handler(this::getInvites);
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(InitServer.RestfulPort, result -> {
                    if (result.succeeded()) {
                        System.out.println("Success deploying REST to " + InitServer.RestfulPort);
                        //fut.complete();
                    } else {
                        System.out.println("Failure deploying REST to " + InitServer.RestfulPort);
                        //fut.fail(result.cause());
                    }

                });

    }

}
