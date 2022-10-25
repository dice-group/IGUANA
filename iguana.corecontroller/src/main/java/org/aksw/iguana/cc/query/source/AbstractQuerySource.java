package org.aksw.iguana.cc.query.source;

import org.aksw.iguana.cc.utils.FileUtils;

public abstract class AbstractQuerySource implements QuerySource {

    protected String path;

    public AbstractQuerySource(String path) {
        this.path = path;
    }

    @Override
    public int getHashcode() {
        return FileUtils.getHashcodeFromFileContent(this.path);
    }
}
