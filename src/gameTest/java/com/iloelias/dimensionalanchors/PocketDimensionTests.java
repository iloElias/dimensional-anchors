package com.iloelias.dimensionalanchors;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;

final class PocketDimensionTests {
    static void dimension(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var id = Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, "pocket");
        var pocket = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        helper.assertTrue(pocket != null, "Shared pocket dimension was not loaded");
        var end = server.getLevel(Level.END);
        helper.assertTrue(end != null, "Vanilla End was not loaded");
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        // GameTestServer ignores standalone dimension entries; verify its test-only preset matches production.
        var dimensionResource = Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, "dimension/pocket.json");
        try (var reader = server.getResourceManager().getResourceOrThrow(dimensionResource).openAsReader()) {
            var production = LevelStem.CODEC.parse(ops, JsonParser.parseReader(reader)).getOrThrow();
            var loaded = new LevelStem(pocket.dimensionTypeRegistration(), pocket.getChunkSource().getGenerator());
            helper.assertTrue(LevelStem.CODEC.encodeStart(ops, loaded).getOrThrow().equals(
                    LevelStem.CODEC.encodeStart(ops, production).getOrThrow()),
                    "GameTest preset differs from the production dimension resource");
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read production pocket dimension", exception);
        }
        var actual = DimensionType.DIRECT_CODEC.encodeStart(ops, pocket.dimensionType())
                .getOrThrow().getAsJsonObject();
        var expected = DimensionType.DIRECT_CODEC.encodeStart(ops, end.dimensionType())
                .getOrThrow().getAsJsonObject();
        expected.addProperty("has_ender_dragon_fight", false);
        helper.assertTrue(actual.equals(expected), "Pocket dimension differs from End beyond disabled dragon fight");
        helper.assertTrue(pocket.getDragonFight() == null && end.getDragonFight() != null,
                "Pocket enabled a dragon fight or altered the real End");
        helper.assertTrue(pocket.getMinY() <= RoomGeometry.FLOOR_Y
                && pocket.getMaxY() >= new RoomGeometry(0, AnchorTier.NETHERITE).shell().maxY(),
                "Dimension height cannot contain the maximum room shell");
        helper.assertTrue(RoomAllocationSavedData.get(pocket) == RoomAllocationSavedData.get(server.overworld()),
                "Pocket dimension has an independent allocator");
        var generator = pocket.getChunkSource().getGenerator();
        helper.assertTrue(generator instanceof FlatLevelSource, "Pocket does not use the empty flat generator");
        if (generator instanceof FlatLevelSource flat) {
            helper.assertTrue(flat.settings().getLayersInfo().isEmpty()
                    && flat.settings().getLayers().isEmpty(), "Void generator contains terrain layers");
            helper.assertTrue(flat.settings().structureOverrides().orElseThrow().size() == 0,
                    "Pocket generator permits natural structures");
        }
        var biomeId = Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, "pocket_void");
        var biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
        var biomes = generator.getBiomeSource().possibleBiomes();
        helper.assertTrue(biomes.size() == 1 && biomes.iterator().next().is(biomeKey),
                "Pocket generator does not exclusively use the custom biome");
        var biome = biomes.iterator().next().value();
        helper.assertTrue(!biome.hasPrecipitation(), "Pocket biome permits precipitation");
        helper.assertTrue(!biome.getGenerationSettings().getCarvers().iterator().hasNext()
                && biome.getGenerationSettings().features().stream().allMatch(features -> features.size() == 0),
                "Pocket biome contains carvers or placed features");
        for (MobCategory category : MobCategory.values()) {
            helper.assertTrue(biome.getMobSettings().getMobs(category).isEmpty(),
                    "Pocket biome permits natural spawning for " + category);
        }
        for (int[] coordinates : new int[][]{{0, 0}, {1, 1}, {127, 127}}) {
            var chunk = pocket.getChunk(coordinates[0], coordinates[1]);
            for (var section : chunk.getSections()) {
                helper.assertTrue(section.hasOnlyAir(), "Generated pocket chunk contains non-air blocks");
            }
            helper.assertTrue(pocket.getBiome(new BlockPos(coordinates[0] * 16, 64, coordinates[1] * 16))
                    .is(biomeKey), "Generated chunk contains another biome");
            helper.assertTrue(chunk.getAllStarts().isEmpty(), "Generated chunk contains structure starts");
        }

    }
}
