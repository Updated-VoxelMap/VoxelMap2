package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class WaypointContainer {
    private final List<Waypoint> wayPts = new ArrayList<>();
    private Waypoint highlightedWaypoint;
    public final MapSettingsManager options;

    public WaypointContainer(MapSettingsManager options) {
        this.options = options;
    }

    public void addWaypoint(Waypoint newWaypoint) {
        this.wayPts.add(newWaypoint);
    }

    public void removeWaypoint(Waypoint waypoint) {
        this.wayPts.remove(waypoint);
    }

    public void setHighlightedWaypoint(Waypoint highlightedWaypoint) {
        this.highlightedWaypoint = highlightedWaypoint;
    }

    private void sortWaypoints() {
        this.wayPts.sort(Collections.reverseOrder());
    }

    public void renderWaypoints(float partialTicks, MatrixStack matrixStack, boolean beacons, boolean signs, boolean withDepth, boolean withoutDepth) {
        this.sortWaypoints();
        Entity cameraEntity = VoxelConstants.getMinecraft().getCameraEntity();
        double renderPosX = GameVariableAccessShim.xCoordDouble();
        double renderPosY = GameVariableAccessShim.yCoordDouble();
        double renderPosZ = GameVariableAccessShim.zCoordDouble();
        RenderSystem.enableCull();
        if (this.options.showBeacons && beacons) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, 1);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

            for (Waypoint pt : this.wayPts) {
                if (pt.isActive() || pt == this.highlightedWaypoint) {
                    int x = pt.getX();
                    int z = pt.getZ();
                    WorldChunk chunk = VoxelConstants.getPlayer().getWorld().getChunk(x >> 4, z >> 4);
                    if (chunk != null && !chunk.isEmpty() && VoxelConstants.getPlayer().getWorld().isChunkLoaded(x >> 4, z >> 4)) {
                        double bottomOfWorld = VoxelConstants.getPlayer().getWorld().getBottomY() - renderPosY;
                        this.renderBeam(pt, x - renderPosX, bottomOfWorld, z - renderPosZ, 64.0F, matrix4f);
                    }
                }
            }

            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }

        if (this.options.showWaypoints && signs) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA, 1, GlConst.GL_ONE_MINUS_SRC_ALPHA);

            for (Waypoint pt : this.wayPts) {
                if (pt.isActive() || pt == this.highlightedWaypoint) {
                    int x = pt.getX();
                    int z = pt.getZ();
                    int y = pt.getY();
                    double distance = Math.sqrt(pt.getDistanceSqToEntity(cameraEntity));
                    if ((distance < this.options.maxWaypointDisplayDistance || this.options.maxWaypointDisplayDistance < 0 || pt == this.highlightedWaypoint) && !VoxelConstants.getMinecraft().options.hudHidden) {
                        boolean isPointedAt = this.isPointedAt(pt, distance, cameraEntity, partialTicks);
                        String label = pt.name;
                        this.renderLabel(matrixStack, pt, distance, isPointedAt, label, x - renderPosX, y - renderPosY - 0.5, z - renderPosZ, 64, withDepth, withoutDepth);
                    }
                }
            }

            if (this.highlightedWaypoint != null && !VoxelConstants.getMinecraft().options.hudHidden) {
                int x = this.highlightedWaypoint.getX();
                int z = this.highlightedWaypoint.getZ();
                int y = this.highlightedWaypoint.getY();
                double distance = Math.sqrt(this.highlightedWaypoint.getDistanceSqToEntity(cameraEntity));
                boolean isPointedAt = this.isPointedAt(this.highlightedWaypoint, distance, cameraEntity, partialTicks);
                this.renderLabel(matrixStack, this.highlightedWaypoint, distance, isPointedAt, "*&^TARget%$^", x - renderPosX, y - renderPosY - 0.5, z - renderPosZ, 64, withDepth, withoutDepth);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

    }

    private boolean isPointedAt(Waypoint waypoint, double distance, Entity cameraEntity, Float partialTicks) {
        Vec3d cameraPos = cameraEntity.getCameraPosVec(partialTicks);
        double degrees = 5.0 + Math.min(5.0 / distance, 5.0);
        double angle = degrees * 0.0174533;
        double size = Math.sin(angle) * distance;
        Vec3d cameraPosPlusDirection = cameraEntity.getRotationVec(partialTicks);
        Vec3d cameraPosPlusDirectionTimesDistance = cameraPos.add(cameraPosPlusDirection.x * distance, cameraPosPlusDirection.y * distance, cameraPosPlusDirection.z * distance);
        Box axisalignedbb = new Box((waypoint.getX() + 0.5F) - size, (waypoint.getY() + 1.5F) - size, (waypoint.getZ() + 0.5F) - size, (waypoint.getX() + 0.5F) + size, (waypoint.getY() + 1.5F) + size, (waypoint.getZ() + 0.5F) + size);
        Optional<Vec3d> raytraceresult = axisalignedbb.raycast(cameraPos, cameraPosPlusDirectionTimesDistance);
        if (axisalignedbb.contains(cameraPos)) {
            return distance >= 1.0;
        } else
            return raytraceresult.isPresent();
    }

    private void renderBeam(Waypoint par1EntityWaypoint, double baseX, double baseY, double baseZ, float par8, Matrix4f matrix4f) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        int height = VoxelConstants.getClientWorld().getHeight();
        float brightness = 0.06F;
        double topWidthFactor = 1.05;
        double bottomWidthFactor = 1.05;
        float r = par1EntityWaypoint.red;
        float b = par1EntityWaypoint.blue;
        float g = par1EntityWaypoint.green;

        for (int width = 0; width < 4; ++width) {
            vertexBuffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            double d6 = 0.1 + width * 0.2;
            d6 *= topWidthFactor;
            double d7 = 0.1 + width * 0.2;
            d7 *= bottomWidthFactor;

            for (int side = 0; side < 5; ++side) {
                float vertX2 = (float) (baseX + 0.5 - d6);
                float vertZ2 = (float) (baseZ + 0.5 - d6);
                if (side == 1 || side == 2) {
                    vertX2 = (float) (vertX2 + d6 * 2.0);
                }

                if (side == 2 || side == 3) {
                    vertZ2 = (float) (vertZ2 + d6 * 2.0);
                }

                float vertX1 = (float) (baseX + 0.5 - d7);
                float vertZ1 = (float) (baseZ + 0.5 - d7);
                if (side == 1 || side == 2) {
                    vertX1 = (float) (vertX1 + d7 * 2.0);
                }

                if (side == 2 || side == 3) {
                    vertZ1 = (float) (vertZ1 + d7 * 2.0);
                }

                vertexBuffer.vertex(matrix4f, vertX1, (float) baseY + 0.0F, vertZ1).color(r * brightness, g * brightness, b * brightness, 0.8F).next();
                vertexBuffer.vertex(matrix4f, vertX2, (float) baseY + height, vertZ2).color(r * brightness, g * brightness, b * brightness, 0.8F).next();
            }

            tessellator.draw();
        }

    }

    private void renderLabel(MatrixStack matrixStack, Waypoint pt, double distance, boolean isPointedAt, String name, double baseX, double baseY, double baseZ, int par9, boolean withDepth, boolean withoutDepth) {
        boolean target = name.equals("*&^TARget%$^");
        if (target) {
            if (pt.red == 2.0F && pt.green == 0.0F && pt.blue == 0.0F) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            } else {
                isPointedAt = false;
            }
        }

        name = name + " (" + (int) distance + "m)";
        double maxDistance = VoxelConstants.getMinecraft().options.getSimulationDistance().getValue() * 16.0 * 0.99;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float var14 = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F;
        matrixStack.push();
        matrixStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getPitch()));
        matrixStack.scale(-var14, -var14, -var14);
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        float fade = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        fade = Math.min(fade, !pt.enabled && !target ? 0.3F : 1.0F);
        float width = 10.0F;
        float r = target ? 1.0F : pt.red;
        float g = target ? 0.0F : pt.green;
        float b = target ? 0.0F : pt.blue;
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        Sprite icon = target ? textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png") : textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, textureAtlas.getGlId());
        if (withDepth) {
            RenderSystem.depthMask(distance < maxDistance);
            RenderSystem.enableDepthTest();
            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(matrix4f, -width, -width, 0.0F).texture(icon.getMinU(), icon.getMinV()).color(r, g, b, fade).next();
            vertexBuffer.vertex(matrix4f, -width, width, 0.0F).texture(icon.getMinU(), icon.getMaxV()).color(r, g, b, fade).next();
            vertexBuffer.vertex(matrix4f, width, width, 0.0F).texture(icon.getMaxU(), icon.getMaxV()).color(r, g, b, fade).next();
            vertexBuffer.vertex(matrix4f, width, -width, 0.0F).texture(icon.getMaxU(), icon.getMinV()).color(r, g, b, fade).next();
            tessellator.draw();
        }

        if (withoutDepth) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(matrix4f, -width, -width, 0.0F).texture(icon.getMinU(), icon.getMinV()).color(r, g, b, 0.3F * fade).next();
            vertexBuffer.vertex(matrix4f, -width, width, 0.0F).texture(icon.getMinU(), icon.getMaxV()).color(r, g, b, 0.3F * fade).next();
            vertexBuffer.vertex(matrix4f, width, width, 0.0F).texture(icon.getMaxU(), icon.getMaxV()).color(r, g, b, 0.3F * fade).next();
            vertexBuffer.vertex(matrix4f, width, -width, 0.0F).texture(icon.getMaxU(), icon.getMinV()).color(r, g, b, 0.3F * fade).next();
            tessellator.draw();
        }

        TextRenderer fontRenderer = VoxelConstants.getMinecraft().textRenderer;
        if (isPointedAt && fontRenderer != null) {
            byte elevateBy = -19;
            RenderSystem.enablePolygonOffset();
            int halfStringWidth = fontRenderer.getWidth(name) / 2;
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            if (withDepth) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(distance < maxDistance);
                RenderSystem.polygonOffset(1.0F, 7.0F);
                vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.6F * fade).next();
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.6F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (9 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.6F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.6F * fade).next();
                tessellator.draw();
                RenderSystem.polygonOffset(1.0F, 5.0F);
                vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                tessellator.draw();
            }

            if (withoutDepth) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.polygonOffset(1.0F, 11.0F);
                vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (9 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).color(pt.red, pt.green, pt.blue, 0.15F * fade).next();
                tessellator.draw();
                RenderSystem.polygonOffset(1.0F, 9.0F);
                vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
                tessellator.draw();
            }

            RenderSystem.disablePolygonOffset();
            RenderSystem.depthMask(false);
            VertexConsumerProvider.Immediate vertexConsumerProvider = VoxelConstants.getMinecraft().getBufferBuilders().getEntityVertexConsumers();
            if (withoutDepth) {
                int textColor = (int) (255.0F * fade) << 24 | 13421772;
                RenderSystem.disableDepthTest();
                fontRenderer.draw(Text.literal(name), (-fontRenderer.getWidth(name) / 2f), elevateBy, textColor, false, matrix4f, vertexConsumerProvider, TextLayerType.SEE_THROUGH, 0, 15728880);
                vertexConsumerProvider.draw();
            }

            RenderSystem.enableBlend();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.pop();
    }
}
