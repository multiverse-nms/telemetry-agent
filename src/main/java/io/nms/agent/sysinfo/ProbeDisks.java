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
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.FormatUtil;

public class ProbeDisks extends AbstractAgentTask {
	public final static String ERROR_PROBE_PARAMS_DNAME = "probe: invalid parameter <diskname>";
	public final static String ERROR_PROBE_PARAMS_PARTID = "probe: invalid parameter <partId>"; 
	public final static String ERROR_PROBE_PARAMS_MISSING = "probe: parameter <partId> or <diskname> is missing"; 
	public final static String ERROR_PROBE_PARTS_NOTFOUND = "probe: partitions not found"; 
	private Logger LOG = LoggerFactory.getLogger(ProbeDisks.class);
	private HardwareAbstractionLayer hal;
	
	public ProbeDisks(Message spec, JsonObject context) {	
		super(spec, context);
		verb = "measure";
		name = "disk";
		label = "Disk statistics and information";
		resultColumns = Arrays.asList(
			"disk.name","disk.model","disk.serial","disk.size","disk.reads",
			"disk.writes","disk.readbytes","disk.writebytes","disk.cql",
			"disk.transfertime","disk.partsnumber",
			"partition.id","partition.name","partition.type","partition.uuid",
			"partition.size","partition.major","partition.minor","partition.mountpoint");
	
		/* TODO: support Registry to fully use parameters */
		parameters.put("disk.name", "");
		//params.put("partition.id", "");

		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
		System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, "System.err");     
		SystemInfo si = new SystemInfo();
		hal = si.getHardware();
	}

	// implementation of Specification exec
	public short executeSpec() {
		LOG.info("Probe Disks...");
		resultValues.clear(); 
		HWDiskStore[] diskStores = hal.getDiskStores();
		if (specification.getResults().get(0).contains("disk.")) {
			if (specification.getParameters().containsKey("disk.name")) {
				String diskname = specification.getParameters().get("disk.name");
				for (HWDiskStore disk : diskStores) {
					if (diskname.equals(disk.getName())) {
						putDiskResultValues(disk);
						break;
					}
				}
			} else {
				for (HWDiskStore disk : diskStores) {
					putDiskResultValues(disk);	
				}
			}
		} else if (specification.getResults().get(0).contains("partition.")) {
			if (specification.getParameters().containsKey("disk.name")) {
				String diskName = specification.getParameters().get("disk.name");
				for (HWDiskStore disk : diskStores) {  
					if (disk.getName().equals(diskName)) {
						HWPartition[] partitions = disk.getPartitions();
						if (partitions != null) {
							for (HWPartition part : partitions) {  
								putPartitionResultValues(part);
							}
						} else {
							errors.add(ERROR_PROBE_PARTS_NOTFOUND);
							return Errors.TASK_ERROR; 
						}
						break;
					}
				}
			} else {
				errors.add(ERROR_PROBE_PARAMS_MISSING);
				return Errors.TASK_ERROR;
			}
		}
		return Errors.TASK_SUCCESS;
	}
  
	private void putDiskResultValues(HWDiskStore d) {
		boolean readwrite = d.getReads() > 0 || d.getWrites() > 0; 
		List<String> resValRow = new ArrayList<String>();
		resValRow.addAll(specification.getResults());     
		int ri = resValRow.indexOf("disk.name");
		if (ri >= 0) {
			resValRow.set(ri, d.getName());
		}
		ri = resValRow.indexOf("disk.model");
		if (ri >= 0) {
			resValRow.set(ri, d.getModel());
		}
		ri = resValRow.indexOf("disk.serial");
		if (ri >= 0) {
			resValRow.set(ri, d.getSerial());
		}
		ri = resValRow.indexOf("disk.size");
		if (ri >= 0) {
			resValRow.set(ri, d.getSize() > 0 ? FormatUtil.formatBytesDecimal(d.getSize()) : "?");
		}
		ri = resValRow.indexOf("disk.reads");
		if (ri >= 0) {
			resValRow.set(ri, readwrite ? String.valueOf(d.getReads()) : "N/A");
		}
		ri = resValRow.indexOf("disk.writes");
		if (ri >= 0) {
			resValRow.set(ri, readwrite ? String.valueOf(d.getWrites()) : "N/A");
		}
		ri = resValRow.indexOf("disk.readbytes");
		if (ri >= 0) {
			resValRow.set(ri, readwrite ? FormatUtil.formatBytes(d.getReadBytes()) : "0");
		}
		ri = resValRow.indexOf("disk.writebytes");
		if (ri >= 0) {
			resValRow.set(ri, readwrite ? FormatUtil.formatBytes(d.getWriteBytes()) : "0");
		}
		ri = resValRow.indexOf("disk.transfertime");
		if (ri >= 0) {
			resValRow.set(ri, readwrite ? String.valueOf(d.getTransferTime()) : "0");
		}
		ri = resValRow.indexOf("disk.cql");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(d.getCurrentQueueLength()));
		}
		ri = resValRow.indexOf("disk.partsnumber");
		if (ri >= 0) {
			HWPartition[] partitions = d.getPartitions();
			if (partitions != null) {
				resValRow.set(ri, String.valueOf(partitions.length));
			} else {
				resValRow.set(ri, "0");
			}
		}
		resultValues.add(resValRow);
	}
	private void putPartitionResultValues(HWPartition p) {
		List<String> resValRow = new ArrayList<String>();
		resValRow.addAll(specification.getResults());
		int ri = resValRow.indexOf("partition.id");
		if (ri >= 0) {
			resValRow.set(ri, p.getIdentification());
		}
		ri = resValRow.indexOf("partition.name");
		if (ri >= 0) {
			resValRow.set(ri, p.getName());
		}
		ri = resValRow.indexOf("partition.type");
		if (ri >= 0) {
			resValRow.set(ri, p.getType());
		}
		ri = resValRow.indexOf("partition.uuid");
		if (ri >= 0) {
			resValRow.set(ri, p.getUuid());
		}
		ri = resValRow.indexOf("partition.size");
		if (ri >= 0) {
			resValRow.set(ri, FormatUtil.formatBytes(p.getSize()));
		}
		ri = resValRow.indexOf("partition.major");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(p.getMajor()));
		}
		ri = resValRow.indexOf("partition.minor");
		if (ri >= 0) {
			resValRow.set(ri, String.valueOf(p.getMinor()));
		}
		ri = resValRow.indexOf("partition.mountpoint");
		if (ri >= 0) {
			resValRow.set(ri, p.getMountPoint().isEmpty() ? "" : p.getMountPoint());
		}
		resultValues.add(resValRow);
	}
}
