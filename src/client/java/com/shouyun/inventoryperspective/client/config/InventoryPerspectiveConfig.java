package com.shouyun.inventoryperspective.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class InventoryPerspectiveConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("inventory-perspective");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("inventory-perspective.json");
    private static InventoryPerspectiveConfig instance = new InventoryPerspectiveConfig();

    public boolean enabled = true;
    public boolean inventoryOnRight = true;
    public int transitionMillis = 350;

    public static InventoryPerspectiveConfig get() {
        return instance;
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            InventoryPerspectiveConfig loaded = GSON.fromJson(reader, InventoryPerspectiveConfig.class);
            if (loaded != null) {
                loaded.transitionMillis = Math.clamp(loaded.transitionMillis, 0, 1000);
                instance = loaded;
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read {}; using defaults", FILE, e);
        }
    }

    public static void save() {
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(temporary, GSON.toJson(instance), StandardCharsets.UTF_8);
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("Could not save {}", FILE, e);
        }
    }
}
