package org.aksw.iguana.cc.storage;

import org.apache.jena.rdf.model.Model;

import java.util.List;

/**
 * This interface provides the functionality to store data in different formats. The data can be stored in CSV files
 * or in RDF models.
 */
public interface Storable {

    record CSVData (
            String folderName,
            List<CSVFileData> files
    ) {
        public record CSVFileData(String filename, String[][] data) {}
    }

    interface AsCSV extends Storable {

        /**
         * Converts the data into CSV files. The key of the map contains the file name for the linked entries.
         *
         * @return CSVFileData list which contains all the files and their data that should be created and stored
         */
        CSVData toCSV();
    }

    interface AsRDF extends Storable {

        /**
         * Converts the data into an RDF model, which will be added to the appropriate storages.
         *
         * @return RDF model that contains the data
         */
        Model toRDF();
    }

}
