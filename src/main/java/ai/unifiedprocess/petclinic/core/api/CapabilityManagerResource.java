package ai.unifiedprocess.petclinic.core.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/capabilities")
public class CapabilityManagerResource {

    private static final Pattern SCENARIO_LINE = Pattern.compile("^\\s*Scenario(?: Outline)?\\s*:\\s*(.+?)\\s*$");
    private static final Path LOCAL_REPOSITORY_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final List<SupportedRepository> SUPPORTED_REPOSITORIES = List.of(
            new SupportedRepository("use-case-bdd-workshop",
                    "https://github.com/skyglass/use-case-bdd-workshop"));

    @GetMapping("/tree")
    public CapabilityTreeResponse tree() {
        List<RepositoryTree> repositoryTrees = SUPPORTED_REPOSITORIES.stream()
                .map(this::repositoryTree)
                .toList();
        List<UseCaseDetails> useCases = new ArrayList<>();
        List<DocNode> capabilityNodes = new ArrayList<>();
        List<RepositorySummary> repositories = new ArrayList<>();

        for (RepositoryTree repositoryTree : repositoryTrees) {
            collectUseCases(repositoryTree.root(), useCases);
            capabilityNodes.addAll(repositoryTree.root().children());
            repositories.add(repositoryTree.repository());
        }

        capabilityNodes.sort(Comparator.comparing(DocNode::name)
                .thenComparing(DocNode::repositoryName));
        DocNode root = new DocNode("root:/", "Use Case Map", "root", "",
                "", "", capabilityNodes, null);
        return new CapabilityTreeResponse(root, useCases, repositories);
    }

    private RepositoryTree repositoryTree(SupportedRepository repository) {
        RepositoryCheckout checkout = repositoryCheckout(repository);
        Path capabilitiesRoot = checkout.root().resolve("docs").resolve("capabilities").normalize();
        ensureCapabilitiesRootExists(repository, capabilitiesRoot);
        DocNode root = buildNode(capabilitiesRoot, repository, capabilitiesRoot);
        RepositorySummary summary = new RepositorySummary(
                repository.name(),
                repository.url(),
                checkout.root().toString(),
                checkout.source());
        return new RepositoryTree(summary, root);
    }

    private RepositoryCheckout repositoryCheckout(SupportedRepository repository) {
        Path checkoutPath = Path.of(System.getProperty("java.io.tmpdir"), repository.name())
                .toAbsolutePath()
                .normalize();
        try {
            if (!Files.isDirectory(checkoutPath.resolve("docs").resolve("capabilities"))) {
                cloneRepository(repository, checkoutPath);
            }
            return new RepositoryCheckout(checkoutPath, "github");
        } catch (ResponseStatusException exception) {
            Path localCapabilitiesRoot = LOCAL_REPOSITORY_ROOT.resolve("docs").resolve("capabilities");
            if (Files.isDirectory(localCapabilitiesRoot)) {
                return new RepositoryCheckout(LOCAL_REPOSITORY_ROOT, "local fallback");
            }
            throw exception;
        }
    }

    private void cloneRepository(SupportedRepository repository, Path checkoutPath) {
        try {
            Files.createDirectories(checkoutPath.getParent());
            if (Files.exists(checkoutPath) && !isEmptyDirectory(checkoutPath)) {
                throw internalError("Temporary checkout exists but does not contain docs/capabilities: "
                        + checkoutPath, null);
            }
            Process process = new ProcessBuilder(
                    "git", "clone", "--depth", "1", repository.url(), checkoutPath.toString())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(90, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw internalError("Timed out cloning " + repository.url(), null);
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw internalError("Unable to clone " + repository.url() + ": " + trimProcessOutput(output), null);
            }
        } catch (IOException exception) {
            throw internalError("Unable to clone " + repository.url(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw internalError("Interrupted while cloning " + repository.url(), exception);
        }
    }

    private boolean isEmptyDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }

    private DocNode buildNode(Path directory, SupportedRepository repository, Path capabilitiesRoot) {
        String relativePath = relativePath(directory, capabilitiesRoot);
        String name = directory.equals(capabilitiesRoot) ? "docs/capabilities" : directory.getFileName().toString();
        String type = nodeType(directory, capabilitiesRoot);
        UseCaseDetails useCase = hasUseCaseFeature(directory)
                ? readUseCase(directory, repository, capabilitiesRoot)
                : null;
        List<DocNode> children = childDirectories(directory).stream()
                .map(child -> buildNode(child, repository, capabilitiesRoot))
                .toList();
        return new DocNode(nodeId(repository, type, relativePath), name, type, relativePath,
                repository.name(), repository.url(), children, useCase);
    }

    private List<Path> childDirectories(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            throw internalError("Unable to list " + directory, exception);
        }
    }

