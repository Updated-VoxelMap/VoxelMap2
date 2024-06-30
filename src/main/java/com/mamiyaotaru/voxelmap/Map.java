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
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.MapChunkCache;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.MutableBlockPosCache;
import com.mamiyaotaru.voxelmap.util.MutableNativeImageBackedTexture;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.ScaledMutableNativeImageBackedTexture;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.OutOfMemoryScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.resource.language.I18n;
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
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30C;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
    private final Identifier arrowResourceLocation = Identifier.of("voxelmap", "images/mmarrow.png");
    private final Identifier roundmapResourceLocation = Identifier.of("voxelmap", "images/roundmap.png");
    private final Identifier squareStencil = Identifier.of("voxelmap", "images/square.png");
    private final Identifier circleStencil = Identifier.of("voxelmap", "images/circle.png");
    private ClientWorld world;
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
    // private MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    // private final MutableBlockPos tempBlockPos = new MutableBlockPos(0, 0, 0);
    private BlockState transparentBlockState;
    private BlockState surfaceBlockState;
    private boolean imageChanged = true;
    private NativeImageBackedTexture lightmapTexture;
    private boolean needLightmapRefresh = true;
    private int tickWithLightChange;
    private boolean lastPaused = true;
    private double lastGamma;
    private float lastSunBrightness;
    private float lastLightning;
    private float lastPotion;
    private final int[] lastLightmapValues = {-16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216};
    private boolean lastBeneathRendering;
    private boolean needSkyColor;
    private boolean lastAboveHorizon = true;
    private int lastBiome;
    private int lastSkyColor;
    private final Random generator = new Random();
    private boolean showWelcomeScreen;
    private Screen lastGuiScreen;
    private boolean fullscreenMap;
    private int zoom;
    private int scWidth;
    private int scHeight;
    private String error = "";
    private final Text[] welcomeText = new Text[8];
    private int ztimer;
    private int heightMapFudge;
    private int timer;
    private boolean doFullRender = true;
    private boolean zoomChanged;
    private int lastX;
    private int lastZ;
    private int lastY;
    private int lastImageX;
    private int lastImageZ;
    private boolean lastFullscreen;
    private float direction;
    private float percentX;
    private float percentY;
    private int northRotate;
    private Thread zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
    private int zCalcTicker;
    private final TextRenderer fontRenderer;
    private final int[] lightmapColors = new int[256];
    private double zoomScale = 1.0;
    private double zoomScaleAdjusted = 1.0;
    private int mapImageInt = -1;
    private static double minTablistOffset;
    private static float statusIconOffset = 0.0F;

    public Map() {
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
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

        OpenGL.Utils.setupFramebuffer();
        this.fontRenderer = VoxelConstants.getMinecraft().textRenderer;
        this.zoom = this.options.zoom;
        this.setZoomScale();
    }

    public void forceFullRender(boolean forceFullRender) {
        this.doFullRender = forceFullRender;
        VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
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
                if (this.world != null) {
                    if (!this.options.hide && this.options.minimapAllowed) {
                        try {
                            this.mapCalc(this.doFullRender);
                            if (!this.doFullRender) {
                                MutableBlockPos blockPos = MutableBlockPosCache.get();
                                this.chunkCache[this.zoom].centerChunks(blockPos.withXYZ(this.lastX, 0, this.lastZ));
                                MutableBlockPosCache.release(blockPos);
                                this.chunkCache[this.zoom].checkIfChunksChanged();
                            }
                        } catch (Exception exception) {
                            VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread", exception);
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
                        VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread", exception);
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
        VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    public void newWorldName() {
        String subworldName = this.waypointManager.getCurrentSubworldDescriptor(true);
        StringBuilder subworldNameBuilder = (new StringBuilder("§r")).append(I18n.translate("worldmap.multiworld.newworld")).append(":").append(" ");
        if (subworldName.isEmpty() && this.waypointManager.isMultiworld()) {
            subworldNameBuilder.append("???");
        } else if (!subworldName.isEmpty()) {
            subworldNameBuilder.append(subworldName);
        }

        this.error = subworldNameBuilder.toString();
    }

    public void onTickInGame(DrawContext drawContext) {
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

            VoxelConstants.getMinecraft().setScreen(new GuiPersistentMap(null));
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindWaypointMenu.wasPressed()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }
            if (VoxelMap.mapOptions.waypointsAllowed) {
                VoxelConstants.getMinecraft().setScreen(new GuiWaypoints(null));
            }
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindWaypoint.wasPressed()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }

            if (VoxelMap.mapOptions.waypointsAllowed) {
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
                dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().getWorld()));
                double dimensionScale = VoxelConstants.getPlayer().getWorld().getDimension().coordinateScale();
                Waypoint newWaypoint = new Waypoint("", (int) (GameVariableAccessShim.xCoord() * dimensionScale), (int) (GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "",
                        VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
                VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(null, newWaypoint, false));
            }
        }

        if (VoxelConstants.getMinecraft().currentScreen == null && this.options.keyBindMobToggle.wasPressed()) {
            VoxelConstants.getVoxelMapInstance().getRadarOptions().setOptionValue(EnumOptionsMinimap.SHOWRADAR);
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
                this.error = I18n.translate("minimap.ui.zoomlevel") + " (0.25x)";
            } else if (this.zoom == 3) {
                this.error = I18n.translate("minimap.ui.zoomlevel") + " (0.5x)";
            } else if (this.zoom == 2) {
                this.error = I18n.translate("minimap.ui.zoomlevel") + " (1.0x)";
            } else if (this.zoom == 1) {
                this.error = I18n.translate("minimap.ui.zoomlevel") + " (2.0x)";
            } else {
                this.error = I18n.translate("minimap.ui.zoomlevel") + " (4.0x)";
            }
        }

        this.checkForChanges();
        if (VoxelMap.mapOptions.deathWaypointAllowed && VoxelConstants.getMinecraft().currentScreen instanceof DeathScreen && !(this.lastGuiScreen instanceof DeathScreen)) {
            this.waypointManager.handleDeath();
        }

        this.lastGuiScreen = VoxelConstants.getMinecraft().currentScreen;
        this.calculateCurrentLightAndSkyColor();
        if (this.threading) {
            if (!this.zCalc.isAlive()) {
                this.zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
                this.zCalc.start();
                this.zCalcTicker = 0;
            }

            if (!(VoxelConstants.getMinecraft().currentScreen instanceof DeathScreen) && !(VoxelConstants.getMinecraft().currentScreen instanceof OutOfMemoryScreen)) {
                ++this.zCalcTicker;
                if (this.zCalcTicker > 2000) {
                    this.zCalcTicker = 0;
                    Exception ex = new Exception();
                    ex.setStackTrace(this.zCalc.getStackTrace());
                    DebugRenderState.print();
                    VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread is hanging?", ex);
                    // this.zCalc.stop();
                } // else {
                synchronized (this.zCalc) {
                    this.zCalc.notify();
                }
                // }
            }
        } else {
            if (!this.options.hide && this.options.minimapAllowed && this.world != null) {
                this.mapCalc(this.doFullRender);
                if (!this.doFullRender) {
                    MutableBlockPos blockPos = MutableBlockPosCache.get();
                    this.chunkCache[this.zoom].centerChunks(blockPos.withXYZ(this.lastX, 0, this.lastZ));
                    MutableBlockPosCache.release(blockPos);
                    this.chunkCache[this.zoom].checkIfChunksChanged();
                }
            }

            this.doFullRender = false;
        }

        boolean enabled = !VoxelConstants.getMinecraft().options.hudHidden && (this.options.showUnderMenus || VoxelConstants.getMinecraft().currentScreen == null) && !VoxelConstants.getMinecraft().getDebugHud().shouldShowDebugHud();

        this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

        while (this.direction >= 360.0F) {
            this.direction -= 360.0F;
        }

        while (this.direction < 0.0F) {
            this.direction += 360.0F;
        }

        if (!this.error.isEmpty() && this.ztimer == 0) {
            this.ztimer = 500;
        }

        if (this.ztimer > 0) {
            --this.ztimer;
        }

        if (this.ztimer == 0 && !this.error.isEmpty()) {
            this.error = "";
        }

        if (enabled && VoxelMap.mapOptions.minimapAllowed) {
            this.drawMinimap(drawContext);
        }

        this.timer = this.timer > 5000 ? 0 : this.timer + 1;
    }

    private void cycleZoomLevel() {
        if (this.options.zoom == 4) {
            this.options.zoom = 3;
            this.error = I18n.translate("minimap.ui.zoomlevel") + " (0.5x)";
        } else if (this.options.zoom == 3) {
            this.options.zoom = 2;
            this.error = I18n.translate("minimap.ui.zoomlevel") + " (1.0x)";
        } else if (this.options.zoom == 2) {
            this.options.zoom = 1;
            this.error = I18n.translate("minimap.ui.zoomlevel") + " (2.0x)";
        } else if (this.options.zoom == 1) {
            this.options.zoom = 0;
            this.error = I18n.translate("minimap.ui.zoomlevel") + " (4.0x)";
        } else if (this.options.zoom == 0) {
            if (this.multicore && VoxelConstants.getMinecraft().options.getViewDistance().getValue() > 8) {
                this.options.zoom = 4;
                this.error = I18n.translate("minimap.ui.zoomlevel") + " (0.25x)";
            } else {
                this.options.zoom = 3;
                this.error = I18n.translate("minimap.ui.zoomlevel") + " (0.5x)";
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
                if (this.needLightmapRefresh && VoxelConstants.getElapsedTicks() != this.tickWithLightChange && !VoxelConstants.getMinecraft().isPaused() || this.options.realTimeTorches) {
                    OpenGL.Utils.disp(this.lightmapTexture.getGlId());
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder());
                    OpenGL.glGetTexImage(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_RGBA, OpenGL.GL11_GL_UNSIGNED_BYTE, byteBuffer);

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

                float sunBrightness = this.world.getSkyBrightness(1.0F);
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
                    this.tickWithLightChange = VoxelConstants.getElapsedTicks();
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

                MutableBlockPos blockPos = MutableBlockPosCache.get();
                int biomeID = this.world.getRegistryManager().get(RegistryKeys.BIOME).getRawId(this.world.getBiome(blockPos.withXYZ(GameVariableAccessShim.xCoord(), GameVariableAccessShim.yCoord(), GameVariableAccessShim.zCoord())).value());
                MutableBlockPosCache.release(blockPos);
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
        OpenGL.glGetFloatv(OpenGL.GL11_GL_COLOR_CLEAR_VALUE, temp);
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

    public void drawMinimap(DrawContext drawContext) {
        this.write(drawContext, "hey, was geht?", 0, -20.0F, 16777215);
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
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_DISTANCE);
        Matrix4fStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.pushMatrix();
        modelViewMatrixStack.identity();
        modelViewMatrixStack.translate(0.0f, 0.0f, -2000.0f);
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

        float statusIconOffset = 0.0F;
        if (VoxelMap.mapOptions.moveMapDownWhileStatusEffect) {
            if (this.options.mapCorner == 1 && !VoxelConstants.getPlayer().getStatusEffects().isEmpty()) {

                for (StatusEffectInstance statusEffectInstance : VoxelConstants.getPlayer().getStatusEffects()) {
                    if (statusEffectInstance.shouldShowIcon()) {
                        if (statusEffectInstance.getEffectType().value().isBeneficial()) {
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
        }
        Map.statusIconOffset = statusIconOffset;

        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, 0);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (!this.options.hide) {
            if (this.fullscreenMap) {
                this.renderMapFull(drawContext, this.scWidth, this.scHeight);
            } else {
                this.renderMap(modelViewMatrixStack, mapX, mapY, scScale);
            }

            OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
            if (VoxelConstants.getVoxelMapInstance().getRadar() != null && !this.fullscreenMap) {
                this.layoutVariables.updateVars(scScale, mapX, mapY, this.zoomScale, this.zoomScaleAdjusted);
                VoxelConstants.getVoxelMapInstance().getRadar().onTickInGame(drawContext, modelViewMatrixStack, this.layoutVariables);
            }

            if (!this.fullscreenMap) {
                this.drawDirections(drawContext, mapX, mapY);
            }

            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            if (this.fullscreenMap) {
                this.drawArrow(modelViewMatrixStack, this.scWidth / 2, this.scHeight / 2);
            } else {
                this.drawArrow(modelViewMatrixStack, mapX, mapY);
            }
        }

        if (this.options.coords) {
            this.showCoords(drawContext, mapX, mapY);
        }

        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        modelViewMatrixStack.popMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.applyModelViewMatrix();
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        VoxelConstants.getMinecraft().textRenderer.getClass();
        VertexConsumerProvider.Immediate vertexConsumerProvider = VoxelConstants.getMinecraft().getBufferBuilders().getEntityVertexConsumers();
        VoxelConstants.getMinecraft().textRenderer.draw(Text.literal("******sdkfjhsdkjfhsdkjfh"), 100.0F, 100.0F, -1, true, matrix4f, vertexConsumerProvider, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
        if (this.showWelcomeScreen) {
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            this.drawWelcomeScreen(drawContext, VoxelConstants.getMinecraft().getWindow().getScaledWidth(), VoxelConstants.getMinecraft().getWindow().getScaledHeight());
        }

        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
        OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
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
            VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }

    }

    private void mapCalc(boolean full) {
        int currentX = GameVariableAccessShim.xCoord();
        int currentZ = GameVariableAccessShim.zCoord();
        int currentY = GameVariableAccessShim.yCoord();
        int offsetX = currentX - this.lastX;
        int offsetZ = currentZ - this.lastZ;
        int offsetY = currentY - this.lastY;
        int zoom = this.zoom;
        int multi = (int) Math.pow(2.0, zoom);
        ClientWorld world = this.world;
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
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getTopY() - 1), world.getBottomY()), this.lastZ);
        if (VoxelConstants.getPlayer().getWorld().getDimension().hasCeiling()) {

            netherPlayerInOpen = world.getChunk(blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (VoxelConstants.getClientWorld().getDimensionEffects().shouldBrightenLighting() && !VoxelConstants.getClientWorld().getDimension().hasSkyLight()) {
            boolean endPlayerInOpen = world.getChunk(blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && world.getLightLevel(LightType.SKY, blockPos) <= 0) {
            caves = true;
        }
        MutableBlockPosCache.release(blockPos);

        boolean beneathRendering = caves || nether;
        if (this.lastBeneathRendering != beneathRendering) {
            full = true;
        }

        this.lastBeneathRendering = beneathRendering;
        needHeightAndID = needHeightMap && (nether || caves);
        int color24;
        synchronized (this.coordinateLock) {
            if (!full) {
                this.mapImages[zoom].moveY(offsetZ);
                this.mapImages[zoom].moveX(offsetX);
            }

            this.lastX = currentX;
            this.lastZ = currentZ;
        }
        int startX = currentX - 16 * multi;
        int startZ = currentZ - 16 * multi;
        if (!full) {
            this.mapData[zoom].moveZ(offsetZ);
            this.mapData[zoom].moveX(offsetX);

            for (int imageY = offsetZ > 0 ? 32 * multi - 1 : -offsetZ - 1; imageY >= (offsetZ > 0 ? 32 * multi - offsetZ : 0); --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }

            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = offsetX > 0 ? 32 * multi - offsetX : 0; imageX < (offsetX > 0 ? 32 * multi : -offsetX); ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if (full || this.options.heightmap && needHeightMap || needHeightAndID || this.options.lightmap && needLight || skyColorChanged) {
            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(full, full || needHeightAndID, full, full || needLight || needHeightAndID, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if ((full || offsetX != 0 || offsetZ != 0 || !this.lastFullscreen) && this.fullscreenMap && this.options.biomeOverlay != 0) {
            this.mapData[zoom].segmentBiomes();
            this.mapData[zoom].findCenterOfSegments(!this.options.oldNorth);
        }

        this.lastFullscreen = this.fullscreenMap;
        if (full || offsetX != 0 || offsetZ != 0 || needHeightMap || needLight || skyColorChanged) {
            this.imageChanged = true;
        }

        if (needLight || skyColorChanged) {
            VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
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
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getTopY()), world.getBottomY()), this.lastZ);
        int currentY = GameVariableAccessShim.yCoord();
        if (VoxelConstants.getPlayer().getWorld().getDimension().hasCeiling()) {
            netherPlayerInOpen = this.world.getChunk(blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (VoxelConstants.getClientWorld().getDimensionEffects().shouldBrightenLighting() && !VoxelConstants.getClientWorld().getDimension().hasSkyLight()) {
            boolean endPlayerInOpen = this.world.getChunk(blockPos).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && this.world.getLightLevel(LightType.SKY, blockPos) <= 0) {
            caves = true;
        }
        MutableBlockPosCache.release(blockPos);

        int zoom = this.zoom;
        int startX = this.lastX;
        int startZ = this.lastZ;
        ClientWorld world = this.world;
        int multi = (int) Math.pow(2.0, zoom);
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
                color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                this.mapImages[zoom].setRGB(imageX, imageY, color24);
            }
        }

        this.imageChanged = true;
    }

    private int getPixelColor(boolean needBiome, boolean needHeightAndID, boolean needTint, boolean needLight, boolean nether, boolean caves, ClientWorld world, int zoom, int multi, int startX, int startZ, int imageX, int imageY) {
        int surfaceHeight = Short.MIN_VALUE;
        int seafloorHeight = Short.MIN_VALUE;
        int transparentHeight = Short.MIN_VALUE;
        int foliageHeight = Short.MIN_VALUE;
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
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        MutableBlockPos tempBlockPos = MutableBlockPosCache.get();
        blockPos.withXYZ(startX + imageX, 64, startZ + imageY);
        int color24;
        Biome biome;
        if (needBiome) {
            if (world.isChunkLoaded(blockPos)) {
                biome = world.getBiome(blockPos).value();
            } else {
                biome = null;
            }

            this.mapData[zoom].setBiome(imageX, imageY, biome);
        } else {
            biome = this.mapData[zoom].getBiome(imageX, imageY);
        }

        if (this.options.biomeOverlay == 1) {
            if (biome != null) {
                color24 = BiomeRepository.getBiomeColor(biome) | 0xFF000000;
            } else {
                color24 = 0;
            }

        } else {
            boolean solid = false;
            if (needHeightAndID) {
                if (!nether && !caves) {
                    WorldChunk chunk = world.getWorldChunk(blockPos);
                    transparentHeight = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) + 1;
                    this.transparentBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY));
                    FluidState fluidState = this.transparentBlockState.getFluidState();
                    if (fluidState != Fluids.EMPTY.getDefaultState()) {
                        this.transparentBlockState = fluidState.getBlockState();
                    }

                    surfaceHeight = transparentHeight;
                    this.surfaceBlockState = this.transparentBlockState;
                    VoxelShape voxelShape;
                    boolean hasOpacity = this.surfaceBlockState.getOpacity(world, blockPos) > 0;
                    if (!hasOpacity && this.surfaceBlockState.isOpaque() && this.surfaceBlockState.hasSidedTransparency()) {
                        voxelShape = this.surfaceBlockState.getCullingFace(world, blockPos, Direction.DOWN);
                        hasOpacity = VoxelShapes.unionCoversFullCube(voxelShape, VoxelShapes.empty());
                        voxelShape = this.surfaceBlockState.getCullingFace(world, blockPos, Direction.UP);
                        hasOpacity = hasOpacity || VoxelShapes.unionCoversFullCube(VoxelShapes.empty(), voxelShape);
                    }

                    while (!hasOpacity && surfaceHeight > world.getBottomY()) {
                        foliageBlockState = this.surfaceBlockState;
                        --surfaceHeight;
                        this.surfaceBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                        fluidState = this.surfaceBlockState.getFluidState();
                        if (fluidState != Fluids.EMPTY.getDefaultState()) {
                            this.surfaceBlockState = fluidState.getBlockState();
                        }

                        hasOpacity = this.surfaceBlockState.getOpacity(world, blockPos) > 0;
                        if (!hasOpacity && this.surfaceBlockState.isOpaque() && this.surfaceBlockState.hasSidedTransparency()) {
                            voxelShape = this.surfaceBlockState.getCullingFace(world, blockPos, Direction.DOWN);
                            hasOpacity = VoxelShapes.unionCoversFullCube(voxelShape, VoxelShapes.empty());
                            voxelShape = this.surfaceBlockState.getCullingFace(world, blockPos, Direction.UP);
                            hasOpacity = hasOpacity || VoxelShapes.unionCoversFullCube(VoxelShapes.empty(), voxelShape);
                        }
                    }

                    if (surfaceHeight == transparentHeight) {
                        transparentHeight = Short.MIN_VALUE;
                        this.transparentBlockState = BlockRepository.air.getDefaultState();
                        foliageBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight, startZ + imageY));
                    }

                    if (foliageBlockState.getBlock() == Blocks.SNOW) {
                        this.surfaceBlockState = foliageBlockState;
                        foliageBlockState = BlockRepository.air.getDefaultState();
                    }

                    if (foliageBlockState == this.transparentBlockState) {
                        foliageBlockState = BlockRepository.air.getDefaultState();
                    }

                    if (foliageBlockState != null && !(foliageBlockState.getBlock() instanceof AirBlock)) {
                        foliageHeight = surfaceHeight + 1;
                    } else {
                        foliageHeight = Short.MIN_VALUE;
                    }

                    Block material = this.surfaceBlockState.getBlock();
                    if (material == Blocks.WATER || material == Blocks.ICE) {
                        seafloorHeight = surfaceHeight;

                        for (seafloorBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY)); seafloorBlockState.getOpacity(world, blockPos) < 5 && !(seafloorBlockState.getBlock() instanceof LeavesBlock)
                                && seafloorHeight > world.getBottomY() + 1; seafloorBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY))) {
                            material = seafloorBlockState.getBlock();
                            if (transparentHeight == Short.MIN_VALUE && material != Blocks.ICE && material != Blocks.WATER && seafloorBlockState.blocksMovement()) {
                                transparentHeight = seafloorHeight;
                                this.transparentBlockState = seafloorBlockState;
                            }

                            if (foliageHeight == Short.MIN_VALUE && seafloorHeight != transparentHeight && this.transparentBlockState != seafloorBlockState && material != Blocks.ICE && material != Blocks.WATER && !(material instanceof AirBlock) && material != Blocks.BUBBLE_COLUMN) {
                                foliageHeight = seafloorHeight;
                                foliageBlockState = seafloorBlockState;
                            }

                            --seafloorHeight;
                        }

                        if (seafloorBlockState.getBlock() == Blocks.WATER) {
                            seafloorBlockState = BlockRepository.air.getDefaultState();
                        }
                    }
                } else {
                    surfaceHeight = this.getNetherHeight(startX + imageX, startZ + imageY);
                    this.surfaceBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                    surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                    foliageHeight = surfaceHeight + 1;
                    blockPos.setXYZ(startX + imageX, foliageHeight - 1, startZ + imageY);
                    foliageBlockState = world.getBlockState(blockPos);
                    Block material = foliageBlockState.getBlock();
                    if (material != Blocks.SNOW && !(material instanceof AirBlock) && material != Blocks.LAVA && material != Blocks.WATER) {
                        foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                    } else {
                        foliageHeight = Short.MIN_VALUE;
                    }
                }

                surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                if (this.options.biomes && this.surfaceBlockState != this.mapData[zoom].getBlockstate(imageX, imageY)) {
                    surfaceBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setHeight(imageX, imageY, surfaceHeight);
                this.mapData[zoom].setBlockstateID(imageX, imageY, surfaceBlockStateID);
                if (this.options.biomes && this.transparentBlockState != this.mapData[zoom].getTransparentBlockstate(imageX, imageY)) {
                    transparentBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setTransparentHeight(imageX, imageY, transparentHeight);
                transparentBlockStateID = BlockRepository.getStateId(this.transparentBlockState);
                this.mapData[zoom].setTransparentBlockstateID(imageX, imageY, transparentBlockStateID);
                if (this.options.biomes && foliageBlockState != this.mapData[zoom].getFoliageBlockstate(imageX, imageY)) {
                    foliageBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setFoliageHeight(imageX, imageY, foliageHeight);
                foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                this.mapData[zoom].setFoliageBlockstateID(imageX, imageY, foliageBlockStateID);
                if (this.options.biomes && seafloorBlockState != this.mapData[zoom].getOceanFloorBlockstate(imageX, imageY)) {
                    seafloorBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setOceanFloorHeight(imageX, imageY, seafloorHeight);
                seafloorBlockStateID = BlockRepository.getStateId(seafloorBlockState);
                this.mapData[zoom].setOceanFloorBlockstateID(imageX, imageY, seafloorBlockStateID);
            } else {
                surfaceHeight = this.mapData[zoom].getHeight(imageX, imageY);
                surfaceBlockStateID = this.mapData[zoom].getBlockstateID(imageX, imageY);
                this.surfaceBlockState = BlockRepository.getStateById(surfaceBlockStateID);
                transparentHeight = this.mapData[zoom].getTransparentHeight(imageX, imageY);
                transparentBlockStateID = this.mapData[zoom].getTransparentBlockstateID(imageX, imageY);
                this.transparentBlockState = BlockRepository.getStateById(transparentBlockStateID);
                foliageHeight = this.mapData[zoom].getFoliageHeight(imageX, imageY);
                foliageBlockStateID = this.mapData[zoom].getFoliageBlockstateID(imageX, imageY);
                foliageBlockState = BlockRepository.getStateById(foliageBlockStateID);
                seafloorHeight = this.mapData[zoom].getOceanFloorHeight(imageX, imageY);
                seafloorBlockStateID = this.mapData[zoom].getOceanFloorBlockstateID(imageX, imageY);
                seafloorBlockState = BlockRepository.getStateById(seafloorBlockStateID);
            }

            if (surfaceHeight == Short.MIN_VALUE) {
                surfaceHeight = this.lastY + 1;
                solid = true;
            }

            if (this.surfaceBlockState.getBlock() == Blocks.LAVA) {
                solid = false;
            }

            if (this.options.biomes) {
                surfaceColor = this.colorManager.getBlockColor(blockPos, surfaceBlockStateID, biome);
                int tint;
                if (!needTint && !surfaceBlockChangeForcedTint) {
                    tint = this.mapData[zoom].getBiomeTint(imageX, imageY);
                } else {
                    tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, this.surfaceBlockState, surfaceBlockStateID, blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                    this.mapData[zoom].setBiomeTint(imageX, imageY, tint);
                }

                if (tint != -1) {
                    surfaceColor = ColorUtils.colorMultiplier(surfaceColor, tint);
                }
            } else {
                surfaceColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, surfaceBlockStateID);
            }

            surfaceColor = this.applyHeight(surfaceColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, surfaceHeight, solid, 1);
            int light;
            if (needLight) {
                light = this.getLight(surfaceColor, this.surfaceBlockState, world, startX + imageX, startZ + imageY, surfaceHeight, solid);
                this.mapData[zoom].setLight(imageX, imageY, light);
            } else {
                light = this.mapData[zoom].getLight(imageX, imageY);
            }

            if (light == 0) {
                surfaceColor = 0;
            } else if (light != 255) {
                surfaceColor = ColorUtils.colorMultiplier(surfaceColor, light);
            }

            if (this.options.waterTransparency && seafloorHeight != Short.MIN_VALUE) {
                if (!this.options.biomes) {
                    seafloorColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, seafloorBlockStateID);
                } else {
                    seafloorColor = this.colorManager.getBlockColor(blockPos, seafloorBlockStateID, biome);
                    int tint;
                    if (!needTint && !seafloorBlockChangeForcedTint) {
                        tint = this.mapData[zoom].getOceanFloorBiomeTint(imageX, imageY);
                    } else {
                        tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, seafloorBlockState, seafloorBlockStateID, blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                        this.mapData[zoom].setOceanFloorBiomeTint(imageX, imageY, tint);
                    }

                    if (tint != -1) {
                        seafloorColor = ColorUtils.colorMultiplier(seafloorColor, tint);
                    }
                }

                seafloorColor = this.applyHeight(seafloorColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, seafloorHeight, solid, 0);
                int seafloorLight;
                if (needLight) {
                    seafloorLight = this.getLight(seafloorColor, seafloorBlockState, world, startX + imageX, startZ + imageY, seafloorHeight, solid);
                    blockPos.setXYZ(startX + imageX, seafloorHeight, startZ + imageY);
                    BlockState blockStateAbove = world.getBlockState(blockPos);
                    Block materialAbove = blockStateAbove.getBlock();
                    if (this.options.lightmap && materialAbove == Blocks.ICE) {
                        int multiplier = VoxelConstants.getMinecraft().options.getAo().getValue() ? 200 : 120;
                        seafloorLight = ColorUtils.colorMultiplier(seafloorLight, 0xFF000000 | multiplier << 16 | multiplier << 8 | multiplier);
                    }

                    this.mapData[zoom].setOceanFloorLight(imageX, imageY, seafloorLight);
                } else {
                    seafloorLight = this.mapData[zoom].getOceanFloorLight(imageX, imageY);
                }

                if (seafloorLight == 0) {
                    seafloorColor = 0;
                } else if (seafloorLight != 255) {
                    seafloorColor = ColorUtils.colorMultiplier(seafloorColor, seafloorLight);
                }
            }

            if (this.options.blockTransparency) {
                if (transparentHeight != Short.MIN_VALUE && this.transparentBlockState != null && this.transparentBlockState != BlockRepository.air.getDefaultState()) {
                    if (this.options.biomes) {
                        transparentColor = this.colorManager.getBlockColor(blockPos, transparentBlockStateID, biome);
                        int tint;
                        if (!needTint && !transparentBlockChangeForcedTint) {
                            tint = this.mapData[zoom].getTransparentBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, this.transparentBlockState, transparentBlockStateID, blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                            this.mapData[zoom].setTransparentBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            transparentColor = ColorUtils.colorMultiplier(transparentColor, tint);
                        }
                    } else {
                        transparentColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, transparentBlockStateID);
                    }

                    transparentColor = this.applyHeight(transparentColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, transparentHeight, solid, 3);
                    int transparentLight;
                    if (needLight) {
                        transparentLight = this.getLight(transparentColor, this.transparentBlockState, world, startX + imageX, startZ + imageY, transparentHeight, solid);
                        this.mapData[zoom].setTransparentLight(imageX, imageY, transparentLight);
                    } else {
                        transparentLight = this.mapData[zoom].getTransparentLight(imageX, imageY);
                    }

                    if (transparentLight == 0) {
                        transparentColor = 0;
                    } else if (transparentLight != 255) {
                        transparentColor = ColorUtils.colorMultiplier(transparentColor, transparentLight);
                    }
                }

                if (foliageHeight != Short.MIN_VALUE && foliageBlockState != null && foliageBlockState != BlockRepository.air.getDefaultState()) {
                    if (!this.options.biomes) {
                        foliageColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, foliageBlockStateID);
                    } else {
                        foliageColor = this.colorManager.getBlockColor(blockPos, foliageBlockStateID, biome);
                        int tint;
                        if (!needTint && !foliageBlockChangeForcedTint) {
                            tint = this.mapData[zoom].getFoliageBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, foliageBlockState, foliageBlockStateID, blockPos.withXYZ(startX + imageX, foliageHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                            this.mapData[zoom].setFoliageBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            foliageColor = ColorUtils.colorMultiplier(foliageColor, tint);
                        }
                    }

                    foliageColor = this.applyHeight(foliageColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, foliageHeight, solid, 2);
                    int foliageLight;
                    if (needLight) {
                        foliageLight = this.getLight(foliageColor, foliageBlockState, world, startX + imageX, startZ + imageY, foliageHeight, solid);
                        this.mapData[zoom].setFoliageLight(imageX, imageY, foliageLight);
                    } else {
                        foliageLight = this.mapData[zoom].getFoliageLight(imageX, imageY);
                    }

                    if (foliageLight == 0) {
                        foliageColor = 0;
                    } else if (foliageLight != 255) {
                        foliageColor = ColorUtils.colorMultiplier(foliageColor, foliageLight);
                    }
                }
            }

            if (seafloorColor != 0 && seafloorHeight > Short.MIN_VALUE) {
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
                if (biome != null) {
                    bc = BiomeRepository.getBiomeColor(biome);
                }

                bc = 2130706432 | bc;
                color24 = ColorUtils.colorAdder(bc, color24);
            }

        }
        MutableBlockPosCache.release(blockPos);
        MutableBlockPosCache.release(tempBlockPos);
        return MapUtils.doSlimeAndGrid(color24, world, startX + imageX, startZ + imageY);
    }

    private int getBlockHeight(boolean nether, boolean caves, World world, int x, int z) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int playerHeight = GameVariableAccessShim.yCoord();
        blockPos.setXYZ(x, playerHeight, z);
        WorldChunk chunk = (WorldChunk) world.getChunk(blockPos);
        int height = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) + 1;
        BlockState blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z));
        FluidState fluidState = this.transparentBlockState.getFluidState();
        if (fluidState != Fluids.EMPTY.getDefaultState()) {
            blockState = fluidState.getBlockState();
        }

        while (blockState.getOpacity(world, blockPos) == 0 && height > world.getBottomY()) {
            --height;
            blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z));
            fluidState = this.surfaceBlockState.getFluidState();
            if (fluidState != Fluids.EMPTY.getDefaultState()) {
                blockState = fluidState.getBlockState();
            }
        }
        MutableBlockPosCache.release(blockPos);
        return (nether || caves) && height > playerHeight ? this.getNetherHeight(x, z) : height;
    }

    private int getNetherHeight(int x, int z) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int y = this.lastY;
        blockPos.setXYZ(x, y, z);
        BlockState blockState = this.world.getBlockState(blockPos);
        if (blockState.getOpacity(this.world, blockPos) == 0 && blockState.getBlock() != Blocks.LAVA) {
            while (y > world.getBottomY()) {
                --y;
                blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(blockPos);
                if (blockState.getOpacity(this.world, blockPos) > 0 || blockState.getBlock() == Blocks.LAVA) {
                    MutableBlockPosCache.release(blockPos);
                    return y + 1;
                }
            }
            MutableBlockPosCache.release(blockPos);
            return y;
        } else {
            while (y <= this.lastY + 10 && y < world.getTopY()) {
                ++y;
                blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(blockPos);
                if (blockState.getOpacity(this.world, blockPos) == 0 && blockState.getBlock() != Blocks.LAVA) {
                    MutableBlockPosCache.release(blockPos);
                    return y;
                }
            }
            MutableBlockPosCache.release(blockPos);
            return -1;
        }
    }

    private int getSeafloorHeight(World world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        for (BlockState blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z)); blockState.getOpacity(world, blockPos) < 5 && !(blockState.getBlock() instanceof LeavesBlock) && height > world.getBottomY() + 1; blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z))) {
            --height;
        }
        MutableBlockPosCache.release(blockPos);
        return height;
    }

    private int getTransparentHeight(boolean nether, boolean caves, World world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int transHeight;
        if (!caves && !nether) {
            transHeight = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, blockPos.withXYZ(x, height, z)).getY();
            if (transHeight <= height) {
                transHeight = Short.MIN_VALUE;
            }
        } else {
            transHeight = Short.MIN_VALUE;
        }

        BlockState blockState = world.getBlockState(blockPos.withXYZ(x, transHeight - 1, z));
        Block material = blockState.getBlock();
        if (transHeight == height + 1 && material == Blocks.SNOW) {
            transHeight = Short.MIN_VALUE;
        }

        if (material == Blocks.BARRIER) {
            ++transHeight;
            blockState = world.getBlockState(blockPos.withXYZ(x, transHeight - 1, z));
            material = blockState.getBlock();
            if (material instanceof AirBlock) {
                transHeight = Short.MIN_VALUE;
            }
        }
        MutableBlockPosCache.release(blockPos);
        return transHeight;
    }

    private int applyHeight(int color24, boolean nether, boolean caves, World world, int zoom, int multi, int startX, int startZ, int imageX, int imageY, int height, boolean solid, int layer) {
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
                        heightComp = this.mapData[zoom].getOceanFloorHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 1) {
                        heightComp = this.mapData[zoom].getHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        heightComp = this.mapData[zoom].getTransparentHeight(imageX - 1, imageY + 1);
                        if (heightComp == Short.MIN_VALUE) {
                            Block block = BlockRepository.getStateById(this.mapData[zoom].getTransparentBlockstateID(imageX, imageY)).getBlock();
                            if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                heightComp = this.mapData[zoom].getHeight(imageX - 1, imageY + 1);
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
                        if (heightComp == Short.MIN_VALUE) {
                            MutableBlockPos blockPos = MutableBlockPosCache.get();
                            BlockState blockState = world.getBlockState(blockPos.withXYZ(startX + imageX, height - 1, startZ + imageY));
                            MutableBlockPosCache.release(blockPos);
                            Block block = blockState.getBlock();
                            if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                heightComp = baseHeight;
                            }
                        }
                    }
                }

                if (heightComp == Short.MIN_VALUE) {
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
        int combinedLight = 255;
        if (solid) {
            combinedLight = 0;
        } else if (color24 != this.colorManager.getAirColor() && color24 != 0 && this.options.lightmap) {
            MutableBlockPos blockPos = MutableBlockPosCache.get();
            blockPos.setXYZ(x, Math.max(Math.min(height, world.getTopY()), world.getBottomY()), z);
            int blockLight = world.getLightLevel(LightType.BLOCK, blockPos);
            int skyLight = world.getLightLevel(LightType.SKY, blockPos);
            if (blockState.getBlock() == Blocks.LAVA || blockState.getBlock() == Blocks.MAGMA_BLOCK) {
                blockLight = 14;
            }
            MutableBlockPosCache.release(blockPos);
            combinedLight = this.lightmapColors[blockLight + skyLight * 16];
        }

        return combinedLight;
    }

    private void renderMap(Matrix4fStack matrixStack, int x, int y, int scScale) {
        float scale = 1.0F;
        if (this.options.squareMap && this.options.rotates) {
            scale = 1.4142F;
        }

        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, 0);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        Matrix4f matrix4f = new Matrix4f().ortho(0.0F, 512.0F, 512.0F, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_DISTANCE);
        OpenGL.Utils.bindFramebuffer();
        OpenGL.glViewport(0, 0, 512, 512);
        matrixStack.pushMatrix();
        matrixStack.identity();
        matrixStack.translate(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();
        OpenGL.glDepthMask(false);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        OpenGL.glClear(16384);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, 0);
        OpenGL.Utils.img2(this.options.squareMap ? this.squareStencil : this.circleStencil);
        OpenGL.Utils.drawPre();
        OpenGL.Utils.ldrawthree(256.0F - 256.0F / scale, 256.0F + 256.0F / scale, 1.0, 0.0F, 0.0F);
        OpenGL.Utils.ldrawthree((256.0F + 256.0F / scale), 256.0F + 256.0F / scale, 1.0, 1.0F, 0.0F);
        OpenGL.Utils.ldrawthree(256.0F + 256.0F / scale, 256.0F - 256.0F / scale, 1.0, 1.0F, 1.0F);
        OpenGL.Utils.ldrawthree(256.0F - 256.0F / scale, 256.0F - 256.0F / scale, 1.0, 0.0F, 1.0F);
        OpenGL.Utils.drawPost();
        // BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //BufferRenderer.drawWithShader(bb.end());
        // BufferRenderer.drawWithGlobalProgram(bb.end());
        OpenGL.glBlendFuncSeparate(1, 0, 774, 0);
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
        OpenGL.Utils.disp2(this.mapImages[this.zoom].getIndex());
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR_MIPMAP_LINEAR);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        matrixStack.pushMatrix();
        matrixStack.translate(256.0f, 256.0f, 0.0f);
        if (!this.options.rotates) {
            matrixStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees((-this.northRotate)));
        } else {
            matrixStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(this.direction));
        }

        matrixStack.translate(-256.0f, -256.0f, 0.0f);
        matrixStack.translate(-this.percentX * 512.0F / 64.0F, this.percentY * 512.0F / 64.0F, 0.0f);
        RenderSystem.applyModelViewMatrix();
        OpenGL.Utils.drawPre();
        OpenGL.Utils.ldrawthree(0.0, 512.0, 1.0, 0.0F, 0.0F);
        OpenGL.Utils.ldrawthree(512.0, 512.0, 1.0, 1.0F, 0.0F);
        OpenGL.Utils.ldrawthree(512.0, 0.0, 1.0, 1.0F, 1.0F);
        OpenGL.Utils.ldrawthree(0.0, 0.0, 1.0, 0.0F, 1.0F);
        OpenGL.Utils.drawPost();
        matrixStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        OpenGL.glDepthMask(true);
        OpenGL.glEnable(GL30C.GL_DEPTH_TEST);
        OpenGL.Utils.unbindFramebuffer();
        OpenGL.glViewport(0, 0, VoxelConstants.getMinecraft().getWindow().getFramebufferWidth(), VoxelConstants.getMinecraft().getWindow().getFramebufferHeight());
        matrixStack.popMatrix();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix, VertexSorter.BY_DISTANCE);
        matrixStack.pushMatrix();
        OpenGL.glBlendFunc(GL30C.GL_SRC_ALPHA, GL30C.GL_ZERO);
        OpenGL.Utils.disp2(OpenGL.Utils.fboTextureId);

        double guiScale = (double) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / this.scWidth;
        minTablistOffset = guiScale * 63;
        OpenGL.glEnable(GL30C.GL_SCISSOR_TEST);
        OpenGL.glScissor((int) (guiScale * (x - 32)), (int) (guiScale * ((this.scHeight - y) - 32.0)), (int) (guiScale * 64.0), (int) (guiScale * 63.0));
        OpenGL.Utils.drawPre();
        OpenGL.Utils.setMapWithScale(x, y, scale);
        OpenGL.Utils.drawPost();
        OpenGL.glDisable(GL30C.GL_SCISSOR_TEST);
        matrixStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        OpenGL.glBlendFunc(GL30C.GL_SRC_ALPHA, GL30C.GL_ONE_MINUS_SRC_ALPHA);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.options.squareMap) {
            this.drawSquareMapFrame(x, y);
        } else {
            this.drawRoundMapFrame(x, y);
        }

        double lastXDouble = GameVariableAccessShim.xCoordDouble();
        double lastZDouble = GameVariableAccessShim.zCoordDouble();
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        OpenGL.Utils.disp2(textureAtlas.getGlId());
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        if (VoxelMap.mapOptions.waypointsAllowed) {
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
        }
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawWaypoint(Matrix4fStack matrixStack, Waypoint pt, TextureAtlas textureAtlas, int x, int y, int scScale, double lastXDouble, double lastZDouble, Sprite icon, Float r, Float g, Float b) {
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
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY);
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
                hypot = (float) (hypot / Math.max(Math.abs(dispX), Math.abs(dispY)) * 30.0);
            }
        } else {
            far = hypot >= 31.0f;
            if (far) {
                hypot = 34.0f;
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

                matrixStack.pushMatrix();
                OpenGL.glColor4f(r, g, b, !pt.enabled && !target ? 0.3F : 1.0F);
                matrixStack.translate(x, y, 0.0f);
                matrixStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(-locate));
                if (uprightIcon) {
                    matrixStack.translate(0.0f, -hypot, 0.0f);
                    matrixStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(locate));
                    matrixStack.translate(-x, -y, 0.0f);
                } else {
                    matrixStack.translate(-x, -y, 0.0f);
                    matrixStack.translate(0.0f, -hypot, 0.0f);
                }

                RenderSystem.applyModelViewMatrix();
                OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
                OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
                OpenGL.Utils.drawPre();
                OpenGL.Utils.setMap(icon, x, y, 16.0F);
                OpenGL.Utils.drawPost();
            } catch (Exception var40) {
                this.error = "Error: marker overlay not found!";
            } finally {
                matrixStack.popMatrix();
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

                matrixStack.pushMatrix();
                OpenGL.glColor4f(r, g, b, !pt.enabled && !target ? 0.3F : 1.0F);
                matrixStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(-locate));
                matrixStack.translate(0.0f, -hypot, 0.0f);
                matrixStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(-(-locate)));
                RenderSystem.applyModelViewMatrix();
                OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
                OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
                OpenGL.Utils.drawPre();
                OpenGL.Utils.setMap(icon, x, y, 16.0F);
                OpenGL.Utils.drawPost();
            } catch (Exception var42) {
                this.error = "Error: waypoint overlay not found!";
            } finally {
                matrixStack.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
        }

    }

    private void drawArrow(Matrix4fStack matrixStack, int x, int y) {
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            matrixStack.pushMatrix();
            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
            OpenGL.Utils.img2(this.arrowResourceLocation);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
            matrixStack.translate(x, y, 0.0f);
            matrixStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(this.options.rotates && !this.fullscreenMap ? 0.0F : this.direction + this.northRotate));
            matrixStack.translate(-x, -y, 0.0f);
            RenderSystem.applyModelViewMatrix();
            OpenGL.Utils.drawPre();
            OpenGL.Utils.setMap(x, y, 16);
            OpenGL.Utils.drawPost();
        } catch (Exception var8) {
            this.error = "Error: minimap arrow not found!";
        } finally {
            matrixStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }

    }

    private void renderMapFull(DrawContext drawContext, int scWidth, int scHeight) {
        MatrixStack matrixStack = drawContext.getMatrices();
        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].write();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        OpenGL.Utils.disp2(this.mapImages[this.zoom].getIndex());
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR_MIPMAP_LINEAR);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        matrixStack.push();
        matrixStack.translate(scWidth / 2.0F, scHeight / 2.0F, -0.0);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.northRotate));
        matrixStack.translate(-(scWidth / 2.0F), -(scHeight / 2.0F), -0.0);
        RenderSystem.applyModelViewMatrix();
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.Utils.drawPre();
        int left = scWidth / 2 - 128;
        int top = scHeight / 2 - 128;
        OpenGL.Utils.ldrawone(left, top + 256, 160.0, 0.0F, 1.0F);
        OpenGL.Utils.ldrawone(left + 256, top + 256, 160.0, 1.0F, 1.0F);
        OpenGL.Utils.ldrawone(left + 256, top, 160.0, 1.0F, 0.0F);
        OpenGL.Utils.ldrawone(left, top, 160.0, 0.0F, 0.0F);
        OpenGL.Utils.drawPost();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        if (this.options.biomeOverlay != 0) {
            double factor = Math.pow(2.0, 3 - this.zoom);
            int minimumSize = (int) Math.pow(2.0, this.zoom);
            minimumSize *= minimumSize;
            ArrayList<AbstractMapData.BiomeLabel> labels = this.mapData[this.zoom].getBiomeLabels();
            OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
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
                        this.write(drawContext, name, (left + 256) - z - (nameWidth / 2f), top + x - 3.0F, 16777215);
                    } else {
                        this.write(drawContext, name, left + x - (nameWidth / 2f), top + z - 3.0F, 16777215);
                    }
                }
            }

            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
            OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        }

    }

    private void drawSquareMapFrame(int x, int y) {
        try {
            OpenGL.Utils.disp2(this.mapImageInt);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, OpenGL.GL12_GL_CLAMP_TO_EDGE);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, OpenGL.GL12_GL_CLAMP_TO_EDGE);
            OpenGL.Utils.drawPre();
            OpenGL.Utils.setMap(x, y, 128);
            OpenGL.Utils.drawPost();
        } catch (Exception var4) {
            this.error = "error: minimap overlay not found!";
        }

    }

    private void loadMapImage() {
        if (this.mapImageInt != -1) {
            OpenGL.Utils.glah(this.mapImageInt);
        }

        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(Identifier.of("voxelmap", "images/squaremap.png")).get().getInputStream();
            BufferedImage mapImage = ImageIO.read(is);
            is.close();
            this.mapImageInt = OpenGL.Utils.tex(mapImage);
        } catch (Exception var8) {
            try {
                InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(Identifier.of("textures/map/map_background.png")).get().getInputStream();
                Image tpMap = ImageIO.read(is);
                is.close();
                BufferedImage mapImage = new BufferedImage(tpMap.getWidth(null), tpMap.getHeight(null), 2);
                Graphics2D gfx = mapImage.createGraphics();
                gfx.drawImage(tpMap, 0, 0, null);
                int border = mapImage.getWidth() * 8 / 128;
                gfx.setComposite(AlphaComposite.Clear);
                gfx.fillRect(border, border, mapImage.getWidth() - border * 2, mapImage.getHeight() - border * 2);
                gfx.dispose();
                this.mapImageInt = OpenGL.Utils.tex(mapImage);
            } catch (Exception var7) {
                VoxelConstants.getLogger().warn("Error loading texture pack's map image: " + var7.getLocalizedMessage());
            }
        }

    }

    private void drawRoundMapFrame(int x, int y) {
        try {
            OpenGL.Utils.img2(this.roundmapResourceLocation);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.Utils.drawPre();
            OpenGL.Utils.setMap(x, y, 128);
            OpenGL.Utils.drawPost();
        } catch (Exception var4) {
            this.error = "Error: minimap overlay not found!";
        }

    }

    private void drawDirections(DrawContext drawContext, int x, int y) {
        MatrixStack matrixStack = drawContext.getMatrices();
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
        this.write(drawContext, "N", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-rotate)), distance * Math.cos(Math.toRadians(-rotate)), 10.0);
        this.write(drawContext, "E", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate + 90.0))), distance * Math.cos(Math.toRadians(-(rotate + 90.0))), 10.0);
        this.write(drawContext, "S", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
        matrixStack.push();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate + 180.0))), distance * Math.cos(Math.toRadians(-(rotate + 180.0))), 10.0);
        this.write(drawContext, "W", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.pop();
    }

    private void showCoords(DrawContext drawContext, int x, int y) {
        MatrixStack matrixStack = drawContext.getMatrices();
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
            String xCoord = Integer.toString(GameVariableAccessShim.xCoord());
            String yCoord = Integer.toString(GameVariableAccessShim.yCoord());
            String zCoord = Integer.toString(GameVariableAccessShim.zCoord());

            String coordinates = xCoord + ", " + yCoord + ", " + zCoord;
            int centerPos = this.chkLen(coordinates) / 2;

            this.write(matrixStack, coordinates, x / scale - centerPos, textStart / scale, 16777215);
            if (this.ztimer > 0) {
                m = this.chkLen(this.error) / 2;
                this.write(drawContext, this.error, x / scale - m, textStart / scale + 19.0F, 16777215); //WORLD NAME
            }

            matrixStack.pop();
        } else {
            int heading = (int) (this.direction + this.northRotate);
            if (heading > 360) {
                heading -= 360;
            }
            String ns = "";
            String ew = "";
            if (heading > 360 - 67.5 || heading <= 67.5) {
                ns = "N";
            } else if (heading > 180 - 67.5 && heading <= 180 + 67.5) {
                ns = "S";
            }
            if (heading > 90 - 67.5 && heading <= 90 + 67.5) {
                ew = "E";
            } else if (heading > 270 - 67.5 && heading <= 270 + 67.5) {
                ew = "W";
            }

            String stats = "(" + this.dCoord(GameVariableAccessShim.xCoord()) + ", " + GameVariableAccessShim.yCoord() + ", " + this.dCoord(GameVariableAccessShim.zCoord()) + ") " + heading + "' " + ns + ew;
            int m = this.chkLen(stats) / 2;
            this.write(drawContext, stats, (this.scWidth / 2f - m), 5.0F, 16777215);
            if (this.ztimer > 0) {
                m = this.chkLen(this.error) / 2;
                this.write(drawContext, this.error, (this.scWidth / 2f - m), 15.0F, 16777215);
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

    private void write(DrawContext drawContext, String text, float x, float y, int color) {
        write(drawContext, Text.of(text), x, y, color);
    }

    private int chkLen(Text text) {
        return this.fontRenderer.getWidth(text);
    }

    private void write(DrawContext drawContext, Text text, float x, float y, int color) {
        drawContext.drawTextWithShadow(this.fontRenderer, text, (int) x, (int) y, color);
    }

    private void drawWelcomeScreen(DrawContext drawContext, int scWidth, int scHeight) {
        if (this.welcomeText[1] == null || this.welcomeText[1].getString().equals("minimap.ui.welcome2")) {
            String zmodver = FabricLoader.getInstance().getModContainer("voxelmap").get().getMetadata().getVersion().getFriendlyString();
            this.welcomeText[0] = (Text.literal("")).append((Text.literal("VoxelMap! ")).formatted(Formatting.RED)).append(zmodver + " ").append(Text.translatable("minimap.ui.welcome1"));
            this.welcomeText[1] = Text.translatable("minimap.ui.welcome2");
            this.welcomeText[2] = Text.translatable("minimap.ui.welcome3");
            this.welcomeText[3] = Text.translatable("minimap.ui.welcome4");
            this.welcomeText[4] = (Text.literal("")).append((Text.keybind(this.options.keyBindZoom.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome5a")).append(", ").append((Text.keybind(this.options.keyBindMenu.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome5b"));
            this.welcomeText[5] = (Text.literal("")).append((Text.keybind(this.options.keyBindFullscreen.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome6"));
            this.welcomeText[6] = (Text.literal("")).append((Text.keybind(this.options.keyBindWaypoint.getTranslationKey())).formatted(Formatting.AQUA)).append(": ").append(Text.translatable("minimap.ui.welcome7"));
            this.welcomeText[7] = this.options.keyBindZoom.getBoundKeyLocalizedText().copy().append(": ").append((Text.translatable("minimap.ui.welcome8")).formatted(Formatting.GRAY));
        }

        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
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
        int leftX = centerX - title / 2 - border;
        int rightX = centerX + title / 2 + border;
        int topY = centerY - (height - 1) / 2 * 10 - border - 20;
        int botY = centerY - (height - 1) / 2 * 10 + border - 10;
        this.drawBox(drawContext, leftX, rightX, topY, botY);
        leftX = centerX - maxSize / 2 - border;
        rightX = centerX + maxSize / 2 + border;
        topY = centerY - (height - 1) / 2 * 10 - border;
        botY = centerY + (height - 1) / 2 * 10 + border;
        this.drawBox(drawContext, leftX, rightX, topY, botY);
        leftX = centerX - footer / 2 - border;
        rightX = centerX + footer / 2 + border;
        topY = centerY + (height - 1) / 2 * 10 - border + 10;
        botY = centerY + (height - 1) / 2 * 10 + border + 20;
        this.drawBox(drawContext, leftX, rightX, topY, botY);
        drawContext.drawTextWithShadow(this.fontRenderer, head, (centerX - title / 2), (centerY - (height - 1) * 10 / 2 - 19), Color.WHITE.getRGB());
        for (int n = 1; n < height; ++n) {
            drawContext.drawTextWithShadow(this.fontRenderer, this.welcomeText[n], (centerX - maxSize / 2), (centerY - (height - 1) * 10 / 2 + n * 10 - 9), Color.WHITE.getRGB());
        }

        drawContext.drawTextWithShadow(this.fontRenderer, hide, (centerX - footer / 2), ((scHeight + 5) / 2 + (height - 1) * 10 / 2 + 11), Color.WHITE.getRGB());
    }

    private void drawBox(DrawContext drawContext, int leftX, int rightX, int topY, int botY) {
        double e = VoxelConstants.getMinecraft().options.getTextBackgroundOpacity().getValue();
        int v = (int) (255.0 * 1.0 * e);
        drawContext.fill(leftX, topY, rightX, botY, v << 24);
    }

    public static double getMinTablistOffset() {
        return minTablistOffset;
    }

    public static float getStatusIconOffset() {
        return statusIconOffset;
    }
}
