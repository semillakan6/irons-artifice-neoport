package io.redspace.irons_artifice.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AttachmentGeoRenderer extends GeoObjectRenderer<GeoAnimatable> {
    private final GeoAnimatable attachment = new StaticAttachment();

    public AttachmentGeoRenderer(GeoModel<GeoAnimatable> model) {
        super(model);
    }

    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, float partialTick) {
        var renderType = getRenderType(attachment, getTextureLocation(attachment), buffers, partialTick);
        super.render(poseStack, attachment, buffers, renderType, buffers.getBuffer(renderType), packedLight, partialTick);
    }

    @Override
    public void preRender(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel model,
                          MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int color) {
        // The parent gun renderer has already positioned the pose at the attachment bone.
        // GeoObjectRenderer's default half-block translation would detach the model.
    }

    private static class StaticAttachment implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object relatedObject) {
            return software.bernie.geckolib.util.RenderUtil.getCurrentTick();
        }
    }
}
