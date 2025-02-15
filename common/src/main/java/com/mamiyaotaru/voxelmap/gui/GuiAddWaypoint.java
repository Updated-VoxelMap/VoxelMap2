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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    private static final ResourceLocation BLANK = ResourceLocation.parse("textures/misc/white.png");
    private static final ResourceLocation PICKER = ResourceLocation.parse("voxelmap:images/colorpicker.png");
    private static final ResourceLocation TARGET = ResourceLocation.parse("voxelmap:images/waypoints/target.png");
    final WaypointManager waypointManager;
    final ColorManager colorManager;
    private final IGuiWaypoints parentGui;
    private PopupGuiButton doneButton;
    private GuiSlotDimensions dimensionList;
    protected DimensionContainer selectedDimension;
    private Component tooltip;
    private EditBox waypointName;
    private EditBox waypointX;
    private EditBox waypointY;
    private EditBox waypointZ;
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

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        this.clearWidgets();
        this.waypointName = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.waypointName.setValue(this.waypoint.name);
        this.waypointX = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointX.setMaxLength(128);
        this.waypointX.setValue(String.valueOf(this.waypoint.getX()));
        this.waypointY = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointY.setMaxLength(128);
        this.waypointY.setValue(String.valueOf(this.waypoint.getY()));
        this.waypointZ = new EditBox(this.getFontRenderer(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setValue(String.valueOf(this.waypoint.getZ()));
        this.addRenderableWidget(this.waypointName);
        this.addRenderableWidget(this.waypointX);
        this.addRenderableWidget(this.waypointY);
        this.addRenderableWidget(this.waypointZ);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addRenderableWidget(this.buttonEnabled = new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY, 100, 20, Component.literal("Enabled: " + (this.waypoint.enabled ? "On" : "Off")), button -> this.waypoint.enabled = !this.waypoint.enabled, this));
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortbycolor") + ":     "), button -> this.choosingColor = true, this));
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortbyicon") + ":     "), button -> this.choosingIcon = true, this));
        this.doneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() / 6 + 168, 150, 20, Component.translatable("addServer.add"), button -> this.acceptWaypoint(), this);
        this.addRenderableWidget(this.doneButton);
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() / 6 + 168, 150, 20, Component.translatable("gui.cancel"), button -> this.cancelWaypoint(), this));
        this.doneButton.active = !this.waypointName.getValue().isEmpty();
        this.setFocused(this.waypointName);
        this.waypointName.setFocused(true);
        this.dimensionList = new GuiSlotDimensions(this);
        this.addRenderableWidget(dimensionList);
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
        waypoint.name = waypointName.getValue();
        waypoint.setX(Integer.parseInt(waypointX.getValue()));
        waypoint.setY(Integer.parseInt(waypointY.getValue()));
        waypoint.setZ(Integer.parseInt(waypointZ.getValue()));

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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.keyPressed(keyCode, scanCode, modifiers);
            boolean acceptable = !this.waypointName.getValue().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getValue());
                Integer.parseInt(this.waypointY.getValue());
                Integer.parseInt(this.waypointZ.getValue());
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

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.charTyped(chr, modifiers);
            boolean acceptable = !this.waypointName.getValue().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getValue());
                Integer.parseInt(this.waypointY.getValue());
                Integer.parseInt(this.waypointZ.getValue());
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
            this.waypointY.mouseClicked(mouseX, mouseY, button);
            this.waypointZ.mouseClicked(mouseX, mouseY, button);
        }
        else if (choosingColor){
            int pickerSize = (int) (80 * VoxelConstants.getMinecraft().getWindow().getGuiScale());
            int pickerCenterX = this.getWidth() / 2 - pickerSize / 2;
            int pickerCenterY = this.getHeight() / 2 - pickerSize / 2;
            if (mouseX >= pickerCenterX && mouseX <= pickerCenterX + pickerSize && mouseY >= pickerCenterY && mouseY <= pickerCenterY + pickerSize){
                int pickPointX = (int) ((mouseX - pickerCenterX) / (float) pickerSize * 255f);
                int pickPointY = (int) ((mouseY - pickerCenterY) / (float) pickerSize * 255f);
                int color = this.colorManager.getColorPicker().getRGB(pickPointX, pickPointY);
                this.waypoint.red = (color >> 16 & 0xFF) / 255.0f;
                this.waypoint.green = (color >> 8 & 0xFF) / 255.0f;
                this.waypoint.blue = (color & 0xFF) / 255.0f;
                this.choosingColor = false;
            }
        }
        else if (choosingIcon){
            TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
            float chooserCenterX = this.getWidth() / 2f - chooser.getWidth() / 2f;
            float chooserCenterY = this.getHeight() / 2f - chooser.getHeight() / 2f;
            Sprite icon = chooser.getIconAt((float) mouseX - chooserCenterX, (float) mouseY - chooserCenterY);

            if (icon != chooser.getMissingImage()){
                this.waypoint.imageSuffix = icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                this.choosingIcon = false;
            }
        }

        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseClicked(mouseX, mouseY, button);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseReleased(mouseX, mouseY, button);
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return !this.popupOpen() || this.dimensionList == null || this.dimensionList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
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

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.buttonEnabled.setMessage(Component.literal(I18n.get("minimap.waypoints.enabled") + " " + (this.waypoint.enabled ? I18n.get("options.on") : I18n.get("options.off"))));

        renderBackgroundTexture(drawContext);
        drawContext.drawCenteredString(this.getFontRenderer(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18n.get("minimap.waypoints.new") : I18n.get("minimap.waypoints.edit"), this.getWidth() / 2, 20, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("minimap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("X"), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("Y"), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("Z"), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 16777215);
        super.render(drawContext, this.choosingColor || this.choosingIcon ? 0 : mouseX, this.choosingColor || this.choosingIcon ? 0 : mouseY, delta);

        int buttonListY = this.getHeight() / 6 + 88;
        ResourceLocation waypointIcon = ResourceLocation.parse("voxelmap:images/waypoints/waypoint" + this.waypoint.imageSuffix + ".png");
        RenderSystem.setShaderColor(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0F);
        drawContext.blit(RenderType::guiTextured, BLANK, this.getWidth() / 2 - 25, buttonListY + 24 + 5, 0, 0, 16, 10, 256, 256);
        drawContext.blit(RenderType::guiTextured, waypointIcon, this.getWidth() / 2 - 25, buttonListY + 48 + 2, 0.0F, 0.0F, 16, 16, 16, 16);
        drawContext.flush();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (this.choosingColor || this.choosingIcon) {
            renderBackgroundTexture(drawContext);

            if (this.choosingColor) {
                int pickerSize = (int) (80 * VoxelConstants.getMinecraft().getWindow().getGuiScale());
                int pickerCenterX = this.getWidth() / 2 - pickerSize / 2;
                int pickerCenterY = this.getHeight() / 2 - pickerSize / 2;
                drawContext.blit(RenderType::guiTextured, PICKER, pickerCenterX, pickerCenterY, 0f, 0f, pickerSize, pickerSize, pickerSize, pickerSize);
                if (mouseX >= pickerCenterX && mouseX <= pickerCenterX + pickerSize && mouseY >= pickerCenterY && mouseY <= pickerCenterY + pickerSize){
                    int pickPointX = (int) ((mouseX - pickerCenterX) / (float) pickerSize * 255f);
                    int pickPointY = (int) ((mouseY - pickerCenterY) / (float) pickerSize * 255f);
                    int color = this.colorManager.getColorPicker().getRGB(pickPointX, pickPointY);
                    int curR = (color >> 16 & 0xFF);
                    int curG = (color >> 8 & 0xFF);
                    int curB = (color & 0xFF);
                    drawContext.blit(RenderType::guiTextured, TARGET, mouseX - 8, mouseY - 8, 0f, 0f, 16, 16, 16, 16);
                    drawContext.drawCenteredString(this.getFontRenderer(), "R: " + curR + ", G: " + curG + ", B: " + curB, this.getWidth() / 2, this.getHeight() / 2 + pickerSize / 2 + 8, color);
                }
                drawContext.flush();
            }
            if (this.choosingIcon) {
                TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
                float chooserCenterX = this.getWidth() / 2f - chooser.getWidth() / 2f;
                float chooserCenterY = this.getHeight() / 2f - chooser.getHeight() / 2f;
                Sprite icon = chooser.getIconAt(mouseX - chooserCenterX, mouseY - chooserCenterY);

                RenderSystem.setShader(CoreShaders.POSITION_TEX);
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.75f);
                RenderSystem.setShaderTexture(0, chooser.getId());
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
                drawTexturedModalRect(chooserCenterX, chooserCenterY, chooser.getWidth(), chooser.getHeight());
                RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

                if (icon != chooser.getMissingImage()){
                    ResourceLocation currentIcon = ResourceLocation.parse(icon.getIconName());
                    int iconSnappedX = icon.getOriginX() + (int) chooserCenterX;
                    int iconSnappedY = icon.getOriginY() + (int) chooserCenterY;
                    RenderSystem.setShaderColor(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1f);
                    drawContext.blit(RenderType::guiTextured, currentIcon, iconSnappedX - 4, iconSnappedY - 4, 0f, 0f, 40, 40, 40, 40);
                    drawContext.flush();
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

                    String iconName = icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                    if (iconName.length() > 1){
                        iconName = iconName.substring(0, 1).toUpperCase() + iconName.substring(1).toLowerCase();
                    }
                    tooltip = Component.literal(iconName);
                }
            }
        }

        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    public void setSelectedDimension(DimensionContainer dimension) {
        this.selectedDimension = dimension;
    }

    public void toggleDimensionSelected() {
        if (this.waypoint.dimensions.size() > 1 && this.waypoint.dimensions.contains(this.selectedDimension) && this.selectedDimension != VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level())) {
            this.waypoint.dimensions.remove(this.selectedDimension);
        } else {
            this.waypoint.dimensions.add(this.selectedDimension);
        }

    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Component par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }

    public void drawTexturedModalRect(float x, float y, float width, float height) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        vertexBuffer.addVertex(x + 0.0F, y + height, 0).setUv(0.0F, 1.0F);
        vertexBuffer.addVertex(x + width, y + height, 0).setUv(1.0F, 1.0F);
        vertexBuffer.addVertex(x + width, y + 0.0F, 0).setUv(1.0F, 0.0F);
        vertexBuffer.addVertex(x + 0.0F, y + 0.0F, 0).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
    }
}
