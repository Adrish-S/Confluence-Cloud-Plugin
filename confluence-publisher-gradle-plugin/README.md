# Confluence Cloud Markdown Publisher Gradle Plugin

A lightweight, modern Gradle plugin designed to publish Markdown documentation directory trees directly to **Confluence Cloud** using the official **REST API v2**.

---

## Features

- **Confluence Cloud REST API v2 Native:** Uses modern v2 endpoints with standard XHTML Confluence Storage Format (`representation: "storage"`).
- **Automatic Space Resolution:** Translates human-readable space keys (e.g., `DEV`, `DOCS`) into Confluence Cloud's required numeric `spaceId`.
- **Safe Optimistic Concurrency:** Checks existing pages and increments `version.number + 1` automatically during updates to prevent overwrites.
- **Directory Hierarchy Mapping:** Replicates your local folder structure as nested parent-child pages in Confluence.
- **Title Extraction:** Uses the first `# Heading 1` from your Markdown file as the page title, falling back to clean file names.
- **Secure Secret Handling:** Supports environment variable expansion (e.g. `${CONFLUENCE_API_TOKEN}`) inside configuration files.
- **Dry Run Support:** Preview scanned documents and parent hierarchy mappings without modifying remote spaces.

---

## Installation

Add the plugin to your `build.gradle`:

```groovy
plugins {
    id 'io.github.adrish_s.confluence-publisher' version '1.0.0'
}
```

Or in `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.adrish_s.confluence-publisher") version "1.0.0"
}
```

---

## Configuration

Create a `confluence.yaml` (or `confluence.json`) file in the root of your project:

```yaml
# Note: Cloud URLs must include the /wiki suffix
baseUrl: "https://your-domain.atlassian.net/wiki"
email: "your-email@example.com"
apiToken: "${CONFLUENCE_API_TOKEN}"
spaceKey: "DEV"

# Optional: Anchor the documentation tree under an existing Confluence page ID
rootParentId: "12345678"
```

### Authentication Options

1. **Basic Auth (Recommended for Cloud):**
   - Provide `email` and an Atlassian API Token via `apiToken`.
   - API tokens can be generated at [id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens).

2. **Bearer Token (OAuth / Internal API Gateways):**
   ```yaml
   baseUrl: "https://confluence.corp.internal/wiki"
   bearerToken: "${CONFLUENCE_BEARER_TOKEN}"
   spaceKey: "DOCS"
   ```

---

## Directory Conventions

By default, the plugin scans the `./docs` directory:

```text
docs/
├── index.md                  # Published as parent page for the section
├── architecture.md           # Sibling page
└── setup/
    ├── index.md              # Child page of root, parent of setup files
    └── installation.md       # Nested under 'setup'
```

---

## Usage

Set your API token in the environment and run the publish task:

```bash
export CONFLUENCE_API_TOKEN="your_atlassian_api_token"
./gradlew publishToConfluence
```

### Dry Run

Preview scanned files, extracted titles, and calculated parent-child hierarchies without sending remote requests:

```bash
./gradlew publishToConfluence --dry-run
```

### Custom Source Directory or Config Location

Configure custom paths in `build.gradle`:

```groovy
confluence {
    configFile = layout.projectDirectory.file("custom-confluence.yaml")
    sourceDir = layout.projectDirectory.dir("documentation/markdown")
}
```

---

## Requirements

- **Java:** Java 11 or higher
- **Gradle:** 7.0 or higher
- **Target Platform:** Confluence Cloud (REST API v2)

---

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.