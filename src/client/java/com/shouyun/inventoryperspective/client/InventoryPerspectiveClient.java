package com.shouyun.inventoryperspective.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfig;

public class InventoryPerspectiveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InventoryPerspectiveConfig.load();
		ClientTickEvents.START_CLIENT_TICK.register(InventoryCameraController::update);
	}
}
