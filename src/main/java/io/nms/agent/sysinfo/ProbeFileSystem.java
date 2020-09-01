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
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

public class ProbeFileSystem extends AbstractAgentTask {
  public final static String ERROR_PROBE_PARAMS_DNAME = "probe process: invalid parameter <diskname>";
  public final static String ERROR_PROBE_PARAMS_PARTID = "probe process: invalid parameter <partId>"; 
  public final static String ERROR_PROBE_PARAMS_MISSING = "probe process: parameter <partId> or <diskname> is missing"; 
  public final static String ERROR_PROBE_DISK_NOTFOUND = "probe process: disk not found"; 
  private Logger LOG = LoggerFactory.getLogger(ProbeFileSystem.class);
  private OperatingSystem os;
	
  public ProbeFileSystem(Message spec, JsonObject context) {	
    super(spec, context);
    verb = "measure";
    name = "filesystem";
	label = "Filesystem information";
	resultColumns = Arrays.asList(
			"fs.openfiledescriptors","fs.maxfiledescriptors",
			"fstore.name",
			"fstore.volume","fstore.logicalvolume","fstore.mount",
			"fstore.description","fstore.type","fstore.uuid",
			"fstore.freespace","fstore.usablespace","fstore.totalspace",
			"fstore.freeinodes","fstore.totalinodes");
	parameters.put("fstore.name", "<String>");

    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
    System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");     
    SystemInfo si = new SystemInfo();
    os = si.getOperatingSystem();
  }

  public short executeSpec() {
    LOG.info("Probe File System...");
    resultValues.clear();
    List<String> resValRow = new ArrayList<String>();
    resValRow.addAll(specification.getResults());
    FileSystem fs = os.getFileSystem();
    if (specification.getResults().get(0).contains("fs.")) {
      int ri = resValRow.indexOf("fs.openfiledescriptors");
      if (ri >= 0) {
        resValRow.set(ri, String.valueOf(fs.getOpenFileDescriptors()));
      }
      ri = resValRow.indexOf("fs.maxfiledescriptors");
      if (ri >= 0) {
        resValRow.set(ri, String.valueOf(fs.getMaxFileDescriptors()));
      }
      resultValues.add(resValRow);
    } else if (specification.getResults().get(0).contains("fstore.")) {
      OSFileStore[] fileStores = fs.getFileStores();
      if (specification.getParameters().containsKey("fstore.name")) {
        String fStoreName = specification.getParameters().get("fstore.name");
        for (OSFileStore fstore : fileStores) {
    	  if (fStoreName.equals(fstore.getName())) {
    	    putFileStoreResultValues(fstore);
    	    break;
    	  }
    	}
      } else { 
        for (OSFileStore fstore : fileStores) {
          putFileStoreResultValues(fstore);
        }
      }
    }
    return Errors.TASK_SUCCESS;
  }
  
  private void putFileStoreResultValues(OSFileStore f) {
	List<String> resValRow = new ArrayList<String>();
    resValRow.addAll(specification.getResults());
    int ri = resValRow.indexOf("fstore.name");
    if (ri >= 0) {
      resValRow.set(ri, f.getName());
    }
    ri = resValRow.indexOf("fstore.volume");
    if (ri >= 0) {
      resValRow.set(ri, f.getVolume());
    }
    ri = resValRow.indexOf("fstore.logicalvolume");
    if (ri >= 0) {
      resValRow.set(ri, f.getLogicalVolume());
    }
    ri = resValRow.indexOf("fstore.mount");
    if (ri >= 0) {
      resValRow.set(ri, f.getMount());
    }
    ri = resValRow.indexOf("fstore.description");
    if (ri >= 0) {
      resValRow.set(ri, f.getDescription().isEmpty() ? "file system" : f.getDescription());
    }
    ri = resValRow.indexOf("fstore.type");
    if (ri >= 0) {
      resValRow.set(ri, f.getType());
    }
    ri = resValRow.indexOf("fstore.uuid");
    if (ri >= 0) {
      resValRow.set(ri, f.getUUID());
    }
    ri = resValRow.indexOf("fstore.freespace");
    if (ri >= 0) {
      long usable = f.getUsableSpace();
      long total = f.getTotalSpace();
      resValRow.set(ri,  String.valueOf(100d * usable / total));
    }
    ri = resValRow.indexOf("fstore.usablespace");
    if (ri >= 0) {
      resValRow.set(ri, FormatUtil.formatBytes(f.getUsableSpace()));
    }
    ri = resValRow.indexOf("fstore.totalspace");
    if (ri >= 0) {
      resValRow.set(ri, FormatUtil.formatBytes(f.getTotalSpace()));
    }
    ri = resValRow.indexOf("fstore.freeinodes");
    if (ri >= 0) {
      resValRow.set(ri, String.valueOf(f.getFreeInodes()));
    }
    ri = resValRow.indexOf("fstore.totalinodes");
    if (ri >= 0) {
      resValRow.set(ri, String.valueOf(f.getTotalInodes()));
    }
    resultValues.add(resValRow);
  }
}
