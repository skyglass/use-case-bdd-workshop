package ai.unifiedprocess.petclinic.core.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/capabilities")
public class CapabilityManagerResource {

    private static final Pattern EPIC_FILE = Pattern.compile("EP-(\\d+)\\.feature");
    private static final Pattern FEATURE_LINE = Pattern.compile("(?m)^\\s*Feature\\s*:\\s*.*$");
    private static final Pattern SCENARIO_LINE = Pattern.compile("^\\s*Scenario(?: Outline)?\\s*:\\s*(.+?)\\s*$");
    private static final Pattern GHERKIN_GIVEN = Pattern.compile("(?m)^\\s*Given\\b");
    private static final Pattern GHERKIN_WHEN = Pattern.compile("(?m)^\\s*When\\b");
    private static final Pattern GHERKIN_THEN = Pattern.compile("(?m)^\\s*Then\\b");
    private static final Path CAPABILITIES_ROOT = Path.of("docs", "capabilities").toAbsolutePath().normalize();

    @GetMapping("/tree")
    public CapabilityTreeResponse tree() {
        ensureCapabilitiesRootExists();
        DocNode root = buildNode(CAPABILITIES_ROOT);
        List<UseCaseDetails> useCases = new ArrayList<>();
        collectUseCases(root, useCases);
        return new CapabilityTreeResponse(root, useCases);
    }

    @PostMapping("/mock-pr/submit-epic")
    public MockPullRequestResponse submitEpic(@RequestBody EpicSubmissionRequest request) {
        Path useCasePath = existingUseCasePath(request.relativePath(), request.capabilityId(), request.activityId(),
                request.useCaseId());
        String useCaseId = useCaseId(useCasePath, request.useCaseId());
        String submittedFeature = normalizeFeatureText(required(request.submittedFeature(), "submittedFeature"), useCaseId);
        validateGherkinScenarios(submittedFeature);

        String currentFeature = readString(useCasePath.resolve("uc.feature"));
        String mergedFeature = mergeFeature(currentFeature, submittedFeature, useCaseId);
        String epicName = nextEpicName(useCasePath);
        String relativeUseCasePath = relativePath(useCasePath);

        return mockPr("Submit epic for " + useCaseId,
                List.of(
                        new ChangedFile(relativeUseCasePath + "/epics/" + epicName, submittedFeature),
                        new ChangedFile(relativeUseCasePath + "/uc.feature", mergedFeature)),
                Map.of(
                        "useCaseId", useCaseId,
                        "relativePath", relativeUseCasePath,
                        "epicName", epicName,
                        "operation", "submit-epic"));
    }

    @PostMapping("/mock-pr/delete-scenario")
    public MockPullRequestResponse deleteScenario(@RequestBody ScenarioDeletionRequest request) {
        Path useCasePath = existingUseCasePath(request.relativePath(), request.capabilityId(), request.activityId(),
                request.useCaseId());
        String useCaseId = useCaseId(useCasePath, request.useCaseId());
        String epicText = normalizeFeatureText(required(request.epicText(), "epicText"), useCaseId);
        validateGherkinScenarios(epicText);

        String currentFeature = readString(useCasePath.resolve("uc.feature"));
        ParsedFeature parsedFeature = parseFeature(currentFeature);
        ScenarioBlock scenario = parsedFeature.scenarios().stream()
                .filter(candidate -> candidate.id().equals(request.scenarioId())
                        || candidate.name().equals(request.scenarioName())
                        || candidate.text().equals(request.scenarioText()))
                .findFirst()
                .orElseThrow(() -> badRequest("Scenario was not found in " + useCaseId));

        String mergedFeature = removeScenario(parsedFeature, scenario.id());
        String epicName = nextEpicName(useCasePath);
        String relativeUseCasePath = relativePath(useCasePath);

        return mockPr("Delete scenario from " + useCaseId,
                List.of(
                        new ChangedFile(relativeUseCasePath + "/epics/" + epicName, epicText),
                        new ChangedFile(relativeUseCasePath + "/uc.feature", mergedFeature)),
                Map.of(
                        "useCaseId", useCaseId,
                        "relativePath", relativeUseCasePath,
                        "epicName", epicName,
                        "scenarioId", scenario.id(),
                        "scenarioName", scenario.name(),
                        "operation", "delete-scenario"));
    }

