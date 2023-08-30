package org.aksw.iguana.cc.storage;

import org.apache.jena.rdf.model.Model;

import java.util.List;

public interface Storable {

    record CSVFileData (
        String filename,
        String[][] data
    ) {}

    interface AsCSV {

        /**
         * Converts the data into CSV files. The key of the map contains the file name for the linked entries.
         *
         * @return CSVFileData list which contains all the files and their data that should be created and stored
         */
        List<CSVFileData> toCSV();
    }

    interface AsRDF {

        /**
         * Converts the data into an RDF model, which will be added to the appropriate storages.
         *
         * @return RDF model that contains the data
         */
        Model toRDF();
    }

}
