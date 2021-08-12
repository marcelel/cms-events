package com.cms.events;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.http.javadsl.Http;
import akka.stream.Materializer;
import com.cms.events.mongo.MongoConfiguration;
import com.cms.events.mongo.ReadDataStore;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static ActorSystem system;
    private static Materializer materializer;
    private static EventRepository eventRepository;
    private static ActorRef eventActorSupervisor;

    public static void main(String[] args) {
        loadConfigOverrides(args);

        initializeActorSystem();
        initializeEventRepository();
        initializeActors();
        initializeHttpServer();
    }

    private static void loadConfigOverrides(String[] args) {
        String regex = "-D(\\S+)=(\\S+)";
        Pattern pattern = Pattern.compile(regex);

        for (String arg : args) {
            Matcher matcher = pattern.matcher(arg);

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                logger.info("Config Override: " + key + " = " + value);
                System.setProperty(key, value);
            }
        }
    }

    private static void initializeActorSystem() {
        system = ActorSystem.create("Events");
        materializer = Materializer.createMaterializer(system);
    }

    private static void initializeEventRepository() {
        MongoDatabase mongoDatabase = new MongoConfiguration().create();
        ReadDataStore readDataStore = new ReadDataStore(system, mongoDatabase);
        eventRepository = new MongoEventRepository(readDataStore);
    }

    private static void initializeActors() {
        eventActorSupervisor = ClusterSharding.get(system).start(
                "eventShardedActor",
                EventActor.create(eventRepository),
                ClusterShardingSettings.create(system),
                new EventShardingMessageExtractor(30)
        );
    }

    private static void initializeHttpServer() {
        EventRoutes routes = new EventRoutes(eventActorSupervisor);

        int httpPort = system.settings()
                .config()
                .getInt("akka.http.server.default-http-port");

        Http.get(system)
                .newServerAt("localhost", httpPort)
                .bind(routes.createRoutes());
    }
}