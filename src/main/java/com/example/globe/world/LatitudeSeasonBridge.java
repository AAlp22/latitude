package com.example.globe.world;

import com.example.globe.GlobeMod;
import com.example.globe.util.LatitudeMath;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;

import java.lang.ref.WeakReference;

/**
 * Latitude-owned runtime season, solar, climate, and effective-light bridge.
 *
 * Raw sky light remains an occlusion map; this class supplies the local solar
 * multiplier at the world query boundary.
 */
public final class LatitudeSeasonBridge {
    private static final ThreadLocal<QueryContext> LAST_BIOME_QUERY = new ThreadLocal<>();
    private static volatile boolean clientGlobeWorld;

    private LatitudeSeasonBridge() {
    }

    public record Snapshot(
            LatitudeCalendarMath.CalendarDate calendar,
            LatitudeCalendarMath.SolarPosition solar,
            double latitudeNorthPositive,
            double longitudeEastPositive,
            char hemisphere,
            boolean active
    ) {
    }

    private record QueryContext(WeakReference<World> world, long posLong) {
    }

    public static void setClientGlobeWorld(boolean globeWorld) {
        clientGlobeWorld = globeWorld;
    }

    public static boolean isLatitudeWorld(World world) {
        if (world == null || !World.OVERWORLD.equals(world.getRegistryKey())) {
            return false;
        }

        if (world instanceof ServerWorld serverWorld) {
            return GlobeMod.isGlobeOverworld(serverWorld);
        }

        return clientGlobeWorld;
    }

