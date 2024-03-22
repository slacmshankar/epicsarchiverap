package org.epics.archiverappliance.mgmt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.epics.archiverappliance.SIOCSetup;
import org.epics.archiverappliance.TomcatSetup;
import org.epics.archiverappliance.retrieval.RetrievalMetrics;
import org.epics.archiverappliance.utils.ui.GetUrlContent;
import org.json.simple.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.epics.archiverappliance.engine.V4.PVAccessUtil.waitForStatusChange;

@Tag("integration")
public class MetricsTest {

    private static final Logger logger = LogManager.getLogger(MetricsTest.class.getName());
    private static final TomcatSetup tomcatSetup = new TomcatSetup();
    private static final String pvPrefix = MetricsTest.class.getSimpleName();
    private static final SIOCSetup siocSetup = new SIOCSetup(MetricsTest.class.getSimpleName());
    private static final String mgmtUrl = "http://localhost:17665/mgmt/bpl/";
    private static final String pvName = pvPrefix + "test_1";

    @BeforeEach
    public void setUp() throws Exception {
        siocSetup.startSIOCWithDefaultDB();
        tomcatSetup.setUpWebApps(this.getClass().getSimpleName());
    }

    @AfterEach
    public void tearDown() throws Exception {
        tomcatSetup.tearDown();
        siocSetup.stopSIOC();
    }

    @Test
    void testApplianceMetrics() {
        Map<String, String> expectedMetrics = new HashMap<>(Map.of(
                "connectedPVCount",
                "0",
                "instance",
                "appliance0",
                "pvCount",
                "0",
                "disconnectedPVCount",
                "0",
                "status",
                "Working"));

        assertApplianceMetricsMatch(expectedMetrics);

        archivePV();

        expectedMetrics.put("pvCount", "1");
        expectedMetrics.put("connectedPVCount", "1");
        assertApplianceMetricsMatch(expectedMetrics);
    }

    private static void archivePV() {

        // Archive PV
        String archivePVURL = mgmtUrl + "archivePV?pv=pva://";

        String pvURLName = URLEncoder.encode(pvName, StandardCharsets.UTF_8);

        GetUrlContent.getURLContentAsJSONArray(archivePVURL + pvURLName);
        waitForStatusChange(pvName, "Being archived", 60, mgmtUrl, 10);
    }

    private static void assertApplianceMetricsMatch(Map<String, String> expectedMetrics) {
        JSONArray metricsResponse = GetUrlContent.getURLContentAsJSONArray(MetricsTest.mgmtUrl + "getApplianceMetrics");
        Map<String, String> metricsResult = (Map<String, String>) metricsResponse.get(0);

        partialAssertMap(expectedMetrics, metricsResult);
    }

    private static void partialAssertMap(Map<String, String> expectedMetrics, Map<String, String> metricsResult) {
        expectedMetrics.forEach((k, v) -> Assertions.assertEquals(v, metricsResult.get(k), "Fail check on " + k));
    }

    private static void assertApplianceMetricsDetailsMatch(Map<String, String> expectedMetrics, String appliance) {
        JSONArray metricsResponse = GetUrlContent.getURLContentAsJSONArray(MetricsTest.mgmtUrl
                + "getApplianceMetricsForAppliance?appliance=" + URLEncoder.encode(appliance, StandardCharsets.UTF_8));
        List<Map<String, String>> metricsResult = (List<Map<String, String>>) metricsResponse;

        Map<String, String> actual = convertToStringMap(metricsResult);
        partialAssertMap(expectedMetrics, actual);
    }

    private static void assertPVDetailsMatch(Map<String, String> expectedMetrics) {
        JSONArray metricsResponse = GetUrlContent.getURLContentAsJSONArray(
                MetricsTest.mgmtUrl + "getPVDetails?pv=" + URLEncoder.encode(pvName, StandardCharsets.UTF_8));
        List<Map<String, String>> metricsResult = (List<Map<String, String>>) metricsResponse;

        Map<String, String> actual = convertToStringMap(metricsResult);
        partialAssertMap(expectedMetrics, actual);
    }

    private static Map<String, String> convertToStringMap(List<Map<String, String>> actualMetrics) {
        logger.info("Actual metrics are: " + actualMetrics);
        return actualMetrics.stream()
                .collect((Collectors.toMap(
                        m -> m.get("source") + m.get("name"), m -> StringUtils.nullSafeToString(m.get("value")))));
    }

    String retrievalSource = "retrieval";
    String engineSource = "engine";
    String mgmtSource = "mgmt";
    String etlSource = "etl";
    String pvSource = "pv";

    @Test
    void testApplianceMetricsForAppliance() {
        Map<String, String> expectedMetrics = Map.of(
                "mgmt" + "Appliance Identity",
                "appliance0",
                engineSource + "Total PV Count",
                "0",
                engineSource + "Disconnected PV count",
                "0",
                engineSource + "Connected PV count",
                "0",
                engineSource + "Paused PV count",
                "0",
                engineSource + "Total channels",
                "0",
                retrievalSource + RetrievalMetrics.NUMBER_OF_RETRIEVAL_REQUESTS,
                "0",
                retrievalSource + RetrievalMetrics.NUMBER_OF_UNIQUE_USERS,
                "0");

        assertApplianceMetricsDetailsMatch(expectedMetrics, "appliance0");

        archivePV();

        assertApplianceMetricsDetailsMatch(expectedMetrics, "appliance0");
    }

    @Test
    void testPVDetails() {

        Map<String, String> expectedMetrics = Map.ofEntries(
                entry(mgmtSource + "PV Name", pvName),
                entry(mgmtSource + "Instance archiving PV", "appliance0"),
                entry(mgmtSource + "Archiver DBR type (from typeinfo):", "DBR_SCALAR_DOUBLE"),
                entry(mgmtSource + "Is this a scalar:", "Yes"),
                entry(mgmtSource + "Number of elements:", "1"),
                entry(mgmtSource + "Precision:", "0.0"),
                entry(mgmtSource + "Units:", "null"),
                entry(mgmtSource + "Is this PV paused:", "No"),
                entry(mgmtSource + "Sampling method:", "MONITOR"),
                entry(mgmtSource + "Sampling period:", "1.0"),
                entry(mgmtSource + "Are we using PVAccess?", "No"),
                entry(pvSource + "Host name", "localhost"),
                entry(pvSource + "Controlling PV", ""),
                entry(pvSource + "Is engine currently archiving this?", "yes"),
                entry(pvSource + "Sample buffer capacity", "3"),
                entry(etlSource + "Name (from ETL)", pvName),
                entry(retrievalSource + RetrievalMetrics.NUMBER_OF_RETRIEVAL_REQUESTS, "0"),
                entry(retrievalSource + RetrievalMetrics.NUMBER_OF_UNIQUE_USERS, "0"));

        archivePV();

        assertPVDetailsMatch(expectedMetrics);
    }
}
