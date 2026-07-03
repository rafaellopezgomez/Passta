package es.uma.morse.passta.cli;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.io.AutomatonExportFormat;
import es.uma.morse.passta.io.AutomatonExporter;
import es.uma.morse.passta.io.AutomatonViewer;

public final class PasstaCli {

    private static final String APP_NAME = "passta";
    private static final String VERSION = "0.2";

    private PasstaCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        System.exit(exitCode);
    }

    static int run(String[] args) {
        CliOptions options;

        try {
            options = CliOptions.parse(args);
        } catch (CliException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            printUsage();
            return ExitCode.USAGE_ERROR;
        }

        if (options.help) {
            printUsage();
            return ExitCode.OK;
        }

        if (options.version) {
            System.out.println(APP_NAME + " " + VERSION);
            return ExitCode.OK;
        }

        try {
            validate(options);

            if (options.verbose) {
                printRunConfiguration(options);
            }

            Passta passta = new Passta(options.input, options.k);
            SRTA automaton = passta.getAutomaton();

            switch (options.command) {
                case VIEW -> {
                    AutomatonViewer.show(automaton);
                    System.out.println("Automaton opened in browser.");
                }

                case EXPORT -> {
                    AutomatonExporter.export(automaton, options.output, options.format);
                    System.out.println("Automaton exported to: "
                            + options.output.toAbsolutePath().normalize());
                }
            }

            return ExitCode.OK;

        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input:");
            System.err.println("  " + e.getMessage());

            if (options.verbose) {
                System.err.println();
                e.printStackTrace(System.err);
            }

            return ExitCode.USAGE_ERROR;

        } catch (Exception e) {
            System.err.println("Error while running PASSTA:");
            System.err.println("  " + rootMessage(e));

            if (options.verbose) {
                System.err.println();
                e.printStackTrace(System.err);
            }

            return ExitCode.RUNTIME_ERROR;
        }
    }

    private static void validate(CliOptions options) {
        if (options.input == null) {
            throw new CliException("Missing input traces file.");
        }

        if (options.k == null) {
            throw new CliException("Missing k parameter.");
        }

        if (options.k < 0) {
            throw new CliException("k must be greater than or equal to 0.");
        }

        if (options.command == Command.EXPORT) {
            if (options.output == null) {
                options.output = defaultOutputFor(options.format);
            }

            if (options.format == null) {
                options.format = inferFormatFromPath(options.output);
            }
        }
    }

    private static Path defaultOutputFor(AutomatonExportFormat format) {
        if (format == null) {
            return Path.of("automaton.svg");
        }

        return switch (format) {
            case SVG -> Path.of("automaton.svg");
            case PNG -> Path.of("automaton.png");
            case UPPAAL -> Path.of("automaton.xml");
        };
    }

    private static AutomatonExportFormat inferFormatFromPath(Path output) {
        String fileName = output.getFileName().toString().toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".svg")) {
            return AutomatonExportFormat.SVG;
        }

        if (fileName.endsWith(".png")) {
            return AutomatonExportFormat.PNG;
        }

        if (fileName.endsWith(".xml") || fileName.endsWith(".uppaal")) {
            return AutomatonExportFormat.UPPAAL;
        }

        throw new CliException(
                "Cannot infer export format from output file: " + output
                        + ". Use --format. Supported formats: " + supportedFormats()
        );
    }

    private static AutomatonExportFormat parseFormat(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);

        try {
            return AutomatonExportFormat.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new CliException(
                    "Unsupported export format: " + value
                            + ". Supported formats: " + supportedFormats()
            );
        }
    }

    private static String supportedFormats() {
        return Arrays.stream(AutomatonExportFormat.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    private static void printRunConfiguration(CliOptions options) {
        System.out.println("PASSTA configuration");
        System.out.println("--------------------");
        System.out.println("Command    : " + options.command);
        System.out.println("Input file : " + options.input.toAbsolutePath().normalize());
        System.out.println("k          : " + options.k);

        if (options.command == Command.EXPORT) {
            System.out.println("Output file: " + options.output.toAbsolutePath().normalize());
            System.out.println("Format     : " + options.format);
        }

        System.out.println();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }

        return message;
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  " + APP_NAME + " <traces.json> <k> [output]");
        System.err.println("  " + APP_NAME + " export <traces.json> <k> [output]");
        System.err.println("  " + APP_NAME + " view <traces.json> <k>");
        System.err.println();
        System.err.println("Named options:");
        System.err.println("  " + APP_NAME + " export --input <traces.json> --k <k> [--output <file>] [--format <format>]");
        System.err.println("  " + APP_NAME + " view --input <traces.json> --k <k>");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  -i, --input <file>       Input JSON traces file.");
        System.err.println("  -k, --k <value>          PASSTA k parameter.");
        System.err.println("  -o, --output <file>      Output file. Default: automaton.svg.");
        System.err.println("  -f, --format <format>    Export format: " + supportedFormats() + ".");
        System.err.println("  -v, --verbose            Show detailed execution information.");
        System.err.println("  -h, --help               Show this help message.");
        System.err.println("      --version            Show version.");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  " + APP_NAME + " data/traces.json 2");
        System.err.println("  " + APP_NAME + " data/traces.json 2 out/automaton.svg");
        System.err.println("  " + APP_NAME + " export data/traces.json 2 out/automaton.png");
        System.err.println("  " + APP_NAME + " view data/traces.json 2");
        System.err.println("  " + APP_NAME + " view -i data/traces.json -k 2");
    }

    private enum Command {
        EXPORT,
        VIEW
    }

    private static final class CliOptions {

        private Command command = Command.EXPORT;
        private Path input;
        private Integer k;
        private Path output;
        private AutomatonExportFormat format;
        private boolean verbose;
        private boolean help;
        private boolean version;

        private static CliOptions parse(String[] args) {
            CliOptions options = new CliOptions();

            if (args.length == 0) {
                options.help = true;
                return options;
            }

            if (isHelp(args[0])) {
                options.help = true;
                return options;
            }

            if ("--version".equals(args[0])) {
                options.version = true;
                return options;
            }

            int offset = parseCommand(args, options);

            if (offset >= args.length) {
                if (options.help || options.version) {
                    return options;
                }

                throw new CliException("Missing arguments.");
            }

            if (!args[offset].startsWith("-")) {
                parsePositional(args, offset, options);
            } else {
                parseNamed(args, offset, options);
            }

            return options;
        }

        private static int parseCommand(String[] args, CliOptions options) {
            String first = args[0].toLowerCase(Locale.ROOT);

            return switch (first) {
                case "export" -> {
                    options.command = Command.EXPORT;
                    yield 1;
                }
                case "view" -> {
                    options.command = Command.VIEW;
                    yield 1;
                }
                default -> 0;
            };
        }

        private static void parsePositional(String[] args, int offset, CliOptions options) {
            int remaining = args.length - offset;

            if (options.command == Command.VIEW) {
                if (remaining != 2) {
                    throw new CliException("Command 'view' expects exactly 2 arguments: <traces.json> <k>.");
                }

                options.input = Path.of(args[offset]);
                options.k = parseInt(args[offset + 1], "k");
                return;
            }

            if (options.command == Command.EXPORT) {
                if (remaining < 2 || remaining > 3) {
                    throw new CliException("Command 'export' expects 2 or 3 arguments: <traces.json> <k> [output].");
                }

                options.input = Path.of(args[offset]);
                options.k = parseInt(args[offset + 1], "k");

                if (remaining == 3) {
                    options.output = Path.of(args[offset + 2]);
                }
            }
        }

        private static void parseNamed(String[] args, int offset, CliOptions options) {
            for (int i = offset; i < args.length; i++) {
                String arg = args[i];

                switch (arg) {
                    case "-h", "--help" -> options.help = true;

                    case "--version" -> options.version = true;

                    case "-v", "--verbose" -> options.verbose = true;

                    case "-i", "--input" ->
                            options.input = Path.of(requireValue(args, ++i, arg));

                    case "-k", "--k" ->
                            options.k = parseInt(requireValue(args, ++i, arg), "k");

                    case "-o", "--output" ->
                            options.output = Path.of(requireValue(args, ++i, arg));

                    case "-f", "--format" ->
                            options.format = parseFormat(requireValue(args, ++i, arg));

                    default -> throw new CliException("Unknown option: " + arg);
                }
            }

            if (options.command == Command.VIEW && options.output != null) {
                throw new CliException("Command 'view' does not accept --output.");
            }

            if (options.command == Command.VIEW && options.format != null) {
                throw new CliException("Command 'view' does not accept --format.");
            }
        }

        private static boolean isHelp(String value) {
            return "-h".equals(value) || "--help".equals(value);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new CliException("Missing value for option " + option + ".");
            }

            String value = args[index];

            if (value.isBlank()) {
                throw new CliException("Blank value for option " + option + ".");
            }

            if (value.startsWith("-")) {
                throw new CliException("Missing value for option " + option + ".");
            }

            return value;
        }

        private static int parseInt(String value, String name) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new CliException(name + " must be an integer: " + value);
            }
        }
    }

    private static final class CliException extends RuntimeException {

        private CliException(String message) {
            super(message);
        }
    }

    private static final class ExitCode {

        private static final int OK = 0;
        private static final int RUNTIME_ERROR = 1;
        private static final int USAGE_ERROR = 2;

        private ExitCode() {
        }
    }
}