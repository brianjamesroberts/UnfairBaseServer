package com.unfairtools;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by brianroberts on 10/12/16.
 */
public class RESTService {


    //done
    // invites?
    // login
    // invite a user
    // New account

    //// TODO: 10/13/16
    // validate invite


    Vertx vertx;

    public RESTService(Vertx vert){
        vertx = vert;
        init();
    }

    public void validateInvite(RoutingContext routingContext){
        String gameNum = routingContext.request().getHeader("game-number");
        vertx.executeBlocking(future -> {
            AccountVerticle.ValidateGame(vertx,gameNum,future);
            },res-> {

                if (res.succeeded()) {
                    System.out.println("Future success validategame");
                    vertx.executeBlocking(future2->{
                        AccountVerticle.createGameIfNotExists(vertx, future2, gameNum);
                    },res2 -> {
                        if(res2.succeeded()) {
                            System.out.println("Created game  #" + gameNum);
                        }else {
                            System.out.println("Failed to createGameIfNotExists #" + gameNum);
                        }
                    });


                } else {
                    System.out.println("Future fail validategame");

                }
            routingContext.response().end(Json.encodePrettily(new InfoObject()));
            });
    }

    public void newAccount(RoutingContext routingContext){

        String user = routingContext.request().getHeader("username");
        String pass = routingContext.request().getHeader("password");

        if(!user.matches("^[a-zA-Z0-9]*$")||!pass.matches("^[a-zA-Z0-9]*$")) {
            InfoObject inf = new InfoObject();
            inf.action = "SNACKBAR";
            inf.vals = new String[]{"Must use 1-9 a-z"};
            routingContext.response().end(Json.encodePrettily(inf));
        }else if(user.length() <3 || pass.length()<3){
            InfoObject inf = new InfoObject();
            inf.action = "SNACKBAR";
            inf.vals = new String[]{"username, password must be 3+ characters"};
            routingContext.response().end(Json.encodePrettily(inf));
        }else {
            vertx.executeBlocking(future -> {
                AccountVerticle.createNewAccount(vertx, future, user, pass);
            }, res -> {
                if (res.succeeded()) {
                    InfoObject inf = new InfoObject();
                    inf.action = "SNACKBAR";
                    inf.vals = new String[]{"Account created! :)"};
                    routingContext.response().end(Json.encodePrettily(inf));
                } else {
                    InfoObject inf = new InfoObject();
                    inf.action = "SNACKBAR";
                    inf.vals = new String[]{"Failed, username taken :("};
                    routingContext.response().end(Json.encodePrettily(inf));
                }
            });
        }

    }

    public void inviteUser(RoutingContext routingContext){

        String invitedUser = routingContext.request().getHeader("invited-user");
        String invitingUser = routingContext.request().getHeader("inviting-user");

        if(!invitedUser.matches("^[a-zA-Z0-9]*$")||!invitingUser.matches("^[a-zA-Z0-9]*$")){
            InfoObject inf = new InfoObject();
            inf.action = "SNACKBAR";
            inf.vals = new String[]{"Must use 1-9 a-z"};
            routingContext.response().end(Json.encodePrettily(inf));
        }else {
            //thread safe
            int id = AccountVerticle.gameID++;
            vertx.executeBlocking(future -> {
                AccountVerticle.InviteUser(vertx, invitedUser, id + "", future, invitingUser);
            }, res -> {
                if (res.succeeded()) {
                    InfoObject inf = new InfoObject();
                    inf.action = "SNACKBAR";
                    inf.vals = new String[]{"Invite sent!"};
                    routingContext.response().end(Json.encodePrettily(inf));
                } else {
                    InfoObject inf = new InfoObject();
                    inf.intVals = new int[]{id};
                    inf.action = "SNACKBAR";
                    inf.vals = new String[]{res.cause().getMessage()};
                    routingContext.response().end(Json.encodePrettily(inf));
                }
            });
        }


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
        router.post("/pongonline/api/invite_user").handler(this::inviteUser);
        router.post("/pongonline/api/new_account").handler(this::newAccount);
        router.post("/pongonline/api/validate_invite").handler(this::validateInvite);
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
