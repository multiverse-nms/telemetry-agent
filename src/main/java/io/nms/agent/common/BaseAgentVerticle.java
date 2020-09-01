package io.nms.agent.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.constants.Errors;
import io.nms.agent.message.Capability;
import io.nms.agent.message.Interrupt;
import io.nms.agent.message.Message;
import io.nms.agent.message.Receipt;
import io.nms.agent.message.Result;
import io.nms.agent.message.Specification;
import io.nms.agent.taskmanager.AbstractAgentTask;
import io.nms.agent.taskmanager.AgentTaskListener;
import io.nms.agent.taskmanager.TaskManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/*
 * Base Verticle of an Agent 
 * discovers and creates locally implemented Capabilities (according to config file)
 * provides operations such as process Specification/Interrupt,
 * and listen to the Results of running Specifications 
 */
public abstract class BaseAgentVerticle extends AbstractVerticle implements AgentTaskListener {
	protected Logger LOG = LoggerFactory.getLogger(BaseAgentVerticle.class);
	
	// some stats
	public static int rcvdSpecs = 0;
	public static int sentRes = 0;
	protected JsonObject context = new JsonObject();
	
	/* metadata */
	protected String agentName = "";
	
	// provided by config file
	protected String moduleName = "[Unnamed]";
	protected JsonArray capClasses = new JsonArray();
	protected JsonObject moduleConfig = new JsonObject();
	
	// TODO: put in config file
	protected int heartbeatMs = 60000;
	
	// reference to task manager
	protected TaskManager taskManager;
	
	// stores created Specification tasks
	protected HashMap<String, AbstractAgentTask> tasks = new HashMap<String, AbstractAgentTask>();
	
	// task creators (constructors)
	protected Map<String, Constructor<?>> taskCreators = new HashMap<String,Constructor<?>>();
	
	// stores available local capabilities
	protected List<Capability> capabilities = new ArrayList<Capability>();
	
	// child Verticle implements these to communicate
	protected abstract void publishResult(String res, Promise<Void> prom);
	protected abstract void subscribeToSpecifications(Promise<Void> prom);
	protected abstract void publishCapabilities(Promise<Void> prom);
	
	@Override
	public void start() {
		taskManager = TaskManager.getInstance();
	}
	
	// retrieve local capabilities and create their constructors
	protected void initModule(Promise<Void> promise) {
		context.put("config", moduleConfig);
		LOG.info("Module config: " + moduleConfig.encodePrettily());
		/* agent configuration through module goes here */
		LOG.info("Module initialized.");
		promise.complete();
	}
	
