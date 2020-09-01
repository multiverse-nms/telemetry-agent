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
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

public class ProbeNetwork extends AbstractAgentTask {
  private Logger LOG = LoggerFactory.getLogger(ProbeNetwork.class);
  private HardwareAbstractionLayer hal;
  public ProbeNetwork(Message spec, JsonObject context) {	
    super(spec, context);
    verb = "measure";
    name = "network";
	label = "Network interface performance";	
	resultColumns = Arrays.asList(
			"bytesrcvd.b","bytessent.b",
			"pktsrcvd.n","pktssent.n",
			"inerrors.n","outerrors.n");
	role = "admin";

    // System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    // System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");     
    SystemInfo si = new SystemInfo();
    hal = si.getHardware();
    si.getOperatingSystem();
  }
	
  public short executeSpec() {
    LOG.info("Probe Network...");
	resultValues.clear();
	List<String> resValRow = new ArrayList<String>();
	resValRow.addAll(specification.getResults());
	List<NetworkIF> netIfs = hal.getNetworkIFs();
    if (specification.getParameters().containsKey("netif.name")) {
        String itfName = specification.getParameters().get("netif.name");
    	for (NetworkIF nif : netIfs) {
    	  if (itfName.equals(nif.getName())) {
    	    putNetItfResultValues(nif);
    	    break;
    	  }
    	}
    } else {
        for (NetworkIF nif : netIfs) {
          putNetItfResultValues(nif);
        }
    }
	return Errors.TASK_SUCCESS;
  }
  
  private void putNetItfResultValues(NetworkIF n) {
	  List<String> resValRow = new ArrayList<String>();
      resValRow.addAll(specification.getResults());     
      int ri = resValRow.indexOf("name");
      if (ri >= 0) {
    	  resValRow.set(ri, n.getName());
      }
      ri = resValRow.indexOf("displayname");
      if (ri >= 0) {
    	  resValRow.set(ri, n.getDisplayName());
      }
      ri = resValRow.indexOf("mtu");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getMTU()));
      }
      ri = resValRow.indexOf("speed");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getSpeed()));
      }
      ri = resValRow.indexOf("macaddress");
      if (ri >= 0) {
    	  resValRow.set(ri, n.getMacaddr());
      }
      ri = resValRow.indexOf("ipv4address");
      if (ri >= 0) {
    	  resValRow.set(ri, Arrays.toString(n.getIPv4addr()));
      }      
      ri = resValRow.indexOf("ipv6address");
      if (ri >= 0) {
    	  resValRow.set(ri, Arrays.toString(n.getIPv6addr()));
      }
      ri = resValRow.indexOf("bytesrcvd.b");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getBytesRecv()));
      }
      ri = resValRow.indexOf("bytessent.b");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getBytesSent()));
      }
      ri = resValRow.indexOf("pktsrcvd.n");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getPacketsRecv()));
      }
      ri = resValRow.indexOf("pktssent.n");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getPacketsSent()));
      }
      ri = resValRow.indexOf("inerrors.n");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getInErrors()));
      }      
      ri = resValRow.indexOf("outerrors.n");
      if (ri >= 0) {
    	  resValRow.set(ri, String.valueOf(n.getOutErrors()));
      }
      resultValues.add(resValRow);
  }
}