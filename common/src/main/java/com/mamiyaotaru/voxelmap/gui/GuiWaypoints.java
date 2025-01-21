package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

public class GuiWaypoints extends GuiScreenMinimap implements IGuiWaypoints {
    private final Screen parentScreen;
    protected final MapSettingsManager options;
    protected final WaypointManager waypointManager;
    protected Component screenTitle;
    private GuiSlotWaypoints waypointList;
    private Button buttonEdit;
    private boolean editClicked;
    private Button buttonDelete;
    private boolean deleteClicked;
    private Button buttonHighlight;
    private Button buttonShare;
    private Button buttonTeleport;
    private Button buttonSortName;
    private Button buttonSortCreated;
    private Button buttonSortDistance;
    private Button buttonSortColor;
    protected EditBox filter;
    private boolean addClicked;
    private Component tooltip;
    protected Waypoint selectedWaypoint;
    protected Waypoint highlightedWaypoint;
    protected Waypoint newWaypoint;
    private final Random generator = new Random();
    private boolean changedSort;

    public GuiWaypoints(Screen parentScreen) {
        this.parentScreen = parentScreen;
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.highlightedWaypoint = this.waypointManager.getHighlightedWaypoint();
    }

    public void tick() {
    }

    public void init() {
        this.screenTitle = Component.translatable("minimap.waypoints.title");
        this.waypointList = new GuiSlotWaypoints(this);
        this.addRenderableWidget(this.buttonSortName = new Button.Builder(Component.translatable("minimap.waypoints.sortbyname"), button -> this.sortClicked(2)).bounds(this.getWidth() / 2 - 154, 34, 77, 20).build());
        this.addRenderableWidget(this.buttonSortDistance = new Button.Builder(Component.translatable("minimap.waypoints.sortbydistance"), button -> this.sortClicked(3)).bounds(this.getWidth() / 2 - 77, 34, 77, 20).build());
        this.addRenderableWidget(this.buttonSortCreated = new Button.Builder(Component.translatable("minimap.waypoints.sortbycreated"), button -> this.sortClicked(1)).bounds(this.getWidth() / 2, 34, 77, 20).build());
        this.addRenderableWidget(this.buttonSortColor = new Button.Builder(Component.translatable("minimap.waypoints.sortbycolor"), button -> this.sortClicked(4)).bounds(this.getWidth() / 2 + 77, 34, 77, 20).build());
        int filterStringWidth = this.getFontRenderer().width(I18n.get("minimap.waypoints.filter") + ":");
        this.filter = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 80, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addRenderableWidget(this.filter);
        this.addRenderableWidget(new Button.Builder(Component.translatable("minimap.waypoints.add"), button -> this.addWaypoint()).bounds(this.getWidth() / 2 - 154, this.getHeight() - 52, 74, 20).build());
        this.addRenderableWidget(this.buttonEdit = new Button.Builder(Component.translatable("selectServer.edit"), button -> this.editWaypoint(this.selectedWaypoint)).bounds(this.getWidth() / 2 - 76, this.getHeight() - 52, 74, 20).build());
        this.addRenderableWidget(this.buttonDelete = new Button.Builder(Component.translatable("selectServer.delete"), button -> this.deleteClicked()).bounds(this.getWidth() / 2 + 2, this.getHeight() - 52, 74, 20).build());
        this.addRenderableWidget(this.buttonHighlight = new Button.Builder(Component.translatable("minimap.waypoints.highlight"), button -> this.setHighlightedWaypoint()).bounds(this.getWidth() / 2 + 80, this.getHeight() - 52, 74, 20).build());
        this.addRenderableWidget(this.buttonTeleport = new Button.Builder(Component.translatable("minimap.waypoints.teleportto"), button -> this.teleportClicked()).bounds(this.getWidth() / 2 - 154, this.getHeight() - 28, 74, 20).build());
        this.addRenderableWidget(this.buttonShare = new Button.Builder(Component.translatable("minimap.waypoints.share"), button -> CommandUtils.sendWaypoint(this.selectedWaypoint)).bounds(this.getWidth() / 2 - 76, this.getHeight() - 28, 74, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("menu.options"), button -> VoxelConstants.getMinecraft().setScreen(new GuiWaypointsOptions(this, this.options))).bounds(this.getWidth() / 2 + 2, this.getHeight() - 28, 74, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 + 80, this.getHeight() - 28, 74, 20).build());
        this.setFocused(this.filter);
        this.filter.setFocused(true);
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonEdit.active = isSomethingSelected;
        this.buttonDelete.active = isSomethingSelected;
        this.buttonHighlight.active = isSomethingSelected;
        this.buttonShare.active = isSomethingSelected;
        this.buttonTeleport.active = isSomethingSelected && this.canTeleport();
        this.sort();
    }

    private void sort() {
        int sortKey = Math.abs(this.options.sort);
        boolean ascending = this.options.sort > 0;
        this.waypointList.sortBy(sortKey, ascending);
        String arrow = ascending ? "↑" : "↓";
        if (sortKey == 2) {
            this.buttonSortName.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortbyname") + " " + arrow));
        } else {
            this.buttonSortName.setMessage(Component.translatable("minimap.waypoints.sortbyname"));
        }

        if (sortKey == 3) {
            this.buttonSortDistance.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortbydistance") + " " + arrow));
        } else {
            this.buttonSortDistance.setMessage(Component.translatable("minimap.waypoints.sortbydistance"));
        }

        if (sortKey == 1) {
            this.buttonSortCreated.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortbycreated") + " " + arrow));
        } else {
            this.buttonSortCreated.setMessage(Component.translatable("minimap.waypoints.sortbycreated"));
        }

        if (sortKey == 4) {
            this.buttonSortColor.setMessage(Component.literal(arrow + " " + I18n.get("minimap.waypoints.sortbycolor") + " " + arrow));
        } else {
            this.buttonSortColor.setMessage(Component.translatable("minimap.waypoints.sortbycolor"));
        }

    }

