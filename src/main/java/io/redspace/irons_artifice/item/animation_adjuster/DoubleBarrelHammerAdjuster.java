package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.api.GunBones;

public final class DoubleBarrelHammerAdjuster implements AnimationAdjuster {
    @Override
    public void adjust(Context context) {
        var leftOpt = context.model().getBone(GunBones.HAMMER_LEFT);
        var rightOpt = context.model().getBone(GunBones.HAMMER_RIGHT);
        if (leftOpt.isEmpty() || rightOpt.isEmpty() || context.magazine() == null) {
            return;
        } else if (context.reloadProgress() <= 1.17) {
            if (context.magazine().count() <= 1) {
                leftOpt.get().updateRotation(0, 0, 0);
            }
            if (context.magazine().isEmpty()) {
                rightOpt.get().updateRotation(0, 0, 0);
            }
        }
    }
}
