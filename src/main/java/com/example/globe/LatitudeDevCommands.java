package com.example.globe;

import com.example.globe.util.LatitudeBands;
import com.example.globe.util.LatitudeMath;
import com.example.globe.world.LatitudeBiomes;
import com.example.globe.world.LatitudeCalendarMath;
import com.example.globe.world.LatitudeSeasonBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.level.ServerWorldProperties;

import java.util.EnumSet;
import java.util.Locale;

/**
 * Safe runtime diagnostics for Latitude's geographic calendar, solar, climate,
 * and effective-light layers. These commands intentionally report raw and
 * derived values side by side so a visual symptom can be localized to the
 * server clock, geographic adapter, solar math, climate bridge, or renderer.
 */
public final class LatitudeDevCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(CommandManager.literal("lattp")
            .then(CommandManager.literal("tropical").executes(ctx -> tp(ctx, LatitudeBands.Band.TROPICAL, +1)))
            .then(CommandManager.literal("equator").executes(ctx -> tp(ctx, LatitudeBands.Band.TROPICAL, +1)))
            .then(CommandManager.literal("tropics").executes(ctx -> tp(ctx, LatitudeBands.Band.TROPICAL, +1)))
            .then(CommandManager.literal("subtropical").executes(ctx -> tp(ctx, LatitudeBands.Band.SUBTROPICAL, +1)))
            .then(CommandManager.literal("subtropics").executes(ctx -> tp(ctx, LatitudeBands.Band.SUBTROPICAL, +1)))
            .then(CommandManager.literal("temperate").executes(ctx -> tp(ctx, LatitudeBands.Band.TEMPERATE, +1)))
            .then(CommandManager.literal("subpolar").executes(ctx -> tp(ctx, LatitudeBands.Band.SUBPOLAR, +1)))
            .then(CommandManager.literal("polar").executes(ctx -> tp(ctx, LatitudeBands.Band.POLAR, +1)))
        );

        dispatcher.register(CommandManager.literal("lattps")
            .then(CommandManager.literal("tropical").executes(ctx -> tp(ctx, LatitudeBands.Band.TROPICAL, -1)))
            .then(CommandManager.literal("equator").executes(ctx -> tp(ctx, LatitudeBands.Band.TROPICAL, -1)))
            .then(CommandManager.literal("tropics").executes(ctx -> tp(ctx, LatitudeBands.Band.TROPICAL, -1)))
            .then(CommandManager.literal("subtropical").executes(ctx -> tp(ctx, LatitudeBands.Band.SUBTROPICAL, -1)))
            .then(CommandManager.literal("subtropics").executes(ctx -> tp(ctx, LatitudeBands.Band.SUBTROPICAL, -1)))
            .then(CommandManager.literal("temperate").executes(ctx -> tp(ctx, LatitudeBands.Band.TEMPERATE, -1)))
            .then(CommandManager.literal("subpolar").executes(ctx -> tp(ctx, LatitudeBands.Band.SUBPOLAR, -1)))
            .then(CommandManager.literal("polar").executes(ctx -> tp(ctx, LatitudeBands.Band.POLAR, -1)))
        );

        dispatcher.register(CommandManager.literal("latitude")
            .then(CommandManager.literal("debug")
                .executes(LatitudeDevCommands::debugHere)
                .then(CommandManager.literal("here").executes(LatitudeDevCommands::debugHere))
                .then(CommandManager.literal("all").executes(LatitudeDevCommands::debugHere))
                .then(CommandManager.literal("time").executes(LatitudeDevCommands::debugTime))
                .then(CommandManager.literal("solar").executes(LatitudeDevCommands::debugSolar))
                .then(CommandManager.literal("climate").executes(LatitudeDevCommands::debugClimate))
                .then(CommandManager.literal("light").executes(LatitudeDevCommands::debugLight))
                .then(CommandManager.literal("samples").executes(LatitudeDevCommands::debugSamples))
                .then(CommandManager.literal("at")
                    .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                            .executes(LatitudeDevCommands::debugAtCoordinates))))
            )
            .then(CommandManager.literal("time")
                .executes(LatitudeDevCommands::debugTime)
                .then(CommandManager.literal("report").executes(LatitudeDevCommands::debugTime))
                .then(CommandManager.literal("set")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.literal("day").executes(ctx -> setTimeOfDay(ctx, 1000L, "day")))
                    .then(CommandManager.literal("noon").executes(ctx -> setTimeOfDay(ctx, 6000L, "noon")))
                    .then(CommandManager.literal("sunset").executes(ctx -> setTimeOfDay(ctx, 12000L, "sunset")))
                    .then(CommandManager.literal("midnight").executes(ctx -> setTimeOfDay(ctx, 18000L, "midnight")))
                    .then(CommandManager.literal("ticks")
                        .then(CommandManager.argument("timeOfDay", LongArgumentType.longArg(0L))
                            .executes(ctx -> setTimeOfDay(ctx,
                                    LongArgumentType.getLong(ctx, "timeOfDay"), "explicit"))))
                    .then(CommandManager.literal("dayofyear")
                        .then(CommandManager.argument("day", IntegerArgumentType.integer(1, LatitudeCalendarMath.DAYS_PER_YEAR))
                            .executes(ctx -> setDayOfYear(ctx, IntegerArgumentType.getInteger(ctx, "day")))))
                )
            )
        );
    }

    private static int debugHere(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            return dump(source, player.getServerWorld(), player.getBlockPos(), "HERE");
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static int debugAtCoordinates(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerWorld world = source.getWorld();
            int x = (int) Math.floor(DoubleArgumentType.getDouble(ctx, "x"));
            int z = (int) Math.floor(DoubleArgumentType.getDouble(ctx, "z"));
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            return dump(source, world, new BlockPos(x, y, z), "COORDINATES");
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static int debugTime(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            BlockPos pos = player.getBlockPos();
            LatitudeSeasonBridge.Snapshot snapshot = LatitudeSeasonBridge.at(world, pos);
            LatitudeCalendarMath.CalendarDate local = LatitudeSeasonBridge.localCalendarAt(world, pos.getX());
            send(source, "[latitude.time] world=" + world.getRegistryKey().getValue()
                    + " latitudeWorld=" + LatitudeSeasonBridge.isLatitudeWorld(world));
            send(source, "[latitude.time] worldAge(getTime)=" + world.getTime()
                    + " timeOfDay(getTimeOfDay)=" + world.getTimeOfDay()
                    + " dayTime=" + Math.floorMod(world.getTimeOfDay(), LatitudeCalendarMath.TICKS_PER_DAY)
                    + " daylightCycle=" + world.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE));
            send(source, "[latitude.time] global calendar=" + calendarText(snapshot.calendar()));
            send(source, "[latitude.time] local calendar=" + calendarText(local)
                    + " longitude=" + fmt(snapshot.longitudeEastPositive()) + "degE"
                    + " offset=" + fmt(snapshot.longitudeEastPositive() / 15.0) + "h");
            send(source, "[latitude.time] localSolarTime=" + fmtClock(snapshot.solar().localSolarTimeHours())
                    + " localDayProgress=" + fmt(snapshot.solar().localDayProgress())
                    + " note=0 ticks is dawn, 6000 is noon, 12000 sunset, 18000 midnight");
            send(source, "[latitude.time] vanilla isDay=" + world.isDay()
                    + " isNight=" + world.isNight()
                    + " raining=" + world.isRaining()
                    + " thundering=" + world.isThundering());
            return 1;
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static int debugSolar(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            LatitudeSeasonBridge.Snapshot snapshot = LatitudeSeasonBridge.at(world, player.getBlockPos());
            LatitudeCalendarMath.SolarPosition solar = snapshot.solar();
            send(source, "[latitude.solar] lat=" + fmt(snapshot.latitudeNorthPositive()) + "degN"
                    + " lon=" + fmt(snapshot.longitudeEastPositive()) + "degE"
                    + " hemisphere=" + snapshot.hemisphere()
                    + " active=" + snapshot.active());
            send(source, "[latitude.solar] globalSeason=" + snapshot.calendar().season()
                    + " localSeason=" + solar.localSeason()
                    + " declination=" + fmt(solar.solarDeclinationDegrees()) + "deg"
                    + " seasonalStrength=" + fmt(solar.seasonalStrength()));
            send(source, "[latitude.solar] elevation=" + fmt(Math.toDegrees(solar.solarElevationRadians())) + "deg"
                    + " daylight=" + fmt(solar.daylightFraction() * 24.0) + "h"
                    + " polarDay=" + solar.polarDay()
                    + " polarNight=" + solar.polarNight());
            send(source, "[latitude.solar] dailyEnergy=" + fmt(solar.dailySolarEnergy())
                    + " heatSignal=" + fmt(solar.solarHeatSignal())
                    + " equatorialWetness=" + fmt(solar.equatorialWetness())
                    + " skyFactor=" + fmt(solar.skyLightFactor()));
            send(source, "[latitude.solar] moonPhase=" + snapshot.calendar().lunarPhase()
                    + " lunarDay=" + snapshot.calendar().lunarDay()
                    + " moonElevation=" + fmt(Math.toDegrees(solar.lunarElevationRadians())) + "deg"
                    + " illumination=" + fmt(solar.lunarIllumination()));
            return 1;
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static int debugClimate(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            BlockPos pos = player.getBlockPos();
            RegistryEntry<Biome> entry = world.getBiome(pos);
            Biome biome = entry.value();
            LatitudeSeasonBridge.Snapshot snapshot = LatitudeSeasonBridge.at(world, pos);
            float baseTemperature = biome.getTemperature();
            float effectiveTemperature = LatitudeSeasonBridge.adjustedTemperature(world, pos, baseTemperature);
            Biome.Precipitation precipitation = LatitudeSeasonBridge.precipitationFor(biome, world, pos);
            boolean noSnow = LatitudeSeasonBridge.doesNotSnow(biome, world, pos);
            send(source, "[latitude.climate] biome=" + biomeId(entry)
                    + " hasPrecipitation=" + biome.hasPrecipitation()
                    + " baseTemperature=" + fmt(baseTemperature)
                    + " effectiveTemperature=" + fmt(effectiveTemperature));
            send(source, "[latitude.climate] precipitation=" + precipitation
                    + " doesNotSnow=" + noSnow
                    + " localSeason=" + snapshot.solar().localSeason()
                    + " heatSignal=" + fmt(snapshot.solar().solarHeatSignal())
                    + " wetness=" + fmt(snapshot.solar().equatorialWetness()));
            send(source, "[latitude.climate] vanillaWeather raining=" + world.isRaining()
                    + " thundering=" + world.isThundering()
                    + " biomePrecipitationIsLocal=" + LatitudeSeasonBridge.isLatitudeWorld(world));
            return 1;
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static int debugLight(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            BlockPos pos = player.getBlockPos();
            int storedSky = world.getLightingProvider().get(LightType.SKY).getLightLevel(pos);
            int storedBlock = world.getLightingProvider().get(LightType.BLOCK).getLightLevel(pos);
            int effectiveSky = LatitudeSeasonBridge.effectiveSkyLight(world, pos, storedSky);
            int combined = LatitudeSeasonBridge.effectiveCombinedLight(world, pos, storedSky, storedBlock);
            int ambient = world.getAmbientDarkness();
            int effectiveBase = LatitudeSeasonBridge.effectiveBaseLight(world, pos, storedSky, storedBlock, ambient);
            LatitudeSeasonBridge.Snapshot snapshot = LatitudeSeasonBridge.at(world, pos);
            send(source, "[latitude.light] pos=" + pos.toShortString()
                    + " storedSky=" + storedSky
                    + " storedBlock=" + storedBlock
                    + " ambientDarkness=" + ambient);
            send(source, "[latitude.light] effectiveSky=" + effectiveSky
                    + " combined=" + combined
                    + " effectiveBase=" + effectiveBase
                    + " visualSkyFactor=" + fmt(snapshot.solar().skyLightFactor())
                    + " gameplaySkyFactor=" + fmt(LatitudeSeasonBridge.gameplaySkyLightFactor(snapshot.solar())));
            send(source, "[latitude.light] raw provider access bypasses local sky scaling="
                    + (!LatitudeSeasonBridge.isLatitudeWorld(world) ? "n/a" : "yes")
                    + " normalWorldQueryUsesLatitude=" + LatitudeSeasonBridge.isLatitudeWorld(world));
            return 1;
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static int debugSamples(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            WorldBorder border = world.getWorldBorder();
            double cx = border.getCenterX();
            double cz = border.getCenterZ();
            double half = LatitudeMath.halfSize(border);
            send(source, "[latitude.samples] borderCenter=(" + fmt(cx) + "," + fmt(cz) + ") halfSize=" + fmt(half));
            sample(source, world, "WEST", cx - half, cz);
            sample(source, world, "EQUATOR", cx, cz);
            sample(source, world, "EAST", cx + half, cz);
            sample(source, world, "NORTH", cx, cz - half);
            sample(source, world, "SOUTH", cx, cz + half);
            sample(source, world, "CURRENT", player.getX(), player.getZ());
            return 1;
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static void sample(ServerCommandSource source, ServerWorld world, String label, double x, double z) {
        LatitudeSeasonBridge.Snapshot snapshot = LatitudeSeasonBridge.at(world, x, z);
        LatitudeCalendarMath.SolarPosition solar = snapshot.solar();
        send(source, "[latitude.sample] " + label
                + " x=" + fmt(x) + " z=" + fmt(z)
                + " lat=" + fmt(snapshot.latitudeNorthPositive()) + "degN"
                + " lon=" + fmt(snapshot.longitudeEastPositive()) + "degE"
                + " localTime=" + fmtClock(solar.localSolarTimeHours())
                + " elevation=" + fmt(Math.toDegrees(solar.solarElevationRadians())) + "deg"
                + " season=" + solar.localSeason()
                + " daylight=" + fmt(solar.daylightFraction() * 24.0) + "h");
    }

    private static int dump(ServerCommandSource source, ServerWorld world, BlockPos pos, String label) {
        RegistryEntry<Biome> entry = world.getBiome(pos);
        Biome biome = entry.value();
        LatitudeSeasonBridge.Snapshot snapshot = LatitudeSeasonBridge.at(world, pos);
        LatitudeCalendarMath.CalendarDate localCalendar = LatitudeSeasonBridge.localCalendarAt(world, pos.getX());
        LatitudeCalendarMath.SolarPosition solar = snapshot.solar();
        WorldBorder border = world.getWorldBorder();
        int storedSky = world.getLightingProvider().get(LightType.SKY).getLightLevel(pos);
        int storedBlock = world.getLightingProvider().get(LightType.BLOCK).getLightLevel(pos);
        int ambient = world.getAmbientDarkness();
        float baseTemperature = biome.getTemperature();
        float effectiveTemperature = LatitudeSeasonBridge.adjustedTemperature(world, pos, baseTemperature);

        send(source, "========== LATITUDE DEBUG " + label + " ==========");
        send(source, "[identity] world=" + world.getRegistryKey().getValue()
                + " latitudeWorld=" + snapshot.active()
                + " pos=" + pos.toShortString()
                + " borderCenter=(" + fmt(border.getCenterX()) + "," + fmt(border.getCenterZ()) + ")"
                + " borderSize=" + fmt(border.getSize()));
        send(source, "[geography] latitude=" + fmt(snapshot.latitudeNorthPositive()) + "degN"
                + " hemisphere=" + snapshot.hemisphere()
                + " longitude=" + fmt(snapshot.longitudeEastPositive()) + "degE"
                + " localOffset=" + fmt(snapshot.longitudeEastPositive() / 15.0) + "h"
                + " zone=" + LatitudeMath.zoneFor(border, pos.getZ() - border.getCenterZ()));
        send(source, "[clock] worldAge=" + world.getTime()
                + " timeOfDay=" + world.getTimeOfDay()
                + " dayTime=" + Math.floorMod(world.getTimeOfDay(), LatitudeCalendarMath.TICKS_PER_DAY)
                + " daylightCycle=" + world.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE));
        send(source, "[calendar.global] " + calendarText(snapshot.calendar()));
        send(source, "[calendar.local]  " + calendarText(localCalendar));
        send(source, "[solar] localTime=" + fmtClock(solar.localSolarTimeHours())
                + " elevation=" + fmt(Math.toDegrees(solar.solarElevationRadians())) + "deg"
                + " declination=" + fmt(solar.solarDeclinationDegrees()) + "deg"
                + " daylight=" + fmt(solar.daylightFraction() * 24.0) + "h"
                + " polarDay=" + solar.polarDay()
                + " polarNight=" + solar.polarNight());
        send(source, "[season] global=" + snapshot.calendar().season()
                + " local=" + solar.localSeason()
                + " seasonalStrength=" + fmt(solar.seasonalStrength())
                + " dailyEnergy=" + fmt(solar.dailySolarEnergy())
                + " heatSignal=" + fmt(solar.solarHeatSignal())
                + " equatorialWetness=" + fmt(solar.equatorialWetness()));
        send(source, "[moon] phase=" + snapshot.calendar().lunarPhase()
                + " lunarDay=" + snapshot.calendar().lunarDay()
                + " elevation=" + fmt(Math.toDegrees(solar.lunarElevationRadians())) + "deg"
                + " illumination=" + fmt(solar.lunarIllumination()));
        send(source, "[biome] id=" + biomeId(entry)
                + " hasPrecipitation=" + biome.hasPrecipitation()
                + " baseTemp=" + fmt(baseTemperature)
                + " effectiveTemp=" + fmt(effectiveTemperature)
                + " precipitation=" + LatitudeSeasonBridge.precipitationFor(biome, world, pos)
                + " doesNotSnow=" + LatitudeSeasonBridge.doesNotSnow(biome, world, pos));
        send(source, "[weather] raining=" + world.isRaining()
                + " thundering=" + world.isThundering());
        send(source, "[light.raw] sky=" + storedSky
                + " block=" + storedBlock
                + " ambientDarkness=" + ambient);
        send(source, "[light.effective] sky=" + LatitudeSeasonBridge.effectiveSkyLight(world, pos, storedSky)
                + " combined=" + LatitudeSeasonBridge.effectiveCombinedLight(world, pos, storedSky, storedBlock)
                + " base=" + LatitudeSeasonBridge.effectiveBaseLight(world, pos, storedSky, storedBlock, ambient)
                + " visualSkyFactor=" + fmt(solar.skyLightFactor())
                + " gameplaySkyFactor=" + fmt(LatitudeSeasonBridge.gameplaySkyLightFactor(solar)));
        send(source, "[interpretation] if timeOfDay changes but localTime/elevation do not, inspect the client sky mixin; if localTime differs by X, longitude is active; if storedSky stays high while effectiveSky falls, local light is working.");
        send(source, "========== END LATITUDE DEBUG ==========");
        return 1;
    }

    private static int setTimeOfDay(CommandContext<ServerCommandSource> ctx, long value, String label) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerWorld world = source.getWorld();
            long normalized = Math.floorMod(value, LatitudeCalendarMath.TICKS_PER_DAY);
            long before = world.getTimeOfDay();
            ((net.minecraft.world.level.ServerWorldProperties) world.getLevelProperties()).setTimeOfDay(normalized);
            source.sendFeedback(() -> Text.literal("[latitude.time] set " + label
                    + ": before=" + before
                    + " after=" + world.getTimeOfDay()
                    + " dayTime=" + Math.floorMod(world.getTimeOfDay(), LatitudeCalendarMath.TICKS_PER_DAY)), false);
            return debugTime(ctx);
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static int setDayOfYear(CommandContext<ServerCommandSource> ctx, int day) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerWorld world = source.getWorld();
            ServerWorldProperties properties = (ServerWorldProperties) world.getLevelProperties();
            long currentDay = Math.floorDiv(world.getTime(), LatitudeCalendarMath.TICKS_PER_DAY);
            long year = Math.floorDiv(currentDay, LatitudeCalendarMath.DAYS_PER_YEAR);
            long tickInDay = Math.floorMod(world.getTimeOfDay(), LatitudeCalendarMath.TICKS_PER_DAY);
            long target = (year * LatitudeCalendarMath.DAYS_PER_YEAR + day - 1L)
                    * LatitudeCalendarMath.TICKS_PER_DAY + tickInDay;
            long beforeTime = world.getTime();
            properties.setTime(target);
            properties.setTimeOfDay(tickInDay);
            source.sendFeedback(() -> Text.literal("[latitude.time] set dayofyear=" + day
                    + ": beforeTime=" + beforeTime
                    + " afterTime=" + world.getTime()
                    + " dayTime=" + Math.floorMod(world.getTimeOfDay(), LatitudeCalendarMath.TICKS_PER_DAY)), false);
            return debugTime(ctx);
        } catch (Exception e) {
            return error(ctx, e);
        }
    }

    private static String calendarText(LatitudeCalendarMath.CalendarDate date) {
        return "Y" + date.year()
                + " day=" + date.dayOfYear()
                + " month=" + date.month() + "/" + date.dayOfMonth()
                + " week=" + date.weekOfYear() + " (monthWeek=" + date.weekOfMonth() + ")"
                + " dow=" + date.dayOfWeek()
                + " lunarDay=" + date.lunarDay()
                + " phase=" + date.lunarPhase()
                + " season=" + date.season()
                + " yearProgress=" + fmt(date.yearProgress());
    }

    private static String biomeId(RegistryEntry<Biome> entry) {
        return entry.getKey().map(key -> key.getValue().toString()).orElse("<unregistered>");
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String fmtClock(double hours) {
        double normalized = hours - Math.floor(hours / 24.0) * 24.0;
        int hour = (int) Math.floor(normalized);
        int minute = (int) Math.floor((normalized - hour) * 60.0);
        return String.format(Locale.ROOT, "%02d:%02d (%s h)", hour, minute, fmt(hours));
    }

    private static void send(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }

    private static int error(CommandContext<ServerCommandSource> ctx, Exception e) {
        ctx.getSource().sendError(Text.literal("[latitude.debug] error: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        e.printStackTrace();
        return 0;
    }

    private static int tp(CommandContext<ServerCommandSource> ctx, LatitudeBands.Band band, int hemiSign) {
        try {
            ServerCommandSource source = ctx.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = source.getWorld();

            int radius = getAuthoritativeRadius(world);
            double targetDeg = (band.lowDeg() + band.highDeg()) * 0.5;
            int targetZ = LatitudeMath.zForLatitudeDeg(targetDeg, radius) * hemiSign;

            source.sendFeedback(() -> Text.literal("[lattp] band=" + band.id()
                + " targetZ=" + targetZ
                + " hemi=" + (hemiSign > 0 ? "N" : "S")
                + " activeRadius=" + radius
                + " deg=" + String.format(Locale.ROOT, "%.2f", targetDeg)
            ), false);

            BlockPos safe = findSafeLand(world, targetZ);
            if (safe == null) {
                source.sendError(Text.literal("[lattp] no land found near Z=" + targetZ));
                return 0;
            }

            world.getChunk(safe.getX() >> 4, safe.getZ() >> 4);
            player.teleport(world, safe.getX() + 0.5, (double)safe.getY(), safe.getZ() + 0.5, EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class), player.getYaw(), player.getPitch());
            source.sendFeedback(() -> Text.literal("[lattp] teleported: " + safe.toShortString()
                + " topY=" + safe.getY()
                + " biome=" + world.getBiome(safe).getKey().map(k -> k.getValue().toString()).orElse("?")
            ), true);
            return 1;

        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("[lattp] error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int getAuthoritativeRadius(ServerWorld world) {
        int active = LatitudeBiomes.ACTIVE_RADIUS_BLOCKS;
        if (active > 0) return active;
        return (int) Math.round(LatitudeMath.halfSize(world.getWorldBorder()));
    }

    private static BlockPos findSafeLand(ServerWorld world, int targetZ) {
        int searchX = 10000;
        int stepX = 64;
        int[] dzs = new int[] {0, 64, -64, 128, -128, 256, -256, 512, -512};

        for (int dz : dzs) {
            int z = targetZ + dz;
            for (int x = 0; x <= searchX; x += stepX) {
                BlockPos p = check(world, x, z);
                if (p != null) return p;
                if (x != 0) {
                    p = check(world, -x, z);
                    if (p != null) return p;
                }
            }
        }
        return null;
    }

    private static BlockPos check(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos p = new BlockPos(x, y, z);

        RegistryEntry<Biome> biome = world.getBiome(p);
        if (biome.isIn(net.minecraft.registry.tag.BiomeTags.IS_OCEAN)) return null;

        return p;
    }
}
