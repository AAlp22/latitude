package com.example.globe.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenerationSettings.class)
public abstract class GenerationSettingsEcoFeatureFilterMixin {
    private static final String ECO_NAMESPACE = "eco";

    @Mutable
    @Shadow
    @Final
    private List<RegistryEntryList<PlacedFeature>> features;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void globe$stripEcoPlacedFeatures(Map<?, ?> carvers,
                                               List<?> configuredFeatures,
                                               CallbackInfo ci) {
        if (Boolean.getBoolean("latitude.disableFeatureStripping")) {
            return;
        }

        List<RegistryEntryList<PlacedFeature>> filtered = new ArrayList<>(features.size());
        for (RegistryEntryList<PlacedFeature> step : features) {
            List<RegistryEntry<PlacedFeature>> filteredEntries = new ArrayList<>(step.size());
            step.stream()
                    .filter(entry -> !globe$isEcoFeature(entry))
                    .forEach(filteredEntries::add);
            filtered.add(filteredEntries.size() == step.size()
                    ? step
                    : RegistryEntryList.of(filteredEntries));
        }
        features = filtered;
    }

    private static boolean globe$isEcoFeature(RegistryEntry<PlacedFeature> entry) {
        Optional<RegistryKey<PlacedFeature>> key = entry.getKey();
        return key.map(RegistryKey::getValue)
                .map(Identifier::getNamespace)
                .filter(ECO_NAMESPACE::equals)
                .isPresent();
    }
}
