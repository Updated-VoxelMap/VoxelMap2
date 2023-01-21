package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.FullMapData;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.MapChunkCache;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.MutableNativeImageBackedTexture;
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.ScaledMutableNativeImageBackedTexture;
import com.mamiyaotaru.voxelmap.util.TickCounter;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.Material;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.OutOfMemoryScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

public class Map implements Runnable, IChangeObserver {
    private final float[] lastLightBrightnessTable = new float[16];
    private final Object coordinateLock = new Object();
    private final Identifier arrowResourceLocation = new Identifier("voxelmap", "images/mmarrow.png");
    private final Identifier roundmapResourceLocation = new Identifier("voxelmap", "images/roundmap.png");
    private final Identifier squareStencil = new Identifier("voxelmap", "images/square.png");
    private final Identifier circleStencil = new Identifier("voxelmap", "images/circle.png");
    private final VoxelMap master;
    private ClientWorld world = null;
    private final MapSettingsManager options;
    private final LayoutVariables layoutVariables;
    private final ColorManager colorManager;
    private final WaypointManager waypointManager;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final boolean multicore = this.availableProcessors > 1;
    private final int heightMapResetHeight = this.multicore ? 2 : 5;
    private final int heightMapResetTime = this.multicore ? 300 : 3000;
    private final boolean threading = this.multicore;
    private final FullMapData[] mapData = new FullMapData[5];
    private final MapChunkCache[] chunkCache = new MapChunkCache[5];
    private MutableNativeImageBackedTexture[] mapImages;
    private final MutableNativeImageBackedTexture[] mapImagesFiltered = new MutableNativeImageBackedTexture[5];
    private final MutableNativeImageBackedTexture[] mapImagesUnfiltered = new MutableNativeImageBackedTexture[5];
    private MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    private final MutableBlockPos tempBlockPos = new MutableBlockPos(0, 0, 0);
    private BlockState transparentBlockState;
    private BlockState surfaceBlockState;
    private boolean imageChanged = true;
    private NativeImageBackedTexture lightmapTexture = null;
    private boolean needLightmapRefresh = true;
    private int tickWithLightChange = 0;
    private boolean lastPaused = true;
    private double lastGamma = 0.0;
    private float lastSunBrightness = 0.0F;
    private float lastLightning = 0.0F;
    private float lastPotion = 0.0F;
    private final int[] lastLightmapValues = new int[]{-16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216};
    private boolean lastBeneathRendering = false;
    private boolean needSkyColor = false;
    private boolean lastAboveHorizon = true;
    private int lastBiome = 0;
    private int lastSkyColor = 0;
    private final Random generator = new Random();
    private boolean showWelcomeScreen;
    private Screen lastGuiScreen = null;
    private boolean fullscreenMap = false;
    private int zoom;
    private int scWidth;
    private int scHeight;
    private String error = "";
    private final Text[] welcomeText = new Text[8];
    private int ztimer = 0;
    private int heightMapFudge = 0;
    private int timer = 0;
    private boolean doFullRender = true;
    private boolean zoomChanged;
    private int lastX = 0;
    private int lastZ = 0;
    private int lastY = 0;
    private int lastImageX = 0;
    private int lastImageZ = 0;
    private boolean lastFullscreen = false;
    private float direction = 0.0F;
    private float percentX;
    private float percentY;
    private int northRotate = 0;
    private Thread zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
    private int zCalcTicker = 0;
    private final TextRenderer fontRenderer;
    private final int[] lightmapColors = new int[256];
    private double zoomScale = 1.0;
    private double zoomScaleAdjusted = 1.0;
    private int mapImageInt = -1;

    public Map(VoxelMap master) {
        this.master = master;
        this.options = master.getMapOptions();
        this.colorManager = master.getColorManager();
        this.waypointManager = master.getWaypointManager();
        this.layoutVariables = new LayoutVariables();
        ArrayList<KeyBinding> tempBindings = new ArrayList<>();
        tempBindings.addAll(Arrays.asList(VoxelConstants.getMinecraft().options.allKeys));
        tempBindings.addAll(Arrays.asList(this.options.keyBindings));
        Field f = ReflectionUtils.getFieldByType(VoxelConstants.getMinecraft().options, GameOptions.class, KeyBinding[].class, 1);

        try {
            f.set(VoxelConstants.getMinecraft().options, tempBindings.toArray(new KeyBinding[0]));
        } catch (IllegalArgumentException | IllegalAccessException var7) {
            VoxelConstants.getLogger().error(var7);
        }

        java.util.Map<String, Integer> categoryOrder = (java.util.Map<String, Integer>) ReflectionUtils.getPrivateFieldValueByType(null, KeyBinding.class, java.util.Map.class, 2);
        VoxelConstants.getLogger().warn("CATEGORY ORDER IS " + categoryOrder.size());
        Integer categoryPlace = categoryOrder.get("controls.minimap.title");
        if (categoryPlace == null) {
            int currentSize = categoryOrder.size();
            categoryOrder.put("controls.minimap.title", currentSize + 1);
        }

        this.showWelcomeScreen = this.options.welcome;
        this.zCalc.start();
        this.zCalc.setPriority(5);
        this.mapData[0] = new FullMapData(32, 32);
        this.mapData[1] = new FullMapData(64, 64);
        this.mapData[2] = new FullMapData(128, 128);
        this.mapData[3] = new FullMapData(256, 256);
        this.mapData[4] = new FullMapData(512, 512);
        this.chunkCache[0] = new MapChunkCache(3, 3, this);
        this.chunkCache[1] = new MapChunkCache(5, 5, this);
        this.chunkCache[2] = new MapChunkCache(9, 9, this);
        this.chunkCache[3] = new MapChunkCache(17, 17, this);
        this.chunkCache[4] = new MapChunkCache(33, 33, this);
        this.mapImagesFiltered[0] = new MutableNativeImageBackedTexture(32, 32, true);
        this.mapImagesFiltered[1] = new MutableNativeImageBackedTexture(64, 64, true);
        this.mapImagesFiltered[2] = new MutableNativeImageBackedTexture(128, 128, true);
        this.mapImagesFiltered[3] = new MutableNativeImageBackedTexture(256, 256, true);
        this.mapImagesFiltered[4] = new MutableNativeImageBackedTexture(512, 512, true);
        this.mapImagesUnfiltered[0] = new ScaledMutableNativeImageBackedTexture(32, 32, true);
        this.mapImagesUnfiltered[1] = new ScaledMutableNativeImageBackedTexture(64, 64, true);
        this.mapImagesUnfiltered[2] = new ScaledMutableNativeImageBackedTexture(128, 128, true);
        this.mapImagesUnfiltered[3] = new ScaledMutableNativeImageBackedTexture(256, 256, true);
        this.mapImagesUnfiltered[4] = new ScaledMutableNativeImageBackedTexture(512, 512, true);
        if (this.options.filtering) {
            this.mapImages = this.mapImagesFiltered;
        } else {
            this.mapImages = this.mapImagesUnfiltered;
        }

        GLUtils.setupFrameBuffer();
        this.fontRenderer = VoxelConstants.getMinecraft().textRenderer;
        this.zoom = this.options.zoom;
        this.setZoomScale();
    }

