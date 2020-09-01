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
	/*resultColumns = Arrays.asList(
			"physical.available","physical.total","physical.pagesize",
			"virtual.swaptotal","virtual.swapused","virtual.swappagesin","virtual.swappagesout");*/
	
	resultColumns = Arrays.asList("available.b","total.b");
	role = "admin";
	
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");     
    SystemInfo si = new SystemInfo();
    hal = si.getHardware();
  }
	
  public short executeSpec() {
    LOG.info("Probe Memory...");
    GlobalMemory gm = hal.getMemory();
    resultValues.clear();
    /*if (specification.getResults().get(0).contains("physical.")) {
      putPhysicalMemoryResultValues(gm);
    } else if (specification.getResults().get(0).contains("virtual.")) {
      putVirtualMemoryResultValues(gm);
    }*/
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
  
  private void putVirtualMemoryResultValues(GlobalMemory m) { 
    List<String> resValRow = new ArrayList<String>();
	resValRow.addAll(specification.getResults());
	int ri = resValRow.indexOf("virtual.swaptotal");
	if (ri >= 0) {
	  resValRow.set(ri, FormatUtil.formatBytes(m.getSwapTotal()));
	}
	ri = resValRow.indexOf("virtual.swapused");
	if (ri >= 0) {
	  resValRow.set(ri, FormatUtil.formatBytes(m.getSwapUsed()));
	}
	ri = resValRow.indexOf("virtual.swappagesin");
	if (ri >= 0) {
	  resValRow.set(ri, String.valueOf(m.getSwapPagesIn()));
	}
	ri = resValRow.indexOf("virtual.swappagesout");
	if (ri >= 0) {
	  resValRow.set(ri, String.valueOf(m.getSwapPagesOut()));
	}
	resultValues.add(resValRow);
  }
}
