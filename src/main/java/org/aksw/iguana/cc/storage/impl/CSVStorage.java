package org.aksw.iguana.cc.storage.impl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.metrics.*;
import org.aksw.iguana.cc.metrics.impl.AggregatedExecutionStatistics;
import org.aksw.iguana.cc.metrics.impl.EachExecutionStatistic;
import org.aksw.iguana.cc.storage.Storable;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

public class CSVStorage implements Storage {

    /** This private record is used to store information about the connections used in a task. */
    private record ConnectionInfo(String connection, String version, String dataset) {}

    public record Config(String directory) implements StorageConfig {
        public Config(String directory) {
            if (directory == null) {
                directory = "results";
            }
            Path path = Paths.get(directory);
            if (Files.exists(path) && !Files.isDirectory(path)) {
                throw new IllegalArgumentException("The given path is not a directory.");
            }
            this.directory = directory;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVStorage.class);

    private final List<Metric> metrics;

    private final Path suiteFolder;
    private Path currentFolder;
    private final Path taskFile;
    private final Path taskConfigFile;

    private List<Resource> workerResources;
    private Resource taskRes;
    List<ConnectionInfo> connections;

    public CSVStorage(Config config, List<Metric> metrics, String suiteID) {
        this(config.directory(), metrics, suiteID);
    }

    public CSVStorage(String folderPath, List<Metric> metrics, String suiteID) {
        this.metrics = metrics;

        Path parentFolder;
        try {
            parentFolder = Paths.get(folderPath);
        } catch (InvalidPathException e) {
            LOGGER.error("Can't store csv files, the given path is invalid.", e);
            this.suiteFolder = null;
            this.taskFile = null;
            this.taskConfigFile = null;
            return;
        }

        this.suiteFolder = parentFolder.resolve("suite-" + suiteID);
        this.taskFile = this.suiteFolder.resolve("suite-summary.csv");
        this.taskConfigFile = this.suiteFolder.resolve("task-configuration.csv");

        if (Files.notExists(suiteFolder)) {
            try {
                Files.createDirectories(suiteFolder);
            } catch (IOException e) {
                LOGGER.error("Can't store csv files, directory could not be created.", e);
                return;
            }
        }

        try {
            Files.createFile(taskFile);
        } catch (IOException e) {
            LOGGER.error("Couldn't create the file: " + taskFile.toAbsolutePath(), e);
            return;
        }

        try {
            Files.createFile(taskConfigFile);
        } catch (IOException e) {
            LOGGER.error("Couldn't create the file: " + taskFile.toAbsolutePath(), e);
            return;
        }

        // write headers for the suite-summary.csv file
        try (CSVWriter csvWriter = getCSVWriter(taskFile)) {
            Metric[] taskMetrics = metrics.stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);
            List<String> headerList = new LinkedList<>();
            // headerList.addAll(List.of("connection", "dataset", "startDate", "endDate", "noOfWorkers"));
            headerList.addAll(List.of("taskID", "startDate", "endDate", "noOfWorkers"));
            headerList.addAll(Arrays.stream(taskMetrics).map(Metric::getAbbreviation).toList());
            String[] header = headerList.toArray(String[]::new);
            csvWriter.writeNext(header, true);
        } catch (IOException e) {
            LOGGER.error("Error while writing to file: " + taskFile.toAbsolutePath(), e);
        }

