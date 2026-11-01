package io.github.adrish_s.confluence.scan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DirectoryScanner {

    public List<MarkdownDocument> scan(Path rootDir) throws IOException {
        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("Scan root directory does not exist: " + rootDir);
        }

        List<MarkdownDocument> documents = new ArrayList<>();

        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.toLowerCase().endsWith(".md")) {
                    String rawMarkdown = Files.readString(file);
                    String title = extractTitle(file, rawMarkdown);

                    Path relativeDir = file.getParent() == null ? Path.of("") : rootDir.relativize(file.getParent());
                    documents.add(new MarkdownDocument(file, relativeDir, title, rawMarkdown));
                }
                return FileVisitResult.CONTINUE;
            }
        });

        documents.sort(Comparator.comparingInt((MarkdownDocument doc) -> doc.getFilePath().getNameCount())
                .thenComparing(doc -> !isIndexFile(doc.getFilePath()))
                .thenComparing(MarkdownDocument::getTitle));

        return Collections.unmodifiableList(documents);
    }

    private boolean isIndexFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.equals("index.md") || name.equals("readme.md");
    }

    private String extractTitle(Path file, String markdown) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(markdown))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("# ") && line.length() > 2) {
                    return line.substring(2).trim();
                }
            }
        }
        String name = file.getFileName().toString();
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0) {
            name = name.substring(0, dotIdx);
        }
        return name.replace('-', ' ').replace('_', ' ').trim();
    }
}