package com.direwolf20.buildinggadgets2.client.renderer;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

import java.awt.*;

public class MyRenderMethods {
    private static float dummyU0 = 0F;
    private static float dummyU1 = 1F;
    private static float dummyV0 = 0F;
    private static float dummyV1 = 1F;

    public static void renderCopy(SubmitNodeCollector collector, PoseStack matrix, BlockPos startPos, BlockPos endPos, Color color) {
        if (startPos.equals(GadgetNBT.nullPos) || endPos.equals(GadgetNBT.nullPos))
            return;

        renderLines(collector, matrix, startPos, endPos, color);
    }

    public static void renderBoxSolid(Matrix4f matrix, VertexConsumer builder, BlockPos pos, float r, float g, float b, float alpha) {
        double x = pos.getX() - 0.001;
        double y = pos.getY() - 0.001;
        double z = pos.getZ() - 0.001;
        double xEnd = pos.getX() + 1.0015;
        double yEnd = pos.getY() + 1.0015;
        double zEnd = pos.getZ() + 1.0015;

        renderBoxSolid(matrix, builder, x, y, z, xEnd, yEnd, zEnd, r, g, b, alpha);
    }

    protected static void renderBoxSolid(Matrix4f matrix, VertexConsumer builder, double x, double y, double z, double xEnd, double yEnd, double zEnd, float red, float green, float blue, float alpha) {
        //careful: mc want's it's vertices to be defined CCW - if you do it the other way around weird cullling issues will arise
        //CCW herby counts as if you were looking at it from the outside
        float startX = (float) x;
        float startY = (float) y;
        float startZ = (float) z;
        float endX = (float) xEnd;
        float endY = (float) yEnd;
        float endZ = (float) zEnd;

        //down
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha);

        //up
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha);

        //east
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha);

        //west
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha);

        //south
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha);

        //north
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha);
    }

    public static void renderLines(SubmitNodeCollector collector, PoseStack matrix, BlockPos startPos, BlockPos endPos, Color color) {
        //We want to draw from the starting position to the (ending position)+1
        int x = Math.min(startPos.getX(), endPos.getX()), y = Math.min(startPos.getY(), endPos.getY()), z = Math.min(startPos.getZ(), endPos.getZ());

        int dx = (startPos.getX() > endPos.getX()) ? startPos.getX() + 1 : endPos.getX() + 1;
        int dy = (startPos.getY() > endPos.getY()) ? startPos.getY() + 1 : endPos.getY() + 1;
        int dz = (startPos.getZ() > endPos.getZ()) ? startPos.getZ() + 1 : endPos.getZ() + 1;

        int colorRGB = color.getRGB();
        // RenderTypes.lines() uses POSITION_COLOR_NORMAL_LINE_WIDTH — every vertex must carry a line width.
        final float lineWidth = 2.0F;

        collector.submitCustomGeometry(matrix, RenderTypes.lines(), (matrix3f, builder) -> {
            Matrix4f matrix4f = matrix3f.pose();

            builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F).setLineWidth(lineWidth);
            builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F).setLineWidth(lineWidth);

        });
    }

    //This one does not block water
    public static void renderBoxSolid(PoseStack.Pose pose, Matrix4f matrix, VertexConsumer builder, double x, double y, double z, double xEnd, double yEnd, double zEnd, float red, float green, float blue, float alpha) {
        //careful: mc want's it's vertices to be defined CCW - if you do it the other way around weird cullling issues will arise
        //CCW herby counts as if you were looking at it from the outside
        float startX = (float) x;
        float startY = (float) y;
        float startZ = (float) z;
        float endX = (float) xEnd;
        float endY = (float) yEnd;
        float endZ = (float) zEnd;

        //down
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);

        //up
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);

        //east
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);

        //west
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);

        //south
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);

        //north
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0F, 0F, 1F);
    }

    public static void renderBoxSolid(PoseStack.Pose pose, Matrix4f matrix, VertexConsumer buffer, BlockPos pos, float r, float g, float b, float alpha) {
        double x = pos.getX() - 0.001;
        double y = pos.getY() - 0.001;
        double z = pos.getZ() - 0.001;
        double xEnd = pos.getX() + 1.0015;
        double yEnd = pos.getY() + 1.0015;
        double zEnd = pos.getZ() + 1.0015;

        renderBoxSolid(pose, matrix, buffer, x, y, z, xEnd, yEnd, zEnd, r, g, b, alpha);
    }

    public static void renderFaceSolid(PoseStack.Pose pose, Matrix4f matrix, VertexConsumer buffer, BlockPos pos, Direction direction, float r, float g, float b, float alpha) {
        double x = pos.getX() - 0.001;
        double y = pos.getY() - 0.001;
        double z = pos.getZ() - 0.001;
        double xEnd = pos.getX() + 1.0015;
        double yEnd = pos.getY() + 1.0015;
        double zEnd = pos.getZ() + 1.0015;

        switch (direction) {
            case DOWN:
                // Draw on the bottom face (y = pos.getY())
                renderBoxSolid(pose, matrix, buffer, x, y - 0.001, z, xEnd, y, zEnd, r, g, b, alpha);
                break;
            case UP:
                // Draw on the top face (y = pos.getY() + 1)
                renderBoxSolid(pose, matrix, buffer, x, yEnd, z, xEnd, yEnd + 0.0015, zEnd, r, g, b, alpha);
                break;
            case NORTH:
                // Draw on the north face (z = pos.getZ())
                renderBoxSolid(pose, matrix, buffer, x, y, z - 0.001, xEnd, yEnd, z, r, g, b, alpha);
                break;
            case SOUTH:
                // Draw on the south face (z = pos.getZ() + 1)
                renderBoxSolid(pose, matrix, buffer, x, y, zEnd, xEnd, yEnd, zEnd + 0.0015, r, g, b, alpha);
                break;
            case WEST:
                // Draw on the west face (x = pos.getX())
                renderBoxSolid(pose, matrix, buffer, x - 0.001, y, z, x, yEnd, zEnd, r, g, b, alpha);
                break;
            case EAST:
                // Draw on the east face (x = pos.getX() + 1)
                renderBoxSolid(pose, matrix, buffer, xEnd, y, z, xEnd + 0.0015, yEnd, zEnd, r, g, b, alpha);
                break;
        }
    }
}
