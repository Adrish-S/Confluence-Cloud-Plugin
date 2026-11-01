package io.github.adrish_s.confluence;

import io.github.adrish_s.confluence.dsl.ConfluenceExtension;
import io.github.adrish_s.confluence.task.PublishToConfluenceTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;

public class ConfluencePublisherPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ConfluenceExtension extension = project.getExtensions().create("confluence", ConfluenceExtension.class);

        extension.getConfigFile().convention(project.provider(() -> {
            File yaml = project.file("confluence.yaml");
            if (yaml.exists()) return project.getLayout().getProjectDirectory().file("confluence.yaml");

            File yml = project.file("confluence.yml");
            if (yml.exists()) return project.getLayout().getProjectDirectory().file("confluence.yml");

            return project.getLayout().getProjectDirectory().file("confluence.json");
        }));

        extension.getSourceDir().convention(project.getLayout().getProjectDirectory().dir("docs"));
        extension.getDryRun().convention(false);

        project.getTasks().register("publishToConfluence", PublishToConfluenceTask.class, task -> {
            task.setGroup("publishing");
            task.setDescription("Publishes local Markdown files to Confluence Cloud REST API v2.");
            task.getConfigFile().convention(extension.getConfigFile());
            task.getSourceDir().convention(extension.getSourceDir());
            task.getDryRun().convention(extension.getDryRun());
        });
    }
}