package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.IPopupGuiScreen;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    final WaypointManager waypointManager;
    final ColorManager colorManager;
    private final IGuiWaypoints parentGui;
    private PopupGuiButton doneButton;
    private GuiSlotDimensions dimensionList;
    protected DimensionContainer selectedDimension;
    private Text tooltip;
    private TextFieldWidget waypointName;
    private TextFieldWidget waypointX;
    private TextFieldWidget waypointZ;
    private TextFieldWidget waypointY;
    private PopupGuiButton buttonEnabled;
    protected final Waypoint waypoint;
    private boolean choosingColor;
    private boolean choosingIcon;
    private final float red;
    private final float green;
    private final float blue;
    private final String suffix;
    private final boolean enabled;
    private final boolean editing;
    private final Identifier pickerResourceLocation = new Identifier("voxelmap", "images/colorpicker.png");
    private final Identifier blank = new Identifier("textures/misc/white.png");

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, Waypoint par2Waypoint, boolean editing) {
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.parentGui = par1GuiScreen;
        this.waypoint = par2Waypoint;
        this.red = this.waypoint.red;
        this.green = this.waypoint.green;
        this.blue = this.waypoint.blue;
        this.suffix = this.waypoint.imageSuffix;
        this.enabled = this.waypoint.enabled;
        this.editing = editing;
    }

    public void tick() {
        //this.waypointName.setFocused(true);
        //this.waypointX.setFocused(true);
        //this.waypointY.setFocused(true);
        //this.waypointZ.setFocused(true);
    }

    public void init() {
        this.clearChildren();
        this.waypointName = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.waypointName.setText(this.waypoint.name);
        this.waypointX = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointX.setMaxLength(128);
        this.waypointX.setText(String.valueOf(this.waypoint.getX()));
        this.waypointZ = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setText(String.valueOf(this.waypoint.getZ()));
        this.waypointY = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointY.setMaxLength(128);
        this.waypointY.setText(String.valueOf(this.waypoint.getY()));
        this.addDrawableChild(this.waypointName);
        this.addDrawableChild(this.waypointX);
        this.addDrawableChild(this.waypointZ);
        this.addDrawableChild(this.waypointY);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addDrawableChild(this.buttonEnabled = new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY, 100, 20, Text.literal("Enabled: " + (this.waypoint.enabled ? "On" : "Off")), button -> this.waypoint.enabled = !this.waypoint.enabled, this));
        this.addDrawableChild(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20, Text.literal(I18n.translate("minimap.waypoints.sortbycolor") + ":     "), button -> this.choosingColor = true, this));
        this.addDrawableChild(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20, Text.literal(I18n.translate("minimap.waypoints.sortbyicon") + ":     "), button -> this.choosingIcon = true, this));
        this.doneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() / 6 + 168, 150, 20, Text.translatable("addServer.add"), button -> this.acceptWaypoint(), this);
        this.addDrawableChild(this.doneButton);
        this.addDrawableChild(new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() / 6 + 168, 150, 20, Text.translatable("gui.cancel"), button -> this.cancelWaypoint(), this));
        this.doneButton.active = !this.waypointName.getText().isEmpty();
        this.setFocused(this.waypointName);
        this.waypointName.setFocused(true);
        this.dimensionList = new GuiSlotDimensions(this);
    }

    @Override
    public void removed() {
    }

    protected void cancelWaypoint() {
        waypoint.red = red;
        waypoint.green = green;
        waypoint.blue = blue;
        waypoint.imageSuffix = suffix;
        waypoint.enabled = enabled;

        if (parentGui != null) {
            parentGui.accept(false);
            return;
        }

        VoxelConstants.getMinecraft().setScreen(null);
    }

    protected void acceptWaypoint() {
        waypoint.name = waypointName.getText();
        waypoint.setX(Integer.parseInt(waypointX.getText()));
        waypoint.setZ(Integer.parseInt(waypointZ.getText()));
        waypoint.setY(Integer.parseInt(waypointY.getText()));

        if (parentGui != null) {
            parentGui.accept(true);

            return;
        }

        if (editing) {
            waypointManager.saveWaypoints();
            VoxelConstants.getMinecraft().setScreen(null);

            return;
        }

        waypointManager.addWaypoint(waypoint);
        VoxelConstants.getMinecraft().setScreen(null);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.keyPressed(keyCode, scanCode, modifiers);
            boolean acceptable = !this.waypointName.getText().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getText());
                Integer.parseInt(this.waypointZ.getText());
                Integer.parseInt(this.waypointY.getText());
            } catch (NumberFormatException var7) {
                acceptable = false;
            }

            this.doneButton.active = acceptable;
            if ((keyCode == 257 || keyCode == 335) && acceptable) {
                this.acceptWaypoint();
            }
        }

        return OK;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.charTyped(chr, modifiers);
            boolean acceptable = !this.waypointName.getText().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getText());
                Integer.parseInt(this.waypointZ.getText());
                Integer.parseInt(this.waypointY.getText());
            } catch (NumberFormatException var6) {
                acceptable = false;
            }

            this.doneButton.active = acceptable;
        }

        return OK;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.popupOpen()) {
            super.mouseClicked(mouseX, mouseY, button);
            this.waypointName.mouseClicked(mouseX, mouseY, button);
            this.waypointX.mouseClicked(mouseX, mouseY, button);
            this.waypointZ.mouseClicked(mouseX, mouseY, button);
            this.waypointY.mouseClicked(mouseX, mouseY, button);
        } else if (this.choosingColor) {
            if (mouseX >= (this.getWidth() / 2f - 128) && mouseX < (this.getWidth() / 2f + 128) && mouseY >= (this.getHeight() / 2f - 128) && mouseY < (this.getHeight() / 2f + 128)) {
                int color = this.colorManager.getColorPicker().getRGB((int) mouseX - (this.getWidth() / 2 - 128), (int) mouseY - (this.getHeight() / 2 - 128));
                this.waypoint.red = (color >> 16 & 0xFF) / 255.0F;
                this.waypoint.green = (color >> 8 & 0xFF) / 255.0F;
                this.waypoint.blue = (color & 0xFF) / 255.0F;
                this.choosingColor = false;
            }
        } else if (this.choosingIcon) {
            float scScale = (float) VoxelConstants.getMinecraft().getWindow().getScaleFactor();
            TextureAtlas chooser = this.waypointManager.getTextureAtlasChooser();
            float scale = scScale / 2.0F;
            float displayWidthFloat = chooser.getWidth() / scale;
            float displayHeightFloat = chooser.getHeight() / scale;
            if (displayWidthFloat > VoxelConstants.getMinecraft().getWindow().getFramebufferWidth()) {
                float adj = displayWidthFloat / VoxelConstants.getMinecraft().getWindow().getFramebufferWidth();
                scale *= adj;
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            if (displayHeightFloat > VoxelConstants.getMinecraft().getWindow().getFramebufferHeight()) {
                float adj = displayHeightFloat / VoxelConstants.getMinecraft().getWindow().getFramebufferHeight();
                scale *= adj;
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            int displayWidth = (int) displayWidthFloat;
            int displayHeight = (int) displayHeightFloat;
            if (mouseX >= (this.getWidth() / 2f - displayWidth / 2f) && mouseX < (this.getWidth() / 2f + displayWidth / 2f) && mouseY >= (this.getHeight() / 2f - displayHeight / 2f) && mouseY < (this.getHeight() / 2f + displayHeight / 2f)) {
                float x = ((float) mouseX - (this.getWidth() / 2f - displayWidth / 2f)) * scale;
                float y = ((float) mouseY - (this.getHeight() / 2f - displayHeight / 2f)) * scale;
                Sprite icon = chooser.getIconAt(x, y);
                if (icon != chooser.getMissingImage()) {
                    this.waypoint.imageSuffix = icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                    this.choosingIcon = false;
                }
            }
        }

        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseClicked(mouseX, mouseY, button);
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseReleased(mouseX, mouseY, button);
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return !this.popupOpen() || this.dimensionList == null || this.dimensionList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        return !this.popupOpen() || this.dimensionList == null || this.dimensionList.mouseScrolled(mouseX, mouseY, 0, amount);
    }

    @Override
    public boolean overPopup(int mouseX, int mouseY) {
        return !this.choosingColor && !this.choosingIcon;
    }

    @Override
    public boolean popupOpen() {
        return !this.choosingColor && !this.choosingIcon;
    }

    @Override
    public void popupAction(Popup popup, int action) {
    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawMap(drawContext);
        float scScale = (float) VoxelConstants.getMinecraft().getWindow().getScaleFactor();
        this.tooltip = null;
        this.buttonEnabled.setMessage(Text.literal(I18n.translate("minimap.waypoints.enabled") + " " + (this.waypoint.enabled ? I18n.translate("options.on") : I18n.translate("options.off"))));
        if (!this.choosingColor && !this.choosingIcon) {
            this.renderInGameBackground(drawContext);
        }

        this.dimensionList.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredTextWithShadow(this.getFontRenderer(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18n.translate("minimap.waypoints.new") : I18n.translate("minimap.waypoints.edit"), this.getWidth() / 2, 20, 16777215);
        drawContext.drawTextWithShadow(this.getFontRenderer(), I18n.translate("minimap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 10526880);
        drawContext.drawTextWithShadow(this.getFontRenderer(), I18n.translate("X"), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 10526880);
        drawContext.drawTextWithShadow(this.getFontRenderer(), I18n.translate("Z"), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 10526880);
        drawContext.drawTextWithShadow(this.getFontRenderer(), I18n.translate("Y"), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 10526880);
        this.waypointName.render(drawContext, mouseX, mouseY, delta);
        this.waypointX.render(drawContext, mouseX, mouseY, delta);
        this.waypointZ.render(drawContext, mouseX, mouseY, delta);
        this.waypointY.render(drawContext, mouseX, mouseY, delta);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        super.render(drawContext, mouseX, mouseY, delta);
        RenderSystem.setShaderColor(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, this.blank);
        drawContext.drawTexture(this.blank, this.getWidth() / 2 - 25, buttonListY + 24 + 5, 0, 0, 16, 10);
        TextureAtlas chooser = this.waypointManager.getTextureAtlasChooser();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, chooser.getGlId());
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
        Sprite icon = chooser.getAtlasSprite("voxelmap:images/waypoints/waypoint" + this.waypoint.imageSuffix + ".png");
        this.drawTexturedModalRect((this.getWidth() / 2f - 25), (buttonListY + 48 + 2), icon, 16.0F, 16.0F);
        if (this.choosingColor || this.choosingIcon) {
            this.renderInGameBackground(drawContext);
        }

        if (this.choosingColor) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, this.pickerResourceLocation);
            RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
            RenderSystem.disableDepthTest();
            drawContext.drawTexture(pickerResourceLocation, this.getWidth() / 2 - 128, this.getHeight() / 2 - 128, 0, 0, 256, 256);
            RenderSystem.enableDepthTest();
        }

        if (this.choosingIcon) {
            float scale = scScale / 2.0F;
            float displayWidthFloat = chooser.getWidth() / scale;
            float displayHeightFloat = chooser.getHeight() / scale;
            if (displayWidthFloat > VoxelConstants.getMinecraft().getWindow().getFramebufferWidth()) {
                float adj = displayWidthFloat / VoxelConstants.getMinecraft().getWindow().getFramebufferWidth();
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            if (displayHeightFloat > VoxelConstants.getMinecraft().getWindow().getFramebufferHeight()) {
                float adj = displayHeightFloat / VoxelConstants.getMinecraft().getWindow().getFramebufferHeight();
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            int displayWidth = (int) displayWidthFloat;
            int displayHeight = (int) displayHeightFloat;
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, this.blank);
            RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
            drawContext.drawTexture(blank, this.getWidth() / 2 - displayWidth / 2 - 1, this.getHeight() / 2 - displayHeight / 2 - 1, 0, 0, displayWidth + 2, displayHeight + 2);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            drawContext.drawTexture(blank, this.getWidth() / 2 - displayWidth / 2, this.getHeight() / 2 - displayHeight / 2, 0, 0, displayWidth, displayHeight);
            RenderSystem.setShaderColor(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, chooser.getGlId());
            RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);

            this.drawTexturedModalRect(this.getWidth() / 2f - displayWidth / 2f, this.getHeight() / 2f - displayHeight / 2f, displayWidth, displayHeight);
            if (mouseX >= this.getWidth() / 2 - displayWidth / 2 && mouseX <= this.getWidth() / 2 + displayWidth / 2 && mouseY >= this.getHeight() / 2 - displayHeight / 2 && mouseY <= this.getHeight() / 2 + displayHeight / 2) {
                float x = (mouseX - (this.getWidth() / 2f - displayWidth / 2f)) * scale;
                float y = (mouseY - (this.getHeight() / 2f - displayHeight / 2f)) * scale;
                icon = chooser.getIconAt(x, y);
                if (icon != chooser.getMissingImage()) {
                    this.tooltip = Text.literal(icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", ""));
                }
            }
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
        }

        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    public void setSelectedDimension(DimensionContainer dimension) {
        this.selectedDimension = dimension;
    }

    public void toggleDimensionSelected() {
        if (this.waypoint.dimensions.size() > 1 && this.waypoint.dimensions.contains(this.selectedDimension) && this.selectedDimension != VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().getWorld())) {
            this.waypoint.dimensions.remove(this.selectedDimension);
        } else
            this.waypoint.dimensions.add(this.selectedDimension);

    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Text par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }

    public void drawTexturedModalRect(float xCoord, float yCoord, Sprite icon, float widthIn, float heightIn) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexbuffer.vertex(xCoord + 0.0F, yCoord + heightIn, 0).texture(icon.getMinU(), icon.getMaxV()).next();
        vertexbuffer.vertex(xCoord + widthIn, yCoord + heightIn, 0).texture(icon.getMaxU(), icon.getMaxV()).next();
        vertexbuffer.vertex(xCoord + widthIn, yCoord + 0.0F, 0).texture(icon.getMaxU(), icon.getMinV()).next();
        vertexbuffer.vertex(xCoord + 0.0F, yCoord + 0.0F, 0).texture(icon.getMinU(), icon.getMinV()).next();
        tessellator.draw();
    }

    public void drawTexturedModalRect(float x, float y, float width, float height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexBuffer.vertex(x + 0.0F, y + height, 0).texture(0.0F, 1.0F).next();
        vertexBuffer.vertex(x + width, y + height, 0).texture(1.0F, 1.0F).next();
        vertexBuffer.vertex(x + width, y + 0.0F, 0).texture(1.0F, 0.0F).next();
        vertexBuffer.vertex(x + 0.0F, y + 0.0F, 0).texture(0.0F, 0.0F).next();
        tessellator.draw();
    }
}
