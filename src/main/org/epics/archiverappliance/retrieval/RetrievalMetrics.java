package org.epics.archiverappliance.retrieval;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public record RetrievalMetrics(long numberOfRequests, Instant lastRequest) {
    public static RetrievalMetrics createUpdatedMetrics(RetrievalMetrics currentRetrievalMetrics, Instant lastRequest) {
        if (currentRetrievalMetrics == null) {
            return new RetrievalMetrics(1, lastRequest);
        }
        return new RetrievalMetrics(currentRetrievalMetrics.numberOfRequests() + 1, lastRequest);
    }

    public LinkedList<Map<String, String>> getDetails() {
        LinkedList<Map<String, String>> result = new LinkedList<>();
        addDetailedStatus(result, "Number of Retrieval Requests", String.valueOf(numberOfRequests()));
        addDetailedStatus(
                result,
                "Time of last Retrieval Request",
                lastRequest != null ? lastRequest.atOffset(ZoneOffset.UTC).toString(): "Never");
        return result;
    }

    private static void addDetailedStatus(LinkedList<Map<String, String>> statuses, String name, String value) {
        Map<String, String> obj = new LinkedHashMap<String, String>();
        obj.put("name", name);
        obj.put("value", value);
        obj.put("source", "retrieval");
        statuses.add(obj);
    }
}
