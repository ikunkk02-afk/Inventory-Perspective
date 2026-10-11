package com.shouyun.inventoryperspective.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfig;
import com.shouyun.inventoryperspective.client.screen.WorldInventoryRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class InventoryPerspectiveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InventoryPerspectiveConfig.load();
		ClientTickEvents.START_CLIENT_TICK.register(InventoryCameraController::update);
		WorldRenderEvents.LAST.register(WorldInventoryRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null) WorldInventoryRenderer.clear();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WorldInventoryRenderer.clear());
	}
}
