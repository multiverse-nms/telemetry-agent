package io.nms.agent.logs;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.constants.Errors;
import io.nms.agent.taskmanager.AbstractAgentTask;
import io.nms.agent.message.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Implementation of a Capability
 * Extends AbstractAgentTask
 * Implements executeSpec
 * */
public class ElasticPacketClient extends AbstractAgentTask {
	private Logger LOG = LoggerFactory.getLogger(ElasticPacketClient.class);
	public static OkHttpClient client = new OkHttpClient();
	//private Date latestLogTimestamp;
	private String baseUrl = "http://10.11.200.125:9200/_search?q="
			+ "_index:packetbeat-* AND ";
	
	// http://10.11.200.125:9200/_search?
	// q=_index:filebeat-* AND  
	// @timestamp:[2019-05-23 TO 2019-10-30] AND
	// host.name:dns
	// &
	// size=50
	
	public ElasticPacketClient(Message spec, JsonObject context) {
		super(spec, context); 
		verb = "collect";
		name = "traffic";
		label = "Collect packet traces";
		resultColumns = Arrays.asList("timestamp",
				"bytes.b","transport","protocol", "type",
				"sourceip","sourceport","destinationip", "destinationport");
		//parameters.put("hostname", "String");
		role = "admin";
		
		taskPeriodMs = 60000;	
		//latestLogTimestamp = new Date();
	}
	
	// implementation of Specification exec
	protected short executeSpec() {
		LOG.info("Requesting Packebeat...");
		resultValues.clear();
		
		Timestamp bTs = new Timestamp(specification.getStart().getTime());
		Timestamp eTs = new Timestamp(specification.getStop().getTime());
		
		String qTime = "@timestamp:[" + bTs.toString().split(" ")[0] + " TO " + eTs.toString().split(" ")[0] + "]";
		
		String qUrl = baseUrl + qTime;
		
		/* TODO: as parameter */
		qUrl+="&size=50";
		
		LOG.info("qURL: " + qUrl);
		
		Request request = new Request.Builder()
			.url(qUrl)
			.build();
			 
		Response reply;
		try {
			reply = client.newCall(request).execute();
			JsonObject response = new JsonObject(reply.body().string());
			final JsonArray results = response.getJsonObject("hits").getJsonArray("hits");
			for (int i = 0; i < results.size(); i++) {
				JsonObject res = results.getJsonObject(i)
					.getJsonObject("_source");
				putResultValues(res);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return Errors.TASK_ERROR;
		}
		//LOG.info("ResultValues size: "+this.resultValues.size());
		return Errors.TASK_SUCCESS;
	}
	
	private void putResultValues(JsonObject result) {
		//LOG.info("put: "+result.encodePrettily());
		List<String> resValRow = new ArrayList<String>();
		resValRow.addAll(specification.getResults());
		
		int ri = resValRow.indexOf("timestamp");
		if (ri >= 0) {
			resValRow.set(ri, result.getString("@timestamp"));
		}
		JsonObject network = result.getJsonObject("network");
		ri = resValRow.indexOf("bytes.b");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(network.getInteger("bytes")));
		}
		ri = resValRow.indexOf("transport");
		if (ri >= 0) {
			resValRow.set(ri, network.getString("transport"));
		}
		ri = resValRow.indexOf("protocol");
		if (ri >= 0) {
			resValRow.set(ri, network.getString("protocol"));
		}
		ri = resValRow.indexOf("type");
		if (ri >= 0) {
			resValRow.set(ri, network.getString("type"));
		}
		
		JsonObject source = result.getJsonObject("source");
		ri = resValRow.indexOf("sourceip");
		if (ri >= 0) {
			resValRow.set(ri, source.getString("ip"));
		}
		ri = resValRow.indexOf("sourceport");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(source.getInteger("port")));
		}
		
		JsonObject destination = result.getJsonObject("destination");
		ri = resValRow.indexOf("destinationip");
		if (ri >= 0) {
			resValRow.set(ri, destination.getString("ip"));
		}
		ri = resValRow.indexOf("destinationport");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(destination.getInteger("port")));
		}
		resultValues.add(resValRow);
	}
}
