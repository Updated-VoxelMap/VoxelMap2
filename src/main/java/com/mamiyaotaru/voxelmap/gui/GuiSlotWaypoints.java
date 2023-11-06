package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiSlotMinimap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

class GuiSlotWaypoints extends GuiSlotMinimap {
    private final ArrayList<WaypointItem> waypoints;
    private ArrayList<?> waypointsFiltered;
    final GuiWaypoints parentGui;
    private String filterString = "";
    static final Text ENABLE = Text.translatable("minimap.waypoints.enable");
    static final Text DISABLE = Text.translatable("minimap.waypoints.disable");
    final Identifier visibleIconIdentifier = new Identifier("textures/mob_effect/night_vision.png");
    final Identifier invisibleIconIdentifier = new Identifier("textures/mob_effect/blindness.png");

    GuiSlotWaypoints(GuiWaypoints par1GuiWaypoints) {
        super(par1GuiWaypoints.getWidth(), par1GuiWaypoints.getHeight(), 54, par1GuiWaypoints.getHeight() - 90 + 4, 18);
        this.parentGui = par1GuiWaypoints;
        this.waypoints = new ArrayList<>();

        for (Waypoint pt : this.parentGui.waypointManager.getWaypoints()) {
            if (pt.inWorld && pt.inDimension) {
                this.waypoints.add(new WaypointItem(this.parentGui, pt));
            }
        }

        this.waypointsFiltered = new ArrayList<>(this.waypoints);
        this.waypointsFiltered.forEach(x -> this.addEntry((Entry) x));
    }

    public void setSelected(WaypointItem entry) {
        super.setSelected(entry);
        if (this.getSelectedOrNull() instanceof WaypointItem) {
            NarratorManager narratorManager = new NarratorManager(VoxelConstants.getMinecraft());
            narratorManager.narrate((Text.translatable("narrator.select", ((WaypointItem) this.getSelectedOrNull()).waypoint.name)).getString());
        }

        this.parentGui.setSelectedWaypoint(entry.waypoint);
    }

    protected boolean isSelectedEntry(int index) {
        return ((WaypointItem) this.waypointsFiltered.get(index)).waypoint.equals(this.parentGui.selectedWaypoint);
    }

    protected int getMaxPosition() {
        return this.getEntryCount() * this.itemHeight;
    }

    public void drawTexturedModalRect(int xCoord, int yCoord, Sprite textureSprite, int widthIn, int heightIn) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexbuffer.vertex(xCoord, yCoord + heightIn, 1.0).texture(textureSprite.getMinU(), textureSprite.getMaxV()).next();
        vertexbuffer.vertex(xCoord + widthIn, yCoord + heightIn, 1.0).texture(textureSprite.getMaxU(), textureSprite.getMaxV()).next();
        vertexbuffer.vertex(xCoord + widthIn, yCoord, 1.0).texture(textureSprite.getMaxU(), textureSprite.getMinV()).next();
        vertexbuffer.vertex(xCoord, yCoord, 1.0).texture(textureSprite.getMinU(), textureSprite.getMinV()).next();
        tessellator.draw();
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

        this.waypointsFiltered.forEach(x -> this.addEntry((Entry) x));
    }

    public class WaypointItem extends EntryListWidget.Entry<WaypointItem> implements Comparable<WaypointItem> {
        private final GuiWaypoints parentGui;
        private final Waypoint waypoint;

        protected WaypointItem(GuiWaypoints waypointScreen, Waypoint waypoint) {
            this.parentGui = waypointScreen;
            this.waypoint = waypoint;
        }

        public void render(DrawContext drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawContext.drawCenteredTextWithShadow(this.parentGui.getFontRenderer(), this.waypoint.name, this.parentGui.getWidth() / 2, y + 3, this.waypoint.getUnifiedColor());
            byte padding = 3;
            if (mouseX >= x - padding && mouseY >= y && mouseX <= x + 215 + padding && mouseY <= y + entryHeight) {
                Text tooltip;
                if (mouseX >= x + 215 - 16 - padding && mouseX <= x + 215 + padding) {
                    tooltip = this.waypoint.enabled ? GuiSlotWaypoints.this.DISABLE : GuiSlotWaypoints.this.ENABLE;
                } else {
                    String tooltipText = "X: " + this.waypoint.getX() + " Z: " + this.waypoint.getZ();
                    if (this.waypoint.getY() > client.world.getBottomY()) {
                        tooltipText = tooltipText + " Y: " + this.waypoint.getY();
                    }

                    tooltip = Text.literal(tooltipText);
                }

                if (mouseX >= GuiSlotWaypoints.this.left && mouseX <= GuiSlotWaypoints.this.right && mouseY >= GuiSlotWaypoints.this.top && mouseY <= GuiSlotWaypoints.this.bottom) {
                    GuiWaypoints.setTooltip(GuiSlotWaypoints.this.parentGui, tooltip);
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, this.waypoint.enabled ? GuiSlotWaypoints.this.visibleIconIdentifier : GuiSlotWaypoints.this.invisibleIconIdentifier);
            drawContext.drawTexture(this.waypoint.enabled ? GuiSlotWaypoints.this.visibleIconIdentifier : GuiSlotWaypoints.this.invisibleIconIdentifier, x + 198, y - 2, 0, 0.0F, 0.0F, 18, 18, 18, 18);
            if (this.waypoint == this.parentGui.highlightedWaypoint) {
                int x1 = x + 199;
                int y1 = y - 1;
                RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
                TextureAtlas textureAtlas = this.parentGui.waypointManager.getTextureAtlas();
                OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, textureAtlas.getGlId());
                Sprite icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png");
                GuiSlotWaypoints.this.drawTexturedModalRect(x1, y1, icon, 16, 16);
            }

        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            GuiSlotWaypoints.this.setSelected(this);
            int leftEdge = this.parentGui.getWidth() / 2 - 92 - 16;
            byte padding = 3;
            int width = 215;
            if (mouseX >= (leftEdge + width - 16 - padding) && mouseX <= (leftEdge + width + padding)) {
                if (GuiSlotWaypoints.this.doubleclick) {
                    this.parentGui.setHighlightedWaypoint();
                }

                this.parentGui.toggleWaypointVisibility();
            } else if (GuiSlotWaypoints.this.doubleclick) {
                this.parentGui.editWaypoint(this.parentGui.selectedWaypoint);
            }

            return true;
        }

        public int compareTo(WaypointItem o) {
            return this.waypoint.compareTo(o.waypoint);
        }
    }
}
