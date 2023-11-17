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
        super (VoxelConstants.getMinecraft(), width, height, bottom, itemHeight);
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
            vertexBuffer.vertex(this.getX(), this.getBottom(), 0.0).texture(this.getX() / f, (this.getBottom() + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(this.getRight(), this.getBottom(), 0.0).texture(this.getRight() / f, (this.getBottom() + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(this.getRight(), this.getY(), 0.0).texture(this.getRight() / f, (this.getY() + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();
            vertexBuffer.vertex(this.getX(), this.getY(), 0.0).texture(this.getX() / f, (this.getY() + (int) getScrollAmount()) / f).color(32, 32, 32, 255).next();

            tessellator.draw();
        }

        int leftEdge = this.getX() + width / 2 - getRowWidth() / 2 + 2;
        int topOfListYPos = this.getY() + 4 - (int) getScrollAmount();

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
            vertexBuffer.vertex(this.getX(), this.getY(), -100.0).texture(0.0F, this.getY() / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.getX() + width, this.getY(), -100.0).texture(width / 32.0F, this.getY() / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.getX() + width, 0.0, -100.0).texture(width / 32.0F, 0.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.getX(), 0.0, -100.0).texture(0.0F, 0.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.getX(), height, -100.0).texture(0.0F, height / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.getX() + width, height, -100.0).texture(width / 32.0F, height / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.getX() + width, this.getBottom(), -100.0).texture(width / 32.0F, this.getBottom() / 32.0F).color(64, 64, 64, 255).next();
            vertexBuffer.vertex(this.getX(), this.getBottom(), -100.0).texture(0.0F, this.getBottom() / 32.0F).color(64, 64, 64, 255).next();

            tessellator.draw();

            RenderSystem.depthFunc(OpenGL.GL11_GL_LEQUAL);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA, 0, 1);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);

            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            vertexBuffer.vertex(this.getX(), this.getY() + topBottomFadeHeight, 0.0).texture(0.0F, 1.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(this.getRight(), this.getY() + topBottomFadeHeight, 0.0).texture(1.0F, 1.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(this.getRight(), this.getY(), 0.0).texture(1.0F, 0.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.getX(), this.getY(), 0.0).texture(0.0F, 0.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.getX(), this.getBottom(), 0.0).texture(0.0F, 1.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.getRight(), this.getBottom(), 0.0).texture(1.0F, 1.0F).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(this.getRight(), this.getBottom() - topBottomFadeHeight, 0.0).texture(1.0F, 0.0F).color(0, 0, 0, 0).next();
            vertexBuffer.vertex(this.getX(), this.getBottom() - topBottomFadeHeight, 0.0).texture(0.0F, 0.0F).color(0, 0, 0, 0).next();

            tessellator.draw();
        }

        int maxScroll = getMaxScroll();

        if (maxScroll > 0) {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            int k1 = MathHelper.clamp((this.getBottom() - this.getY()) * (this.getBottom() - this.getY()) / getMaxPosition(), 32, this.getBottom() - this.getY() - 8);
            int l1 = (int) getScrollAmount() * (this.getBottom() - this.getY() - k1) / maxScroll + this.getY();

            if (l1 < this.getY()) l1 = this.getY();

            vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            vertexBuffer.vertex(scrollBarLeft, this.getBottom(), 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarRight, this.getBottom(), 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarRight, this.getY(), 0.0).color(0, 0, 0, 255).next();
            vertexBuffer.vertex(scrollBarLeft, this.getY(), 0.0).color(0, 0, 0, 255).next();
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

    protected int getScrollbarPositionX() { return slotWidth >= 220 ? width / 2 + 124 : this.getRight() - 6; }

    public void setLeftPos(int left) {
        this.setX(left);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.doubleclick = System.currentTimeMillis() - this.lastClicked < 250L;
        this.lastClicked = System.currentTimeMillis();
        return super.mouseClicked(mouseX, mouseY, button);
    }
}