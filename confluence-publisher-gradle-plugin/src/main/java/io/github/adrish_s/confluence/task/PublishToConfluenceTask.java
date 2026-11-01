package io.github.adrish_s.confluence.task;

import io.github.adrish_s.confluence.auth.AuthenticationStrategy;
import io.github.adrish_s.confluence.auth.BasicTokenAuth;
import io.github.adrish_s.confluence.auth.BearerTokenAuth;
import io.github.adrish_s.confluence.client.ConfluenceGateway;
import io.github.adrish_s.confluence.client.ConfluencePageRequest;
import io.github.adrish_s.confluence.client.HttpConfluenceGateway;
import io.github.adrish_s.confluence.config.ConfigLoader;
import io.github.adrish_s.confluence.config.ConfluenceConfig;
import io.github.adrish_s.confluence.scan.DirectoryScanner;
import io.github.adrish_s.confluence.scan.MarkdownDocument;
import io.github.adrish_s.confluence.transform.ContentTransformer;
import io.github.adrish_s.confluence.transform.FlexmarkContentTransformer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PublishToConfluenceTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getConfigFile();

    @InputDirectory
    public abstract DirectoryProperty getSourceDir();

    @Input
    @Optional
    public abstract Property<Boolean> getDryRun();

    @Option(option = "dry-run", description = "Simulate operations without calling Confluence Cloud.")
    public void setDryRunOption(boolean dryRun) {
        getDryRun().set(dryRun);
    }

    @TaskAction
    public void execute() throws Exception {
        File cfgFile = getConfigFile().get().getAsFile();
        ConfluenceConfig config = ConfigLoader.load(cfgFile);
        boolean isDryRun = getDryRun().getOrElse(false);

        AuthenticationStrategy authStrategy;
        if (config.getBearerToken() != null && !config.getBearerToken().isBlank()) {
            authStrategy = new BearerTokenAuth(config.getBearerToken());
        } else {
            authStrategy = new BasicTokenAuth(config.getEmail(), config.getApiToken());
        }

        ConfluenceGateway gateway = new HttpConfluenceGateway(config.getBaseUrl(), authStrategy);
        ContentTransformer transformer = new FlexmarkContentTransformer();
        DirectoryScanner scanner = new DirectoryScanner();

        Path scanPath = getSourceDir().get().getAsFile().toPath();
        List<MarkdownDocument> documents = scanner.scan(scanPath);

        getLogger().lifecycle("Confluence Publisher: Discovered {} documents in {}", documents.size(), scanPath);
        if (isDryRun) {
            getLogger().lifecycle("Confluence Publisher: DRY RUN ACTIVE - No network modifications will occur.");
        }

        Map<Path, String> dirToParentPageId = new HashMap<>();
        if (config.getRootParentId() != null && !config.getRootParentId().isBlank()) {
            dirToParentPageId.put(Path.of(""), config.getRootParentId());
        }

        for (MarkdownDocument doc : documents) {
            String title = doc.getTitle();
            Path relDir = doc.getRelativeDirectory();
            String parentId = dirToParentPageId.get(relDir);

            getLogger().lifecycle("Publishing [{}] (Space: {}, Parent ID: {})", title, config.getSpaceKey(), parentId);

            String xhtml = transformer.transform(doc.getRawMarkdown());

            if (!isDryRun) {
                String publishedPageId = gateway.upsertPage(new ConfluencePageRequest(
                        title,
                        config.getSpaceKey(),
                        parentId,
                        xhtml
                ));

                String fileName = doc.getFilePath().getFileName().toString().toLowerCase();
                if (fileName.equals("index.md") || fileName.equals("readme.md")) {
                    dirToParentPageId.put(relDir, publishedPageId);
                }
            }
        }

        getLogger().lifecycle("Confluence Publisher: Execution completed successfully.");
    }
}