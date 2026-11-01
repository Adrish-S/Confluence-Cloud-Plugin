package io.github.adrish_s.confluence.dsl;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public abstract class ConfluenceExtension {
    public abstract RegularFileProperty getConfigFile();
    public abstract DirectoryProperty getSourceDir();
    public abstract Property<Boolean> getDryRun();
}