    @PostMapping("/mock-pr/create-use-case")
    public MockPullRequestResponse createUseCase(@RequestBody NewUseCaseRequest request) {
        String capabilityId = cleanSegment(required(request.capabilityId(), "capabilityId"), "capabilityId");
        String activityPath = cleanPath(required(request.activityPath(), "activityPath"), "activityPath");
        String useCaseId = cleanSegment(required(request.useCaseId(), "useCaseId"), "useCaseId");
        String featureText = normalizeFeatureText(required(request.featureText(), "featureText"), useCaseId);
        validateGherkinScenarios(featureText);

        String relativeUseCasePath = capabilityId + "/activities/" + activityPath + "/use-cases/" + useCaseId;
        Path useCasePath = CAPABILITIES_ROOT.resolve(relativeUseCasePath).normalize();
        if (!useCasePath.startsWith(CAPABILITIES_ROOT)) {
            throw badRequest("The requested use-case path is outside docs/capabilities");
        }

        return mockPr("Create use case " + useCaseId,
                List.of(
                        new ChangedFile(relativeUseCasePath + "/epics/EP-001.feature", featureText),
                        new ChangedFile(relativeUseCasePath + "/uc.feature", featureText)),
                Map.of(
                        "capabilityId", capabilityId,
                        "activityPath", activityPath,
                        "useCaseId", useCaseId,
                        "relativePath", relativeUseCasePath,
                        "epicName", "EP-001.feature",
                        "operation", "create-use-case"));
    }

