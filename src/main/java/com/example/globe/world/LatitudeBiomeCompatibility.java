package com.example.globe.world;

public final class LatitudeBiomeCompatibility {
    private static final String ECO_FLOATING_RIVER_ID = "eco:floating_river";

    private LatitudeBiomeCompatibility() {
    }

    public static boolean shouldPreserveEcoFloatingRiver(String biomeId, boolean coldBand) {
        return !coldBand && ECO_FLOATING_RIVER_ID.equals(biomeId);
    }
}
