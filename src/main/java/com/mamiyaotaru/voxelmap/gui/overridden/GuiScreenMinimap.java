package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiScreenMinimap extends Screen {
    protected GuiScreenMinimap() { this (Component.literal("")); }

    protected GuiScreenMinimap(Component title) {
        super (title);
    }

    public void removed() { MapSettingsManager.instance.saveAll(); }

    public void renderTooltip(GuiGraphics drawContext, Component text, int x, int y) {
        if (!(text != null && text.getString() != null && !text.getString().isEmpty())) return;
        drawContext.renderTooltip(VoxelConstants.getMinecraft().font, text, x, y);
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public List<? extends GuiEventListener> getButtonList() { return children(); }

    public Font getFontRenderer() { return font; }

    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    public void renderBackgroundTexture(GuiGraphics context) {
        context.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        context.blit(VoxelConstants.getOptionsBackgroundTexture(), 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
        context.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}