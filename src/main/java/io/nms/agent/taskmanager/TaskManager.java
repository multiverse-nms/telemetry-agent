package io.nms.agent.taskmanager;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Promise;

public class TaskManager {
	private static TaskManager instance = new TaskManager();
	
	// stores running tasks
	private static HashMap<String, java.util.concurrent.Future<?>> runningTasks 
				= new HashMap<String, java.util.concurrent.Future<?>>(); 
	
	private static ScheduledExecutorService scheduler 
				= Executors.newScheduledThreadPool(1);
	
	private static int totalTasksNbr = 0;
	
	private TaskManager() {}
  
	public static TaskManager getInstance() {
		return instance; 
	}
	
	// returns always true, more features later
	public boolean submit(AbstractAgentTask task, long initialDelay, long period) {
		java.util.concurrent.Future<?> f = 
			scheduler.scheduleWithFixedDelay(task, initialDelay, period, TimeUnit.MILLISECONDS);
		runningTasks.put(task.getTaskId(), f);
		totalTasksNbr+=1;
		return true;
	}
	
	// returns true if found, false otherwise
	public boolean cancel(String taskId) {
		java.util.concurrent.Future<?> taskRef = runningTasks.get(taskId);
		if (taskRef != null) {
			taskRef.cancel(true);
			runningTasks.remove(taskId);
			return true;
		}
		return false;
	}
	
	// cancels a list of tasks
	public void cancel(Set<String> taskIdList, Promise<Void> future) {
		for(String taskId : taskIdList){
			java.util.concurrent.Future<?> taskRef = runningTasks.get(taskId);
			if (taskRef != null) {
				taskRef.cancel(true);
				runningTasks.remove(taskId);
			}
		}
		future.complete();
	}
	
	public int getTotalTasksNbr() {
		return totalTasksNbr;
	}
	
	public int getRunningTasksNbr() {
		return runningTasks.size();
	}
}
