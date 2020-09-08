package io.vertx.issue;

import graphql.GraphQL;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.graphql.ApolloWSHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.issue.entities.Sign;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class IssueReproducer {

    public static void main(String[] args) {
        PublishSubject<Sign> provider = PublishSubject.create();
        Flowable<Sign> publisher = provider.toFlowable(BackpressureStrategy.BUFFER);
        List<Sign> signs = new ArrayList<>();

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", wiring -> wiring
                        .dataFetcher("signs", new StaticDataFetcher(signs))
                )
                .type("Mutation", wiring -> wiring
                        .dataFetcher("createSign", environment -> {
                            Map<String, Object> signInput = environment.getArgument("input");
                            Sign sign = Util.serialize(Sign.class, signInput, new Sign());
                            provider.onNext(sign);
                            signs.add(sign);
                            return sign;
                        })
                )
                .type("Subscription", wiring -> wiring
                        .dataFetcher("subscribeSigns", environment -> publisher)
                )
                .build();

        SchemaParser parser = new SchemaParser();
        SchemaGenerator generator = new SchemaGenerator();

        TypeDefinitionRegistry signRegistry = parser.parse(IssueReproducer.class.getClassLoader().getResourceAsStream("schema.graphqls"));

        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        typeRegistry.merge(signRegistry);

        GraphQLSchema schema = generator.makeExecutableSchema(typeRegistry, runtimeWiring);
        GraphQL graphQL = GraphQL.newGraphQL(schema)
                .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                .build();

        /* Initialise vertx instance */
        Vertx vertx = Vertx.vertx();
        HttpServerOptions options = new HttpServerOptions().addWebSocketSubProtocol("graphql-ws");
        HttpServer server = vertx.createHttpServer(options);
        Router router = Router.router(vertx);

        /* Add cors header for GraphQL client */
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*")
                .allowedHeader("Access-Control-Allow-Origin")
                .allowedHeader("Access-Control-Allow-Headers")
                .allowedHeader("Content-Type"));

        /* Custom error handler */
        router.errorHandler(500, rc -> log.error("Error 500", rc.failure()));

        /* GraphQL server inclusive websocket subscriptions */
        router.route("/graphql").handler(ApolloWSHandler.create(graphQL));
        router.route("/graphql").handler(GraphQLHandler.create(graphQL));

        /* Static handling of client and GraphQL playground */
        router.route("/playground").handler(StaticHandler.create().setWebRoot("webroot/playground.html"));

        server.requestHandler(router).listen(8080);
        log.info("Server is up and running on port 8080.");
        log.info("http://localhost:8080/playground");
    }

}
