package com.direwolf20.buildinggadgets2.datagen;

import com.direwolf20.buildinggadgets2.BuildingGadgets2;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.BG2Tags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class BG2BlockTags extends BlockTagsProvider {

    public BG2BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BuildingGadgets2.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BG2Tags.BG2DENY)
                .add(Blocks.PISTON_HEAD.builtInRegistryHolder().key())
                .add(Blocks.BEDROCK.builtInRegistryHolder().key())
                .add(Blocks.END_PORTAL_FRAME.builtInRegistryHolder().key())
                .add(Blocks.CANDLE_CAKE.builtInRegistryHolder().key())
                .addTag(BlockTags.BEDS)
                .addTag(BlockTags.PORTALS)
                .addTag(BlockTags.DOORS);

        tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED)
                .add(Registration.RenderBlock.getKey());
    }

    @Override
    public String getName() {
        return "BuildingGadgets2 Block Tags";
    }
}
