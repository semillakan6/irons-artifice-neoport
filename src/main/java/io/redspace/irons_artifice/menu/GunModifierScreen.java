package io.redspace.irons_artifice.menu;

import io.redspace.irons_artifice.IronsArtifice;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;

import static io.redspace.irons_artifice.menu.GunModifierMenu.SLOT_SIZE;

public class GunModifierScreen extends AbstractContainerScreen<GunModifierMenu> {
    private static final ResourceLocation BG_TEXTURE = IronsArtifice.id("textures/gui/gun_modifier_screen.png");
    private static final ResourceLocation SLOT_SPRITE = IronsArtifice.id("modifier_screen/slot");
    private static final float PREVIEW_SCALE = 16.0F * 3.0F;

    public GunModifierScreen(GunModifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, menu.gunstack.getHoverName().copy().setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withUnderlined(true)));
        this.imageWidth = 176;
        this.imageHeight = 183;
    }

    @Override
    protected void init() {
        super.init();
        int margin = (SLOT_SIZE - 16) / 2;
        for (var slot : menu.getModifierSlots()) {
            this.addRenderableOnly((graphics, mx, my, a) ->
                    graphics.blitSprite(SLOT_SPRITE, leftPos + slot.x - margin, topPos + slot.y - margin, SLOT_SIZE, SLOT_SIZE)
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(BG_TEXTURE, xo, yo, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        this.renderGunPreview(graphics);
    }

    private void renderGunPreview(GuiGraphics graphics) {
        ItemStack gun = this.menu.gunstack;
        if (gun.isEmpty()) {
            return;
        }

        int previewTop = this.topPos;
        int previewBottom = this.topPos + 93;
        graphics.enableScissor(this.leftPos, previewTop, this.leftPos + this.imageWidth, previewBottom);

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.width / 2.0F, this.topPos + 40.0F, 150.0F);
        pose.scale(PREVIEW_SCALE, -PREVIEW_SCALE, -PREVIEW_SCALE);
        pose.mulPose(Axis.XP.rotationDegrees(15.0F));
        float yRot = 15.0F + Mth.sin(Minecraft.getInstance().player.tickCount * Mth.DEG_TO_RAD * 2.0F) * 5.0F;
        pose.mulPose(Axis.YP.rotationDegrees(yRot));

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(
                gun,
                ItemDisplayContext.FIXED,
                15728880,
                OverlayTexture.NO_OVERLAY,
                pose,
                graphics.bufferSource(),
                minecraft.level,
                0
        );
        graphics.flush();
        pose.popPose();
        graphics.disableScissor();
    }
}