        // write headers for the task-configuration.csv file
        try (CSVWriter csvWriter = getCSVWriter(taskConfigFile)) {
            csvWriter.writeNext(new String[]{"taskID", "connection", "version", "dataset"}, true);
        } catch (IOException e) {
            LOGGER.error("Error while writing to file: " + taskConfigFile.toAbsolutePath(), e);
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

        this.currentFolder = this.suiteFolder.resolve("task-" + retrieveTaskID(this.taskRes));
        try {
            Files.createDirectory(this.currentFolder);
        } catch (IOException e) {
            LOGGER.error("Error while storing the task result in a csv file.", e);
        }

        try {
            storeTaskInfo();
            storeTaskResults(data);
        } catch (IOException e) {
            LOGGER.error("Error while storing the task result in a csv file.", e);
        } catch (NoSuchElementException | ParseException e) {
            LOGGER.error("Error while storing the task result in a csv file. The given model is probably incorrect.", e);
        }

        try {
            Path temp = createCSVFile("worker", "summary");
            storeWorkerResults(this.taskRes, temp, data, this.metrics);
            for (Resource workerRes : workerResources) {
                String workerID = data.listObjectsOfProperty(workerRes, IPROP.workerID).next().asLiteral().getLexicalForm();
                try {
                    Path file = createCSVFile("query", "summary", "worker", workerID);
                    Path file2 = createCSVFile("each", "execution", "worker", workerID);
                    storeSummarizedQueryResults(workerRes, file, data, this.metrics);
                    storeEachQueryResults(workerRes, file2, data, this.metrics);
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

        try {
            Path file = createCSVFile("query", "summary", "task");
            storeSummarizedQueryResults(taskRes, file, data, this.metrics);
        } catch (IOException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file.", e);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file. The given model is probably incorrect.", e);
        }
    }

    @Override
    public void storeData(Storable data) {
        if (!(data instanceof Storable.AsCSV)) return; // dismiss data if it can't be stored as csv
        Storable.CSVData csvdata = ((Storable.AsCSV) data).toCSV();

        Path responseTypeDir = Path.of(csvdata.folderName());
        responseTypeDir = this.currentFolder.resolve(responseTypeDir);

        try {
            Files.createDirectory(responseTypeDir);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            LOGGER.error("Error while creating the directory for the language processor results. ", e);
            return;
        }

        for (var csvFile : csvdata.files()) {
            // check for file extension
            String filename = csvFile.filename().endsWith(".csv") ? csvFile.filename() : csvFile.filename() + ".csv";
            Path file = responseTypeDir.resolve(filename);

            int i = 1; // skip the header by default

            if (Files.notExists(file)) {
                try {
                    Files.createFile(file);
                } catch (IOException e) {
                    LOGGER.error("Error while creating a csv file for language processor results. The storing of language processor results will be skipped.", e);
                    return;
                }
                i = 0; // include header if file is new
            }

            try (CSVWriter writer = getCSVWriter(file)) {
                for (; i < csvFile.data().length; i++) {
                    writer.writeNext(csvFile.data()[i], true);
                }
            } catch (IOException e) {
                LOGGER.error("Error while writing the data into a csv file for language processor results. The storing of language processor results will be skipped.", e);
                return;
            }
        }
    }

    /**
     * This method sets the objects attributes by querying the given model.
     *
     * @param data the result model
     * @throws NoSuchElementException might be thrown if the model is incorrect
     */
    private void setObjectAttributes(Model data) throws NoSuchElementException {
        // obtain connection information of task
        this.connections = new ArrayList<>();
        ResIterator resIterator = data.listSubjectsWithProperty(RDF.type, IONT.connection);
        while (resIterator.hasNext()) {
            Resource connectionRes = resIterator.nextResource();
            NodeIterator nodeIterator = data.listObjectsOfProperty(connectionRes, RDFS.label);
            String conString = nodeIterator.next().asLiteral().getLexicalForm();

            // obtain connection version
            String conVersionString = "";
            nodeIterator = data.listObjectsOfProperty(connectionRes, IPROP.version);
            if (nodeIterator.hasNext()) {
                conVersionString = nodeIterator.next().toString();
            }

            // obtain dataset
            String conDatasetString = "";
            nodeIterator = data.listObjectsOfProperty(connectionRes, IPROP.dataset);
            if (nodeIterator.hasNext()) {
                conDatasetString = nodeIterator.next().toString();
            }
            this.connections.add(new ConnectionInfo(conString, conVersionString, conDatasetString));
        }

        // obtain task type
        resIterator = data.listSubjectsWithProperty(RDF.type, IONT.task);
        this.taskRes = resIterator.nextResource();

        // obtain worker resources
        NodeIterator nodeIterator = data.listObjectsOfProperty(this.taskRes, IPROP.workerResult);
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
        Path file = this.currentFolder.resolve(filename);
        Files.createFile(file);
        return file;
    }

    private static void storeSummarizedQueryResults(Resource parentRes, Path file, Model data, List<Metric> metrics) throws IOException, NoSuchElementException {
        boolean containsAggrStats = !metrics.stream().filter(AggregatedExecutionStatistics.class::isInstance).toList().isEmpty();
        Metric[] queryMetrics = metrics.stream().filter(x -> QueryMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(parentRes, IPROP.query, "?eQ");
        queryProperties(sb, "?eQ", IPROP.queryID);
        if (containsAggrStats) {
            queryProperties(sb, "?eQ", IPROP.succeeded, IPROP.failed, IPROP.totalTime, IPROP.resultSize, IPROP.wrongCodes, IPROP.timeOuts, IPROP.unknownException);
        }
        queryMetrics(sb, "?eQ", queryMetrics);

        executeAndStoreQuery(sb, file, data);
    }

    private static void storeEachQueryResults(Resource parentRes, Path file, Model data, List<Metric> metrics) throws IOException {
        boolean containsEachStats = !metrics.stream().filter(EachExecutionStatistic.class::isInstance).toList().isEmpty();
        if (!containsEachStats) {
            return;
        }

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(parentRes, IPROP.query, "?eQ") // variable name should be different from property names
                .addWhere("?eQ", IPROP.queryExecution, "?exec")
                .addOptional(new WhereBuilder().addWhere("?exec", IPROP.responseBody, "?rb").addWhere("?rb", IPROP.responseBodyHash, "?responseBodyHash"))
                .addOptional(new WhereBuilder().addWhere("?exec", IPROP.exception, "?exception"))
                .addOptional(new WhereBuilder().addWhere("?exec", IPROP.httpCode, "?httpCode"));
        queryProperties(sb, "?exec", IPROP.queryID, IPROP.run, IPROP.success, IPROP.startTime, IPROP.time, IPROP.resultSize, IPROP.code);
        sb.addVar("httpCode").addVar("exception").addVar("responseBodyHash");
        executeAndStoreQuery(sb, file, data);
    }

    /**
     * Stores the current task information into the task configuration file.
     */
    private void storeTaskInfo() {
        try (CSVWriter csvWriter = getCSVWriter(taskConfigFile)) {
            for (ConnectionInfo connectionInfo : connections) {
                csvWriter.writeNext(new String[]{this.taskRes.toString(), connectionInfo.connection(), connectionInfo.version(), connectionInfo.dataset()}, true);
            }
        } catch (IOException e) {
            LOGGER.error("Error while writing to file: " + taskConfigFile.toAbsolutePath(), e);
        }
    }

    private void storeTaskResults(Model data) throws IOException, NoSuchElementException, ParseException {
        Metric[] taskMetrics = metrics.stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        queryProperties(sb, String.format("<%s>", this.taskRes.toString()), IPROP.startDate, IPROP.endDate, IPROP.noOfWorkers);
        queryMetrics(sb, String.format("<%s>", this.taskRes.toString()), taskMetrics);

        try (QueryExecution exec = QueryExecutionFactory.create(sb.build(), data);
            CSVWriter csvWriter = getCSVWriter(taskFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ResultSet results = exec.execSelect();
            ResultSetFormatter.outputAsCSV(baos, results);

            // workaround to remove the created header from the ResultSetFormatter
            CSVReader reader = new CSVReader(new StringReader(baos.toString()));
            try {
                reader.readNext();

                // inject connection and dataset information
                String[] row = reader.readNext();
                String[] newRow = new String[row.length + 1];
                newRow[0] = this.taskRes.getURI();
                // newRow[0] = connection;
                // newRow[1] = dataset;
                System.arraycopy(row, 0, newRow, 1, row.length);
                csvWriter.writeNext(newRow, true);
            } catch (CsvValidationException ignored) {
                // shouldn't happen
            }
        }
    }

    private static void storeWorkerResults(Resource taskRes, Path file, Model data, List<Metric> metrics) throws IOException, NoSuchElementException {
        Metric[] workerMetrics = metrics.stream().filter(x -> WorkerMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(taskRes, IPROP.workerResult, "?worker");
        queryProperties(sb, "?worker", IPROP.workerID, IPROP.workerType, IPROP.noOfQueries, IPROP.timeOut, IPROP.startDate, IPROP.endDate);
        queryMetrics(sb, "?worker", workerMetrics);

        executeAndStoreQuery(sb, file, data);
    }

    private static CSVWriter getCSVWriter(Path file) throws IOException {
        return (CSVWriter) new CSVWriterBuilder(new FileWriter(file.toAbsolutePath().toString(), true))
                .withQuoteChar('\"')
                .withSeparator(',')
                .withLineEnd("\n")
                .build();
    }

    private static void queryProperties(SelectBuilder sb, String variable, Property... properties) {
        for (Property prop : properties) {
            sb.addVar(prop.getLocalName()).addWhere(variable, prop, "?" + prop.getLocalName());
        }
    }

    private static void queryMetrics(SelectBuilder sb, String variable, Metric[] metrics) {
        for (Metric m : metrics) {
            // Optional, in case metric isn't created, because of failed executions
            sb.addVar(m.getAbbreviation()).addOptional(variable, IPROP.createMetricProperty(m), "?" + m.getAbbreviation());
        }
    }

    private static void executeAndStoreQuery(SelectBuilder sb, Path file, Model data) throws IOException {
        try(QueryExecution exec = QueryExecutionFactory.create(sb.build(), data);
            FileOutputStream fos = new FileOutputStream(file.toFile())) {
            ResultSet results = exec.execSelect();
            ResultSetFormatter.outputAsCSV(fos, results);
        }
    }

    /**
     * Retrieves the task ID from the given task resource. The current model doesn't save the task ID as a property of
     * the task resource. Therefore, the task ID is extracted from the URI of the task resource.
     *
     * @param taskRes the task resource
     * @return the task ID
     */
    private static String retrieveTaskID(Resource taskRes) {
        return taskRes.getURI().substring(taskRes.getURI().lastIndexOf("/") + 1);
    }
}
