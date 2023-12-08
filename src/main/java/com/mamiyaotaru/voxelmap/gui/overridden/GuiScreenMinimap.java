package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.List;

public class GuiScreenMinimap extends Screen {
    protected GuiScreenMinimap() { this (Text.literal("")); }

    protected GuiScreenMinimap(Text title) {
        super (title);
    }

    public void drawMap(DrawContext drawContext) {
        if (VoxelConstants.getVoxelMapInstance().getMapOptions().showUnderMenus) return;

        VoxelConstants.getVoxelMapInstance().getMap().drawMinimap(drawContext);
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, false);
    }

    public void removed() { MapSettingsManager.instance.saveAll(); }

    public void renderTooltip(DrawContext drawContext, Text text, int x, int y) {
        if (!(text != null && text.getString() != null && !text.getString().isEmpty())) return;
        drawContext.drawTooltip(VoxelConstants.getMinecraft().textRenderer, text, x, y);
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public List<? extends Element> getButtonList() { return children(); }

    public TextRenderer getFontRenderer() { return textRenderer; }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}