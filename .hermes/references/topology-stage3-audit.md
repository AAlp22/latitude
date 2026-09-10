# Stage 3 seam-support audit

## Scope

This is a static audit of the opt-in Latitude topology prototype on Fabric/Minecraft 1.20.1. Minecraft was not launched. The current supported behavior remains player-only seam correction.

## Verified current path

`ServerPlayNetworkHandlerLatitudeTopologyMixin` intercepts `onPlayerMove` only when the experimental longitude/pole system properties are enabled. It:

1. rejects unsafe player states, invalid coordinates, excessive jumps, and ambiguous simultaneous seams;
2. maps the small crossing through `LatitudeLongitudeMovePlanner` and `LatitudeWorldTopologyMapper`;
3. checks the mapped destination with `ServerPlayerEntity.doesNotCollide`;
4. calls `requestTeleport` with the mapped position and transformed pole heading;
5. calls `ServerChunkManager.updatePosition`;
6. restores the saved velocity and cancels the original movement callback.

Mapped 1.20.1 bytecode confirms `requestTeleport` calls `ServerPlayerEntity.updatePositionAndAngles` before queuing `PlayerPositionLookS2CPacket`. `ServerChunkManager.updatePosition` then delegates native chunk/entity watching updates through `ThreadedAnvilChunkStorage`.

## What native chunk refresh does and does not do

`ServerChunkManager.updatePosition` and `ThreadedAnvilChunkStorage.updatePosition` use the player’s ordinary `ChunkSectionPos` and `ChunkPos`. They send a native `ChunkRenderDistanceCenterS2CPacket`, move the player’s native watched-chunk window, and load/unload normal native chunk coordinates.

This is sufficient to refresh chunks after the player has already been teleported to the opposite native edge. It is not a seam representation: the client still sees a planar coordinate grid, and the two sides of the longitude seam are not simultaneously adjacent in client space.

A true seam-visible chunk layer would need coordinated handling for at least:

- chunk watch/unwatch and render-distance-center packets;
- `ChunkDataS2CPacket` chunk X/Z and light data;
- unload packets and client chunk storage;
- block updates, section updates, block-entity updates, and lighting updates;
- entity spawn, destroy, relative movement, teleport, and velocity packets;
- observer-relative distance/tracking decisions.

Patching only `updatePosition` or only `ChunkDataS2CPacket` would create inconsistent client state.

## Entity and collision boundary

Mapped bytecode shows entity tracking is also keyed by native chunk sections and native distance checks. `EntityPositionS2CPacket` carries native absolute X/Y/Z values. Non-player entities are not currently remapped, and the player hook explicitly rejects vehicles and passengers.

Hooking `Entity.move` for all entities is not a safe next increment: the movement method owns collision and world-border behavior, so a post-movement coordinate rewrite is too late, while a head rewrite would need to re-run collision against the mapped destination and preserve fall distance, velocity, passengers, portals, and entity-section registration. This should remain disabled until a supported entity list and runtime test world exist.

The current `doesNotCollide` destination check is appropriate for the player-only teleport, but it does not establish collision continuity across a seam for arbitrary entities or block access.

## Polar-cap illusion boundary

The folded atlas can reflect a player across a pole and shift the atlas longitude by 180 degrees. It cannot make the rectangular Minecraft chunk grid shrink circumferentially toward the pole. A fixed-width row remains a planar strip, and exact-pole longitude remains a canonical representative rather than a unique physical coordinate.

Therefore the current pole path is a bounded player traversal illusion, not a spherical terrain surface. A real polar cap requires a multi-face atlas or another explicit chunk topology; direct Z wrapping would make a torus-like topology and is not acceptable.

## Recommendation

Do not add broad entity/chunk/packet/collision mixins to the normal Latitude build yet. First manually validate the opt-in player-only path in a disposable fresh world. If it works, the next isolated experiment should target a packet/accounting design with a declared supported object set, not a global `Entity.move` hook. Keep all broader seam behavior behind a separate experimental mode and preserve the current bounded fallback.

Static build success, generated refmaps, and this audit do not prove runtime seam continuity.

## Pure seam-coordinate layer added

`LatitudeSeamCoordinateFrame` is now a production-side pure math helper with focused tests. It provides:

- centered half-open canonical X coordinates;
- nearest periodic entity/absolute-packet images relative to an observer;
- exact integer block-coordinate translations when the circumference is integral;
- whole-chunk translations only when both world-border boundaries are exact chunk boundaries;
- explicit rejection values for unaligned borders, non-integral block circumferences, and arithmetic overflow.

This is deliberately not registered as a packet or entity Mixin. It defines the coordinate contract that a future client/server transport layer would need without claiming that native Minecraft packets already support it.

For the configured centered globe sizes, the chunk-boundary audit is:

| Diameter | West/east boundary alignment | Whole-chunk frame |
|---:|---|---|
| 25,000 | boundaries at ±12,500; not 16-block aligned | rejected |
| 50,000 | boundaries at ±25,000; not 16-block aligned | rejected |
| 100,000 | boundaries at ±50,000; aligned | mathematically possible |
| 200,000 | boundaries at ±100,000; aligned | mathematically possible |
| 400,000 | boundaries at ±200,000; aligned | mathematically possible |

Even the aligned cases still require coordinated packet rewriting and client chunk-state ownership; alignment alone is not runtime seam support.
