package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.persistent.ThreadManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Matrix4fStack;

public final class VoxelConstants {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");
    private static final VoxelMap VOXELMAP_INSTANCE = new VoxelMap();
    private static int elapsedTicks;
    private static final ResourceLocation OPTIONS_BACKGROUND_TEXTURE = ResourceLocation.parse("textures/block/dirt.png");
    public static final boolean DEBUG = false;
    private static boolean initialized;
    private static Events events;
    private static PacketBridge packetBridge;

    private VoxelConstants() {}

    @NotNull
    public static Minecraft getMinecraft() { return Minecraft.getInstance(); }

    public static boolean isSystemMacOS() { return Minecraft.ON_OSX; }

    public static boolean isFabulousGraphicsOrBetter() { return Minecraft.useShaderTransparency(); }

    public static boolean isSinglePlayer() { return getMinecraft().isLocalServer(); }
    public static boolean isRealmServer() {
        ClientPacketListener playNetworkHandler = getMinecraft().getConnection();
        ServerData serverInfo = playNetworkHandler != null ? getMinecraft().getConnection().getServerData() : null;
        return serverInfo != null && serverInfo.isRealm();
    }

    @NotNull
    public static Logger getLogger() { return LOGGER; }

    @NotNull
    public static Optional<IntegratedServer> getIntegratedServer() { return Optional.ofNullable(getMinecraft().getSingleplayerServer()); }

    @NotNull
    public static Optional<Level> getWorldByKey(ResourceKey<Level> key) { return getIntegratedServer().map(integratedServer -> integratedServer.getLevel(key)); }

    @NotNull
    public static ClientLevel getClientWorld() { return getPlayer().clientLevel; }

    @NotNull
    public static LocalPlayer getPlayer() {
        LocalPlayer player = getMinecraft().player;

        if (player == null) {
            String error = "Attempted to fetch player entity while not in-game!";

            getLogger().fatal(error);
            throw new IllegalStateException(error);
        }

        return player;
    }

    @NotNull
    public static VoxelMap getVoxelMapInstance() { return VOXELMAP_INSTANCE; }

    static void tick() { elapsedTicks = elapsedTicks == Integer.MAX_VALUE ? 1 : elapsedTicks + 1; }

    public static int getElapsedTicks() { return elapsedTicks; }

    static { elapsedTicks = 0; }

    public static ResourceLocation getOptionsBackgroundTexture() {
        return OPTIONS_BACKGROUND_TEXTURE;
    }

    public static void lateInit() {
        initialized = true;
        VoxelConstants.getVoxelMapInstance().lateInit(true, false);
    }

    public static void clientTick() {
        if (!initialized) {
            boolean OK = VoxelConstants.getMinecraft().getResourceManager() != null && VoxelConstants.getMinecraft().getTextureManager() != null;

            if (OK) {
                lateInit();
            }
        }

        if (initialized) {
            VoxelConstants.getVoxelMapInstance().onTick();
        }

    }

    public static void renderOverlay(GuiGraphics drawContext) {
        if (!initialized) {
            lateInit();
        }

        try {
            VoxelConstants.getVoxelMapInstance().onTickInGame(drawContext);
        } catch (RuntimeException e) {
            VoxelConstants.getLogger().log(org.apache.logging.log4j.Level.ERROR, "Error while render overlay", e);
        }
    }

    public static boolean onChat(Component chat, GuiMessageTag indicator) {
        return CommandUtils.checkForWaypoints(chat, indicator);
    }

    public static boolean onSendChatMessage(String message) {
        if (message.startsWith("newWaypoint")) {
            CommandUtils.waypointClicked(message);
            return false;
        } else if (message.startsWith("ztp")) {
            CommandUtils.teleport(message);
            return false;
        } else {
            return true;
        }
    }

    public static void onRenderHand(float partialTicks, Matrix4fStack matrixStack, boolean beacons, boolean signs, boolean withDepth, boolean withoutDepth) {
        try {
            VoxelConstants.getVoxelMapInstance().getWaypointManager().renderWaypoints(partialTicks, matrixStack, beacons, signs, withDepth, withoutDepth);
        } catch (RuntimeException e) {
            VoxelConstants.getLogger().log(org.apache.logging.log4j.Level.ERROR, "Error while render waypoints", e);
        }

    }

    public static void onShutDown() {
        VoxelConstants.getLogger().info("Saving all world maps");
        VoxelConstants.getVoxelMapInstance().getPersistentMap().purgeCachedRegions();
        VoxelConstants.getVoxelMapInstance().getMapOptions().saveAll();
        BiomeRepository.saveBiomeColors();
        long shutdownTime = System.currentTimeMillis();

        while (ThreadManager.executorService.getQueue().size() + ThreadManager.executorService.getActiveCount() > 0 && System.currentTimeMillis() - shutdownTime < 10000L) {
            Thread.onSpinWait();
        }
    }

    public static void playerRunTeleportCommand(double x, double y, double z) {
        MapSettingsManager mapSettingsManager = VoxelConstants.getVoxelMapInstance().getMapOptions();
        String cmd = mapSettingsManager.serverTeleportCommand == null ? mapSettingsManager.teleportCommand : mapSettingsManager.serverTeleportCommand;
        cmd = cmd.replace("%p", VoxelConstants.getPlayer().getName().getString()).replace("%x", String.valueOf(x + 0.5)).replace("%y", String.valueOf(y)).replace("%z", String.valueOf(z + 0.5));
        VoxelConstants.getPlayer().connection.sendUnsignedCommand(cmd);
    }

    public static int moveScoreboard(int bottomX, int entriesHeight) {
        double unscaledHeight = Map.getMinTablistOffset(); // / scaleFactor;
        if (VoxelMap.mapOptions.hide || !VoxelMap.mapOptions.minimapAllowed || VoxelMap.mapOptions.mapCorner != 1 || !VoxelMap.mapOptions.moveScoreBoardDown || !Double.isFinite(unscaledHeight)) {
            return bottomX;
        }
        double scaleFactor = Minecraft.getInstance().getWindow().getGuiScale(); // 1x 2x 3x, ...
        double mapHeightScaled = unscaledHeight * 1.37 / scaleFactor; // * 1.37 because unscaledHeight is just the map without the text around it

        int fontHeight = Minecraft.getInstance().font.lineHeight; // height of the title line
        float statusIconOffset = Map.getStatusIconOffset();
        int statusIconOffsetInt = Float.isFinite(statusIconOffset) ? (int) statusIconOffset : 0;
        int minBottom = (int) (mapHeightScaled + entriesHeight + fontHeight + statusIconOffsetInt);

        return Math.max(bottomX, minBottom);
    }

    public static void setEvents(Events events) {
        VoxelConstants.events = events;
    }

    public static Events getEvents() {
        return events;
    }

    public static PacketBridge getPacketBridge() {
        return packetBridge;
    }

    public static void setPacketBridge(PacketBridge packetBridge) {
        VoxelConstants.packetBridge = packetBridge;
    }
}