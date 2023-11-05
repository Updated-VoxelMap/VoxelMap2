package com.mamiyaotaru.voxelmap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.RealmsConnection;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget.Entry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class VoxelConstants {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");
    private static final VoxelMap VOXELMAP_INSTANCE = new VoxelMap();
    private static int elapsedTicks;

    private VoxelConstants() {}

    @NotNull
    public static MinecraftClient getMinecraft() { return MinecraftClient.getInstance(); }

    public static boolean isFabulousGraphicsOrBetter() { return MinecraftClient.isFabulousGraphicsOrBetter(); }

    public static boolean isSinglePlayer() { return getMinecraft().isInSingleplayer(); }
    public static boolean isRealmServer() {
        ClientPlayNetworkHandler playNetworkHandler = getMinecraft().getNetworkHandler();
        ServerInfo serverInfo = playNetworkHandler != null ? getMinecraft().getNetworkHandler().getServerInfo() : null;
        return serverInfo != null && serverInfo.isRealm();
    }

    @NotNull
    public static Logger getLogger() { return LOGGER; }

    @NotNull
    public static Optional<IntegratedServer> getIntegratedServer() { return Optional.ofNullable(getMinecraft().getServer()); }

    @NotNull
    public static Optional<World> getWorldByKey(RegistryKey<World> key) { return getIntegratedServer().map(integratedServer -> integratedServer.getWorld(key)); }

    @NotNull
    public static ClientWorld getClientWorld() { return getPlayer().clientWorld; }

    @NotNull
    public static ClientPlayerEntity getPlayer() {
        ClientPlayerEntity player = getMinecraft().player;

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
}