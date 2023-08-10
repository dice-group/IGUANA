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


    private static final Logger LOGGER = LoggerFactory
            .getLogger(MainController.class);

    /**
     * The main method for executing IGUANA
     *
     * @param argc The command line arguments that are passed to the program.
     * @throws IOException If an I/O exception occurs.
     */
    public static void main(String[] argc) throws IOException {
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
        // TODO: a bit of error handling
        Suite parse = IguanaSuiteParser.parse(args.suitePath);
        parse.run();
        System.exit(0);
    }

}
