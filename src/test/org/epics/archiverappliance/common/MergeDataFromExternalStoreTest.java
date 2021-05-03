/*******************************************************************************
 * Copyright (c) 2011 The Board of Trustees of the Leland Stanford Junior University
 * as Operator of the SLAC National Accelerator Laboratory.
 * Copyright (c) 2011 Brookhaven National Laboratory.
 * EPICS archiver appliance is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 *******************************************************************************/
package org.epics.archiverappliance.common;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.sql.Timestamp;

import org.apache.log4j.Logger;
import org.epics.archiverappliance.Event;
import org.epics.archiverappliance.EventStream;
import org.epics.archiverappliance.StoragePlugin;
import org.epics.archiverappliance.TomcatSetup;
import org.epics.archiverappliance.config.ArchDBRTypes;
import org.epics.archiverappliance.config.ConfigServiceForTests;
import org.epics.archiverappliance.config.PVTypeInfo;
import org.epics.archiverappliance.config.StoragePluginURLParser;
import org.epics.archiverappliance.data.ScalarValue;
import org.epics.archiverappliance.engine.membuf.ArrayListEventStream;
import org.epics.archiverappliance.retrieval.RemotableEventStreamDesc;
import org.epics.archiverappliance.retrieval.client.RawDataRetrievalAsEventStream;
import org.epics.archiverappliance.utils.ui.GetUrlContent;
import org.epics.archiverappliance.utils.ui.JSONDecoder;
import org.epics.archiverappliance.utils.ui.JSONEncoder;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test merging in data from an external store. 
 * @author mshankar
 *
 */
public class MergeDataFromExternalStoreTest {
	private static Logger logger = Logger.getLogger(MergeDataFromExternalStoreTest.class.getName());
	private ConfigServiceForTests configService;
	String pvName = "MergeDataFromExternalStoreTest";
	ArchDBRTypes dbrType = ArchDBRTypes.DBR_SCALAR_DOUBLE;
	TomcatSetup tomcatSetup = new TomcatSetup();
	long tCount = 0;
	long stepSeconds = 2;
	
	@Before
	public void setUp() throws Exception {
		configService = new ConfigServiceForTests(new File("./bin"));
		tomcatSetup.setUpFailoverWithWebApps(this.getClass().getSimpleName());		
	}

	/**
	 * Generate a months worth of data for the given appserver.
	 * @param applURL - The URL for the appliance.
	 * @param applianceName - The name of the appliance
	 * @param lastMonth - The month we generate data for. We generate a month's worth of MTS data.
	 * @param startingOffset - Use 0 for even seconds; 1 for odd seconds. When merged, we test to make sure; we get data one second apart.
	 * @throws Exception
	 */
	private long generateMTSData(String applURL, String applianceName, Timestamp lastMonth, int startingOffset)
			throws Exception {
		int genEventCount = 0;
		StoragePlugin plugin = StoragePluginURLParser.parseStoragePlugin("pb://localhost?name=LTS&rootFolder=" + "tomcat_"+ this.getClass().getSimpleName() + "/" + applianceName + "/mts" + "&partitionGranularity=PARTITION_DAY", configService);
		try(BasicContext context = new BasicContext()) {
			for(long s = TimeUtils.getPreviousPartitionLastSecond(TimeUtils.convertToEpochSeconds(lastMonth), PartitionGranularity.PARTITION_MONTH) + 1 + startingOffset; // We generate a months worth of data.
					s < TimeUtils.getNextPartitionFirstSecond(TimeUtils.convertToEpochSeconds(lastMonth), PartitionGranularity.PARTITION_MONTH); 
					s = s + stepSeconds) {
				ArrayListEventStream strm = new ArrayListEventStream(0, new RemotableEventStreamDesc(ArchDBRTypes.DBR_SCALAR_DOUBLE, pvName, TimeUtils.convertToYearSecondTimestamp(s).getYear()));
				strm.add(new POJOEvent(ArchDBRTypes.DBR_SCALAR_DOUBLE, TimeUtils.convertFromEpochSeconds(s, 0), new ScalarValue<Double>((double)s), 0, 0));
				genEventCount++;
				plugin.appendData(context, pvName, strm);
			}			
		}		
		logger.info("Done generating dest data");
		
		JSONObject srcPVTypeInfoJSON = (JSONObject) JSONValue.parse(new InputStreamReader(new FileInputStream(new File("src/test/org/epics/archiverappliance/retrieval/postprocessor/data/PVTypeInfoPrototype.json"))));
		PVTypeInfo destPVTypeInfo = new PVTypeInfo();
		JSONDecoder<PVTypeInfo> decoder = JSONDecoder.getDecoder(PVTypeInfo.class);
		JSONEncoder<PVTypeInfo> encoder = JSONEncoder.getEncoder(PVTypeInfo.class);
		decoder.decode(srcPVTypeInfoJSON, destPVTypeInfo);
		
		destPVTypeInfo.setPaused(true);
		destPVTypeInfo.setPvName(pvName);
		destPVTypeInfo.setApplianceIdentity(applianceName);
		destPVTypeInfo.setChunkKey(configService.getPVNameToKeyConverter().convertPVNameToKey(pvName));
		destPVTypeInfo.setCreationTime(TimeUtils.convertFromISO8601String("2020-11-11T14:49:58.523Z"));
		destPVTypeInfo.setModificationTime(TimeUtils.now());
		GetUrlContent.postObjectAndGetContentAsJSONObject(applURL + "/mgmt/bpl/putPVTypeInfo?pv=" + URLEncoder.encode(pvName, "UTF-8") + "&override=true&createnew=true", encoder.encode(destPVTypeInfo));
		logger.info("Added " + pvName + " to the appliance " + applianceName);
		
		RawDataRetrievalAsEventStream rawDataRetrieval = new RawDataRetrievalAsEventStream(applURL + "/retrieval/data/getData.raw");
		long rtvlEventCount = 0;
		try(EventStream stream = rawDataRetrieval.getDataForPVS(new String[] { pvName }, TimeUtils.minusDays(TimeUtils.now(), 90), TimeUtils.plusDays(TimeUtils.now(), 31), null)) {
			long lastEvEpoch = 0;
			if(stream != null) {
				for(Event e : stream) {
					long evEpoch = TimeUtils.convertToEpochSeconds(e.getEventTimeStamp());
					if(lastEvEpoch != 0) {
						assertTrue("We got events more than " + stepSeconds + " seconds apart " + TimeUtils.convertToHumanReadableString(lastEvEpoch) + " and  " +  TimeUtils.convertToHumanReadableString(evEpoch), (evEpoch - lastEvEpoch) == stepSeconds);
					}
					lastEvEpoch = evEpoch;
					rtvlEventCount++;
				}
			} else { 
				fail("Stream is null when retrieving data.");
			}
		}		
		assertTrue("We expected event count  " + genEventCount + " but got  " + rtvlEventCount, genEventCount == rtvlEventCount);
		return rtvlEventCount;
	}
	
