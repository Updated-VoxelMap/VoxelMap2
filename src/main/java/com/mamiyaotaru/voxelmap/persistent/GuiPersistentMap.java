package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.fabricmod.FabricModVoxelMap;
import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.mamiyaotaru.voxelmap.gui.GuiSubworldsSelect;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.IGuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiScreen;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BackgroundImageInfo;
import com.mamiyaotaru.voxelmap.util.BiomeMapData;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class GuiPersistentMap extends PopupGuiScreen implements IGuiWaypoints {
    private final Random generator = new Random();
    private final PersistentMap persistentMap;
    private final WaypointManager waypointManager;
    private final Screen parent;
    private final MapSettingsManager mapOptions;
    private final PersistentMapSettingsManager options;
    protected String screenTitle = "World Map";
    protected String worldNameDisplay = "";
    protected int worldNameDisplayLength;
    protected int maxWorldNameDisplayLength;
    private String subworldName = "";
    private PopupGuiButton buttonMultiworld;
    private int top;
    private int bottom;
    private boolean oldNorth;
    private boolean lastStill;
    private boolean editingCoordinates;
    private boolean lastEditingCoordinates;
    private TextFieldWidget coordinates;
    int centerX;
    int centerY;
    float mapCenterX;
    float mapCenterZ;
    float deltaX;
    float deltaY;
    float deltaXonRelease;
    float deltaYonRelease;
    long timeOfRelease;
    boolean mouseCursorShown = true;
    long timeAtLastTick;
    long timeOfLastKBInput;
    long timeOfLastMouseInput;
    float lastMouseX;
    float lastMouseY;
    protected int mouseX;
    protected int mouseY;
    boolean leftMouseButtonDown;
    float zoom;
    float zoomStart;
    float zoomGoal;
    long timeOfZoom;
    float zoomDirectX;
    float zoomDirectY;
    private float scScale = 1.0F;
    private float guiToMap = 2.0F;
    private float mapToGui = 0.5F;
    private float mouseDirectToMap = 1.0F;
    private float guiToDirectMouse = 2.0F;
    private static int playerGLID;
    private static boolean gotSkin;
    private static int skinTries;
    private boolean closed;
    private CachedRegion[] regions = new CachedRegion[0];
    BackgroundImageInfo backGroundImageInfo;
    private final BiomeMapData biomeMapData = new BiomeMapData(760, 360);
    private float mapPixelsX;
    private float mapPixelsY;
    private final Object closedLock = new Object();
    private final KeyBinding keyBindForward = new KeyBinding("key.forward.fake", 17, "key.categories.movement");
    private final KeyBinding keyBindLeft = new KeyBinding("key.left.fake", 30, "key.categories.movement");
    private final KeyBinding keyBindBack = new KeyBinding("key.back.fake", 31, "key.categories.movement");
    private final KeyBinding keyBindRight = new KeyBinding("key.right.fake", 32, "key.categories.movement");
    private final KeyBinding keyBindSprint = new KeyBinding("key.sprint.fake", 29, "key.categories.movement");
    private final InputUtil.Key forwardCode;
    private final InputUtil.Key leftCode;
    private final InputUtil.Key backCode;
    private final InputUtil.Key rightCode;
    private final InputUtil.Key sprintCode;
    final InputUtil.Key nullInput = InputUtil.fromTranslationKey("key.keyboard.unknown");
    private Text multiworldButtonName;
    private MutableText multiworldButtonNameRed;
    int sideMargin = 10;
    int buttonCount = 5;
    int buttonSeparation = 4;
    int buttonWidth = 66;
    public boolean editClicked;
    public boolean deleteClicked;
    public boolean addClicked;
    Waypoint newWaypoint;
    Waypoint selectedWaypoint;
    public boolean passEvents;

    public GuiPersistentMap(Screen parent) {
        this.parent = parent;
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
        this.zoom = this.options.zoom;
        this.zoomStart = this.options.zoom;
        this.zoomGoal = this.options.zoom;
        this.persistentMap.setLightMapArray(VoxelConstants.getVoxelMapInstance().getMap().getLightmapArray());
        if (!gotSkin && skinTries < 5) {
            this.getSkin();
        }

        this.forwardCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.forwardKey.getBoundKeyTranslationKey());
        this.leftCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.leftKey.getBoundKeyTranslationKey());
        this.backCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.backKey.getBoundKeyTranslationKey());
        this.rightCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.rightKey.getBoundKeyTranslationKey());
        this.sprintCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.sprintKey.getBoundKeyTranslationKey());
    }

    private void getSkin() {
        Identifier skinLocation = VoxelConstants.getMinecraft().getSkinProvider().getSkinTextures(VoxelConstants.getPlayer().getGameProfile()).texture();
        PlayerSkinTexture imageData = null;

        try {
            if (skinLocation != DefaultSkinHelper.getSkinTextures(VoxelConstants.getPlayer().getUuid()).texture()) {
                //FIXME 1.20.2 AbstractClientPlayerEntity.loadSkin(skinLocation, VoxelConstants.getPlayer().getName().getString());
                imageData = (PlayerSkinTexture) VoxelConstants.getMinecraft().getTextureManager().getTexture(skinLocation);
            }
        } catch (RuntimeException ignored) {
        }

        if (imageData != null) {
            gotSkin = true;
            OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, imageData.getGlId());
        } else {
            ++skinTries;
            OpenGL.Utils.img(skinLocation);
        }

        BufferedImage skinImage = ImageUtils.createBufferedImageFromCurrentGLImage();
        boolean showHat = VoxelConstants.getPlayer().isPartVisible(PlayerModelPart.HAT);
        if (showHat) {
            skinImage = ImageUtils.addImages(ImageUtils.loadImage(skinImage, 8, 8, 8, 8), ImageUtils.loadImage(skinImage, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
        } else {
            skinImage = ImageUtils.loadImage(skinImage, 8, 8, 8, 8);
        }

        float scale = skinImage.getWidth() / 8.0F;
        skinImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(skinImage, 2.0F / scale)), true, 1);
        if (playerGLID != 0) {
            RenderSystem.deleteTexture(playerGLID);
        }

        playerGLID = OpenGL.Utils.tex(skinImage);
    }

    public void init() {
        this.passEvents = true;
        this.oldNorth = mapOptions.oldNorth;
        this.centerAt(this.options.mapX, this.options.mapZ);
        if (VoxelConstants.getMinecraft().currentScreen == this) {
            this.closed = false;
        }

        this.screenTitle = I18n.translate("worldmap.title");
        this.buildWorldName();
        this.leftMouseButtonDown = false;
        this.sideMargin = 10;
        this.buttonCount = 5;
        this.buttonSeparation = 4;
        this.buttonWidth = (this.width - this.sideMargin * 2 - this.buttonSeparation * (this.buttonCount - 1)) / this.buttonCount;
        this.addDrawableChild(new PopupGuiButton(this.sideMargin, this.getHeight() - 28, this.buttonWidth, 20, Text.translatable("options.minimap.waypoints"), buttonWidget_1 -> VoxelConstants.getMinecraft().setScreen(new GuiWaypoints(this)), this));
        this.multiworldButtonName = Text.translatable(VoxelConstants.isRealmServer() ? "menu.online" : "options.worldmap.multiworld");
        this.multiworldButtonNameRed = (Text.translatable(VoxelConstants.isRealmServer() ? "menu.online" : "options.worldmap.multiworld")).formatted(Formatting.RED);
        if (!VoxelConstants.getMinecraft().isIntegratedServerRunning() && !VoxelConstants.getVoxelMapInstance().getWaypointManager().receivedAutoSubworldName()) {
            this.addDrawableChild(this.buttonMultiworld = new PopupGuiButton(this.sideMargin + (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, this.multiworldButtonName, buttonWidget_1 -> VoxelConstants.getMinecraft().setScreen(new GuiSubworldsSelect(this)), this));
        }

        this.addDrawableChild(new PopupGuiButton(this.sideMargin + 3 * (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, Text.translatable("menu.options"), null, this) {
            public void onPress() {
                VoxelConstants.getMinecraft().setScreen(new GuiMinimapOptions(GuiPersistentMap.this));
            }
        });
        this.addDrawableChild(new PopupGuiButton(this.sideMargin + 4 * (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, Text.translatable("gui.done"), null, this) {
            public void onPress() {
                VoxelConstants.getMinecraft().setScreen(GuiPersistentMap.this.parent);
            }
        });
        this.coordinates = new TextFieldWidget(this.getFontRenderer(), this.sideMargin, 10, 140, 20, null);
        this.top = 32;
        this.bottom = this.getHeight() - 32;
        this.centerX = this.getWidth() / 2;
        this.centerY = (this.bottom - this.top) / 2;
        this.scScale = (float) VoxelConstants.getMinecraft().getWindow().getScaleFactor();
        this.mapPixelsX = VoxelConstants.getMinecraft().getWindow().getFramebufferWidth();
        this.mapPixelsY = (VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() - (int) (64.0F * this.scScale));
        this.lastStill = false;
        this.timeAtLastTick = System.currentTimeMillis();
        this.keyBindForward.setBoundKey(this.forwardCode);
        this.keyBindLeft.setBoundKey(this.leftCode);
        this.keyBindBack.setBoundKey(this.backCode);
        this.keyBindRight.setBoundKey(this.rightCode);
        this.keyBindSprint.setBoundKey(this.sprintCode);
        VoxelConstants.getMinecraft().options.forwardKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.leftKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.backKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.rightKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.sprintKey.setBoundKey(this.nullInput);
        KeyBinding.updateKeysByCode();
    }

    private void centerAt(int x, int z) {
        if (this.oldNorth) {
            this.mapCenterX = (-z);
            this.mapCenterZ = x;
        } else {
            this.mapCenterX = x;
            this.mapCenterZ = z;
        }

    }

    private void buildWorldName() {
        final AtomicReference<String> worldName = new AtomicReference<>();

        VoxelConstants.getIntegratedServer().ifPresentOrElse(integratedServer -> {
            worldName.set(integratedServer.getSaveProperties().getLevelName());

            if (worldName.get() == null || worldName.get().isBlank()) worldName.set("Singleplayer World");
        }, () -> {
            ServerInfo info = VoxelConstants.getMinecraft().getCurrentServerEntry();

            if (info != null) worldName.set(info.name);
            if (worldName.get() == null || worldName.get().isBlank()) worldName.set("Multiplayer Server");
            if (VoxelConstants.isRealmServer()) worldName.set("Realms");
        });

        StringBuilder worldNameBuilder = (new StringBuilder("§r")).append(worldName.get());
        String subworldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true);
        this.subworldName = subworldName;
        if ((subworldName == null || subworldName.isEmpty()) && VoxelConstants.getVoxelMapInstance().getWaypointManager().isMultiworld()) {
            subworldName = "???";
        }

        if (subworldName != null && !subworldName.isEmpty()) {
            worldNameBuilder.append(" - ").append(subworldName);
        }

        this.worldNameDisplay = worldNameBuilder.toString();
        this.worldNameDisplayLength = this.getFontRenderer().getWidth(this.worldNameDisplay);

        for (this.maxWorldNameDisplayLength = this.getWidth() / 2 - this.getFontRenderer().getWidth(this.screenTitle) / 2 - this.sideMargin * 2; this.worldNameDisplayLength > this.maxWorldNameDisplayLength && worldName.get().length() > 5; this.worldNameDisplayLength = this.getFontRenderer().getWidth(this.worldNameDisplay)) {
            worldName.set(worldName.get().substring(0, worldName.get().length() - 1));
            worldNameBuilder = new StringBuilder(worldName.get());
            worldNameBuilder.append("...");
            if (subworldName != null && !subworldName.isEmpty()) {
                worldNameBuilder.append(" - ").append(subworldName);
            }

            this.worldNameDisplay = worldNameBuilder.toString();
        }

        if (subworldName != null && !subworldName.isEmpty()) {
            while (this.worldNameDisplayLength > this.maxWorldNameDisplayLength && subworldName.length() > 5) {
                worldNameBuilder = new StringBuilder(worldName.get());
                worldNameBuilder.append("...");
                subworldName = subworldName.substring(0, subworldName.length() - 1);
                worldNameBuilder.append(" - ").append(subworldName);
                this.worldNameDisplay = worldNameBuilder.toString();
                this.worldNameDisplayLength = this.getFontRenderer().getWidth(this.worldNameDisplay);
            }
        }

    }

    private float bindZoom(float zoom) {
        zoom = Math.max(this.options.minZoom, zoom);
        return Math.min(this.options.maxZoom, zoom);
    }

    private float easeOut(float elapsedTime, float startValue, float finalDelta, float totalTime) {
        float value;
        if (elapsedTime == totalTime) {
            value = startValue + finalDelta;
        } else {
            value = finalDelta * (-((float) Math.pow(2.0, -10.0F * elapsedTime / totalTime)) + 1.0F) + startValue;
        }

        return value;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        this.timeOfLastMouseInput = System.currentTimeMillis();
        this.switchToMouseInput();
        float mouseDirectX = (float) VoxelConstants.getMinecraft().mouse.getX();
        float mouseDirectY = (float) VoxelConstants.getMinecraft().mouse.getY();
        if (amount != 0.0) {
            if (amount > 0.0) {
                this.zoomGoal *= 1.26F;
            } else if (amount < 0.0) {
                this.zoomGoal /= 1.26F;
            }

            this.zoomStart = this.zoom;
            this.zoomGoal = this.bindZoom(this.zoomGoal);
            this.timeOfZoom = System.currentTimeMillis();
            this.zoomDirectX = mouseDirectX;
            this.zoomDirectY = mouseDirectY;
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseY > this.top && mouseY < this.bottom && button == 1) {
            this.timeOfLastKBInput = 0L;
            int mouseDirectX = (int) VoxelConstants.getMinecraft().mouse.getX();
            int mouseDirectY = (int) VoxelConstants.getMinecraft().mouse.getY();
            this.createPopup((int) mouseX, (int) mouseY, mouseDirectX, mouseDirectY);
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.popupOpen()) {
            this.coordinates.mouseClicked(mouseX, mouseY, button);
            this.editingCoordinates = this.coordinates.isFocused();
            if (this.editingCoordinates && !this.lastEditingCoordinates) {
                int x;
                int z;
                if (this.oldNorth) {
                    x = (int) Math.floor(this.mapCenterZ);
                    z = -((int) Math.floor(this.mapCenterX));
                } else {
                    x = (int) Math.floor(this.mapCenterX);
                    z = (int) Math.floor(this.mapCenterZ);
                }

                this.coordinates.setText(x + ", " + z);
                this.coordinates.setEditableColor(16777215);
            }

            this.lastEditingCoordinates = this.editingCoordinates;
        }

        return super.mouseClicked(mouseX, mouseY, button) || button == 1;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.editingCoordinates && (VoxelConstants.getMinecraft().options.jumpKey.matchesKey(keyCode, scanCode) || VoxelConstants.getMinecraft().options.sneakKey.matchesKey(keyCode, scanCode))) {
            if (VoxelConstants.getMinecraft().options.jumpKey.matchesKey(keyCode, scanCode)) {
                this.zoomGoal /= 1.26F;
            }

            if (VoxelConstants.getMinecraft().options.sneakKey.matchesKey(keyCode, scanCode)) {
                this.zoomGoal *= 1.26F;
            }

            this.zoomStart = this.zoom;
            this.zoomGoal = this.bindZoom(this.zoomGoal);
            this.timeOfZoom = System.currentTimeMillis();
            this.zoomDirectX = (VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / 2f);
            this.zoomDirectY = (VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() - VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() / 2f);
            this.switchToKeyboardInput();
        }

        this.clearPopups();
        if (this.editingCoordinates) {
            this.coordinates.keyPressed(keyCode, scanCode, modifiers);
            boolean isGood = this.isAcceptable(this.coordinates.getText());
            this.coordinates.setEditableColor(isGood ? 16777215 : 16711680);
            if ((keyCode == 257 || keyCode == 335) && this.coordinates.isFocused() && isGood) {
                String[] xz = this.coordinates.getText().split(",");
                this.centerAt(Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim()));
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }

            if (keyCode == 258 && this.coordinates.isFocused()) {
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }
        }

        if (VoxelConstants.getVoxelMapInstance().getMapOptions().keyBindMenu.matchesKey(keyCode, scanCode)) {
            keyCode = 256;
            scanCode = -1;
            modifiers = -1;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        this.clearPopups();
        if (this.editingCoordinates) {
            this.coordinates.charTyped(chr, modifiers);
            boolean isGood = this.isAcceptable(this.coordinates.getText());
            this.coordinates.setEditableColor(isGood ? 16777215 : 16711680);
            if (chr == '\r' && this.coordinates.isFocused() && isGood) {
                String[] xz = this.coordinates.getText().split(",");
                this.centerAt(Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim()));
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }
        }

        if (VoxelConstants.getVoxelMapInstance().getMapOptions().keyBindMenu.matchesKey(modifiers, -1)) {
            super.keyPressed(256, -1, -1);
        }

        return super.charTyped(chr, modifiers);
    }

    private boolean isAcceptable(String input) {
        try {
            String[] xz = this.coordinates.getText().split(",");
            Integer.valueOf(xz[0].trim());
            Integer.valueOf(xz[1].trim());
            return true;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException var3) {
            return false;
        }
    }

    private void switchToMouseInput() {
        this.timeOfLastKBInput = 0L;
        if (!this.mouseCursorShown) {
            GLFW.glfwSetInputMode(VoxelConstants.getMinecraft().getWindow().getHandle(), 208897, 212993);
        }

        this.mouseCursorShown = true;
    }

    private void switchToKeyboardInput() {
        this.timeOfLastKBInput = System.currentTimeMillis();
        this.mouseCursorShown = false;
        GLFW.glfwSetInputMode(VoxelConstants.getMinecraft().getWindow().getHandle(), 208897, 212995);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.zoomGoal = this.bindZoom(this.zoomGoal);
        if (this.mouseX != mouseX || this.mouseY != mouseY) {
            this.timeOfLastMouseInput = System.currentTimeMillis();
            this.switchToMouseInput();
        }

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        float mouseDirectX = (float) VoxelConstants.getMinecraft().mouse.getX();
        float mouseDirectY = (float) VoxelConstants.getMinecraft().mouse.getY();
        if (this.zoom != this.zoomGoal) {
            float previousZoom = this.zoom;
            long timeSinceZoom = System.currentTimeMillis() - this.timeOfZoom;
            if (timeSinceZoom < 700.0F) {
                this.zoom = this.easeOut(timeSinceZoom, this.zoomStart, this.zoomGoal - this.zoomStart, 700.0F);
            } else {
                this.zoom = this.zoomGoal;
            }

            float scaledZoom = this.zoom;
            if (VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() > 1600) {
                scaledZoom = this.zoom * VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / 1600.0F;
            }

            float zoomDelta = this.zoom / previousZoom;
            float zoomOffsetX = this.centerX * this.guiToDirectMouse - this.zoomDirectX;
            float zoomOffsetY = (this.top + this.centerY) * this.guiToDirectMouse - this.zoomDirectY;
            float zoomDeltaX = zoomOffsetX - zoomOffsetX * zoomDelta;
            float zoomDeltaY = zoomOffsetY - zoomOffsetY * zoomDelta;
            this.mapCenterX += zoomDeltaX / scaledZoom;
            this.mapCenterZ += zoomDeltaY / scaledZoom;
        }

        this.options.zoom = this.zoomGoal;
        float scaledZoom = this.zoom;
        if (VoxelConstants.getMinecraft().getWindow().getWidth() > 1600) {
            scaledZoom = this.zoom * VoxelConstants.getMinecraft().getWindow().getWidth() / 1600.0F;
        }

        this.guiToMap = this.scScale / scaledZoom;
        this.mapToGui = 1.0F / this.scScale * scaledZoom;
        this.mouseDirectToMap = 1.0F / scaledZoom;
        this.guiToDirectMouse = this.scScale;
        this.renderBackground(drawContext);
        if (VoxelConstants.getMinecraft().mouse.wasLeftButtonClicked()) {
            if (!this.leftMouseButtonDown && this.overPopup(mouseX, mouseY)) {
                this.deltaX = 0.0F;
                this.deltaY = 0.0F;
                this.lastMouseX = mouseDirectX;
                this.lastMouseY = mouseDirectY;
                this.leftMouseButtonDown = true;
            } else if (this.leftMouseButtonDown) {
                this.deltaX = (this.lastMouseX - mouseDirectX) * this.mouseDirectToMap;
                this.deltaY = (this.lastMouseY - mouseDirectY) * this.mouseDirectToMap;
                this.lastMouseX = mouseDirectX;
                this.lastMouseY = mouseDirectY;
                this.deltaXonRelease = this.deltaX;
                this.deltaYonRelease = this.deltaY;
                this.timeOfRelease = System.currentTimeMillis();
            }
        } else {
            long timeSinceRelease = System.currentTimeMillis() - this.timeOfRelease;
            if (timeSinceRelease < 700.0F) {
                this.deltaX = this.deltaXonRelease * (float) Math.exp((-timeSinceRelease) / 350.0F);
                this.deltaY = this.deltaYonRelease * (float) Math.exp((-timeSinceRelease) / 350.0F);
            } else {
                this.deltaX = 0.0F;
                this.deltaY = 0.0F;
                this.deltaXonRelease = 0.0F;
                this.deltaYonRelease = 0.0F;
            }

            this.leftMouseButtonDown = false;
        }

        long timeSinceLastTick = System.currentTimeMillis() - this.timeAtLastTick;
        this.timeAtLastTick = System.currentTimeMillis();
        if (!this.editingCoordinates) {
            int kbDelta = 5;
            if (this.keyBindSprint.isPressed()) {
                kbDelta = 10;
            }

            if (this.keyBindForward.isPressed()) {
                this.deltaY -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (this.keyBindBack.isPressed()) {
                this.deltaY += kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (this.keyBindLeft.isPressed()) {
                this.deltaX -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (this.keyBindRight.isPressed()) {
                this.deltaX += kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }
        }

        this.mapCenterX += this.deltaX;
        this.mapCenterZ += this.deltaY;
        if (this.oldNorth) {
            this.options.mapX = (int) this.mapCenterZ;
            this.options.mapZ = -((int) this.mapCenterX);
        } else {
            this.options.mapX = (int) this.mapCenterX;
            this.options.mapZ = (int) this.mapCenterZ;
        }

        this.centerX = this.getWidth() / 2;
        this.centerY = (this.bottom - this.top) / 2;
        int left;
        int right;
        int top;
        int bottom;
        if (this.oldNorth) {
            left = (int) Math.floor((this.mapCenterZ - this.centerY * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterZ + this.centerY * this.guiToMap) / 256.0F);
            top = (int) Math.floor((-this.mapCenterX - this.centerX * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((-this.mapCenterX + this.centerX * this.guiToMap) / 256.0F);
        } else {
            left = (int) Math.floor((this.mapCenterX - this.centerX * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterX + this.centerX * this.guiToMap) / 256.0F);
            top = (int) Math.floor((this.mapCenterZ - this.centerY * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((this.mapCenterZ + this.centerY * this.guiToMap) / 256.0F);
        }

        synchronized (this.closedLock) {
            if (this.closed) {
                return;
            }

            this.regions = this.persistentMap.getRegions(left - 1, right + 1, top - 1, bottom + 1);
        }

        MatrixStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.push();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0f);
        modelViewMatrixStack.translate(this.centerX - this.mapCenterX * this.mapToGui, (this.top + this.centerY) - this.mapCenterZ * this.mapToGui, 0.0);
        if (this.oldNorth) {
            modelViewMatrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
        }

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        this.backGroundImageInfo = this.waypointManager.getBackgroundImageInfo();
        if (this.backGroundImageInfo != null) {
            RenderSystem.setShaderTexture(0, this.backGroundImageInfo.glid);
            this.drawTexturedModalRect(this.backGroundImageInfo.left * this.mapToGui, this.backGroundImageInfo.top * this.mapToGui, this.backGroundImageInfo.width * this.mapToGui, this.backGroundImageInfo.height * this.mapToGui);
        }

        for (CachedRegion region : this.regions) {
            int glid = region.getGLID();
            if (glid != 0) {
                RenderSystem.setShaderTexture(0, glid);
                RenderSystem.bindTextureForSetup(glid);
                if (mapOptions.filtering) {
                    RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR_MIPMAP_LINEAR);
                    RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
                } else {
                    RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR_MIPMAP_LINEAR);
                    RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
                }

                this.drawTexturedModalRect((region.getX() * 256) * this.mapToGui, (region.getZ() * 256) * this.mapToGui, region.getWidth() * this.mapToGui, region.getWidth() * this.mapToGui);
            }
        }

        float cursorX;
        float cursorY;
        if (this.mouseCursorShown) {
            cursorX = mouseDirectX;
            cursorY = mouseDirectY - this.top * this.guiToDirectMouse;
        } else {
            cursorX = (VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / 2f);
            cursorY = (VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() - VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() / 2f) - this.top * this.guiToDirectMouse;
        }

        float cursorCoordZ;
        float cursorCoordX;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        if (this.options.showWaypoints) {
            for (Waypoint pt : this.waypointManager.getWaypoints()) {
                this.drawWaypoint(drawContext, pt, cursorCoordX, cursorCoordZ, null, null, null, null);
            }

            if (this.waypointManager.getHighlightedWaypoint() != null) {
                this.drawWaypoint(drawContext, this.waypointManager.getHighlightedWaypoint(), cursorCoordX, cursorCoordZ, VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/target.png"), 1.0F, 0.0F, 0.0F);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, playerGLID);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
        RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        float playerX = (float) GameVariableAccessShim.xCoordDouble();
        float playerZ = (float) GameVariableAccessShim.zCoordDouble();
        if (this.oldNorth) {
            modelViewMatrixStack.push();
            modelViewMatrixStack.translate(playerX * this.mapToGui, playerZ * this.mapToGui, 0.0);
            modelViewMatrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
            modelViewMatrixStack.translate(-(playerX * this.mapToGui), -(playerZ * this.mapToGui), 0.0);
            RenderSystem.applyModelViewMatrix();
        }

        this.drawTexturedModalRect(-10.0F / this.scScale + playerX * this.mapToGui, -10.0F / this.scScale + playerZ * this.mapToGui, 20.0F / this.scScale, 20.0F / this.scScale);
        if (this.oldNorth) {
            modelViewMatrixStack.pop();
        }

        if (this.oldNorth) {
            modelViewMatrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
        }

        modelViewMatrixStack.translate(-(this.centerX - this.mapCenterX * this.mapToGui), -((this.top + this.centerY) - this.mapCenterZ * this.mapToGui), 0.0);
        RenderSystem.applyModelViewMatrix();
        if (mapOptions.biomeOverlay != 0) {
            float biomeScaleX = this.mapPixelsX / 760.0F;
            float biomeScaleY = this.mapPixelsY / 360.0F;
            boolean still = !this.leftMouseButtonDown;
            still = still && this.zoom == this.zoomGoal;
            still = still && this.deltaX == 0.0F && this.deltaY == 0.0F;
            still = still && ThreadManager.executorService.getActiveCount() == 0;
            if (still && !this.lastStill) {
                int column;
                if (this.oldNorth) {
                    column = (int) Math.floor(Math.floor(this.mapCenterZ - this.centerY * this.guiToMap) / 256.0) - (left - 1);
                } else {
                    column = (int) Math.floor(Math.floor(this.mapCenterX - this.centerX * this.guiToMap) / 256.0) - (left - 1);
                }

                for (int x = 0; x < this.biomeMapData.getWidth(); ++x) {
                    for (int z = 0; z < this.biomeMapData.getHeight(); ++z) {
                        float floatMapX;
                        float floatMapZ;
                        if (this.oldNorth) {
                            floatMapX = z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                            floatMapZ = -(x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
                        } else {
                            floatMapX = x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
                            floatMapZ = z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                        }

                        int mapX = (int) Math.floor(floatMapX);
                        int mapZ = (int) Math.floor(floatMapZ);
                        int regionX = (int) Math.floor(mapX / 256.0F) - (left - 1);
                        int regionZ = (int) Math.floor(mapZ / 256.0F) - (top - 1);
                        if (!this.oldNorth && regionX != column || this.oldNorth && regionZ != column) {
                            this.persistentMap.compress();
                        }

                        column = !this.oldNorth ? regionX : regionZ;
                        CachedRegion region = this.regions[regionZ * (right + 1 - (left - 1) + 1) + regionX];
                        int id = -1;
                        if (region.getMapData() != null && region.isLoaded() && !region.isEmpty()) {
                            int inRegionX = mapX - region.getX() * region.getWidth();
                            int inRegionZ = mapZ - region.getZ() * region.getWidth();
                            int height = region.getMapData().getHeight(inRegionX, inRegionZ);
                            int light = region.getMapData().getLight(inRegionX, inRegionZ);
                            if (height != 0 || light != 0) {
                                id = region.getMapData().getBiomeID(inRegionX, inRegionZ);
                            }
                        }

                        this.biomeMapData.setBiomeID(x, z, id);
                    }
                }

                this.persistentMap.compress();
                this.biomeMapData.segmentBiomes();
                this.biomeMapData.findCenterOfSegments(true);
            }

            this.lastStill = still;
            boolean displayStill = !this.leftMouseButtonDown;
            displayStill = displayStill && this.zoom == this.zoomGoal;
            displayStill = displayStill && this.deltaX == 0.0F && this.deltaY == 0.0F;
            if (displayStill) {
                int minimumSize = (int) (20.0F * this.scScale / biomeScaleX);
                minimumSize *= minimumSize;
                ArrayList<AbstractMapData.BiomeLabel> labels = this.biomeMapData.getBiomeLabels();
                OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);

                for (AbstractMapData.BiomeLabel biomeLabel : labels) {
                    if (biomeLabel.segmentSize > minimumSize) {
                        int nameWidth = this.chkLen(biomeLabel.name);
                        float x = biomeLabel.x * biomeScaleX / this.scScale;
                        float z = biomeLabel.z * biomeScaleY / this.scScale;
                        this.write(drawContext, biomeLabel.name, x - (nameWidth / 2f), this.top + z - 3.0F, 16777215);
                    }
                }

                OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            }
        }

        modelViewMatrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        if (System.currentTimeMillis() - this.timeOfLastKBInput < 2000L) {
            int scWidth = VoxelConstants.getMinecraft().getWindow().getScaledWidth();
            int scHeight = VoxelConstants.getMinecraft().getWindow().getScaledHeight();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            Identifier GUI_ICONS_TEXTURE = new Identifier("textures/gui/icons.png");
            RenderSystem.setShaderTexture(0, GUI_ICONS_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(775, 769, 1, 0);
            drawContext.drawTexture(GUI_ICONS_TEXTURE, scWidth / 2 - 7, scHeight / 2 - 7, 0, 0, 16, 16);
            RenderSystem.blendFuncSeparate(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        } else {
            this.switchToMouseInput();
        }

        this.overlayBackground(0, this.top, 255, 255);
        this.overlayBackground(this.bottom, this.getHeight(), 255, 255);
        drawContext.drawCenteredTextWithShadow(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 16, 16777215);
        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        if (VoxelConstants.getVoxelMapInstance().getMapOptions().coords) {
            if (!this.editingCoordinates) {
                drawContext.drawTextWithShadow(this.getFontRenderer(), "X: " + x, this.sideMargin, 16, 16777215);
                drawContext.drawTextWithShadow(this.getFontRenderer(), "Z: " + z, this.sideMargin + 64, 16, 16777215);
            } else {
                this.coordinates.render(drawContext, mouseX, mouseY, delta);
            }
        }

        if (this.subworldName != null && !this.subworldName.equals(VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true)) || VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true) != null && !VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true).equals(this.subworldName)) {
            this.buildWorldName();
        }

        drawContext.drawTextWithShadow(this.getFontRenderer(), this.worldNameDisplay, this.getWidth() - this.sideMargin - this.worldNameDisplayLength, 16, 16777215);
        if (this.buttonMultiworld != null) {
            if ((this.subworldName == null || this.subworldName.isEmpty()) && VoxelConstants.getVoxelMapInstance().getWaypointManager().isMultiworld()) {
                if ((int) (System.currentTimeMillis() / 1000L % 2L) == 0) {
                    this.buttonMultiworld.setMessage(this.multiworldButtonNameRed);
                } else {
                    this.buttonMultiworld.setMessage(this.multiworldButtonName);
                }
            } else {
                this.buttonMultiworld.setMessage(this.multiworldButtonName);
            }
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private void drawWaypoint(DrawContext drawContext, Waypoint pt, float cursorCoordX, float cursorCoordZ, Sprite icon, Float r, Float g, Float b) {
        MatrixStack matrixStack = drawContext.getMatrices();
        if (pt.inWorld && pt.inDimension && this.isOnScreen(pt.getX(), pt.getZ())) {
            String name = pt.name;
            if (r == null) {
                r = pt.red;
            }

            if (g == null) {
                g = pt.green;
            }

            if (b == null) {
                b = pt.blue;
            }

            float ptX = pt.getX();
            float ptZ = pt.getZ();
            if (this.backGroundImageInfo != null && this.backGroundImageInfo.isInRange((int) ptX, (int) ptZ) || this.persistentMap.isRegionLoaded((int) ptX, (int) ptZ)) {
                ptX += 0.5F;
                ptZ += 0.5F;
                boolean hover = cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
                boolean target = false;
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                TextureAtlas atlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
                RenderSystem.setShaderTexture(0, atlas.getGlId());
                if (icon == null) {
                    icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
                    if (icon == atlas.getMissingImage()) {
                        icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
                    }
                } else {
                    name = "";
                    target = true;
                }

                RenderSystem.setShaderColor(r, g, b, !pt.enabled && !target && !hover ? 0.3F : 1.0F);
                RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
                RenderSystem.texParameter(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
                if (this.oldNorth) {
                    matrixStack.push();
                    matrixStack.translate(ptX * this.mapToGui, ptZ * this.mapToGui, 0.0);
                    matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
                    matrixStack.translate(-(ptX * this.mapToGui), -(ptZ * this.mapToGui), 0.0);
                    RenderSystem.applyModelViewMatrix();
                }

                this.drawTexturedModalRect(-16.0F / this.scScale + ptX * this.mapToGui, -16.0F / this.scScale + ptZ * this.mapToGui, icon, 32.0F / this.scScale, 32.0F / this.scScale);
                if (this.oldNorth) {
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();
                }

                if (mapOptions.biomeOverlay == 0 && this.options.showWaypointNames || target || hover) {
                    float fontScale = 2.0F / this.scScale;
                    int m = this.chkLen(name) / 2;
                    matrixStack.push();
                    matrixStack.scale(fontScale, fontScale, 1.0F);
                    if (this.oldNorth) {
                        matrixStack.translate(ptX * this.mapToGui / fontScale, ptZ * this.mapToGui / fontScale, 0.0);
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
                        matrixStack.translate(-(ptX * this.mapToGui / fontScale), -(ptZ * this.mapToGui / fontScale), 0.0);
                        RenderSystem.applyModelViewMatrix();
                    }

                    this.write(drawContext, name, ptX * this.mapToGui / fontScale - m, ptZ * this.mapToGui / fontScale + 16.0F / this.scScale / fontScale, !pt.enabled && !target && !hover ? 1442840575 : 16777215);
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();
                    OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
                }
            }
        }

    }

    private boolean isOnScreen(int x, int z) {
        int left;
        int right;
        int top;
        int bottom;
        if (this.oldNorth) {
            left = (int) Math.floor(this.mapCenterZ - (this.centerY * this.guiToMap) * 1.1);
            right = (int) Math.floor(this.mapCenterZ + (this.centerY * this.guiToMap) * 1.1);
            top = (int) Math.floor((-this.mapCenterX) - (this.centerX * this.guiToMap) * 1.1);
            bottom = (int) Math.floor((-this.mapCenterX) + (this.centerX * this.guiToMap) * 1.1);
        } else {
            left = (int) Math.floor(this.mapCenterX - (this.centerX * this.guiToMap) * 1.1);
            right = (int) Math.floor(this.mapCenterX + (this.centerX * this.guiToMap) * 1.1);
            top = (int) Math.floor(this.mapCenterZ - (this.centerY * this.guiToMap) * 1.1);
            bottom = (int) Math.floor(this.mapCenterZ + (this.centerY * this.guiToMap) * 1.1);
        }

        return x > left && x < right && z > top && z < bottom;
    }

    public void renderBackground(DrawContext drawContext) {
        drawContext.fill(0, 0, this.getWidth(), this.getHeight(), -16777216);
    }

    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        vertexBuffer.vertex(0.0, endY, 0.0).texture(0.0F, endY / 32.0F).color(64, 64, 64, endAlpha).next();
        vertexBuffer.vertex(this.getWidth(), endY, 0.0).texture(this.width / 32.0F, endY / 32.0F).color(64, 64, 64, endAlpha).next();
        vertexBuffer.vertex(this.getWidth(), startY, 0.0).texture(this.width / 32.0F, startY / 32.0F).color(64, 64, 64, startAlpha).next();
        vertexBuffer.vertex(0.0, startY, 0.0).texture(0.0F, startY / 32.0F).color(64, 64, 64, startAlpha).next();
        tessellator.draw();
    }

    public void tick() {
        //this.coordinates.setFocused(true);
    }

    @Override
    public void removed() {
        VoxelConstants.getMinecraft().options.forwardKey.setBoundKey(this.forwardCode);
        VoxelConstants.getMinecraft().options.leftKey.setBoundKey(this.leftCode);
        VoxelConstants.getMinecraft().options.backKey.setBoundKey(this.backCode);
        VoxelConstants.getMinecraft().options.rightKey.setBoundKey(this.rightCode);
        VoxelConstants.getMinecraft().options.sprintKey.setBoundKey(this.sprintCode);
        this.keyBindForward.setBoundKey(this.nullInput);
        this.keyBindLeft.setBoundKey(this.nullInput);
        this.keyBindBack.setBoundKey(this.nullInput);
        this.keyBindRight.setBoundKey(this.nullInput);
        this.keyBindSprint.setBoundKey(this.nullInput);
        KeyBinding.updateKeysByCode();
        KeyBinding.unpressAll();
        synchronized (this.closedLock) {
            this.closed = true;
            this.persistentMap.getRegions(0, -1, 0, -1);
            this.regions = new CachedRegion[0];
        }
    }

    public void drawTexturedModalRect(float x, float y, float width, float height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexBuffer.vertex(x + 0.0F, y + height, 0).texture(0.0F, 1.0F).next();
        vertexBuffer.vertex(x + width, y + height, 0).texture(1.0F, 1.0F).next();
        vertexBuffer.vertex(x + width, y + 0.0F, 0).texture(1.0F, 0.0F).next();
        vertexBuffer.vertex(x + 0.0F, y + 0.0F, 0).texture(0.0F, 0.0F).next();
        tessellator.draw();
    }

    public void drawTexturedModalRect(float xCoord, float yCoord, Sprite icon, float widthIn, float heightIn) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexBuffer.vertex(xCoord + 0.0F, yCoord + heightIn, 0).texture(icon.getMinU(), icon.getMaxV()).next();
        vertexBuffer.vertex(xCoord + widthIn, yCoord + heightIn, 0).texture(icon.getMaxU(), icon.getMaxV()).next();
        vertexBuffer.vertex(xCoord + widthIn, yCoord + 0.0F, 0).texture(icon.getMaxU(), icon.getMinV()).next();
        vertexBuffer.vertex(xCoord + 0.0F, yCoord + 0.0F, 0).texture(icon.getMinU(), icon.getMinV()).next();
        tessellator.draw();
    }

    private void createPopup(int x, int y, int directX, int directY) {
        ArrayList<Popup.PopupEntry> entries = new ArrayList<>();
        float cursorX = directX;
        float cursorY = directY - this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
        }

        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        Popup.PopupEntry entry;
        if (hovered != null && this.waypointManager.getWaypoints().contains(hovered)) {
            entry = new Popup.PopupEntry(I18n.translate("selectServer.edit"), 4, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.translate("selectServer.delete"), 5, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.translate(hovered != this.waypointManager.getHighlightedWaypoint() ? "minimap.waypoints.highlight" : "minimap.waypoints.removehighlight"), 1, true, true);
        } else {
            entry = new Popup.PopupEntry(I18n.translate("minimap.waypoints.newwaypoint"), 0, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.translate(hovered == null ? "minimap.waypoints.highlight" : "minimap.waypoints.removehighlight"), 1, true, true);
        }
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.translate("minimap.waypoints.teleportto"), 3, true, true);
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.translate("minimap.waypoints.share"), 2, true, true);
        entries.add(entry);

        this.createPopup(x, y, directX, directY, entries);
    }

    private Waypoint getHovered(float cursorCoordX, float cursorCoordZ) {
        Waypoint waypoint = null;

        for (Waypoint pt : this.waypointManager.getWaypoints()) {
            float ptX = pt.getX() + 0.5F;
            float ptZ = pt.getZ() + 0.5F;
            boolean hover = pt.inDimension && pt.inWorld && cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
            if (hover) {
                waypoint = pt;
            }
        }

        if (waypoint == null) {
            Waypoint pt = this.waypointManager.getHighlightedWaypoint();
            if (pt != null) {
                float ptX = pt.getX() + 0.5F;
                float ptZ = pt.getZ() + 0.5F;
                boolean hover = pt.inDimension && pt.inWorld && cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
                if (hover) {
                    waypoint = pt;
                }
            }
        }

        return waypoint;
    }

    @Override
    public void popupAction(Popup popup, int action) {
        int mouseDirectX = popup.clickedDirectX;
        int mouseDirectY = popup.clickedDirectY;
        float cursorX = mouseDirectX;
        float cursorY = mouseDirectY - this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
        }

        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        int y = this.persistentMap.getHeightAt(x, z);
        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        this.editClicked = false;
        this.addClicked = false;
        this.deleteClicked = false;
        double dimensionScale = VoxelConstants.getPlayer().getWorld().getDimension().coordinateScale();
        switch (action) {
            case 0 -> {
                if (hovered != null) {
                    x = hovered.getX();
                    z = hovered.getZ();
                }
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
                dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().getWorld()));
                this.newWaypoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y, true, r, g, b, "", VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
                VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.newWaypoint, false));
            }
            case 1 -> {
                if (hovered != null) {
                    this.waypointManager.setHighlightedWaypoint(hovered, true);
                } else {
                    y = y > VoxelConstants.getPlayer().getWorld().getBottomY() ? y : 64;
                    TreeSet<DimensionContainer> dimensions2 = new TreeSet<>();
                    dimensions2.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().getWorld()));
                    Waypoint fakePoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y, true, 1.0F, 0.0F, 0.0F, "", VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions2);
                    this.waypointManager.setHighlightedWaypoint(fakePoint, true);
                }
            }
            case 2 -> {
                if (hovered != null) {
                    CommandUtils.sendWaypoint(hovered);
                } else {
                    y = y > VoxelConstants.getPlayer().getWorld().getBottomY() ? y : 64;
                    CommandUtils.sendCoordinate(x, y, z);
                }
            }
            case 3 -> {
                if (hovered == null) {
                    if (y == 0)
                        y = (!(VoxelConstants.getPlayer().getWorld().getDimension().hasCeiling()) ? VoxelConstants.getPlayer().getWorld().getTopY() : 64);
                    FabricModVoxelMap.instance.playerRunTeleportCommand(x, y, z);
                    break;
                }

                this.selectedWaypoint = hovered;
                y = selectedWaypoint.getY() > VoxelConstants.getPlayer().getWorld().getBottomY() ? selectedWaypoint.getY() : (!(VoxelConstants.getPlayer().getWorld().getDimension().hasCeiling()) ? VoxelConstants.getPlayer().getWorld().getTopY() : 64);
                FabricModVoxelMap.instance.playerRunTeleportCommand(selectedWaypoint.getX(), y, selectedWaypoint.getZ());
            }
            case 4 -> {
                if (hovered != null) {
                    this.editClicked = true;
                    this.selectedWaypoint = hovered;
                    VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, hovered, true));
                }
            }
            case 5 -> {
                if (hovered != null) {
                    this.deleteClicked = true;
                    this.selectedWaypoint = hovered;
                    Text title = Text.translatable("minimap.waypoints.deleteconfirm");
                    Text explanation = Text.translatable("selectServer.deleteWarning", this.selectedWaypoint.name);
                    Text affirm = Text.translatable("selectServer.deleteButton");
                    Text deny = Text.translatable("gui.cancel");
                    ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
                    VoxelConstants.getMinecraft().setScreen(confirmScreen);
                }
            }
            default -> VoxelConstants.getLogger().warn("unimplemented command");
        }

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
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (b) {
                this.waypointManager.saveWaypoints();
            }
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (b) {
                this.waypointManager.addWaypoint(this.newWaypoint);
            }
        }

        VoxelConstants.getMinecraft().setScreen(this);
    }

    private int chkLen(String string) {
        return this.getFontRenderer().getWidth(string);
    }

    private void write(DrawContext drawContext, String string, float x, float y, int color) {
        drawContext.drawTextWithShadow(this.textRenderer, string, (int) x, (int) y, color);
    }
}
