package io.github.adrish_s.confluence.scan;

import java.nio.file.Path;

public class MarkdownDocument {
    private final Path filePath;
    private final Path relativeDirectory;
    private final String title;
    private final String rawMarkdown;

    public MarkdownDocument(Path filePath, Path relativeDirectory, String title, String rawMarkdown) {
        this.filePath = filePath;
        this.relativeDirectory = relativeDirectory;
        this.title = title;
        this.rawMarkdown = rawMarkdown;
    }

    public Path getFilePath() { return filePath; }
    public Path getRelativeDirectory() { return relativeDirectory; }
    public String getTitle() { return title; }
    public String getRawMarkdown() { return rawMarkdown; }
}