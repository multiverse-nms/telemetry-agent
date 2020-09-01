package io.nms.agent.taskmanager;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.constants.Errors;
import io.nms.agent.message.Capability;
import io.nms.agent.message.Message;
import io.vertx.core.json.JsonObject;

/*
 * Specification Task Abstraction
 * Responsible for providing ONe Capability 
 * and processing corresponding Specifications
 * */
public abstract class AbstractAgentTask extends Thread {
	protected Logger LOG = LoggerFactory.getLogger(AbstractAgentTask.class);
	
	private String taskId;
	
	// specification to execute
	protected Message specification;
	protected int specNbr = 0;
	protected JsonObject context;
  
	// task execution results   
	protected List<List<String>> resultValues = new ArrayList<List<String>>();
	// listener to send results to
	protected AgentTaskListener tListener = null;
	
	// potential execution errors
	protected List<String> errors = new ArrayList<String>();
		
	// to create the Capability (see NMS doc)
	protected Map<String, String> parameters = new HashMap<String,String>();
	protected List<String> resultColumns = new ArrayList<String>();
	protected String label = "undefined";
	protected String name = "undefined";
	protected String verb = "undefined";
	
	protected String agentId = "";
	
	// TODO: put in config file
	protected String role = "admin";
	
	protected Date taskStart;
	protected Date taskStop;
	protected int taskPeriodMs = 5000;
	
	// implement this to execute the task
	protected abstract short executeSpec();
	
	public AbstractAgentTask(Message specification, JsonObject context) {
		this.taskId = "unknown";
		this.specification = specification;
		this.context = context;
		this.specNbr = 1;
	}
	
	// used to receive results
	public void registerTaskListener(AgentTaskListener tl) {
		this.tListener = tl;
	}
	
	// create and get the Capability of the implemented task
	public Capability getCapability() {
		Capability capability = new Capability();
		capability.setName(name);
		capability.setAgentId(agentId);
		capability.setLabel(label);
		capability.setVerb(verb);
		capability.setWhen(Message.Format.WHEN + String.valueOf(taskPeriodMs));
		capability.setResults(resultColumns);
		capability.setParameters(parameters);
		capability.setRole(role);
		//capability.setTimestampNow();
		return capability;
	}
	
	/* Must be called before submit task
	 * TODO: improve and use hash schema to compare */
	public short check() {
		// if spec is correct
		if (!specification.check()) {
			this.errors.add(Errors.Task.SPEC);
			return Errors.TASK_ERROR;
		}
		
		// if name is supported
		if (!specification.getName().equals(name)) {
			this.errors.add(Errors.Task.SPEC_UNSUPPORTED);
			return Errors.TASK_ERROR;
		}
		
		/* TODO: support taskStart in future, currently supports only 'now' */
		
		// check specification period
		if (specification.getPeriod() < taskPeriodMs) {
			this.errors.add(Errors.Task.WHEN_UNSUPPORTED);
			return Errors.TASK_ERROR;
		} else {
			LOG.info("Use Specification period.");
			taskPeriodMs = specification.getPeriod();
		}
		
		// if measure: 'now' <= start < stop 
		if (verb.equals("measure")) {
			// taskStart = 'now'
			LOG.info("Measurement will stop as in Specification.");
			taskStop = specification.getStop();
	
		// if collect: may stop in the past
		} else if (verb.equals("collect")) {
			// spec stops in the future, use it
			if (specification.getStop().after(new Date())) {
				LOG.info("Collection will stop as in Specification.");
				taskStop = specification.getStop();
				
			// spec stops in the past, use one period in the future
			} else {
				LOG.info("Collection with Specification in the past.");
				// stop = now + one_period + short_safety_time
				taskStop = Date.from(Instant.now().plusMillis(taskPeriodMs + 500));
			}
		}
		return Errors.TASK_SUCCESS;
	}
	
	public long getInitialDelayMs() {
		if (specification.isFuture()) {
		return new Date().toInstant()
			.until(specification.getStart().toInstant(), ChronoUnit.MILLIS);
		}
		return 0;
	}
	
	public long getTaskPeriodMs() {
		return taskPeriodMs;
	}
	
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	
	public String getTaskId() {
		return taskId;
	}
	
	public List<List<String>> getResultValues() {
		return resultValues;
	}
	public List<String> getErrors() {
		return errors;
	}
  
	public Message getSpecification() {
		return specification;
	}
	public void setSpecification(Message specification) {
		this.specification = specification;
	}
	
	public int incrementSpecNbr() {
		this.specNbr+=1;
		return this.specNbr;
	}
	
	public int decrementSpecNbr() {
		this.specNbr-=1;
		return this.specNbr;
	}
	
	@Override
	public void run() {
		if (tListener == null) {
			LOG.warn("No listener registered for task results.");
			return;
		}
		// if one delayed execution only
		/*if (specification.getStop() == null) {
			Timestamp ts = new Timestamp(new Date().getTime());
			tListener.onResult(taskId, executeSpec(), ts);
			tListener.onFinished(taskId);
		// if periodic execution
		} else*/ 
		LOG.info("Run task with context: "+context.encodePrettily());
		if (this.taskStop.before(new Date())) {
			LOG.info("Task terminated.");
			tListener.onFinished(taskId);
			return;
		} else {
			short resultCode = executeSpec();
			Timestamp ts = new Timestamp(new Date().getTime());
			if (resultCode == Errors.TASK_SUCCESS) {
				this.tListener.onResult(taskId, resultCode, ts);
			} else {
				LOG.warn("Something went wrong with task "+taskId);
			}
		}
	}
}
