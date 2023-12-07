package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.IllusionerEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.GlowSquidEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SalmonEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.TadpoleEntity;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.Identifier;

import java.util.Arrays;

public enum EnumMobs {
    GENERICHOSTILE(null, "Monster", false, 8.0F, "textures/entity/zombie/zombie.png", "", true, false),
    GENERICNEUTRAL(null, "Mob", false, 8.0F, "textures/entity/pig/pig.png", "", false, true),
    GENERICTAME(null, "Unknown_Tame", false, 8.0F, "textures/entity/wolf/wolf.png", "", false, true),
    AXOLOTL(AxolotlEntity.class, "Axolotl", true, 0.0F, "textures/entity/axolotl/axolotl_blue.png", "", false, true),
    ALLAY(AllayEntity.class, "Allay", true, 4.0F, "textures/entity/allay/allay.png", "", false, true),
    ARMOR_STAND(ArmorStandEntity.class, "Armor_Stand", true, 0.0F, "textures/entity/armorstand/wood.png", "", true, true),
    BAT(BatEntity.class, "Bat", true, 4.0F, "textures/entity/bat.png", "", false, true),
    BEE(BeeEntity.class, "Bee", true, 0.0F, "textures/entity/bee/bee.png", "", true, true),
    BLAZE(BlazeEntity.class, "Blaze", true, 0.0F, "textures/entity/blaze.png", "", true, false),
    BREEZE(BreezeEntity.class, "Breeze", true, 0.0F, "textures/entity/breeze.png", "", true, false),
    CAT(CatEntity.class, "Cat", true, 0.0F, "textures/entity/cat/siamese.png", "", false, true),
    CAMEL(CamelEntity.class, "Camel", true, 0.0F, "textures/entity/camel/camel.png", "", false, true),
    CAVESPIDER(CaveSpiderEntity.class, "Cave_Spider", true, 0.0F, "textures/entity/spider/cave_spider.png", "", true, false),
    CHICKEN(ChickenEntity.class, "Chicken", true, 6.0F, "textures/entity/chicken.png", "", false, true),
    COD(CodEntity.class, "Cod", true, 8.0F, "textures/entity/fish/cod.png", "", false, true),
    COW(CowEntity.class, "Cow", true, 0.0F, "textures/entity/cow/cow.png", "", false, true),
    CREEPER(CreeperEntity.class, "Creeper", true, 0.0F, "textures/entity/creeper/creeper.png", "", true, false),
    DOLPHIN(DolphinEntity.class, "Dolphin", true, 0.0F, "textures/entity/dolphin.png", "", false, true),
    DROWNED(DrownedEntity.class, "Drowned", true, 0.0F, "textures/entity/zombie/drowned.png", "textures/entity/zombie/drowned_outer_layer.png", true, false),
    ENDERDRAGON(EnderDragonEntity.class, "Ender_Dragon", true, 16.0F, "textures/entity/enderdragon/dragon.png", "", true, false),
    ENDERMAN(EndermanEntity.class, "Enderman", true, 0.0F, "textures/entity/enderman/enderman.png", "textures/entity/enderman/enderman_eyes.png", true, false),
    ENDERMITE(EndermiteEntity.class, "Endermite", true, 0.0F, "textures/entity/endermite.png", "", true, false),
    EVOKER(EvokerEntity.class, "Evoker", true, 0.0F, "textures/entity/illager/evoker.png", "", true, false),
    FOX(FoxEntity.class, "Fox", true, 0.0F, "textures/entity/fox/fox.png", "", false, true),
    FROG(FrogEntity.class, "Frog", true, 0.0F, "textures/entity/frog/cold_frog.png", "", false, true),
    GHAST(GhastEntity.class, "Ghast", true, 16.0F, "textures/entity/ghast/ghast.png", "", true, false),
    GHASTATTACKING(null, "Ghast", false, 16.0F, "textures/entity/ghast/ghast_shooting.png", "", true, false),
    GLOWSQUID(GlowSquidEntity.class, "Glow_Squid", true, 0.0F, "textures/entity/squid/glow_squid.png", "", false, true),
    GOAT(GoatEntity.class, "Goat", true, 0.0F, "textures/entity/goat/goat.png", "", false, true),
    GUARDIAN(GuardianEntity.class, "Guardian", true, 6.0F, "textures/entity/guardian.png", "", true, false),
    GUARDIANELDER(ElderGuardianEntity.class, "Elder_Guardian", true, 12.0F, "textures/entity/guardian_elder.png", "", true, false),
    HOGLIN(HoglinEntity.class, "Hoglin", true, 0.0F, "textures/entity/hoglin/hoglin.png", "", true, false),
    HORSE(HorseEntity.class, "Horse", true, 8.0F, "textures/entity/horse/horse_creamy.png", "textures/entity/horse/horse_markings_white.png", false, true),
    HUSK(HuskEntity.class, "Husk", true, 0.0F, "textures/entity/zombie/husk.png", "", true, false),
    ILLUSIONER(IllusionerEntity.class, "Illusioner", true, 0.0F, "textures/entity/illager/illusioner.png", "", true, false),
    IRONGOLEM(IronGolemEntity.class, "Iron_Golem", true, 8.0F, "textures/entity/iron_golem/iron_golem.png", "", false, true),
    LLAMA(LlamaEntity.class, "Llama", true, 8.0F, "textures/entity/llama/brown.png", "", false, true),
    LLAMATRADER(TraderLlamaEntity.class, "Trader_Llama", true, 8.0F, "textures/entity/llama/brown.png", "", false, true),
    MAGMA(MagmaCubeEntity.class, "Magma_Cube", true, 8.0F, "textures/entity/slime/magmacube.png", "", true, false),
    MOOSHROOM(MooshroomEntity.class, "Mooshroom", true, 40.0F, "textures/entity/cow/red_mooshroom.png", "", false, true),
    OCELOT(OcelotEntity.class, "Ocelot", true, 0.0F, "textures/entity/cat/ocelot.png", "", false, true),
    PANDA(PandaEntity.class, "Panda", true, 0.0F, "textures/entity/panda/panda.png", "", true, true),
    PARROT(ParrotEntity.class, "Parrot", true, 8.0F, "textures/entity/parrot/parrot_red_blue.png", "", false, true),
    PHANTOM(PhantomEntity.class, "Phantom", true, 10.0F, "textures/entity/phantom.png", "textures/entity/phantom_eyes.png", true, false),
    PIG(PigEntity.class, "Pig", true, 0.0F, "textures/entity/pig/pig.png", "", false, true),
    PIGLIN(PiglinEntity.class, "Piglin", true, 0.0F, "textures/entity/piglin/piglin.png", "", true, false),
    PIGLINBRUTE(PiglinBruteEntity.class, "Piglin_Brute", true, 0.0F, "textures/entity/piglin/piglin_brute.png", "", true, false),
    PIGLINZOMBIE(ZombifiedPiglinEntity.class, "Zombie_Piglin", true, 0.0F, "textures/entity/piglin/zombified_piglin.png", "", true, true),
    PILLAGER(PillagerEntity.class, "Pillager", true, 0.0F, "textures/entity/illager/pillager.png", "", true, false),
    PLAYER(OtherClientPlayerEntity.class, "Player", false, 8.0F, "textures/entity/steve.png", "", false, false),
    POLARBEAR(PolarBearEntity.class, "Polar_Bear", true, 0.0F, "textures/entity/bear/polarbear.png", "", true, true),
    PUFFERFISH(PufferfishEntity.class, "Pufferfish", true, 3.0F, "textures/entity/fish/pufferfish.png", "", false, true),
    PUFFERFISHHALF(null, "Pufferfish_Half", false, 5.0F, "textures/entity/fish/pufferfish.png", "", false, true),
    PUFFERFISHFULL(null, "Pufferfish_Full", false, 8.0F, "textures/entity/fish/pufferfish.png", "", false, true),
    RABBIT(RabbitEntity.class, "Rabbit", true, 0.0F, "textures/entity/rabbit/salt.png", "", false, true),
    RAVAGER(RavagerEntity.class, "Ravager", true, 0.0F, "textures/entity/illager/ravager.png", "", true, false),
    SALMON(SalmonEntity.class, "Salmon", true, 13.0F, "textures/entity/fish/salmon.png", "", false, true),
    SHEEP(SheepEntity.class, "Sheep", true, 0.0F, "textures/entity/sheep/sheep.png", "", false, true),
    SHULKER(ShulkerEntity.class, "Shulker", true, 0.0F, "textures/entity/shulker/shulker_purple.png", "", true, false),
    SILVERFISH(SilverfishEntity.class, "Silverfish", true, 0.0F, "textures/entity/silverfish.png", "", true, false),
    SKELETON(SkeletonEntity.class, "Skeleton", true, 0.0F, "textures/entity/skeleton/skeleton.png", "", true, false),
    SKELETONWITHER(WitherSkeletonEntity.class, "Wither_Skeleton", true, 0.0F, "textures/entity/skeleton/wither_skeleton.png", "", true, false),
    SLIME(SlimeEntity.class, "Slime", true, 8.0F, "textures/entity/slime/slime.png", "", true, false),
    SNIFFER(SnifferEntity.class, "Sniffer", true, 0.0F, "textures/entity/sniffer/sniffer.png", "", false, true),
    SNOWGOLEM(SnowGolemEntity.class, "Snow_Golem", true, 0.0F, "textures/entity/snow_golem.png", "", false, true),
    SPIDER(SpiderEntity.class, "Spider", true, 0.0F, "textures/entity/spider/spider.png", "", true, false),
    SQUID(SquidEntity.class, "Squid", true, 0.0F, "textures/entity/squid/squid.png", "", false, true),
    STRAY(StrayEntity.class, "Stray", true, 0.0F, "textures/entity/skeleton/stray.png", "textures/entity/skeleton/stray_overlay.png", true, false),
    STRIDER(StriderEntity.class, "Strider", true, 0.0F, "textures/entity/strider/strider.png", "", false, true),
    TADPOLE(TadpoleEntity.class, "Tadpole", true, 0.0F, "textures/entity/tadpole/tadpole.png", "", false, true),
    TROPICALFISHA(TropicalFishEntity.class, "Tropical_Fish", true, 5.0F, "textures/entity/fish/tropical_a.png", "textures/entity/fish/tropical_a_pattern_1.png", false, true),
    TROPICALFISHB(null, "Tropical_Fish", false, 6.0F, "textures/entity/fish/tropical_b.png", "textures/entity/fish/tropical_b_pattern_4.png", false, true),
    TURTLE(TurtleEntity.class, "Turtle", true, 0.0F, "textures/entity/turtle/big_sea_turtle.png", "", false, true),
    VEX(VexEntity.class, "Vex", true, 0.0F, "textures/entity/illager/vex.png", "", true, false),
    VEXCHARGING(null, "Vex", false, 0.0F, "textures/entity/illager/vex_charging.png", "", true, false),
    VILLAGER(VillagerEntity.class, "Villager", true, 0.0F, "textures/entity/villager/villager.png", "textures/entity/villager/profession/farmer.png", false, true),
    VINDICATOR(VindicatorEntity.class, "Vindicator", true, 0.0F, "textures/entity/illager/vindicator.png", "", true, false),
    WANDERINGTRADER(WanderingTraderEntity.class, "Wandering_Trader", true, 0.0F, "textures/entity/wandering_trader.png", "", false, true),
    WITCH(WitchEntity.class, "Witch", true, 0.0F, "textures/entity/witch.png", "", true, false),
    WITHER(WitherEntity.class, "Wither", true, 24.0F, "textures/entity/wither/wither.png", "", true, false),
    WITHERINVULNERABLE(null, "Wither", false, 24.0F, "textures/entity/wither/wither_invulnerable.png", "", true, false),
    WARDEN(WardenEntity.class, "Wolf", true, 0.0F, "textures/entity/warden/warden.png", "", true, false),
    WOLF(WolfEntity.class, "Wolf", true, 0.0F, "textures/entity/wolf/wolf.png", "", true, true),
    ZOGLIN(ZoglinEntity.class, "Zoglin", true, 0.0F, "textures/entity/hoglin/zoglin.png", "", true, false),
    ZOMBIE(ZombieEntity.class, "Zombie", true, 0.0F, "textures/entity/zombie/zombie.png", "", true, false),
    ZOMBIEVILLAGER(ZombieVillagerEntity.class, "Zombie_villager", true, 0.0F, "textures/entity/zombie_villager/zombie_villager.png", "textures/entity/zombie_villager/profession/farmer.png", true, false),
    UNKNOWN(null, "Unknown", false, 8.0F, "/mob/uknown.png", "", true, true);

