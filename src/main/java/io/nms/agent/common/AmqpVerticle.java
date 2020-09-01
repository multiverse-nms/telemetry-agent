package io.nms.agent.common;

import io.nms.agent.message.Capability;
import io.nms.agent.message.Message;
import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpConnection;
import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpSender;
import io.vertx.core.Future;
import io.vertx.core.Promise;

/*
 * AMQP operations of Agent
 * extends BaseAgentVerticle 
 * provides connection to the Broker, authentication to DSS,
 * publish Capabilities to DSS, receive and process Client Specs,
 * and publish Results 
 */
public abstract class AmqpVerticle extends BaseAgentVerticle {
	protected String host = "";
	protected int port = 0; 
	
	private AmqpConnection connection = null;
	
	/* Create AMQP connection to the broker */
	protected void createAmqpConnection(Promise<Void> promise) {
		boolean isAmqp = !host.isEmpty() && (port > 0);
		if (isAmqp) {
			//LOG.info("Connecting to the messaging platform."); 
			AmqpClientOptions options = new AmqpClientOptions()
				.setHost(host)
				.setPort(port);
			AmqpClient client = AmqpClient.create(options);
			client.connect(ar -> {
				if (ar.failed()) {
					LOG.error("Unable to connect to the messaging platform", ar.cause());
					promise.fail(ar.cause());
				} else {
					LOG.info("Connected to the messaging platform.");
					connection = ar.result();
					promise.complete();
				}
			});
		} else {
			promise.fail("Wrong AMQP parameters");
		}
	}
	
	/* Agent is sender. req-rep for authentication to DSS */
	protected void requestAuthentication(Promise<Void> promise) {
		/*connection.createDynamicReceiver(replyReceiver -> {
			if (replyReceiver.succeeded()) {
				String replyToAddress = replyReceiver.result().address();
				replyReceiver.result().handler(msg -> {
					JsonObject repJson = new JsonObject(msg.bodyAsString());
					agentName = repJson.getString("name","");
					if (agentName.isEmpty()) {			
						promise.fail("Authentication failed.");
					} else {
						LOG.info("Authenticated with Agent name: "+agentName);
						promise.complete();
					}
				});
				connection.createSender("/agent/authentication", sender -> {
					if (sender.succeeded()) {
						JsonObject req = new JsonObject();
						req.put("username", username);
						req.put("password", password);
						sender.result().send(AmqpMessage.create()
						  .replyTo(replyToAddress)
						  .id("2")
						  .withBody(req.encode())
						  .build());
						LOG.info("Authenticating "+moduleName+"...");
					} else {
						promise.fail(sender.cause());
					}
				});
			} else {
				promise.fail(replyReceiver.cause());
			}
		});*/
		agentName = username;
		promise.complete();
	}
	
	/* Agent is publisher. pub-sub for Agents to send Caps. to DSS */
	protected void publishCapabilities(Promise<Void> promise) {		
		for (Capability c : capabilities) {
			if (c.getAgentId().isEmpty()) {
				c.setAgentId(agentName);
			}
			c.setEndpoint(agentName+"/"+moduleName);
			c.setSchema();
			c.setTimestampNow();
			
			String sCap  = Message.toJsonString(c, false);
			//JsonObject jCap = new JsonObject(sCap);
			
			connection.createSender("/capabilities", done -> {
				if (done.failed()) {
					promise.fail(done.cause());
				} else {
					AmqpSender sender = done.result();
					AmqpMessage msg = AmqpMessage.create().withBody(sCap).build();
					sender.send(msg);					
				}
		    });
		}
		promise.complete();
		
		
		/*JsonObject req = new JsonObject().put("capabilities", capsArray);
		connection.createSender("/capabilities", done -> {
			if (done.failed()) {
				promise.fail(done.cause());
			} else {
				AmqpSender sender = done.result();
				AmqpMessage msg = AmqpMessage.create().withBody(req.encodePrettily()).build();
				sender.send(msg);
				promise.complete();
			}
	    });*/
	}
	
	/* Agent is receiver. req-rep for Specs. from Clients */
	protected void subscribeToSpecifications(Promise<Void> promise) {
		connection.createAnonymousSender(responseSender -> {
			if (responseSender.succeeded()) {
				connection.createReceiver(agentName+"/"+moduleName+"/specifications", msg -> {
					Message req = Message.fromJsonString(msg.bodyAsString());
					if (req != null) {
						rcvdSpecs+=1;
						Message rep = processMessage(req);
						responseSender.result().send(AmqpMessage.create()
							.address(msg.replyTo())
							.correlationId(msg.id())
							.withBody(Message.toJsonString(rep, false))
							.build());
					}
				}, done -> {
					if (done.failed()) {
						promise.fail(done.cause());
					} else {
						LOG.info(moduleName+" module ready to receive Specifications.");
						promise.complete();
					}
				});
			} else {
				promise.fail(responseSender.cause());
			}
		});
	}
	
	/* Agent is publisher. pub-sub for Agents to send Results */
	protected void publishResult(String res, Promise<Void> promise) {
		Message m = Message.fromJsonString(res);
		connection.createSender(m.getEndpoint()+"/results/"+m.getRole(), done -> {
			if (done.failed()) {
				LOG.error("Unable to publish results.", done.cause());
				promise.fail(done.cause());
			} else {
				AmqpSender sender = done.result();
				AmqpMessage msg = AmqpMessage.create().withBody(res).build();
				sender.send(msg);
				sentRes+=1;
				promise.complete();
			}
	    });
	}
	
	/* Agent is publisher. pub-sub for Agents to send Status to DSS */
	protected void publishStatus(String sta, Promise<Void> promise) {
		connection.createSender("/status", done -> {
			LOG.info("Status: "+sta);
			if (done.failed()) {
				LOG.error("Unable to publish status.", done.cause());
			} else {
				AmqpSender sender = done.result();
				AmqpMessage msg = AmqpMessage.create().withBody(sta).build();
				sender.send(msg);
			}
	    });
	}
	
	@Override
	public void stop(Future stopFuture) throws Exception {	
		Future<Void> futStop = Future.future(promise -> {
			try {
				super.stop(promise);
			} catch (Exception e) {
				LOG.error("Error on stopping", e.getMessage());
				promise.fail(e.getMessage());
			}
		});
		futStop.setHandler(res -> {
			LOG.info("AMQP connection closed.");
			connection.close(stopFuture);
		});
	}

}