    private DocNode buildNode(Path directory) {
        String relativePath = relativePath(directory);
        String name = directory.equals(CAPABILITIES_ROOT) ? "docs/capabilities" : directory.getFileName().toString();
        String type = nodeType(directory);
        UseCaseDetails useCase = hasUseCaseFeature(directory) ? readUseCase(directory) : null;
        List<DocNode> children = childDirectories(directory).stream()
                .map(this::buildNode)
                .toList();
        return new DocNode(nodeId(type, relativePath), name, type, relativePath, children, useCase);
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

    private String nodeType(Path directory) {
        if (directory.equals(CAPABILITIES_ROOT)) {
            return "root";
        }
        if (hasUseCaseFeature(directory)) {
            return "use-case";
        }
        Path relative = CAPABILITIES_ROOT.relativize(directory);
        int count = relative.getNameCount();
        if (count == 1) {
            return "capability";
        }
        if (count >= 3 && "activities".equals(relative.getName(1).toString()) && count == 3) {
            return "activity";
        }
        return "folder";
    }

    private boolean hasUseCaseFeature(Path directory) {
        return Files.isRegularFile(directory.resolve("uc.feature"));
    }

    private UseCaseDetails readUseCase(Path useCasePath) {
        String featureText = readString(useCasePath.resolve("uc.feature"));
        ParsedFeature parsedFeature = parseFeature(featureText);
        return new UseCaseDetails(
                capabilityId(useCasePath),
                activityId(useCasePath),
                useCasePath.getFileName().toString(),
                relativePath(useCasePath),
                optionalReadString(useCasePath.resolve("uc.md")).orElse(""),
                featureText,
                parsedFeature.scenarios(),
                nextEpicName(useCasePath));
    }

    private void collectUseCases(DocNode node, List<UseCaseDetails> useCases) {
        if (node.useCase() != null) {
            useCases.add(node.useCase());
        }
        node.children().forEach(child -> collectUseCases(child, useCases));
    }

    private Path existingUseCasePath(String relativePath, String capabilityId, String activityId, String useCaseId) {
        String path = relativePath == null || relativePath.isBlank()
                ? cleanSegment(required(capabilityId, "capabilityId"), "capabilityId") + "/activities/"
                + cleanSegment(required(activityId, "activityId"), "activityId") + "/use-cases/"
                + cleanSegment(required(useCaseId, "useCaseId"), "useCaseId")
                : cleanPath(relativePath, "relativePath");
        Path resolved = CAPABILITIES_ROOT.resolve(path).normalize();
        if (!resolved.startsWith(CAPABILITIES_ROOT)) {
            throw badRequest("The requested use-case path is outside docs/capabilities");
        }
        if (!Files.isRegularFile(resolved.resolve("uc.feature"))) {
            throw badRequest("No uc.feature exists at " + path);
        }
        return resolved;
    }

    private String mergeFeature(String currentFeature, String submittedFeature, String useCaseId) {
        ParsedFeature current = parseFeature(normalizeFeatureText(currentFeature, useCaseId));
        ParsedFeature submitted = parseFeature(normalizeFeatureText(submittedFeature, useCaseId));
        Map<String, ScenarioBlock> submittedByName = new LinkedHashMap<>();
        submitted.scenarios().forEach(scenario -> submittedByName.put(scenario.name(), scenario));

        List<ScenarioBlock> merged = new ArrayList<>();
        for (ScenarioBlock currentScenario : current.scenarios()) {
            ScenarioBlock replacement = submittedByName.remove(currentScenario.name());
            merged.add(replacement == null ? currentScenario : replacement);
        }
        merged.addAll(submittedByName.values());
        return renderFeature(current.prefix(), merged, useCaseId);
    }

    private String removeScenario(ParsedFeature parsedFeature, String scenarioId) {
        List<ScenarioBlock> remaining = parsedFeature.scenarios().stream()
                .filter(scenario -> !scenario.id().equals(scenarioId))
                .toList();
        return renderFeature(parsedFeature.prefix(), remaining, parsedFeature.featureName());
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
        Map<String, Integer> seenIds = new LinkedHashMap<>();
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
            int count = seenIds.merge(baseId, 1, Integer::sum);
            String id = count == 1 ? baseId : baseId + "-" + count;
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

    private String renderFeature(String prefix, List<ScenarioBlock> scenarios, String useCaseId) {
        String normalizedPrefix = normalizeFeatureText(prefix, useCaseId);
        StringBuilder builder = new StringBuilder(normalizedPrefix.stripTrailing());
        if (!scenarios.isEmpty()) {
            builder.append("\n\n");
        }
        for (int index = 0; index < scenarios.size(); index++) {
            if (index > 0) {
                builder.append("\n\n");
            }
            builder.append(scenarios.get(index).text().stripTrailing());
        }
        builder.append("\n");
        return builder.toString();
    }

    private String normalizeFeatureText(String text, String useCaseId) {
        String normalized = normalizeLineEndings(text).stripTrailing();
        Matcher featureMatcher = FEATURE_LINE.matcher(normalized);
        if (featureMatcher.find()) {
            return featureMatcher.replaceFirst("Feature: " + useCaseId);
        }
        return "Feature: " + useCaseId + (normalized.isBlank() ? "" : "\n\n" + normalized);
    }

    private void validateGherkinScenarios(String featureText) {
        List<ScenarioBlock> scenarios = parseFeature(featureText).scenarios();
        if (scenarios.isEmpty()) {
            throw badRequest("At least one Scenario is required");
        }
        for (ScenarioBlock scenario : scenarios) {
            if (!GHERKIN_GIVEN.matcher(scenario.text()).find()
                    || !GHERKIN_WHEN.matcher(scenario.text()).find()
                    || !GHERKIN_THEN.matcher(scenario.text()).find()) {
                throw badRequest("Scenario \"" + scenario.name() + "\" must include Given, When, and Then steps");
            }
        }
    }

    private String nextEpicName(Path useCasePath) {
        Path epicsPath = useCasePath.resolve("epics");
        if (!Files.isDirectory(epicsPath)) {
            return "EP-001.feature";
        }
        try (Stream<Path> stream = Files.list(epicsPath)) {
            int max = stream.filter(Files::isRegularFile)
                    .map(path -> EPIC_FILE.matcher(path.getFileName().toString()))
                    .filter(Matcher::matches)
                    .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                    .max()
                    .orElse(0);
            return "EP-%03d.feature".formatted(max + 1);
        } catch (IOException exception) {
            throw internalError("Unable to list epics for " + useCasePath, exception);
        }
    }

    private MockPullRequestResponse mockPr(String title, List<ChangedFile> changedFiles, Map<String, String> submitted) {
        return new MockPullRequestResponse(
                "MOCK-PR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                title,
                "mock/" + slugify(title),
                "mocked",
                Instant.now().toString(),
                submitted,
                changedFiles);
    }

    private String capabilityId(Path useCasePath) {
        Path relative = CAPABILITIES_ROOT.relativize(useCasePath);
        return relative.getNameCount() > 0 ? relative.getName(0).toString() : "";
    }

    private String activityId(Path useCasePath) {
        Path relative = CAPABILITIES_ROOT.relativize(useCasePath);
        for (int index = 0; index < relative.getNameCount() - 1; index++) {
            if ("activities".equals(relative.getName(index).toString()) && index + 1 < relative.getNameCount()) {
                return relative.getName(index + 1).toString();
            }
        }
        return "";
    }

    private String useCaseId(Path useCasePath, String providedUseCaseId) {
        return providedUseCaseId == null || providedUseCaseId.isBlank()
                ? useCasePath.getFileName().toString()
                : providedUseCaseId;
    }

    private String nodeId(String type, String relativePath) {
        return type + ":" + (relativePath.isBlank() ? "/" : relativePath);
    }

    private String relativePath(Path path) {
        if (path.equals(CAPABILITIES_ROOT)) {
            return "";
        }
        return CAPABILITIES_ROOT.relativize(path).toString().replace('\\', '/');
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

    private void ensureCapabilitiesRootExists() {
        if (!Files.isDirectory(CAPABILITIES_ROOT)) {
            throw internalError("docs/capabilities was not found", null);
        }
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw badRequest(fieldName + " is required");
        }
        return value;
    }

    private String cleanSegment(String value, String fieldName) {
        if (!value.matches("[a-z0-9][a-z0-9-]*")) {
            throw badRequest(fieldName + " must be dash-case");
        }
        return value;
    }

    private String cleanPath(String value, String fieldName) {
        String normalized = normalizeLineEndings(value).strip();
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("..")) {
            throw badRequest(fieldName + " is not a valid relative path");
        }
        for (String segment : normalized.split("/")) {
            cleanSegment(segment, fieldName);
        }
        return normalized;
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

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException internalError(String message, Throwable cause) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public record CapabilityTreeResponse(DocNode root, List<UseCaseDetails> useCases) {
    }

    public record DocNode(
            String id,
            String name,
            String type,
            String relativePath,
            List<DocNode> children,
            UseCaseDetails useCase) {
    }

    public record UseCaseDetails(
            String capabilityId,
            String activityId,
            String useCaseId,
            String relativePath,
            String ucMarkdown,
            String featureText,
            List<ScenarioBlock> scenarios,
            String nextEpicName) {
    }

    private record ParsedFeature(String featureName, String prefix, List<ScenarioBlock> scenarios) {
    }

    public record ScenarioBlock(String id, String name, String text) {
    }

    public record EpicSubmissionRequest(
            String capabilityId,
            String activityId,
            String useCaseId,
            String relativePath,
            String submittedFeature) {
    }

    public record ScenarioDeletionRequest(
            String capabilityId,
            String activityId,
            String useCaseId,
            String relativePath,
            String scenarioId,
            String scenarioName,
            String scenarioText,
            String epicText) {
    }

    public record NewUseCaseRequest(
            String capabilityId,
            String activityPath,
            String useCaseId,
            String featureText) {
    }

    public record MockPullRequestResponse(
            String id,
            String title,
            String branch,
            String status,
            String createdAt,
            Map<String, String> submitted,
            List<ChangedFile> changedFiles) {
    }

    public record ChangedFile(String path, String content) {
    }
}
