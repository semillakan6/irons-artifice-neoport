package io.redspace.irons_artifice.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.client.MuzzleFlashEmitter;
import io.redspace.irons_artifice.data.HandOccupancy;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.item.AttachmentMap;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.item.ReloadState;
import io.redspace.irons_artifice.item.animation_adjuster.AnimationAdjuster;
import io.redspace.irons_artifice.registry.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.Set;

public class GunInHandRenderer extends GeoItemRenderer<GunItem> {
    private static final Set<ItemDisplayContext> HAND_CONTEXTS = Set.of(
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);

    private AttachmentMap attachments = AttachmentMap.EMPTY;
    private HandOccupancy occupancy = HandOccupancy.BOTH;
    private int ownerId = -1;
    private boolean boneAdjustmentsApplied;
    private float renderPartialTick;

    public GunInHandRenderer(GeoModel<GunItem> model) {
        super(model);
        addRenderLayer(new GunBoneLayer(this));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext perspective, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        attachments = stack.getOrDefault(DataComponentRegistry.ATTACHMENT, AttachmentMap.EMPTY);
        occupancy = HandOccupancy.BOTH;
        ownerId = -1;
        boneAdjustmentsApplied = false;
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player != null && (stack == player.getMainHandItem() || stack == player.getOffhandItem())) {
            HandOccupancy value = GunItem.currentOccupancy(player, stack);
            occupancy = value == null ? HandOccupancy.BOTH : value;
            ownerId = player.getId();
        }
        super.renderByItem(stack, perspective, poseStack, buffers, packedLight, packedOverlay);
    }

    @Override
    public void preRender(PoseStack poseStack, GunItem gun, BakedGeoModel model, MultiBufferSource buffers,
                          VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int color) {
        renderPartialTick = partialTick;
        if (renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.scale(-1, 1, 1);
        }
        super.preRender(poseStack, gun, model, buffers, buffer, isReRender, partialTick,
                packedLight, packedOverlay, color);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, GunItem gun, GeoBone bone, RenderType renderType,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int color) {
        // GeckoLib 4 evaluates animations after preRender. Apply the 26.x perspective
        // normalization here, immediately before the first bone is drawn, so first-person
        // root transforms cannot leak into GUI, FIXED, ground, or frame renders.
        if (!boneAdjustmentsApplied) {
            applyBoneAdjustments(gun, renderPartialTick);
            boneAdjustmentsApplied = true;
        }
        super.renderRecursively(poseStack, gun, bone, renderType, buffers, buffer, isReRender,
                partialTick, packedLight, packedOverlay, color);
    }

    private void applyBoneAdjustments(GunItem gun, float partialTick) {
        if (!HAND_CONTEXTS.contains(renderPerspective)) {
            for (GeoBone bone : getGeoModel().getAnimationProcessor().getRegisteredBones()) {
                if (!bone.getName().contains(GunBones.HAMMER)) {
                    restoreInitialTransform(bone, true);
                }
            }
        } else if (!isFirstPerson()) {
            getGeoModel().getBone(GunBones.ROOT).ifPresent(root -> restoreInitialTransform(root, false));
        }

        // Match the original renderer order: clear perspective animation first, then
        // restore state-driven parts such as magazines, hammers, and muzzle offsets.
        ItemStack stack = getCurrentItemStack();
        MagazineContents magazine = GunItem.getMagazine(stack);
        ReloadState reload = ReloadState.get(stack);
        float reloadPercent = reload == null ? 0 : reload.percent(partialTick);
        double reloadSeconds = reload == null ? 0 : reloadPercent * reload.durationTicks() / 20.0;
        float muzzleOffset = (float) GunplayManager.compose(null, gun.getGun(), stack)
                .value(ShotComponents.MUZZLE_OFFSET);
        AnimationAdjuster.Context context = new AnimationAdjuster.Context(
                getGeoModel(), magazine, reloadSeconds, reloadPercent, muzzleOffset);
        for (AnimationAdjuster adjuster : gun.getGun().animationAdjusters()) {
            adjuster.adjust(context);
        }
    }

    private static void restoreInitialTransform(GeoBone bone, boolean restoreScale) {
        // GeckoLib 4 stores a model bone's baked rotation in the same fields used by
        // animation output. Resetting those fields to zero breaks structural bones
        // such as the clockwork rifle's -45/+45 degree stock pair. The initial
        // snapshot is the unanimated model pose and is therefore the correct reset.
        var initial = bone.getInitialSnapshot();
        bone.updatePosition(initial.getOffsetX(), initial.getOffsetY(), initial.getOffsetZ());
        bone.updateRotation(initial.getRotX(), initial.getRotY(), initial.getRotZ());
        if (restoreScale) {
            bone.updateScale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
        }
    }

    private boolean isFirstPerson() {
        return renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    private final class GunBoneLayer extends GeoRenderLayer<GunItem> {
        private GunBoneLayer(GunInHandRenderer renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(PoseStack poseStack, GunItem gun, GeoBone bone, RenderType renderType,
                                  MultiBufferSource buffers, VertexConsumer buffer, float partialTick,
                                  int packedLight, int packedOverlay) {
            var attachment = attachments.attachments().get(bone.getName());
            if (attachment != null) {
                AttachmentRenderableRegistry.get(attachment).ifPresent(renderer -> {
                    poseStack.pushPose();
                    RenderUtil.translateToPivotPoint(poseStack, bone);
                    renderer.render(poseStack, buffers, packedLight, partialTick);
                    poseStack.popPose();
                });
            }
            if (bone.getName().equals(GunBones.SOCKET_MUZZLE) && ownerId >= 0 && HAND_CONTEXTS.contains(renderPerspective)) {
                poseStack.pushPose();
                RenderUtil.translateToPivotPoint(poseStack, bone);
                MuzzleFlashEmitter.tryEmit(ownerId, poseStack);
                poseStack.popPose();
            }
            if (isFirstPerson()) {
                if (bone.getName().equals(GunBones.RIGHT_ARM)) {
                    renderHand(poseStack, buffers, packedLight, true);
                } else if (bone.getName().equals(GunBones.LEFT_ARM) && occupancy == HandOccupancy.BOTH) {
                    renderHand(poseStack, buffers, packedLight, false);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void renderHand(PoseStack poseStack, MultiBufferSource buffers, int packedLight, boolean right) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> playerModel = renderer.getModel();
        ModelPart arm = right ? playerModel.rightArm : playerModel.leftArm;
        arm.setPos(0, 0, 0);
        arm.setRotation(0, 0, 0);
        poseStack.pushPose();
        poseStack.scale(-1, -1, 1);
        poseStack.translate(1 / 16f, -10 / 16f, 0);
        arm.render(poseStack, buffers.getBuffer(RenderType.entitySolid(player.getSkin().texture())),
                packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
