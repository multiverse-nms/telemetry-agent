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
import oshi.util.FormatUtil;

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
		/*resultColumns = Arrays.asList(
			"physical.vendor","physical.name","physical.processorId","physical.identifier",
			"physical.is64","physical.model","physical.family",
			"physical.vendorfreq","physical.maxfreq","physical.currentfreq",
			"physical.stepping",
			"physical.systemcpuload","physical.systemuptime",
			"physical.logprocessorcount","physical.phyprocessorcount",
			"physical.contextswitches","physical.intrpt");*/
		
		resultColumns = Arrays.asList("systemcpuload.pc","contextswitches.n");
		role = "admin";
 
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
		System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");     
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
		int ri = resValRow.indexOf("vendor");
		if (ri >= 0) {
			resValRow.set(ri, c.getVendor());
		}
		ri = resValRow.indexOf("name");
		if (ri >= 0) {
			resValRow.set(ri, c.getName());
		}
		ri = resValRow.indexOf("processorId");
		if (ri >= 0) {
			resValRow.set(ri, c.getProcessorID());
		}
		ri = resValRow.indexOf("identifier");
		if (ri >= 0) {
			resValRow.set(ri, c.getIdentifier());
		}
		ri = resValRow.indexOf("is64");
		if (ri >= 0) {
			resValRow.set(ri, Boolean.toString(c.isCpu64bit()));
		}
		ri = resValRow.indexOf("model");
		if (ri >= 0) {
			resValRow.set(ri, c.getModel());
		}
		ri = resValRow.indexOf("family");
		if (ri >= 0) {
			resValRow.set(ri, c.getFamily());
		}
		ri = resValRow.indexOf("vendorfreq");
		if (ri >= 0) {
			resValRow.set(ri, FormatUtil.formatHertz(c.getVendorFreq()));
		}
		ri = resValRow.indexOf("maxfreq");
		if (ri >= 0) {
			resValRow.set(ri, FormatUtil.formatHertz(c.getVendorFreq()));
		}
		ri = resValRow.indexOf("currentfreq");
		if (ri >= 0) {
			resValRow.set(ri, FormatUtil.formatHertz(c.getVendorFreq()));
		}
		ri = resValRow.indexOf("stepping");
		if (ri >= 0) {
			resValRow.set(ri, c.getStepping());
		}
		ri = resValRow.indexOf("systemcpuload.pc");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getSystemCpuLoad()*100.0));
		}
		ri = resValRow.indexOf("systemuptime.s");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getSystemUptime()));
		}
		ri = resValRow.indexOf("logprocessorcount");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getLogicalProcessorCount()));
		}
		ri = resValRow.indexOf("phyprocessorcount");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getPhysicalProcessorCount()));
		}
		ri = resValRow.indexOf("contextswitches.n");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getContextSwitches()));
		}
		ri = resValRow.indexOf("interrupts");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(c.getInterrupts()));
		}
		resultValues.add(resValRow);
	}
}
