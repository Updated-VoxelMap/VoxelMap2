package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.textures.IIconCreator;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BackgroundImageInfo;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mamiyaotaru.voxelmap.util.WaypointContainer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class WaypointManager {
    public final MapSettingsManager options;
    final TextureAtlas textureAtlas;
    final TextureAtlas textureAtlasChooser;
    private boolean loaded;
    private boolean needSave;
    private ArrayList<Waypoint> wayPts = new ArrayList<>();
    private Waypoint highlightedWaypoint;
    private String worldName = "";
    private String currentSubWorldName = "";
    private String currentSubworldDescriptor = "";
    private String currentSubworldDescriptorNoCodes = "";
    private boolean multiworld;
    private boolean gotAutoSubworldName;
    private DimensionContainer currentDimension;
    private final TreeSet<String> knownSubworldNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final HashSet<String> oldNorthWorldNames = new HashSet<>();
    private final HashMap<String, String> worldSeeds = new HashMap<>();
    private BackgroundImageInfo backgroundImageInfo;
    private WaypointContainer waypointContainer;
    private File settingsFile;
    private Long lastNewWorldNameTime = 0L;
    private final Object waypointLock = new Object();

    public WaypointManager() {
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.textureAtlas = new TextureAtlas("waypoints");
        this.textureAtlas.setFilter(false, false);
        this.textureAtlasChooser = new TextureAtlas("chooser");
        this.textureAtlasChooser.setFilter(false, false);
        this.waypointContainer = new WaypointContainer(this.options);
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        List<Identifier> images = new ArrayList<>();
        IIconCreator iconCreator = textureAtlas -> {

            Map<Identifier, Resource> resourceMap = VoxelConstants.getMinecraft().getResourceManager().findResources("images", asset -> asset.getPath().endsWith(".png"));
            for (Identifier candidate : resourceMap.keySet()) { //TODO 1.19
                if (candidate.getNamespace().equals("voxelmap") && candidate.getPath().contains("images/waypoints")) {
                    images.add(candidate);
                }
            }

            Sprite markerIcon = textureAtlas.registerIconForResource(new Identifier("voxelmap", "images/waypoints/marker.png"), VoxelConstants.getMinecraft().getResourceManager());
            Sprite markerIconSmall = textureAtlas.registerIconForResource(new Identifier("voxelmap", "images/waypoints/markersmall.png"), VoxelConstants.getMinecraft().getResourceManager());

            for (Identifier resourceLocation : images) {
                Sprite icon = textureAtlas.registerIconForResource(resourceLocation, VoxelConstants.getMinecraft().getResourceManager());
                String name = resourceLocation.toString();
                if (name.toLowerCase().contains("waypoints/waypoint") && !name.toLowerCase().contains("small")) {
                    textureAtlas.registerMaskedIcon(name.replace(".png", "Small.png"), icon);
                    textureAtlas.registerMaskedIcon(name.replace("waypoints/waypoint", "waypoints/marker"), markerIcon);
                    textureAtlas.registerMaskedIcon(name.replace("waypoints/waypoint", "waypoints/marker").replace(".png", "Small.png"), markerIconSmall);
                } else if (name.toLowerCase().contains("waypoints/marker") && !name.toLowerCase().contains("small")) {
                    textureAtlas.registerMaskedIcon(name.replace(".png", "Small.png"), icon);
                }
            }

        };
        this.textureAtlas.loadTextureAtlas(iconCreator);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        this.textureAtlasChooser.reset();
        int expectedSize = 32;

        for (Identifier resourceLocation : images) {
            String name = resourceLocation.toString();
            if (name.toLowerCase().contains("waypoints/waypoint") && !name.toLowerCase().contains("small")) {
                try {
                    Optional<Resource> imageResource = resourceManager.getResource(resourceLocation);
                    BufferedImage bufferedImage = ImageIO.read(imageResource.get().getInputStream());
                    imageResource.get().getReader().close();
                    float scale = expectedSize / (float) bufferedImage.getWidth();
                    bufferedImage = ImageUtils.scaleImage(bufferedImage, scale);
                    this.textureAtlasChooser.registerIconForBufferedImage(name, bufferedImage);
                } catch (IOException var11) {
                    this.textureAtlasChooser.registerIconForResource(resourceLocation, VoxelConstants.getMinecraft().getResourceManager());
                }
            }
        }

        this.textureAtlasChooser.stitch();
    }

    public TextureAtlas getTextureAtlas() {
        return this.textureAtlas;
    }

    public TextureAtlas getTextureAtlasChooser() {
        return this.textureAtlasChooser;
    }

    public ArrayList<Waypoint> getWaypoints() {
        return this.wayPts;
    }

    public void newWorld(World world) {
        if (world == null) {
            this.currentDimension = null;
        } else {
            String mapName;
            if (VoxelConstants.getMinecraft().isIntegratedServerRunning()) {
                mapName = this.getMapName();
            } else {
                mapName = this.getServerName();
                if (mapName != null) {
                    mapName = mapName.toLowerCase();
                }
            }

            if (!this.worldName.equals(mapName) && mapName != null && !mapName.isEmpty()) {
                this.currentDimension = null;
                this.worldName = mapName;
                VoxelConstants.getVoxelMapInstance().getDimensionManager().populateDimensions(world);
                this.loadWaypoints();
            }

            VoxelConstants.getVoxelMapInstance().getDimensionManager().enteredWorld(world);
            DimensionContainer dim = VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(world);
            this.enteredDimension(dim);
            this.setSubWorldDescriptor("");
        }

    }

    public String getMapName() {
        Optional<IntegratedServer> integratedServer = VoxelConstants.getIntegratedServer();

        if (integratedServer.isEmpty()) {
            String error = "Tried fetching map name on a non-integrated server!";

            VoxelConstants.getLogger().fatal(error);
            throw new IllegalStateException(error);
        }

        return integratedServer.get().getSavePath(WorldSavePath.ROOT).normalize().toFile().getName();
    }

    public String getServerName() {
        String serverName = "";

        try {
            ServerInfo serverData = VoxelConstants.getMinecraft().getCurrentServerEntry();
            if (serverData != null) {
                boolean isOnLAN = serverData.isLocal();
                if (isOnLAN) {
                    VoxelConstants.getLogger().warn("LAN server detected!");
                    serverName = serverData.name;
                } else {
                    serverName = serverData.address;
                }
            } else if (VoxelConstants.isRealmServer()) {
                Session session = VoxelConstants.getMinecraft().getSession();
                serverName = session.getSessionId();
                VoxelConstants.getLogger().info(serverName);
            } else {
                ClientPlayNetworkHandler netHandler = VoxelConstants.getMinecraft().getNetworkHandler();
                ClientConnection networkManager = netHandler.getConnection();
                InetSocketAddress socketAddress = (InetSocketAddress) networkManager.getAddress();
                serverName = socketAddress.getHostString() + ":" + socketAddress.getPort();
            }
        } catch (Exception var6) {
            VoxelConstants.getLogger().error("error getting ServerData", var6);
        }

        return serverName;
    }

    public String getCurrentWorldName() {
        return this.worldName;
    }

    public void handleDeath() {
        HashSet<Waypoint> toDel = new HashSet<>();

        for (Waypoint pt : this.wayPts) {
            if (pt.name.equals("Latest Death")) {
                pt.name = "Previous Death";
            }

            if (pt.name.startsWith("Previous Death")) {
                if (this.options.deathpoints == 2) {
                    int num = 0;

                    try {
                        if (pt.name.length() > 15) {
                            num = Integer.parseInt(pt.name.substring(15));
                        }
                    } catch (NumberFormatException ignored) {}

                    pt.red -= (pt.red - 0.5F) / 8.0F;
                    pt.green -= (pt.green - 0.5F) / 8.0F;
                    pt.blue -= (pt.blue - 0.5F) / 8.0F;
                    pt.name = "Previous Death " + (num + 1);
                } else {
                    toDel.add(pt);
                }
            }
        }

        if (this.options.deathpoints != 2 && (!(toDel.isEmpty()))) {
            for (Waypoint pt : toDel) {
                this.deleteWaypoint(pt);
            }
        }

        if (this.options.deathpoints != 0) {
            TreeSet<DimensionContainer> dimensions = new TreeSet<>();
            dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().getWorld()));
            double dimensionScale = VoxelConstants.getPlayer().getWorld().getDimension().coordinateScale();
            this.addWaypoint(new Waypoint("Latest Death", (int) (GameVariableAccessShim.xCoord() * dimensionScale), (int) (GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord() - 1, true, 1.0F, 1.0F, 1.0F, "Skull", this.getCurrentSubworldDescriptor(false), dimensions));
        }

    }

    private void enteredDimension(DimensionContainer dimension) {
        this.highlightedWaypoint = null;
        if (dimension == this.currentDimension) {
            this.multiworld = true;
        }

        this.currentDimension = dimension;
        synchronized (this.waypointLock) {
            this.waypointContainer = new WaypointContainer(this.options);

            for (Waypoint pt : this.wayPts) {
                pt.inDimension = pt.dimensions.isEmpty() || pt.dimensions.contains(dimension);

                this.waypointContainer.addWaypoint(pt);
            }

            this.waypointContainer.setHighlightedWaypoint(this.highlightedWaypoint);
        }

        this.loadBackgroundMapImage();
    }

    public void setOldNorth(boolean oldNorth) {
        String oldNorthWorldName;
        if (this.knownSubworldNames.isEmpty()) {
            oldNorthWorldName = "all";
        } else {
            oldNorthWorldName = this.getCurrentSubworldDescriptor(false);
        }

        if (oldNorth) {
            this.oldNorthWorldNames.add(oldNorthWorldName);
        } else {
            this.oldNorthWorldNames.remove(oldNorthWorldName);
        }

        this.saveWaypoints();
    }

    public TreeSet<String> getKnownSubworldNames() {
        return this.knownSubworldNames;
    }

    public boolean receivedAutoSubworldName() {
        return this.gotAutoSubworldName;
    }

    public boolean isMultiworld() {
        return this.multiworld || VoxelConstants.isRealmServer();
    }

    public synchronized void setSubworldName(String name, boolean fromServer) {
        boolean notNull = !name.isEmpty();
        if (notNull || System.currentTimeMillis() - this.lastNewWorldNameTime > 2000L) {
            if (notNull) {
                if (fromServer) {
                    this.gotAutoSubworldName = true;
                }

                if (!name.equals(this.currentSubWorldName)) {
                    VoxelConstants.getLogger().info("New world name: " + TextUtils.scrubCodes(name));
                }

                this.lastNewWorldNameTime = System.currentTimeMillis();
            }

            this.currentSubWorldName = name;
            this.setSubWorldDescriptor(this.currentSubWorldName);
        }

    }

    private void setSubWorldDescriptor(String descriptor) {
        boolean serverSaysOldNorth = false;
        if (descriptor.endsWith("§o§n")) {
            descriptor = descriptor.substring(0, descriptor.length() - 4);
            serverSaysOldNorth = true;
        }

        this.currentSubworldDescriptor = descriptor;
        this.currentSubworldDescriptorNoCodes = TextUtils.scrubCodes(this.currentSubworldDescriptor);
        this.newSubworldName(this.currentSubworldDescriptorNoCodes);
        String currentSubWorldDescriptorScrubbed = TextUtils.scrubName(this.currentSubworldDescriptorNoCodes);
        synchronized (this.waypointLock) {
            for (Waypoint pt : this.wayPts) {
                pt.inWorld = currentSubWorldDescriptorScrubbed.isEmpty() || Objects.equals(pt.world, "") || currentSubWorldDescriptorScrubbed.equals(pt.world);
            }
        }

        if (serverSaysOldNorth) {
            if (this.currentSubworldDescriptorNoCodes.isEmpty()) {
                this.oldNorthWorldNames.add("all");
            } else {
                this.oldNorthWorldNames.add(this.currentSubworldDescriptorNoCodes);
            }
        }

        VoxelConstants.getVoxelMapInstance().getMapOptions().oldNorth = this.oldNorthWorldNames.contains(this.currentSubworldDescriptorNoCodes);
    }

    private void newSubworldName(String name) {
        if (name != null && !name.isEmpty()) {
            this.multiworld = true;
            if (this.knownSubworldNames.add(name)) {
                if (this.loaded) {
                    this.saveWaypoints();
                } else {
                    this.needSave = true;
                }
            }

            this.loadBackgroundMapImage();
        }
    }

    public void changeSubworldName(String oldName, String newName) {
        if (!newName.equals(oldName) && this.knownSubworldNames.remove(oldName)) {
            this.knownSubworldNames.add(newName);
            synchronized (this.waypointLock) {
                for (Waypoint pt : this.wayPts) {
                    if (pt.world.equals(oldName)) {
                        pt.world = newName;
                    }
                }
            }

            VoxelConstants.getVoxelMapInstance().getPersistentMap().renameSubworld(oldName, newName);
            String worldName = this.getCurrentWorldName();
            String worldNamePathPart = TextUtils.scrubNameFile(worldName);
            String subWorldNamePathPart = TextUtils.scrubNameFile(oldName) + "/";
            File oldCachedRegionFileDir = new File(VoxelConstants.getMinecraft().runDirectory, "/mods/mamiyaotaru/voxelmap/cache/" + worldNamePathPart + "/" + subWorldNamePathPart);
            if (oldCachedRegionFileDir.exists() && oldCachedRegionFileDir.isDirectory()) {
                subWorldNamePathPart = TextUtils.scrubNameFile(newName) + "/";
                File newCachedRegionFileDir = new File(VoxelConstants.getMinecraft().runDirectory, "/mods/mamiyaotaru/voxelmap/cache/" + worldNamePathPart + "/" + subWorldNamePathPart);
                boolean success = oldCachedRegionFileDir.renameTo(newCachedRegionFileDir);
                if (!success) {
                    VoxelConstants.getLogger().warn("Failed renaming " + oldCachedRegionFileDir.getPath() + " to " + newCachedRegionFileDir.getPath());
                }
            }

            if (oldName.equals(this.getCurrentSubworldDescriptor(false))) {
                this.setSubworldName(newName, false);
            }

            this.saveWaypoints();
        }

    }

    public void deleteSubworld(String name) {
        if (this.knownSubworldNames.remove(name)) {
            synchronized (this.waypointLock) {
                for (Waypoint pt : this.wayPts) {
                    if (pt.world.equals(name)) {
                        pt.world = "";
                        pt.inWorld = true;
                    }
                }
            }

            this.saveWaypoints();
            this.lastNewWorldNameTime = 0L;
            this.setSubworldName("", false);
        }

        if (this.knownSubworldNames.isEmpty()) {
            this.multiworld = false;
        }

    }

    public String getCurrentSubworldDescriptor(boolean withCodes) {
        return withCodes ? this.currentSubworldDescriptor : this.currentSubworldDescriptorNoCodes;
    }

    public String getWorldSeed() {
        String key = "all";
        if (!this.knownSubworldNames.isEmpty()) {
            key = this.getCurrentSubworldDescriptor(false);
        }

        String seed = this.worldSeeds.get(key);
        if (seed == null) {
            seed = "";
        }

        return seed;
    }

    public void setWorldSeed(String newSeed) {
        String worldName = "all";
        if (!this.knownSubworldNames.isEmpty()) {
            worldName = this.getCurrentSubworldDescriptor(false);
        }

        this.worldSeeds.put(worldName, newSeed);
        this.saveWaypoints();
    }

    public void saveWaypoints() {
        String worldNameSave = this.getCurrentWorldName();
        if (worldNameSave.endsWith(":25565")) {
            int portSepLoc = worldNameSave.lastIndexOf(':');
            if (portSepLoc != -1) {
                worldNameSave = worldNameSave.substring(0, portSepLoc);
            }
        }

        worldNameSave = TextUtils.scrubNameFile(worldNameSave);
        File saveDir = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/");
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        this.settingsFile = new File(saveDir, worldNameSave + ".points");

        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.settingsFile), StandardCharsets.UTF_8));
            String knownSubworldsString = this.knownSubworldNames.stream().map(subworldName -> TextUtils.scrubName(subworldName) + ",").collect(Collectors.joining());

            out.println("subworlds:" + knownSubworldsString);
            String oldNorthWorldsString = this.oldNorthWorldNames.stream().map(oldNorthWorldName -> TextUtils.scrubName(oldNorthWorldName) + ",").collect(Collectors.joining());

            out.println("oldNorthWorlds:" + oldNorthWorldsString);
            String seedsString = this.worldSeeds.entrySet().stream().map(entry -> TextUtils.scrubName(entry.getKey()) + "#" + entry.getValue() + ",").collect(Collectors.joining());

            out.println("seeds:" + seedsString);

            for (Waypoint pt : this.wayPts) {
                if (!(!pt.name.isEmpty() && pt.name.charAt(0) == '^')) {
                    StringBuilder dimensionsString = new StringBuilder();

                    for (DimensionContainer dimension : pt.dimensions) {
                        dimensionsString.append(dimension.getStorageName()).append("#");
                    }

                    if (dimensionsString.toString().isEmpty()) {
                        dimensionsString.append(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByResourceLocation(DimensionTypes.OVERWORLD.getValue()).getStorageName());
                    }

                    out.println("name:" + TextUtils.scrubName(pt.name) + ",x:" + pt.x + ",z:" + pt.z + ",y:" + pt.y + ",enabled:" + pt.enabled + ",red:" + pt.red + ",green:" + pt.green + ",blue:" + pt.blue + ",suffix:" + pt.imageSuffix + ",world:" + TextUtils.scrubName(pt.world) + ",dimensions:" + dimensionsString);
                }
            }

            out.close();
        } catch (FileNotFoundException var12) {
            MessageUtils.chatInfo("§EError Saving Waypoints");
            VoxelConstants.getLogger().error(var12);
        }

    }

    private void loadWaypoints() {
        this.loaded = false;
        this.multiworld = false;
        this.gotAutoSubworldName = false;
        this.currentDimension = null;
        this.setSubWorldDescriptor("");
        this.knownSubworldNames.clear();
        this.oldNorthWorldNames.clear();
        this.worldSeeds.clear();
        synchronized (this.waypointLock) {
            boolean loaded;
            this.wayPts = new ArrayList<>();
            String worldNameStandard = this.getCurrentWorldName();
            if (worldNameStandard.endsWith(":25565")) {
                int portSepLoc = worldNameStandard.lastIndexOf(':');
                if (portSepLoc != -1) {
                    worldNameStandard = worldNameStandard.substring(0, portSepLoc);
                }
            }

            worldNameStandard = TextUtils.scrubNameFile(worldNameStandard);
            loaded = this.loadWaypointsExtensible(worldNameStandard);
            if (!loaded) {
                MessageUtils.chatInfo("§ENo waypoints exist for this world/server.");
            }
        }

        this.loaded = true;
        if (this.needSave) {
            this.needSave = false;
            this.saveWaypoints();
        }

        this.multiworld = this.multiworld || !this.knownSubworldNames.isEmpty();
    }

    private boolean loadWaypointsExtensible(String worldNameStandard) {
        File settingsFileNew = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/" + worldNameStandard + ".points");
        File settingsFileOld = new File(VoxelConstants.getMinecraft().runDirectory, "/mods/mamiyaotaru/voxelmap/" + worldNameStandard + ".points");
        if (!settingsFileOld.exists() && !settingsFileNew.exists()) {
            return false;
        } else {
            if (!settingsFileOld.exists()) {
                this.settingsFile = settingsFileNew;
            } else if (!settingsFileNew.exists()) {
                this.settingsFile = settingsFileOld;
            } else {
                this.settingsFile = settingsFileNew;
            }

            if (this.settingsFile.exists()) {
                try {
                    Properties properties = new Properties();
                    FileReader fr = new FileReader(this.settingsFile);
                    properties.load(fr);
                    String subWorldsS = properties.getProperty("subworlds", "");
                    String[] subWorlds = subWorldsS.split(",");

                    for (String subWorld : subWorlds) {
                        if (!subWorld.isEmpty()) {
                            this.knownSubworldNames.add(TextUtils.descrubName(subWorld));
                        }
                    }

                    String oldNorthWorldsS = properties.getProperty("oldNorthWorlds", "");
                    String[] oldNorthWorlds = oldNorthWorldsS.split(",");

                    for (String oldNorthWorld : oldNorthWorlds) {
                        if (!oldNorthWorld.isEmpty()) {
                            this.oldNorthWorldNames.add(TextUtils.descrubName(oldNorthWorld));
                        }
                    }

                    String worldSeedsS = properties.getProperty("seeds", "");
                    String[] worldSeedPairs = worldSeedsS.split(",");

                    for (String pair : worldSeedPairs) {
                        String[] worldSeedPair = pair.split("#");
                        if (worldSeedPair.length == 2) {
                            this.worldSeeds.put(worldSeedPair[0], worldSeedPair[1]);
                        }
                    }

                    fr.close();
                } catch (IOException exception) {
                    VoxelConstants.getLogger().error(exception);
                }

                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFile), StandardCharsets.UTF_8));

                    String sCurrentLine;
                    while ((sCurrentLine = in.readLine()) != null) {
                        try {
                            String[] pairs = sCurrentLine.split(",");
                            if (pairs.length > 1) {
                                String name = "";
                                int x = 0;
                                int z = 0;
                                int y = -1;
                                boolean enabled = false;
                                float red = 0.5F;
                                float green = 0.0F;
                                float blue = 0.0F;
                                String suffix = "";
                                String world = "";
                                TreeSet<DimensionContainer> dimensions = new TreeSet<>();

                                for (String pair : pairs) {
                                    int splitIndex = pair.indexOf(':');
                                    if (splitIndex != -1) {
                                        String key = pair.substring(0, splitIndex).toLowerCase().trim();
                                        String value = pair.substring(splitIndex + 1).trim();
                                        switch (key) {
                                            case "name" -> name = TextUtils.descrubName(value);
                                            case "x" -> x = Integer.parseInt(value);
                                            case "z" -> z = Integer.parseInt(value);
                                            case "y" -> y = Integer.parseInt(value);
                                            case "enabled" -> enabled = Boolean.parseBoolean(value);
                                            case "red" -> red = Float.parseFloat(value);
                                            case "green" -> green = Float.parseFloat(value);
                                            case "blue" -> blue = Float.parseFloat(value);
                                            case "suffix" -> suffix = value;
                                            case "world" -> world = TextUtils.descrubName(value);
                                            case "dimensions" -> {
                                                String[] dimensionStrings = value.split("#");
                                                for (String dimensionString : dimensionStrings) {
                                                    String convertOldFormat = dimensionString.equals("1") ? "the_end" : dimensionString.equals("-1") ? "the_nether" : dimensionString.equals("0") ? "overworld" : dimensionString;
                                                    dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByIdentifier(convertOldFormat));
                                                }
                                                if (dimensions.isEmpty()) {
                                                    dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByResourceLocation(DimensionTypes.OVERWORLD.getValue()));
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!name.isEmpty()) {
                                    this.loadWaypoint(name, x, z, y, enabled, red, green, blue, suffix, world, dimensions);
                                    if (!world.isEmpty()) {
                                        this.knownSubworldNames.add(TextUtils.descrubName(world));
                                    }
                                }
                            }
                        } catch (Exception exception) {
                            VoxelConstants.getLogger().error(exception);
                        }
                    }

                    in.close();
                    return true;
                } catch (IOException var25) {
                    MessageUtils.chatInfo("§EError Loading Waypoints");
                    VoxelConstants.getLogger().error("waypoint load error: " + var25.getLocalizedMessage(), var25);
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    private void loadWaypoint(String name, int x, int z, int y, boolean enabled, float red, float green, float blue, String suffix, String world, TreeSet<DimensionContainer> dimensions) {
        Waypoint newWaypoint = new Waypoint(name, x, z, y, enabled, red, green, blue, suffix, world, dimensions);
        if (!this.wayPts.contains(newWaypoint)) {
            this.wayPts.add(newWaypoint);
        }

    }

    public void deleteWaypoint(Waypoint point) {
        this.waypointContainer.removeWaypoint(point);
        this.wayPts.remove(point);
        this.saveWaypoints();
        if (point == this.highlightedWaypoint) {
            this.setHighlightedWaypoint(null, false);
        }

    }

    public void addWaypoint(Waypoint newWaypoint) {
        this.wayPts.add(newWaypoint);
        this.waypointContainer.addWaypoint(newWaypoint);
        this.saveWaypoints();
        if (this.highlightedWaypoint != null && this.highlightedWaypoint.getX() == newWaypoint.getX() && this.highlightedWaypoint.getZ() == newWaypoint.getZ()) {
            this.setHighlightedWaypoint(newWaypoint, false);
        }

    }

    public void setHighlightedWaypoint(Waypoint waypoint, boolean toggle) {
        if (toggle && waypoint == this.highlightedWaypoint) {
            this.highlightedWaypoint = null;
        } else {
            if (waypoint != null && !this.wayPts.contains(waypoint)) {
                waypoint.red = 2.0F;
                waypoint.blue = 0.0F;
                waypoint.green = 0.0F;
            }

            this.highlightedWaypoint = waypoint;
        }

        this.waypointContainer.setHighlightedWaypoint(this.highlightedWaypoint);
    }

    public Waypoint getHighlightedWaypoint() {
        return this.highlightedWaypoint;
    }

    public void renderWaypoints(float partialTicks, MatrixStack matrixStack, boolean beacons, boolean signs, boolean withDepth, boolean withoutDepth) {
        if (this.waypointContainer != null) {
            this.waypointContainer.renderWaypoints(partialTicks, matrixStack, beacons, signs, withDepth, withoutDepth);
        }

    }

    private void loadBackgroundMapImage() {
        if (this.backgroundImageInfo != null) {
            RenderSystem.deleteTexture(this.backgroundImageInfo.glid);
            this.backgroundImageInfo = null;
        }

        try {
            String path = this.getCurrentWorldName();
            String subworldDescriptor = this.getCurrentSubworldDescriptor(false);
            if (subworldDescriptor != null && !subworldDescriptor.isEmpty()) {
                path = path + "/" + subworldDescriptor;
            }

            path = path + "/" + this.currentDimension.getStorageName();
            String tempPath = "images/backgroundmaps/" + path + "/map.png";
            Identifier identifier = new Identifier("voxelmap", tempPath);
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(identifier).get().getInputStream();
            Image image = ImageIO.read(is);
            is.close();
            BufferedImage mapImage = new BufferedImage(image.getWidth(null), image.getHeight(null), 2);
            Graphics gfx = mapImage.createGraphics();
            gfx.drawImage(image, 0, 0, null);
            gfx.dispose();
            is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier("voxelmap", "images/backgroundmaps/" + path + "/map.txt")).get().getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            Properties mapProperties = new Properties();
            mapProperties.load(isr);
            String left = mapProperties.getProperty("left");
            String right = mapProperties.getProperty("right");
            String top = mapProperties.getProperty("top");
            String bottom = mapProperties.getProperty("bottom");
            String width = mapProperties.getProperty("width");
            String height = mapProperties.getProperty("height");
            String scale = mapProperties.getProperty("scale");
            if (left != null && top != null && width != null && height != null) {
                this.backgroundImageInfo = new BackgroundImageInfo(mapImage, Integer.parseInt(left), Integer.parseInt(top), Integer.parseInt(width), Integer.parseInt(height));
            } else if (left != null && top != null && scale != null) {
                this.backgroundImageInfo = new BackgroundImageInfo(mapImage, Integer.parseInt(left), Integer.parseInt(top), Float.parseFloat(scale));
            } else if (left != null && top != null && right != null && bottom != null) {
                int widthInt = Integer.parseInt(right) - Integer.parseInt(left);
                this.backgroundImageInfo = new BackgroundImageInfo(mapImage, Integer.parseInt(left), Integer.parseInt(top), widthInt, widthInt);
            }

            isr.close();
        } catch (Exception ignore) {}
    }

    public BackgroundImageInfo getBackgroundImageInfo() {
        return this.backgroundImageInfo;
    }
}
