# Circumnavigate 0.7 reference audit

## Scope

This is a read-only inspection of:

```text
C:\Users\alp\Downloads\circumnavigate-0.7+1.21.1-fabric.jar
```

No code, assets, mappings, or configuration were copied into Latitude. The JAR was not installed, added to TerrainTest, launched, or deployed.

Upstream metadata identifies the project as:

- Mod ID: `circumnavigate`
- Embedded mod version: `0.6.1`
- Filename/manifest implementation version: `0.7`
- Minecraft: `1.21.1`
- Fabric Loader: `>=0.18.6`
- Fabric API: `>=0.116.10+1.21.1`
- Java: `>=21`
- License: `AGPL-3.0`
- Project description: finite, tiled, seamless world wrapping
- Source: `https://github.com/FamroFexl/Circumnavigate`

The filename is therefore not a direct substitute for a Fabric 1.20.1 Latitude dependency. The AGPL license is another reason this report is reference-only.

## What the mod actually implements

### 1. A configurable periodic tile, not a globe

`DimensionWrappingSettings` stores independent chunk bounds for X and Z:

- `xChunkBoundMin` / `xChunkBoundMax`
- `zChunkBoundMin` / `zChunkBoundMax`
- an optional shift axis and shift amount
- a `useWrappedWorldGen` switch

`DimensionTransformer` creates separate X and Z coordinate transformers. The core transformer calculates a block-domain length from the configured bounds and remaps coordinates outside the finite interval back into that interval.

The bytecode implements negative-remainder correction after `%`, giving it positive wrapping semantics in practice. It does **not** call `Math.floorMod`. Latitude should use an explicit floor-mod helper or `Math.floorMod` for integral coordinates rather than copying this implementation.

The transformer also has an important second operation: it can choose the nearest periodic image of a coordinate relative to an anchor. That is used to make distances, AABBs, chunk ranges, and client-relative positions behave as though the opposite seam is nearby.

### 2. Its topology is toroidal/tiled

Because X and Z are independently periodic, the reference mod makes a finite tile whose opposite X edges and opposite Z edges are identified. That is a torus-like/tiled topology in gameplay terms.

There is no evidence of:

- a latitude clamp at `[-90°, +90°]`;
- a north-pole or south-pole singularity;
- longitude convergence at a pole;
- a 180-degree pole crossing transform;
- a north/south velocity or camera-frame rotation;
- a spherical or cubed-sphere face identifier;
- a `WorldRenderer` or `SkyRendering` celestial transform.

Therefore the pasted “hold Z at the pole, add 180 degrees to X, and rotate the player” advice is not something this JAR implements or proves. It is an atlas trick at best, not a complete spherical topology.

### 3. World generation is made periodic separately

The JAR contains worldgen mixins for `ImprovedNoise`, `NormalNoise`, `BlendedNoise`, Perlin/simplex noise, density functions, and surface processing.

The inspected `ImprovedNoiseMixin` maps normalized X and Z coordinates onto sine/cosine pairs and samples a four-dimensional OpenSimplex-style fallback. The inspected `BlendedNoiseMixin` also performs periodic noise work when wrapped worldgen is enabled. This is the technique that makes generated terrain repeat at both configured seams.

This is not a spherical projection. It makes the chosen rectangular tile repeat consistently. If Latitude eventually implements an X-only longitude loop, the general lesson is that every terrain/noise input that crosses the seam must be made periodic; remapping entity positions alone is insufficient.

### 4. Gameplay integration is broad

The mixin configuration shows that the mod does not rely on a single boundary teleport. It patches multiple engine layers, including:

- entity positions and movement deltas;
- living-entity distances;
- player and server-player behavior;
- entity and block collision queries;
- AABBs, hit results, and block positions;
- chunk maps, chunk tracking, chunk tickets, and server chunk cache;
- player chunk sending and packet handling;
- server-side coordinate broadcasts;
- path/navigation and game-event distance handling;
- rails, pistons, portals, fluids, explosions, vibrations, and other block mechanics;
- sky/block light engine paths;
- wrapped worldgen and surface processing;
- client view-distance and section-occlusion checks.

The design pattern worth retaining is the separation between:

1. a canonical wrapped/server coordinate;
2. an unwrapped coordinate selected relative to an observer or query anchor;
3. explicit conversions for blocks, vectors, AABBs, chunks, packets, and distances.

The broad patch surface is also a warning: a physical seamless loop is an engine integration project, not just a player teleport mixin.

## What this means for Latitude

### The reference does not solve the Sun problem

Its mixin list has no `WorldRenderer` or `SkyRendering` patch. It should not be used as evidence for the seasonal north/south Sun-declination fix. Latitude still needs its own 1.20.1 mapping-level renderer investigation for the celestial matrix path.

The earlier conclusion remains valid: a logical `getSkyAngle` change can alter daily phase but cannot by itself add seasonal declination to the renderer's celestial basis. The additional matrix transform must be designed against the actual 1.20.1 draw order and coordinate handedness, then verified in-game.

