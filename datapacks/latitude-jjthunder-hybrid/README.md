# Latitude + JJThunder hybrid worldgen retry

This pack is a second, bounded merge built from JJThunder v0.6.0 data after the first hybrid produced malformed terrain.

## Ownership contract

Latitude/Globe remains authoritative for:

- the `globe:globe_*` world presets and size-specific settings IDs;
- Globe continent/distribution sampling;
- Latitude's runtime biome selector;
- longitude/solar time, seasons, sky/light behavior, world border, and topology.

JJThunder contributes:

- the overworld dimension type's 2096-block height (`min_y=-64`, `logical_height=384`);
- its tall-world final-density graph;
- its biome JSON replacements;
- caves/carvers, configured and placed features, and ores.

The retry leaves JJ's `minecraft:new_combination/interpolated` as the active terrain density on land. A separate `globe:hybrid/final_density` function applies only a bounded sea-level cap to columns whose Globe continentalness signal is in the Globe ocean range. It does not intersect JJ density with Globe's full vertical `base_terrain` function.

The `minecraft:continentalness` density function is exposed through the Globe continent signal so JJ's biome climate graph and the active terrain ocean mask use the same large-scale distribution source.

JJ's default overworld dimension/generator, default overworld noise-settings ID, and structure/structure-set overrides are intentionally excluded. Vanilla/Latitude structures remain untouched.

Create a fresh world after enabling this pack. Existing chunks cannot validate this merge.
