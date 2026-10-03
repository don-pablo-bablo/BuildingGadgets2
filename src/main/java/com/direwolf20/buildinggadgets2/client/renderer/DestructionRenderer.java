package com.direwolf20.buildinggadgets2.client.renderer;

import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.GadgetUtils;
import com.direwolf20.buildinggadgets2.util.VectorHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;


public class DestructionRenderer {
    public static void render(SubmitNodeCollector collector, PoseStack stack, Player player, ItemStack gadget) {
        //if (!GadgetDestruction.getOverlay(gadget)) //TODO
        //    return;

        BlockHitResult lookingAt = VectorHelper.getLookingAt(player, gadget);
        Level level = player.level();
        BlockPos anchor = GadgetNBT.getAnchorPos(gadget);
        Direction anchorSide = GadgetNBT.getAnchorSide(gadget);

        if (level.getBlockState(VectorHelper.getLookingAt(player, gadget).getBlockPos()) == Blocks.AIR.defaultBlockState() && anchor == null)
            return;

        BlockPos startBlock = (anchor == GadgetNBT.nullPos) ? lookingAt.getBlockPos() : anchor;
        Direction facing = (anchorSide == null) ? lookingAt.getDirection() : anchorSide;
        if (level.getBlockState(startBlock) == Registration.RenderBlock.get().defaultBlockState())
            return;

        Vec3 playerPos = Minecraft.getInstance().gameRenderer.mainCamera().position();

        final int[] counter = {BuildingUtils.getEnergyStored(gadget)};
        final int energyCost = BuildingUtils.getEnergyCost(gadget);
        List<BlockPos> affected = new ArrayList<>();
        //Todo More Efficient for more FPS, consider a VBO?
        GadgetUtils.getDestructionArea(level, startBlock, facing, player, gadget)
                .forEach(pos -> {
                    if (counter[0] >= energyCost || player.isCreative())
                        affected.add(pos.pos);
                    counter[0] -= energyCost;
                });
        if (affected.isEmpty())
            return;

        stack.pushPose();
        stack.translate(-playerPos.x(), -playerPos.y(), -playerPos.z());
        collector.submitCustomGeometry(stack, OurRenderTypes.MissingBlockOverlay, (pose, builder) -> {
            for (BlockPos pos : affected)
                MyRenderMethods.renderBoxSolid(pose.pose(), builder, pos, 1, 0, 0, 0.35f);
        });
        stack.popPose();
    }
}
