package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public abstract class GuiSlotMinimap extends EntryListWidget {
    protected int slotWidth = 220;
    private boolean showTopBottomBG = true;
    private boolean showSlotBG = true;
    private boolean hasListHeader;
    protected long lastClicked;
    public boolean doubleclick;

    protected GuiSlotMinimap(int width, int height, int top, int bottom, int itemHeight) {
        super (VoxelConstants.getMinecraft(), width, height, top, bottom, itemHeight);
    }

    public void setShowTopBottomBG(boolean showTopBottomBG) { this.showTopBottomBG = showTopBottomBG; }

    public void setShowSlotBG(boolean showSlotBG) { this.showSlotBG = showSlotBG; }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        //FIXME 1.20.2 renderBackground(drawContext); ?? renderInGameBackground

        int scrollBarLeft = getScrollbarPositionX();
        int scrollBarRight = scrollBarLeft + 6;

        setScrollAmount(getScrollAmount());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();

        if (this.showSlotBG) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            float f = 32.0f;

            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(left, bottom, 0.0).texture(left / f, (bottom + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(right, bottom, 0.0).texture(right / f, (bottom + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(right, top, 0.0).texture(right / f, (top + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(left, top, 0.0).texture(left / f, (top + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();

            tessellator.draw();
        }

        int leftEdge = left + width / 2 - getRowWidth() / 2 + 2;
        int topOfListYPos = top + 4 - (int) getScrollAmount();

        if (this.hasListHeader) renderHeader(drawContext, leftEdge, topOfListYPos);

        renderList(drawContext, mouseX, mouseY, delta);
        RenderSystem.disableDepthTest();

        byte topBottomFadeHeight = 4;

        if (this.showTopBottomBG) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(OpenGL.GL11_GL_ALWAYS);

            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(left, top, -100.0).texture(0.0F, top / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(left + width, top, -100.0).texture(width / 32.0F, top / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(left + width, 0.0, -100.0).texture(width / 32.0F, 0.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(left, 0.0, -100.0).texture(0.0F, 0.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(left, height, -100.0).texture(0.0F, height / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(left + width, height, -100.0).texture(width / 32.0F, height / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(left + width, bottom, -100.0).texture(width / 32.0F, bottom / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(left, bottom, -100.0).texture(0.0F, bottom / 32.0F).color(64, 64, 64, 255).next();

            tessellator.draw();

            RenderSystem.depthFunc(OpenGL.GL11_GL_LEQUAL);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA, 0, 1);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);

            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(left, top + topBottomFadeHeight, 0.0).texture(0.0F, 1.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(right, top + topBottomFadeHeight, 0.0).texture(1.0F, 1.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(right, top, 0.0).texture(1.0F, 0.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(left, top, 0.0).texture(0.0F, 0.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(left, bottom, 0.0).texture(0.0F, 1.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(right, bottom, 0.0).texture(1.0F, 1.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(right, bottom - topBottomFadeHeight, 0.0).texture(1.0F, 0.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(left, bottom - topBottomFadeHeight, 0.0).texture(0.0F, 0.0F).color(0, 0, 0, 0).next();

            tessellator.draw();
        }

        int maxScroll = getMaxScroll();

        if (maxScroll > 0) {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            int k1 = MathHelper.clamp((bottom - top) * (bottom - top) / getMaxPosition(), 32, bottom - top - 8);
            int l1 = (int) getScrollAmount() * (bottom - top - k1) / maxScroll + top;

            if (l1 < top) l1 = top;

            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            vertexBuffer.vertex(scrollBarLeft, bottom, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarRight, bottom, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarRight, top, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarLeft, top, 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1 + k1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarRight, l1 + k1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarRight, l1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1, 0.0).color(128, 128, 128, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1 + k1 - 1, 0.0).color(192, 192, 192, 255).next();
            vertexBuffer.vertex(scrollBarRight - 1, l1 + k1 - 1, 0.0).color(192, 192, 192, 255).next();
            vertexBuffer.vertex(scrollBarRight - 1, l1, 0.0).color(192, 192, 192, 255).next();
            vertexBuffer.vertex(scrollBarLeft, l1, 0.0).color(192, 192, 192, 255).next();

            tessellator.draw();
        }

        renderDecorations(drawContext, mouseX, mouseY);

        RenderSystem.disableBlend();
    }

    public int getRowWidth() { return slotWidth; }

    public void setSlotWidth(int slotWidth) { this.slotWidth = slotWidth; }

    protected int getScrollbarPositionX() { return slotWidth >= 220 ? width / 2 + 124 : right - 6; }

    public void setLeftPos(int left) {
        this.left = left;
        this.right = left + width;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.doubleclick = System.currentTimeMillis() - this.lastClicked < 250L;
        this.lastClicked = System.currentTimeMillis();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void appendNarrations(NarrationMessageBuilder builder) {}
}