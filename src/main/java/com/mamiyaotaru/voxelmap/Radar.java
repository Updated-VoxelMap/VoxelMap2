package com.mamiyaotaru.voxelmap;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.StitcherException;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.VillagerResourceMetadata;
import net.minecraft.client.render.entity.model.AxolotlEntityModel;
import net.minecraft.client.render.entity.model.BatEntityModel;
import net.minecraft.client.render.entity.model.BeeEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.BlazeEntityModel;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.entity.model.DolphinEntityModel;
import net.minecraft.client.render.entity.model.DrownedEntityModel;
import net.minecraft.client.render.entity.model.EndermanEntityModel;
import net.minecraft.client.render.entity.model.EndermiteEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.GhastEntityModel;
import net.minecraft.client.render.entity.model.GuardianEntityModel;
import net.minecraft.client.render.entity.model.HoglinEntityModel;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.client.render.entity.model.IllagerEntityModel;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.client.render.entity.model.MagmaCubeEntityModel;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.client.render.entity.model.PhantomEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import net.minecraft.client.render.entity.model.RabbitEntityModel;
import net.minecraft.client.render.entity.model.RavagerEntityModel;
import net.minecraft.client.render.entity.model.ShulkerEntityModel;
import net.minecraft.client.render.entity.model.SilverfishEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.render.entity.model.SlimeEntityModel;
import net.minecraft.client.render.entity.model.SnifferEntityModel;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.minecraft.client.render.entity.model.SquidEntityModel;
import net.minecraft.client.render.entity.model.StriderEntityModel;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.render.entity.model.WardenEntityModel;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.DyeableHorseArmorItem;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Radar implements IRadar {
    private LayoutVariables layoutVariables;
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    private boolean newMobs;
    private boolean completedLoading;
    private int timer = 500;
    private float direction;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);
    public final HashMap<String, Integer> mpContactsSkinGetTries = new HashMap<>();
    public final HashMap<String, Integer> contactsSkinGetTries = new HashMap<>();
    private Sprite clothIcon;
    private static final int UNKNOWN = EnumMobs.UNKNOWN.ordinal();
    private final String[] armorNames = {"cloth", "clothOverlay", "clothOuter", "clothOverlayOuter", "chain", "iron", "gold", "diamond", "netherite", "turtle"};
    private boolean randomobsOptifine;
    private Map<String, Object> mapProperties;
    private Object randomEntity;
    private Class<?> randomEntityClass;
    private Method setEntityMethod;
    private Class<?> randomEntitiesPropertiesClass;
    private Method getEntityTextureMethod;
    private boolean hasCustomNPCs;
    private Class<?> entityCustomNpcClass;
    private Field modelDataField;
    private Method getEntityMethod;
    private boolean lastOutlines = true;
    private SkullEntityModel playerSkullModel;
    private BipedEntityModel<LivingEntity> bipedArmorModel;
    private SkeletonEntityModel<net.minecraft.entity.boss.WitherEntity> strayOverlayModel;
    private DrownedEntityModel<net.minecraft.entity.mob.ZombieEntity> drownedOverlayModel;
    private BipedEntityModel<LivingEntity> piglinArmorModel;
    private NativeImageBackedTexture nativeBackedTexture = new NativeImageBackedTexture(2, 2, false);
    private final Identifier nativeBackedTextureLocation = new Identifier("voxelmap", "tempimage");
    private final Vector3f fullbright = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final HashMap<UUID, BufferedImage> entityIconMap = new HashMap<>();

    private static final Int2ObjectMap<Identifier> LEVEL_TO_ID = Util.make(new Int2ObjectOpenHashMap<>(), int2ObjectOpenHashMap -> {
        int2ObjectOpenHashMap.put(1, new Identifier("stone"));
        int2ObjectOpenHashMap.put(2, new Identifier("iron"));
        int2ObjectOpenHashMap.put(3, new Identifier("gold"));
        int2ObjectOpenHashMap.put(4, new Identifier("emerald"));
        int2ObjectOpenHashMap.put(5, new Identifier("diamond"));
    });
    private static final Map<HorseMarking, Object> TEXTURES = Util.make(Maps.newEnumMap(HorseMarking.class), enumMap -> {
        enumMap.put(HorseMarking.NONE, null);
        enumMap.put(HorseMarking.WHITE, new Identifier("textures/entity/horse/horse_markings_white.png"));
        enumMap.put(HorseMarking.WHITE_FIELD, new Identifier("textures/entity/horse/horse_markings_whitefield.png"));
        enumMap.put(HorseMarking.WHITE_DOTS, new Identifier("textures/entity/horse/horse_markings_whitedots.png"));
        enumMap.put(HorseMarking.BLACK_DOTS, new Identifier("textures/entity/horse/horse_markings_blackdots.png"));
    });

    public Radar() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("mobs");
        this.textureAtlas.setFilter(false, false);

        try {
            Class<?> randomEntitiesClass = Class.forName("net.optifine.RandomEntities");
            Field mapPropertiesField = randomEntitiesClass.getDeclaredField("mapProperties");
            mapPropertiesField.setAccessible(true);
            this.mapProperties = (Map) mapPropertiesField.get(null);
            Field randomEntityField = randomEntitiesClass.getDeclaredField("randomEntity");
            randomEntityField.setAccessible(true);
            this.randomEntity = randomEntityField.get(null);
            Class<?> iRandomEntityClass = Class.forName("net.optifine.IRandomEntity");
            this.randomEntityClass = Class.forName("net.optifine.RandomEntity");
            Class<?>[] argClasses1 = new Class[]{Entity.class};
            this.setEntityMethod = this.randomEntityClass.getDeclaredMethod("setEntity", argClasses1);
            this.randomEntitiesPropertiesClass = Class.forName("net.optifine.RandomEntityProperties");
            Class<?>[] argClasses2 = new Class[]{Identifier.class, iRandomEntityClass};
            this.getEntityTextureMethod = this.randomEntitiesPropertiesClass.getDeclaredMethod("getTextureLocation", argClasses2);
            this.randomobsOptifine = true;
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | SecurityException |
                 IllegalArgumentException | IllegalAccessException var7) {
            this.randomobsOptifine = false;
        }

        try {
            this.entityCustomNpcClass = Class.forName("noppes.npcs.entity.EntityCustomNpc");
            Class<?> modelDataClass = Class.forName("noppes.npcs.ModelData");
            this.modelDataField = this.entityCustomNpcClass.getField("modelData");
            Class<?> entityNPCInterfaceClass = Class.forName("noppes.npcs.entity.EntityNPCInterface");
            this.getEntityMethod = modelDataClass.getMethod("getEntity", entityNPCInterfaceClass);
            this.hasCustomNPCs = true;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException var4) {
            this.hasCustomNPCs = false;
        }

    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.mpContactsSkinGetTries.clear();
            this.contactsSkinGetTries.clear();
            this.textureAtlas.reset();
            TexturedModelData texturedModelData12 = SkullEntityModel.getHeadTexturedModelData();
            ModelPart skullModelPart = texturedModelData12.createModel();
            this.playerSkullModel = new SkullEntityModel(skullModelPart);
            Dilation ARMOR_DILATION = new Dilation(1.0F);
            TexturedModelData texturedModelData2 = TexturedModelData.of(BipedEntityModel.getModelData(ARMOR_DILATION, 0.0F), 64, 32);
            ModelPart bipedArmorModelPart = texturedModelData2.createModel();
            this.bipedArmorModel = new BipedEntityModel<>(bipedArmorModelPart);
            TexturedModelData strayModelData = TexturedModelData.of(BipedEntityModel.getModelData(new Dilation(0.25F), 0.0F), 64, 32);
            ModelPart strayOverlayModelPart = strayModelData.createModel();
            this.strayOverlayModel = new SkeletonEntityModel<>(strayOverlayModelPart);
            TexturedModelData drownedModelData = DrownedEntityModel.getTexturedModelData(new Dilation(0.25F));
            ModelPart drownedOverlayModelPart = drownedModelData.createModel();
            this.drownedOverlayModel = new DrownedEntityModel<>(drownedOverlayModelPart);
            TexturedModelData texturedModelData3 = TexturedModelData.of(BipedEntityModel.getModelData(new Dilation(1.02F), 0.0F), 64, 32);
            ModelPart piglinArmorModelPart = texturedModelData3.createModel();
            this.piglinArmorModel = new BipedEntityModel<>(piglinArmorModelPart);
            if (ReflectionUtils.classExists("com.prupe.mcpatcher.mob.MobOverlay") && ImageUtils.loadImage(new Identifier("mcpatcher/mob/cow/mooshroom_overlay.png"), 0, 0, 1, 1) != null) {
                EnumMobs.MOOSHROOM.secondaryResourceLocation = new Identifier("mcpatcher/mob/cow/mooshroom_overlay.png");
            } else {
                EnumMobs.MOOSHROOM.secondaryResourceLocation = new Identifier("textures/block/red_mushroom.png");
            }

            for (int t = 0; t < EnumMobs.values().length - 1; ++t) {
                String identifier = "minecraft." + EnumMobs.values()[t].id;
                String identifierSimple = EnumMobs.values()[t].id;
                String spriteName = identifier + EnumMobs.values()[t].resourceLocation.toString();
                spriteName = spriteName + (EnumMobs.values()[t].secondaryResourceLocation != null ? EnumMobs.values()[t].secondaryResourceLocation.toString() : "");
                BufferedImage mobImage = this.getCustomMobImage(identifier, identifierSimple);
                if (mobImage != null) {
                    Sprite sprite = this.textureAtlas.registerIconForBufferedImage(identifier + "custom", mobImage);
                    this.textureAtlas.registerMaskedIcon(spriteName, sprite);
                } else {
                    this.textureAtlas.registerFailedIcon(identifier + "custom");
                    if (EnumMobs.values()[t].expectedWidth > 0.5) {
                        mobImage = this.createImageFromTypeAndResourceLocations(EnumMobs.values()[t], EnumMobs.values()[t].resourceLocation, EnumMobs.values()[t].secondaryResourceLocation, null);
                        if (mobImage != null) {
                            float scale = mobImage.getWidth() / EnumMobs.values()[t].expectedWidth;
                            mobImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobImage, 4.0F / scale)), this.options.outlines, 2);
                            this.textureAtlas.registerIconForBufferedImage(spriteName, mobImage);
                        }
                    }
                }
            }

            BufferedImage[] armorImages = {ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1.png"), 8, 8, 8, 8), ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1.png"), 40, 8, 8, 8), ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1_overlay.png"), 8, 8, 8, 8), ImageUtils.loadImage(new Identifier("textures/models/armor/leather_layer_1_overlay.png"), 40, 8, 8, 8)};

            for (int t = 0; t < armorImages.length; ++t) {
                float scale = armorImages[t].getWidth() / 8.0F;
                armorImages[t] = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorImages[t], 4.0F / scale * 47.0F / 38.0F)), this.options.outlines && t != 2 && t != 3, true, 37.6F, 37.6F, 2);
                Sprite icon = this.textureAtlas.registerIconForBufferedImage("armor " + this.armorNames[t], armorImages[t]);
                if (t == 0) {
                    this.clothIcon = icon;
                }
            }

            BufferedImage zombie = ImageUtils.loadImage(EnumMobs.ZOMBIE.resourceLocation, 8, 8, 8, 8, 64, 64);
            float scale = zombie.getWidth() / 8.0F;
            zombie = ImageUtils.scaleImage(zombie, 4.0F / scale * 47.0F / 38.0F);
            BufferedImage zombieHat = ImageUtils.loadImage(EnumMobs.ZOMBIE.resourceLocation, 40, 8, 8, 8, 64, 64);
            zombieHat = ImageUtils.scaleImage(zombieHat, 4.0F / scale * 47.0F / 35.0F);
            zombie = ImageUtils.addImages(ImageUtils.addImages(new BufferedImage(zombieHat.getWidth(), zombieHat.getHeight() + 8, 6), zombie, (zombieHat.getWidth() - zombie.getWidth()) / 2.0F, (zombieHat.getHeight() - zombie.getHeight()) / 2.0F, zombieHat.getWidth(), zombieHat.getHeight() + 8), zombieHat, 0.0F, 0.0F, zombieHat.getWidth(), zombieHat.getHeight() + 8);
            zombieHat.flush();
            zombie = ImageUtils.fillOutline(ImageUtils.pad(zombie), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.ZOMBIE.id + EnumMobs.ZOMBIE.resourceLocation.toString() + "head", zombie);
            BufferedImage skeleton = ImageUtils.loadImage(EnumMobs.SKELETON.resourceLocation, 8, 8, 8, 8, 64, 32);
            scale = skeleton.getWidth() / 8.0F;
            skeleton = ImageUtils.scaleImage(skeleton, 4.0F / scale * 47.0F / 38.0F);
            skeleton = ImageUtils.addImages(new BufferedImage(skeleton.getWidth(), skeleton.getHeight() + 8, 6), skeleton, 0.0F, 0.0F, skeleton.getWidth(), skeleton.getHeight() + 8);
            skeleton = ImageUtils.fillOutline(ImageUtils.pad(skeleton), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.SKELETON.id + EnumMobs.SKELETON.resourceLocation.toString() + "head", skeleton);
            BufferedImage witherSkeleton = ImageUtils.loadImage(EnumMobs.SKELETONWITHER.resourceLocation, 8, 8, 8, 8, 64, 32);
            scale = witherSkeleton.getWidth() / 8.0F;
            witherSkeleton = ImageUtils.scaleImage(witherSkeleton, 4.0F / scale * 47.0F / 38.0F);
            witherSkeleton = ImageUtils.addImages(new BufferedImage(witherSkeleton.getWidth(), witherSkeleton.getHeight() + 8, 6), witherSkeleton, 0.0F, 0.0F, witherSkeleton.getWidth(), witherSkeleton.getHeight() + 8);
            witherSkeleton = ImageUtils.fillOutline(ImageUtils.pad(witherSkeleton), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.SKELETONWITHER.id + EnumMobs.SKELETONWITHER.resourceLocation.toString() + "head", witherSkeleton);
            BufferedImage creeper = ImageUtils.addImages(ImageUtils.blankImage(EnumMobs.CREEPER.resourceLocation, 8, 10), ImageUtils.loadImage(EnumMobs.CREEPER.resourceLocation, 8, 8, 8, 8), 0.0F, 0.0F, 8, 10);
            scale = creeper.getWidth() / EnumMobs.CREEPER.expectedWidth;
            creeper = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(creeper, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 37.6F, 37.6F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.CREEPER.id + EnumMobs.CREEPER.resourceLocation.toString() + "head", creeper);
            BufferedImage dragon = this.createImageFromTypeAndResourceLocations(EnumMobs.ENDERDRAGON, EnumMobs.ENDERDRAGON.resourceLocation, null, null);
            scale = dragon.getWidth() / EnumMobs.ENDERDRAGON.expectedWidth;
            dragon = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(dragon, 4.0F / scale)), this.options.outlines, true, 32.0F, 32.0F, 2);
            this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.ENDERDRAGON.id + EnumMobs.ENDERDRAGON.resourceLocation.toString() + "head", dragon);
            BufferedImage sheepFur = ImageUtils.loadImage(new Identifier("textures/entity/sheep/sheep_fur.png"), 6, 6, 6, 6);
            scale = sheepFur.getWidth() / 6.0F;
            sheepFur = ImageUtils.scaleImage(sheepFur, 4.0F / scale * 1.0625F);
            int chop = (int) Math.max(1.0F, 2.0F);
            ImageUtils.eraseArea(sheepFur, chop, chop, sheepFur.getWidth() - chop * 2, sheepFur.getHeight() - chop * 2, sheepFur.getWidth(), sheepFur.getHeight());
            sheepFur = ImageUtils.fillOutline(ImageUtils.pad(sheepFur), this.options.outlines, true, 27.5F, 27.5F, (int) Math.max(1.0F, 2.0F));
            this.textureAtlas.registerIconForBufferedImage("sheepfur", sheepFur);
            Identifier fontResourceLocation = new Identifier("textures/font/ascii.png");
            BufferedImage fontImage = ImageUtils.loadImage(fontResourceLocation, 0, 0, 128, 128, 128, 128);
            if (fontImage.getWidth() > 512 || fontImage.getHeight() > 512) {
                int maxDim = Math.max(fontImage.getWidth(), fontImage.getHeight());
                float scaleBy = 512.0F / maxDim;
                fontImage = ImageUtils.scaleImage(fontImage, scaleBy);
            }

            this.textureAtlas.stitch();
            this.completedLoading = true;
        } catch (Exception var30) {
            VoxelConstants.getLogger().error("Failed getting mobs" + var30.getLocalizedMessage(), var30);
        }

    }

    private BufferedImage createImageFromTypeAndResourceLocations(EnumMobs type, Identifier resourceLocation, Identifier resourceLocationSecondary, Entity entity) {
        BufferedImage mobImage = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
        BufferedImage mobImageSecondary = null;
        if (resourceLocationSecondary != null) {
            mobImageSecondary = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationSecondary);
        }

        try {
            return this.createImageFromTypeAndImages(type, mobImage, mobImageSecondary, entity);
        } catch (Exception var8) {
            return null;
        }
    }

    private BufferedImage createImageFromTypeAndImages(EnumMobs type, BufferedImage mobImage, BufferedImage mobImageSecondary, Entity entity) {
        BufferedImage image = null;
        switch (type) {
            case GENERICHOSTILE ->
                    image = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16);
            case GENERICNEUTRAL ->
                    image = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16);
            case GENERICTAME ->
                    image = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/tame.png"), 0, 0, 16, 16, 16, 16);
            case BAT ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 12, 64, 64), ImageUtils.loadImage(mobImage, 25, 1, 3, 4), 0.0F, 0.0F, 8, 12), ImageUtils.flipHorizontal(ImageUtils.loadImage(mobImage, 25, 1, 3, 4)), 5.0F, 0.0F, 8, 12), ImageUtils.loadImage(mobImage, 6, 6, 6, 6), 1.0F, 3.0F, 8, 12);
            case CHICKEN ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 2, 3, 6, 6), ImageUtils.loadImage(mobImage, 16, 2, 4, 2), 1.0F, 2.0F, 6, 6), ImageUtils.loadImage(mobImage, 16, 6, 2, 2), 2.0F, 4.0F, 6, 6);
            case COD ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 5, 32, 32), ImageUtils.loadImage(mobImage, 15, 3, 1, 3, 32, 32), 1.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 16, 3, 3, 4, 32, 32), 2.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 9, 7, 7, 4, 32, 32), 5.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 26, 7, 4, 4, 32, 32), 12.0F, 1.0F, 16, 5), ImageUtils.loadImage(mobImage, 26, 0, 6, 1, 32, 32), 4.0F, 0.0F, 16, 5);
            case ENDERDRAGON ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 20, 256, 256), ImageUtils.loadImage(mobImage, 128, 46, 16, 16, 256, 256), 0.0F, 4.0F, 16, 16), ImageUtils.loadImage(mobImage, 192, 60, 12, 5, 256, 256), 2.0F, 11.0F, 16, 16), ImageUtils.loadImage(mobImage, 192, 81, 12, 4, 256, 256), 2.0F, 16.0F, 16, 16), ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256), 3.0F, 0.0F, 16, 16), ImageUtils.flipHorizontal(ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256)), 11.0F, 0.0F, 16, 16);
            case GHAST, GHASTATTACKING -> image = ImageUtils.loadImage(mobImage, 16, 16, 16, 16);
            case GUARDIAN ->
                    image = ImageUtils.scaleImage(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 16, 16, 12, 12), ImageUtils.loadImage(mobImage, 9, 1, 2, 2), 5.0F, 5.5F, 12, 12), 0.5F);
            case GUARDIANELDER ->
                    image = ImageUtils.addImages(ImageUtils.loadImage(mobImage, 16, 16, 12, 12), ImageUtils.loadImage(mobImage, 9, 1, 2, 2), 5.0F, 5.5F, 12, 12);
            case HORSE ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 16, 24, 64, 64), ImageUtils.loadImage(mobImage, 56, 38, 2, 16, 64, 64), 1.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 42, 7, 12, 64, 64), 3.0F, 12.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 20, 7, 5, 64, 64), 3.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 30, 5, 5, 64, 64), 10.0F, 7.0F, 16, 24), ImageUtils.loadImage(mobImage, 19, 17, 1, 3, 64, 64), 3.0F, 4.0F, 16, 24), ImageUtils.loadImage(mobImage, 0, 13, 1, 7, 64, 64), 3.0F, 0.0F, 16, 24);
            case IRONGOLEM ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 12, 128, 128), ImageUtils.loadImage(mobImage, 8, 8, 8, 10, 128, 128), 0.0F, 1.0F, 8, 12), ImageUtils.loadImage(mobImage, 26, 2, 2, 4, 128, 128), 3.0F, 8.0F, 8, 12);
            case LLAMA, LLAMATRADER ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 14, 128, 64), ImageUtils.loadImage(mobImage, 6, 20, 8, 8, 128, 64), 0.0F, 3.0F, 8, 14), ImageUtils.loadImage(mobImage, 9, 9, 4, 4, 128, 64), 2.0F, 5.0F, 8, 14), ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 0.0F, 0.0F, 8, 14), ImageUtils.loadImage(mobImage, 19, 2, 3, 3, 128, 64), 5.0F, 0.0F, 8, 14);
            case MAGMA ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.loadImage(mobImage, 8, 8, 8, 8), ImageUtils.loadImage(mobImage, 32, 18, 8, 1), 0.0F, 3.0F, 8, 8), ImageUtils.loadImage(mobImage, 32, 27, 8, 1), 0.0F, 4.0F, 8, 8);
            case MOOSHROOM -> {
                image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 40, 40), ImageUtils.loadImage(mobImage, 6, 6, 8, 8), 16.0F, 16.0F, 40, 40), ImageUtils.loadImage(mobImage, 23, 1, 1, 3), 15.0F, 15.0F, 40, 40), ImageUtils.loadImage(mobImage, 23, 1, 1, 3), 24.0F, 15.0F, 40, 40);
                if (mobImageSecondary != null) {
                    BufferedImage mushroomImage;
                    if (mobImageSecondary.getWidth() != mobImageSecondary.getHeight()) {
                        mushroomImage = ImageUtils.loadImage(mobImageSecondary, 32, 0, 16, 16, 48, 16);
                    } else {
                        mushroomImage = ImageUtils.loadImage(mobImageSecondary, 0, 0, 16, 16, 16, 16);
                    }

                    float ratio = (float) image.getWidth() / mushroomImage.getWidth();
                    if (ratio < 2.5) {
                        image = ImageUtils.scaleImage(image, 2.5F / ratio);
                    } else if (ratio > 2.5) {
                        mushroomImage = ImageUtils.scaleImage(mushroomImage, ratio / 2.5F);
                    }

                    ImageUtils.addImages(image, mushroomImage, 12.0F, 0.0F, 40, 40);
                }
            }
            case PARROT ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 8, 32, 32), ImageUtils.loadImage(mobImage, 2, 22, 3, 5, 32, 32), 1.0F, 0.0F, 8, 8), ImageUtils.loadImage(mobImage, 10, 4, 4, 1, 32, 32), 2.0F, 4.0F, 8, 8), ImageUtils.loadImage(mobImage, 2, 4, 2, 3, 32, 32), 2.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 11, 8, 1, 2, 32, 32), 4.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 16, 8, 1, 2, 32, 32), 5.0F, 5.0F, 8, 8);
            case PHANTOM ->
                    image = ImageUtils.addImages(ImageUtils.loadImage(mobImage, 5, 5, 7, 3, 64, 64), ImageUtils.loadImage(mobImageSecondary, 5, 5, 7, 3, 64, 64), 0.0F, 0.0F, 7, 3);
            case PUFFERFISH ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 3, 3, 32, 32), ImageUtils.loadImage(mobImage, 3, 30, 3, 2, 32, 32), 0.0F, 1.0F, 3, 3), ImageUtils.loadImage(mobImage, 3, 29, 1, 1, 32, 32), 0.0F, 0.0F, 3, 3), ImageUtils.loadImage(mobImage, 5, 29, 1, 1, 32, 32), 2.0F, 0.0F, 3, 3);
            case PUFFERFISHHALF -> image = ImageUtils.loadImage(mobImage, 17, 27, 5, 5, 32, 32);
            case PUFFERFISHFULL -> image = ImageUtils.loadImage(mobImage, 8, 8, 8, 8, 32, 32);
            case SALMON ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 26, 7, 32, 32), ImageUtils.loadImage(mobImage, 27, 3, 3, 4, 32, 32), 1.0F, 2.5F, 26, 7), ImageUtils.loadImage(mobImage, 11, 8, 8, 5, 32, 32), 4.0F, 2.0F, 26, 7), ImageUtils.loadImage(mobImage, 11, 21, 8, 5, 32, 32), 12.0F, 2.0F, 26, 7), ImageUtils.loadImage(mobImage, 26, 16, 6, 5, 32, 32), 20.0F, 2.0F, 26, 7), ImageUtils.loadImage(mobImage, 0, 0, 2, 2, 32, 32), 10.0F, 0.0F, 26, 7), ImageUtils.loadImage(mobImage, 5, 6, 3, 2, 32, 32), 12.0F, 0.0F, 26, 7);
            case SLIME ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 8, 8), ImageUtils.loadImage(mobImage, 6, 22, 6, 6), 1.0F, 1.0F, 8, 8), ImageUtils.loadImage(mobImage, 34, 6, 2, 2), 5.0F, 2.0F, 8, 8), ImageUtils.loadImage(mobImage, 34, 2, 2, 2), 1.0F, 2.0F, 8, 8), ImageUtils.loadImage(mobImage, 33, 9, 1, 1), 4.0F, 5.0F, 8, 8), ImageUtils.loadImage(mobImage, 8, 8, 8, 8), 0.0F, 0.0F, 8, 8);
            case TROPICALFISHA -> {
                float[] primaryColorsA = {0.9765F, 0.502F, 0.1137F};
                float[] secondaryColorsA = {0.9765F, 1.0F, 0.9961F};
                if (entity instanceof TropicalFishEntity fish) {
                    primaryColorsA = fish.getBaseColorComponents().getColorComponents();
                    secondaryColorsA = fish.getPatternColorComponents().getColorComponents();
                }
                BufferedImage baseA = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 10, 6, 32, 32), ImageUtils.loadImage(mobImage, 8, 6, 6, 3, 32, 32), 0.0F, 3.0F, 10, 6), ImageUtils.loadImage(mobImage, 17, 1, 5, 3, 32, 32), 1.0F, 0.0F, 10, 6), ImageUtils.loadImage(mobImage, 28, 0, 4, 3, 32, 32), 6.0F, 3.0F, 10, 6), primaryColorsA[0], primaryColorsA[1], primaryColorsA[2]);
                BufferedImage patternA = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImageSecondary, 10, 6, 32, 32), ImageUtils.loadImage(mobImageSecondary, 8, 6, 6, 3, 32, 32), 0.0F, 3.0F, 10, 6), ImageUtils.loadImage(mobImageSecondary, 17, 1, 5, 3, 32, 32), 1.0F, 0.0F, 10, 6), ImageUtils.loadImage(mobImageSecondary, 28, 0, 4, 3, 32, 32), 6.0F, 3.0F, 10, 6), secondaryColorsA[0], secondaryColorsA[1], secondaryColorsA[2]);
                image = ImageUtils.addImages(baseA, patternA, 0.0F, 0.0F, 10, 6);
                baseA.flush();
                patternA.flush();
            }
            case TROPICALFISHB -> {
                float[] primaryColorsB = {0.5373F, 0.1961F, 0.7216F};
                float[] secondaryColorsB = {0.9961F, 0.8471F, 0.2392F};
                if (entity instanceof TropicalFishEntity fish) {
                    primaryColorsB = fish.getBaseColorComponents().getColorComponents();
                    secondaryColorsB = fish.getPatternColorComponents().getColorComponents();
                }
                BufferedImage baseB = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 12, 12, 32, 32), ImageUtils.loadImage(mobImage, 0, 26, 6, 6, 32, 32), 6.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 21, 6, 6, 32, 32), 0.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 18, 5, 3, 32, 32), 6.0F, 0.0F, 12, 12), ImageUtils.loadImage(mobImage, 20, 27, 5, 3, 32, 32), 6.0F, 9.0F, 12, 12), primaryColorsB[0], primaryColorsB[1], primaryColorsB[2]);
                BufferedImage patternB = ImageUtils.colorify(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImageSecondary, 12, 12, 32, 32), ImageUtils.loadImage(mobImageSecondary, 0, 26, 6, 6, 32, 32), 6.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImageSecondary, 20, 21, 6, 6, 32, 32), 0.0F, 3.0F, 12, 12), ImageUtils.loadImage(mobImageSecondary, 20, 18, 5, 3, 32, 32), 6.0F, 0.0F, 12, 12), ImageUtils.loadImage(mobImageSecondary, 20, 27, 5, 3, 32, 32), 6.0F, 9.0F, 12, 12), secondaryColorsB[0], secondaryColorsB[1], secondaryColorsB[2]);
                image = ImageUtils.addImages(baseB, patternB, 0.0F, 0.0F, 12, 12);
                baseB.flush();
                patternB.flush();
            }
            case WITHER, WITHERINVULNERABLE ->
                    image = ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.blankImage(mobImage, 24, 10, 64, 64), ImageUtils.loadImage(mobImage, 8, 8, 8, 8, 64, 64), 8.0F, 0.0F, 24, 10), ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 0.0F, 2.0F, 24, 10), ImageUtils.loadImage(mobImage, 38, 6, 6, 6, 64, 64), 18.0F, 2.0F, 24, 10);
            default -> {
            }
        }

        mobImage.flush();
        if (mobImageSecondary != null) {
            mobImageSecondary.flush();
        }

        return image;
    }

    @Override
    public void onTickInGame(DrawContext drawContext, MatrixStack matrixStack, LayoutVariables layoutVariables) {
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            this.layoutVariables = layoutVariables;
            if (this.options.isChanged()) {
                this.timer = 500;
                if (this.options.outlines != this.lastOutlines) {
                    this.lastOutlines = this.options.outlines;
                    this.loadTexturePackIcons();
                }
            }

            this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

            while (this.direction >= 360.0F) {
                this.direction -= 360.0F;
            }

            while (this.direction < 0.0F) {
                this.direction += 360.0F;
            }

            if (this.completedLoading && this.timer > 95) {
                this.calculateMobs();
                this.timer = 0;
            }

            ++this.timer;
            if (this.completedLoading) {
                this.renderMapMobs(drawContext, matrixStack, this.layoutVariables.mapX, this.layoutVariables.mapY);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        }
    }

    private boolean isEntityShown(Entity entity) {
        return entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity) || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity));
    }

    public void calculateMobs() {
        this.contacts.clear();

        Iterable<Entity> entities = VoxelConstants.getClientWorld().getEntities();

        for (Entity entity : entities) {
            try {
                if (this.isEntityShown(entity)) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.getPos().getX();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.getPos().getZ();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.getPos().getY();
                    double hypot = wayX * wayX + wayZ * wayZ + wayY * wayY;
                    hypot /= this.layoutVariables.zoomScaleAdjusted * this.layoutVariables.zoomScaleAdjusted;
                    if (hypot < 961.0) {
                        if (this.hasCustomNPCs) {
                            try {
                                if (this.entityCustomNpcClass.isInstance(entity)) {
                                    Object modelData = this.modelDataField.get(entity);
                                    LivingEntity wrappedEntity = (LivingEntity) this.getEntityMethod.invoke(modelData, entity);
                                    if (wrappedEntity != null) {
                                        entity = wrappedEntity;
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        Contact contact = new Contact(entity, EnumMobs.getMobTypeByEntity(entity));
                        String unscrubbedName = TextUtils.asFormattedString(contact.entity.getDisplayName());
                        contact.setName(unscrubbedName);
                        if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                            contact.yFudge = 1;
                        }

                        contact.updateLocation();
                        boolean enabled = false;
                        if (!contact.vanillaType) {
                            String type = entity.getType().getTranslationKey();
                            CustomMob customMob = CustomMobsManager.getCustomMobByType(type);
                            if (customMob == null || customMob.enabled) {
                                enabled = true;
                            }
                        } else if (contact.type.enabled) {
                            enabled = true;
                        }

                        if (enabled) {
                            if (contact.type == EnumMobs.PLAYER) {
                                this.handleMPplayer(contact);
                            }

                            if (contact.icon == null) {
                                this.tryCustomIcon(contact);
                            }

                            if (contact.icon == null) {
                                this.tryAutoIcon(contact);
                            }

                            if (contact.icon == null) {
                                this.getGenericIcon(contact);
                            }

                            if (contact.type == EnumMobs.HORSE) {
                                contact.setRotationFactor(45);
                            }

                            String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
                            if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof PlayerEntity) || ((PlayerEntity) contact.entity).isPartVisible(PlayerModelPart.CAPE))) {
                                contact.setRotationFactor(contact.rotationFactor + 180);
                            }

                            if (this.options.showHelmetsPlayers && contact.type == EnumMobs.PLAYER || this.options.showHelmetsMobs && contact.type != EnumMobs.PLAYER || contact.type == EnumMobs.SHEEP) {
                                this.getArmor(contact, entity);
                            }

                            this.contacts.add(contact);
                        }
                    }
                }
            } catch (Exception var16) {
                VoxelConstants.getLogger().error(var16.getLocalizedMessage(), var16);
            }
        }

        if (this.newMobs) {
            try {
                this.textureAtlas.stitchNew();
            } catch (StitcherException var14) {
                VoxelConstants.getLogger().warn("Stitcher exception!  Resetting mobs texture atlas.");
                this.loadTexturePackIcons();
            }
        }

        this.newMobs = false;
        this.contacts.sort(Comparator.comparingInt(contact -> contact.y));
    }

    private void tryCustomIcon(Contact contact) {
        String identifier = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String identifierSimple = contact.vanillaType ? contact.type.id : contact.entity.getClass().getSimpleName();
        Sprite icon = this.textureAtlas.getAtlasSprite(identifier + "custom");
        if (icon == this.textureAtlas.getMissingImage()) {
            boolean isHostile = this.isHostile(contact.entity);
            CustomMobsManager.add(contact.entity.getType().getTranslationKey(), isHostile, !isHostile);
            BufferedImage mobSkin = this.getCustomMobImage(identifier, identifierSimple);
            if (mobSkin != null) {
                icon = this.textureAtlas.registerIconForBufferedImage(identifier + "custom", mobSkin);
                this.newMobs = true;
                contact.icon = icon;
                contact.custom = true;
            } else {
                this.textureAtlas.registerFailedIcon(identifier + "custom");
            }
        } else if (icon != this.textureAtlas.getFailedImage()) {
            contact.custom = true;
            contact.icon = icon;
        }
    }

    private BufferedImage getCustomMobImage(String identifier, String identifierSimple) {
        BufferedImage mobSkin = null;

        try {
            int intendedSize = 8;
            String fullPath = ("textures/icons/" + identifier + ".png").toLowerCase();
            InputStream is = null;

            try {
                is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
            } catch (IOException ignored) {
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + ".png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifier + "8.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "8.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                intendedSize = 16;
                fullPath = ("textures/icons/" + identifier + "16.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "16.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                intendedSize = 32;
                fullPath = ("textures/icons/" + identifier + "32.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException ignored) {
                }
            }

            if (is == null) {
                fullPath = ("textures/icons/" + identifierSimple + "32.png").toLowerCase();

                try {
                    is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier(fullPath)).get().getInputStream();
                } catch (IOException ignored) {
                }
            }

            if (is != null) {
                mobSkin = ImageIO.read(is);
                is.close();
                mobSkin = ImageUtils.validateImage(mobSkin);
                float scale = (float) mobSkin.getWidth() / intendedSize;
                mobSkin = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobSkin, 4.0F / scale)), this.options.outlines, 2);
            }
        } catch (Exception var16) {
            mobSkin = null;
        }

        return mobSkin;
    }

    private void tryAutoIcon(Contact contact) {
        EntityRenderer<Entity> render = (EntityRenderer<Entity>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
        Identifier resourceLocation = render.getTexture(contact.entity);
        resourceLocation = this.getRandomizedResourceLocationForEntity(resourceLocation, contact.entity);
        Identifier resourceLocationSecondary = null;
        Identifier resourceLocationTertiary = null;
        Identifier resourceLocationQuaternary = null;
        String color = "";
        if (contact.type.secondaryResourceLocation != null) {
            if (contact.type == EnumMobs.MOOSHROOM) {
                if (!((MooshroomEntity) contact.entity).isBaby()) {
                    resourceLocationSecondary = EnumMobs.MOOSHROOM.secondaryResourceLocation;
                }
            } else if (contact.type != EnumMobs.TROPICALFISHA && contact.type != EnumMobs.TROPICALFISHB) {
                label166:
                {
                    if (contact.type == EnumMobs.HORSE) {
                        Entity var22 = contact.entity;
                        if (var22 instanceof HorseEntity horse) {
                            resourceLocationSecondary = (Identifier) TEXTURES.get(horse.getMarking());
                            ItemStack itemStack = horse.getArmorType();
                            if (this.options.showHelmetsMobs) {
                                Item var30 = itemStack.getItem();
                                if (var30 instanceof HorseArmorItem horseArmorItem) {
                                    resourceLocationTertiary = horseArmorItem.getEntityTexture();
                                    if (horseArmorItem instanceof DyeableHorseArmorItem dyableHorseArmorItem) {
                                        contact.armorColor = dyableHorseArmorItem.getColor(itemStack);
                                    }
                                }
                            }
                            break label166;
                        }
                    }

                    if (contact.type != EnumMobs.VILLAGER && contact.type != EnumMobs.ZOMBIEVILLAGER) {
                        resourceLocationSecondary = contact.type.secondaryResourceLocation;
                    } else {
                        String zombie = contact.type == EnumMobs.ZOMBIEVILLAGER ? "zombie_" : "";
                        VillagerData villagerData = ((VillagerDataContainer) contact.entity).getVillagerData();
                        VillagerType villagerType = villagerData.getType();
                        VillagerProfession villagerProfession = villagerData.getProfession();
                        resourceLocationSecondary = Registries.VILLAGER_TYPE.getId(villagerType);
                        resourceLocationSecondary = new Identifier(resourceLocationSecondary.getNamespace(), "textures/entity/" + zombie + "villager/type/" + resourceLocationSecondary.getPath() + ".png");
                        if (villagerProfession != VillagerProfession.NONE && !((LivingEntity) contact.entity).isBaby()) {
                            resourceLocationTertiary = Registries.VILLAGER_PROFESSION.getId(villagerProfession);
                            resourceLocationTertiary = new Identifier(resourceLocationTertiary.getNamespace(), "textures/entity/" + zombie + "villager/profession/" + resourceLocationTertiary.getPath() + ".png");
                            if (villagerProfession != VillagerProfession.NITWIT) {
                                resourceLocationQuaternary = LEVEL_TO_ID.get(MathHelper.clamp(villagerData.getLevel(), 1, LEVEL_TO_ID.size()));
                                resourceLocationQuaternary = new Identifier(resourceLocationQuaternary.getNamespace(), "textures/entity/" + zombie + "villager/profession_level/" + resourceLocationQuaternary.getPath() + ".png");
                            }
                        }

                        VillagerResourceMetadata.HatType biomeHatType = this.getHatType(resourceLocationSecondary);
                        VillagerResourceMetadata.HatType professionHatType = this.getHatType(resourceLocationTertiary);
                        boolean showBiomeHat = professionHatType == VillagerResourceMetadata.HatType.NONE || professionHatType == VillagerResourceMetadata.HatType.PARTIAL && biomeHatType != VillagerResourceMetadata.HatType.FULL;
                        if (!showBiomeHat) {
                            resourceLocationSecondary = null;
                        }
                    }
                }
            } else {
                TropicalFishEntity fish = (TropicalFishEntity) contact.entity;
                // TODO 1.19.3
                //resourceLocationSecondary = fish.getVarietyId();
                //color = Arrays.toString(fish.getBaseColorComponents()) + " " + Arrays.toString(fish.getPatternColorComponents());
            }

            if (resourceLocationSecondary != null) {
                resourceLocationSecondary = this.getRandomizedResourceLocationForEntity(resourceLocationSecondary, contact.entity);
            }

            if (resourceLocationTertiary != null) {
                resourceLocationTertiary = this.getRandomizedResourceLocationForEntity(resourceLocationTertiary, contact.entity);
            }

            if (resourceLocationQuaternary != null) {
                resourceLocationQuaternary = this.getRandomizedResourceLocationForEntity(resourceLocationQuaternary, contact.entity);
            }
        }

        String entityName = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String resourceLocationString = (resourceLocation != null ? resourceLocation.toString() : "") + (resourceLocationSecondary != null ? resourceLocationSecondary.toString() : "");
        resourceLocationString = resourceLocationString + (resourceLocationTertiary != null ? resourceLocationTertiary.toString() : "") + (resourceLocationQuaternary != null ? resourceLocationQuaternary.toString() : "");
        resourceLocationString = resourceLocationString + (contact.armorColor != -1 ? contact.armorColor : "");
        String name = entityName + color + resourceLocationString;
        Sprite icon = this.textureAtlas.getAtlasSprite(name);
        if (icon == this.textureAtlas.getMissingImage()) {
            Integer checkCount = this.contactsSkinGetTries.get(name);
            if (checkCount == null) {
                checkCount = 0;
            }

            BufferedImage mobImage = null;
            if (contact.type == EnumMobs.HORSE) {
                BufferedImage base = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
                if (resourceLocationSecondary != null && base != null) {
                    BufferedImage pattern = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationSecondary);
                    pattern = ImageUtils.scaleImage(pattern, (float) base.getWidth() / pattern.getWidth(), (float) base.getHeight() / pattern.getHeight());
                    base = ImageUtils.addImages(base, pattern, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    pattern.flush();
                }

                if (resourceLocationTertiary != null && base != null) {
                    BufferedImage armor = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationTertiary);
                    armor = ImageUtils.scaleImage(armor, (float) base.getWidth() / armor.getWidth(), (float) base.getHeight() / armor.getHeight());
                    armor = ImageUtils.colorify(armor, contact.armorColor);
                    base = ImageUtils.addImages(base, armor, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    armor.flush();
                }

                mobImage = this.createImageFromTypeAndImages(contact.type, base, null, contact.entity);
                base.flush();
            } else if (contact.type.expectedWidth > 0.5) {
                mobImage = this.createImageFromTypeAndResourceLocations(contact.type, resourceLocation, resourceLocationSecondary, contact.entity);
            }

            if (mobImage != null) {
                mobImage = this.trimAndOutlineImage(contact, mobImage, false, true);
            } else {
                mobImage = this.createAutoIconImageFromResourceLocations(contact, render, resourceLocation, resourceLocationSecondary, resourceLocationTertiary, resourceLocationQuaternary);
            }

            if (mobImage != null) {
                try {
                    icon = this.textureAtlas.registerIconForBufferedImage(name, mobImage);
                    contact.icon = icon;
                    this.newMobs = true;
                    this.contactsSkinGetTries.remove(name);
                } catch (Exception var16) {
                    checkCount = checkCount + 1;
                    if (checkCount > 4) {
                        this.textureAtlas.registerFailedIcon(name);
                        this.contactsSkinGetTries.remove(name);
                    } else {
                        this.contactsSkinGetTries.put(name, checkCount);
                    }
                }
            } else {
                checkCount = checkCount + 1;
                if (checkCount > 4) {
                    this.textureAtlas.registerFailedIcon(name);
                    this.contactsSkinGetTries.remove(name);
                } else {
                    this.contactsSkinGetTries.put(name, checkCount);
                }
            }
        } else if (icon != this.textureAtlas.getFailedImage()) {
            contact.icon = icon;
        }

    }

    public VillagerResourceMetadata.HatType getHatType(Identifier resourceLocation) {
        VillagerResourceMetadata.HatType hatType = VillagerResourceMetadata.HatType.NONE;
        if (resourceLocation != null) {
            try {
                Optional<Resource> resource = VoxelConstants.getMinecraft().getResourceManager().getResource(resourceLocation);
                if (resource.isPresent()) {
                    VillagerResourceMetadata villagerResourceMetadata = (VillagerResourceMetadata) resource.get().getMetadata();
                    if (villagerResourceMetadata != null) {
                        hatType = villagerResourceMetadata.getHatType();
                    }

                    resource.get().getReader().close();
                }
            } catch (IOException | ClassCastException ignored) {
                hatType = VillagerResourceMetadata.HatType.NONE;
            }
        }

        return hatType;
    }

    private BufferedImage createAutoIconImageFromResourceLocations(Contact contact, EntityRenderer<Entity> entityRenderer, Identifier... resourceLocations) {
        Entity entity = contact.entity;
        EnumMobs type = contact.type;
        UUID entityUUID = contact.uuid;
        if (type != EnumMobs.UNKNOWN && entityIconMap.containsKey(entityUUID)) {
            return entityIconMap.get(entityUUID);
        }

        if (type == EnumMobs.UNKNOWN) {
            VoxelConstants.getLogger().info("Unknown Entity: " + entity.getType());
        }

        BufferedImage headImage = null;
        Model model = null;
        if (entityRenderer instanceof LivingEntityRenderer<?, ?> render) {
            try {
                model = render.getModel();
                ArrayList<Field> submodels = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart.class);
                ArrayList<Field> submodelArrays = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart[].class);
                ModelPart[] headBits = null;
                ArrayList<ModelPartWithResourceLocation> headPartsWithResourceLocationList = new ArrayList<>();
                Properties properties = new Properties();
                String fullName = contact.vanillaType ? "minecraft." + type.id : entity.getClass().getName();
                String simpleName = contact.vanillaType ? type.id : entity.getClass().getSimpleName();
                String fullPath = ("textures/icons/" + fullName + ".properties").toLowerCase();

                ResourceManager resourceManager = VoxelConstants.getMinecraft().getResourceManager();
                Optional<Resource> resource = resourceManager.getResource(new Identifier(fullPath));

                if (resource.isEmpty()) {
                    fullPath = ("textures/icons/" + simpleName + ".properties").toLowerCase();
                    resource = resourceManager.getResource(new Identifier(fullPath));
                }
                if (resource.isPresent())
                    try (InputStream is = resource.get().getInputStream()) {
                        properties.load(is);
                        is.close();
                        String subModelNames = properties.getProperty("models", "").toLowerCase();
                        String[] submodelNamesArray = subModelNames.split(",");
                        List<String> subModelNamesList = Arrays.asList(submodelNamesArray);
                        HashSet<String> subModelNamesSet = new HashSet<>(subModelNamesList);
                        ArrayList<ModelPart> headPartsArrayList = new ArrayList<>();

                        for (Field submodelArray : submodelArrays) {
                            String name = submodelArray.getName().toLowerCase();
                            if (subModelNamesSet.contains(name) || subModelNames.equals("all")) {
                                ModelPart[] submodelArrayValue = (ModelPart[]) submodelArray.get(model);
                                if (submodelArrayValue != null) {
                                    Collections.addAll(headPartsArrayList, submodelArrayValue);
                                }
                            }
                        }

                        for (Field submodel : submodels) {
                            String name = submodel.getName().toLowerCase();
                            if ((subModelNamesSet.contains(name) || subModelNames.equals("all")) && submodel.get(model) != null) {
                                Object modelPartObjekt = submodel.get(model);
                                if (modelPartObjekt instanceof ModelPart modelPart) {
                                    headPartsArrayList.add(modelPart);
                                }
                            }
                        }

                        if (!headPartsArrayList.isEmpty()) {
                            headBits = headPartsArrayList.toArray(new ModelPart[0]);
                        }
                    }

                if (headBits == null) {
                    if (model instanceof PlayerEntityModel) {
                        boolean showHat = true;
                        if (entity instanceof PlayerEntity player) {
                            showHat = player.isPartVisible(PlayerModelPart.HAT);
                        }

                        if (showHat) {
                            headBits = new ModelPart[]{((PlayerEntityModel<?>) model).head, ((PlayerEntityModel<?>) model).hat};
                        } else {
                            headBits = new ModelPart[]{((PlayerEntityModel<?>) model).head};
                        }
                    } else if (type == EnumMobs.STRAY) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((SkeletonEntityModel<?>) model).head, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((SkeletonEntityModel<?>) model).hat, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.strayOverlayModel.head, resourceLocations[1]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.strayOverlayModel.hat, resourceLocations[1]));
                    } else if (type == EnumMobs.DROWNED) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((DrownedEntityModel<?>) model).head, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(((DrownedEntityModel<?>) model).hat, resourceLocations[0]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.drownedOverlayModel.head, resourceLocations[1]));
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(this.drownedOverlayModel.hat, resourceLocations[1]));
                    } else if (model instanceof AxolotlEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, AxolotlEntityModel.class, ModelPart.class, 6)};
                    } else if (model instanceof BatEntityModel batEntityModel) {
                        headBits = new ModelPart[]{batEntityModel.getPart().getChild("head")};
                    } else if (model instanceof BeeEntityModel) {
                        headBits = new ModelPart[]{((ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, BeeEntityModel.class, ModelPart.class, 0)).getChild("body")};
                    } else if (model instanceof BipedEntityModel<? extends LivingEntity> bipedEntityModel) {
                        headBits = new ModelPart[]{bipedEntityModel.head, bipedEntityModel.hat};
                    } else if (model instanceof BlazeEntityModel<? extends Entity> blazeEntityModel) {
                        headBits = new ModelPart[]{blazeEntityModel.getPart().getChild("head")};
                    } else if (model instanceof ChickenEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, ChickenEntityModel.class, ModelPart.class)};
                    } else if (model instanceof CreeperEntityModel<? extends Entity> creeperEntityModel) {
                        headBits = new ModelPart[]{creeperEntityModel.getPart().getChild("head")};
                    } else if (model instanceof DolphinEntityModel<? extends Entity> dolphinEntityModel) {
                        headBits = new ModelPart[]{dolphinEntityModel.getPart().getChild("body").getChild("head")};
                    } else if (model instanceof EndermiteEntityModel<? extends Entity> endermiteEntityModel) {
                        headBits = new ModelPart[]{endermiteEntityModel.getPart().getChild("segment0"), endermiteEntityModel.getPart().getChild("segment1")};
                    } else if (model instanceof GhastEntityModel<? extends Entity> ghastEntityModel) {
                        headBits = new ModelPart[]{ghastEntityModel.getPart()};
                    } else if (model instanceof GuardianEntityModel guardianEntityModel) {
                        headBits = new ModelPart[]{guardianEntityModel.getPart().getChild("head")};
                    } else if (model instanceof HoglinEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, HoglinEntityModel.class, ModelPart.class)};
                    } else if (model instanceof HorseEntityModel<? extends AbstractHorseEntity> horseEntityModel) {
                        headBits = StreamSupport.stream(horseEntityModel.getHeadParts().spliterator(), false).toArray(ModelPart[]::new);
                    } else if (model instanceof IllagerEntityModel<? extends IllagerEntity> illagerEntityModel) {
                        headBits = new ModelPart[]{illagerEntityModel.getPart().getChild("head")};
                    } else if (model instanceof IronGolemEntityModel<? extends IronGolemEntity> ironGolemEntityModel) {
                        headBits = new ModelPart[]{ironGolemEntityModel.getPart().getChild("head")};
                    } else if (model instanceof MagmaCubeEntityModel) {
                        headBits = (ModelPart[]) ReflectionUtils.getPrivateFieldValueByType(model, MagmaCubeEntityModel.class, ModelPart[].class);
                    } else if (model instanceof OcelotEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, OcelotEntityModel.class, ModelPart.class, 6)};
                    } else if (model instanceof PhantomEntityModel<? extends PhantomEntity> phantomEntityModel) {
                        headBits = new ModelPart[]{phantomEntityModel.getPart().getChild("body")};
                    } else if (model instanceof RabbitEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 7), (ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 8), (ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 9), (ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, RabbitEntityModel.class, ModelPart.class, 11)};
                    } else if (model instanceof RavagerEntityModel ravagerEntityModel) {
                        headBits = new ModelPart[]{ravagerEntityModel.getPart().getChild("neck").getChild("head")};
                    } else if (model instanceof ShulkerEntityModel<? extends ShulkerEntity> shulkerEntityModel) {
                        headBits = new ModelPart[]{shulkerEntityModel.getHead()};
                    } else if (model instanceof SilverfishEntityModel<? extends Entity> silverFishEntityModel) {
                        headBits = new ModelPart[]{silverFishEntityModel.getPart().getChild("segment0"), silverFishEntityModel.getPart().getChild("segment1")};
                    } else if (model instanceof SlimeEntityModel<? extends Entity> slimeEntityModel) {
                        headBits = new ModelPart[]{slimeEntityModel.getPart()};
                    } else if (model instanceof SnifferEntityModel<? extends SnifferEntity> snifferModel) {
                        headBits = new ModelPart[]{snifferModel.getPart().getChild("bone").getChild("body").getChild("head")};
                    } else if (model instanceof SnowGolemEntityModel<? extends Entity> snowGolemEntityModel) {
                        headBits = new ModelPart[]{snowGolemEntityModel.getPart().getChild("head")};
                    } else if (model instanceof SpiderEntityModel<? extends Entity> spiderEntityModel) {
                        headBits = new ModelPart[]{spiderEntityModel.getPart().getChild("head"), spiderEntityModel.getPart().getChild("body0")};
                    } else if (model instanceof SquidEntityModel<? extends Entity> squidEntityModel) {
                        headBits = new ModelPart[]{squidEntityModel.getPart().getChild("body")};
                    } else if (model instanceof WardenEntityModel<? extends WardenEntity> wardenEntityModel) {
                        headBits = new ModelPart[]{wardenEntityModel.getPart().getChild("bone").getChild("body").getChild("head")};
                    } else if (model instanceof StriderEntityModel<? extends StriderEntity> striderEntityModel) {
                        headBits = new ModelPart[]{striderEntityModel.getPart().getChild("body")};
                    } else if (model instanceof VillagerResemblingModel<? extends Entity> villagerResemblingModel) {
                        headBits = new ModelPart[]{villagerResemblingModel.getHead()};
                    } else if (model instanceof WolfEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, WolfEntityModel.class, ModelPart.class)};
                    } else if (model instanceof QuadrupedEntityModel) {
                        headBits = new ModelPart[]{(ModelPart) ReflectionUtils.getPrivateFieldValueByType(model, QuadrupedEntityModel.class, ModelPart.class)};
                    } else if (model instanceof SinglePartEntityModel<? extends Entity> singlePartEntityModel) {
                        try {
                            headBits = new ModelPart[]{singlePartEntityModel.getPart().getChild("head")};
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (headBits == null) {
                    ArrayList<ModelPart> headPartsArrayList = new ArrayList<>();
                    ArrayList<ModelPart> purge = new ArrayList<>();

                    for (Field submodelArray : submodelArrays) {
                        String name = submodelArray.getName().toLowerCase();
                        if (name.contains("head") | name.contains("eye") | name.contains("mouth") | name.contains("teeth") | name.contains("tooth") | name.contains("tusk") | name.contains("jaw") | name.contains("mand") | name.contains("nose") | name.contains("beak") | name.contains("snout") | name.contains("muzzle") | (!name.contains("rear") && name.contains("ear")) | name.contains("trunk") | name.contains("mane") | name.contains("horn") | name.contains("antler")) {
                            ModelPart[] submodelArrayValue = (ModelPart[]) submodelArray.get(model);
                            if (submodelArrayValue != null) {
                                headPartsArrayList.add(submodelArrayValue[0]);
                            }
                        }
                    }

                    for (Field submodel : submodels) {
                        String name = submodel.getName().toLowerCase();
                        String nameS = submodel.getName();
                        if (name.contains("head") | name.contains("eye") | name.contains("mouth") | name.contains("teeth") | name.contains("tooth") | name.contains("tusk") | name.contains("jaw") | name.contains("mand") | name.contains("nose") | name.contains("beak") | name.contains("snout") | name.contains("muzzle") | (!name.contains("rear") && name.contains("ear")) | name.contains("trunk") | name.contains("mane") | name.contains("horn") | name.contains("antler") | nameS.equals("REar") | nameS.equals("Trout") && !nameS.equals("LeftSmallEar") & !nameS.equals("RightSmallEar") & !nameS.equals("BHead") & !nameS.equals("BSnout") & !nameS.equals("BMouth") & !nameS.equals("BMouthOpen") & !nameS.equals("BLEar") & !nameS.equals("BREar") & !nameS.equals("CHead") & !nameS.equals("CSnout") & !nameS.equals("CMouth") & !nameS.equals("CMouthOpen") & !nameS.equals("CLEar") & !nameS.equals("CREar") && submodel.get(model) != null) {
                            headPartsArrayList.add((ModelPart) submodel.get(model));
                        }
                    }

                    if (headPartsArrayList.isEmpty()) {
                        int pos = model instanceof SinglePartEntityModel ? 1 : 0;
                        if (submodels.size() > pos) {
                            if (submodels.get(pos).get(model) != null) {
                                headPartsArrayList.add((ModelPart) submodels.get(pos).get(model));
                            }
                        } else if (!submodelArrays.isEmpty() && submodelArrays.get(0).get(model) != null) {
                            ModelPart[] submodelArrayValue = (ModelPart[]) submodelArrays.get(0).get(model);
                            if (submodelArrayValue.length > 0) {
                                headPartsArrayList.add(submodelArrayValue[0]);
                            }
                        }
                    }

                    for (ModelPart bit : headPartsArrayList) {
                        try {
                            Object childrenObj = ReflectionUtils.getPrivateFieldValueByType(bit, ModelPart.class, ObjectList.class, 1);
                            if (childrenObj != null) {
                                List<ModelPart> children = (List<ModelPart>) childrenObj;
                                purge.addAll(children);
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    headPartsArrayList.removeAll(purge);
                    headBits = headPartsArrayList.toArray(new ModelPart[0]);
                }

                if ((headBits.length > 0 || !headPartsWithResourceLocationList.isEmpty()) && resourceLocations[0] != null) {
                    String scaleString = properties.getProperty("scale", "1");
                    float scale = Float.parseFloat(scaleString);
                    Direction facing = Direction.NORTH;
                    String facingString = properties.getProperty("facing", "front");
                    if (facingString.equals("top")) {
                        facing = Direction.UP;
                    } else if (facingString.equals("side")) {
                        facing = Direction.EAST;
                    }

                    Identifier resourceLocation = this.combineResourceLocations(resourceLocations);
                    for (ModelPart headBit : headBits) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(headBit, resourceLocation));
                    }

                    ModelPartWithResourceLocation[] headBitsWithLocations = headPartsWithResourceLocationList.toArray(new ModelPartWithResourceLocation[0]);
                    boolean success = this.drawModel(scale, 1000, (LivingEntity) entity, facing, model, headBitsWithLocations);
                    ImageUtils.saveImage(type.id, OpenGL.Utils.fboTextureId, 0, 512, 512);
                    if (success) {
                        headImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
                    }
                }
            } catch (Exception exception) {
                VoxelConstants.getLogger().error(exception);
            }
        }

        if (headImage != null) {
            headImage = this.trimAndOutlineImage(contact, headImage, true, model instanceof BipedEntityModel);
        }

        if (contact.type == EnumMobs.CAMEL || contact.type == EnumMobs.SNIFFER) {
            headImage = resizeBufferedImage(headImage, headImage.getHeight() / 2, headImage.getHeight() / 2);
        }

        entityIconMap.put(entityUUID, headImage);
        return headImage;
    }

    public static BufferedImage resizeBufferedImage(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return dimg;
    }

    private Identifier combineResourceLocations(Identifier... resourceLocations) {
        Identifier resourceLocation = resourceLocations[0];
        if (resourceLocations.length > 1) {
            boolean hasAdditional = false;

            try {
                BufferedImage base = null;

                for (int t = 1; t < resourceLocations.length; ++t) {
                    if (resourceLocations[t] != null) {
                        if (!hasAdditional) {
                            base = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
                        }

                        hasAdditional = true;
                        BufferedImage overlay = ImageUtils.createBufferedImageFromResourceLocation(resourceLocations[t]);
                        float xScale = ((float) base.getWidth() / overlay.getWidth());
                        float yScale = ((float) base.getHeight() / overlay.getHeight());
                        if (xScale != 1.0F || yScale != 1.0F) {
                            overlay = ImageUtils.scaleImage(overlay, xScale, yScale);
                        }

                        ImageUtils.addImages(base, overlay, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                        overlay.flush();
                    }
                }

                if (hasAdditional) {
                    NativeImage nativeImage = OpenGL.Utils.nativeImageFromBufferedImage(base);
                    base.flush();
                    this.nativeBackedTexture.close();
                    this.nativeBackedTexture = new NativeImageBackedTexture(nativeImage);
                    OpenGL.Utils.register(this.nativeBackedTextureLocation, this.nativeBackedTexture);
                    resourceLocation = this.nativeBackedTextureLocation;
                }
            } catch (Exception var9) {
                VoxelConstants.getLogger().warn(var9);
            }
        }

        return resourceLocation;
    }

    private boolean drawModel(float scale, int captureDepth, LivingEntity livingEntity, Direction facing, Model model, ModelPartWithResourceLocation[] headBits) {
        boolean failed = false;
        float size = 64.0F * scale;
        RenderSystem.bindTexture(OpenGL.Utils.fboTextureId);
        int width = GlStateManager._getTexLevelParameter(GlConst.GL_TEXTURE_2D, 0, OpenGL.GL_TRANSFORM_BIT);
        int height = GlStateManager._getTexLevelParameter(GlConst.GL_TEXTURE_2D, 0, OpenGL.GL_TEXTURE_HEIGHT);
        RenderSystem.bindTexture(0);
        RenderSystem.viewport(0, 0, width, height);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f matrix4f = new Matrix4f().ortho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_DISTANCE);
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.loadIdentity();
        matrixStack.translate(0.0, 0.0, -3000.0 + captureDepth);
        RenderSystem.applyModelViewMatrix();
        OpenGL.Utils.bindFramebuffer();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 0.0F);
        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(GlConst.GL_COLOR_BUFFER_BIT | GlConst.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);
        matrixStack.push();
        matrixStack.translate(width / 2f, height / 2f, 0.0);
        matrixStack.scale(size, size, size);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        if (facing == Direction.EAST) {
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
        } else if (facing == Direction.UP) {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        }

        RenderSystem.applyModelViewMatrix();
        Vector4f fullbright2 = new Vector4f(fullbright.x, fullbright.y, fullbright.z, 0);
        fullbright2.mul(matrixStack.peek().getPositionMatrix());
        Vector3f fullbright3 = new Vector3f(fullbright2.x, fullbright2.y, fullbright2.z);
        RenderSystem.setShaderLights(fullbright3, fullbright3);

        try {
            MatrixStack newMatrixStack = new MatrixStack();
            VertexConsumerProvider.Immediate immediate = VoxelConstants.getMinecraft().getBufferBuilders().getEntityVertexConsumers();
            float offsetByY = model instanceof EndermanEntityModel ? 8.0F : (!(model instanceof BipedEntityModel) && !(model instanceof SkullEntityModel) ? 0.0F : 4.0F);
            float maxY = 0.0F;
            float minY = 0.0F;

            for (ModelPartWithResourceLocation headBit : headBits) {
                if (headBit.modelPart.pivotY < minY) {
                    minY = headBit.modelPart.pivotY;
                }

                if (headBit.modelPart.pivotY > maxY) {
                    maxY = headBit.modelPart.pivotY;
                }
            }

            if (minY < -25.0F) {
                offsetByY = -25.0F - minY;
            } else if (maxY > 25.0F) {
                offsetByY = 25.0F - maxY;
            }

            for (ModelPartWithResourceLocation headBit : headBits) {
                VertexConsumer vertexConsumer = immediate.getBuffer(model.getLayer(headBit.resourceLocation));
                if (model instanceof EntityModel entityModel) {
                    entityModel.setAngles(livingEntity, 0.0F, 0.0F, 163.0F, 360.0F, 0.0F);
                }

                float y = headBit.modelPart.pivotY;
                headBit.modelPart.pivotY += offsetByY;
                headBit.modelPart.render(newMatrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV);
                headBit.modelPart.pivotY = y;
                immediate.draw();
            }
        } catch (Exception var25) {
            VoxelConstants.getLogger().warn("Error attempting to render head bits for " + livingEntity.getClass().getSimpleName(), var25);
            failed = true;
        }

        matrixStack.pop();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        OpenGL.Utils.unbindFramebuffer();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix, VertexSorter.BY_DISTANCE);
        RenderSystem.viewport(0, 0, VoxelConstants.getMinecraft().getWindow().getFramebufferWidth(), VoxelConstants.getMinecraft().getWindow().getFramebufferHeight());
        return !failed;
    }

    private void getGenericIcon(Contact contact) {
        contact.type = this.getUnknownMobNeutrality(contact.entity);
        String name = "minecraft." + contact.type.id + contact.type.resourceLocation.toString();
        contact.icon = this.textureAtlas.getAtlasSprite(name);
    }

    private Identifier getRandomizedResourceLocationForEntity(Identifier resourceLocation, Entity entity) {
        try {
            if (this.randomobsOptifine) {
                Object randomEntitiesProperties = this.mapProperties.get(resourceLocation.getPath());
                if (randomEntitiesProperties != null) {
                    this.setEntityMethod.invoke(this.randomEntityClass.cast(this.randomEntity), entity);
                    resourceLocation = (Identifier) this.getEntityTextureMethod.invoke(this.randomEntitiesPropertiesClass.cast(randomEntitiesProperties), resourceLocation, this.randomEntityClass.cast(this.randomEntity));
                }
            }
        } catch (Exception ignored) {
        }

        return resourceLocation;
    }

    private BufferedImage trimAndOutlineImage(Contact contact, BufferedImage image, boolean auto, boolean centered) {
        if (auto) {
            image = centered ? ImageUtils.trimCentered(image) : ImageUtils.trim(image);
            double acceptableMax = 64.0;
            if (ImageUtils.percentageOfEdgePixelsThatAreSolid(image) < 30.0F) {
                acceptableMax = 128.0;
            }

            int maxDimension = Math.max(image.getWidth(), image.getHeight());
            float scale = (float) Math.ceil(maxDimension / acceptableMax);
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 1.0F / scale)), this.options.outlines, 2);
        } else {
            float scale = image.getWidth() / contact.type.expectedWidth;
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 4.0F / scale)), this.options.outlines, 2);
        }
    }

    private void handleMPplayer(Contact contact) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) contact.entity;
        GameProfile gameProfile = player.getGameProfile();
        UUID uuid = gameProfile.getId();
        contact.setUUID(uuid);
        String playerName = this.scrubCodes(gameProfile.getName());
        Sprite icon = this.textureAtlas.getAtlasSprite(playerName);
        Integer checkCount;
        if (icon == this.textureAtlas.getMissingImage()) {
            checkCount = this.mpContactsSkinGetTries.get(playerName);
            if (checkCount == null) {
                checkCount = 0;
            }

            if (checkCount < 5) {
                PlayerSkinTexture imageData;

                try {
                    Identifier skinIdentifier = VoxelConstants.getMinecraft().getSkinProvider().getSkinTextures(player.getGameProfile()).texture();
                    if (skinIdentifier == DefaultSkinHelper.getSkinTextures(player.getUuid()).texture()) {
                        throw new Exception("failed to get skin: skin is default");
                    }

                    //FIXME 1.20.2 AbstractClientPlayerEntity.loadSkin(skinIdentifier, player.getName().getString());
                    imageData = (PlayerSkinTexture) VoxelConstants.getMinecraft().getTextureManager().getTexture(skinIdentifier);
                    if (imageData == null) {
                        throw new Exception("failed to get skin: image data was null");
                    }

                    EntityRenderer<Entity> render = (EntityRenderer<Entity>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
                    BufferedImage skinImage = this.createAutoIconImageFromResourceLocations(contact, render, skinIdentifier, null);
                    icon = this.textureAtlas.registerIconForBufferedImage(playerName, skinImage);
                    this.newMobs = true;
                    this.mpContactsSkinGetTries.remove(playerName);
                } catch (Exception var11) {
                    icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.PLAYER.id + EnumMobs.PLAYER.resourceLocation.toString());
                    checkCount = checkCount + 1;
                    this.mpContactsSkinGetTries.put(playerName, checkCount);
                }

                contact.icon = icon;
            }
        } else {
            contact.icon = icon;
        }

    }

    private void getArmor(Contact contact, Entity entity) {
        Sprite icon = null;
        ItemStack stack = ((LivingEntity) entity).getEquippedStack(EquipmentSlot.HEAD);
        Item helmet = null;
        if (stack != null && stack.getCount() > 0) {
            helmet = stack.getItem();
        }

        if (contact.type == EnumMobs.SHEEP) {
            SheepEntity sheepEntity = (SheepEntity) contact.entity;
            if (!sheepEntity.isSheared()) {
                icon = this.textureAtlas.getAtlasSprite("sheepfur");
                float[] sheepColors = SheepEntity.getRgbColor(sheepEntity.getColor());
                contact.setArmorColor((int) (sheepColors[0] * 255.0F) << 16 | (int) (sheepColors[1] * 255.0F) << 8 | (int) (sheepColors[2] * 255.0F));
            }
        } else if (helmet != null) {
            if (helmet == Items.SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.SKELETON.id + EnumMobs.SKELETON.resourceLocation.toString() + "head");
            } else if (helmet == Items.WITHER_SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.SKELETONWITHER.id + EnumMobs.SKELETONWITHER.resourceLocation.toString() + "head");
            } else if (helmet == Items.ZOMBIE_HEAD) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.ZOMBIE.id + EnumMobs.ZOMBIE.resourceLocation.toString() + "head");
            } else if (helmet == Items.CREEPER_HEAD) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.CREEPER.id + EnumMobs.CREEPER.resourceLocation.toString() + "head");
            } else if (helmet == Items.DRAGON_HEAD) {
                icon = this.textureAtlas.getAtlasSprite("minecraft." + EnumMobs.ENDERDRAGON.id + EnumMobs.ENDERDRAGON.resourceLocation.toString() + "head");
            } else if (helmet == Items.PLAYER_HEAD) {
                GameProfile gameProfile = null;
                if (stack.hasNbt()) {
                    NbtCompound nbttagcompound = stack.getNbt();
                    if (nbttagcompound.contains("SkullOwner", 10)) {
                        gameProfile = NbtHelper.toGameProfile(nbttagcompound.getCompound("SkullOwner"));
                    } else if (nbttagcompound.contains("SkullOwner", 8)) {
                        String name = nbttagcompound.getString("SkullOwner");
                        if (name != null && !name.isEmpty()) {
                            gameProfile = new GameProfile(null, name);
                            nbttagcompound.remove("SkullOwner");
                            //FIXME 1.20.2 SkullBlockEntity.loadProperties(gameProfile, gameProfilex -> nbttagcompound.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), gameProfilex)));
                        }
                    }
                }

                Identifier resourceLocation = DefaultSkinHelper.getTexture();
                if (gameProfile != null) {
                    resourceLocation = VoxelConstants.getMinecraft().getSkinProvider().getSkinTextures(gameProfile).texture();
                    //FIXME 1.20.2
                    /*if (map.containsKey(Type.SKIN)) {
                        resourceLocation = VoxelConstants.getMinecraft().getSkinProvider().loadSkin(map.get(Type.SKIN), Type.SKIN);
                    }*/
                }

                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.PLAYER.id + resourceLocation.toString() + "head");
                if (icon == this.textureAtlas.getMissingImage()) {
                    ModelPart inner = (ModelPart) ReflectionUtils.getPrivateFieldValueByType(this.playerSkullModel, SkullEntityModel.class, ModelPart.class, 0);
                    ModelPart outer = (ModelPart) ReflectionUtils.getPrivateFieldValueByType(this.playerSkullModel, SkullEntityModel.class, ModelPart.class, 1);
                    ModelPartWithResourceLocation[] headBits = {new ModelPartWithResourceLocation(inner, resourceLocation), new ModelPartWithResourceLocation(outer, resourceLocation)};
                    boolean success = this.drawModel(1.1875F, 1000, (LivingEntity) contact.entity, Direction.NORTH, this.playerSkullModel, headBits);
                    if (success) {
                        BufferedImage headImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
                        headImage = this.trimAndOutlineImage(new Contact(VoxelConstants.getPlayer(), EnumMobs.PLAYER), headImage, true, true);
                        icon = this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.PLAYER.id + resourceLocation + "head", headImage);
                        this.newMobs = true;
                    }
                }
            } else if (helmet instanceof ArmorItem helmetArmor) {
                int armorType = this.getArmorType(helmetArmor);
                if (armorType != UNKNOWN) {
                    icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[armorType]);
                } else {
                    boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.PIGLINZOMBIE;
                    icon = this.textureAtlas.getAtlasSprite("armor " + helmet.getTranslationKey() + (isPiglin ? "_piglin" : ""));
                    if (icon == this.textureAtlas.getMissingImage()) {
                        icon = this.createUnknownArmorIcons(contact, stack, helmet);
                    } else if (icon == this.textureAtlas.getFailedImage()) {
                        icon = null;
                    }
                }

                if (helmetArmor instanceof DyeableArmorItem dyeableHelmetArmor) {
                    contact.setArmorColor(dyeableHelmetArmor.getColor(stack));
                }
            } else if (helmet instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BlockState blockState = block.getDefaultState();
                int stateID = Block.getRawIdFromState(blockState);
                icon = this.textureAtlas.getAtlasSprite("blockArmor " + stateID);
                if (icon == this.textureAtlas.getMissingImage()) {
                    BufferedImage blockImage = VoxelConstants.getVoxelMapInstance().getColorManager().getBlockImage(blockState, stack, entity.getWorld(), 4.9473686F, -8.0F);
                    if (blockImage != null) {
                        int width = blockImage.getWidth();
                        int height = blockImage.getHeight();
                        ImageUtils.eraseArea(blockImage, width / 2 - 15, height / 2 - 15, 30, 30, width, height);
                        BufferedImage blockImageFront = VoxelConstants.getVoxelMapInstance().getColorManager().getBlockImage(blockState, stack, entity.getWorld(), 4.9473686F, 7.25F);
                        blockImageFront = blockImageFront.getSubimage(width / 2 - 15, height / 2 - 15, 30, 30);
                        ImageUtils.addImages(blockImage, blockImageFront, (width / 2f - 15), (height / 2f - 15), width, height);
                        blockImageFront.flush();
                        blockImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(blockImage)), this.options.outlines, true, 37.6F, 37.6F, 2);
                        icon = this.textureAtlas.registerIconForBufferedImage("blockArmor " + stateID, blockImage);
                        this.newMobs = true;
                    }
                }
            }
        }

        contact.armorIcon = icon;
    }

    private Sprite createUnknownArmorIcons(Contact contact, ItemStack stack, Item helmet) {
        Sprite icon = null;
        boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.PIGLINZOMBIE;

        Identifier resourceLocation = null;

        try {
            String materialName = ((ArmorItem) helmet).getMaterial().getName();
            String domain = "minecraft";
            int sep = materialName.indexOf(58);
            if (sep != -1) {
                domain = materialName.substring(0, sep);
                materialName = materialName.substring(sep + 1);
            }

            String suffix;
            suffix = "";
            String resourcePath = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, materialName, 1, suffix);

            resourceLocation = new Identifier(resourcePath);
        } catch (RuntimeException ignored) {
        }

        BipedEntityModel<LivingEntity> modelBiped;

        float intendedWidth = 9.0F;
        float intendedHeight = 9.0F;

        if (isPiglin) {
            modelBiped = this.piglinArmorModel;
            intendedWidth = 11.5F;
        } else {
            modelBiped = this.bipedArmorModel;
        }

        if (modelBiped != null && resourceLocation != null) {
            ModelPartWithResourceLocation[] headBitsWithResourceLocation = {new ModelPartWithResourceLocation(modelBiped.head, resourceLocation), new ModelPartWithResourceLocation(modelBiped.hat, resourceLocation)};
            this.drawModel(1.0F, 2, (LivingEntity) contact.entity, Direction.NORTH, modelBiped, headBitsWithResourceLocation);
            BufferedImage armorImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
            armorImage = armorImage.getSubimage(200, 200, 112, 112);
            armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(armorImage)), this.options.outlines, true, intendedWidth * 4.0F, intendedHeight * 4.0F, 2);
            icon = this.textureAtlas.registerIconForBufferedImage("armor " + helmet.getTranslationKey() + (isPiglin ? "_piglin" : ""), armorImage);
            this.newMobs = true;
        }

        if (icon == null && resourceLocation != null) {
            BufferedImage armorTexture = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
            if (armorTexture != null) {
                if (!isPiglin) {
                    armorTexture = ImageUtils.addImages(ImageUtils.loadImage(armorTexture, 8, 8, 8, 8), ImageUtils.loadImage(armorTexture, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                    float scale = armorTexture.getWidth() / 8.0F;
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 37.6F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation, armorImage);
                } else {
                    armorTexture = ImageUtils.addImages(ImageUtils.loadImage(armorTexture, 8, 8, 8, 8), ImageUtils.loadImage(armorTexture, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                    float scale = armorTexture.getWidth() / 8.0F;
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 47.0F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation + "_piglin", armorImage);
                }

                this.newMobs = true;
            }
        }

        if (icon == null) {
            VoxelConstants.getLogger().warn("can't get texture for custom armor type: " + helmet.getClass());
            this.textureAtlas.registerFailedIcon("armor " + helmet.getTranslationKey() + helmet.getClass().getName());
        }

        return icon;
    }

    private String scrubCodes(String string) {
        return string.replaceAll("(\\xA7.)", "");
    }

    private EnumMobs getUnknownMobNeutrality(Entity entity) {
        if (this.isHostile(entity)) {
            return EnumMobs.GENERICHOSTILE;
        } else {
            if (entity instanceof TameableEntity tameableEntity) {
                if (tameableEntity.isTamed() && (VoxelConstants.getMinecraft().isIntegratedServerRunning() || tameableEntity.getOwner().equals(VoxelConstants.getPlayer()))) {
                    return EnumMobs.GENERICTAME;
                }
            }

            return EnumMobs.GENERICNEUTRAL;
        }
    }

    private int getArmorType(ArmorItem helmet) {
        return helmet.getTranslationKey().equals("item.minecraft.leather_helmet") ? 0 : UNKNOWN;
    }

    public void renderMapMobs(DrawContext drawContext, MatrixStack matrixStack, int x, int y) {
        double max = this.layoutVariables.zoomScaleAdjusted * 32.0;
        double lastX = GameVariableAccessShim.xCoordDouble();
        double lastZ = GameVariableAccessShim.zCoordDouble();
        int lastY = GameVariableAccessShim.yCoord();

        for (Contact contact : this.contacts) {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, this.textureAtlas.getGlId());
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            int contactY = contact.y;
            double wayX = lastX - contactX;
            double wayZ = lastZ - contactZ;
            int wayY = lastY - contactY;
            double adjustedDiff = max - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / max, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) / this.layoutVariables.zoomScaleAdjusted;
            if (wayY < 0) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, contact.brightness);
            } else {
                RenderSystem.setShaderColor(contact.brightness, contact.brightness, contact.brightness, 1.0f);
            }

            if (this.minimapOptions.rotates) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            boolean inRange;
            if (!this.minimapOptions.squareMap) {
                inRange = contact.distance < 31.0;
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double dispX = contact.distance * Math.cos(radLocate);
                double dispY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(dispX) <= 28.5 && Math.abs(dispY) <= 28.5;
            }

            if (inRange) {
                try {
                    matrixStack.push();
                    if (this.options.filtering) {
                        matrixStack.translate(x, y, 0.0);
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-contact.angle));
                        matrixStack.translate(0.0, -contact.distance, 0.0);
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(contact.angle + contact.rotationFactor));
                        matrixStack.translate((-x), (-y), 0.0);
                    } else {
                        wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                        matrixStack.translate((double) Math.round(-wayX * this.layoutVariables.scScale) / this.layoutVariables.scScale, (double) Math.round(-wayZ * this.layoutVariables.scScale) / this.layoutVariables.scScale, 0.0);
                    }

                    RenderSystem.applyModelViewMatrix();
                    float yOffset = 0.0F;
                    if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                        yOffset = -4.0F;
                    }

                    if (Stream.of(EnumMobs.GHAST, EnumMobs.GHASTATTACKING, EnumMobs.WITHER, EnumMobs.WITHERINVULNERABLE, EnumMobs.VEX, EnumMobs.VEXCHARGING, EnumMobs.PUFFERFISH, EnumMobs.PUFFERFISHHALF, EnumMobs.PUFFERFISHFULL).anyMatch(enumMobs -> contact.type == enumMobs)) {
                        if (contact.type != EnumMobs.GHAST && contact.type != EnumMobs.GHASTATTACKING) {
                            if (contact.type != EnumMobs.WITHER && contact.type != EnumMobs.WITHERINVULNERABLE) {
                                if (contact.type != EnumMobs.VEX && contact.type != EnumMobs.VEXCHARGING) {
                                    int size = ((PufferfishEntity) contact.entity).getPuffState();
                                    switch (size) {
                                        case 0 -> contact.type = EnumMobs.PUFFERFISH;
                                        case 1 -> contact.type = EnumMobs.PUFFERFISHHALF;
                                        case 2 -> contact.type = EnumMobs.PUFFERFISHFULL;
                                    }
                                } else {
                                    EntityRenderer<Entity> render = (EntityRenderer<Entity>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
                                    String path = render.getTexture(contact.entity).getPath();
                                    contact.type = path.endsWith("vex_charging.png") ? EnumMobs.VEXCHARGING : EnumMobs.VEX;
                                }
                            } else {
                                EntityRenderer<Entity> render = (EntityRenderer<Entity>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
                                String path = render.getTexture(contact.entity).getPath();
                                contact.type = path.endsWith("wither_invulnerable.png") ? EnumMobs.WITHERINVULNERABLE : EnumMobs.WITHER;
                            }
                        } else {
                            EntityRenderer<Entity> render = (EntityRenderer<Entity>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
                            String path = render.getTexture(contact.entity).getPath();
                            contact.type = path.endsWith("ghast_fire.png") ? EnumMobs.GHASTATTACKING : EnumMobs.GHAST;
                        }

                        this.tryAutoIcon(contact);
                        this.tryCustomIcon(contact);
                        if (this.newMobs) {
                            try {
                                this.textureAtlas.stitchNew();
                            } catch (StitcherException var45) {
                                VoxelConstants.getLogger().warn("Stitcher exception in render method!  Resetting mobs texture atlas.");
                                this.loadTexturePackIcons();
                            }

                            RenderSystem.setShaderTexture(0, this.textureAtlas.getGlId());
                        }

                        this.newMobs = false;
                    }

                    this.applyFilteringParameters();
                    OpenGL.Utils.drawPre();
                    OpenGL.Utils.setMap(contact.icon, x, y + yOffset, ((int) (contact.icon.getIconWidth() / 4.0F)));
                    OpenGL.Utils.drawPost();
                    if ((this.options.showHelmetsPlayers && contact.type == EnumMobs.PLAYER || this.options.showHelmetsMobs && contact.type != EnumMobs.PLAYER || contact.type == EnumMobs.SHEEP) && contact.armorIcon != null) {
                        Sprite icon = contact.armorIcon;
                        float armorOffset = 0.0F;
                        if (contact.type == EnumMobs.ZOMBIEVILLAGER) {
                            armorOffset = -0.5F;
                        }

                        float armorScale = 1.0F;
                        float red = 1.0F;
                        float green = 1.0F;
                        float blue = 1.0F;
                        if (contact.armorColor != -1) {
                            red = (contact.armorColor >> 16 & 0xFF) / 255.0F;
                            green = (contact.armorColor >> 8 & 0xFF) / 255.0F;
                            blue = (contact.armorColor & 0xFF) / 255.0F;
                            if (contact.type == EnumMobs.SHEEP) {
                                SheepEntity sheepEntity = (SheepEntity) contact.entity;
                                if (sheepEntity.hasCustomName() && "jeb_".equals(sheepEntity.getName().getString())) {
                                    int semiRandom = sheepEntity.age / 25 + sheepEntity.getId();
                                    int numDyeColors = DyeColor.values().length;
                                    int colorID1 = semiRandom % numDyeColors;
                                    int colorID2 = (semiRandom + 1) % numDyeColors;
                                    float lerpVal = ((sheepEntity.age % 25) + VoxelConstants.getMinecraft().getTickDelta()) / 25.0F;
                                    float[] sheepColors1 = SheepEntity.getRgbColor(DyeColor.byId(colorID1));
                                    float[] sheepColors2 = SheepEntity.getRgbColor(DyeColor.byId(colorID2));
                                    red = sheepColors1[0] * (1.0F - lerpVal) + sheepColors2[0] * lerpVal;
                                    green = sheepColors1[1] * (1.0F - lerpVal) + sheepColors2[1] * lerpVal;
                                    blue = sheepColors1[2] * (1.0F - lerpVal) + sheepColors2[2] * lerpVal;
                                }

                                armorScale = 1.04F;
                            }

                            if (wayY < 0) {
                                RenderSystem.setShaderColor(red, green, blue, contact.brightness);
                            } else {
                                RenderSystem.setShaderColor(red * contact.brightness, green * contact.brightness, blue * contact.brightness, 1.0f);
                            }
                        }

                        this.applyFilteringParameters();
                        OpenGL.Utils.drawPre();
                        OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, ((int) (icon.getIconWidth() / 4.0F * armorScale)));
                        OpenGL.Utils.drawPost();
                        if (icon == this.clothIcon) {
                            if (wayY < 0) {
                                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, contact.brightness);
                            } else {
                                RenderSystem.setShaderColor(contact.brightness, contact.brightness, contact.brightness, 1.0f);
                            }

                            icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[2]);
                            this.applyFilteringParameters();
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale);
                            OpenGL.Utils.drawPost();
                            if (wayY < 0) {
                                RenderSystem.setShaderColor(red, green, blue, contact.brightness);
                            } else {
                                RenderSystem.setShaderColor(red * contact.brightness, green * contact.brightness, blue * contact.brightness, 1.0f);
                            }

                            icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[1]);
                            this.applyFilteringParameters();
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            OpenGL.Utils.drawPost();
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0f);
                            icon = this.textureAtlas.getAtlasSprite("armor " + this.armorNames[3]);
                            this.applyFilteringParameters();
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            OpenGL.Utils.drawPost();
                        }
                    }

                    if (contact.name != null && (this.options.showPlayerNames && contact.type == EnumMobs.PLAYER || this.options.showMobNames && contact.type != EnumMobs.PLAYER)) {

                        float scaleFactor = this.layoutVariables.scScale / this.options.fontScale;
                        matrixStack.scale(1.0F / scaleFactor, 1.0F / scaleFactor, 1.0F);
                        RenderSystem.applyModelViewMatrix();

                        String name = contact.entity.getDisplayName().getString();
                        int m = VoxelConstants.getMinecraft().textRenderer.getWidth(name) / 2;

                        MatrixStack textMatrixStack = drawContext.getMatrices();
                        textMatrixStack.push();
                        textMatrixStack.loadIdentity();
                        textMatrixStack.translate(0, 0, 900);
                        drawContext.drawText(VoxelConstants.getMinecraft().textRenderer, name, (int) (x * scaleFactor - m), (int) ((y + 3) * scaleFactor), 0xffffffff, false);
                        textMatrixStack.pop();
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + contact.type, e);
                } finally {
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        }
    }

    private void applyFilteringParameters() {
        if (this.options.filtering) {
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_LINEAR);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_LINEAR);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_S, GlConst.GL_CLAMP_TO_EDGE);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_WRAP_T, GlConst.GL_CLAMP_TO_EDGE);
        } else {
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
            RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);
        }

    }

    private boolean isHostile(Entity entity) {
        if (entity instanceof ZombifiedPiglinEntity zombifiedPiglinEntity) {
            return zombifiedPiglinEntity.isAngryAt(VoxelConstants.getPlayer());
        } else if (entity instanceof Monster) {
            return true;
        } else if (entity instanceof BeeEntity beeEntity) {
            return beeEntity.hasAngerTime();
        } else {
            if (entity instanceof PolarBearEntity polarBearEntity) {

                for (PolarBearEntity object : polarBearEntity.getWorld().getNonSpectatingEntities(PolarBearEntity.class, polarBearEntity.getBoundingBox().expand(8.0, 4.0, 8.0))) {
                    if (object.isBaby()) {
                        return true;
                    }
                }
            }

            if (entity instanceof RabbitEntity rabbitEntity) {
                return rabbitEntity.getVariant().getId() == 99;
            } else if (entity instanceof WolfEntity wolfEntity) {
                return wolfEntity.hasAngerTime();
            } else {
                return false;
            }
        }
    }

    private boolean isPlayer(Entity entity) {
        return entity instanceof OtherClientPlayerEntity;
    }

    private boolean isNeutral(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        } else {
            return !(entity instanceof PlayerEntity) && !this.isHostile(entity);
        }
    }

    private record ModelPartWithResourceLocation(ModelPart modelPart, Identifier resourceLocation) {
    }
}