    private String nodeType(Path directory, Path capabilitiesRoot) {
        if (directory.equals(capabilitiesRoot)) {
            return "root";
        }
        if (hasUseCaseFeature(directory)) {
            return "use-case";
        }
        Path relative = capabilitiesRoot.relativize(directory);
        int count = relative.getNameCount();
        if (count == 1) {
            return "capability";
        }
        if (isActivityDirectory(relative)) {
            return "activity";
        }
        return "folder";
    }

    private boolean isActivityDirectory(Path relative) {
        if (relative.getNameCount() < 2) {
            return false;
        }
        String lastSegment = relative.getFileName().toString();
        if ("activities".equals(lastSegment) || "use-cases".equals(lastSegment)) {
            return false;
        }
        boolean insideActivities = false;
        for (int index = 0; index < relative.getNameCount(); index++) {
            String segment = relative.getName(index).toString();
            if ("use-cases".equals(segment)) {
                return false;
            }
            if ("activities".equals(segment)) {
                insideActivities = true;
            }
        }
        return insideActivities;
    }

    private boolean hasUseCaseFeature(Path directory) {
        return Files.isRegularFile(directory.resolve("uc.feature"));
    }

    private UseCaseDetails readUseCase(Path useCasePath, SupportedRepository repository, Path capabilitiesRoot) {
        String featureText = readString(useCasePath.resolve("uc.feature"));
        ParsedFeature parsedFeature = parseFeature(featureText);
        String capabilityId = capabilityId(useCasePath, capabilitiesRoot);
        List<String> activityIds = activityIds(useCasePath, capabilitiesRoot);
        String useCaseId = useCasePath.getFileName().toString();
        String useCasePathText = useCasePathText(capabilityId, activityIds, useCaseId);
        return new UseCaseDetails(
                repository.name(),
                repository.url(),
                capabilityId,
                String.join("/", activityIds),
                activityIds,
                useCaseId,
                useCasePathText,
                relativePath(useCasePath, capabilitiesRoot),
                optionalReadString(useCasePath.resolve("uc.md")).orElse(""),
                featureText,
                parsedFeature.scenarios());
    }

    private void collectUseCases(DocNode node, List<UseCaseDetails> useCases) {
        if (node.useCase() != null) {
            useCases.add(node.useCase());
        }
        node.children().forEach(child -> collectUseCases(child, useCases));
    }

    private ParsedFeature parseFeature(String featureText) {
        String normalized = normalizeLineEndings(featureText);
        List<String> lines = normalized.isEmpty()
                ? List.of()
                : new ArrayList<>(List.of(normalized.split("\n", -1)));
        List<Integer> scenarioLines = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (SCENARIO_LINE.matcher(lines.get(index)).matches()) {
                scenarioLines.add(index);
            }
        }

        int firstBlockStart = scenarioLines.isEmpty()
                ? lines.size()
                : scenarioBlockStart(lines, scenarioLines.getFirst(), 0);
        String prefix = joinLines(lines.subList(0, firstBlockStart));
        List<ScenarioBlock> scenarios = new ArrayList<>();
        List<String> seenIds = new ArrayList<>();
        for (int scenarioIndex = 0; scenarioIndex < scenarioLines.size(); scenarioIndex++) {
            int scenarioLine = scenarioLines.get(scenarioIndex);
            int lowerBound = scenarioIndex == 0 ? 0 : scenarioLines.get(scenarioIndex - 1) + 1;
            int blockStart = scenarioBlockStart(lines, scenarioLine, lowerBound);
            int blockEnd = scenarioIndex + 1 >= scenarioLines.size()
                    ? lines.size()
                    : scenarioBlockStart(lines, scenarioLines.get(scenarioIndex + 1), scenarioLine + 1);
            String text = joinLines(lines.subList(blockStart, blockEnd)).stripTrailing();
            String name = scenarioName(lines.get(scenarioLine));
            String baseId = slugify(name);
            int count = 1;
            String id = baseId;
            while (seenIds.contains(id)) {
                count++;
                id = baseId + "-" + count;
            }
            seenIds.add(id);
            scenarios.add(new ScenarioBlock(id, name, text));
        }

