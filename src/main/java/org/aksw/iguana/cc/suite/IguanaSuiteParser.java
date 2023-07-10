package org.aksw.iguana.cc.suite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.config.elements.DatasetConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates an IguanaConfig from a given JSON or YAML file, and validates the config using a JSON schema file
 */
public class IguanaSuiteParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(IguanaSuiteParser.class);

    private static final String schemaFile = "iguana-schema.json";

    enum DataFormat {
        YAML, JSON;

        public static DataFormat getFormat(Path file) {
            final var extension = FilenameUtils.getExtension(file.toString());
            switch (extension) {
                case "yml", "yaml" -> {
                    return YAML;
                }
                case "json" -> {
                    return JSON;
                }
                default -> throw new IllegalStateException("Unexpected suite file extension: " + extension);
            }
        }
    }


    public static Suite parse(Path config) throws IOException {
        return parse(new FileInputStream(config.toFile()), DataFormat.getFormat(config), true);
    }

    public static Suite parse(InputStream stream, DataFormat format) throws IOException {
        return parse(stream, format, true);
    }


    public static Suite parse(InputStream inputStream, DataFormat format, Boolean validate) throws IOException {
        JsonFactory factory = switch (format) {
            case YAML -> new YAMLFactory();
            case JSON -> new JsonFactory();
        };
        SuiteConfigWithID configWithID = parse(inputStream, factory, validate);
        return new Suite(configWithID.id(), configWithID.config());
    }

    record SuiteConfigWithID(long id, Suite.Config config) {
    }

    /**
     * Parses a IGUANA configuration file.
     * <p>
     * This involves two steps: First, datasets and connections are parsed and stored. In a second step, the rest of the file is parsed. If the names of datasets and connections are used , they are replaced with the respective configurations that were parsed in the first step.
     *
     * @param inputStream
     * @param factory
     * @param validate
     * @return
     * @throws IOException
     */
    private static SuiteConfigWithID parse(InputStream inputStream, JsonFactory factory, Boolean validate) throws IOException {
        ObjectMapper mapper = new ObjectMapper(factory);

        final var input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final var datasets = preparseDataset(mapper, input);

        class DatasetDeserializer extends StdDeserializer<DatasetConfig> {
            public DatasetDeserializer() {
                this(null);
            }

            protected DatasetDeserializer(Class<?> vc) {
                super(vc);
            }

            @Override
            public DatasetConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                JsonNode node = jp.getCodec().readTree(jp);
                if (node.isTextual()) {
                    final var datasetName = node.asText();
                    if (!datasets.containsKey(datasetName))
                        throw new IllegalStateException(MessageFormat.format("Unknown dataset name: {0}", datasetName));
                    return datasets.get(datasetName);
                } else {
                    DatasetConfig datasetConfig = ctxt.readValue(jp, DatasetConfig.class);
                    if (datasets.containsKey(datasetConfig.name()))
                        assert datasets.get(datasetConfig.name()) == datasetConfig;
                    else datasets.put(datasetConfig.name(), datasetConfig);
                    return datasetConfig; // TODO: double check if this really works
                }
            }
        }
        mapper = new ObjectMapper(factory).registerModule(new SimpleModule()
                .addDeserializer(DatasetConfig.class, new DatasetDeserializer()));

        final var connections = preparseConnections(mapper, input);

        class ConnectionDeserializer extends StdDeserializer<ConnectionConfig> {

            public ConnectionDeserializer() {
                this(null);
            }

            protected ConnectionDeserializer(Class<?> vc) {
                super(vc);
            }

            @Override
            public ConnectionConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                JsonNode node = jp.getCodec().readTree(jp);
                if (node.isTextual()) {
                    final var connectionName = node.asText();
                    if (!connections.containsKey(connectionName))
                        throw new IllegalStateException(MessageFormat.format("Unknown connection name: {0}", connectionName));
                    return connections.get(connectionName);
                } else {
                    ConnectionConfig connectionConfig = ctxt.readValue(jp, ConnectionConfig.class);
                    if (connections.containsKey(connectionConfig.name()))
                        assert connections.get(connectionConfig.name()) == connectionConfig;
                    else connections.put(connectionConfig.name(), connectionConfig);
                    return connectionConfig; // TODO: double check if this really works
                }
            }
        }

        final var queryHandlers = new HashMap<QueryHandler.Config, QueryHandler>();

        class QueryHandlerDeserializer extends StdDeserializer<QueryHandler> {

            public QueryHandlerDeserializer() {
                this(null);
            }

            protected QueryHandlerDeserializer(Class<?> vc) {
                super(vc);
            }

            @Override
            public QueryHandler deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                QueryHandler.Config queryHandlerConfig = ctxt.readValue(jp, QueryHandler.Config.class);
                if (!queryHandlers.containsKey(queryHandlerConfig))
                    queryHandlers.put(queryHandlerConfig, new QueryHandler(queryHandlerConfig));

                return queryHandlers.get(queryHandlerConfig);
            }
        }

        class HumanReadableDurationDeserializer extends StdDeserializer<Duration> {

            public HumanReadableDurationDeserializer() {
                this(null);
            }

            protected HumanReadableDurationDeserializer(Class<?> vc) {
                super(vc);
            }

            @Override
            public Duration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                var durationString = jp.getValueAsString()
                        .replaceAll("\\s+", "")
                        .replace("years", "y")
                        .replace("year", "y")
                        .replace("months", "m")
                        .replace("month", "m")
                        .replace("weeks", "w")
                        .replace("week", "w")
                        .replace("days", "d")
                        .replace("day", "d")
                        .replace("mins", "m")
                        .replace("min", "m")
                        .replace("hrs", "h")
                        .replace("hr", "h")
                        .replace("secs", "s")
                        .replace("sec", "s")
                        .replaceFirst("(\\d+d)", "P$1T");
                if ((durationString.charAt(0) != 'P')) durationString = "PT" + durationString;
                return Duration.parse(durationString);
            }
        }


        mapper = new ObjectMapper(factory).registerModule(new JavaTimeModule())
                .registerModule(new SimpleModule()
                        .addDeserializer(DatasetConfig.class, new DatasetDeserializer())
                        .addDeserializer(ConnectionConfig.class, new ConnectionDeserializer())
                        .addDeserializer(QueryHandler.class, new QueryHandlerDeserializer())
                        .addDeserializer(Duration.class, new HumanReadableDurationDeserializer()));
        // TODO: update validator and reactivate
