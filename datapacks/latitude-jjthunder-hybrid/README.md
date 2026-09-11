# Latitude + JJThunder hybrid worldgen

This pack is a committed, Latitude-specific merge built from JJThunder v0.6.0 data.

## Ownership contract

Latitude/Globe remains authoritative for:

- the `globe:globe_*` world presets and their size-specific settings IDs;
- Globe continent/distribution density functions;
- Latitude's runtime latitude biome selector;
- longitude/solar time, seasons, sky/light behavior, world border, and topology.

JJThunder contributes:

- the overworld dimension type's 2096-block height (`min_y=-64`, `logical_height=384`);
- its tall-world noise/final-density dependency graph;
- its biome JSON replacements;
- caves/carvers, configured and placed features, and ores.
- vanilla/Latitude structure resources remain untouched; JJThunder structure overrides are intentionally excluded because structures were not selected in this merge.

The five `globe:overworld_*` settings use JJThunder's tall graph but replace the router's
continent/depth/erosion/ridge fields with Globe functions. Their final density is the
intersection of JJThunder's tall final density and `globe:base_terrain`, so Globe's
land/ocean distribution constrains actual terrain rather than merely affecting biome
sampling.

This pack intentionally does not include JJThunder's `dimension/overworld.json` or its
`worldgen/noise_settings/overworld.json`; those would replace the Globe preset contract.
The original JJThunder ZIP in Downloads is not modified.

Create a fresh world after enabling this pack. Existing chunks cannot validate this
worldgen merge.
