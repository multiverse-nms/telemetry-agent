package io.nms.agent.sysinfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nms.agent.constants.Errors;
import io.nms.agent.message.Message;
import io.nms.agent.taskmanager.AbstractAgentTask;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

public class TopologyInfo extends AbstractAgentTask {
  private Logger LOG = LoggerFactory.getLogger(TopologyInfo.class);
  private HardwareAbstractionLayer hal;
  public TopologyInfo(Message spec, JsonObject context) {	
    super(spec, context);
    verb = "measure";
    name = "topology";
    agentId = "agent0-switch";
	label = "Node network connections";
	resultColumns = Arrays.asList(
			"itfname",
			"macaddress",
			"ipv4address",
			"status",
			"target");
	//parameters.put("itfname", "<String>");
	role = "admin";
	taskPeriodMs = 5000;

    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");     
    SystemInfo si = new SystemInfo();
    hal = si.getHardware();
    si.getOperatingSystem();
  }
	
  public short executeSpec() {
    LOG.info("Check topology...");
	resultValues.clear();
	
	// parse topology info
	Map<String, String> itfsConfig = new HashMap<String,String>();
	JsonArray topo = context.getJsonObject("config").getJsonArray("topology", new JsonArray());
	for (int i = 0; i < topo.size(); i++) {
		String[] itf = topo.getString(i).split(">");
		itfsConfig.put(itf[0], itf[1]);
	}
	
	// get system net info
	NetworkIF[] netIfs = hal.getNetworkIFs();
    for (NetworkIF nif : netIfs) {
    	putNetItfResultValues(nif, itfsConfig.getOrDefault(nif.getName(), "N/A"));
    }
	return Errors.TASK_SUCCESS;
  }
  
  private void putNetItfResultValues(NetworkIF n, String target) {
	  List<String> resValRow = new ArrayList<String>();
      resValRow.addAll(specification.getResults());     
      int ri = resValRow.indexOf("itfname");
      if (ri >= 0) {
    	  resValRow.set(ri, n.getName());
      }
      ri = resValRow.indexOf("status");
      if (ri >= 0) {
    	  resValRow.set(ri, "UP");
      }
      ri = resValRow.indexOf("macaddress");
      if (ri >= 0) {
    	  resValRow.set(ri, n.getMacaddr());
      }
      ri = resValRow.indexOf("ipv4address");
      if (ri >= 0) {
    	  resValRow.set(ri, n.getIPv4addr()[0]);
      }
      ri = resValRow.indexOf("target");
      if (ri >= 0) {
    	  resValRow.set(ri, target);
      }
      resultValues.add(resValRow);
  }
}