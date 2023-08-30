package org.aksw.iguana.cc.storage.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.metrics.*;
import org.aksw.iguana.cc.metrics.impl.AggregatedExecutionStatistics;
import org.aksw.iguana.cc.metrics.impl.EachExecutionStatistic;
import org.aksw.iguana.cc.storage.Storable;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;
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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

public class CSVStorage implements Storage {

    public record Config(
            @JsonProperty String path
    ) implements StorageConfig {}

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVStorage.class);

    private final Path suiteFolder;
    private Path currentFolder;
    private final Path taskFile;

    private List<Resource> workerResources;
    private Resource taskRes;
    private String connection;
    private String dataset;

    public CSVStorage(Config config) {
        this(config.path());
    }

    // TODO: better error handling
    public CSVStorage(String folderPath) {
        Path parentFolder;
        try {
            parentFolder = Paths.get(folderPath);
        } catch (InvalidPathException e) {
            LOGGER.error("Can't store csv files, the given path is invalid.", e);
            this.suiteFolder = null;
            this.taskFile = null;
            return;
        }

        // TODO: add the id suite back
        this.suiteFolder = parentFolder.resolve(String.valueOf(UUID.randomUUID()));
        this.taskFile = this.suiteFolder.resolve("suite-summary.csv");

        if (Files.notExists(parentFolder)) {
            try {
                Files.createDirectory(parentFolder);
            } catch (IOException e) {
                LOGGER.error("Can't store csv files, directory couldn't be created.", e);
                return;
            }
        }

        if (Files.notExists(suiteFolder)) {
            try {
                Files.createDirectory(suiteFolder);
            } catch (IOException e) {
                LOGGER.error("Can't store csv files, directory couldn't be created.", e);
                return;
            }
        }

        try {
            Files.createFile(taskFile);
        } catch (IOException e) {
            LOGGER.error("Couldn't create the file: " + taskFile.toAbsolutePath(), e);
            return;
        }

        // write headers for the tasks.csv file
        try (CSVWriter csvWriter = getCSVWriter(taskFile)) {
            Metric[] taskMetrics = MetricManager.getMetrics().stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);
            List<String> headerList = new LinkedList<>();
            headerList.addAll(List.of("connection", "dataset", "startDate", "endDate", "noOfWorkers"));
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

        this.currentFolder = this.suiteFolder.resolve(this.connection + "-" + this.dataset);
        try {
            Files.createDirectory(this.currentFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            storeTaskResults(data);
        } catch (IOException e) {
            LOGGER.error("Error while storing the task result in a csv file.", e);
        } catch (NoSuchElementException | ParseException e) {
            LOGGER.error("Error while storing the task result in a csv file. The given model is probably incorrect.", e);
        }

        try {
            Path temp = createCSVFile("worker", "summary");
            storeWorkerResults(this.taskRes, temp, data);
            for (Resource workerRes : workerResources) {
                String workerID = data.listObjectsOfProperty(workerRes, IPROP.workerID).next().asLiteral().getLexicalForm();
                try {
                    Path file = createCSVFile("query", "summary", "worker", workerID);
                    Path file2 = createCSVFile("each", "execution", "worker", workerID);
                    storeSummarizedQueryResults(workerRes, file, data);
                    storeEachQueryResults(workerRes, file2, data);
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
            storeSummarizedQueryResults(taskRes, file, data);
        } catch (IOException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file.", e);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file. The given model is probably incorrect.", e);
        }

        try {
            createLanguageProcessorFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createLanguageProcessorFiles() throws IOException {
        Map<String, ResponseBodyProcessor> rbpMap = ResponseBodyProcessorInstances.getEveryProcessor();
        for (String responseType : rbpMap.keySet()) {
            var responseDataMetrics = rbpMap.get(responseType).getResponseDataMetrics();
            if (responseDataMetrics.isEmpty()) {
                continue;
            }

            Path langProcDir = Path.of(responseDataMetrics.get(0).processor().getSimpleName().replaceAll("\\W+", "-")); // sanitize filename
            langProcDir = this.currentFolder.resolve(langProcDir);
            Files.createDirectory(langProcDir);
            for (LanguageProcessor.LanguageProcessingData singleData : responseDataMetrics) {
                List<Storable.CSVFileData> csvDataList;
                if (singleData instanceof Storable.AsCSV) {
                    csvDataList = ((Storable.AsCSV) singleData).toCSV();
                } else {
                    continue;
                }

                for (var csvData : csvDataList) {
                    // check for file extension
                    String filename = csvData.filename().endsWith(".csv") ? csvData.filename() : csvData.filename() + ".csv";
                    Path file = langProcDir.resolve(filename);

                    int i = 1; // skip the header by default

                    if (Files.notExists(file)) {
                        Files.createFile(file);
                        i = 0; // include header if file is new
                    }

                    try (CSVWriter writer = getCSVWriter(file)) {
                        for (; i < csvData.data().length; i++) {
                            writer.writeNext(csvData.data()[i], true);
                        }
                    }
                }
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
        List<String> datasets = new ArrayList<>();
        ResIterator resIterator = data.listSubjectsWithProperty(RDF.type, IONT.dataset);
        while (resIterator.hasNext()) {
            Resource datasetRes = resIterator.nextResource();
            NodeIterator nodeIterator = data.listObjectsOfProperty(datasetRes, RDFS.label);
            datasets.add(nodeIterator.next().asLiteral().getLexicalForm());
        }
        this.dataset = String.join(";", datasets);

        List<String> connections = new ArrayList<>();
        resIterator = data.listSubjectsWithProperty(RDF.type, IONT.connection);
        while (resIterator.hasNext()) {
            Resource connectionRes = resIterator.nextResource();
            NodeIterator nodeIterator = data.listObjectsOfProperty(connectionRes, RDFS.label);
            String conString = nodeIterator.next().asLiteral().getLexicalForm();
            nodeIterator = data.listObjectsOfProperty(connectionRes, IPROP.version);
            if (nodeIterator.hasNext()) {
                String conVersionString = nodeIterator.next().toString();
                connections.add(conString + "#" + conVersionString);
            } else {
                connections.add(conString);
            }

        }
        this.connection = String.join(";", connections);

        resIterator = data.listSubjectsWithProperty(RDF.type, IONT.task);
        this.taskRes = resIterator.nextResource();

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

    private static void storeSummarizedQueryResults(Resource parentRes, Path file, Model data) throws IOException, NoSuchElementException {
        boolean containsAggrStats = !MetricManager.getMetrics().stream().filter(AggregatedExecutionStatistics.class::isInstance).toList().isEmpty();
        Metric[] queryMetrics = MetricManager.getMetrics().stream().filter(x -> QueryMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(parentRes, IPROP.query, "?eQ");
        queryProperties(sb, "?eQ", IPROP.queryID);
        if (containsAggrStats) {
            queryProperties(sb, "?eQ", IPROP.succeeded, IPROP.failed, IPROP.totalTime, IPROP.resultSize, IPROP.wrongCodes, IPROP.timeOuts, IPROP.unknownException);
        }
        queryMetrics(sb, "?eQ", queryMetrics);

        executeAndStoreQuery(sb, file, data);
    }

    private static void storeEachQueryResults(Resource parentRes, Path file, Model data) throws IOException {
        boolean containsEachStats = !MetricManager.getMetrics().stream().filter(EachExecutionStatistic.class::isInstance).toList().isEmpty();
        if (!containsEachStats) {
            return;
        }

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(parentRes, IPROP.query, "?eQ") // variable name should be different from property names
                .addWhere("?eQ", IPROP.queryExecution, "?exec")
                .addOptional(new WhereBuilder().addWhere("?exec", IPROP.responseBody, "?rb").addWhere("?rb", IPROP.responseBodyHash, "?responseBodyHash"));
        queryProperties(sb, "?exec", IPROP.queryID, IPROP.run, IPROP.success, IPROP.time, IPROP.resultSize, IPROP.code);
        sb.addVar("responseBodyHash");
        executeAndStoreQuery(sb, file, data);
    }

    private void storeTaskResults(Model data) throws IOException, NoSuchElementException, ParseException {
        Metric[] taskMetrics = MetricManager.getMetrics().stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        queryProperties(sb, String.format("<%s>", this.taskRes.toString()), IPROP.startDate, IPROP.endDate, IPROP.noOfWorkers);
        queryMetrics(sb, String.format("<%s>", this.taskRes.toString()), taskMetrics);

        try(QueryExecution exec = QueryExecutionFactory.create(sb.build(), data);
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
                String[] newRow = new String[row.length + 2];
                newRow[0] = connection;
                newRow[1] = dataset;
                System.arraycopy(row, 0, newRow, 2, row.length);
                csvWriter.writeNext(newRow, true);
            } catch (CsvValidationException ignored) {
                // shouldn't happen
            }
        }
    }

    private static void storeWorkerResults(Resource taskRes, Path file, Model data) throws IOException, NoSuchElementException {
        Metric[] workerMetrics = MetricManager.getMetrics().stream().filter(x -> WorkerMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(taskRes, IPROP.workerResult, "?worker");
        queryProperties(sb, "?worker", IPROP.workerID, IPROP.workerType, IPROP.noOfQueries, IPROP.timeOut);
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
}
