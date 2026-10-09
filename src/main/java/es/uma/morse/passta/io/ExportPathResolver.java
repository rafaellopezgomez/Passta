package es.uma.morse.passta.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class ExportPathResolver {

    private static final String DEFAULT_FILE_NAME =
        "automaton";

    private ExportPathResolver() {
    }

    static Path resolve(
            Path target,
            String extension) {

        Objects.requireNonNull(
            target,
            "Target path is null"
        );

        String normalizedExtension =
            normalizeExtension(extension);

        Path normalizedTarget = target
            .toAbsolutePath()
            .normalize();

        if (Files.exists(normalizedTarget)
                && Files.isDirectory(normalizedTarget)) {

            return normalizedTarget.resolve(
                DEFAULT_FILE_NAME
                    + normalizedExtension
            );
        }

        Path fileNamePath =
            normalizedTarget.getFileName();

        String fileName =
            resolveFileName(fileNamePath);

        String baseName =
            stripExtension(fileName);

        Path parent =
            normalizedTarget.getParent();

        if (parent == null) {
            parent = Path.of(".")
                .toAbsolutePath()
                .normalize();
        }

        return parent.resolve(
            baseName + normalizedExtension
        );
    }

    static void createParentDirectories(
            Path outputFile) {

        Objects.requireNonNull(
            outputFile,
            "Output file is null"
        );

        Path parent =
            outputFile.getParent();

        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new RuntimeException(
                "Cannot create output directory: "
                    + parent,
                exception
            );
        }
    }

    private static String resolveFileName(
            Path fileNamePath) {

        if (fileNamePath == null) {
            return DEFAULT_FILE_NAME;
        }

        String fileName =
            fileNamePath.toString();

        if (fileName.isBlank()) {
            return DEFAULT_FILE_NAME;
        }

        return fileName;
    }

    private static String normalizeExtension(
            String extension) {

        Objects.requireNonNull(
            extension,
            "Extension is null"
        );

        if (extension.isBlank()) {
            throw new IllegalArgumentException(
                "Extension is blank"
            );
        }

        if (extension.startsWith(".")) {
            return extension.toLowerCase();
        }

        return "." + extension.toLowerCase();
    }

    private static String stripExtension(
            String fileName) {

        int dot =
            fileName.lastIndexOf('.');

        if (dot <= 0) {
            return fileName;
        }

        return fileName.substring(0, dot);
    }
}