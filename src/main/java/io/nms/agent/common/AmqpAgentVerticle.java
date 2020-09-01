package io.nms.agent.common;

import io.vertx.core.Future;
import io.vertx.core.Promise;

/*
 * Agent Verticle with AMQP and base operations
 * Extends AmqpVerticle 
 * Deployment sequence:
 * - create local Capabilities
 * - create AMQP connection
 * - publish Capabilities
 * - listen to Specifications
 */
public class AmqpAgentVerticle extends AmqpVerticle {
	
	public void start(Promise<Void> fut) {
		super.start();

		agentName = config().getJsonObject("agent").getString("name", "");
		
	    moduleName = config().getJsonObject("module").getString("name", "[Unnamed]");
		capClasses = config().getJsonObject("module").getJsonArray("capabilities");
		
		host = config().getJsonObject("amqp").getString("host", "");
		port = config().getJsonObject("amqp").getInteger("port", 0);
		
		moduleConfig = config().getJsonObject("config");
		
		Future<Void> futConn = Future.future(promise -> initModule(promise));
		Future<Void> futInit = futConn
			.compose(v -> {
				return Future.<Void>future(promise -> createCapabilities(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> createAmqpConnection(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> publishCapabilities(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> subscribeToSpecifications(promise));
			});
			futInit.onComplete(res -> {
				if (res.failed()) {
					fut.fail(res.cause());
				} else {
					vertx.setPeriodic(heartbeatMs, id -> {
				    	Future<Void> capsFut = Future.future(promise -> publishCapabilities(promise));
				    	capsFut.onComplete(r -> {
					        LOG.info("Capabilities updated.");
					    });
					});
					fut.complete();
				}
			});
	}
}
