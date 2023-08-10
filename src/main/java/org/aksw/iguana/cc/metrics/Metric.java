package org.aksw.iguana.cc.metrics;

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
