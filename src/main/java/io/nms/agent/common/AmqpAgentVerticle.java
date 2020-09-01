package io.nms.agent.common;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/*
 * Agent Verticle with AMQP and base operations
 * Extends AmqpVerticle 
 * Deployment sequence:
 * 1- create local Capabilities
 * 2- create AMQP connection
 * 3- authenticate this module
 * 4- publish Capabilities to DSS
 * 5- listen to Client Specifications
 */
public class AmqpAgentVerticle extends AmqpVerticle {
	
	public void start(Future<Void> fut) {
		super.start();

	    moduleName = config().getJsonObject("module").getString("name", "[Unnamed]");
		capClasses = config().getJsonObject("module").getJsonArray("capabilities");
		username = config().getJsonObject("agent").getString("username", "");
		password = config().getJsonObject("agent").getString("password", "");
	
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
				return Future.<Void>future(promise -> requestAuthentication(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> publishCapabilities(promise));
			})
			.compose(v -> {
				return Future.<Void>future(promise -> subscribeToSpecifications(promise));
			});
			futInit.setHandler(res -> {
				if (res.failed()) {
					fut.fail(res.cause());
				} else {
					vertx.setPeriodic(heartbeatMs, id -> {
						/*JsonObject status = new JsonObject();
						status.put("total_tsks", taskManager.getTotalTasksNbr());
						status.put("running_tsks", taskManager.getRunningTasksNbr());
						status.put("rcvd_spec", BaseAgentVerticle.rcvdSpecs);
						status.put("sent_results", BaseAgentVerticle.sentRes);
						
						JsonObject payload = new JsonObject();
						payload.put("name", agentName);
						payload.put("status", status);
				    	Future<Void> statusFut = Future.future(promise -> publishStatus(payload.encode(), promise));
				    	statusFut.setHandler(r -> {
					        LOG.info("Status published.");
					    });*/
				    	Future<Void> capsFut = Future.future(promise -> publishCapabilities(promise));
				    	capsFut.setHandler(r -> {
					        LOG.info("Capabilities updated.");
					    });
					});
					fut.complete();
				}
			});
	}	
}