	@After
	public void tearDown() throws Exception {
		tomcatSetup.tearDown();
	}
	
	private void mergeInDataFromRemoteServer() throws Exception {
		JSONObject cresp = GetUrlContent.getURLContentAsJSONObject("http://localhost:17665/mgmt/bpl/consolidateDataForPV" 
				+ "?pv=" + URLEncoder.encode(pvName, "UTF-8") 
				+ "&storage=LTS");
		assertTrue("Invalid response for consolidate data", cresp.get("status").equals("ok"));
		String otherClientURL = "http://localhost:17669/retrieval";
		JSONObject resp = GetUrlContent.getURLContentAsJSONObject("http://localhost:17665/mgmt/bpl/mergeInData" 
				+ "?pv=" + URLEncoder.encode(pvName, "UTF-8") 
				+ "&other=" + URLEncoder.encode(otherClientURL, "UTF-8") 
				+ "&storage=LTS"
				+ "&from=" + TimeUtils.convertToISO8601String(TimeUtils.minusDays(TimeUtils.now(), 366*2)));
		assertTrue("Invalid response for merge data", resp.get("status").equals("ok"));
		logger.info("Merged data for " + pvName + " into the dest tomcat");
	}
	
	private void testMergedRetrieval() throws Exception {
		RawDataRetrievalAsEventStream rawDataRetrieval = new RawDataRetrievalAsEventStream("http://localhost:17665/retrieval/data/getData.raw");
		long rtvlEventCount = 0;
		try(EventStream stream = rawDataRetrieval.getDataForPVS(new String[] { pvName }, TimeUtils.minusDays(TimeUtils.now(), 90), TimeUtils.plusDays(TimeUtils.now(), 31), null)) {
			long lastEvEpoch = 0;
			if(stream != null) {
				for(Event e : stream) {
					long evEpoch = TimeUtils.convertToEpochSeconds(e.getEventTimeStamp());
					logger.debug("Current event " + TimeUtils.convertToHumanReadableString(evEpoch) + " Previous: " + TimeUtils.convertToHumanReadableString(lastEvEpoch));
					if(lastEvEpoch != 0) {
						assertTrue("We got events out of order " + TimeUtils.convertToHumanReadableString(lastEvEpoch) + " and  " +  TimeUtils.convertToHumanReadableString(evEpoch) + " at event count " + rtvlEventCount, evEpoch > lastEvEpoch);
						assertTrue("We got events more than a second apart " + TimeUtils.convertToHumanReadableString(lastEvEpoch) + " and  " +  TimeUtils.convertToHumanReadableString(evEpoch) + " at event count " + rtvlEventCount, (evEpoch - lastEvEpoch) == 1);
					}
					lastEvEpoch = evEpoch;
					rtvlEventCount++;
				}
			} else { 
				fail("Stream is null when retrieving data.");
			}
		}		
		assertTrue("We expected event count  " + tCount + " but got  " + rtvlEventCount, tCount == rtvlEventCount);
	}

	@Test
	public void testRetrieval() throws Exception {
		// Register the PV with both appliances and generate data.
		Timestamp lastMonth = TimeUtils.minusDays(TimeUtils.now(), 31);		
		long dCount = generateMTSData("http://localhost:17665", "dest_appliance", lastMonth, 0);
		long oCount = generateMTSData("http://localhost:17669", "other_appliance", lastMonth, 1);
		tCount = dCount + oCount;
		
		mergeInDataFromRemoteServer();
		testMergedRetrieval();
	}	
}
