package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.api.GunBones;

public final class HarmonicaMagazineAdjuster implements AnimationAdjuster {
    @Override
    public void adjust(Context context) {
        var magazineOpt = context.model().getBone(GunBones.MAGAZINE);
        if (magazineOpt.isEmpty() || context.magazine() == null) {
            return;
        }
        boolean ignoreForReload = context.reloadProgress() > 0.42;
        if (!ignoreForReload) {
            float percent = 1 - context.magazine().count() / 10f;
            magazineOpt.get().updatePosition(4 * percent, 0, 0);
        }
    }
}