    public static Snapshot at(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return inactiveSnapshot();
        }
        return at(world, pos.getX(), pos.getZ());
    }

    public static Snapshot at(World world, double z) {
        return at(world, 0.0, z);
    }

    public static Snapshot at(World world, double x, double z) {
        if (world == null) {
            return inactiveSnapshot();
        }

        WorldBorder border = world.getWorldBorder();
        double offsetZ = z - border.getCenterZ();
        double latitude = -LatitudeMath.degreesFromZ(border, offsetZ);
        double longitude = longitudeDegrees(world, x);
        long calendarTicks = world.getTime();
        long timeOfDayTicks = world.getTimeOfDay();
        LatitudeCalendarMath.CalendarDate calendar = LatitudeCalendarMath.calendarAt(calendarTicks);
        LatitudeCalendarMath.SolarPosition solar = LatitudeCalendarMath.solarAt(calendarTicks, timeOfDayTicks, latitude, longitude);
        char hemisphere = latitude >= 0.0 ? 'N' : 'S';
        return new Snapshot(calendar, solar, latitude, longitude, hemisphere, isLatitudeWorld(world));
    }

    /** Maps the active world border's west/east extent to -180/+180 degrees. */
    public static double longitudeDegrees(World world, double x) {
        if (world == null) {
            return 0.0;
        }
        WorldBorder border = world.getWorldBorder();
        double half = Math.max(1.0, LatitudeMath.halfSize(border));
        double normalized = (x - border.getCenterX()) / half;
        return Math.max(-180.0, Math.min(180.0, normalized * 180.0));
    }

    public static double localSolarOffsetHours(World world, double x) {
        return longitudeDegrees(world, x) / 15.0;
    }

    public static LatitudeCalendarMath.CalendarDate localCalendarAt(World world, double x) {
        if (world == null) {
            return LatitudeCalendarMath.calendarAt(0L);
        }
        return LatitudeCalendarMath.calendarAt(world.getTime(), world.getTimeOfDay(), longitudeDegrees(world, x));
    }

    private static Snapshot inactiveSnapshot() {
        LatitudeCalendarMath.CalendarDate calendar = LatitudeCalendarMath.calendarAt(0L);
        LatitudeCalendarMath.SolarPosition solar = LatitudeCalendarMath.solarAt(0L, 0.0);
        return new Snapshot(calendar, solar, 0.0, 0.0, 'N', false);
    }

    public static void rememberBiomeQuery(World world, BlockPos pos) {
        if (world != null && pos != null && isLatitudeWorld(world)) {
            LAST_BIOME_QUERY.set(new QueryContext(new WeakReference<>(world), pos.asLong()));
        }
    }

    public static World contextWorldFor(BlockPos pos) {
        if (pos == null) {
            return null;
        }

        QueryContext context = LAST_BIOME_QUERY.get();
        if (context == null || context.posLong() != pos.asLong()) {
            return null;
        }

        World world = context.world().get();
        if (world == null || !isLatitudeWorld(world)) {
            return null;
        }
        return world;
    }

    public static float adjustedTemperature(World world, BlockPos pos, float baseTemperature) {
        if (!isLatitudeWorld(world)) {
            return baseTemperature;
        }

        Snapshot snapshot = at(world, pos);
        double solarDelta = snapshot.solar().solarHeatSignal()
                * (0.02 + 0.14 * snapshot.solar().seasonalStrength());

        // Humidity changes the felt temperature slightly at the equator; the
        // biome's own temperature remains the dominant climate input.
        double equatorialHumidityDelta = 0.0;
        if (Math.abs(snapshot.latitudeNorthPositive()) <= 18.0) {
            equatorialHumidityDelta = (snapshot.solar().equatorialWetness() - 0.5) * 0.01;
        }

        double adjusted = baseTemperature + solarDelta + equatorialHumidityDelta;
        return (float) Math.max(-1.0, Math.min(2.0, adjusted));
    }

    public static Biome.Precipitation precipitationFor(Biome biome, World world, BlockPos pos) {
        if (!biome.hasPrecipitation() || !isLatitudeWorld(world)) {
            return biome.hasPrecipitation() ? Biome.Precipitation.RAIN : Biome.Precipitation.NONE;
        }

        Snapshot snapshot = at(world, pos);
        if (Math.abs(snapshot.latitudeNorthPositive()) <= 18.0
                && snapshot.solar().equatorialWetness() < 0.23) {
            return Biome.Precipitation.NONE;
        }

        float temperature = adjustedTemperature(world, pos, biome.getTemperature());
        return temperature < 0.15f ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
    }

    public static boolean doesNotSnow(Biome biome, World world, BlockPos pos) {
        if (!biome.hasPrecipitation() || !isLatitudeWorld(world)) {
            return !biome.hasPrecipitation();
        }
        return adjustedTemperature(world, pos, biome.getTemperature()) >= 0.15f;
    }

    public static int effectiveSkyLight(World world, BlockPos pos, int storedSkyLight) {
        if (!isLatitudeWorld(world)) {
            return storedSkyLight;
        }
        Snapshot snapshot = at(world, pos);
        double scaled = storedSkyLight * gameplaySkyLightFactor(snapshot.solar());
        return clampLight((int) Math.round(scaled));
    }

    public static int effectiveCombinedLight(World world, BlockPos pos, int storedSkyLight, int storedBlockLight) {
        if (!isLatitudeWorld(world)) {
            return Math.max(storedSkyLight, storedBlockLight);
        }
        return Math.max(effectiveSkyLight(world, pos, storedSkyLight), storedBlockLight);
    }

    public static int effectiveBaseLight(World world, BlockPos pos, int storedSkyLight, int storedBlockLight, int ambientDarkness) {
        int effectiveSky = effectiveSkyLight(world, pos, storedSkyLight);
        int skyAfterAmbient = Math.max(0, effectiveSky - Math.max(0, ambientDarkness));
        return clampLight(Math.max(skyAfterAmbient, storedBlockLight));
    }

    public static float localSkyBrightness(World world, BlockPos pos) {
        if (!isLatitudeWorld(world)) {
            return 1.0f;
        }
        return (float) Math.max(0.0, Math.min(1.0, at(world, pos).solar().skyLightFactor()));
    }

    /**
     * Scales only the visible daytime sky after vanilla has applied biome,
     * rain, thunder, and lightning colors. Diffuse sky radiance falls more
     * gently than direct ground irradiance, so this is intentionally separate
     * from gameplaySkyLightFactor and the stored sky-light map.
     */
    public static double visualSkyColorFactor(World world, double x, double z) {
        if (!isLatitudeWorld(world)) {
            return 1.0;
        }

        LatitudeCalendarMath.SolarPosition solar = at(world, x, z).solar();
        double elevationDegrees = Math.toDegrees(solar.solarElevationRadians());
        if (elevationDegrees <= -6.0) {
            return 1.0;
        }

        double directSunFactor = Math.max(0.0, Math.min(1.0,
                (Math.sin(solar.solarElevationRadians()) + 0.08) / 1.08));
        return Math.max(0.0, Math.min(1.0, Math.pow(directSunFactor, 0.75)));
    }

    /**
     * Gameplay sky light stays full while the Sun is above the horizon. The
     * renderer may still use the continuous physical factor, but Minecraft's
     * discrete sky-light level must not turn ordinary morning into level 5.
     */
    public static double gameplaySkyLightFactor(LatitudeCalendarMath.SolarPosition solar) {
        double elevationDegrees = Math.toDegrees(solar.solarElevationRadians());
        if (solar.polarDay() || elevationDegrees >= 0.0) {
            return 1.0;
        }
        double twilight = Math.max(0.0, Math.min(1.0, (elevationDegrees + 6.0) / 6.0));
        double moonContribution = Math.min(0.18, Math.max(0.0, solar.skyLightFactor()));
        return Math.max(twilight, moonContribution);
    }

    /**
     * Remaps the vanilla one-axis sky orbit to the local sunrise/sunset window.
     * The stored chunk sky-occlusion map remains untouched.
     */
    public static float localSkyAngle(World world, double z) {
        return localSkyAngle(world, 0.0, z);
    }

    public static float localSkyAngle(World world, double x, double z) {
        if (!isLatitudeWorld(world)) {
            return vanillaSkyAngle(fraction(world.getTimeOfDay() / (double) LatitudeCalendarMath.TICKS_PER_DAY));
        }

        Snapshot snapshot = at(world, x, z);
        LatitudeCalendarMath.SolarPosition solar = snapshot.solar();
        double localDayProgress = solar.localDayProgress();
        double daylight = solar.daylightFraction();
        double vanillaProgress;

        if (solar.polarDay()) {
            vanillaProgress = 0.25;
        } else if (solar.polarNight()) {
            vanillaProgress = 0.75;
        } else {
            double sunrise = 0.25 - daylight * 0.5;
            double sunset = 0.25 + daylight * 0.5;
            if (localDayProgress >= sunrise && localDayProgress < sunset) {
                double localDay = (localDayProgress - sunrise) / daylight;
                vanillaProgress = localDay * 0.5;
            } else {
                double nightLength = 1.0 - daylight;
                double localNight = localDayProgress < sunrise
                        ? (localDayProgress + 1.0 - sunset) / nightLength
                        : (localDayProgress - sunset) / nightLength;
                vanillaProgress = 0.5 + localNight * 0.5;
            }
        }

        return vanillaSkyAngle(vanillaProgress);
    }

    public static int localMoonPhase(World world) {
        if (!isLatitudeWorld(world)) {
            return 0;
        }
        return at(world, 0.0).calendar().lunarPhase().ordinal();
    }

    private static float vanillaSkyAngle(double progress) {
        double d = fraction(progress - 0.25);
        double e = 0.5 - 0.5 * Math.cos(d * Math.PI);
        return (float) ((d * 2.0 + e) / 3.0);
    }

    private static double fraction(double value) {
        return value - Math.floor(value);
    }

    private static int clampLight(int value) {
        return Math.max(0, Math.min(15, value));
    }

    public static int localBiomeColor(World world, BlockPos pos, int baseColor, boolean foliage) {
        if (!isLatitudeWorld(world) || baseColor < 0) {
            return baseColor;
        }

        Snapshot snapshot = at(world, pos);
        double strength = snapshot.solar().seasonalStrength();
        if (strength <= 0.001) {
            return baseColor;
        }

        int target = switch (snapshot.solar().localSeason()) {
            case SPRING -> foliage ? 0x86B94F : 0x78AD50;
            case SUMMER -> foliage ? 0x429A43 : 0x4E9B45;
            case FALL -> foliage ? 0xC47A32 : 0xB88939;
            case WINTER -> foliage ? 0xA9B7A7 : 0xAAB5B2;
        };
        double tint = Math.min(0.30, 0.08 + strength * 0.22);
        return blendRgb(baseColor, target, tint);
    }

    private static int blendRgb(int base, int target, double amount) {
        int br = (base >> 16) & 0xFF;
        int bg = (base >> 8) & 0xFF;
        int bb = base & 0xFF;
        int tr = (target >> 16) & 0xFF;
        int tg = (target >> 8) & 0xFF;
        int tb = target & 0xFF;
        int r = (int) Math.round(br + (tr - br) * amount);
        int g = (int) Math.round(bg + (tg - bg) * amount);
        int b = (int) Math.round(bb + (tb - bb) * amount);
        return (r << 16) | (g << 8) | b;
    }

    /** Kept as a single semantic point for future raw-provider audits. */
    public static boolean isSkyLight(LightType type) {
        return type == LightType.SKY;
    }
}
