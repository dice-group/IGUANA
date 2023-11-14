package org.aksw.iguana.cc.mockup;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;

import java.net.URI;

public class MockupConnection {

    /**
     * Creates a connection config with the given parameters
     *
     * @param name          The name of the connection
     * @param endpoint      The endpoint of the connection
     * @param datasetName   The name of the dataset
     */
    public static ConnectionConfig createConnectionConfig(String name, String datasetName, String endpoint) {
        return new ConnectionConfig(name, "", null, URI.create(endpoint), null, null, null);
    }
}
