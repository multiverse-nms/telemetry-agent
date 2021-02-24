package io.nms.agent.sysinfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.constants.Errors;
import io.nms.agent.taskmanager.AbstractAgentTask;
import io.nms.agent.message.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProbeProcesses extends AbstractAgentTask {

  public static final int WINDOWS_OS = 0;
  public static final int LINUX_OS = 1;
  public static final int MAC_OS = 2;
  public static final int SOLARIS_OS = 3;
  public static final int UNKNOWN_OS = -1;

  private Logger LOG = LoggerFactory.getLogger(ProbeProcesses.class);
	
  public ProbeProcesses(Message spec, JsonObject context) {	
    super(spec, context);
    verb = "collect";
    name = "processes";
	  label = "Probe running processes on the host system";
	  resultColumns = Arrays.asList("pid", "username", "name", "state", "threads", "total_size");
	  role = "admin";
  }
	
  public short executeSpec() {
    LOG.info("Asking osquery...");
    JsonArray json  = executeCommand();
    if (json == null) {
      return Errors.TASK_ERROR;
    }
    putProcessesResultValues(json);
    return Errors.TASK_SUCCESS;
  }
  
  private void putProcessesResultValues(JsonArray json) {
    resultValues.clear();
    for (int i = 0; i < json.size(); i++) {
      JsonObject row = json.getJsonObject(i);
      List<String> resValRow = new ArrayList<String>();
	    resValRow.addAll(specification.getResults());	
	    int ri = resValRow.indexOf("pid");
	    if (ri >= 0) {
	      resValRow.set(ri, String.valueOf(row.getString("pid")));
      }
      ri = resValRow.indexOf("username");
	    if (ri >= 0) {
	      resValRow.set(ri, String.valueOf(row.getString("username")));
      }
	    ri = resValRow.indexOf("name");
	    if (ri >= 0) {
	      resValRow.set(ri, String.valueOf(row.getString("name")));
      }
      ri = resValRow.indexOf("state");
	    if (ri >= 0) {
	      resValRow.set(ri, String.valueOf(row.getString("state")));
      }
      ri = resValRow.indexOf("threads");
	    if (ri >= 0) {
	      resValRow.set(ri, String.valueOf(row.getString("threads")));
      }
      ri = resValRow.indexOf("total_size");
	    if (ri >= 0) {
	      resValRow.set(ri, String.valueOf(row.getString("total_size")));
	    }
      resultValues.add(resValRow);
    }
  }
  
  protected JsonArray executeCommand() {
    List<String> commands = new ArrayList<String>();
    int os = getOs();
    switch (os) {
      case WINDOWS_OS:
        commands.add("cmd");
        commands.add("/c");
        break;
      case LINUX_OS:
        commands.add("/bin/sh");
        commands.add("-c");
        break;
      default:
        errors.add("os not supported");
        return null;
    }
    commands.add("osqueryi"); 
    commands.add("--json");
    commands.add("SELECT pid, parent, username, name, state, threads, total_size "
     + "FROM processes p JOIN users u ON u.uid = p.uid "
     + "WHERE name != 'osqueryi.exe' ORDER BY start_time DESC LIMIT 10;");

    try {
      ProcessBuilder pb = new ProcessBuilder(commands);
      pb.redirectError();
      Process p = pb.start();

      int exitVal = p.waitFor();

      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      String raw = "";
        while ((line = reader.readLine()) != null) {
          raw = raw + line;
        }
        JsonArray json = new JsonArray(raw);
        if (exitVal == 0) {
          return json;
        } else {
          errors.add("failed to execute osquery command");
          return null;
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        errors.add(e.toString());
        return null;
      }
}

private int getOs() {
  String os = System.getProperty("os.name").toLowerCase();
  if (os.indexOf("win") >= 0) {
    return WINDOWS_OS;
  } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0) {
    return LINUX_OS;
  } else if (os.indexOf("mac") >= 0) {
    return MAC_OS;
  } else if (os.indexOf("sunos") >= 0) {
    return SOLARIS_OS;
  } else {
    return UNKNOWN_OS;
  }
}
}