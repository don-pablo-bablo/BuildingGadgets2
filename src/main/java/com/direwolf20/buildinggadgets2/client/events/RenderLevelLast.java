package com.direwolf20.buildinggadgets2.client.events;

import com.direwolf20.buildinggadgets2.client.renderer.DestructionRenderer;
import com.direwolf20.buildinggadgets2.client.renderer.MyRenderMethods;
import com.direwolf20.buildinggadgets2.client.renderer.OurRenderTypes;
import com.direwolf20.buildinggadgets2.client.renderer.VBORenderer;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4fStack;

import java.awt.*;
import java.util.Optional;
import java.util.OptionalDouble;

public class RenderLevelLast {
    /**
     * Lines, boxes and overlays. As of 26.3 there is no immediate-mode buffer source, so these are
     * submitted as custom geometry alongside entities and block entities.
     */
    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent evt) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        ItemStack heldItem = BaseGadget.getGadget(player);
        if (heldItem.isEmpty())
            return;

        SubmitNodeCollector collector = evt.getSubmitNodeCollector();
        PoseStack poseStack = evt.getPoseStack();
        if (heldItem.getItem() instanceof GadgetDestruction) {
            DestructionRenderer.render(collector, poseStack, player, heldItem);
        } else {
            // No render pass is open yet at this point, so this is where the preview mesh gets uploaded/re-sorted.
            VBORenderer.prepareRender(player, heldItem);
            VBORenderer.submitOverlays(collector, poseStack, player, heldItem);
        }

        BlockPos anchorPos = GadgetNBT.getAnchorPos(heldItem);
        if (anchorPos != null && !anchorPos.equals(GadgetNBT.nullPos))
            renderSelectedBlock(collector, poseStack, anchorPos);
    }

    /**
     * Ghost-block preview with classic transparency. Only fires when improved transparency (OIT) is off;
     * the camera view rotation is already on the model-view stack here.
     */
    @SubscribeEvent
    static void onAfterTranslucentBlocks(RenderLevelStageEvent.AfterTranslucentBlocks evt) {
        Player player = Minecraft.getInstance().player;
        ItemStack heldItem = getPreviewGadget(player);
        if (heldItem.isEmpty())
            return;

        VBORenderer.drawRender(evt.getRenderPass(), player, heldItem);
    }

    /**
     * Ghost-block preview with improved transparency. Translucent stages go through the OIT passes then,
     * so draw into the main target once the level is done instead. The level renderer has already popped
     * the camera view rotation by this point, so re-apply it.
     */
    @SubscribeEvent
    static void onAfterLevel(RenderLevelStageEvent.AfterLevel evt) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.gameRenderer.useImprovedTransparency())
            return;

        Player player = mc.player;
        ItemStack heldItem = getPreviewGadget(player);
        if (heldItem.isEmpty())
            return;

        RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(evt.getModelViewMatrix());
        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "BG2 preview", mainTarget.getColorTextureView(), Optional.empty(), mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            VBORenderer.drawRender(pass, player, heldItem);
        } finally {
            modelViewStack.popMatrix();
        }
    }

    private static ItemStack getPreviewGadget(Player player) {
        if (player == null)
            return ItemStack.EMPTY;

        ItemStack heldItem = BaseGadget.getGadget(player);
        if (heldItem.getItem() instanceof GadgetDestruction)
            return ItemStack.EMPTY;
        return heldItem;
    }

    public static void renderSelectedBlock(SubmitNodeCollector collector, PoseStack matrix, BlockPos pos) {
        final Minecraft mc = Minecraft.getInstance();

        Vec3 view = mc.gameRenderer.mainCamera().position();

        matrix.pushPose();
        matrix.translate(-view.x(), -view.y(), -view.z());

        matrix.pushPose();
        matrix.translate(pos.getX(), pos.getY(), pos.getZ());
        matrix.translate(-0.005f, -0.005f, -0.005f);
        matrix.scale(1.01f, 1.01f, 1.01f);
        //matrix.mulPose(Axis.YP.rotationDegrees(-90.0F));

        collector.submitCustomGeometry(matrix, OurRenderTypes.TRANSPARENT_BOX, (pose, buffer) ->
                MyRenderMethods.renderBoxSolid(pose, pose.pose(), buffer, BlockPos.ZERO, 0, 1, 0, 0.25f));
        //MyRenderMethods.renderFaceSolid(pose, pose.pose(), buffer, BlockPos.ZERO, direction, 0, 0, 1, 0.25f);
        MyRenderMethods.renderLines(collector, matrix, BlockPos.ZERO, BlockPos.ZERO, Color.WHITE);
        matrix.popPose();

        matrix.popPose();
    }
}
