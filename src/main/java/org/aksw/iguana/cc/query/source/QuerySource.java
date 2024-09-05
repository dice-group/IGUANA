package org.aksw.iguana.cc.query.source;

import org.aksw.iguana.cc.utils.files.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * The abstract class for a QuerySource. <br/>
 * The QuerySource provides the queries to the QueryList. It abstracts the actual format of the query files.
 *
 * @author frensing
 */
public abstract class QuerySource {

    /** This string represents the path of the file or folder, that contains the queries. */
    final protected Path path;

    /**
     * This integer represents the hashcode of the file or folder, that contains the queries. It is stored for
     * performance reasons, so that the hashcode does not have to be calculated every time it is needed.
     * (It's needed everytime the id of a query is requested.)
     */
    final protected int hashCode;

    public QuerySource(Path path) {
        this.path = path;
        this.hashCode = FileUtils.getHashcodeFromFileContent(path);
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
     * @throws IOException if the query could not be read
     */
    public abstract String getQuery(int index) throws IOException;

    public abstract InputStream getQueryStream(int index) throws IOException;

    /**
     * This method returns all queries in the source as a list of Strings.
     *
     * @return List of Strings of all queries
     * @throws IOException if the queries could not be read
     */
    public abstract List<String> getAllQueries() throws IOException;

    /**
     * This method returns the path of the file or folder, that contains the queries.
     *
     * @return the path of the file or folder, that contains the queries
     */
    public Path getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
