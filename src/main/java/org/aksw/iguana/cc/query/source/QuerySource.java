package org.aksw.iguana.cc.query.source;

import org.aksw.iguana.cc.utils.FileUtils;

import java.io.IOException;
import java.util.List;

/**
 * The abstract class for a QuerySource. <br/>
 * The QuerySource provides the queries to the QueryList. It abstracts the actual format of the query files.
 *
 * @author frensing
 */
public abstract class QuerySource {

    /** This string represents the path of the file or folder, that contains the queries. */
    protected String path;

    public QuerySource(String path) {
        this.path = path;
    }

    /**
     * This method returns the amount of queries in the source.
     *
     * @return the number of queries in the source
     */
    public abstract int size();

    /**
     * This method returns the query at the given index.
     *
     * @param index the index of the query counted from the first query (in the first file)
     * @return String of the query
     * @throws IOException
     */
    public abstract String getQuery(int index) throws IOException;

    /**
     * This method returns all queries in the source as a list of Strings.
     *
     * @return List of Strings of all queries
     * @throws IOException
     */
    public abstract List<String> getAllQueries() throws IOException;

    @Override
    public int hashCode() {
        return FileUtils.getHashcodeFromFileContent(this.path);
    }
}
