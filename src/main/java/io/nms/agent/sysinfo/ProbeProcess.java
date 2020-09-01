package io.nms.agent.sysinfo;

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
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessSort;
import oshi.util.FormatUtil;

public class ProbeProcess extends AbstractAgentTask {	
  public final static String ERROR_PROBE_PARAMS_TOP = "probe process: invalid parameter <top>";
  public final static String ERROR_PROBE_PARAMS_PID = "probe process: invalid parameter <PID>";
  public final static String ERROR_PROBE_PID_NOTFOUND = "probe process: PID not found";
	
  private Logger LOG = LoggerFactory.getLogger(ProbeProcess.class);
  private HardwareAbstractionLayer hal;
  private OperatingSystem os;
  private String[] sort = {"CPU","MEMORY","OLDEST","NEWEST","PID","PARENTPID","NAME"};	
  
  public ProbeProcess(Message spec, JsonObject context) {
	super(spec, context);
	verb = "measure";
	name = "process";
	label = "Process execution performance";
	resultColumns = Arrays.asList(
			"count.process","count.thread",
			"process.cpu",
			"process.id","process.name","process.cmdline","process.cwd","process.user",
			"process.userId","process.group","process.groupId","process.state",
			"process.threadcount","process.priority","process.virtsize","process.rss",
			"process.kerneltime","process.usertime","process.uptime","process.starttime",
			"process.bytesread","process.byteswritten","process.memory");
	parameters.put("process.id", "<Integer>");
	parameters.put("top", "<Integer>");
	parameters.put("sort", Arrays.toString(sort));

    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");     
    SystemInfo si = new SystemInfo();
    hal = si.getHardware();
    os = si.getOperatingSystem();
  }

  @Override
  public short executeSpec() {
	LOG.info("Probe Processes...");
    resultValues.clear();
    if (specification.getResults().get(0).contains("count.")) {
      List<String> resValRow = new ArrayList<String>();
      resValRow.addAll(specification.getResults());
      int ri = resValRow.indexOf("count.thread");
      if (ri >= 0) {
        resValRow.set(ri, String.valueOf(os.getThreadCount()));
      }
      ri = resValRow.indexOf("count.process");
      if (ri >= 0) {
        resValRow.set(ri, String.valueOf(os.getProcessCount()));
      }
      resultValues.add(resValRow);
    } else if (specification.getResults().get(0).contains("process.")) {
      if (specification.getParameters().containsKey("process.id")) {
        try {
    	  String pPID = specification.getParameters().get("process.id");
          int pid = Integer.parseInt(pPID);
          OSProcess p = os.getProcess(pid);
          if (p == null) {
            errors.add(ERROR_PROBE_PID_NOTFOUND);
            return Errors.TASK_ERROR;
          }
          putProcessResultValues(p);
        } catch(NumberFormatException e) {
          errors.add(ERROR_PROBE_PARAMS_PID);
          return Errors.TASK_ERROR;
        }
      } else if (specification.getParameters().containsKey("top")) {
        try {
          String ptop = specification.getParameters().get("top");
          String psort = specification.getParameters().getOrDefault("sort", "NEWEST");
          int top = Integer.parseInt(ptop);
          if (top > os.getProcessCount()) {
            top = os.getProcessCount();
          }
          List<OSProcess> processes = Arrays.asList(os.getProcesses(top, ProcessSort.valueOf(psort)));
          for (int i = 0; i < processes.size() && i < top; i++) {
            OSProcess p = processes.get(i);
            putProcessResultValues(p);
          }
        } catch(NumberFormatException e) {
          errors.add(ERROR_PROBE_PARAMS_TOP);
          return Errors.TASK_ERROR;
        }
      }
    }
	return Errors.TASK_SUCCESS;
  }
  
  private void putProcessResultValues(OSProcess p) {
	  List<String> resValRow = new ArrayList<String>();
      resValRow.addAll(specification.getResults());     
      int ri = resValRow.indexOf("process.id");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(p.getProcessID()));
      }
      ri = resValRow.indexOf("process.name");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getName());
      }
      ri = resValRow.indexOf("process.cmdline");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getCommandLine());
      }
      ri = resValRow.indexOf("process.cwd");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getCurrentWorkingDirectory());
      }
      ri = resValRow.indexOf("process.user");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getUser());
      }
      ri = resValRow.indexOf("process.userId");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getUserID());
      }      
      ri = resValRow.indexOf("process.group");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getGroup());
      }
      ri = resValRow.indexOf("process.groupId");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getGroupID());
      }
      ri = resValRow.indexOf("process.state");
      if (ri >= 0) {
    	  resValRow.set(ri, p.getState().toString());
      }
      ri = resValRow.indexOf("process.threadcount");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(p.getThreadCount()));
      }
      ri = resValRow.indexOf("process.priority");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(p.getPriority()));
      }
      ri = resValRow.indexOf("process.virtsize");
      if (ri >= 0) {
    	  resValRow.set(ri, FormatUtil.formatBytes(p.getVirtualSize()));
      }      
      ri = resValRow.indexOf("process.rss");
      if (ri >= 0) {
    	  resValRow.set(ri, FormatUtil.formatBytes(p.getResidentSetSize()));
      }
      ri = resValRow.indexOf("process.kerneltime");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(p.getKernelTime()));
      }
      ri = resValRow.indexOf("process.usertime");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(p.getUserTime()));
      }     
      ri = resValRow.indexOf("process.uptime");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(p.getUpTime()));
      }
      ri = resValRow.indexOf("process.cpu");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(p.calculateCpuPercent()));
      }
      ri = resValRow.indexOf("process.starttime");
      if (ri >= 0) {
    	  Date stime = new Date(p.getStartTime());
    	  resValRow.set(ri, stime.toString());
      }
      ri = resValRow.indexOf("process.bytesread");
      if (ri >= 0) {
    	  resValRow.set(ri, FormatUtil.formatBytes(p.getBytesRead()));
      }
      ri = resValRow.indexOf("process.byteswritten");
      if (ri >= 0) {
    	  resValRow.set(ri, FormatUtil.formatBytes(p.getBytesWritten()));
      }
      ri = resValRow.indexOf("process.memory");
      if (ri >= 0) {
    	  GlobalMemory gm = hal.getMemory();
    	  resValRow.set(ri, String.valueOf(100d * p.getResidentSetSize() / gm.getTotal()));
      }
      resultValues.add(resValRow);
  }
}
