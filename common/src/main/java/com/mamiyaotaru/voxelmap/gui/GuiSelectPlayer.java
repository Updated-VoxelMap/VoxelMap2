package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class GuiSelectPlayer extends GuiScreenMinimap implements BooleanConsumer {
    private final Screen parentScreen;
    protected Component screenTitle = Component.literal("players");
    private final boolean sharingWaypoint;
    private GuiButtonRowListPlayers playerList;
    protected boolean allClicked;
    protected EditBox message;
    protected EditBox filter;
    private Component tooltip;
    private final String locInfo;
    static final MutableComponent SHARE_MESSAGE = (Component.translatable("minimap.waypointshare.sharemessage")).append(":");
    static final Component SHARE_WITH = Component.translatable("minimap.waypointshare.sharewith");
    static final Component SHARE_WAYPOINT = Component.translatable("minimap.waypointshare.title");
    static final Component SHARE_COORDINATES = Component.translatable("minimap.waypointshare.titlecoordinate");

    public GuiSelectPlayer(Screen parentScreen, String locInfo, boolean sharingWaypoint) {
        this.parentScreen = parentScreen;
        this.locInfo = locInfo;
        this.sharingWaypoint = sharingWaypoint;
    }

    public void tick() {
    }

    public void init() {
        this.screenTitle = this.sharingWaypoint ? SHARE_WAYPOINT : SHARE_COORDINATES;
        this.playerList = new GuiButtonRowListPlayers(this);
        int messageStringWidth = this.getFontRenderer().width(I18n.get("minimap.waypointshare.sharemessage") + ":");
        this.message = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 153 + messageStringWidth + 5, 34, 305 - messageStringWidth - 5, 20, null);
        this.message.setMaxLength(78);
        this.addRenderableWidget(this.message);
        int filterStringWidth = this.getFontRenderer().width(I18n.get("minimap.waypoints.filter") + ":");
        this.filter = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 55, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addRenderableWidget(this.filter);
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.width / 2 - 100, this.height - 27, 150, 20).build());
        this.setFocused(this.filter);
        this.filter.setFocused(true);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = super.keyPressed(keyCode, scanCode, modifiers);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = super.charTyped(chr, modifiers);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.playerList.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.playerList.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.playerList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        return this.playerList.mouseScrolled(mouseX, mouseY, 0, amount);
    }

    public void accept(boolean b) {
        if (this.allClicked) {
            this.allClicked = false;
            if (b) {
                String combined = this.message.getValue() + " " + this.locInfo;
                if (combined.length() > 256) {
                    VoxelConstants.getPlayer().connection.sendChat(this.message.getValue());
                    VoxelConstants.getPlayer().connection.sendChat(this.locInfo);
                } else {
                    VoxelConstants.getPlayer().connection.sendChat(combined);
                }

                VoxelConstants.getMinecraft().setScreen(this.parentScreen);
            } else {
                VoxelConstants.getMinecraft().setScreen(this);
            }
        }

    }

    protected void sendMessageToPlayer(String name) {
        String combined = "msg " + name + " " + this.message.getValue() + " " + this.locInfo;
        if (combined.length() > 256) {
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + this.message.getValue());
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + this.locInfo);
        } else {
            VoxelConstants.getPlayer().connection.sendCommand(combined);
        }

        VoxelConstants.getMinecraft().setScreen(this.parentScreen);
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        renderBackgroundTexture(drawContext);
        this.tooltip = null;
        this.playerList.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawString(this.getFontRenderer(), SHARE_MESSAGE, this.getWidth() / 2 - 153, 39, 10526880);
        this.message.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredString(this.getFontRenderer(), SHARE_WITH, this.getWidth() / 2, 75, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 50, 10526880);
        this.filter.render(drawContext, mouseX, mouseY, delta);
        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    static void setTooltip(GuiSelectPlayer par0GuiWaypoints, Component par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }

    @Override
    public void removed() {
    }
}
