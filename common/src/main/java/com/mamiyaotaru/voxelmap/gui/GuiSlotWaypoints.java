package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

class GuiSlotWaypoints extends AbstractSelectionList<GuiSlotWaypoints.WaypointItem> {
    private final ArrayList<WaypointItem> waypoints;
    private ArrayList<?> waypointsFiltered;
    final GuiWaypoints parentGui;
    private String filterString = "";
    static final Component ENABLETOOLTIP = Component.translatable("minimap.waypoints.enabletooltip");
    static final Component DISABLETOOLTIP = Component.translatable("minimap.waypoints.disabletooltip");
    static final Component HIGHLIGHTTOOLTIP = Component.translatable("minimap.waypoints.highlighttooltip");
    static final Component REMOVEHIGHLIGHTTOOLTIP = Component.translatable("minimap.waypoints.removehighlighttooltip");
    final ResourceLocation visibleIconIdentifier = ResourceLocation.parse("textures/gui/sprites/container/beacon/confirm.png");
    final ResourceLocation invisibleIconIdentifier = ResourceLocation.parse("textures/gui/sprites/container/beacon/cancel.png");
    final ResourceLocation highlightedIconIdentifier = ResourceLocation.parse("voxelmap:images/waypoints/target.png");
    protected long lastClicked;
    public boolean doubleClicked;

    GuiSlotWaypoints(GuiWaypoints par1GuiWaypoints) {
        super(VoxelConstants.getMinecraft(), par1GuiWaypoints.getWidth(), par1GuiWaypoints.getHeight() - 140, 54, 18);
        this.parentGui = par1GuiWaypoints;
        this.waypoints = new ArrayList<>();

        for (Waypoint pt : this.parentGui.waypointManager.getWaypoints()) {
            if (pt.inWorld && pt.inDimension) {
                this.waypoints.add(new WaypointItem(this.parentGui, pt));
            }
        }

        this.waypointsFiltered = new ArrayList<>(this.waypoints);
        this.waypointsFiltered.forEach(x -> this.addEntry((WaypointItem) x));
    }

    public void setSelected(WaypointItem entry) {
        super.setSelected(entry);
        if (this.getSelected() instanceof WaypointItem) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayNow((Component.translatable("narrator.select", ((WaypointItem) this.getSelected()).waypoint.name)).getString());
        }

