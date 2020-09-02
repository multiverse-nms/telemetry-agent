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
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.FormatUtil;

public class ProbeMemory extends AbstractAgentTask {
  private Logger LOG = LoggerFactory.getLogger(ProbeMemory.class);
  private HardwareAbstractionLayer hal;
	
  public ProbeMemory(Message spec, JsonObject context) {	
    super(spec, context);
    verb = "measure";
    name = "memory";
	label = "Physical memory usage";
	
	resultColumns = Arrays.asList("available.b","total.b");
	role = "admin";
    
    SystemInfo si = new SystemInfo();
    hal = si.getHardware();
  }
	
  public short executeSpec() {
    LOG.info("Probe Memory...");
    GlobalMemory gm = hal.getMemory();
    resultValues.clear();
    putPhysicalMemoryResultValues(gm);
    return Errors.TASK_SUCCESS;
  }
  
  private void putPhysicalMemoryResultValues(GlobalMemory m) { 
    List<String> resValRow = new ArrayList<String>();
	resValRow.addAll(specification.getResults());	
	int ri = resValRow.indexOf("available.b");
	if (ri >= 0) {
	  resValRow.set(ri, String.valueOf(m.getAvailable()));
	}
	ri = resValRow.indexOf("total.b");
	if (ri >= 0) {
	  resValRow.set(ri, String.valueOf(m.getTotal()));
	}
	ri = resValRow.indexOf("pagesize");
	if (ri >= 0) {
	  resValRow.set(ri, FormatUtil.formatBytes(m.getPageSize()));
	}
	resultValues.add(resValRow);
  }  
}
