package org.aksw.iguana.cc.metrics;

import com.fasterxml.jackson.annotation.*;
import org.aksw.iguana.cc.metrics.impl.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AggregatedExecutionStatistics.class, name = "AES"),
        @JsonSubTypes.Type(value = AvgQPS.class, name = "AvgQPS"),
        @JsonSubTypes.Type(value = EachExecutionStatistic.class, name = "EachQuery"),
        @JsonSubTypes.Type(value = NoQ.class, name = "NoQ"),
        @JsonSubTypes.Type(value = NoQPH.class, name = "NoQPH"),
        @JsonSubTypes.Type(value = PAvgQPS.class, name = "PAvgQPS"),
        @JsonSubTypes.Type(value = PQPS.class, name = "PQPS"),
        @JsonSubTypes.Type(value = QMPH.class, name = "QMPH"),
        @JsonSubTypes.Type(value = QPS.class, name = "QPS")
})
public abstract class Metric {
    private final String name;
    private final String abbreviation;
    private final String description;

    public Metric(String name, String abbreviation, String description) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.description = description;
    }


    public String getDescription(){
        return this.description;
    }

    public String getName(){
        return this.name;
    }

    public String getAbbreviation(){
        return this.abbreviation;
    }
}
