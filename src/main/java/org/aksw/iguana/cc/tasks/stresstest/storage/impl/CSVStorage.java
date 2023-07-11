package org.aksw.iguana.cc.tasks.stresstest.storage.impl;

import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import org.aksw.iguana.cc.config.IguanaConfig;
import org.aksw.iguana.cc.tasks.stresstest.metrics.*;
import org.aksw.iguana.cc.tasks.stresstest.metrics.impl.AggregatedExecutionStatistics;
import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

@Shorthand("CSVStorage")
public class CSVStorage implements Storage {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVStorage.class);

    private final Path folder;
    private final Path taskFile;

    private List<Resource> workerResources;
    private Resource taskRes;
    private String connection;
    private String connectionVersion;
    private String dataset;

    public CSVStorage(String folderPath) {
        Path parentFolder;
        try {
            parentFolder = Paths.get(folderPath);
        } catch (InvalidPathException e) {
            LOGGER.error("Can't store csv files, the given path is invalid.", e);
            this.folder = null;
            this.taskFile = null;
            return;
        }

        this.folder = parentFolder.resolve(IguanaConfig.getSuiteID());
        this.taskFile = this.folder.resolve("tasks-overview.csv");

        if (Files.notExists(parentFolder)) {
            try {
                Files.createDirectory(parentFolder);
            } catch (IOException e) {
                LOGGER.error("Can't store csv files, directory couldn't be created.", e);
                return;
            }
        }

        if (Files.notExists(folder)) {
            try {
                Files.createDirectory(folder);
            } catch (IOException e) {
                LOGGER.error("Can't store csv files, directory couldn't be created.", e);
                return;
            }
        }

        // write headers for the tasks.csv file
        try {
            Files.createFile(taskFile);
        } catch (IOException e) {
            LOGGER.error("Couldn't create the file: " + taskFile.toAbsolutePath(), e);
            return;
        }

        // This only works because the metrics are initialized sooner
        try (CSVWriter csvWriter = getCSVWriter(taskFile)) {
            Metric[] taskMetrics = MetricManager.getMetrics().stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);
            List<String> headerList = new LinkedList<>();
            headerList.addAll(List.of("dataset", "connection", "startDate", "endDate", "noOfWorkers"));
            headerList.addAll(Arrays.stream(taskMetrics).map(Metric::getAbbreviation).toList());
            String[] header = headerList.toArray(String[]::new);
            csvWriter.writeNext(header, true);
        } catch (IOException e) {
            LOGGER.error("Error while writing to file: " + taskFile.toAbsolutePath(), e);
        }
    }

    /**
     * Stores the task result into the storage. This method will be executed after a task has finished.
     *
     * @param data the given result model
     */
    @Override
    public void storeResult(Model data) {
        try {
            setObjectAttributes(data);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while querying the result model. The given model is probably incorrect.", e);
            return;
        }

        try {
            storeTaskResults(data);
        } catch (IOException e) {
            LOGGER.error("Error while storing the task result in a csv file.", e);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while storing the task result in a csv file. The given model is probably incorrect.", e);
        }

        // if there is only one worker, the values are going to be the same as from the task
        if (workerResources.size() > 1) {
            try {
                Path temp = createCSVFile(dataset, connection, connectionVersion, "worker");
                storeWorkerResults(workerResources, temp, data);
                for (Resource workerRes : workerResources) {
                    String workerID = data.listObjectsOfProperty(workerRes, IPROP.workerID).next().asLiteral().getLexicalForm();
                    try {
                        Path file = createCSVFile(dataset, connection, connectionVersion, "worker", "query", workerID);
                        storeQueryResults(workerRes, file, data);
                    } catch (IOException e) {
                        LOGGER.error("Error while storing the query results of a worker in a csv file.", e);
                    } catch (NoSuchElementException e) {
                        LOGGER.error("Error while storing the query results of a worker in a csv file. The given model is probably incorrect.", e);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error while storing the worker results in a csv file.", e);
            } catch (NoSuchElementException e) {
                LOGGER.error("Error while storing the worker results in a csv file. The given model is probably incorrect.", e);
            }
        }

        try {
            Path file = createCSVFile(dataset, connection, connectionVersion, "query");
            storeQueryResults(taskRes, file, data);
        } catch (IOException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file.", e);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file. The given model is probably incorrect.", e);
        }
    }

    /**
     * This method sets the objects attributes by querying the given model.
     *
     * @param data the result model
     * @throws NoSuchElementException might be thrown if the model is incorrect
     */
    private void setObjectAttributes(Model data) throws NoSuchElementException {
        ResIterator resIterator = data.listSubjectsWithProperty(RDF.type, IONT.dataset);
        Resource datasetRes = resIterator.nextResource();
        NodeIterator nodeIterator = data.listObjectsOfProperty(datasetRes, RDFS.label);
        this.dataset = nodeIterator.next().asLiteral().getLexicalForm();

        resIterator = data.listSubjectsWithProperty(RDF.type, IONT.connection);
        Resource connectionRes = resIterator.nextResource();
        nodeIterator = data.listObjectsOfProperty(connectionRes, RDFS.label);
        this.connection = nodeIterator.next().asLiteral().getLexicalForm();
        this.connectionVersion = "";
        nodeIterator = data.listObjectsOfProperty(connectionRes, IPROP.version);
        if (nodeIterator.hasNext()) {
            this.connectionVersion = nodeIterator.next().toString();
        }

        resIterator = data.listSubjectsWithProperty(RDF.type, IONT.task);
        this.taskRes = resIterator.nextResource();

        nodeIterator = data.listObjectsOfProperty(this.taskRes, IPROP.workerResult);
        this.workerResources = nodeIterator.toList().stream().map(RDFNode::asResource).toList();
    }

    /**
     * Creates a CSV file with the given name values that will be located inside the parent folder. The name value are
     * joined together with the character '-'. Empty values will be ignored.
     *
     * @param nameValues strings that build up the name of the file
     * @throws IOException if an I/O error occurs
     * @return path object to the created CSV file
     */
    private Path createCSVFile(String... nameValues) throws IOException {
        // remove empty string values
        nameValues = Arrays.stream(nameValues).filter(Predicate.not(String::isEmpty)).toArray(String[]::new);
        String filename = String.join("-", nameValues) + ".csv";
        Path file = this.folder.resolve(filename);
        Files.createFile(file);
        return file;
    }

    private static void storeQueryResults(Resource parentRes, Path file, Model data) throws IOException, NoSuchElementException {
        NodeIterator nodeIterator = data.listObjectsOfProperty(parentRes, IPROP.query);
        List<Resource> queryResources = nodeIterator.toList().stream().map(RDFNode::asResource).toList();
        int values = 1;
        boolean containsAggrStats = !MetricManager.getMetrics().stream().filter(AggregatedExecutionStatistics.class::isInstance).toList().isEmpty();
        Metric[] queryMetrics = MetricManager.getMetrics().stream().filter(x -> QueryMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);
        try (CSVWriter csvWriter = getCSVWriter(file)) {
            List<String> headerList = new LinkedList<>();
            headerList.add("queryID");
            if (containsAggrStats) {
                headerList.addAll(List.of("succeeded", "failed", "totalTime", "resultSize", "wrongCodes", "timeOuts", "unknownExceptions"));
                values += 7;
            }
            headerList.addAll(Arrays.stream(queryMetrics).map(Metric::getAbbreviation).toList());
            csvWriter.writeNext(headerList.toArray(String[]::new));
            for (Resource queryRes : queryResources) {
                String[] line = new String[values + queryMetrics.length];
                line[0] = data.listObjectsOfProperty(queryRes, IPROP.queryID).next().toString();
                if (containsAggrStats) {
                    // might happen if queries weren't executed
                    try {
                        line[1] = data.listObjectsOfProperty(queryRes, IPROP.succeeded).next().asLiteral().getLexicalForm();
                        line[2] = data.listObjectsOfProperty(queryRes, IPROP.failed).next().asLiteral().getLexicalForm();
                        line[3] = data.listObjectsOfProperty(queryRes, IPROP.totalTime).next().asLiteral().getLexicalForm();
                        line[4] = data.listObjectsOfProperty(queryRes, IPROP.resultSize).next().asLiteral().getLexicalForm();
                        line[5] = data.listObjectsOfProperty(queryRes, IPROP.wrongCodes).next().asLiteral().getLexicalForm();
                        line[6] = data.listObjectsOfProperty(queryRes, IPROP.timeOuts).next().asLiteral().getLexicalForm();
                        line[7] = data.listObjectsOfProperty(queryRes, IPROP.unknownException).next().asLiteral().getLexicalForm();
                    } catch (NoSuchElementException e) {
                        continue;
                    }
                }
                for (int i = 0; i < queryMetrics.length; i++) {
                    try {
                        line[values + i] = data.listObjectsOfProperty(queryRes, IPROP.createMetricProperty(queryMetrics[i])).next().asLiteral().getLexicalForm();
                    } catch (NoSuchElementException e) {
                        line[values + i] = "";
                    }
                }
                csvWriter.writeNext(line, true);
            }
        }
    }

    private void storeTaskResults(Model data) throws IOException, NoSuchElementException {
        final int values = 5;
        Metric[] taskMetrics = MetricManager.getMetrics().stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);
        try (CSVWriter csvWriter = getCSVWriter(this.taskFile)) {
            String[] line = new String[values + taskMetrics.length];
            // taskID ?
            line[0] = this.dataset;
            line[1] = this.connection;
            line[2] = data.listObjectsOfProperty(this.taskRes, IPROP.startDate).next().asLiteral().getLexicalForm();
            line[3] = data.listObjectsOfProperty(this.taskRes, IPROP.endDate).next().asLiteral().getLexicalForm();
            line[4] = String.valueOf(this.workerResources.size());
            for (int i = 0; i < taskMetrics.length; i++) {
                try {
                    line[values + i] = data.listObjectsOfProperty(this.taskRes, IPROP.createMetricProperty(taskMetrics[i])).next().asLiteral().getLexicalForm();
                } catch (NoSuchElementException ignored) {}
            }
            csvWriter.writeNext(line, true);
        }
    }

    private static void storeWorkerResults(List<Resource> workerResources, Path file, Model data) throws IOException, NoSuchElementException {
        try (CSVWriter csvWriter = getCSVWriter(file)) {
            Metric[] workerMetrics = MetricManager.getMetrics().stream().filter(x -> WorkerMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);
            List<String> headerList = new LinkedList<>();
            headerList.addAll(List.of("workerID", "workerType", "noOfQueries", "timeOut"));
            headerList.addAll(Arrays.stream(workerMetrics).map(Metric::getAbbreviation).toList());
            csvWriter.writeNext(headerList.toArray(String[]::new));
            for (Resource workerRes : workerResources) {
                int defaultNumber = 4;
                String[] line = new String[defaultNumber + workerMetrics.length];
                line[0] = data.listObjectsOfProperty(workerRes, IPROP.workerID).next().asLiteral().getLexicalForm();
                line[1] = data.listObjectsOfProperty(workerRes, IPROP.workerType).next().asLiteral().getLexicalForm();
                line[2] = data.listObjectsOfProperty(workerRes, IPROP.noOfQueries).next().asLiteral().getLexicalForm();
                line[3] = data.listObjectsOfProperty(workerRes, IPROP.timeOut).next().asLiteral().getLexicalForm();
                for (int i = 0; i < workerMetrics.length; i++) {
                    // Workers might miss metrics, which is fine
                    try {
                        line[defaultNumber + i] = data.listObjectsOfProperty(workerRes, IPROP.createMetricProperty(workerMetrics[i])).next().asLiteral().getLexicalForm();
                    } catch (NoSuchElementException ignored) {}
                }
                csvWriter.writeNext(line, true);
            }
        }
    }

    private static CSVWriter getCSVWriter(Path file) throws IOException {
        return (CSVWriter) new CSVWriterBuilder(new FileWriter(file.toAbsolutePath().toString(), true))
                .withQuoteChar('\"')
                .withSeparator(',')
                .withLineEnd("\n")
                .build();
    }
}
