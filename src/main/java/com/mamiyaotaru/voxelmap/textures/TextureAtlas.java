package com.mamiyaotaru.voxelmap.textures;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TextureAtlas extends AbstractTexture {
    private final HashMap<String, Sprite> mapRegisteredSprites;
    private final HashMap<String, Sprite> mapUploadedSprites;
    private final String basePath;
    private final IIconCreator iconCreator;
    private final Sprite missingImage;
    private final Sprite failedImage;
    private Stitcher stitcher;

    public TextureAtlas(String basePath) {
        this(basePath, null);
    }

    public TextureAtlas(String basePath, IIconCreator iconCreator) {
        this.mapRegisteredSprites = Maps.newHashMap();
        this.mapUploadedSprites = Maps.newHashMap();
        this.missingImage = new Sprite("missingno");
        this.failedImage = new Sprite("notfound");
        this.basePath = basePath;
        this.iconCreator = iconCreator;
    }

    private void initMissingImage() {
        int[] missingTextureData = new int[1];
        Arrays.fill(missingTextureData, 0);
        this.missingImage.setIconWidth(1);
        this.missingImage.setIconHeight(1);
        this.missingImage.setTextureData(missingTextureData);
        this.failedImage.copyFrom(this.missingImage);
        this.failedImage.setTextureData(missingTextureData);
    }

    public void load(ResourceManager manager) {
        if (this.iconCreator != null) {
            this.loadTextureAtlas(this.iconCreator);
        }

    }

    public void reset() {
        this.mapRegisteredSprites.clear();
        this.mapUploadedSprites.clear();
        this.initMissingImage();
        int glMaxTextureSize = RenderSystem.maxSupportedTextureSize();
        this.stitcher = new Stitcher(glMaxTextureSize, glMaxTextureSize, 0);
    }

    public void loadTextureAtlas(IIconCreator iconCreator) {
        this.reset();
        iconCreator.addIcons(this);
        this.stitch();
    }

    public void stitch() {
        for (Map.Entry<String, Sprite> entry : this.mapRegisteredSprites.entrySet()) {
            Sprite icon = entry.getValue();
            this.stitcher.addSprite(icon);
        }

        this.stitcher.doStitch();

        VoxelConstants.getLogger().info("Created: {}x{} {}-atlas", new Object[]{this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath});
        TextureUtilLegacy.allocateTextureImpl(this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
        int[] zeros = new int[this.stitcher.getCurrentImageWidth() * this.stitcher.getCurrentImageHeight()];
        Arrays.fill(zeros, 0);
        TextureUtilLegacy.uploadTexture(this.getGlId(), zeros, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
        HashMap<String, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);

        for (Sprite icon : this.stitcher.getStitchSlots()) {
            String iconName = icon.getIconName();
            tempMapRegisteredSprites.remove(iconName);
            this.mapUploadedSprites.put(iconName, icon);

            try {
                TextureUtilLegacy.uploadTextureMipmap(new int[][]{icon.getTextureData()}, icon.getIconWidth(), icon.getIconHeight(), icon.getOriginX(), icon.getOriginY(), false, false);
            } catch (Throwable var10) {
                CrashReport crashReport = CrashReport.create(var10, "Stitching texture atlas");
                CrashReportSection crashReportCategory = crashReport.addElement("Texture being stitched together");
                crashReportCategory.add("Atlas path", this.basePath);
                crashReportCategory.add("Sprite", icon);
                throw new CrashException(crashReport);
            }
        }

        for (Sprite icon : tempMapRegisteredSprites.values()) {
            icon.copyFrom(this.missingImage);
        }

        this.mapRegisteredSprites.clear();
        this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        ImageUtils.saveImage(this.basePath.replaceAll("/", "_"), this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
    }

    public void stitchNew() {
        for (Map.Entry<String, Sprite> entry : this.mapRegisteredSprites.entrySet()) {
            Sprite icon = entry.getValue();
            this.stitcher.addSprite(icon);
        }

        int oldWidth = this.stitcher.getCurrentImageWidth();
        int oldHeight = this.stitcher.getCurrentImageHeight();

        this.stitcher.doStitchNew();

        if (oldWidth == this.stitcher.getCurrentImageWidth() && oldHeight == this.stitcher.getCurrentImageHeight()) {
            RenderSystem.bindTexture(this.glId);
        } else {
            VoxelConstants.getLogger().info("Resized to: {}x{} {}-atlas", new Object[]{this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight(), this.basePath});
            TextureUtilLegacy.allocateTextureImpl(this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
            int[] zeros = new int[this.stitcher.getCurrentImageWidth() * this.stitcher.getCurrentImageHeight()];
            Arrays.fill(zeros, 0);
            TextureUtilLegacy.uploadTexture(this.getGlId(), zeros, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
        }

        HashMap<String, Sprite> tempMapRegisteredSprites = Maps.newHashMap(this.mapRegisteredSprites);

        for (Sprite icon : this.stitcher.getStitchSlots()) {
            String iconName = icon.getIconName();
            tempMapRegisteredSprites.remove(iconName);
            this.mapUploadedSprites.put(iconName, icon);

            try {
                TextureUtilLegacy.uploadTextureMipmap(new int[][]{icon.getTextureData()}, icon.getIconWidth(), icon.getIconHeight(), icon.getOriginX(), icon.getOriginY(), false, false);
            } catch (Throwable var11) {
                CrashReport crashReport = CrashReport.create(var11, "Stitching texture atlas");
                CrashReportSection crashReportCategory = crashReport.addElement("Texture being stitched together");
                crashReportCategory.add("Atlas path", this.basePath);
                crashReportCategory.add("Sprite", icon);
                throw new CrashException(crashReport);
            }
        }

        for (Sprite icon : tempMapRegisteredSprites.values()) {
            icon.copyFrom(this.missingImage);
        }

        this.mapRegisteredSprites.clear();
        this.missingImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        this.failedImage.initSprite(this.getHeight(), this.getWidth(), 0, 0);
        if (oldWidth != this.stitcher.getCurrentImageWidth() || oldHeight != this.stitcher.getCurrentImageHeight()) {
            ImageUtils.saveImage(this.basePath.replaceAll("/", "_"), this.getGlId(), 0, this.stitcher.getCurrentImageWidth(), this.stitcher.getCurrentImageHeight());
        }

    }

    public Sprite getIconAt(float x, float y) {
        return this.mapUploadedSprites.entrySet().stream().map(stringSpriteEntry -> (Sprite) ((Map.Entry<?, ?>) stringSpriteEntry).getValue()).filter(icon -> x >= icon.originX && x < (icon.originX + icon.width) && y >= icon.originY && y < (icon.originY + icon.height)).findFirst().orElse(this.missingImage);
    }

    public Sprite getAtlasSprite(String name) {
        Sprite icon = this.mapUploadedSprites.get(name);
        if (icon == null) {
            icon = this.missingImage;
        }

        return icon;
    }

    public Sprite getAtlasSpriteIncludingYetToBeStitched(String name) {
        Sprite icon = this.mapUploadedSprites.get(name);
        if (icon == null) {
            icon = this.mapRegisteredSprites.get(name);
        }

        if (icon == null) {
            icon = this.missingImage;
        }

        return icon;
    }

    public Sprite registerIconForResource(Identifier resourceLocation, ResourceManager resourceManager) {
        if (resourceLocation == null) {
            throw new IllegalArgumentException("Location cannot be null!");
        } else {
            Sprite icon = this.mapRegisteredSprites.get(resourceLocation.toString());
            if (icon == null) {
                icon = Sprite.spriteFromResourceLocation(resourceLocation);

                try {
                    Optional<Resource> entryResource = resourceManager.getResource(resourceLocation);
                    BufferedImage entryBufferedImage = TextureUtilLegacy.readBufferedImage(entryResource.get().getInputStream());
                    icon.bufferedImageToIntData(entryBufferedImage);
                    entryBufferedImage.flush();
                } catch (RuntimeException var6) {
                    VoxelConstants.getLogger().error("Unable to parse metadata from " + resourceLocation, var6);
                } catch (IOException var7) {
                    VoxelConstants.getLogger().error("Using missing texture, unable to load " + resourceLocation, var7);
                }

                this.mapRegisteredSprites.put(resourceLocation.toString(), icon);
            }

            return icon;
        }
    }

    public Sprite registerIconForBufferedImage(String name, BufferedImage bufferedImage) {
        if (name != null && !name.isEmpty()) {
            Sprite icon = this.mapRegisteredSprites.get(name);
            if (icon == null) {
                icon = Sprite.spriteFromString(name);
                icon.bufferedImageToIntData(bufferedImage);
                bufferedImage.flush();

                for (Sprite existing : this.mapUploadedSprites.values()) {
                    if (Arrays.equals(existing.imageData, icon.imageData)) {
                        this.registerMaskedIcon(name, existing);
                        return existing;
                    }
                }

                for (Sprite existing : this.mapRegisteredSprites.values()) {
                    if (Arrays.equals(existing.imageData, icon.imageData)) {
                        this.registerMaskedIcon(name, existing);
                        return existing;
                    }
                }

                this.mapRegisteredSprites.put(name, icon);
            }

            return icon;
        } else {
            throw new IllegalArgumentException("Name cannot be null!");
        }
    }

    public Sprite getMissingImage() {
        return this.missingImage;
    }

    public Sprite getFailedImage() {
        return this.failedImage;
    }

    public void registerFailedIcon(String name) {
        this.mapUploadedSprites.put(name, this.failedImage);
    }

    public void registerMaskedIcon(String name, Sprite originalIcon) {
        Sprite existingIcon = this.mapUploadedSprites.get(name);
        if (existingIcon == null) {
            existingIcon = this.mapRegisteredSprites.get(name);
        }

        if (existingIcon == null) {
            this.mapUploadedSprites.put(name, originalIcon);
        }

    }

    public int getWidth() {
        return this.stitcher.getCurrentWidth();
    }

    public int getHeight() {
        return this.stitcher.getCurrentHeight();
    }

    public int getImageWidth() {
        return this.stitcher.getCurrentImageWidth();
    }

    public int getImageHeight() {
        return this.stitcher.getCurrentImageHeight();
    }
}
