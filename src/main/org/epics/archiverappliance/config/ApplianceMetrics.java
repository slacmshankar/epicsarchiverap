package org.epics.archiverappliance.config;

import org.epics.archiverappliance.etl.common.ETLMetricsForLifetime;
import org.epics.archiverappliance.retrieval.RetrievalMetrics;

import java.util.List;

public class ApplianceMetrics {
    private List<ETLMetricsForLifetime> etlMetricsForLifetimeList;
    private List<RetrievalMetrics> retrievalMetricsList;
}