    public void forceFullRender(boolean forceFullRender) {
        this.doFullRender = forceFullRender;
        this.master.getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    public float getPercentX() {
        return this.percentX;
    }

    public float getPercentY() {
        return this.percentY;
    }

    @Override
    public void run() {
        if (VoxelConstants.getMinecraft() != null) {
            while (true) {
                while (!this.threading) {
                    synchronized (this.zCalc) {
                        try {
                            this.zCalc.wait(0L);
                        } catch (InterruptedException exception) {
                            VoxelConstants.getLogger().error(exception);
                        }
                    }
                }

                for (boolean active = true; this.world != null && active; active = false) {
                    if (!this.options.hide) {
                        try {
                            this.mapCalc(this.doFullRender);
                            if (!this.doFullRender) {
                                this.chunkCache[this.zoom].centerChunks(this.blockPos.withXYZ(this.lastX, 0, this.lastZ));
                                this.chunkCache[this.zoom].checkIfChunksChanged();
                            }
                        } catch (Exception exception) {
                            VoxelConstants.getLogger().error(exception);
                        }
                    }

                    this.doFullRender = this.zoomChanged;
                    this.zoomChanged = false;
                }

                this.zCalcTicker = 0;
                synchronized (this.zCalc) {
                    try {
                        this.zCalc.wait(0L);
                    } catch (InterruptedException exception) {
                        VoxelConstants.getLogger().error(exception);
                    }
                }
            }
        }

    }

    public void newWorld(ClientWorld world) {
        this.world = world;
        this.lightmapTexture = this.getLightmapTexture();
        this.mapData[this.zoom].blank();
        this.doFullRender = true;
        this.master.getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    public void newWorldName() {
        String subworldName = this.waypointManager.getCurrentSubworldDescriptor(true);
        StringBuilder subworldNameBuilder = (new StringBuilder("§r")).append(I18nUtils.getString("worldmap.multiworld.newworld")).append(":").append(" ");
        if (subworldName.equals("") && this.waypointManager.isMultiworld()) {
            subworldNameBuilder.append("???");
        } else if (!subworldName.equals("")) {
            subworldNameBuilder.append(subworldName);
        }

        this.error = subworldNameBuilder.toString();
    }

    public void onTickInGame(MatrixStack matrixStack) {
        this.northRotate = this.options.oldNorth ? 90 : 0;

        if (this.lightmapTexture == null) {
            this.lightmapTexture = this.getLightmapTexture();
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindMenu.wasPressed()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }

            VoxelConstants.getMinecraft().setScreen(new GuiPersistentMap(null, this.master));
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindWaypointMenu.wasPressed()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }

            VoxelConstants.getMinecraft().setScreen(new GuiWaypoints(null, this.master));
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindWaypoint.wasPressed()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }

            float r;
            float g;
            float b;
            if (this.waypointManager.getWaypoints().size() == 0) {
                r = 0.0F;
                g = 1.0F;
                b = 0.0F;
            } else {
                r = this.generator.nextFloat();
                g = this.generator.nextFloat();
                b = this.generator.nextFloat();
            }

            TreeSet<DimensionContainer> dimensions = new TreeSet<>();
            dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getMinecraft().world));
            double dimensionScale = VoxelConstants.getPlayer().world.getDimension().coordinateScale();
            Waypoint newWaypoint = new Waypoint("", (int) (GameVariableAccessShim.xCoord() * dimensionScale), (int) (GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "", this.master.getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
            VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(null, this.master, newWaypoint, false));
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindMobToggle.wasPressed()) {
            this.master.getRadarOptions().setOptionValue(EnumOptionsMinimap.SHOWRADAR);
            this.options.saveAll();
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindWaypointToggle.wasPressed()) {
            this.options.toggleIngameWaypoints();
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindZoom.wasPressed()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            } else {
                this.cycleZoomLevel();
            }
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindFullscreen.wasPressed()) {
            this.fullscreenMap = !this.fullscreenMap;
            if (this.zoom == 4) {
                this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (0.25x)";
            } else if (this.zoom == 3) {
                this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (0.5x)";
            } else if (this.zoom == 2) {
                this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (1.0x)";
            } else if (this.zoom == 1) {
                this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (2.0x)";
            } else {
                this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (4.0x)";
            }
        }

        this.checkForChanges();
        if (VoxelConstants.getMinecraft().currentScreen instanceof DeathScreen && !(this.lastGuiScreen instanceof DeathScreen)) {
            this.waypointManager.handleDeath();
        }

        this.lastGuiScreen = VoxelConstants.getMinecraft().currentScreen;
        this.calculateCurrentLightAndSkyColor();
        if (this.threading) {
            if (!this.zCalc.isAlive()) {
                this.zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
                this.zCalc.setPriority(5);
                this.zCalc.start();
            }

            if (!(VoxelConstants.getMinecraft().currentScreen instanceof DeathScreen) && !(VoxelConstants.getMinecraft().currentScreen instanceof OutOfMemoryScreen)) {
                ++this.zCalcTicker;
                if (this.zCalcTicker > 2000) {
                    this.zCalcTicker = 0;
                    this.zCalc.stop();
                } else {
                    synchronized (this.zCalc) {
                        this.zCalc.notify();
                    }
                }
            }
        } else {
            if (!this.options.hide && this.world != null) {
                this.mapCalc(this.doFullRender);
                if (!this.doFullRender) {
                    this.chunkCache[this.zoom].centerChunks(this.blockPos.withXYZ(this.lastX, 0, this.lastZ));
                    this.chunkCache[this.zoom].checkIfChunksChanged();
                }
            }

            this.doFullRender = false;
        }

        boolean enabled = !VoxelConstants.getMinecraft().options.hudHidden && (this.options.showUnderMenus || VoxelConstants.getMinecraft().currentScreen == null) && !VoxelConstants.getMinecraft().options.debugEnabled;

        this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

        while (this.direction >= 360.0F) {
            this.direction -= 360.0F;
        }

        while (this.direction < 0.0F) {
            this.direction += 360.0F;
        }

        if (!this.error.equals("") && this.ztimer == 0) {
            this.ztimer = 500;
        }

        if (this.ztimer > 0) {
            --this.ztimer;
        }

        if (this.ztimer == 0 && !this.error.equals("")) {
            this.error = "";
        }

        if (enabled) {
            this.drawMinimap(matrixStack);
        }

        this.timer = this.timer > 5000 ? 0 : this.timer + 1;
    }

    private void cycleZoomLevel() {
        if (this.options.zoom == 4) {
            this.options.zoom = 3;
            this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (0.5x)";
        } else if (this.options.zoom == 3) {
            this.options.zoom = 2;
            this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (1.0x)";
        } else if (this.options.zoom == 2) {
            this.options.zoom = 1;
            this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (2.0x)";
        } else if (this.options.zoom == 1) {
            this.options.zoom = 0;
            this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (4.0x)";
        } else if (this.options.zoom == 0) {
            if (this.multicore && VoxelConstants.getMinecraft().options.getSimulationDistance().getValue() > 8) {
                this.options.zoom = 4;
                this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (0.25x)";
            } else {
                this.options.zoom = 3;
                this.error = I18nUtils.getString("minimap.ui.zoomlevel") + " (0.5x)";
            }
        }

        this.options.saveAll();
        this.zoomChanged = true;
        this.zoom = this.options.zoom;
        this.setZoomScale();
        this.doFullRender = true;
    }

    private void setZoomScale() {
        this.zoomScale = Math.pow(2.0, this.zoom) / 2.0;
        if (this.options.squareMap && this.options.rotates) {
            this.zoomScaleAdjusted = this.zoomScale / 1.4142F;
        } else {
            this.zoomScaleAdjusted = this.zoomScale;
        }

    }

    private NativeImageBackedTexture getLightmapTexture() {
        LightmapTextureManager lightTextureManager = VoxelConstants.getMinecraft().gameRenderer.getLightmapTextureManager();
        Object lightmapTextureObj = ReflectionUtils.getPrivateFieldValueByType(lightTextureManager, LightmapTextureManager.class, NativeImageBackedTexture.class);
        return lightmapTextureObj == null ? null : (NativeImageBackedTexture) lightmapTextureObj;
    }

    public void calculateCurrentLightAndSkyColor() {
        try {
            if (this.world != null) {
                if (this.needLightmapRefresh && TickCounter.tickCounter != this.tickWithLightChange && !VoxelConstants.getMinecraft().isPaused() || this.options.realTimeTorches) {
                    GLUtils.disp(this.lightmapTexture.getGlId());
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder());
                    GLShim.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);

                    for (int i = 0; i < this.lightmapColors.length; ++i) {
                        int index = i * 4;
                        this.lightmapColors[i] = (byteBuffer.get(index + 3) << 24) + (byteBuffer.get(index) << 16) + (byteBuffer.get(index + 1) << 8) + (byteBuffer.get(index + 2));
                    }

                    if (this.lightmapColors[255] != 0) {
                        this.needLightmapRefresh = false;
                    }
                }

                boolean lightChanged = false;
                if (VoxelConstants.getMinecraft().options.getGamma().getValue() != this.lastGamma) {
                    lightChanged = true;
                    this.lastGamma = VoxelConstants.getMinecraft().options.getGamma().getValue();
                }

                float[] providerLightBrightnessTable = new float[16];

                for (int t = 0; t < 16; ++t) {
                    providerLightBrightnessTable[t] = this.world.getDimension().getSkyAngle(t);
                }

                for (int t = 0; t < 16; ++t) {
                    if (providerLightBrightnessTable[t] != this.lastLightBrightnessTable[t]) {
                        lightChanged = true;
                        this.lastLightBrightnessTable[t] = providerLightBrightnessTable[t];
                    }
                }

                float sunBrightness = this.world.getStarBrightness(1.0F);
                if (Math.abs(this.lastSunBrightness - sunBrightness) > 0.01 || sunBrightness == 1.0 && sunBrightness != this.lastSunBrightness || sunBrightness == 0.0 && sunBrightness != this.lastSunBrightness) {
                    lightChanged = true;
                    this.needSkyColor = true;
                    this.lastSunBrightness = sunBrightness;
                }

                float potionEffect = 0.0F;
                if (VoxelConstants.getPlayer().hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    int duration = VoxelConstants.getPlayer().getStatusEffect(StatusEffects.NIGHT_VISION).getDuration();
                    potionEffect = duration > 200 ? 1.0F : 0.7F + MathHelper.sin((duration - 1.0F) * (float) Math.PI * 0.2F) * 0.3F;
                }

                if (this.lastPotion != potionEffect) {
                    this.lastPotion = potionEffect;
                    lightChanged = true;
                }

                int lastLightningBolt = this.world.getLightningTicksLeft();
                if (this.lastLightning != lastLightningBolt) {
                    this.lastLightning = lastLightningBolt;
                    lightChanged = true;
                }

                if (this.lastPaused != VoxelConstants.getMinecraft().isPaused()) {
                    this.lastPaused = !this.lastPaused;
                    lightChanged = true;
                }

                boolean scheduledUpdate = (this.timer - 50) % (this.lastLightBrightnessTable[0] == 0.0F ? 250 : 2000) == 0;
                if (lightChanged || scheduledUpdate) {
                    this.tickWithLightChange = TickCounter.tickCounter;
                    this.needLightmapRefresh = true;
                }

                boolean aboveHorizon = VoxelConstants.getPlayer().getCameraPosVec(0.0F).y >= this.world.getLevelProperties().getSkyDarknessHeight(this.world);
                if (this.world.getRegistryKey().getValue().toString().toLowerCase().contains("ether")) {
                    aboveHorizon = true;
                }

                if (aboveHorizon != this.lastAboveHorizon) {
                    this.needSkyColor = true;
                    this.lastAboveHorizon = aboveHorizon;
                }

                int biomeID = this.world.getRegistryManager().get(RegistryKeys.BIOME).getRawId(this.world.getBiome(this.blockPos.withXYZ(GameVariableAccessShim.xCoord(), GameVariableAccessShim.yCoord(), GameVariableAccessShim.zCoord())).value());
                if (biomeID != this.lastBiome) {
                    this.needSkyColor = true;
                    this.lastBiome = biomeID;
                }

                if (this.needSkyColor || scheduledUpdate) {
                    this.colorManager.setSkyColor(this.getSkyColor());
                }
            }
        } catch (NullPointerException ignore) {

        }
    }

    private int getSkyColor() {
        this.needSkyColor = false;
        boolean aboveHorizon = this.lastAboveHorizon;
        float[] fogColors = new float[4];
        FloatBuffer temp = BufferUtils.createFloatBuffer(4);
        BackgroundRenderer.render(VoxelConstants.getMinecraft().gameRenderer.getCamera(), 0.0F, this.world, VoxelConstants.getMinecraft().options.getViewDistance().getValue(), VoxelConstants.getMinecraft().gameRenderer.getSkyDarkness(0.0F));
        GLShim.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, temp);
        temp.get(fogColors);
        float r = fogColors[0];
        float g = fogColors[1];
        float b = fogColors[2];
        if (!aboveHorizon && VoxelConstants.getMinecraft().options.getViewDistance().getValue() >= 4) {
            return 167772160 + (int) (r * 255.0F) * 65536 + (int) (g * 255.0F) * 256 + (int) (b * 255.0F);
        } else {
            int backgroundColor = -16777216 + (int) (r * 255.0F) * 65536 + (int) (g * 255.0F) * 256 + (int) (b * 255.0F);
            float[] sunsetColors = this.world.getDimensionEffects().getFogColorOverride(this.world.getSkyAngle(0.0F), 0.0F);
            if (sunsetColors != null && VoxelConstants.getMinecraft().options.getViewDistance().getValue() >= 4) {
                int sunsetColor = (int) (sunsetColors[3] * 128.0F) * 16777216 + (int) (sunsetColors[0] * 255.0F) * 65536 + (int) (sunsetColors[1] * 255.0F) * 256 + (int) (sunsetColors[2] * 255.0F);
                return ColorUtils.colorAdder(sunsetColor, backgroundColor);
            } else {
                return backgroundColor;
            }
        }
    }

    public int[] getLightmapArray() {
        return this.lightmapColors;
    }

    public void drawMinimap(MatrixStack matrixStack) {
        int scScaleOrig = 1;

        while (VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / (scScaleOrig + 1) >= 320 && VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() / (scScaleOrig + 1) >= 240) {
            ++scScaleOrig;
        }

        int scScale = scScaleOrig + (this.fullscreenMap ? 0 : this.options.sizeModifier);
        double scaledWidthD = (double) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / scScale;
        double scaledHeightD = (double) VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() / scScale;
        this.scWidth = MathHelper.ceil(scaledWidthD);
        this.scHeight = MathHelper.ceil(scaledHeightD);
        RenderSystem.backupProjectionMatrix();
        Matrix4f matrix4f = new Matrix4f().ortho(0.0f, (float) scaledWidthD, (float) scaledHeightD, 0.0f, 1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix(matrix4f);
        MatrixStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.loadIdentity();
        modelViewMatrixStack.translate(0.0, 0.0, -2000.0);
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();
        int mapX;
        if (this.options.mapCorner != 0 && this.options.mapCorner != 3) {
            mapX = this.scWidth - 37;
        } else {
            mapX = 37;
        }

        int mapY;
        if (this.options.mapCorner != 0 && this.options.mapCorner != 1) {
            mapY = this.scHeight - 37;
        } else {
            mapY = 37;
        }

        if (this.options.mapCorner == 1 && VoxelConstants.getPlayer().getStatusEffects().size() > 0) {
            float statusIconOffset = 0.0F;

            for (StatusEffectInstance statusEffectInstance : VoxelConstants.getPlayer().getStatusEffects()) {
                if (statusEffectInstance.shouldShowIcon()) {
                    if (statusEffectInstance.getEffectType().isBeneficial()) {
                        statusIconOffset = Math.max(statusIconOffset, 24.0F);
                    } else {
                        statusIconOffset = 50.0F;
                    }
                }
            }

            int scHeight = VoxelConstants.getMinecraft().getWindow().getScaledHeight();
            float resFactor = (float) this.scHeight / scHeight;
            mapY += (int) (statusIconOffset * resFactor);
        }

        GLShim.glEnable(GL11.GL_BLEND);
        GLShim.glEnable(GL11.GL_TEXTURE_2D);
        GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, 0);
        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (!this.options.hide) {
            if (this.fullscreenMap) {
                this.renderMapFull(modelViewMatrixStack, this.scWidth, this.scHeight);
            } else {
                this.renderMap(modelViewMatrixStack, mapX, mapY, scScale);
            }

            GLShim.glDisable(GL11.GL_DEPTH_TEST);
            if (this.master.getRadar() != null && !this.fullscreenMap) {
                this.layoutVariables.updateVars(scScale, mapX, mapY, this.zoomScale, this.zoomScaleAdjusted);
                this.master.getRadar().onTickInGame(modelViewMatrixStack, this.layoutVariables);
            }

            if (!this.fullscreenMap) {
                this.drawDirections(matrixStack, mapX, mapY);
            }

            GLShim.glEnable(GL11.GL_BLEND);
            if (this.fullscreenMap) {
                this.drawArrow(modelViewMatrixStack, this.scWidth / 2, this.scHeight / 2);
            } else {
                this.drawArrow(modelViewMatrixStack, mapX, mapY);
            }
        }

        if (this.options.coords) {
            this.showCoords(matrixStack, mapX, mapY);
        }

        GLShim.glDepthMask(true);
        GLShim.glEnable(GL11.GL_DEPTH_TEST);
        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.applyModelViewMatrix();
        GLShim.glDisable(GL11.GL_DEPTH_TEST);
        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        VoxelConstants.getMinecraft().textRenderer.getClass();
        VoxelConstants.getMinecraft().textRenderer.drawWithShadow(modelViewMatrixStack, Text.literal("******sdkfjhsdkjfhsdkjfh"), 100.0F, 100.0F, -1);
        if (this.showWelcomeScreen) {
            GLShim.glEnable(GL11.GL_BLEND);
            this.drawWelcomeScreen(matrixStack, VoxelConstants.getMinecraft().getWindow().getScaledWidth(), VoxelConstants.getMinecraft().getWindow().getScaledHeight());
        }

        GLShim.glDepthMask(true);
        GLShim.glEnable(GL11.GL_DEPTH_TEST);
        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    private void checkForChanges() {
        boolean changed = false;
        if (this.colorManager.checkForChanges()) {
            this.loadMapImage();
            changed = true;
        }

        if (this.options.isChanged()) {
            if (this.options.filtering) {
                this.mapImages = this.mapImagesFiltered;
            } else {
                this.mapImages = this.mapImagesUnfiltered;
            }

            changed = true;
            this.setZoomScale();
        }

        if (changed) {
            this.doFullRender = true;
            this.master.getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }

    }

    private void mapCalc(boolean full) {
        int currentX = GameVariableAccessShim.xCoord();
        int currentZ = GameVariableAccessShim.zCoord();
        int currentY = GameVariableAccessShim.yCoord();
        int offsetX = currentX - this.lastX;
        int offsetZ = currentZ - this.lastZ;
        int offsetY = currentY - this.lastY;
        int multi = (int) Math.pow(2.0, this.zoom);
        boolean needHeightAndID;
        boolean needHeightMap = false;
        boolean needLight = false;
        boolean skyColorChanged = false;
        int skyColor = this.colorManager.getAirColor();
        if (this.lastSkyColor != skyColor) {
            skyColorChanged = true;
            this.lastSkyColor = skyColor;
        }

        if (this.options.lightmap) {
            int torchOffset = this.options.realTimeTorches ? 8 : 0;
            int skylightMultiplier = 16;

            for (int t = 0; t < 16; ++t) {
                if (this.lastLightmapValues[t] != this.lightmapColors[t * skylightMultiplier + torchOffset]) {
                    needLight = true;
                    this.lastLightmapValues[t] = this.lightmapColors[t * skylightMultiplier + torchOffset];
                }
            }
        }

        if (offsetY != 0) {
            ++this.heightMapFudge;
        } else if (this.heightMapFudge != 0) {
            ++this.heightMapFudge;
        }

        if (full || Math.abs(offsetY) >= this.heightMapResetHeight || this.heightMapFudge > this.heightMapResetTime) {
            if (this.lastY != currentY) {
                needHeightMap = true;
            }

            this.lastY = currentY;
            this.heightMapFudge = 0;
        }

        if (Math.abs(offsetX) > 32 * multi || Math.abs(offsetZ) > 32 * multi) {
            full = true;
        }

        boolean nether = false;
        boolean caves = false;
        boolean netherPlayerInOpen;
        this.blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), 256 - 1), 0), this.lastZ);
        if (VoxelConstants.getPlayer().world.getDimension().hasCeiling()) {

            netherPlayerInOpen = this.world.getChunk(this.blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, this.blockPos.getX() & 15, this.blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (VoxelConstants.getPlayer().clientWorld.getDimensionEffects().shouldBrightenLighting() && !VoxelConstants.getPlayer().clientWorld.getDimension().hasSkyLight()) {
            boolean endPlayerInOpen = this.world.getChunk(this.blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, this.blockPos.getX() & 15, this.blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && this.world.getLightLevel(LightType.SKY, this.blockPos) <= 0) {
            caves = true;
        }

        boolean beneathRendering = caves || nether;
        if (this.lastBeneathRendering != beneathRendering) {
            full = true;
        }

        this.lastBeneathRendering = beneathRendering;
        needHeightAndID = needHeightMap && (nether || caves);
        int color24;
        synchronized (this.coordinateLock) {
            if (!full) {
                this.mapImages[this.zoom].moveY(offsetZ);
                this.mapImages[this.zoom].moveX(offsetX);
            }

            this.lastX = currentX;
            this.lastZ = currentZ;
        }

        int startX = currentX - 16 * multi;
        int startZ = currentZ - 16 * multi;
        if (!full) {
            this.mapData[this.zoom].moveZ(offsetZ);
            this.mapData[this.zoom].moveX(offsetX);

            for (int imageY = offsetZ > 0 ? 32 * multi - 1 : -offsetZ - 1; imageY >= (offsetZ > 0 ? 32 * multi - offsetZ : 0); --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, this.world, multi, startX, startZ, imageX, imageY);
                    this.mapImages[this.zoom].setRGB(imageX, imageY, color24);
                }
            }

            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = offsetX > 0 ? 32 * multi - offsetX : 0; imageX < (offsetX > 0 ? 32 * multi : -offsetX); ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, this.world, multi, startX, startZ, imageX, imageY);
                    this.mapImages[this.zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if (full || this.options.heightmap && needHeightMap || needHeightAndID || this.options.lightmap && needLight || skyColorChanged) {//TODO Dynamic Light below 0
            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(full, full || needHeightAndID, full, full || needLight || needHeightAndID, nether, caves, this.world, multi, startX, startZ, imageX, imageY);
                    this.mapImages[this.zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if ((full || offsetX != 0 || offsetZ != 0 || !this.lastFullscreen) && this.fullscreenMap && this.options.biomeOverlay != 0) {
            this.mapData[this.zoom].segmentBiomes();
            this.mapData[this.zoom].findCenterOfSegments(!this.options.oldNorth);
        }

        this.lastFullscreen = this.fullscreenMap;
        if (full || offsetX != 0 || offsetZ != 0 || needHeightMap || needLight || skyColorChanged) {
            this.imageChanged = true;
        }

        if (needLight || skyColorChanged) {
            this.master.getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }

    }

    @Override
    public void handleChangeInWorld(int chunkX, int chunkZ) {
        try {
            this.chunkCache[this.zoom].registerChangeAt(chunkX, chunkZ);
        } catch (Exception e) {
            VoxelConstants.getLogger().warn(e);
        }
    }

    @Override
    public void processChunk(WorldChunk chunk) {
        this.rectangleCalc(chunk.getPos().x * 16, chunk.getPos().z * 16, chunk.getPos().x * 16 + 15, chunk.getPos().z * 16 + 15);
    }

    private void rectangleCalc(int left, int top, int right, int bottom) {
        boolean nether = false;
        boolean caves = false;
        boolean netherPlayerInOpen;
        this.blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), 256 - 1), 0), this.lastZ);
        int currentY = GameVariableAccessShim.yCoord();
        if (VoxelConstants.getPlayer().world.getDimension().hasCeiling()) {
            netherPlayerInOpen = this.world.getChunk(this.blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, this.blockPos.getX() & 15, this.blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (VoxelConstants.getPlayer().clientWorld.getDimensionEffects().shouldBrightenLighting() && !VoxelConstants.getPlayer().clientWorld.getDimension().hasSkyLight()) {
            boolean endPlayerInOpen = this.world.getChunk(this.blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, this.blockPos.getX() & 15, this.blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && this.world.getLightLevel(LightType.SKY, this.blockPos) <= 0) {
            caves = true;
        }

        int startX = this.lastX;
        int startZ = this.lastZ;
        int multi = (int) Math.pow(2.0, this.zoom);
        startX -= 16 * multi;
        startZ -= 16 * multi;
        left = left - startX - 1;
        right = right - startX + 1;
        top = top - startZ - 1;
        bottom = bottom - startZ + 1;
        left = Math.max(0, left);
        right = Math.min(32 * multi - 1, right);
        top = Math.max(0, top);
        bottom = Math.min(32 * multi - 1, bottom);
        int color24;

        for (int imageY = bottom; imageY >= top; --imageY) {
            for (int imageX = left; imageX <= right; ++imageX) {
                color24 = this.getPixelColor(true, true, true, true, nether, caves, this.world, multi, startX, startZ, imageX, imageY);
                this.mapImages[this.zoom].setRGB(imageX, imageY, color24);
            }
        }

        this.imageChanged = true;
    }

    private int getPixelColor(boolean needBiome, boolean needHeightAndID, boolean needTint, boolean needLight, boolean nether, boolean caves, World world, int multi, int startX, int startZ, int imageX, int imageY) {
        int surfaceHeight;
        int seafloorHeight = -1;
        int transparentHeight = -1;
        int foliageHeight;
        int surfaceColor;
        int seafloorColor = 0;
        int transparentColor = 0;
        int foliageColor = 0;
        this.surfaceBlockState = null;
        this.transparentBlockState = BlockRepository.air.getDefaultState();
        BlockState foliageBlockState = BlockRepository.air.getDefaultState();
        BlockState seafloorBlockState = BlockRepository.air.getDefaultState();
        boolean surfaceBlockChangeForcedTint = false;
        boolean transparentBlockChangeForcedTint = false;
        boolean foliageBlockChangeForcedTint = false;
        boolean seafloorBlockChangeForcedTint = false;
        int surfaceBlockStateID;
        int transparentBlockStateID;
        int foliageBlockStateID;
        int seafloorBlockStateID;
        this.blockPos = this.blockPos.withXYZ(startX + imageX, 0, startZ + imageY);
        int color24;
        int biomeID;
        if (needBiome) {
            if (world.isChunkLoaded(this.blockPos)) {
                biomeID = world.getRegistryManager().get(RegistryKeys.BIOME).getRawId(world.getBiome(this.blockPos).value());
            } else {
                biomeID = -1;
            }

            this.mapData[this.zoom].setBiomeID(imageX, imageY, biomeID);
        } else {
            biomeID = this.mapData[this.zoom].getBiomeID(imageX, imageY);
        }

        if (this.options.biomeOverlay == 1) {
            if (biomeID >= 0) {
                color24 = BiomeRepository.getBiomeColor(biomeID) | 0xFF000000;
            } else {
                color24 = 0;
            }

        } else {
            boolean solid = false;
            if (needHeightAndID) {
                if (!nether && !caves) {
                    WorldChunk chunk = world.getWorldChunk(this.blockPos);
                    transparentHeight = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, this.blockPos.getX() & 15, this.blockPos.getZ() & 15) + 1;
                    this.transparentBlockState = world.getBlockState(this.blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY));
                    FluidState fluidState = this.transparentBlockState.getFluidState();
                    if (fluidState != Fluids.EMPTY.getDefaultState()) {
                        this.transparentBlockState = fluidState.getBlockState();
                    }

                    surfaceHeight = transparentHeight;
                    this.surfaceBlockState = this.transparentBlockState;
                    VoxelShape voxelShape;
                    boolean hasOpacity = this.surfaceBlockState.getOpacity(world, this.blockPos) > 0;
                    if (!hasOpacity && this.surfaceBlockState.isOpaque() && this.surfaceBlockState.hasSidedTransparency()) {
                        voxelShape = this.surfaceBlockState.getCullingFace(world, this.blockPos, Direction.DOWN);
                        hasOpacity = VoxelShapes.unionCoversFullCube(voxelShape, VoxelShapes.empty());
                        voxelShape = this.surfaceBlockState.getCullingFace(world, this.blockPos, Direction.UP);
                        hasOpacity = hasOpacity || VoxelShapes.unionCoversFullCube(VoxelShapes.empty(), voxelShape);
                    }

                    while (!hasOpacity && surfaceHeight > 0) {
                        foliageBlockState = this.surfaceBlockState;
                        --surfaceHeight;
                        this.surfaceBlockState = world.getBlockState(this.blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                        fluidState = this.surfaceBlockState.getFluidState();
                        if (fluidState != Fluids.EMPTY.getDefaultState()) {
                            this.surfaceBlockState = fluidState.getBlockState();
                        }

                        hasOpacity = this.surfaceBlockState.getOpacity(world, this.blockPos) > 0;
                        if (!hasOpacity && this.surfaceBlockState.isOpaque() && this.surfaceBlockState.hasSidedTransparency()) {
                            voxelShape = this.surfaceBlockState.getCullingFace(world, this.blockPos, Direction.DOWN);
                            hasOpacity = VoxelShapes.unionCoversFullCube(voxelShape, VoxelShapes.empty());
                            voxelShape = this.surfaceBlockState.getCullingFace(world, this.blockPos, Direction.UP);
                            hasOpacity = hasOpacity || VoxelShapes.unionCoversFullCube(VoxelShapes.empty(), voxelShape);
                        }
                    }

                    if (surfaceHeight == transparentHeight) {
                        transparentHeight = -1;
                        this.transparentBlockState = BlockRepository.air.getDefaultState();
                        foliageBlockState = world.getBlockState(this.blockPos.withXYZ(startX + imageX, surfaceHeight, startZ + imageY));
                    }

                    if (foliageBlockState.getMaterial() == Material.SNOW_LAYER) {
                        this.surfaceBlockState = foliageBlockState;
                        foliageBlockState = BlockRepository.air.getDefaultState();
                    }

                    if (foliageBlockState == this.transparentBlockState) {
                        foliageBlockState = BlockRepository.air.getDefaultState();
                    }

                    if (foliageBlockState != null && foliageBlockState.getMaterial() != Material.AIR) {
                        foliageHeight = surfaceHeight + 1;
                    } else {
                        foliageHeight = -1;
                    }

                    Material material = this.surfaceBlockState.getMaterial();
                    if (material == Material.WATER || material == Material.ICE) {
                        seafloorHeight = surfaceHeight;

                        for (seafloorBlockState = world.getBlockState(this.blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY)); seafloorBlockState.getOpacity(world, this.blockPos) < 5 && seafloorBlockState.getMaterial() != Material.LEAVES && seafloorHeight > 1; seafloorBlockState = world.getBlockState(this.blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY))) {
                            material = seafloorBlockState.getMaterial();
                            if (transparentHeight == -1 && material != Material.ICE && material != Material.WATER && material.blocksMovement()) {
                                transparentHeight = seafloorHeight;
                                this.transparentBlockState = seafloorBlockState;
                            }

                            if (foliageHeight == -1 && seafloorHeight != transparentHeight && this.transparentBlockState != seafloorBlockState && material != Material.ICE && material != Material.WATER && material != Material.AIR && material != Material.BUBBLE_COLUMN) {
                                foliageHeight = seafloorHeight;
                                foliageBlockState = seafloorBlockState;
                            }

                            --seafloorHeight;
                        }

                        if (seafloorBlockState.getMaterial() == Material.WATER) {
                            seafloorBlockState = BlockRepository.air.getDefaultState();
                        }
                    }
                } else {
                    surfaceHeight = this.getNetherHeight(startX + imageX, startZ + imageY);
                    this.surfaceBlockState = world.getBlockState(this.blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                    surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                    foliageHeight = surfaceHeight + 1;
                    this.blockPos.setXYZ(startX + imageX, foliageHeight - 1, startZ + imageY);
                    foliageBlockState = world.getBlockState(this.blockPos);
                    Material material = foliageBlockState.getMaterial();
                    if (material != Material.SNOW_LAYER && material != Material.AIR && material != Material.LAVA && material != Material.WATER) {
                        foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                    } else {
                        foliageHeight = -1;
                    }
                }

                surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                if (this.options.biomes && this.surfaceBlockState != this.mapData[this.zoom].getBlockstate(imageX, imageY)) {
                    surfaceBlockChangeForcedTint = true;
                }

                this.mapData[this.zoom].setHeight(imageX, imageY, surfaceHeight);
                this.mapData[this.zoom].setBlockstateID(imageX, imageY, surfaceBlockStateID);
                if (this.options.biomes && this.transparentBlockState != this.mapData[this.zoom].getTransparentBlockstate(imageX, imageY)) {
                    transparentBlockChangeForcedTint = true;
                }

                this.mapData[this.zoom].setTransparentHeight(imageX, imageY, transparentHeight);
                transparentBlockStateID = BlockRepository.getStateId(this.transparentBlockState);
                this.mapData[this.zoom].setTransparentBlockstateID(imageX, imageY, transparentBlockStateID);
                if (this.options.biomes && foliageBlockState != this.mapData[this.zoom].getFoliageBlockstate(imageX, imageY)) {
                    foliageBlockChangeForcedTint = true;
                }

                this.mapData[this.zoom].setFoliageHeight(imageX, imageY, foliageHeight);
                foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                this.mapData[this.zoom].setFoliageBlockstateID(imageX, imageY, foliageBlockStateID);
                if (this.options.biomes && seafloorBlockState != this.mapData[this.zoom].getOceanFloorBlockstate(imageX, imageY)) {
                    seafloorBlockChangeForcedTint = true;
                }

                this.mapData[this.zoom].setOceanFloorHeight(imageX, imageY, seafloorHeight);
                seafloorBlockStateID = BlockRepository.getStateId(seafloorBlockState);
                this.mapData[this.zoom].setOceanFloorBlockstateID(imageX, imageY, seafloorBlockStateID);
            } else {
                surfaceHeight = this.mapData[this.zoom].getHeight(imageX, imageY);
                surfaceBlockStateID = this.mapData[this.zoom].getBlockstateID(imageX, imageY);
                this.surfaceBlockState = BlockRepository.getStateById(surfaceBlockStateID);
                transparentHeight = this.mapData[this.zoom].getTransparentHeight(imageX, imageY);
                transparentBlockStateID = this.mapData[this.zoom].getTransparentBlockstateID(imageX, imageY);
                this.transparentBlockState = BlockRepository.getStateById(transparentBlockStateID);
                foliageHeight = this.mapData[this.zoom].getFoliageHeight(imageX, imageY);
                foliageBlockStateID = this.mapData[this.zoom].getFoliageBlockstateID(imageX, imageY);
                foliageBlockState = BlockRepository.getStateById(foliageBlockStateID);
                seafloorHeight = this.mapData[this.zoom].getOceanFloorHeight(imageX, imageY);
                seafloorBlockStateID = this.mapData[this.zoom].getOceanFloorBlockstateID(imageX, imageY);
                seafloorBlockState = BlockRepository.getStateById(seafloorBlockStateID);
            }

            if (surfaceHeight == -1) {
                surfaceHeight = this.lastY + 1;
                solid = true;
            }

            if (this.surfaceBlockState.getMaterial() == Material.LAVA) {
                solid = false;
            }

            if (this.options.biomes) {
                surfaceColor = this.colorManager.getBlockColor(this.blockPos, surfaceBlockStateID, biomeID);
                int tint;
                if (!needTint && !surfaceBlockChangeForcedTint) {
                    tint = this.mapData[this.zoom].getBiomeTint(imageX, imageY);
                } else {
                    tint = this.colorManager.getBiomeTint(this.mapData[this.zoom], world, this.surfaceBlockState, surfaceBlockStateID, this.blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY), this.tempBlockPos, startX, startZ);
                    this.mapData[this.zoom].setBiomeTint(imageX, imageY, tint);
                }

                if (tint != -1) {
                    surfaceColor = ColorUtils.colorMultiplier(surfaceColor, tint);
                }
            } else {
                surfaceColor = this.colorManager.getBlockColorWithDefaultTint(this.blockPos, surfaceBlockStateID);
            }

            surfaceColor = this.applyHeight(surfaceColor, nether, caves, world, multi, startX, startZ, imageX, imageY, surfaceHeight, solid, 1);
            int light;
            if (needLight) {
                light = this.getLight(surfaceColor, this.surfaceBlockState, world, startX + imageX, startZ + imageY, surfaceHeight, solid);
                this.mapData[this.zoom].setLight(imageX, imageY, light);
            } else {
                light = this.mapData[this.zoom].getLight(imageX, imageY);
            }

            if (light == 0) {
                surfaceColor = 0;
            } else if (light != 255) {
                surfaceColor = ColorUtils.colorMultiplier(surfaceColor, light);
            }

            if (this.options.waterTransparency && seafloorHeight != -1) {
                if (!this.options.biomes) {
                    seafloorColor = this.colorManager.getBlockColorWithDefaultTint(this.blockPos, seafloorBlockStateID);
                } else {
                    seafloorColor = this.colorManager.getBlockColor(this.blockPos, seafloorBlockStateID, biomeID);
                    int tint;
                    if (!needTint && !seafloorBlockChangeForcedTint) {
                        tint = this.mapData[this.zoom].getOceanFloorBiomeTint(imageX, imageY);
                    } else {
                        tint = this.colorManager.getBiomeTint(this.mapData[this.zoom], world, seafloorBlockState, seafloorBlockStateID, this.blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY), this.tempBlockPos, startX, startZ);
                        this.mapData[this.zoom].setOceanFloorBiomeTint(imageX, imageY, tint);
                    }

                    if (tint != -1) {
                        seafloorColor = ColorUtils.colorMultiplier(seafloorColor, tint);
                    }
                }

                seafloorColor = this.applyHeight(seafloorColor, nether, caves, world, multi, startX, startZ, imageX, imageY, seafloorHeight, solid, 0);
                int seafloorLight;
                if (needLight) {
                    seafloorLight = this.getLight(seafloorColor, seafloorBlockState, world, startX + imageX, startZ + imageY, seafloorHeight, solid);
                    this.blockPos.setXYZ(startX + imageX, seafloorHeight, startZ + imageY);
                    BlockState blockStateAbove = world.getBlockState(this.blockPos);
                    Material materialAbove = blockStateAbove.getMaterial();
                    if (this.options.lightmap && materialAbove == Material.ICE) {
                        int multiplier = 255;
                        // I'm not sure if this will be a 100% correct fix, but we'll see // Algo
                        //if (this.game.options.getAo().getValue() == AoMode.MIN) {
                        //    multiplier = 200;
                        //} else if (this.game.options.getAo().getValue() == AoMode.MAX) {
                        //    multiplier = 120;
                        //}
                        multiplier = (VoxelConstants.getMinecraft().options.getAo().getValue()) ? 200 : 120;

                        seafloorLight = ColorUtils.colorMultiplier(seafloorLight, 0xFF000000 | multiplier << 16 | multiplier << 8 | multiplier);
                    }

                    this.mapData[this.zoom].setOceanFloorLight(imageX, imageY, seafloorLight);
                } else {
                    seafloorLight = this.mapData[this.zoom].getOceanFloorLight(imageX, imageY);
                }

                if (seafloorLight == 0) {
                    seafloorColor = 0;
                } else if (seafloorLight != 255) {
                    seafloorColor = ColorUtils.colorMultiplier(seafloorColor, seafloorLight);
                }
            }

            if (this.options.blockTransparency) {
                if (transparentHeight != -1 && this.transparentBlockState != null && this.transparentBlockState != BlockRepository.air.getDefaultState()) {
                    if (this.options.biomes) {
                        transparentColor = this.colorManager.getBlockColor(this.blockPos, transparentBlockStateID, biomeID);
                        int tint;
                        if (!needTint && !transparentBlockChangeForcedTint) {
                            tint = this.mapData[this.zoom].getTransparentBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[this.zoom], world, this.transparentBlockState, transparentBlockStateID, this.blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY), this.tempBlockPos, startX, startZ);
                            this.mapData[this.zoom].setTransparentBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            transparentColor = ColorUtils.colorMultiplier(transparentColor, tint);
                        }
                    } else {
                        transparentColor = this.colorManager.getBlockColorWithDefaultTint(this.blockPos, transparentBlockStateID);
                    }

                    transparentColor = this.applyHeight(transparentColor, nether, caves, world, multi, startX, startZ, imageX, imageY, transparentHeight, solid, 3);
                    int transparentLight;
                    if (needLight) {
                        transparentLight = this.getLight(transparentColor, this.transparentBlockState, world, startX + imageX, startZ + imageY, transparentHeight, solid);
                        this.mapData[this.zoom].setTransparentLight(imageX, imageY, transparentLight);
                    } else {
                        transparentLight = this.mapData[this.zoom].getTransparentLight(imageX, imageY);
                    }

                    if (transparentLight == 0) {
                        transparentColor = 0;
                    } else if (transparentLight != 255) {
                        transparentColor = ColorUtils.colorMultiplier(transparentColor, transparentLight);
                    }
                }

                if (foliageHeight != -1 && foliageBlockState != null && foliageBlockState != BlockRepository.air.getDefaultState()) {
                    if (!this.options.biomes) {
                        foliageColor = this.colorManager.getBlockColorWithDefaultTint(this.blockPos, foliageBlockStateID);
                    } else {
                        foliageColor = this.colorManager.getBlockColor(this.blockPos, foliageBlockStateID, biomeID);
                        int tint;
                        if (!needTint && !foliageBlockChangeForcedTint) {
                            tint = this.mapData[this.zoom].getFoliageBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[this.zoom], world, foliageBlockState, foliageBlockStateID, this.blockPos.withXYZ(startX + imageX, foliageHeight - 1, startZ + imageY), this.tempBlockPos, startX, startZ);
                            this.mapData[this.zoom].setFoliageBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            foliageColor = ColorUtils.colorMultiplier(foliageColor, tint);
                        }
                    }

                    foliageColor = this.applyHeight(foliageColor, nether, caves, world, multi, startX, startZ, imageX, imageY, foliageHeight, solid, 2);
                    int foliageLight;
                    if (needLight) {
                        foliageLight = this.getLight(foliageColor, foliageBlockState, world, startX + imageX, startZ + imageY, foliageHeight, solid);
                        this.mapData[this.zoom].setFoliageLight(imageX, imageY, foliageLight);
                    } else {
                        foliageLight = this.mapData[this.zoom].getFoliageLight(imageX, imageY);
                    }

                    if (foliageLight == 0) {
                        foliageColor = 0;
                    } else if (foliageLight != 255) {
                        foliageColor = ColorUtils.colorMultiplier(foliageColor, foliageLight);
                    }
                }
            }

            if (seafloorColor != 0 && seafloorHeight > 0) {
                color24 = seafloorColor;
                if (foliageColor != 0 && foliageHeight <= surfaceHeight) {
                    color24 = ColorUtils.colorAdder(foliageColor, seafloorColor);
                }

                if (transparentColor != 0 && transparentHeight <= surfaceHeight) {
                    color24 = ColorUtils.colorAdder(transparentColor, color24);
                }

                color24 = ColorUtils.colorAdder(surfaceColor, color24);
            } else {
                color24 = surfaceColor;
            }

            if (foliageColor != 0 && foliageHeight > surfaceHeight) {
                color24 = ColorUtils.colorAdder(foliageColor, color24);
            }

            if (transparentColor != 0 && transparentHeight > surfaceHeight) {
                color24 = ColorUtils.colorAdder(transparentColor, color24);
            }

            if (this.options.biomeOverlay == 2) {
                int bc = 0;
                if (biomeID >= 0) {
                    bc = BiomeRepository.getBiomeColor(biomeID);
                }

                bc = 2130706432 | bc;
                color24 = ColorUtils.colorAdder(bc, color24);
            }

        }
        return MapUtils.doSlimeAndGrid(color24, startX + imageX, startZ + imageY);
    }

    private int getBlockHeight(boolean nether, boolean caves, World world, int x, int z) {
        int playerHeight = GameVariableAccessShim.yCoord();
        this.blockPos.setXYZ(x, playerHeight, z);
        WorldChunk chunk = (WorldChunk) world.getChunk(this.blockPos);
        int height = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, this.blockPos.getX() & 15, this.blockPos.getZ() & 15) + 1;
        BlockState blockState = world.getBlockState(this.blockPos.withXYZ(x, height - 1, z));
        FluidState fluidState = this.transparentBlockState.getFluidState();
        if (fluidState != Fluids.EMPTY.getDefaultState()) {
            blockState = fluidState.getBlockState();
        }

        while (blockState.getOpacity(world, this.blockPos) == 0 && height > 0) {
            --height;
            blockState = world.getBlockState(this.blockPos.withXYZ(x, height - 1, z));
            fluidState = this.surfaceBlockState.getFluidState();
            if (fluidState != Fluids.EMPTY.getDefaultState()) {
                blockState = fluidState.getBlockState();
            }
        }

        return (nether || caves) && height > playerHeight ? this.getNetherHeight(x, z) : height;
    }

    private int getNetherHeight(int x, int z) {
        int y = this.lastY;
        this.blockPos.setXYZ(x, y, z);
        BlockState blockState = this.world.getBlockState(this.blockPos);
        if (blockState.getOpacity(this.world, this.blockPos) == 0 && blockState.getMaterial() != Material.LAVA) {
            while (y > world.getBottomY()) {
                --y;
                this.blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(this.blockPos);
                if (blockState.getOpacity(this.world, this.blockPos) > 0 || blockState.getMaterial() == Material.LAVA) {
                    return y + 1;
                }
            }

            return y;
        } else {
            while (y <= this.lastY + 10 && y < world.getTopY()) {
                ++y;
                this.blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(this.blockPos);
                if (blockState.getOpacity(this.world, this.blockPos) == 0 && blockState.getMaterial() != Material.LAVA) {
                    return y;
                }
            }

            return -1;
        }
    }

    private int getSeafloorHeight(World world, int x, int z, int height) {
        for (BlockState blockState = world.getBlockState(this.blockPos.withXYZ(x, height - 1, z)); blockState.getOpacity(world, this.blockPos) < 5 && blockState.getMaterial() != Material.LEAVES && height > 1; blockState = world.getBlockState(this.blockPos.withXYZ(x, height - 1, z))) {
            --height;
        }

        return height;
    }

    private int getTransparentHeight(boolean nether, boolean caves, World world, int x, int z, int height) {
        int transHeight;
        if (!caves && !nether) {
            transHeight = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, this.blockPos.withXYZ(x, height, z)).getY();
            if (transHeight <= height) {
                transHeight = -1;
            }
        } else {
            transHeight = -1;
        }

        BlockState blockState = world.getBlockState(this.blockPos.withXYZ(x, transHeight - 1, z));
        Material material = blockState.getMaterial();
        if (transHeight == height + 1 && material == Material.SNOW_LAYER) {
            transHeight = -1;
        }

        if (material == Material.BARRIER) {
            ++transHeight;
            blockState = world.getBlockState(this.blockPos.withXYZ(x, transHeight - 1, z));
            material = blockState.getMaterial();
            if (material == Material.AIR) {
                transHeight = -1;
            }
        }

        return transHeight;
    }

    private int applyHeight(int color24, boolean nether, boolean caves, World world, int multi, int startX, int startZ, int imageX, int imageY, int height, boolean solid, int layer) {
        if (color24 != this.colorManager.getAirColor() && color24 != 0 && (this.options.heightmap || this.options.slopemap) && !solid) {
            int heightComp = -1;
            int diff;
            double sc = 0.0;
            if (!this.options.slopemap) {
                diff = height - this.lastY;
                sc = Math.log10(Math.abs(diff) / 8.0 + 1.0) / 1.8;
                if (diff < 0) {
                    sc = 0.0 - sc;
                }
            } else {
                if (imageX > 0 && imageY < 32 * multi - 1) {
                    if (layer == 0) {
                        heightComp = this.mapData[this.zoom].getOceanFloorHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 1) {
                        heightComp = this.mapData[this.zoom].getHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        heightComp = this.mapData[this.zoom].getTransparentHeight(imageX - 1, imageY + 1);
                        if (heightComp == -1) {
                            Block block = BlockRepository.getStateById(this.mapData[this.zoom].getTransparentBlockstateID(imageX, imageY)).getBlock();
                            if (block instanceof GlassBlock || block instanceof StainedGlassBlock) {
                                heightComp = this.mapData[this.zoom].getHeight(imageX - 1, imageY + 1);
                            }
                        }
                    }
                } else {
                    if (layer == 0) {
                        int baseHeight = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getSeafloorHeight(world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
                    }

                    if (layer == 1) {
                        heightComp = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        int baseHeight = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getTransparentHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
                        if (heightComp == -1) {
                            BlockState blockState = world.getBlockState(this.blockPos.withXYZ(startX + imageX, height - 1, startZ + imageY));
                            Block block = blockState.getBlock();
                            if (block instanceof GlassBlock || block instanceof StainedGlassBlock) {
                                heightComp = baseHeight;
                            }
                        }
                    }
                }

                if (heightComp == -1) {
                    heightComp = height;
                }

                diff = heightComp - height;
                if (diff != 0) {
                    sc = diff > 0 ? 1.0 : -1.0;
                    sc /= 8.0;
                }

                if (this.options.heightmap) {
                    diff = height - this.lastY;
                    double heightsc = Math.log10(Math.abs(diff) / 8.0 + 1.0) / 3.0;
                    sc = diff > 0 ? sc + heightsc : sc - heightsc;
                }
            }

            int alpha = color24 >> 24 & 0xFF;
            int r = color24 >> 16 & 0xFF;
            int g = color24 >> 8 & 0xFF;
            int b = color24 & 0xFF;
            if (sc > 0.0) {
                r += (int) (sc * (255 - r));
                g += (int) (sc * (255 - g));
                b += (int) (sc * (255 - b));
            } else if (sc < 0.0) {
                sc = Math.abs(sc);
                r -= (int) (sc * r);
                g -= (int) (sc * g);
                b -= (int) (sc * b);
            }

            color24 = alpha * 16777216 + r * 65536 + g * 256 + b;
        }

        return color24;
    }

    private int getLight(int color24, BlockState blockState, World world, int x, int z, int height, boolean solid) {
        int i3 = 255;
        if (solid) {
            i3 = 0;
        } else if (color24 != this.colorManager.getAirColor() && color24 != 0 && this.options.lightmap) {
            this.blockPos.setXYZ(x, Math.max(Math.min(height, 256 - 1), 0), z);
            int blockLight = world.getLightLevel(LightType.BLOCK, this.blockPos);
            int skyLight = world.getLightLevel(LightType.SKY, this.blockPos);
            if (blockState.getMaterial() == Material.LAVA || blockState.getBlock() == Blocks.MAGMA_BLOCK) {
                blockLight = 14;
            }

            i3 = this.lightmapColors[blockLight + skyLight * 16];
        }

        return i3;
    }

    private void renderMap(MatrixStack matrixStack, int x, int y, int scScale) {
        float scale = 1.0F;
        if (this.options.squareMap && this.options.rotates) {
            scale = 1.4142F;
        }

        if (GLUtils.hasAlphaBits) {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            GLShim.glColorMask(false, false, false, true);
            GLShim.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GLShim.glClear(16384);
            GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GLShim.glColorMask(true, true, true, true);
            GLUtils.img2(this.options.squareMap ? this.squareStencil : this.circleStencil);
            GLUtils.drawPre();
            GLUtils.setMap(x, y, 128);
            GLUtils.drawPost();
            GLShim.glBlendFunc(772, 773);
            synchronized (this.coordinateLock) {
                if (this.imageChanged) {
                    this.imageChanged = false;
                    this.mapImages[this.zoom].write();
                    this.lastImageX = this.lastX;
                    this.lastImageZ = this.lastZ;
                }
            }

            float multi = (float) (1.0 / this.zoomScaleAdjusted);
            this.percentX = (float) (GameVariableAccessShim.xCoordDouble() - this.lastImageX);
            this.percentY = (float) (GameVariableAccessShim.zCoordDouble() - this.lastImageZ);
            this.percentX *= multi;
            this.percentY *= multi;
            GLUtils.disp2(this.mapImages[this.zoom].getIndex());
            matrixStack.push();
            matrixStack.translate(x, y, 0.0);
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(!this.options.rotates ? this.northRotate : -this.direction));
            matrixStack.translate(-x, -y, 0.0);
            matrixStack.translate(-this.percentX, -this.percentY, 0.0);
            RenderSystem.applyModelViewMatrix();
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            GLShim.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            Matrix4f matrix4f = new Matrix4f().ortho(0.0F, 512.0F, 512.0F, 0.0F, 1000.0F, 3000.0F);
            RenderSystem.setProjectionMatrix(matrix4f);
            GLUtils.bindFrameBuffer();
            GLShim.glViewport(0, 0, 512, 512);
            matrixStack.push();
            matrixStack.loadIdentity();
            matrixStack.translate(0.0, 0.0, -2000.0);
            RenderSystem.applyModelViewMatrix();
            GLShim.glDepthMask(false);
            GLShim.glDisable(GL11.GL_DEPTH_TEST);
            GLShim.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GLShim.glClear(16384);
            GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, 0);
            GLUtils.img2(this.options.squareMap ? this.squareStencil : this.circleStencil);
            GLUtils.drawPre();
            GLUtils.ldrawthree(256.0F - 256.0F / scale, 256.0F + 256.0F / scale, 1.0, 0.0F, 0.0F);
            GLUtils.ldrawthree( (256.0F + 256.0F / scale), 256.0F + 256.0F / scale, 1.0, 1.0F, 0.0F);
            GLUtils.ldrawthree(256.0F + 256.0F / scale, 256.0F - 256.0F / scale, 1.0, 1.0F, 1.0F);
            GLUtils.ldrawthree(256.0F - 256.0F / scale, 256.0F - 256.0F / scale, 1.0, 0.0F, 1.0F);
            BufferBuilder bb = Tessellator.getInstance().getBuffer();
            //BufferRenderer.drawWithShader(bb.end());
            BufferRenderer.drawWithGlobalProgram(bb.end());
            GLShim.glBlendFuncSeparate(1, 0, 774, 0);
            synchronized (this.coordinateLock) {
                if (this.imageChanged) {
                    this.imageChanged = false;
                    this.mapImages[this.zoom].write();
                    this.lastImageX = this.lastX;
                    this.lastImageZ = this.lastZ;
                }
            }

            float multi = (float) (1.0 / this.zoomScale);
            this.percentX = (float) (GameVariableAccessShim.xCoordDouble() - this.lastImageX);
            this.percentY = (float) (GameVariableAccessShim.zCoordDouble() - this.lastImageZ);
            this.percentX *= multi;
            this.percentY *= multi;
            GLUtils.disp2(this.mapImages[this.zoom].getIndex());
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            matrixStack.push();
            matrixStack.translate(256.0, 256.0, 0.0);
            if (!this.options.rotates) {
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-this.northRotate)));
            } else {
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.direction));
            }

            matrixStack.translate(-256.0, -256.0, 0.0);
            matrixStack.translate(-this.percentX * 512.0F / 64.0F, this.percentY * 512.0F / 64.0F, 0.0);
            RenderSystem.applyModelViewMatrix();
            GLUtils.drawPre();
            GLUtils.ldrawthree(0.0, 512.0, 1.0, 0.0F, 0.0F);
            GLUtils.ldrawthree(512.0, 512.0, 1.0, 1.0F, 0.0F);
            GLUtils.ldrawthree(512.0, 0.0, 1.0, 1.0F, 1.0F);
            GLUtils.ldrawthree(0.0, 0.0, 1.0, 0.0F, 1.0F);
            GLUtils.drawPost();
            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
            GLShim.glDepthMask(true);
            GLShim.glEnable(GL11.GL_DEPTH_TEST);
            GLUtils.unbindFrameBuffer();
            GLShim.glViewport(0, 0, VoxelConstants.getMinecraft().getWindow().getFramebufferWidth(), VoxelConstants.getMinecraft().getWindow().getFramebufferHeight());
            matrixStack.pop();
            RenderSystem.setProjectionMatrix(minimapProjectionMatrix);
            matrixStack.push();
            GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, 0);
            GLUtils.disp2(GLUtils.fboTextureID);
        }

        double guiScale = (double) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / this.scWidth;
        GLShim.glEnable(3089);
        GLShim.glScissor((int) (guiScale * (x - 32)), (int) (guiScale * ((this.scHeight - y) - 32.0)), (int) (guiScale * 64.0), (int) (guiScale * 63.0));
        GLUtils.drawPre();
        GLUtils.setMapWithScale(x, y, scale);
        GLUtils.drawPost();
        GLShim.glDisable(3089);
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.options.squareMap) {
            this.drawSquareMapFrame(x, y);
        } else {
            this.drawRoundMapFrame(x, y);
        }

        double lastXDouble = GameVariableAccessShim.xCoordDouble();
        double lastZDouble = GameVariableAccessShim.zCoordDouble();
        TextureAtlas textureAtlas = this.master.getWaypointManager().getTextureAtlas();
        GLUtils.disp2(textureAtlas.getGlId());
        GLShim.glEnable(GL11.GL_BLEND);
        GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GLShim.glDisable(GL11.GL_DEPTH_TEST);
        Waypoint highlightedPoint = this.waypointManager.getHighlightedWaypoint();

        for (Waypoint pt : this.waypointManager.getWaypoints()) {
            if (pt.isActive() || pt == highlightedPoint) {
                double distanceSq = pt.getDistanceSqToEntity(VoxelConstants.getMinecraft().getCameraEntity());
                if (distanceSq < (this.options.maxWaypointDisplayDistance * this.options.maxWaypointDisplayDistance) || this.options.maxWaypointDisplayDistance < 0 || pt == highlightedPoint) {
                    this.drawWaypoint(matrixStack, pt, textureAtlas, x, y, scScale, lastXDouble, lastZDouble, null, null, null, null);
                }
            }
        }

        if (highlightedPoint != null) {
            this.drawWaypoint(matrixStack, highlightedPoint, textureAtlas, x, y, scScale, lastXDouble, lastZDouble, textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png"), 1.0F, 0.0F, 0.0F);
        }

        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawWaypoint(MatrixStack matrixStack, Waypoint pt, TextureAtlas textureAtlas, int x, int y, int scScale, double lastXDouble, double lastZDouble, Sprite icon, Float r, Float g, Float b) {
        boolean uprightIcon = icon != null;
        if (r == null) {
            r = pt.red;
        }

        if (g == null) {
            g = pt.green;
        }

        if (b == null) {
            b = pt.blue;
        }

        double wayX = lastXDouble - pt.getX() - 0.5;
        double wayY = lastZDouble - pt.getZ() - 0.5;
        float locate = (float) Math.toDegrees(Math.atan2(wayX, wayY));
        double hypot = Math.sqrt(wayX * wayX + wayY * wayY);
        boolean far;
        if (this.options.rotates) {
            locate += this.direction;
        } else {
            locate -= this.northRotate;
        }

        hypot /= this.zoomScaleAdjusted;
        if (this.options.squareMap) {
            double radLocate = Math.toRadians(locate);
            double dispX = hypot * Math.cos(radLocate);
            double dispY = hypot * Math.sin(radLocate);
            far = Math.abs(dispX) > 28.5 || Math.abs(dispY) > 28.5;
            if (far) {
                hypot = hypot / Math.max(Math.abs(dispX), Math.abs(dispY)) * 30.0;
            }
        } else {
            far = hypot >= 31.0;
            if (far) {
                hypot = 34.0;
            }
        }

        boolean target = false;
        if (far) {
            try {
                if (icon == null) {
                    if (scScale >= 3) {
                        icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/marker" + pt.imageSuffix + ".png");
                    } else {
                        icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/marker" + pt.imageSuffix + "Small.png");
                    }

                    if (icon == textureAtlas.getMissingImage()) {
                        if (scScale >= 3) {
                            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/marker.png");
                        } else {
                            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/markerSmall.png");
                        }
                    }
                } else {
                    target = true;
                }

                matrixStack.push();
                GLShim.glColor4f(r, g, b, !pt.enabled && !target ? 0.3F : 1.0F);
                matrixStack.translate(x, y, 0.0);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-locate));
                if (uprightIcon) {
                    matrixStack.translate(0.0, -hypot, 0.0);
                    matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(locate));
                    matrixStack.translate(-x, -y, 0.0);
                } else {
                    matrixStack.translate(-x, -y, 0.0);
                    matrixStack.translate(0.0, -hypot, 0.0);
                }

                RenderSystem.applyModelViewMatrix();
                GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GLUtils.drawPre();
                GLUtils.setMap(icon, x, y, 16.0F);
                GLUtils.drawPost();
            } catch (Exception var40) {
                this.error = "Error: marker overlay not found!";
            } finally {
                matrixStack.pop();
                RenderSystem.applyModelViewMatrix();
            }
        } else {
            try {
                if (icon == null) {
                    if (scScale >= 3) {
                        icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
                    } else {
                        icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + "Small.png");
                    }

                    if (icon == textureAtlas.getMissingImage()) {
                        if (scScale >= 3) {
                            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
                        } else {
                            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypointSmall.png");
                        }
                    }
                } else {
                    target = true;
                }

                matrixStack.push();
                GLShim.glColor4f(r, g, b, !pt.enabled && !target ? 0.3F : 1.0F);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-locate));
                matrixStack.translate(0.0, -hypot, 0.0);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-(-locate)));
                RenderSystem.applyModelViewMatrix();
                GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GLUtils.drawPre();
                GLUtils.setMap(icon, x, y, 16.0F);
                GLUtils.drawPost();
            } catch (Exception var42) {
                this.error = "Error: waypoint overlay not found!";
            } finally {
                matrixStack.pop();
                RenderSystem.applyModelViewMatrix();
            }
        }

    }

    private void drawArrow(MatrixStack matrixStack, int x, int y) {
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            matrixStack.push();
            GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GLUtils.img2(this.arrowResourceLocation);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            matrixStack.translate(x, y, 0.0);
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.options.rotates && !this.fullscreenMap ? 0.0F : this.direction + this.northRotate));
            matrixStack.translate(-x, -y, 0.0);
            RenderSystem.applyModelViewMatrix();
            GLUtils.drawPre();
            GLUtils.setMap(x, y, 16);
            GLUtils.drawPost();
        } catch (Exception var8) {
            this.error = "Error: minimap arrow not found!";
        } finally {
            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
        }

    }

    private void renderMapFull(MatrixStack matrixStack, int scWidth, int scHeight) {
        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].write();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        GLUtils.disp2(this.mapImages[this.zoom].getIndex());
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        matrixStack.push();
        matrixStack.translate(scWidth / 2.0F, scHeight / 2.0F, -0.0);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.northRotate));
        matrixStack.translate(-(scWidth / 2.0F), -(scHeight / 2.0F), -0.0);
        RenderSystem.applyModelViewMatrix();
        GLShim.glDisable(GL11.GL_DEPTH_TEST);
        GLUtils.drawPre();
        int left = scWidth / 2 - 128;
        int top = scHeight / 2 - 128;
        GLUtils.ldrawone(left, top + 256, 160.0, 0.0F, 1.0F);
        GLUtils.ldrawone(left + 256, top + 256, 160.0, 1.0F, 1.0F);
        GLUtils.ldrawone(left + 256, top, 160.0, 1.0F, 0.0F);
        GLUtils.ldrawone(left, top, 160.0, 0.0F, 0.0F);
        GLUtils.drawPost();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        if (this.options.biomeOverlay != 0) {
            double factor = Math.pow(2.0, 3 - this.zoom);
            int minimumSize = (int) Math.pow(2.0, this.zoom);
            minimumSize *= minimumSize;
            ArrayList<AbstractMapData.BiomeLabel> labels = this.mapData[this.zoom].getBiomeLabels();
            GLShim.glDisable(GL11.GL_DEPTH_TEST);
            matrixStack.push();
            matrixStack.translate(0.0, 0.0, 1160.0);
            RenderSystem.applyModelViewMatrix();

            for (AbstractMapData.BiomeLabel o : labels) {
                if (o.segmentSize > minimumSize) {
                    String name = o.name;
                    int nameWidth = this.chkLen(name);
                    float x = (float) (o.x * factor);
                    float z = (float) (o.z * factor);
                    if (this.options.oldNorth) {
                        this.write(matrixStack, name, (left + 256) - z - (nameWidth / 2), top + x - 3.0F, 16777215);
                    } else {
                        this.write(matrixStack, name, left + x - (nameWidth / 2), top + z - 3.0F, 16777215);
                    }
                }
            }

            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
            GLShim.glEnable(GL11.GL_DEPTH_TEST);
        }

    }

    private void drawSquareMapFrame(int x, int y) {
        try {
            GLUtils.disp2(this.mapImageInt);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10496);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10496);
            GLUtils.drawPre();
            GLUtils.setMap(x, y, 128);
            GLUtils.drawPost();
        } catch (Exception var4) {
            this.error = "error: minimap overlay not found!";
        }

    }

    private void loadMapImage() {
        if (this.mapImageInt != -1) {
            GLUtils.glah(this.mapImageInt);
        }

        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier("voxelmap", "images/squaremap.png")).get().getInputStream();
            BufferedImage mapImage = ImageIO.read(is);
            is.close();
            this.mapImageInt = GLUtils.tex(mapImage);
        } catch (Exception var8) {
            try {
                InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier("textures/map/map_background.png")).get().getInputStream();
                Image tpMap = ImageIO.read(is);
                is.close();
                BufferedImage mapImage = new BufferedImage(tpMap.getWidth(null), tpMap.getHeight(null), 2);
                Graphics2D gfx = mapImage.createGraphics();
                gfx.drawImage(tpMap, 0, 0, null);
                int border = mapImage.getWidth() * 8 / 128;
                gfx.setComposite(AlphaComposite.Clear);
                gfx.fillRect(border, border, mapImage.getWidth() - border * 2, mapImage.getHeight() - border * 2);
                gfx.dispose();
                this.mapImageInt = GLUtils.tex(mapImage);
            } catch (Exception var7) {
                VoxelConstants.getLogger().warn("Error loading texture pack's map image: " + var7.getLocalizedMessage());
            }
        }

    }

    private void drawRoundMapFrame(int x, int y) {
        try {
            GLUtils.img2(this.roundmapResourceLocation);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GLUtils.drawPre();
            GLUtils.setMap(x, y, 128);
            GLUtils.drawPost();
        } catch (Exception var4) {
            this.error = "Error: minimap overlay not found!";
        }

    }

    private void drawDirections(MatrixStack matrixStack, int x, int y) {
        boolean unicode = VoxelConstants.getMinecraft().options.getForceUnicodeFont().getValue();
        float scale = unicode ? 0.65F : 0.5F;
        float rotate;
        if (this.options.rotates) {
            rotate = -this.direction - 90.0F - this.northRotate;
        } else {
            rotate = -90.0F;
        }

        float distance;
        if (this.options.squareMap) {
            if (this.options.rotates) {
                float tempdir = this.direction % 90.0F;
                tempdir = 45.0F - Math.abs(45.0F - tempdir);
                distance = (float) (33.5 / scale / Math.cos(Math.toRadians(tempdir)));
            } else {
                distance = 33.5F / scale;
            }
        } else {
            distance = 32.0F / scale;
        }

        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate - 90.0))), distance * Math.cos(Math.toRadians(-(rotate - 90.0))), 100.0);
        this.write(matrixStack, "N", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-rotate)), distance * Math.cos(Math.toRadians(-rotate)), 10.0);
        this.write(matrixStack, "E", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate + 90.0))), distance * Math.cos(Math.toRadians(-(rotate + 90.0))), 10.0);
        this.write(matrixStack, "S", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate + 180.0))), distance * Math.cos(Math.toRadians(-(rotate + 180.0))), 10.0);
        this.write(matrixStack, "W", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
    }

    private void showCoords(MatrixStack matrixStack, int x, int y) {
        int textStart;
        if (y > this.scHeight - 37 - 32 - 4 - 15) {
            textStart = y - 32 - 4 - 9;
        } else {
            textStart = y + 32 + 4;
        }

        if (!this.options.hide && !this.fullscreenMap) {
            boolean unicode = VoxelConstants.getMinecraft().options.getForceUnicodeFont().getValue();
            float scale = unicode ? 0.65F : 0.5F;
            matrixStack.push();
            matrixStack.scale(scale, scale, 1.0F);
            String xy = this.dCoord(GameVariableAccessShim.xCoord()) + ", " + this.dCoord(GameVariableAccessShim.zCoord());
            int m = this.chkLen(xy) / 2;
            this.write(matrixStack, xy, x / scale - m, textStart / scale, 16777215); //X, Z
            xy = Integer.toString(GameVariableAccessShim.yCoord());
            m = this.chkLen(xy) / 2;
            this.write(matrixStack, xy, x / scale - m, textStart / scale + 10.0F, 16777215); //Y
            if (this.ztimer > 0) {
                m = this.chkLen(this.error) / 2;
                this.write(matrixStack, this.error, x / scale - m, textStart / scale + 19.0F, 16777215); //WORLD NAME
            }

            matrixStack.pop();
        } else {
            int heading = (int) (this.direction + this.northRotate);
            if (heading > 360) {
                heading -= 360;
            }
            String ns = "";
            String ew = "";
            if (heading > 360 - 60 || heading <= 60) {
                ns = "N";
            } else if (heading > 180 - 60 && heading <= 180 + 60) {
                ns = "S";
            }
            if (heading > 90 - 60 && heading <= 90 + 60) {
                ew = "E";
            } else if (heading > 270 - 60 && heading <= 270 + 60) {
                ew = "W";
            }

            String stats = "(" + this.dCoord(GameVariableAccessShim.xCoord()) + ", " + GameVariableAccessShim.yCoord() + ", " + this.dCoord(GameVariableAccessShim.zCoord()) + ") " + heading + "' " + ns + ew;
            int m = this.chkLen(stats) / 2;
            this.write(matrixStack, stats, (this.scWidth / 2 - m), 5.0F, 16777215);
            if (this.ztimer > 0) {
                m = this.chkLen(this.error) / 2;
                this.write(matrixStack, this.error, (this.scWidth / 2 - m), 15.0F, 16777215);
            }
        }

    }

    private String dCoord(int paramInt1) {
        if (paramInt1 < 0) {
            return "-" + Math.abs(paramInt1);
        } else {
            return paramInt1 > 0 ? "+" + paramInt1 : " " + paramInt1;
        }
    }

    private int chkLen(String string) {
        return this.fontRenderer.getWidth(string);
    }

    private void write(MatrixStack matrixStack, String text, float x, float y, int color) {
        this.fontRenderer.drawWithShadow(matrixStack, text, x, y, color);
    }

    private int chkLen(Text text) {
        return this.fontRenderer.getWidth(text);
    }

    private void write(MatrixStack matrixStack, Text text, float x, float y, int color) {
        this.fontRenderer.drawWithShadow(matrixStack, text, x, y, color);
    }

    private void drawWelcomeScreen(MatrixStack matrixStack, int scWidth, int scHeight) {
        if (this.welcomeText[1] == null || this.welcomeText[1].getString().equals("minimap.ui.welcome2")) {
            String zmodver = "v1.11.10";
            this.welcomeText[0] = (Text.literal("")).append((Text.literal("VoxelMap! ")).formatted(Formatting.RED)).append(zmodver + " ").append(Text.translatable("minimap.ui.welcome1"));
            this.welcomeText[1] = Text.translatable("minimap.ui.welcome2");
            this.welcomeText[2] = Text.translatable("minimap.ui.welcome3");
            this.welcomeText[3] = Text.translatable("minimap.ui.welcome4");
            this.welcomeText[4] = (Text.literal("")).append((Text.keybind(this.options.keyBindZoom.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome5a")).append(", ").append((Text.keybind(this.options.keyBindMenu.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome5b"));
            this.welcomeText[5] = (Text.literal("")).append((Text.keybind(this.options.keyBindFullscreen.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome6"));
            this.welcomeText[6] = (Text.literal("")).append((Text.keybind(this.options.keyBindWaypoint.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome7"));
            this.welcomeText[7] = this.options.keyBindZoom.getBoundKeyLocalizedText().copy().append(": ").append((Text.translatable("minimap.ui.welcome8")).formatted(Formatting.GRAY));
        }

        GLShim.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int maxSize = 0;
        int border = 2;
        Text head = this.welcomeText[0];

        int height;
        for (height = 1; height < this.welcomeText.length - 1; ++height) {
            if (this.chkLen(this.welcomeText[height]) > maxSize) {
                maxSize = this.chkLen(this.welcomeText[height]);
            }
        }

        int title = this.chkLen(head);
        int centerX = (int) ((scWidth + 5) / 2.0);
        int centerY = (int) ((scHeight + 5) / 2.0);
        Text hide = this.welcomeText[this.welcomeText.length - 1];
        int footer = this.chkLen(hide);
        GLShim.glDisable(GL11.GL_TEXTURE_2D);
        GLShim.glColor4f(0.0F, 0.0F, 0.0F, 0.7F);
        double leftX = centerX - title / 2.0 - border;
        double rightX = centerX + title / 2.0 + border;
        double topY = centerY - (height - 1) / 2.0 * 10.0 - border - 20.0;
        double botY = centerY - (height - 1) / 2.0 * 10.0 + border - 10.0;
        this.drawBox(leftX, rightX, topY, botY);
        leftX = centerX - maxSize / 2.0 - border;
        rightX = centerX + maxSize / 2.0 + border;
        topY = centerY - (height - 1) / 2.0 * 10.0 - border;
        botY = centerY + (height - 1) / 2.0 * 10.0 + border;
        this.drawBox(leftX, rightX, topY, botY);
        leftX = centerX - footer / 2.0 - border;
        rightX = centerX + footer / 2.0 + border;
        topY = centerY + (height - 1) / 2.0 * 10.0 - border + 10.0;
        botY = centerY + (height - 1) / 2.0 * 10.0 + border + 20.0;
        this.drawBox(leftX, rightX, topY, botY);
        GLShim.glEnable(GL11.GL_TEXTURE_2D);
        this.write(matrixStack, head, (centerX - title / 2), (centerY - (height - 1) * 10 / 2 - 19), 16777215);

        for (int n = 1; n < height; ++n) {
            this.write(matrixStack, this.welcomeText[n], (centerX - maxSize / 2), (centerY - (height - 1) * 10 / 2 + n * 10 - 9), 16777215);
        }

        this.write(matrixStack, hide, (centerX - footer / 2), ((scHeight + 5) / 2 + (height - 1) * 10 / 2 + 11), 16777215);
    }

    private void drawBox(double leftX, double rightX, double topY, double botY) {
        GLUtils.drawPre(VertexFormats.POSITION);
        GLUtils.ldrawtwo(leftX, botY, 0.0);
        GLUtils.ldrawtwo(rightX, botY, 0.0);
        GLUtils.ldrawtwo(rightX, topY, 0.0);
        GLUtils.ldrawtwo(leftX, topY, 0.0);
        GLUtils.drawPost();
    }
}
