
package io.nms.agent.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.common.AmqpAgentVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/*
 * Agent entry point
 * reads config file and deploys modules as Verticles
 * */
public class Main {
	private static Logger LOG = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		if (args.length < 1) {
			LOG.error("No configuration file found.");
			System.exit(1);
		}
		String configFile = args[0];
		JsonObject configuration = new JsonObject();
		String content;
		
	    try {
	    	LOG.info("Reading configuration file.");
	        content = new String(Files.readAllBytes(Paths.get(configFile)));
			configuration = new JsonObject(content);
	    } catch (IOException e) {
	    	LOG.error(e.getMessage());
			System.exit(1);
	    }
	    
	    Vertx vertx = Vertx.vertx();
	    List<Future> futures = new ArrayList<>();
		
		List<AmqpAgentVerticle> verticles = new ArrayList<>();
		final JsonArray modules = configuration.getJsonArray("modules");
		for (int i = 0; i < modules.size(); i++) {
			String moduleName = modules.getJsonObject(i).getString("name", "[Unnamed]");
			LOG.info("Deploying "+moduleName+" module.");
			
			Future<Void> deployFuture = Future.future();
			futures.add(deployFuture);
				
			JsonObject vertConfig = new JsonObject();
			vertConfig.put("module", modules.getJsonObject(i));
			vertConfig.put("config", modules.getJsonObject(i)
					.getJsonObject("config", new JsonObject()));
			vertConfig.put("agent", configuration.getJsonObject("agent"));
			vertConfig.put("amqp", configuration.getJsonObject("amqp"));

			AmqpAgentVerticle vModule = new AmqpAgentVerticle();
			vertx.deployVerticle(vModule, new DeploymentOptions()
				.setWorker(true)
				.setConfig(vertConfig),
				res -> {
					if (res.succeeded()) {
						LOG.info(moduleName+" module deployed.");
						verticles.add(vModule);
						deployFuture.complete();
					} else {
						LOG.error("Error in deploying "+moduleName+" module", res.cause());
						deployFuture.fail(res.cause());
					}
				});
		}
			
		if (futures.isEmpty()) {
			LOG.error("No modules to deploy. Agent stops.");
			System.exit(0);
		} else {
			CompositeFuture.all(futures)
				.setHandler(ar -> {
					if (ar.failed()) {
						LOG.error("Error on starting agent", ar.cause().getMessage());
			        	System.exit(1);
					} else {
						LOG.info(futures.size() + " module(s) deployed.");
						LOG.info("Agent running...");
					}
				});
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	List<Future> futures = new ArrayList<>();
            	Future<Void> undeployFuture = Future.future();
    			
            	for (int i = 0; i < verticles.size(); i++) {
        			undeployFuture = Future.future();
        			futures.add(undeployFuture);
        			try {
        				verticles.get(i).stop(undeployFuture);
        			} catch (Exception e) {	
        				undeployFuture.fail(e.getMessage());
        			}
        		}
            	
            	CompositeFuture.all(futures)
				.setHandler(ar -> {
					if (ar.failed()) {
						LOG.error("Agent terminated with errors", ar.cause());
			        	System.exit(1);
					} else {
						LOG.info(futures.size() + " module(s) undeployed.");
						LOG.info("Agent successfully terminated.");
						System.exit(0);
					}
				});
            }
        });
	}
}