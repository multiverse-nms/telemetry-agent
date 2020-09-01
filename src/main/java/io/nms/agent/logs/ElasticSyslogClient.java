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
public class ElasticSyslogClient extends AbstractAgentTask {
	private Logger LOG = LoggerFactory.getLogger(ElasticSyslogClient.class);
	public static OkHttpClient client = new OkHttpClient();
	//private Date latestLogTimestamp;
	private String baseUrl = "http://10.11.200.125:9200/_search?q="
			+ "_index:filebeat-* AND "
			+ "event.dataset:system.syslog AND ";
	
	// http://10.11.200.125:9200/_search?
	// q=_index:filebeat-* AND 
	// event.dataset:system.syslog AND 
	// @timestamp:[2019-05-23 TO 2019-10-30] AND
	// host.name:dns
	// &
	// size=50
	
	public ElasticSyslogClient(Message spec, JsonObject context) {
		super(spec, context); 
		verb = "collect";
		name = "syslog";
		label = "Collect (remote) syslog";
		resultColumns = Arrays.asList("pid","timestamp","program","hostname","message");
		parameters.put("hostname", "String");
		taskPeriodMs = 60000;
		role = "admin";
		
		//latestLogTimestamp = new Date();
	}
	
	// implementation of Specification exec
	protected short executeSpec() {
		LOG.info("Requesting Filebeat...");
		resultValues.clear();
		
		Timestamp bTs = new Timestamp(specification.getStart().getTime());
		Timestamp eTs = new Timestamp(specification.getStop().getTime());
		
		String qTime = "@timestamp:[" + bTs.toString().split(" ")[0] + " TO " + eTs.toString().split(" ")[0] + "]";
		
		String qUrl = baseUrl + qTime;
		
		String hostname = specification.getParameter("hostname");
		if (!hostname.isEmpty()) {
			qUrl+=" AND host.name:"+hostname;
	    }
		
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
					.getJsonObject("_source")
					.getJsonObject("system")
					.getJsonObject("syslog");
				putResultValues(res);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return Errors.TASK_ERROR;
		}
		LOG.info("ResultValues: "+this.resultValues.size());
		return Errors.TASK_SUCCESS;
	}
	
	private void putResultValues(JsonObject result) {
		List<String> resValRow = new ArrayList<String>();
		resValRow.addAll(specification.getResults());
		int ri = resValRow.indexOf("pid");
		if (ri >= 0) {
			resValRow.set(ri, result.getString("pid"));
		}
		ri = resValRow.indexOf("timestamp");
		if (ri >= 0) {
			resValRow.set(ri, result.getString("timestamp"));
		}
		ri = resValRow.indexOf("program");
		if (ri >= 0) {
			resValRow.set(ri, result.getString("program"));
		}
		ri = resValRow.indexOf("hostname");
		if (ri >= 0) {
			resValRow.set(ri, result.getString("hostname"));
		}
		ri = resValRow.indexOf("message");
		if (ri >= 0) {
			resValRow.set(ri, result.getString("message"));
		}
		resultValues.add(resValRow);
	}
}
