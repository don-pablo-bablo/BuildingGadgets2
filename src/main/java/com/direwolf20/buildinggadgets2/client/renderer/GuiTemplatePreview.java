package com.direwolf20.buildinggadgets2.client.renderer;

import com.direwolf20.buildinggadgets2.util.FakeRenderingWorld;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * 3D template preview rendered into the Template Manager GUI panel.
 * <p>
 * Registered as a {@link PictureInPictureRenderer}, but as of 26.3 the parent's {@link #prepare} only lets
 * subclasses submit geometry, which it then draws with a hard-coded orthographic projection. That rules out
 * both the perspective camera and the retained GPU mesh this preview relies on, so {@link #prepare} is
 * overridden wholesale: we own a color+depth texture pair sized to the panel, draw the cached meshes into
 * it with a perspective projection, and queue the same GUI blit the parent would.
 * <p>
 * The retained-GPU-mesh bake/draw pattern from {@link VBORenderer} is replayed against a private
 * per-instance {@link LayerCache} — so the GUI preview cache is independent of VBORenderer's in-world
 * preview cache and the two never thrash each other when a player holds a loaded template and also aims a
 * gadget at the world.
 */
public class GuiTemplatePreview extends PictureInPictureRenderer<GuiTemplatePreview.State> {
    private static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();

    // Tracks whatever statePos list we last baked. Identity compare against the current state's
    // list is cheap and correct — the caller (TemplateManagerGUI) re-fetches from BG2DataClient
    // only on UUID change, so the reference is stable across non-template-change frames.
    private @Nullable ArrayList<StatePos> cachedList;
    private @Nullable UUID cachedUuid;
    private float cachedCenterX, cachedCenterY, cachedCenterZ;
    private float cachedRadius;

    private final Map<ChunkSectionLayer, LayerCache> layerCaches = new EnumMap<>(ChunkSectionLayer.class);

    // Scratch ring for sorted-index rebuilds. One per renderer instance; fine — pooled across frames.
    private final ByteBufferBuilder sortIndexScratch = new ByteBufferBuilder(131072);

    // Our own perspective projection and render targets; the parent's are private and ortho-only.
    private final ProjectionMatrixBuffer projectionMatrixBuffer = new ProjectionMatrixBuffer("BG2 template preview");
    private final Projection projection = new Projection();
    private @Nullable GpuTexture colorTexture;
    private @Nullable GpuTextureView colorTextureView;
    private @Nullable GpuTexture depthTexture;
    private @Nullable GpuTextureView depthTextureView;

    // Non-AO model renderer. Free-floating ghost blocks: don't cull against neighbors.
    private @Nullable ModelBlockRenderer modelBlockRenderer;


    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "bg2 template preview";
    }

    @Override
    public boolean canBeReusedFor(State state, int textureWidth, int textureHeight) {
        // Reuse whenever the texture dimensions match. The actual mesh cache lives on this renderer
        // instance and is keyed by state's template UUID, so if the template swapped we rebuild
        // inside renderToTexture — texture reuse is independent of mesh reuse.
        return super.canBeReusedFor(state, textureWidth, textureHeight);
    }

    @Override
    public void prepare(State state, GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
        int texW = (state.x1() - state.x0()) * guiScale;
        int texH = (state.y1() - state.y0()) * guiScale;
        if (texW <= 0 || texH <= 0) {
            return;
        }
        if (state.statePosList != cachedList || !java.util.Objects.equals(state.templateUuid, cachedUuid)) {
            rebuildCache(state);
        }
        prepareTextures(texW, texH);

        if (!layerCaches.isEmpty()) {
            // Perspective projection sized to the texture so the aspect ratio matches the GUI rect we blit into.
            RenderSystem.backupProjectionMatrix();
            projection.setupPerspective(0.05f, 1000.0f, 60.0f, texW, texH);
            RenderSystem.setProjectionMatrix(projectionMatrixBuffer.getBuffer(projection), ProjectionType.PERSPECTIVE);

            // Camera pulled back along -Z by (radius * 2.5 - zoom). Larger radius → camera further away.
            // Zoom is added directly — positive zoom pushes the camera forward, matching the old code.
            // Then rotate, and center the template on the origin so rotation pivots around its center.
            float cameraDistance = Math.max(1.0f, cachedRadius * 2.5f - state.zoom * 0.01f);
            Matrix4f modelView = new Matrix4f()
                    .translate(state.panX * 0.01f, -state.panY * 0.01f, -cameraDistance)
                    .rotateX(state.rotX * (float) Math.PI / 180.0f)
                    .rotateY(state.rotY * (float) Math.PI / 180.0f)
                    .translate(-cachedCenterX, -cachedCenterY, -cachedCenterZ);

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.identity().mul(modelView);

            try (RenderPass pass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "BG2 template preview", colorTextureView, Optional.empty(), depthTextureView, OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass);
                drawLayer(pass, ChunkSectionLayer.SOLID, OurRenderTypes.RenderBlock);
                drawLayer(pass, ChunkSectionLayer.CUTOUT, OurRenderTypes.RenderBlock);
                drawLayer(pass, ChunkSectionLayer.TRANSLUCENT, OurRenderTypes.RenderBlock);
            } finally {
                modelViewStack.popMatrix();
                RenderSystem.restoreProjectionMatrix();
            }
        }

        guiRenderState.addBlitToCurrentLayer(
                new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.singleTexture(colorTextureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                        state.pose(),
                        state.x0(),
                        state.y0(),
                        state.x1(),
                        state.y1(),
                        0.0F,
                        1.0F,
                        1.0F,
                        0.0F,
                        -1,
                        state.scissorArea(),
                        null
                )
        );
    }

    @Override
    protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        // Unused: prepare() is overridden and draws the retained meshes itself.
    }

    /**
     * (Re)creates the color+depth targets when the panel size changes, then clears them. Mirrors the
     * parent's private texture management.
     */
    private void prepareTextures(int width, int height) {
        if (colorTexture != null && (colorTexture.getWidth(0) != width || colorTexture.getHeight(0) != height)) {
            closeTextures();
        }

        GpuDevice device = RenderSystem.getDevice();
        if (colorTexture == null) {
            colorTexture = device.createTexture(() -> "UI " + getTextureLabel() + " texture", 13, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
            colorTextureView = device.createTextureView(colorTexture);
            GpuFormat depthFormat = Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTexture().getFormat();
            depthTexture = device.createTexture(() -> "UI " + getTextureLabel() + " depth texture", 9, depthFormat, width, height, 1, 1);
            depthTextureView = device.createTextureView(depthTexture);
        }

        device.createCommandEncoder().clearColorAndDepthTextures(colorTexture, GuiRenderer.CLEAR_COLOR, depthTexture, 0.0);
    }

    private void closeTextures() {
        if (colorTextureView != null) colorTextureView.close();
        if (colorTexture != null) colorTexture.close();
        if (depthTextureView != null) depthTextureView.close();
        if (depthTexture != null) depthTexture.close();
        colorTextureView = null;
        colorTexture = null;
        depthTextureView = null;
        depthTexture = null;
    }

    /**
     * Tesselate {@code state.statePosList} into per-layer {@link GpuBuffer}s. Mirrors
     * {@link VBORenderer#generateRender} but writes into this instance's private cache.
     */
    private void rebuildCache(State state) {
        clearCache();
        ArrayList<StatePos> list = state.statePosList;
        if (list == null || list.isEmpty()) {
            cachedList = list;
            cachedUuid = state.templateUuid;
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        if (modelBlockRenderer == null) {
            modelBlockRenderer = new ModelBlockRenderer(true, false, Minecraft.getInstance().getBlockColors());
        }

        // Compute bounding box so we can center the template on the origin at draw time.
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (StatePos sp : list) {
            BlockPos p = sp.pos;
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }
        cachedCenterX = (minX + maxX) * 0.5f + 0.5f;
        cachedCenterY = (minY + maxY) * 0.5f + 0.5f;
        cachedCenterZ = (minZ + maxZ) * 0.5f + 0.5f;
        float dx = maxX - minX + 1, dy = maxY - minY + 1, dz = maxZ - minZ + 1;
        cachedRadius = 0.5f * (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Fake world for neighbor-aware block models. renderOrigin is BlockPos.ZERO because we
        // already bake absolute positions into the vertices and fold the centering into the
        // per-frame model-view.
        BlockPos renderOrigin = BlockPos.ZERO;
        FakeRenderingWorld fake = new FakeRenderingWorld(level, list, renderOrigin);
        VBORenderer.FakeWorldTintAdapter levelAdapter = new VBORenderer.FakeWorldTintAdapter(fake, level);

        Map<ChunkSectionLayer, BufferBuilder> builders = new EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, ByteBufferBuilder> byteBuilders = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : LAYERS) {
            ByteBufferBuilder bb = new ByteBufferBuilder(layer.vertexFormat().getVertexSize() * 1024);
            byteBuilders.put(layer, bb);
            builders.put(layer, new BufferBuilder(bb, PrimitiveTopology.QUADS, layer.vertexFormat()));
        }

        final float alpha = 1.0f;
        final RandomSource random = RandomSource.create();

        for (StatePos pos : list) {
            BlockState renderState = fake.getBlockState(pos.pos);
            if (renderState.isAir()) continue;
            // Fluids: route through the translucent builder via the same alpha-stamping path as
            // VBORenderer. Fluids rarely appear in templates but keep the code symmetric.
            if (!renderState.getFluidState().isEmpty()) {
                PoseStack fluidMatrix = new PoseStack();
                fluidMatrix.translate(pos.pos.getX(), pos.pos.getY(), pos.pos.getZ());
                RenderFluidBlock.renderFluidBlock(
                        renderState, level, pos.pos.above(255), fluidMatrix,
                        builders.get(ChunkSectionLayer.TRANSLUCENT), false);
                continue;
            }

            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(renderState);
            long seed = renderState.getSeed(pos.pos);
            float px = pos.pos.getX();
            float py = pos.pos.getY();
            float pz = pos.pos.getZ();

            BlockQuadOutput out = (qx, qy, qz, quad, inst) -> {
                // Alpha is 1.0 here but we still route through ARGB.color so the call shape matches
                // VBORenderer and any future translucent tweak is one line away.
                inst.setColor(0, ARGB.color(alpha, inst.getColor(0)));
                inst.setColor(1, ARGB.color(alpha, inst.getColor(1)));
                inst.setColor(2, ARGB.color(alpha, inst.getColor(2)));
                inst.setColor(3, ARGB.color(alpha, inst.getColor(3)));
                ChunkSectionLayer layer = quad.materialInfo().layer();
                builders.get(layer).putBlockBakedQuad(qx, qy, qz, quad, inst);
            };

            try {
                modelBlockRenderer.tesselateBlock(out, px, py, pz, levelAdapter, pos.pos.above(255),
                        renderState, model, seed);
            } catch (Exception ignored) {
                // Some blocks (Create, etc.) throw during tesselation with a non-standard level;
                // swallow per-block so the whole preview still shows.
            }
        }

        // Build each layer's MeshData and upload vertices to persistent GpuBuffers.
        GpuDevice device = RenderSystem.getDevice();
        for (ChunkSectionLayer layer : LAYERS) {
            MeshData mesh = builders.get(layer).build();
            if (mesh == null) {
                byteBuilders.get(layer).close();
                continue;
            }
            LayerCache cache = new LayerCache();
            java.nio.ByteBuffer vtx = mesh.vertexBuffer();
            cache.vertexBuffer = device.createBuffer(
                    () -> "BG2 gui preview " + layer.name() + " VBO",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    vtx);
            cache.indexCount = mesh.drawState().indexCount();
            cache.autoIndexType = mesh.drawState().indexType();

            if (layer == ChunkSectionLayer.TRANSLUCENT) {
                // Sort once at bake time. Resort-per-frame from the GUI camera is overkill for
                // a 20-block template; a single sort from the template center is good enough.
                Vector3f sortOrigin = new Vector3f(cachedCenterX, cachedCenterY, cachedCenterZ);
                MeshData.SortState sortState = mesh.sortQuads(sortIndexScratch,
                        VertexSorting.byDistance(v -> sortOrigin.distanceSquared(v)));
                cache.sortState = sortState;
                java.nio.ByteBuffer sortedIdx = mesh.indexBuffer();
                if (sortedIdx != null) {
                    cache.sortedIndexBuffer = device.createBuffer(
                            () -> "BG2 gui preview " + layer.name() + " IBO",
                            GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                            sortedIdx);
                }
            }
            mesh.close();
            byteBuilders.get(layer).close();
            layerCaches.put(layer, cache);
        }

        cachedList = list;
        cachedUuid = state.templateUuid;
    }

    private void drawLayer(RenderPass pass, ChunkSectionLayer layer, RenderType bg2Type) {
        LayerCache cache = layerCaches.get(layer);
        if (cache == null || cache.vertexBuffer == null || cache.indexCount == 0) return;

        // No scissor: the off-screen target is our whole drawable area, and the GUI-rect scissor is
        // applied by the blit afterwards. A render-type scissor would clip in main-window coordinates.
        VBORenderer.drawRetainedMesh(pass, "BG2 gui preview draw " + layer.name(), bg2Type, cache.vertexBuffer,
                cache.indexCount, cache.sortedIndexBuffer, cache.autoIndexType, false);
    }

    private void clearCache() {
        for (LayerCache c : layerCaches.values()) c.close();
        layerCaches.clear();
    }

    @Override
    public void close() {
        clearCache();
        closeTextures();
        sortIndexScratch.close();
        projectionMatrixBuffer.close();
        super.close();
    }

    /**
     * Per-instance retained GPU mesh for one chunk-section layer. Same shape as
     * {@link VBORenderer}'s inner {@code LayerCache} but kept separate so the two don't share state.
     */
    private static final class LayerCache implements AutoCloseable {
        GpuBuffer vertexBuffer;
        int indexCount;
        IndexType autoIndexType;
        GpuBuffer sortedIndexBuffer;
        MeshData.SortState sortState;

        @Override
        public void close() {
            if (vertexBuffer != null) {
                vertexBuffer.close();
                vertexBuffer = null;
            }
            if (sortedIndexBuffer != null) {
                sortedIndexBuffer.close();
                sortedIndexBuffer = null;
            }
            sortState = null;
            indexCount = 0;
        }
    }

    /**
     * PiP render state carrying the panel rect, the user's current rotate/zoom/pan inputs, and
     * the statePos list to draw. The {@link PictureInPictureRenderState} contract wants x0/y0/x1/y1
     * in GUI coordinates plus a scale factor; we fold guiScale into the state so the renderer can
     * see it without a static dependency on {@link Minecraft#getInstance}.
     */
    public static final class State implements PictureInPictureRenderState {
        public final int x0;
        public final int y0;
        public final int x1;
        public final int y1;
        public final float scale;
        public final Matrix3x2f pose;
        public final @Nullable ScreenRectangle scissorArea;
        public final @Nullable ScreenRectangle bounds;

        public final float rotX, rotY, zoom, panX, panY;
        public final int guiScale;
        public final @Nullable UUID templateUuid;
        public final @Nullable ArrayList<StatePos> statePosList;

        public State(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea,
                     int guiScale,
                     float rotX, float rotY, float zoom, float panX, float panY,
                     @Nullable UUID templateUuid, @Nullable ArrayList<StatePos> statePosList) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.scale = 1.0f;
            this.pose = pose;
            this.scissorArea = scissorArea;
            // bounds is stored for internal consumers that want the clipped rect; PictureInPictureRenderState
            // itself doesn't require exposing it through the interface, so we keep it private to State.
            this.bounds = PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea);
            this.guiScale = guiScale;
            this.rotX = rotX;
            this.rotY = rotY;
            this.zoom = zoom;
            this.panX = panX;
            this.panY = panY;
            this.templateUuid = templateUuid;
            this.statePosList = statePosList;
        }

        @Override public int x0() { return x0; }
        @Override public int x1() { return x1; }
        @Override public int y0() { return y0; }
        @Override public int y1() { return y1; }
        @Override public float scale() { return scale; }
        @Override public Matrix3x2f pose() { return pose; }
        @Override public @Nullable ScreenRectangle scissorArea() { return scissorArea; }
        @Override public @Nullable ScreenRectangle bounds() { return bounds; }
    }
}
