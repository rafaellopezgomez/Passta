package es.uma.morse.passta.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

import es.uma.morse.passta.core.automaton.SRTA;

public final class AutomatonExporter {

    private static final String DEFAULT_AUTOMATON_NAME = "automaton";

    private AutomatonExporter() {
    }
    
    public static void export(SRTA automaton, Path target, AutomatonExportFormat format) {
        Objects.requireNonNull(automaton, "Automaton is null");
        Objects.requireNonNull(target, "Target path is null");
        Objects.requireNonNull(format, "Export format is null");

        switch (format) {
            case PNG -> writeLayoutToFile(target, automaton, FileType.PNG);
            case SVG -> writeLayoutToFile(target, automaton, FileType.SVG);
            case UPPAAL -> exportToUppaal(target, automaton);
        }
    }

    public static void export(SRTA automaton, String target, AutomatonExportFormat format) {
        Objects.requireNonNull(target, "Target path is null");

        if (target.isBlank()) {
            throw new IllegalArgumentException("Target path is blank");
        }

        export(automaton, Path.of(target), format);
    }

    private static void writeLayoutToFile(Path target, SRTA automaton, FileType fileType) {
        Objects.requireNonNull(target, "Target path is null");
        Objects.requireNonNull(automaton, "Automaton is null");
        Objects.requireNonNull(fileType, "File type is null");

        Graphviz graphviz = AutomatonGraphvizRenderer.toGraphviz(automaton);

        Path normalizedTarget = target.toAbsolutePath().normalize();

        final Path dir;
        final String baseName;

        if (Files.exists(normalizedTarget) && Files.isDirectory(normalizedTarget)) {
            dir = normalizedTarget;
            baseName = DEFAULT_AUTOMATON_NAME;
        } else {
            dir = normalizedTarget.getParent() != null
                    ? normalizedTarget.getParent()
                    : Path.of(".");

            Path fileName = normalizedTarget.getFileName();

            String name = fileName == null || fileName.toString().isBlank()
                    ? DEFAULT_AUTOMATON_NAME
                    : fileName.toString();

            baseName = stripExtension(name);
        }

        try {
            Files.createDirectories(dir);
            graphviz.toFile(fileType).save(dir.toString(), baseName);
        } catch (IOException | ExecuteException e) {
            throw new RuntimeException(
                    "Cannot write layout to target: " + normalizedTarget
                            + ", dir: " + dir
                            + ", baseName: " + baseName
                            + ", type: " + fileType,
                    e
            );
        }
    }

    private static void exportToUppaal(Path target, SRTA automaton) {
        Objects.requireNonNull(target, "Target path is null");
        Objects.requireNonNull(automaton, "Automaton is null");

        Path normalizedTarget = target.toAbsolutePath().normalize();

        Path parent = normalizedTarget.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create directories for UPPAAL export: " + parent, e);
            }
        }

        new UPPAAL(normalizedTarget, automaton);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}