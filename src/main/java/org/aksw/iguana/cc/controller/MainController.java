package org.aksw.iguana.cc.controller;

import com.beust.jcommander.*;
import org.aksw.iguana.cc.suite.IguanaSuiteParser;
import org.aksw.iguana.cc.suite.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;


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

        @Parameter(names = "--help", help = true)
        private boolean help;

        @Parameter(description = "suite file {yml,yaml,json}", arity = 1, required = true, converter = PathConverter.class)
        private Path suitePath;
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    /**
     * The main method for executing IGUANA
     *
     * @param argc The command line arguments that are passed to the program.
     */
    public static void main(String[] argc) {
        var args = new Args();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .build();
        try {
            jc.parse(argc);
        } catch (ParameterException e) {
            System.err.println(e.getLocalizedMessage());
            jc.usage();
            System.exit(0);
        }
        if (args.help) {
            jc.usage();
            System.exit(1);
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

}