        return new ParsedFeature(featureName(prefix), prefix, scenarios);
    }

    private int scenarioBlockStart(List<String> lines, int scenarioLine, int lowerBound) {
        int start = scenarioLine;
        while (start > lowerBound) {
            String previous = lines.get(start - 1).strip();
            if (previous.isBlank() || previous.startsWith("@") || previous.startsWith("#")) {
                start--;
            } else {
                break;
            }
        }
        return start;
    }

    private String capabilityId(Path useCasePath, Path capabilitiesRoot) {
        Path relative = capabilitiesRoot.relativize(useCasePath);
        return relative.getNameCount() > 0 ? relative.getName(0).toString() : "";
    }

    private List<String> activityIds(Path useCasePath, Path capabilitiesRoot) {
        Path relative = capabilitiesRoot.relativize(useCasePath);
        List<String> activities = new ArrayList<>();
        for (int index = 1; index < relative.getNameCount(); index++) {
            String segment = relative.getName(index).toString();
            if ("use-cases".equals(segment)) {
                break;
            }
            if (!"activities".equals(segment)) {
                activities.add(segment);
            }
        }
        return activities;
    }

    private String useCasePathText(String capabilityId, List<String> activityIds, String useCaseId) {
        List<String> path = new ArrayList<>();
        path.add(capabilityId);
        path.addAll(activityIds);
        path.add(useCaseId);
        return String.join(" -> ", path);
    }

    private String nodeId(SupportedRepository repository, String type, String relativePath) {
        return repository.name() + ":" + type + ":" + (relativePath.isBlank() ? "/" : relativePath);
    }

    private String relativePath(Path path, Path capabilitiesRoot) {
        if (path.equals(capabilitiesRoot)) {
            return "";
        }
        return capabilitiesRoot.relativize(path).toString().replace('\\', '/');
    }

    private String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw internalError("Unable to read " + path, exception);
        }
    }

    private Optional<String> optionalReadString(Path path) {
        return Files.isRegularFile(path) ? Optional.of(readString(path)) : Optional.empty();
    }

    private void ensureCapabilitiesRootExists(SupportedRepository repository, Path capabilitiesRoot) {
        if (!Files.isDirectory(capabilitiesRoot)) {
            throw internalError(repository.url() + " does not contain docs/capabilities", null);
        }
    }

    private String normalizeLineEndings(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String joinLines(List<String> lines) {
        return String.join("\n", lines);
    }

    private String scenarioName(String scenarioLine) {
        Matcher matcher = SCENARIO_LINE.matcher(scenarioLine);
        return matcher.matches() ? matcher.group(1) : scenarioLine.strip();
    }

    private String featureName(String prefix) {
        Matcher matcher = Pattern.compile("(?m)^\\s*Feature\\s*:\\s*(.+?)\\s*$").matcher(prefix);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? "scenario" : slug;
    }

    private String trimProcessOutput(String output) {
        String trimmed = output == null ? "" : output.strip();
        return trimmed.length() <= 800 ? trimmed : trimmed.substring(0, 800) + "...";
    }

    private ResponseStatusException internalError(String message, Throwable cause) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public record CapabilityTreeResponse(
            DocNode root,
            List<UseCaseDetails> useCases,
            List<RepositorySummary> repositories) {
    }

    public record RepositorySummary(
            String name,
            String url,
            String checkoutPath,
            String source) {
    }

    public record DocNode(
            String id,
            String name,
            String type,
            String relativePath,
            String repositoryName,
            String repositoryUrl,
            List<DocNode> children,
            UseCaseDetails useCase) {
    }

    public record UseCaseDetails(
            String repositoryName,
            String repositoryUrl,
            String capabilityId,
            String activityPath,
            List<String> activityIds,
            String useCaseId,
            String useCasePath,
            String relativePath,
            String ucMarkdown,
            String featureText,
            List<ScenarioBlock> scenarios) {
    }

    private record SupportedRepository(String name, String url) {
    }

    private record RepositoryCheckout(Path root, String source) {
    }

    private record RepositoryTree(RepositorySummary repository, DocNode root) {
    }

    private record ParsedFeature(String featureName, String prefix, List<ScenarioBlock> scenarios) {
    }

    public record ScenarioBlock(String id, String name, String text) {
    }
}
