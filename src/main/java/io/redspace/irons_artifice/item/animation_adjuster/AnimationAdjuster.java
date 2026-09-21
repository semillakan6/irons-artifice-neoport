package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.MagazineContents;
import software.bernie.geckolib.model.GeoModel;

public interface AnimationAdjuster {
    AnimationAdjuster LOWER_HAMMER = new LowerHammerAdjuster();
    AnimationAdjuster DOUBLE_BARREL_HAMMER = new DoubleBarrelHammerAdjuster();
    AnimationAdjuster HARMONICA_MAGAZINE = new HarmonicaMagazineAdjuster();
    AnimationAdjuster MUZZLE_LOAD_OFFSET = new MuzzleLoadOffsetAdjuster();

    void adjust(Context context);

    record Context(GeoModel<GunItem> model, MagazineContents magazine, double reloadProgress,
                   float reloadPercent, float muzzleOffset) {
    }
}
