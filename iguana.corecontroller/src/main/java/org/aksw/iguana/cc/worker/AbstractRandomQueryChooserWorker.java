package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public abstract class AbstractRandomQueryChooserWorker extends AbstractWorker {

    protected int currentQueryID;
    protected Random queryChooser;


    public AbstractRandomQueryChooserWorker(String taskID, Connection connection, String queriesFile, Integer timeOut, Integer timeLimit, Integer fixedLatency, Integer gaussianLatency, String workerType, Integer workerID) {
        super(taskID, connection, queriesFile, timeOut, timeLimit, fixedLatency, gaussianLatency, workerType, workerID);
        queryChooser = new Random(this.workerID);

    }

    @Override
    public void setQueriesList(QuerySet[] queries) {
        super.setQueriesList(queries);
        this.currentQueryID = queryChooser.nextInt(this.queryFileList.length);
    }


    @Override
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        // get next Query File and next random Query out of it.
        QuerySet currentQueryFile = this.queryFileList[this.currentQueryID++];
        queryID.append(currentQueryFile.getName());

        int queriesInFile = currentQueryFile.size();
        int queryLine = queryChooser.nextInt(queriesInFile);
        queryStr.append(currentQueryFile.getQueryAtPos(queryLine));

        // If there is no more query(Pattern) start from beginning.
        if (this.currentQueryID >= this.queryFileList.length) {
            this.currentQueryID = 0;
        }

    }

}
