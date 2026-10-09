package com.shouyun.inventoryperspective.client.screen;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.shouyun.inventoryperspective.client.camera.InventoryCameraController;
import com.shouyun.inventoryperspective.client.config.InventoryPerspectiveConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Draws the original screen onto a transparent texture, then places it in the world. */
public final class WorldInventoryRenderer {
    private static TextureTarget target;
    private static InventoryScreen lastScreen;
    private static PanelGeometry geometry;
    private static boolean renderingTexture;

    private WorldInventoryRenderer() {}
    public static boolean renderingTexture() { return renderingTexture; }
    public static PanelGeometry geometry() { return geometry; }

    public static void clear() {
        geometry = null;
        lastScreen = null;
        if (target != null) { target.destroyBuffers(); target = null; }
    }

    public static PanelGeometry.Point pick(double x, double y) {
        Minecraft client = Minecraft.getInstance();
        if (geometry == null || client.screen != lastScreen || client.level == null
                || client.screen.width != geometry.width() || client.screen.height != geometry.height()) return null;
        var point = geometry.intersect(x, y);
        if (point == null) return null;
        Vec3 hit = geometry.worldPoint(point.x(), point.y());
        HitResult block = client.level.clip(new ClipContext(geometry.camera(), hit,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, client.player));
        return block.getType() == HitResult.Type.MISS
                || block.getLocation().distanceTo(hit) < 0.01 ? point : null;
    }

    public static void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        boolean active = InventoryCameraController.isInventoryActive(client);
        if (active) lastScreen = (InventoryScreen) client.screen;
        if (!InventoryCameraController.controlsCamera() || lastScreen == null || client.player == null
                || (!active && client.screen != null) || client.getOverlay() != null) {
            geometry = null;
            lastScreen = null;
            return;
        }
        if (lastScreen.width != client.getWindow().getGuiScaledWidth()
                || lastScreen.height != client.getWindow().getGuiScaledHeight()) {
            lastScreen.resize(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        }
        double progress = InventoryCameraController.progress();
        if (progress < 0.001) { geometry = null; return; }
        float delta = context.tickCounter().getGameTimeDeltaPartialTick(true);
        var player = client.player;
        double yaw = Math.toRadians(player.getViewYRot(delta));
        double side = InventoryPerspectiveConfig.get().inventoryOnRight ? 1 : -1;
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        double scale = player.getScale();
        double fit = Math.min(1, (double) client.getWindow().getWidth() / client.getWindow().getHeight() / 1.6);
        Vec3 feet = new Vec3(Mth.lerp(delta, player.xo, player.getX()), Mth.lerp(delta, player.yo, player.getY()),
                Mth.lerp(delta, player.zo, player.getZ()));
        Vec3 center = feet.add(forward.scale(Mth.lerp(progress, 0.45, 0.8) * scale))
                .add(right.scale(side * Mth.lerp(progress, 0.25, 2.05 * fit) * scale))
                .add(0, player.getEyeHeight() - 0.05 * scale, 0);
        double panelYaw = yaw + Math.toRadians(side * Mth.lerp(progress, -100, -24));
        Vec3 planeRight = new Vec3(-Math.cos(panelYaw), 0, -Math.sin(panelYaw));
        double unit = 2.4 / 176 * scale * fit * Mth.lerp(progress, 0.12, 1);
        if (lastScreen.getRecipeBookComponent().isVisible()) unit *= 0.72;
        geometry = new PanelGeometry(center, planeRight, unit, lastScreen.width, lastScreen.height,
                context.camera().getPosition(), new Matrix4f(context.projectionMatrix()).mul(context.positionMatrix()));
        double mouseX = client.mouseHandler.xpos() * lastScreen.width / client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * lastScreen.height / client.getWindow().getScreenHeight();
        var mouse = active ? pick(mouseX, mouseY) : null;
        renderTexture(client, mouse, context.tickCounter().getRealtimeDeltaTicks());
        drawPlane((float) progress);
    }

    private static void renderTexture(Minecraft client, PanelGeometry.Point mouse, float delta) {
        var window = client.getWindow();
        if (target == null) {
            target = new TextureTarget(window.getWidth(), window.getHeight(), true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
        } else if (target.width != window.getWidth() || target.height != window.getHeight()) {
            target.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
        }
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            renderingTexture = true;
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            FogRenderer.setupNoFog();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, lastScreen.width,
                    lastScreen.height, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            modelView.identity().translate(0, 0, -11000);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
            lastScreen.renderWithTooltip(graphics, mouse == null ? -10000 : (int) mouse.x(),
                    mouse == null ? -10000 : (int) mouse.y(), delta);
            graphics.flush();
        } finally {
            renderingTexture = false;
            RenderSystem.disableScissor();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            client.getMainRenderTarget().bindWrite(true);
            client.getEntityRenderDispatcher().overrideCameraOrientation(client.gameRenderer.getMainCamera().rotation());
        }
    }

    private static void drawPlane(float alpha) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vertex(buffer, 0, geometry.height(), 0, 0, alpha);
        vertex(buffer, geometry.width(), geometry.height(), 1, 0, alpha);
        vertex(buffer, geometry.width(), 0, 1, 1, alpha);
        vertex(buffer, 0, 0, 0, 1, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void vertex(BufferBuilder buffer, double x, double y, float u, float v, float alpha) {
        Vec3 p = geometry.worldPoint(x, y).subtract(geometry.camera());
        buffer.addVertex((float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(alpha, alpha, alpha, alpha);
    }
}
