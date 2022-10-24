package org.aksw.iguana.cc.query.source;

import java.io.IOException;
import java.util.List;

public interface QuerySource {

    int size();

    String getQuery(int index) throws IOException;

    List<String> getAllQueries() throws IOException;
}
