package com.cms.events;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static ActorSystem system;

    public static void main(String[] args) {
        loadConfigOverrides(args);

        initializeActorSystem();
        initializeHttpServer();
    }

    private static void loadConfigOverrides(String[] args) {
        String regex = "-D(\\S+)=(\\S+)";
        Pattern pattern = Pattern.compile(regex);

        for (String arg : args) {
            Matcher matcher = pattern.matcher(arg);

            while(matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                logger.info("Config Override: "+key+" = "+value);
                System.setProperty(key, value);
            }
        }
    }

    private static void initializeActorSystem() {
        system = ActorSystem.create("events");
    }

    private static void initializeHttpServer() {
        EventRoutes routes = new EventRoutes();

        int httpPort = system.settings()
            .config()
            .getInt("akka.http.server.default-http-port");

        Http.get(system)
            .newServerAt("localhost", httpPort)
            .bind(routes.createRoutes());
    }
}