    private void deleteClicked() {
        String var2 = this.selectedWaypoint.name;
        if (var2 != null) {
            this.deleteClicked = true;
            Component title = Component.translatable("minimap.waypoints.deleteconfirm");
            Component explanation = Component.translatable("selectServer.deleteWarning", var2);
            Component affirm = Component.translatable("selectServer.deleteButton");
            Component deny = Component.translatable("gui.cancel");
            ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
            VoxelConstants.getMinecraft().setScreen(confirmScreen);
        }

    }

    private void teleportClicked() {
        int y = selectedWaypoint.getY() > VoxelConstants.getPlayer().level().getMinY() ? selectedWaypoint.getY() : (!(VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) ? VoxelConstants.getPlayer().level().getMaxY() : 64);
        VoxelConstants.playerRunTeleportCommand(selectedWaypoint.getX(), y, selectedWaypoint.getZ());
        VoxelConstants.getMinecraft().setScreen(null);
    }

    protected void sortClicked(int id) {
        this.options.setSort(id);
        this.changedSort = true;
        this.sort();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = super.keyPressed(keyCode, scanCode, modifiers);
        if (this.filter.isFocused()) {
            this.waypointList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = super.charTyped(chr, modifiers);
        if (this.filter.isFocused()) {
            this.waypointList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.waypointList.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.waypointList.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.waypointList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        return this.waypointList.mouseScrolled(mouseX, mouseY, 0, amount);
    }

    @Override
    public boolean isEditing() {
        return this.editClicked;
    }

    public void accept(boolean b) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (b) {
                this.waypointManager.deleteWaypoint(this.selectedWaypoint);
                this.selectedWaypoint = null;
            }

            VoxelConstants.getMinecraft().setScreen(this);
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (b) {
                this.waypointManager.saveWaypoints();
            }

            VoxelConstants.getMinecraft().setScreen(this);
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (b) {
                this.waypointManager.addWaypoint(this.newWaypoint);
                this.setSelectedWaypoint(this.newWaypoint);
            }

            VoxelConstants.getMinecraft().setScreen(this);
        }

    }

    protected void setSelectedWaypoint(Waypoint waypoint) {
        this.selectedWaypoint = waypoint;
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonEdit.active = isSomethingSelected;
        this.buttonDelete.active = isSomethingSelected;
        this.buttonHighlight.active = isSomethingSelected;
        this.buttonHighlight.setMessage(Component.translatable(isSomethingSelected && this.selectedWaypoint == this.highlightedWaypoint ? "minimap.waypoints.removehighlight" : "minimap.waypoints.highlight"));
        this.buttonShare.active = isSomethingSelected;
        this.buttonTeleport.active = isSomethingSelected && this.canTeleport();
    }

    protected void setHighlightedWaypoint() {
        this.waypointManager.setHighlightedWaypoint(this.selectedWaypoint, true);
        this.highlightedWaypoint = this.waypointManager.getHighlightedWaypoint();
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonHighlight.setMessage(Component.translatable(isSomethingSelected && this.selectedWaypoint == this.highlightedWaypoint ? "minimap.waypoints.removehighlight" : "minimap.waypoints.highlight"));
    }

    protected void editWaypoint(Waypoint waypoint) {
        this.editClicked = true;
        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, waypoint, true));
    }

    protected void addWaypoint() {
        this.addClicked = true;
        float r;
        float g;
        float b;
        if (this.waypointManager.getWaypoints().isEmpty()) {
            r = 0.0F;
            g = 1.0F;
            b = 0.0F;
        } else {
            r = this.generator.nextFloat();
            g = this.generator.nextFloat();
            b = this.generator.nextFloat();
        }

        TreeSet<DimensionContainer> dimensions = new TreeSet<>();
        dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
        double dimensionScale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();
        this.newWaypoint = new Waypoint("", (int) (GameVariableAccessShim.xCoord() * dimensionScale), (int) (GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "", VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.newWaypoint, false));
    }

    protected void toggleWaypointVisibility() {
        this.selectedWaypoint.enabled = !this.selectedWaypoint.enabled;
        this.waypointManager.saveWaypoints();
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        renderBackgroundTexture(drawContext);
        this.tooltip = null;
        this.waypointList.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawString(this.getFontRenderer(), I18n.get("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 75, 10526880);
        this.filter.render(drawContext, mouseX, mouseY, delta);
        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    static void setTooltip(GuiWaypoints par0GuiWaypoints, Component par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }

    public boolean canTeleport() {
        Optional<IntegratedServer> integratedServer = VoxelConstants.getIntegratedServer();

        if (integratedServer.isEmpty()) return true;

        try {
            return integratedServer.get().getPlayerList().isOp(VoxelConstants.getPlayer().getGameProfile());
        } catch (RuntimeException exception) {
            return integratedServer.get().getWorldData().isAllowCommands();
        }
    }

    @Override
    public void removed() {

        if (changedSort) super.removed();
    }
}
