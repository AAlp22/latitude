#!/usr/bin/env bash
# Deploy Latitude (current HEAD) + Eco 2.4.6 to TerrainTest, and retire the abandoned
# JJThunder hybrid pack. No Latitude source changes in this step: baseline first.
set -euo pipefail

R='C:/Users/alp/Desktop/Latitude-1.20.1-build'
R_WIN="$(cygpath -m "$R")"
ECO_SRC="$R/.hermes/rollback/terrain-test-20260911-before-eco-removal/eco-1.20.x-2.4.6.jar"
ECO_URL='https://cdn.modrinth.com/data/WtcuxDNN/versions/kgiB4umm/eco-1.20.x-2.4.6.jar'
INST='C:/Users/alp/AppData/Roaming/PrismLauncher/instances/TerrainTest'
MC="$INST/minecraft"
MODS="$MC/mods"
LIB="$MC/datapacks"
SRC="$R/.hermes/mods-source"
RB="$R/.hermes/rollback/terrain-test-$(date +%Y%m%d-%H%M%S)-lat-eco-baseline"

echo '=== stage a fresh Eco copy from Modrinth ==='
mkdir -p "$SRC"
curl -sL --max-time 120 -o "$SRC/eco-1.20.x-2.4.6.jar" "$ECO_URL"
test -s "$SRC/eco-1.20.x-2.4.6.jar" || { echo 'ABORT: eco download empty'; exit 2; }
unzip -t "$SRC/eco-1.20.x-2.4.6.jar" >/dev/null || { echo 'ABORT: eco zip invalid'; exit 3; }
id="$(unzip -p "$SRC/eco-1.20.x-2.4.6.jar" fabric.mod.json | jq -r '.id')"
ver="$(unzip -p "$SRC/eco-1.20.x-2.4.6.jar" fabric.mod.json | jq -r '.version')"
mc_rng="$(unzip -p "$SRC/eco-1.20.x-2.4.6.jar" fabric.mod.json | jq -r '.depends.minecraft')"
echo "eco_id=$id eco_version=$ver eco_minecraft=$mc_rng"
new_size="$(stat -c %s "$SRC/eco-1.20.x-2.4.6.jar")"
old_size="$(stat -c %s "$ECO_SRC")"
echo "fresh_size=$new_size rollback_size=$old_size"
[ "$new_size" = "$old_size" ] && echo 'fresh_copy_matches_previous=yes' || echo 'fresh_copy_matches_previous=NO (different bytes)'

echo '=== latitude state ==='
echo "branch=$(git -C "$R_WIN" branch --show-current)"
echo "commit=$(git -C "$R_WIN" rev-parse --short HEAD)"
test -z "$(git -C "$R_WIN" diff --name-only HEAD)" && echo 'tracked_worktree=clean'
LAT_JAR="$R/build/libs/latitude-1.3.0+1.20.1-r1.jar"
test -f "$LAT_JAR" || { echo 'ABORT: latitude build output missing'; exit 4; }

echo '=== install ==='
tasklist.exe 2>/dev/null | grep -Ei '^java(w)?\.exe' >/dev/null && { echo 'ABORT: Java/Minecraft running'; exit 5; }
mkdir -p "$RB" "$MODS" "$LIB"

# Retire the JJThunder detour: its pack and the Latitude jar currently live there.
for stale in "$LIB"/latitude-jjthunder-hybrid-*.zip "$LIB"/jjthunder-*.zip; do
  [ -e "$stale" ] || continue
  mv -f "$stale" "$RB/"
  echo "retired=$(basename "$stale")"
done
for stale in "$MODS"/latitude-*.jar; do
  [ -e "$stale" ] || continue
  mv -f "$stale" "$RB/"
  echo "preserved=$(basename "$stale")"
done

cp -p "$LAT_JAR" "$MODS/.latitude-installing.jar"
mv -f "$MODS/.latitude-installing.jar" "$MODS/latitude-1.3.0+1.20.1-r1.jar"
cmp -s "$LAT_JAR" "$MODS/latitude-1.3.0+1.20.1-r1.jar" || { echo 'ABORT: latitude byte mismatch'; exit 6; }
echo 'latitude_build_live_match=ok'

cp -p "$SRC/eco-1.20.x-2.4.6.jar" "$MODS/.eco-installing.jar"
mv -f "$MODS/.eco-installing.jar" "$MODS/eco-1.20.x-2.4.6.jar"
cmp -s "$SRC/eco-1.20.x-2.4.6.jar" "$MODS/eco-1.20.x-2.4.6.jar" || { echo 'ABORT: eco byte mismatch'; exit 7; }
echo 'eco_staged_live_match=ok'

echo '=== verify ==='
echo "latitude_commit=$(git -C "$R_WIN" rev-parse --short HEAD)"
echo '-- mods --'
find "$MODS" -mindepth 1 -maxdepth 1 -printf '  %f\n' | sort
echo '-- instance datapacks --'
find "$LIB" -mindepth 1 -maxdepth 1 -printf '  %f\n' | sort || true
compgen -G "$MODS/*.installing*" >/dev/null && { echo 'ABORT: temp installer present'; exit 8; }
if find "$LIB" -name '*jjthunder*' | grep . >/dev/null; then echo 'ABORT: jjthunder pack still present'; exit 9; fi
echo 'jjthunder_packs=none'
echo 'rollback_dir='"$RB"
echo 'minecraft_launch=not_performed'
