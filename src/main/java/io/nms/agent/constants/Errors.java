package io.nms.agent.constants;

public class Errors {
	public final static int TASK_SUCCESS = 0;
	public final static int TASK_ERROR = -1;
	
	public class Task {
		public final static String PARAM_UNSUPPORTED = "Unsupported parameter(s)";	
		public final static String SPEC_UNSUPPORTED = "Unsupported Specification";
		public final static String WHEN_UNSUPPORTED = "Unsupported temporal scope";
		public final static String SPEC = "Error in Specification message";
	}
	
	public class TaskManager{
		public final static String WHEN_UNSUPPORTED = "Unsupported temporal scope";
		public final static String TASK_NOT_FOUND = "Task not found";
		public final static String MSG_UNSUPPORTED = "Unsupported message";
	}
}