    public final Class<? extends Entity> clazz;
    public final String id;
    public final boolean isTopLevelUnit;
    public final float expectedWidth;
    public final Identifier resourceLocation;
    public Identifier secondaryResourceLocation;
    public final boolean isHostile;
    public final boolean isNeutral;
    public boolean enabled;

    public static EnumMobs getMobByName(String par0) {
        return Arrays.stream(values()).filter(enumMob -> enumMob.id.equals(par0)).findFirst().orElse(null);
    }

    public static EnumMobs getMobTypeByEntity(Entity entity) {
        Class<? extends Entity> clazz = entity.getClass();
        if (clazz.equals(TropicalFishEntity.class)) {
            return ((TropicalFishEntity) entity).getVariant().getId() == 0 ? TROPICALFISHA : TROPICALFISHB;
        } else {
            return getMobTypeByClass(clazz);
        }
    }

    private static EnumMobs getMobTypeByClass(Class<? extends Entity> clazz) {
        if (OtherClientPlayerEntity.class.isAssignableFrom(clazz)) {
            return PLAYER;
        } else if (!clazz.equals(HorseEntity.class) && !clazz.equals(DonkeyEntity.class) && !clazz.equals(MuleEntity.class) && !clazz.equals(SkeletonHorseEntity.class) && !clazz.equals(ZombieHorseEntity.class)) {
            return Arrays.stream(values()).filter(enumMob -> clazz.equals(enumMob.clazz)).findFirst().orElse(UNKNOWN);
        } else {
            return HORSE;
        }
    }

    EnumMobs(Class<? extends Entity> clazz, String name, boolean topLevelUnit, float expectedWidth, String path, String secondaryPath, boolean isHostile, boolean isNeutral) {
        this.clazz = clazz;
        this.id = name;
        this.isTopLevelUnit = topLevelUnit;
        this.expectedWidth = expectedWidth;
        this.resourceLocation = new Identifier(path.toLowerCase());
        this.secondaryResourceLocation = secondaryPath.isEmpty() ? null : new Identifier(secondaryPath.toLowerCase());
        this.isHostile = isHostile;
        this.isNeutral = isNeutral;
        this.enabled = true;
    }

    public int returnEnumOrdinal() {
        return this.ordinal();
    }
}
