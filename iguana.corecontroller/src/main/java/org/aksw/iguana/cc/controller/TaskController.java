package org.aksw.iguana.cc.controller;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.config.elements.Task;
import org.aksw.iguana.cc.tasks.TaskFactory;
import org.aksw.iguana.cc.tasks.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * Task Controlling, will start the actual benchmark tasks and its {@link org.aksw.iguana.cc.tasks.TaskManager}
 *
 * @author f.conrads
 */
public class TaskController {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TaskController.class);

    public void startTask(String[] ids, String dataset, Connection con, Task task) {
        TaskManager tmanager = new TaskManager();
        String className = task.getClassName();
        TaskFactory factory = new TaskFactory();
        tmanager.setTask(factory.create(className, task.getConfiguration()));
        try {
            tmanager.startTask(ids, dataset, con, task.getName());
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Could not start Task " + className, e);
        }
    }
}
