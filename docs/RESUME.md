# Resume notes

## Where things are

TerrainTest (Latitude instance): Latitude `ee6e3c4` + Eco 2.4.6 + `eco-trim-ee6e3c4.zip` +
`latitude-dev-tools-f4d7aff.zip`. JJTest (now a generic isolated test instance): Fabric +
Fabric API + Geophilic 3.6 + the dev-tools datapack; Eco parked in
`.hermes/rollback/jjtest-eco-out/`.

## Parked decision (no code depends on it yet)

`ClimateProvider.resolve(biome, x, y, z)` has no time, so it cannot see Latitude's season.
Recommendation was a 5th arg `long worldTime` (`dayOfYear = (time / 24000) % 256` fully
determines the season). Nothing is built against either shape, so this is free to change.

## Next steps

1. Own biome content layer, seeded from Eco (MIT permits this with attribution). Eco is the
   only large data-only biome overhaul on 1.20.1 Fabric that is not ARR/ND.
2. Latitude side of the climate bridge: `compileOnly` ES: Core from Maven Local, a
   `GlobeClimateProvider` implementing the `es-climate-provider` entrypoint, declare it in
   Latitude's `fabric.mod.json`, then confirm `[ES] climate provider registered from globe`.
3. Verify whether Geophilic actually applied its biome data. It loads (`- geophilic 3.6`) but
   was reported as doing nothing; the jar is a multi-version bundle, so check which data set
   was applied rather than concluding the mod is weak.

## Findings worth not rediscovering

- Latitude's terrain bands are degrees: TROPICAL 0-23.5, SUBTROPICAL 23.5-35, TEMPERATE 35-50,
  SUBPOLAR 50-66.5, POLAR 66.5-90. `latitude = |z| / radius * 90`.
- Radius is hardcoded in THREE places that must move together: `GlobeWorldSize` (enum values +
  labels), `GlobeMod` (`BORDER_RADIUS` is the regular radius AND the fallback), and
  `ChunkGeneratorBiomeSourceMixin` (generator gate). `GlobePresetRadiusTest` guards this.
- `ice_spikes` was reachable from the SUBPOLAR pool (`lat_subpolar.json`) while all five
  selection sites in `LatitudeBiomes` gate it to POLAR. Tag was the bug.
- Frozen oceans are correctly polar-only (`lat_ocean_polar`, `ocean_frozen`, `ocean_deep_frozen`).
- Eco's coral comes from `minecraft:warm_ocean_vegetation` in 20 biomes (19 land + warm_ocean),
  not from its `cave_*_coral` features, which are water-scoped and inert on dry land.
- Cave vegetation everywhere is Eco's `lush_cave_vines` (`noise_based_count` ratio 110). If caves
  are still lush once removed, the owner is the `minecraft:lush_caves` biome itself.
- Vanilla underground inherits the surface biome (the `depth` axis is a Y gradient); only
  dripstone_caves/lush_caves/deep_dark have narrow enough ranges to win. So do NOT make
  Latitude return `base` underground unconditionally - it would desync the column and break
  per-biome cave content.
- ES: Geology's climate is static per biome (`GeologyClimateCatalog`), keyed biome -> profile,
  and it already carries `elevationLapseRateCPerKm`. Core's new `ClimateProviders` SPI is how
  Latitude overrides it per position.
- ES: Geology overrides vanilla ore/geode/blob features; Eco overrides no
  `configured_feature`/`placed_feature` at all, so those two never collided.
