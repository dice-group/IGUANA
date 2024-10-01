package org.aksw.iguana.cc.mockup;

import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.query.selector.QuerySelector;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;


public class MockupQueryHandler extends QueryHandler {
    private final int id;
    private final int queryNumber;

    public MockupQueryHandler(int id, int queryNumber) {
        super();
        this.queryNumber = queryNumber;
        this.id = id;
    }

    @Override
    public String getQueryId(int i) {
        return "MockQueryHandler" + this.id + ":" + i;
    }

    @Override
    public String[] getAllQueryIds() {
        String[] out = new String[queryNumber];
        for (int i = 0; i < queryNumber; i++) {
            out[i] = getQueryId(i);
        }
        return out;
    }

    @Override
    public int getExecutableQueryCount() {
        return queryNumber;
    }

    @Override
    public int getRepresentativeQueryCount() {
        return queryNumber;
    }

    @Override
    public QuerySelector getQuerySelectorInstance() {
        return new LinearQuerySelector(queryNumber);
    }
}