	// retrieve local capabilities and create their constructors
	protected void createCapabilities(Promise<Void> promise) {
		capabilities.clear();
		for (int i = 0; i < capClasses.size(); i++) {
			String className = "";
			try {
				className = capClasses.getString(i);
				Class<?> clazz = Class.forName(className);
				Constructor<?> ctor = clazz.getConstructor(Message.class, JsonObject.class);
				AbstractAgentTask t = createTask(ctor, new Message(), new JsonObject());
				if (t != null) {
					taskCreators.put(t.getCapability().getName(), ctor);
					capabilities.add(t.getCapability());
				}
			}
			catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				LOG.error("Task not found: " + className);
				continue;
			}
		}
		LOG.info("Capabilities created.");
		promise.complete();
	}
	
	protected Message processMessage(Message msg) {
		if (msg.getMsgType() == Message.Type.SPECIFICATION) {
			Specification spec = (Specification) msg;
			if (!spec.check()) {
				Message rct = new Receipt(spec);
				rct.setErrors(spec.getErrors());
				rct.setTimestampNow();
				return rct;
			}
			return processSpecification(spec);
		}
		if (msg.getMsgType() == Message.Type.INTERRUPT) {			
			return processInterrupt((Interrupt) msg);
		}
		Message rct = new Receipt(msg);
		rct.setErrors(Arrays.asList(Errors.TaskManager.MSG_UNSUPPORTED));
		rct.setTimestampNow();
		return rct;
	}
	
	private Message processSpecification(Specification spec) {
		LOG.info("Process Specification.");
		LOG.info("Module config: " + moduleConfig.encodePrettily());
		AbstractAgentTask task = createTask(taskCreators.get(spec.getName()), spec, context);
		if (task == null) {
			Message rct = new Receipt(spec);
			rct.setErrors(Arrays.asList(Errors.TaskManager.TASK_NOT_FOUND));
			rct.setTimestampNow();
			return rct;
		}
		if (task.check() != Errors.TASK_SUCCESS) {
			Message rct = new Receipt(spec);
			rct.setErrors(task.getErrors());
			rct.setTimestampNow();
			return rct;
		}
		
		// use (token + capability name) as task ID:
		//String taskId = spec.getToken() + "-" + spec.getName();
		
		// use spec.schema as task id
		String taskId = spec.getSchema();
		
		LOG.info("Corresponding task found and checked: "+taskId);
		
		// for now, accept only periodic tasks
		if (spec.isPeriodic()) {
			
			// if already exists, stop and remove
			if (tasks.containsKey(taskId)) {
				LOG.info("Corresponding task already running.");
				tasks.get(taskId).incrementSpecNbr();
				//taskManager.cancel(taskId);
				//tasks.remove(taskId);
			} else {
				LOG.info("Create new task.");
				// set new task
				task.setTaskId(taskId);
				task.registerTaskListener(this);
				tasks.put(taskId, task);
			
				// schedule task and store reference
				taskManager.submit(task, task.getInitialDelayMs(), task.getTaskPeriodMs());
			}
			// create receipt
			Message rct = new Receipt(spec);
			rct.setTimestampNow();
			return rct;
		}
		Message rct = new Receipt(spec);
		rct.setErrors(Arrays.asList(Errors.TaskManager.WHEN_UNSUPPORTED));
		rct.setTimestampNow();
		return rct;
	}
	
	private Message processInterrupt(Interrupt itr) {
		LOG.info("Process Interrupt.");
		Message rct = new Receipt(itr);		
		String taskId = itr.getSchema();
		if (tasks.containsKey(taskId)) {
			if (tasks.get(taskId).decrementSpecNbr() == 0) {
				LOG.info("Stop and remove task.");
				taskManager.cancel(taskId);
				tasks.remove(taskId);
			}
		}
		rct.setTimestampNow();
		return rct;
	}
	
	private AbstractAgentTask createTask(Constructor<?> ctor, Message spec, JsonObject cont) {
		if (ctor == null) {
			return null;
		}
		AbstractAgentTask aTask;
		try {
			aTask = (AbstractAgentTask) ctor.newInstance(new Object[] { spec, cont });
			return aTask;
		} catch (InstantiationException e1) {
			e1.printStackTrace();
			return null;
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
			return null;
		} catch (InvocationTargetException e1) {
			e1.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void onResult(String taskId, short resultCode, Timestamp ts) {
		AbstractAgentTask t = tasks.get(taskId);
		if (t == null) {
			LOG.info("No task found for: "+taskId);
			return;
		}
		if (resultCode == Errors.TASK_SUCCESS) {
			Message res = new Result(t.getSpecification());
			res.setResultValues(t.getResultValues());
			res.setTimestamp(ts.toString());
			
			final Promise<Void> pub = Promise.promise();
			pub.future().onComplete(pubRes -> {
				if (pubRes.succeeded()) {
					LOG.info("Result published.");
				} else {
					LOG.info("Failed to publish result.");
				}
			});
			publishResult(Message.toJsonString(res, false), pub);
		} else {
			LOG.warn("Something went wrong with task " + taskId);
		}
	}
	@Override
	public void onFinished(String taskId) {
		LOG.info("Task terminated: " + taskId);
		taskManager.cancel(taskId);
		tasks.remove(taskId);
	}
	
	@Override
	public void stop(Future stopFuture) throws Exception {
		Promise<Void> futCancelTasks = Promise.promise();
		futCancelTasks.future().onComplete(res -> {
			// TODO: publish Receipt...
			LOG.info("Running tasks canceled.");
			tasks.clear();
			try {
				super.stop(stopFuture);
			} catch (Exception e) {
				LOG.error("Error on stopping", e.getMessage());
			}
		});
		taskManager.cancel(tasks.keySet(), futCancelTasks);
	}
}