### Do not import its Z wrapping

Independent Z wrapping would make the geographic north and south extremes adjacent. That would turn the Latitude world into a torus and would make a northern polar location connect directly to a southern polar location. It is incompatible with the current geographic contract.

For a first physical loop, Latitude should wrap X only and keep Z bounded by the declared latitude domain. A hard north/south boundary is honest for that stage. It is not the final answer to walking over a pole, but it does not silently create physically wrong polar adjacency.

### The `%` warning is valid, but the reference is not the implementation standard

The downloaded mod's integer code visibly uses `%` followed by a negative-result correction. That is semantically different from raw Java `%`, but it is still not the requested project convention. Latitude's reusable coordinate helper should use floor-modulo semantics explicitly, use a half-open interval, and have tests for negative values and exact seam boundaries.

## Corrected 1 -> 2 -> 3 architecture

### Stage 1: Geographic globe model, no physical terrain remap

Keep the logical geographic model independent from Minecraft's raw planar block coordinates:

- latitude is north-positive and bounded;
- longitude is periodic and half-open;
- longitude controls continuous local solar time and date rollover;
- solar declination comes from season and axial tilt;
- solar elevation and azimuth come from observer latitude, declination, and local hour angle;
- polar day/night is an illumination state, not a frozen world clock.

If `(x=0, z=0)` must be the north-pole center, that requirement changes the coordinate contract. A global linear mapping in which one axis is latitude cannot also make one planar point represent the entire north pole without a singularity. The choices are:

- a latitude/longitude rectangular atlas: simple, but each pole is a collapsed longitude edge rather than a unique ordinary grid point;
- an azimuthal/polar projection: the north pole can be the center, but scale varies with distance and the opposite pole becomes a boundary singularity;
- a cubed-sphere atlas: the north face can have its center at `(0,0)`, but a face ID and edge-neighbor transforms are required in addition to ordinary X/Z.

For production Latitude, this should first be represented as an explicit logical `GlobeCoordinate`/topology design, not smuggled into the existing block coordinate mapping. No source implementation is authorized by this audit.

### Stage 2: Physical east/west loop

Implement only an X/longitude loop after Stage 1 is stable:

- choose one exact circumference in blocks;
- use a central coordinate transformer with wrapped and observer-relative-unwrapped forms;
- use floor-modulo semantics for negative X and chunk coordinates;
- keep Z bounded and do not wrap it;
- make worldgen periodic in X if newly generated terrain must match at the seam;
- add seam-aware chunk loading before claiming seamless travel;
- then add controlled support for players, entities, collisions, block interaction, packets, and save/reload.

Circumnavigate's nearest-image distance logic, AABB splitting, observer-relative unwrapping, chunk tracking, and packet separation are useful architectural references. Its independent Z transformer and AGPL code are not to be copied.

This stage is a cylinder-like physical surface, intentionally not a complete sphere. It is still valuable because it validates the coordinate and chunk infrastructure without introducing a pole singularity at the same time.

### Stage 3: Experimental spherical topology

A genuine “walk over the north pole and continue south on the opposite longitude” behavior needs more than a polar teleport. The viable research direction is a cubed-sphere or another multi-face atlas:

- represent a location as `(face, u, v)` or an equivalent face-aware atlas coordinate;
- map each face to a normal Minecraft chunk grid;
- define all six face-edge neighbor transforms, including coordinate reversal and tangent-frame rotation;
- make the north face center the designated north-pole point if that is the desired command/world-origin convention;
- derive latitude, longitude, solar geometry, climate, and biome sampling from the face-aware globe coordinate;
- rotate movement direction, view orientation, and velocity through a face transition;
- make chunk keys, block lookups, collisions, structures, lighting, heightmaps, pathfinding, redstone, portals, and block entities face-aware;
- make terrain generation agree across every face edge and around both poles;
- preserve a planar fallback and isolate the experiment from normal Latitude worlds.

A “polar flip” can be a temporary compatibility experiment for a limited walking route, but it must not be called a perfect sphere. At the exact pole, longitude is undefined, so choosing `longitude + 180°` is only one arbitrary atlas representative. The physically meaningful state is the new tangent direction and face/longitude reached after crossing the pole.

A true perfect loop also requires deciding what “straight” means. On a sphere, a straight surface path is a great-circle direction, and a complete circuit returns to the starting geographic point. On a block atlas, the path must be transported across seams with a rotated tangent frame; a raw X/Z modulo or a one-time teleport cannot provide that guarantee.

## Bottom line

Circumnavigate confirms that a seamless finite loop needs a shared coordinate-transform layer plus broad chunk/entity/packet/query integration. It does **not** provide a spherical Earth, pole crossing, seasonal sky rendering, or a safe recipe for Latitude's Z axis.

The safe conclusion is:

1. finish and verify Latitude's logical globe/solar contract;
2. prototype an X-only physical loop with no Z wrapping;
3. research a separate face-aware spherical experiment for pole traversal and a north-pole-centered origin.

No implementation changes were made from this reference audit.
