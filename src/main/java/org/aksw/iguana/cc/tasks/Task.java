package org.aksw.iguana.cc.tasks;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.tasks.impl.Stresstest;

public interface Task {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Stresstest.Config.class, name = "stresstest"),
    })
    interface Config {}

    void run();
}
