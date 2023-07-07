package org.aksw.iguana.cc.controller;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.tasks.Stresstest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Task Controlling, will start the actual benchmark tasks and its {@link org.aksw.iguana.cc.tasks.TaskManager}
 *
 * @author f.conrads
 */
public class TaskController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskController.class);

    public void startTask(String[] ids, String dataset, ConnectionConfig con, Stresstest.Config task) {
    }
}
