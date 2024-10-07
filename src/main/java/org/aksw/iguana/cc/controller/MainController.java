package org.aksw.iguana.cc.controller;

import com.beust.jcommander.*;
import org.aksw.iguana.cc.suite.IguanaSuiteParser;
import org.aksw.iguana.cc.suite.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;


/**
 * The MainController class is responsible for executing the IGUANA program.
 */
public class MainController {


    public static class Args {
        public class PathConverter implements IStringConverter<Path> {
            @Override
            public Path convert(String value) {
                return Path.of(value);
            }
        }


        @Parameter(names = {"--ignore-schema", "-is"}, description = "Do not check the schema before parsing the suite file.")
        private boolean ignoreShema = false;

        @Parameter(names = {"--dry-run", "-d"}, hidden = true)
        public static boolean dryRun = false;

        @Parameter(names = "--help", help = true)
        private boolean help;

        @Parameter(description = "suite file {yml,yaml,json}", arity = 1, required = true, converter = PathConverter.class)
        private Path suitePath;

        @Parameter(names = {"--version", "-v"}, description = "Outputs the version number of the program and result ontology.")
        private boolean version;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    /**
     * The main method for executing IGUANA
     *
     * @param argc The command line arguments that are passed to the program.
     */
    public static void main(String[] argc) throws IOException {
        // Configurator.reconfigure(URI.create("log4j2.yml"));

        var args = new Args();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .build();
        try {
            jc.parse(argc);
        } catch (ParameterException e) {
            // The exception is also thrown when no suite file is provided. In the case where only the version option
            // is provided, this would still throw. Therefore, we need to check if the version option is provided.
            if (args.version) {
                outputVersion();
                System.exit(0);
            }

            System.err.println(e.getLocalizedMessage());
            jc.usage();
            System.exit(0);
        }
        if (args.help) {
            jc.usage();
            System.exit(1);
        }
        if (args.version) {
            outputVersion();
            System.exit(0);
        }

        try {
            Suite parse = IguanaSuiteParser.parse(args.suitePath, !args.ignoreShema);
            parse.run();
        } catch (IOException e) {
            LOGGER.error("Error while reading the configuration file.", e);
            System.exit(0);
        }
        System.exit(0);
    }

    private static void outputVersion() throws IOException {
        ClassLoader classloader = MainController.class.getClassLoader();
        String properties = new String(Objects.requireNonNull(classloader.getResourceAsStream("version.properties")).readAllBytes());
        String[] lines = properties.split("\\n");
        String projectVersion = lines[0].split("=")[1];
        String ontologyVersion = lines[1].split("=")[1];
        System.out.println("IGUANA version: " + projectVersion);
        System.out.println("Result ontology version: " + ontologyVersion);
    }

}
