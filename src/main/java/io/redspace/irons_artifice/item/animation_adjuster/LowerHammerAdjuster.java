package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.api.GunBones;

public final class LowerHammerAdjuster implements AnimationAdjuster {
    @Override
    public void adjust(Context context) {
        var boneOpt = context.model().getBone(GunBones.HAMMER);
        if (boneOpt.isEmpty() || context.magazine() == null) {
            return;
        } else if (context.magazine().isEmpty() && context.reloadProgress() <= 0) {
            boneOpt.get().updateRotation(0, 0, 0);
        }
    }
}
