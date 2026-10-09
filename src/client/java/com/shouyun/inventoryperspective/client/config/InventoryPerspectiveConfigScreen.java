package com.shouyun.inventoryperspective.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class InventoryPerspectiveConfigScreen {
    private InventoryPerspectiveConfigScreen() {}

    public static Screen create(Screen parent) {
        InventoryPerspectiveConfig config = InventoryPerspectiveConfig.get();
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Component.translatable("inventory-perspective.config.title"))
                .setSavingRunnable(InventoryPerspectiveConfig::save);
        var entries = builder.entryBuilder();
        var category = builder.getOrCreateCategory(Component.translatable("inventory-perspective.config.general"));
        category.addEntry(entries.startBooleanToggle(Component.translatable("inventory-perspective.config.enabled"), config.enabled)
                .setDefaultValue(true).setSaveConsumer(value -> config.enabled = value).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("inventory-perspective.config.right"), config.inventoryOnRight)
                .setDefaultValue(true).setTooltip(Component.translatable("inventory-perspective.config.right.tooltip"))
                .setSaveConsumer(value -> config.inventoryOnRight = value).build());
        category.addEntry(entries.startIntSlider(Component.translatable("inventory-perspective.config.duration"), config.transitionMillis, 0, 1000)
                .setDefaultValue(350).setTooltip(Component.translatable("inventory-perspective.config.duration.tooltip"))
                .setSaveConsumer(value -> config.transitionMillis = value).build());
        return builder.build();
    }
}
