package io.nms.agent.common;

import java.util.ArrayList;
import java.util.List;

import io.nms.agent.message.Capability;
import io.nms.agent.message.Message;
import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpConnection;
import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpReceiver;
import io.vertx.amqp.AmqpSender;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;

/*
 * AMQP operations of Agent
 * extends BaseAgentVerticle 
 * provides connection to the Broker,
 * publish Capabilities, receive and process operator's Specifications and publish Results 
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
	
	/* Agent is publisher. pub-sub for Agents to send Caps. to DSS */
	protected void publishCapabilities(Promise<Void> promise) {		
		List<Future> fCaps = new ArrayList<>();
		for (Capability c : capabilities) {
			Promise<Void> p = Promise.promise();
			fCaps.add(p.future());

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
					p.fail(done.cause());
				} else {
					AmqpSender sender = done.result();
					AmqpMessage msg = AmqpMessage.create().withBody(sCap).build();
					sender.send(msg);
					p.complete();					
				}
		    });
		}
		CompositeFuture.all(fCaps).map((Void) null).onComplete(promise);
	}
	
	/* Agent is receiver. req-rep for Specs. from Clients */
	protected void subscribeToSpecifications(Promise<Void> promise) {
		connection.createAnonymousSender(responseSender -> {
			if (responseSender.succeeded()) {
				connection.createReceiver(agentName+"/"+moduleName+"/specifications", ar -> {
					if (ar.succeeded()) {
						AmqpReceiver receiver = ar.result();
						receiver.handler(msg -> {
							Message req = Message.fromJsonString(msg.bodyAsString());
							if (req != null) {
								Message rep = processMessage(req);
								responseSender.result().send(AmqpMessage.create()
									.address(msg.replyTo())
									.correlationId(msg.id())
									.withBody(Message.toJsonString(rep, false))
									.build());
							}
						});
						LOG.info(moduleName+" module ready to receive Specifications.");
						promise.complete();
					} else {
						promise.fail(ar.cause());
					}
				});
			} else {
				promise.fail(responseSender.cause());
			}
		});
	}

	protected void subscribeToSpecifications2(Promise<Void> promise) {
		connection.createReceiver(agentName+"/"+moduleName+"/specifications", ar -> {
			if (ar.succeeded()) {
				AmqpReceiver receiver = ar.result();
				receiver.handler(msg -> {
					Message req = Message.fromJsonString(msg.bodyAsString());
					if (req != null) {
						Message rep = processMessage(req);

						connection.createSender(rep.getEndpoint()+"/specifications/receipt", done -> {
							if (done.failed()) {
								LOG.error("Unable to publish receipts.", done.cause());
							} else {
								AmqpSender sender = done.result();
								AmqpMessage receiptBody = 
										AmqpMessage.create().withBody(Message.toJsonString(rep, false)).build();
								sender.send(receiptBody);
							}
						});
					}
				});
				LOG.info(moduleName+" module ready to receive Specifications.");
				promise.complete();
			} else {
				promise.fail(ar.cause());
			}
		});
		connection.createAnonymousSender(responseSender -> {
			if (responseSender.succeeded()) {
				
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
	public void stop(Future<Void> stopFuture) throws Exception {	
		Future<Void> futStop = Future.future(promise -> {
			try {
				super.stop(promise);
			} catch (Exception e) {
				LOG.error("Error on stopping", e.getMessage());
				promise.fail(e.getMessage());
			}
		});
		futStop.onComplete(res -> {
			LOG.info("AMQP connection closed.");
			connection.close(stopFuture);
		});
	}

}
