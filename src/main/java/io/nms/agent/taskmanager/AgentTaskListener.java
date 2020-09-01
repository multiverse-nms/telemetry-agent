package io.nms.agent.taskmanager;

import java.sql.Timestamp;

public interface AgentTaskListener {
  //void onResult(String taskId, short resultCode); 
  void onResult(String taskId, short resultCode, Timestamp ts); 
  void onFinished(String taskId); 
}
