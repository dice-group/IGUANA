package org.aksw.iguana.cc.query.source;

import org.aksw.iguana.cc.utils.FileUtils;

/**
 * The abstract class for a QuerySource.
 * It implements the basic functions that are shared for all QuerySources.
 *
 * @author frensing
 */
public abstract class AbstractQuerySource implements QuerySource {

    protected String path;

    public AbstractQuerySource(String path) {
        this.path = path;
    }

    @Override
    public int hashCode() {
        return FileUtils.getHashcodeFromFileContent(this.path);
    }
}
