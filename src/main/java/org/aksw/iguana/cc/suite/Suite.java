package org.aksw.iguana.cc.suite;

import org.aksw.iguana.cc.config.SuiteConfig;
import org.aksw.iguana.cc.config.elements.TaskConfig;
import org.aksw.iguana.cc.tasks.Stresstest;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;

import java.util.ArrayList;
import java.util.List;

public class Suite {

    public record Result(List<Stresstest.Result> stresstest) {

    }

    private final long suiteId;
    private final SuiteConfig config;
    private final ResponseBodyProcessorInstances responseBodyProcessorInstances;

    private final List<Stresstest> stresstests = new ArrayList<>();

    Suite(long suiteId, SuiteConfig config){

        this.suiteId = suiteId;
        this.config = config;
        long stresstestId = 0;
        responseBodyProcessorInstances = new ResponseBodyProcessorInstances();


        for (TaskConfig task : config.tasks()) {
            if (task instanceof Stresstest.Config) {
                stresstests.add(new Stresstest(stresstestId, (Stresstest.Config) task, responseBodyProcessorInstances));
            }
        }
    }

    public Suite.Result run() {
        List<Stresstest.Result> stresstestResults = new ArrayList<>();
        for (Stresstest stresstest : stresstests) {
            stresstestResults.add(stresstest.run());
        }
        return new Result(stresstestResults);
    }

}


