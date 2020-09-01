package io.nms.agent.logs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.constants.Errors;
import io.nms.agent.taskmanager.AbstractAgentTask;
import io.nms.agent.message.Message;
import io.vertx.core.json.JsonObject;

/*
 * Implementation of a Capability
 * Extends AbstractAgentTask
 * Implements executeSpec
 * */
public class LocalSyslogReader extends AbstractAgentTask {
	private Logger LOG = LoggerFactory.getLogger(LocalSyslogReader.class);
	
	Date latestLogTimestamp;
	
	public LocalSyslogReader(Message spec, JsonObject context) {
		super(spec, context); 
		verb = "collect";
		name = "syslog";
		label = "Collect Agent Syslog";
		resultColumns = Arrays.asList("msg");
		taskPeriodMs = 60000;
		//this.specification.setPeriod(10000);
		
		latestLogTimestamp = new Date();
		
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
		System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");
	}
	
	// implementation of Specification exec
	protected short executeSpec() {
		LOG.info("Collecting Syslog...");
		resultValues.clear();
		
		List<String> commands = new ArrayList<String>();
	    commands.add("/bin/sh");
	    commands.add("-c");
	    commands.add("sudo grep sudo /var/log/auth.log | tail -10");
		
		try {
		    ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectError();
     
            Process p = pb.start();
		
			BufferedReader reader = 
					new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] logLine = line.split(" ", 3);
				
				SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
				Date timestamp = formatter.parse(logLine[0]+" "+logLine[1]);
				
				if (timestamp.after(latestLogTimestamp)) {
					LOG.info(logLine[2]);
					List<String> resValRow = new ArrayList<String>();
					//resValRow.addAll(Arrays.asList(timestamp.toString(), logLine[2]));
					resValRow.add(line);
					resultValues.add(resValRow);
					latestLogTimestamp = timestamp;
				}
			}
			int exitVal = p.waitFor();
			if ((exitVal == 0) && (!resultValues.isEmpty())) {
				return Errors.TASK_SUCCESS;
			} else {
				return Errors.TASK_ERROR;
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return Errors.TASK_ERROR;
		} catch (ParseException e) {
			e.printStackTrace();
			return Errors.TASK_ERROR;
		}
	}
}
