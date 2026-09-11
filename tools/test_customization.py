import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java"
RES = ROOT / "src/main/resources"

def text(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

def main():
    checks = []
    client = text("src/main/java/com/example/globe/GlobeModClient.java")
    server = text("src/main/java/com/example/globe/GlobeMod.java")
    pending = text("src/main/java/com/example/globe/GlobePending.java")
    create = text("src/main/java/com/example/globe/client/create/LatitudeCreateWorldScreen.java")
    launcher = text("src/main/java/com/example/globe/client/create/LatitudeWorldLauncher.java")
    sizes = text("src/main/java/com/example/globe/client/GlobeWorldSize.java")
    mixins = json.loads(text("src/main/resources/globe.mixins.json"))
    biome_source_mixin = text("src/main/java/com/example/globe/mixin/ChunkGeneratorBiomeSourceMixin.java")
    latitude_biome_source = text("src/main/java/com/example/globe/world/LatitudeBiomeSource.java")
    latitude_biomes = text("src/main/java/com/example/globe/world/LatitudeBiomes.java")
    populate_biomes_mixin = text("src/main/java/com/example/globe/mixin/ChunkGeneratorPopulateBiomesMixin.java")

    checks += [
        ("no compass HUD registration", "CompassHud.init();" not in client),
        ("no compass keybind registration", "ClientKeybinds.init();" not in client),
        ("no first-join compass grant", "giveItemStack(new ItemStack(Items.COMPASS))" not in server),
        ("no compass pending state", "startWithCompass" not in pending + launcher + create + server),
        ("no HUD in-game mixin", "client.InGameHudMixin" not in mixins.get("client", []) and "client.InGameHudMixin" not in mixins.get("mixins", [])),
        ("no compass toggle mixin", "HandledScreenCompassToggleMixin" not in mixins.get("mixins", [])),
        ("biome source lookup is lazy", "this.biomeSource.getBiomes()" not in biome_source_mixin and "Supplier<Collection<RegistryEntry<Biome>>>" in latitude_biome_source),
        ("wrapped source preserves the original biome pool", "LatitudeBiomes.completeBiomePool" not in latitude_biome_source and "return biomes.get().stream();" in latitude_biome_source),
        ("live registry is published to Latitude", "LatitudeBiomes.setActiveBiomeRegistry" in populate_biomes_mixin),
        ("complete registry pool enumerates registry entries", "streamEntries()" in latitude_biomes),
        ("Biolith dimension-type handoff is preserved", "globe$canExposeWrappedBiomeSource()" in biome_source_mixin and "InterfaceBiomeSource" in biome_source_mixin and "biolith$getDimensionType" in biome_source_mixin),
        ("five requested sizes exist", all(value in sizes for value in [
            "25,000 x 25,000", "50,000 x 50,000", "100,000 x 100,000",
            "200,000 x 200,000", "400,000 x 400,000",
        ])),
        ("100k is the Regular default", "REGULAR" in sizes and "globe_regular" in sizes and "50000" in sizes),
        ("five server settings exist", all(value in server for value in [
            "GLOBE_SETTINGS_SMALL", "GLOBE_SETTINGS_MEDIUM", "GLOBE_SETTINGS_REGULAR",
            "GLOBE_SETTINGS_LARGE", "GLOBE_SETTINGS_MASSIVE",
        ])),
        ("old selectable presets removed", not any(value in sizes for value in ["ITTY_BITTY", "TINY", "COLOSSAL", "globe_colossal"])),
        ("five preset resources exist", all((RES / "data/globe/worldgen/world_preset" / f"globe_{name}.json").exists() for name in ["small", "medium", "regular", "large", "massive"])),
        ("five noise settings exist", all((RES / "data/globe/worldgen/noise_settings" / f"overworld_{name}.json").exists() for name in ["small", "medium", "regular", "large", "massive"])),
        ("old preset resources removed", not any((RES / "data/globe/worldgen/world_preset" / f"{name}.json").exists() for name in ["globe", "globe_xsmall", "globe_colossal"])),
        ("old noise resources removed", not any((RES / "data/globe/worldgen/noise_settings" / f"{name}.json").exists() for name in ["overworld", "overworld_xsmall", "overworld_colossal"])),
    ]

    nyctophobia = {
        "nyctophobia:ancient_dead_coral_reef",
        "nyctophobia:burnt_forest",
        "nyctophobia:deep_dark_forest",
        "nyctophobia:eroded_haunted_forest",
        "nyctophobia:haunted_forest",
        "nyctophobia:haunted_lakes",
        "nyctophobia:marshlands",
    }
    tagged = set()
    for p in (RES / "data/globe/tags/worldgen/biome").glob("lat_*.json"):
        for value in json.loads(p.read_text(encoding="utf-8")).get("values", []):
            ident = value if isinstance(value, str) else value.get("id")
            if ident and ident.startswith("nyctophobia:"):
                tagged.add(ident)
    checks.append(("all Nyctophobia biomes tagged", nyctophobia <= tagged))

    failed = [name for name, ok in checks if not ok]
    for name, ok in checks:
        print(("PASS" if ok else "FAIL") + " " + name)
    if failed:
        raise SystemExit("failed checks: " + ", ".join(failed))

if __name__ == "__main__":
    main()
