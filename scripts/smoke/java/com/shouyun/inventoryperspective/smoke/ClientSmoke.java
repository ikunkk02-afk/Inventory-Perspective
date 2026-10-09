package com.shouyun.inventoryperspective.smoke;

import com.google.gson.GsonBuilder;
import com.shouyun.inventoryperspective.smoke.mixin.ContainerAccessor;
import com.shouyun.inventoryperspective.smoke.mixin.RecipeBookAccessor;
import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfig;
import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfigScreen;
import me.pieking1215.invmove.InvMove;
import me.pieking1215.invmove.InvMoveConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientSmoke implements ClientModInitializer {
    public static final Set<Integer> keys = new HashSet<>();
    public static boolean simulatingKeys;
    private final List<String> passed = new ArrayList<>();
    private final long started = System.nanoTime();
    private int tick;
    private boolean loading;
    private Vec3 movementStart;
    private Path output;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                if (output == null) {
                    output = client.gameDirectory.toPath().resolve("smoke-evidence");
                    Files.createDirectories(output);
                }
                if (System.nanoTime() - started > 240_000_000_000L) throw new AssertionError("Smoke test timed out");
                run(client);
            } catch (Throwable error) {
                try { Files.writeString(output.resolve("FAILED.txt"), passed + "\n" + error); } catch (Exception ignored) {}
                error.printStackTrace();
                client.stop();
            }
        });
    }

    private void run(Minecraft client) throws Exception {
        if (!loading && client.screen instanceof TitleScreen && client.getOverlay() == null) {
            loading = true;
            client.options.renderDistance().set(4);
            client.options.simulationDistance().set(5);
            client.options.tutorialStep = TutorialSteps.NONE;
            client.options.guiScale().set(3);
            client.options.pauseOnLostFocus = false;
            client.resizeDisplay();
            client.createWorldOpenFlows().createFreshLevel("inventory-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Inventory smoke", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42L, false, false),
                    registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(),
                    client.screen);
        }
        if (client.player == null || client.level == null || client.getOverlay() != null) return;
        if (tick == 0 && client.screen != null) return;
        tick++;
        client.getToasts().clear();
        client.gui.getChat().clearMessages(true);
        switch (tick) {
            case 20 -> {
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.player.setYRot(0);
                client.player.setXRot(0);
                // Seed the server inventory so clicks are validated by the integrated server.
                var server = client.getSingleplayerServer();
                var uuid = client.player.getUUID();
                server.execute(() -> {
                    server.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
                    var player = server.getPlayerList().getPlayer(uuid);
                    player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 8));
                    player.inventoryMenu.broadcastChanges();
                });
                simulatingKeys = true;
                keys.add(GLFW.GLFW_KEY_W);
                client.options.keyUp.setDown(true);
            }
            case 30 -> {
                movementStart = client.player.position();
                client.setScreen(new InventoryScreen(client.player));
                check(client.options.getCameraType() == CameraType.THIRD_PERSON_BACK, "opening switches to third person");
            }
            case 31 -> check(client.player.input.forwardImpulse > 0, "movement remains active on the first tick after opening");
            case 50 -> {
                check(client.screen instanceof InventoryScreen, "inventory stays open during movement");
                check(client.player.position().distanceTo(movementStart) > 0.5, "real InvMove movement while inventory is open");
                check(client.player.input.forwardImpulse > 0, "forward input continues after opening");
                keys.clear();
                var screen = inventory(client);
                check(((ContainerAccessor) screen).smokeLeft() > (screen.width - 176) / 2, "inventory panel is on the right");
                check(InventoryCameraController.progress() > 0.99, "opening transition completes");
                Vec3 eye = client.player.getEyePosition(client.getTimer().getGameTimeDeltaPartialTick(true));
                check(client.gameRenderer.getMainCamera().getPosition().x < eye.x, "camera on right shoulder puts player on the left");
                shot(client, "01-right-inventory.png");
            }
            case 55 -> clickSlot(client, 9, 0);
            case 58 -> {
                check(client.player.inventoryMenu.getCarried().getCount() == 8, "left-click picks up original slot stack");
                clickSlot(client, 10, 1);
            }
            case 61 -> {
                check(client.player.inventoryMenu.getSlot(10).getItem().getCount() == 1, "right-click places one item");
                clickSlot(client, 9, 0);
            }
            case 65 -> {
                check(client.player.inventoryMenu.getCarried().isEmpty(), "left-click returns remaining items");
                var screen = inventory(client);
                var button = screen.children().stream().filter(ImageButton.class::isInstance).map(ImageButton.class::cast).findFirst().orElseThrow();
                screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
                screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
            }
            case 70 -> {
                var screen = inventory(client);
                check(screen.getRecipeBookComponent().isVisible(), "recipe book button works after relocation");
                var search = ((RecipeBookAccessor) screen.getRecipeBookComponent()).smokeSearch();
                screen.mouseClicked(search.getX() + 4, search.getY() + 4, 0);
                screen.charTyped('w', 0);
                check(search.getValue().equals("w"), "recipe search accepts text");
                keys.add(GLFW.GLFW_KEY_W);
                movementStart = client.player.position();
                shot(client, "02-recipe-book.png");
            }
            case 80 -> {
                check(!InvMove.instance().allowMovementInScreen(client.screen), "InvMove disables movement while recipe search is focused");
                keys.clear();
                client.setScreen(null);
            }
            case 95 -> {
                check(client.options.getCameraType() == CameraType.FIRST_PERSON, "close restores first-person perspective");
                check(!InventoryCameraController.controlsCamera(), "close releases camera control");
                InventoryPerspectiveConfig.get().inventoryOnRight = false;
                client.setScreen(new InventoryScreen(client.player));
                inventory(client).getRecipeBookComponent().toggleVisibility();
            }
            case 110 -> {
                var screen = inventory(client);
                check(((ContainerAccessor) screen).smokeLeft() < (screen.width - 176) / 2, "left-side option relocates panel");
                check(client.gameRenderer.getMainCamera().getPosition().x > client.player.getX(), "left-side option mirrors camera shoulder");
                shot(client, "03-left-inventory.png");
                client.setScreen(null);
            }
            case 125 -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                client.setScreen(new InventoryScreen(client.player));
            }
            case 140 -> client.setScreen(null);
            case 155 -> {
                check(client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT, "close restores front third-person perspective");
                InventoryPerspectiveConfig.get().inventoryOnRight = true;
                client.setScreen(new InventoryScreen(client.player));
            }
            case 157 -> client.setScreen(null);
            case 159 -> client.setScreen(new InventoryScreen(client.player));
            case 161 -> client.setScreen(null);
            case 180 -> {
                check(client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT, "rapid reopening preserves original perspective");
                client.setScreen(new InventoryScreen(client.player));
            }
            case 195 -> {
                if (FabricLoader.getInstance().isModLoaded("modmenu")) {
                    check(com.terraformersmc.modmenu.ModMenu.hasConfigScreen("inventory-perspective"), "Mod Menu discovers settings entrypoint");
                    client.setScreen(com.terraformersmc.modmenu.ModMenu.getConfigScreen("inventory-perspective", null));
                } else {
                    client.setScreen(InventoryPerspectiveConfigScreen.create(null));
                }
                check(!InventoryCameraController.controlsCamera(), "switching screens releases camera immediately");
                check(client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT, "switching screens restores perspective");
            }
            case 200 -> shot(client, "04-cloth-config.png");
            case 205 -> {
                client.setScreen(null);
                InventoryPerspectiveConfig.get().enabled = false;
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.setScreen(new InventoryScreen(client.player));
            }
            case 215 -> {
                check(client.options.getCameraType() == CameraType.FIRST_PERSON, "disabled mod leaves perspective unchanged");
                var screen = inventory(client);
                check(((ContainerAccessor) screen).smokeLeft() == (screen.width - 176) / 2, "disabled mod uses vanilla panel position");
                client.setScreen(null);
                InventoryPerspectiveConfig.get().enabled = true;
                InventoryPerspectiveConfig.get().transitionMillis = 0;
                client.setScreen(new InventoryScreen(client.player));
                check(InventoryCameraController.progress() == 1, "zero duration opens instantly");
                client.setScreen(null);
                check(client.options.getCameraType() == CameraType.FIRST_PERSON, "zero duration restores instantly");
                InventoryPerspectiveConfig.get().transitionMillis = 350;
                InventoryPerspectiveConfig.save();
                InventoryPerspectiveConfig.load();
                check(InventoryPerspectiveConfig.get().transitionMillis == 350, "settings survive save and reload");
            }
            case 220 -> {
                keys.add(GLFW.GLFW_KEY_LEFT_SHIFT);
                client.options.keyShift.setDown(true);
            }
            case 216 -> {
                client.setScreen(new InventoryScreen(client.player));
                clickSlot(client, 9, 0);
            }
            case 217 -> {
                var screen = inventory(client);
                var position = (ContainerAccessor) screen;
                var first = screen.getMenu().getSlot(11);
                var second = screen.getMenu().getSlot(12);
                screen.mouseClicked(position.smokeLeft() + first.x + 8, position.smokeTop() + first.y + 8, 0);
                screen.mouseDragged(position.smokeLeft() + first.x + 8, position.smokeTop() + first.y + 8, 0, 1, 0);
                screen.mouseDragged(position.smokeLeft() + second.x + 8, position.smokeTop() + second.y + 8, 0, 18, 0);
                screen.mouseReleased(position.smokeLeft() + second.x + 8, position.smokeTop() + second.y + 8, 0);
            }
            case 219 -> {
                check(client.player.inventoryMenu.getSlot(11).getItem().getCount() == 3
                        && client.player.inventoryMenu.getSlot(12).getItem().getCount() == 3, "drag distribution works at relocated slots");
                clickSlot(client, 9, 0);
                client.setScreen(null);
            }
            case 225 -> {
                check(client.player.isShiftKeyDown(), "player is crouching before opening");
                client.setScreen(new InventoryScreen(client.player));
            }
            case 235 -> {
                check(client.player.input.shiftKeyDown, "default InvMove mode maintains crouch after opening");
                clickSlot(client, 9, 0);
            }
            case 238 -> {
                var screen = inventory(client);
                ((ContainerAccessor) screen).smokeHover(screen.getMenu().getSlot(10));
                screen.keyPressed(GLFW.GLFW_KEY_1, 0, 0);
            }
            case 240 -> {
                check(client.player.inventoryMenu.getSlot(9).getItem().isEmpty(), "shift-click quick transfer works at relocated slot");
                check(client.player.inventoryMenu.getSlot(36).getItem().getCount() == 1, "number key swaps hovered inventory slot with hotbar");
                keys.clear();
                client.setScreen(null);
            }
            case 255 -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                client.setScreen(new InventoryScreen(client.player));
            }
            case 270 -> client.setScreen(null);
            case 285 -> {
                check(client.options.getCameraType() == CameraType.THIRD_PERSON_BACK, "close restores back third-person perspective");
                GLFW.glfwSetWindowSize(client.getWindow().getWindow(), 240, 480);
                client.options.guiScale().set(1);
                client.resizeDisplay();
                client.setScreen(new InventoryScreen(client.player));
            }
            case 295 -> {
                var screen = inventory(client);
                check(((ContainerAccessor) screen).smokeLeft() == (screen.width - 176) / 2, "small GUI falls back to centered panel");
                client.setScreen(null);
                GLFW.glfwSetWindowSize(client.getWindow().getWindow(), 1280, 720);
                client.options.guiScale().set(3);
                client.resizeDisplay();
            }
            case 305 -> {
                var uuid = client.player.getUUID();
                var server = client.getSingleplayerServer();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    player.teleportTo(0.5, -60, 0.5);
                    for (int x = -4; x <= 3; x++) for (int y = -60; y <= -56; y++) {
                        player.serverLevel().setBlock(new BlockPos(x, y, -2), Blocks.STONE.defaultBlockState(), 3);
                    }
                });
            }
            case 325 -> {
                client.player.setYRot(0);
                client.player.setXRot(0);
                client.setScreen(new InventoryScreen(client.player));
            }
            case 340 -> {
                var camera = client.gameRenderer.getMainCamera();
                check(camera.getPosition().z > -1, "shoulder camera stays in front of nearby wall");
                var hit = client.level.clip(new ClipContext(client.player.getEyePosition(), camera.getPosition(),
                        ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, client.player));
                check(hit.getType() == HitResult.Type.MISS, "camera path does not cross wall geometry");
                shot(client, "05-camera-collision.png");
                client.setScreen(null);
                Path pack = client.gameDirectory.toPath().resolve("resourcepacks/inventory-smoke-pack");
                Path texture = pack.resolve("assets/minecraft/textures/gui/container/inventory.png");
                Files.createDirectories(texture.getParent());
                Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":34,\"description\":\"Inventory smoke texture override\"}}");
                try (var input = client.getResourceManager().getResourceOrThrow(ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png")).open()) {
                    Files.copy(input, texture, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                client.getResourcePackRepository().reload();
                var selected = new ArrayList<>(client.getResourcePackRepository().getSelectedIds());
                selected.add("file/inventory-smoke-pack");
                client.getResourcePackRepository().setSelected(selected);
                client.reloadResourcePacks();
            }
            case 355 -> {
                check(client.getResourceManager().getResourceOrThrow(ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png"))
                        .sourcePackId().contains("inventory-smoke-pack"), "vanilla inventory texture resolves from active resource pack");
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.setScreen(new InventoryScreen(client.player));
            }
            case 370 -> {
                shot(client, "06-resource-pack-inventory.png");
                client.player.closeContainer();
                var uuid = client.player.getUUID();
                var server = client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayer(uuid).setGameMode(GameType.CREATIVE));
            }
            case 390 -> client.setScreen(new InventoryScreen(client.player));
            case 395 -> {
                check(!(client.screen instanceof InventoryScreen), "creative inventory uses its own vanilla screen");
                check(!InventoryCameraController.controlsCamera(), "creative inventory does not take over camera");
                check(client.options.getCameraType() == CameraType.FIRST_PERSON, "creative inventory preserves perspective");
                simulatingKeys = false;
                Files.writeString(output.resolve("result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                        java.util.Map.of("status", "passed", "assertions", passed, "modMenu", FabricLoader.getInstance().isModLoaded("modmenu"))));
                client.stop();
            }
        }
    }

    private InventoryScreen inventory(Minecraft client) {
        if (!(client.screen instanceof InventoryScreen screen)) throw new AssertionError("Expected inventory at tick " + tick);
        return screen;
    }

    private void clickSlot(Minecraft client, int index, int button) {
        var screen = inventory(client);
        var slot = screen.getMenu().getSlot(index);
        var positions = (ContainerAccessor) screen;
        double x = positions.smokeLeft() + slot.x + 8;
        double y = positions.smokeTop() + slot.y + 8;
        screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);
    }

    private void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description + " at tick " + tick);
        passed.add(description);
        System.out.println("SMOKE PASS: " + description);
    }

    private void shot(Minecraft client, String name) throws Exception {
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            image.writeToFile(output.resolve(name));
        }
    }
}
