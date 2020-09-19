package org.aksw.iguana.cc.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Creates an IguanaConfig from a given JSON or YAML file, and validates the config using a JSON schema file
 */
public class IguanaConfigFactory {

    private static Logger LOGGER = LoggerFactory.getLogger(IguanaConfigFactory.class);

    private static String schemaFile = "iguana-schema.json";

    public static IguanaConfig parse(File config) throws IOException {
        return parse(config, true);
    }

    public static IguanaConfig parse(File config, Boolean validate) throws IOException {
        if(config.getName().endsWith(".yml") || config.getName().endsWith(".yaml")){
            return parse(config, new YAMLFactory(), validate);
        }
        else if(config.getName().endsWith(".json")){
            return parse(config, new JsonFactory(), validate);
        }
        return  null;
    }
    private static IguanaConfig parse(File config, JsonFactory factory) throws IOException {
        return parse(config, factory, true);
    }

    private static IguanaConfig parse(File config, JsonFactory factory, Boolean validate) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(factory);
        if(validate && !validateConfig(config, schemaFile, mapper)){
            return null;
        }
        return mapper.readValue(config, IguanaConfig.class);
    }

    private static boolean validateConfig(File configuration, String schemaFile, ObjectMapper mapper) throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(schemaFile);
        JsonSchema schema = factory.getSchema(is);
        JsonNode node = mapper.readTree(configuration);
        Set<ValidationMessage> errors = schema.validate(node);
        if(errors.size()>0){
            LOGGER.error("Found {} errors in configuration file.", errors.size());
        }
        for(ValidationMessage message : errors){
            LOGGER.error(message.getMessage());
        }
        return errors.size()==0;
    }

}
