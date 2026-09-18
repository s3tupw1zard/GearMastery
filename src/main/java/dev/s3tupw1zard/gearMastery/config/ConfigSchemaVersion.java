package dev.s3tupw1zard.gearMastery.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

/** Shared strict parsing for installed configuration schema versions. */
final class ConfigSchemaVersion {
    static final int CURRENT_VERSION = 3;
    private static final Pattern EXPLICIT_NULL_VERSION = Pattern.compile("(?m)^config-version\\s*:\\s*(?:(?:null|~)\\s*)?(?:#.*)?$");

    private ConfigSchemaVersion() { }

    static int read(final String name, final File file, final YamlConfiguration configuration) throws IOException {
        if (!configuration.getKeys(false).contains("config-version")) {
            if (EXPLICIT_NULL_VERSION.matcher(Files.readString(file.toPath())).find()) throw new IllegalArgumentException("Invalid config-version in " + name + ": expected an integer.");
            return 1;
        }
        final Object value = configuration.get("config-version");
        if (!(value instanceof Integer version)) throw new IllegalArgumentException("Invalid config-version in " + name + ": expected an integer.");
        if (version < 1) throw new IllegalArgumentException("Invalid config-version " + version + " in " + name + ": supported versions start at 1.");
        if (version > CURRENT_VERSION) throw new IllegalArgumentException("Unsupported future config version " + version + " in " + name + "; this plugin supports up to version " + CURRENT_VERSION + ".");
        return version;
    }

    static void requireCurrent(final String name, final File file, final YamlConfiguration configuration) throws IOException {
        final int version = read(name, file, configuration);
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported config version " + version + " in " + name + "; live reload requires version " + CURRENT_VERSION + ".");
    }
}
