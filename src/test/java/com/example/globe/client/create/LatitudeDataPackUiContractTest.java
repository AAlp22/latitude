package com.example.globe.client.create;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatitudeDataPackUiContractTest {
    private static final Path SCREEN = Path.of(
            "src/main/java/com/example/globe/client/create/LatitudeCreateWorldScreen.java");

    @Test
    void rulesRailExposesTheDataPackControlInBothLayoutModes() throws IOException {
        String source = Files.readString(SCREEN);

        assertTrue(source.contains("private ButtonWidget dataPacksBtn;"));
        assertTrue(source.contains("private int dataPacksRowY;"));
        assertTrue(source.contains("ButtonWidget.builder(Text.literal(\"Data Packs...\")"));
        assertTrue(source.contains("settingsContentHeight = blockHeight * 6 + rowGap * 5;"));
        assertTrue(source.contains("drawSettingsRowLabel(context, \"Data Packs\", settLabelX, dataPacksRowY, MUTED);"));
        assertTrue(source.contains("setTabbedWidgetVisible(dataPacksBtn, showRules);"));
    }

    @Test
    void dataPackButtonUsesTheVanillaPickerAndReloadsRegistries() throws IOException {
        String source = Files.readString(SCREEN);

        assertTrue(source.contains("private void openDataPacks()"));
        assertTrue(source.contains("new PackScreen("));
        assertTrue(source.contains("new DataPackSettings("));
        assertTrue(source.contains("SaveLoading.load("));
        assertTrue(source.contains("this.holder = reloadedHolder;"));
        assertFalse(source.contains("private final GeneratorOptionsHolder holder;"));
    }

    @Test
    void instanceDataPackLibraryFeedsAvailableSideOfNativePicker() throws IOException {
        String source = Files.readString(SCREEN);

        assertTrue(source.contains("getInstanceDataPackDir"));
        assertTrue(source.contains("getSavesDirectory().getParent().resolve(\"datapacks\")"));
        assertTrue(source.contains("seedDataPackLibrary(tempDir)"));
        assertTrue(source.contains("Files.list(libraryDir)"));
        assertTrue(source.contains("copyDataPack(libraryDir, tempDir, pack)"));
        assertTrue(source.contains("Util.relativeCopy(source, destination, dataPack)"));
    }

    @Test
    void selectedPacksAreCopiedIntoTheNewSaveBeforeServerStart() throws IOException {
        String screen = Files.readString(SCREEN);
        String launcher = Files.readString(Path.of(
                "src/main/java/com/example/globe/client/create/LatitudeWorldLauncher.java"));

        assertTrue(screen.contains("copyDataPacksToSession"));
        assertTrue(screen.contains("WorldSavePath.DATAPACKS"));
        assertTrue(screen.contains("Util.relativeCopy"));
        assertTrue(launcher.contains("copyDataPacksToSession(session)"));
        assertTrue(launcher.indexOf("copyDataPacksToSession(session)") < launcher.indexOf("start(session"));
    }
}