        this.parentGui.setSelectedWaypoint(entry.waypoint);
    }

    protected boolean isSelectedItem(int index) {
        return ((WaypointItem) this.waypointsFiltered.get(index)).waypoint.equals(this.parentGui.selectedWaypoint);
    }

    protected void sortBy(int sortKey, boolean ascending) {
        final int order = ascending ? 1 : -1;
        if (sortKey == 1) {
            final ArrayList<?> masterWaypointsList = this.parentGui.waypointManager.getWaypoints();
            this.waypoints.sort((waypointEntry1, waypointEntry2) -> Double.compare(masterWaypointsList.indexOf(waypointEntry1.waypoint), masterWaypointsList.indexOf(waypointEntry2.waypoint)) * order);
        } else if (sortKey == 3) {
            if (ascending) {
                Collections.sort(this.waypoints);
            } else {
                this.waypoints.sort(Collections.reverseOrder());
            }
        } else if (sortKey == 2) {
            this.waypoints.sort((waypointEntry1, waypointEntry2) -> String.CASE_INSENSITIVE_ORDER.compare(waypointEntry1.waypoint.name, waypointEntry2.waypoint.name) * order);
        } else if (sortKey == 4) {
            this.waypoints.sort((waypointEntry1, waypointEntry2) -> {
                Waypoint waypoint1 = waypointEntry1.waypoint;
                Waypoint waypoint2 = waypointEntry2.waypoint;
                float hue1 = Color.RGBtoHSB((int) (waypoint1.red * 255.0F), (int) (waypoint1.green * 255.0F), (int) (waypoint1.blue * 255.0F), null)[0];
                float hue2 = Color.RGBtoHSB((int) (waypoint2.red * 255.0F), (int) (waypoint2.green * 255.0F), (int) (waypoint2.blue * 255.0F), null)[0];
                return Double.compare(hue1, hue2) * order;
            });
        }

        this.updateFilter(this.filterString);
    }

    protected void updateFilter(String filterString) {
        this.clearEntries();
        this.filterString = filterString;
        this.waypointsFiltered = new ArrayList<>(this.waypoints);
        Iterator<?> iterator = this.waypointsFiltered.iterator();

        while (iterator.hasNext()) {
            Waypoint waypoint = ((WaypointItem) iterator.next()).waypoint;
            if (!TextUtils.scrubCodes(waypoint.name).toLowerCase().contains(filterString)) {
                if (waypoint == this.parentGui.selectedWaypoint) {
                    this.parentGui.setSelectedWaypoint(null);
                }

                iterator.remove();
            }
        }

        this.waypointsFiltered.forEach(x -> this.addEntry((WaypointItem) x));
    }

    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.doubleClicked = System.currentTimeMillis() - this.lastClicked < 200L;
        this.lastClicked = System.currentTimeMillis();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public class WaypointItem extends AbstractSelectionList.Entry<WaypointItem> implements Comparable<WaypointItem> {
        private final GuiWaypoints parentGui;
        private final Waypoint waypoint;
        private final ResourceLocation iconSprite;

        protected WaypointItem(GuiWaypoints waypointScreen, Waypoint waypoint) {
            this.parentGui = waypointScreen;
            this.waypoint = waypoint;
            this.iconSprite = ResourceLocation.parse("voxelmap:images/waypoints/waypoint" + waypoint.imageSuffix + ".png");
        }

        @Override
        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawContext.drawCenteredString(this.parentGui.getFontRenderer(), this.waypoint.name, this.parentGui.getWidth() / 2, y + 3, this.waypoint.getUnifiedColor());
            byte padding = 3;
            byte iconWidth = 16;
            if (mouseX >= x - padding && mouseY >= y && mouseX <= x + 215 + padding && mouseY <= y + entryHeight) {
                Component tooltip;
                if (mouseX >= x + 215 - iconWidth - padding && mouseX <= x + 215 + padding) {
                    tooltip = this.waypoint.enabled ? GuiSlotWaypoints.DISABLETOOLTIP : GuiSlotWaypoints.ENABLETOOLTIP;
                } else if (mouseX >= x + padding && mouseX <= x + iconWidth + padding){
                    tooltip = this.waypoint == this.parentGui.highlightedWaypoint ? REMOVEHIGHLIGHTTOOLTIP : HIGHLIGHTTOOLTIP;
                } else{
                    String tooltipText = "X: " + this.waypoint.getX() + " Z: " + this.waypoint.getZ();
                    if (this.waypoint.getY() > minecraft.level.getMinY()) {
                        tooltipText = tooltipText + " Y: " + this.waypoint.getY();
                    }

                    tooltip = Component.literal(tooltipText);
                }

                if (mouseX >= GuiSlotWaypoints.this.getX() && mouseX <= GuiSlotWaypoints.this.getRight() && mouseY >= GuiSlotWaypoints.this.getY() && mouseY <= GuiSlotWaypoints.this.getBottom()) {
                    GuiWaypoints.setTooltip(GuiSlotWaypoints.this.parentGui, tooltip);
                }
            }
            drawContext.blit(RenderType::guiTextured, this.waypoint.enabled ? GuiSlotWaypoints.this.visibleIconIdentifier : GuiSlotWaypoints.this.invisibleIconIdentifier, x + 198, y - 2, 0.0F, 0.0F, 18, 18, 18, 18);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            drawContext.blit(RenderType::guiTextured, this.iconSprite, x, y - 2, 0.0F, 0.0F, 18, 18, 18, 18);
            RenderSystem.setShaderColor(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0f);

            if (this.waypoint == this.parentGui.highlightedWaypoint) {
                drawContext.blit(RenderType::guiTextured, GuiSlotWaypoints.this.highlightedIconIdentifier, x, y - 2, 0.0F, 0.0F, 18, 18, 18, 18);
                RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 1.0f);
            }
            drawContext.flush();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            GuiSlotWaypoints.this.setSelected(this);
            int leftEdge = this.parentGui.getWidth() / 2 - 92 - 16;
            byte padding = 3;
            byte iconWidth = 16;
            int width = 215;
            if (mouseX >= (leftEdge + width - iconWidth - padding) && mouseX <= (leftEdge + width + padding)) {
                this.parentGui.toggleWaypointVisibility();
            } else if (mouseX >= (leftEdge + padding) && mouseX <= (leftEdge + iconWidth + padding)){
                this.parentGui.setHighlightedWaypoint();
            } else if (GuiSlotWaypoints.this.doubleClicked) {
                this.parentGui.editWaypoint(this.parentGui.selectedWaypoint);
            }

            return true;
        }

        public int compareTo(WaypointItem o) {
            return this.waypoint.compareTo(o.waypoint);
        }
    }
}
