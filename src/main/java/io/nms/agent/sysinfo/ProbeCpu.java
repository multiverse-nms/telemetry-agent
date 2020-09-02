package io.nms.agent.sysinfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.constants.Errors;
import io.nms.agent.message.Message;
import io.nms.agent.taskmanager.AbstractAgentTask;
import io.vertx.core.json.JsonObject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

/*
 * Implementation of a Capability
 * Extends AbstractAgentTask
 * Implements executeSpec to perform actual measurement
 * */
public class ProbeCpu extends AbstractAgentTask {
	private Logger LOG = LoggerFactory.getLogger(ProbeCpu.class);
	private HardwareAbstractionLayer hal;
	
	public ProbeCpu(Message spec, JsonObject context) {
		super(spec, context); 
		verb = "measure";
		name = "cpu";
		label = "CPU performance and info";		
		resultColumns = Arrays.asList("systemcpuload.pc","contextswitches.n");
		role = "admin"; 
		SystemInfo si = new SystemInfo();
		hal = si.getHardware();
	}
	
	// implementation of Specification exec
	protected short executeSpec() {
		LOG.info("Probe CPU...");
		CentralProcessor cp = hal.getProcessor();
		resultValues.clear();
		putPhysicalCpuResultValues(cp);
		return Errors.TASK_SUCCESS;
	}
  
	private void putPhysicalCpuResultValues(CentralProcessor c) { 
		List<String> resValRow = new ArrayList<String>();
		resValRow.addAll(specification.getResults());		
		int ri = resValRow.indexOf("contextswitches.n");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getContextSwitches()));
		}
		ri = resValRow.indexOf("systemcpuload.pc");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getSystemCpuLoadTicks()[0]*100.0));
		}
		resultValues.add(resValRow);
	}
}