//        if(validate && !validateConfig(config, schemaFile, mapper)){
//            return null;
//        }
        return new SuiteConfigWithID(input.hashCode(), mapper.readValue(input, Suite.Config.class));
    }

    /**
     * Preparses the datasets field in a IGUANA configuration file and adds a custom Deserializer to mapper to enable retrieving already parsed datasets by name.
     *
     * @param mapper The ObjectMapper instance used for parsing the configuration file.
     * @param input  The input String containing the configuration file content.
     * @return A Map of DatasetConfig objects, where the key is the dataset name and the value is the corresponding DatasetConfig object.
     * @throws JsonProcessingException If there is an error during JSON processing.
     */
    private static Map<String, DatasetConfig> preparseDataset(ObjectMapper mapper, String input) throws JsonProcessingException {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record PreparsingDatasets(@JsonProperty(required = true) List<DatasetConfig> datasets) {
        }
        final var preparsingDatasets = mapper.readValue(input, PreparsingDatasets.class);

        return preparsingDatasets.datasets().stream().collect(Collectors.toMap(DatasetConfig::name, Function.identity()));
    }

    private static Map<String, ConnectionConfig> preparseConnections(ObjectMapper mapper, String input) throws JsonProcessingException {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record PreparsingConnections(@JsonProperty(required = true) List<ConnectionConfig> connections) {
        }
        final var preparsingConnections = mapper.readValue(input, PreparsingConnections.class);

        return preparsingConnections.connections().stream().collect(Collectors.toMap(ConnectionConfig::name, Function.identity()));
    }

    private static boolean validateConfig(Path config, String schemaFile, ObjectMapper mapper) throws IOException {
        // TODO: update
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaFile);
        JsonSchema schema = factory.getSchema(is);
        JsonNode node = mapper.readTree(config.toFile());
        Set<ValidationMessage> errors = schema.validate(node);
        if (!errors.isEmpty()) {
            LOGGER.error("Found {} errors in configuration file.", errors.size());
        }
        for (ValidationMessage message : errors) {
            LOGGER.error(message.getMessage());
        }
        return errors.isEmpty();
    }

}
