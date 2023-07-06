package org.aksw.iguana.cc.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

/**
 * Creates an IguanaConfig from a given JSON or YAML file, and validates the config using a JSON schema file
 */
public class IguanaConfigFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(IguanaConfigFactory.class);

    private static final String schemaFile = "iguana-schema.json";

    public static IguanaConfig parse(Path config) throws IOException {
        return parse(config, true);
    }

    public static IguanaConfig parse(Path config, Boolean validate) throws IOException {
        Path fileName = config.getFileName();
        final var extension = FilenameUtils.getExtension(config.toString());
        switch (extension){
            case "yml", "yaml" -> {
                return parse(config, new YAMLFactory(), validate);
            }
            case "json" -> {
                return parse(config, new JsonFactory(), validate);
            }
            default -> throw new IllegalStateException("Unexpected suite file extension: " + extension);
        }
    }
    private static IguanaConfig parse(Path config, JsonFactory factory) throws IOException {
        return parse(config, factory, true);
    }

    private static IguanaConfig parse(Path config, JsonFactory factory, Boolean validate) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(factory);
        if(validate && !validateConfig(config, schemaFile, mapper)){
            return null;
        }
        return mapper.readValue(config.toFile(), IguanaConfig.class);
    }

    private static boolean validateConfig(Path config, String schemaFile, ObjectMapper mapper) throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(schemaFile);
        JsonSchema schema = factory.getSchema(is);
        JsonNode node = mapper.readTree(config.toFile());
        Set<ValidationMessage> errors = schema.validate(node);
        if(!errors.isEmpty()){
            LOGGER.error("Found {} errors in configuration file.", errors.size());
        }
        for(ValidationMessage message : errors){
            LOGGER.error(message.getMessage());
        }
        return errors.isEmpty();
    }

}
