package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public final class CredentialProvider {
    private static final ArrayList<LocalCredentialSource> LOCAL_SOURCES = loadLocalSources();

    private CredentialProvider() {
    }

    public static String get(String envName, String propertyName) {
        String systemProperty = System.getProperty(propertyName);
        if (hasText(systemProperty)) {
            return systemProperty.trim();
        }

        for (LocalCredentialSource source : LOCAL_SOURCES) {
            String localValue = source.properties.getProperty(envName);
            if (hasText(localValue)) {
                return localValue.trim();
            }

            localValue = source.properties.getProperty(propertyName);
            if (hasText(localValue)) {
                return localValue.trim();
            }
        }

        String envValue = System.getenv(envName);
        if (hasText(envValue)) {
            return envValue.trim();
        }

        return "";
    }

    public static String describeSource(String envName, String propertyName) {
        if (hasText(System.getProperty(propertyName))) {
            return "system-property";
        }
        for (LocalCredentialSource source : LOCAL_SOURCES) {
            if (hasText(source.properties.getProperty(envName))
                    || hasText(source.properties.getProperty(propertyName))) {
                return source.path.toString();
            }
        }
        if (hasText(System.getenv(envName))) {
            return "environment";
        }
        return "missing";
    }

    public static String describeWorkingDirectory() {
        return Path.of("").toAbsolutePath().normalize().toString();
    }

    private static ArrayList<LocalCredentialSource> loadLocalSources() {
        ArrayList<LocalCredentialSource> sources = new ArrayList<>();
        Set<Path> candidatePaths = new LinkedHashSet<>();
        addCandidatePaths(candidatePaths, Path.of("").toAbsolutePath().normalize());
        addCandidatePaths(candidatePaths, classLocation());

        for (Path path : candidatePaths) {
            Properties properties = loadProperties(path);
            if (!properties.isEmpty()) {
                sources.add(new LocalCredentialSource(path, properties));
            }
        }
        return sources;
    }

    private static void addCandidatePaths(Set<Path> candidatePaths, Path startPath) {
        if (startPath == null) {
            return;
        }

        Path current = Files.isRegularFile(startPath) ? startPath.getParent() : startPath;
        while (current != null) {
            candidatePaths.add(current.resolve(Path.of("config", "local.properties")).normalize());
            candidatePaths.add(current.resolve("local.properties").normalize());
            current = current.getParent();
        }
    }

    private static Path classLocation() {
        try {
            return Path.of(CredentialProvider.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()).toAbsolutePath().normalize();
        } catch (Exception e) {
            return null;
        }
    }

    private static Properties loadProperties(Path path) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(path)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class LocalCredentialSource {
        private final Path path;
        private final Properties properties;

        private LocalCredentialSource(Path path, Properties properties) {
            this.path = path;
            this.properties = properties;
        }
    }
}
