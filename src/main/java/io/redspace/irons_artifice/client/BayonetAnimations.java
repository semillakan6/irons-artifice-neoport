package io.redspace.irons_artifice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class BayonetAnimations {
    private static float progress(float time, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
    }

    private static float hitFeedbackAmount(float ticksSinceFeedbackStart) {
        return 0.4F * (outQuart(progress(ticksSinceFeedbackStart, 1.0F, 3.0F)) - inOutSine(progress(ticksSinceFeedbackStart, 3.0F, 10.0F)));
    }

    public static void firstPersonUse(float ticksSinceKineticHitFeedback, PoseStack poseStack, float timeHeld, HumanoidArm arm, ItemStack itemStack) {
        if (itemStack.has(io.redspace.irons_artifice.registry.DataComponentRegistry.BAYONET)) {
            BayonetAnimations.UseParams params = BayonetAnimations.UseParams.fromTime(timeHeld);
            int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
            Vec3 mainPose = new Vec3(-0.5, -0.075, -0.75);
            double mainPoseInterpolation = inOutBack(params.raiseProgress() - params.swayProgress() * 0.5f);
            poseStack.translate(
                    invert
                            * (
                            mainPoseInterpolation * mainPose.x
                                    + params.raiseProgressEnd() * -0.05F
                                    + params.swayProgress() * -0.1F
                                    + params.swayScaleSlow() * 0.005F
                    ),
                    mainPoseInterpolation * mainPose.y + params.raiseProgressMiddle() * 0.075F + params.swayScaleFast() * 0.01F,
                    mainPoseInterpolation * mainPose.z + params.raiseProgressStart() * 0.2 + params.raiseProgressEnd() * -0.2 + params.swayScaleSlow() * 0.005F
            );
            poseStack.rotateAround(
                    Axis.XP
                            .rotationDegrees(
//                        -65.0F * Ease.inOutBack(params.raiseProgress())
                                    -25.0F * params.lowerProgress()
                                            + -0.5F * params.swayScaleFast()
                            ),
                    0.0F,
                    0.1F,
                    0.0F
            );
            float angle = 45;
            poseStack.rotateAround(
                    Axis.ZN
                            .rotationDegrees(
                                    invert * (-angle * inOutBack(params.raiseProgress()) + angle * params.swayProgress() * 0.25f + 2.0F * params.swayScaleSlow())
                            ),
                    0,
                    0.0F,
                    0.0F
            );
            poseStack.translate(0.0F, -hitFeedbackAmount(ticksSinceKineticHitFeedback), 0.0F);
        }
    }

    record UseParams(
            float raiseProgress,
            float raiseProgressStart,
            float raiseProgressMiddle,
            float raiseProgressEnd,
            float swayProgress,
            float lowerProgress,
            float raiseBackProgress,
            float swayIntensity,
            float swayScaleSlow,
            float swayScaleFast
    ) {
        public static BayonetAnimations.UseParams fromTime(float time) {
            int finishRaisingTick = 10;
            int finishSwayingTick = 50 + finishRaisingTick;
            int startSwayingTick = finishSwayingTick - 20;
            int finishLoweringTick = 102 + finishRaisingTick;
            int startLoweringTick = finishLoweringTick - 40;
            int finishRaisingBackTick = 92 + finishRaisingTick;
            float raiseProgress = BayonetAnimations.progress(time, 0.0F, (float) finishRaisingTick);
            float raiseProgressStart = BayonetAnimations.progress(raiseProgress, 0.0F, 0.5F);
            float raiseProgressMiddle = BayonetAnimations.progress(raiseProgress, 0.5F, 0.8F);
            float raiseProgressEnd = BayonetAnimations.progress(raiseProgress, 0.8F, 1.0F);
            float swayProgress = BayonetAnimations.progress(time, (float) startSwayingTick, (float) finishSwayingTick);
            float lowerProgress = outCubic(inOutElastic(BayonetAnimations.progress(time - 20.0F, (float) startLoweringTick, (float) finishLoweringTick)));
            float raiseBackProgress = BayonetAnimations.progress(time, (float) (finishRaisingBackTick - 5), (float) finishRaisingBackTick);
            float swayIntensity = 2.0F * outCirc(swayProgress);
            float swayScaleSlow = Mth.sin(time * 19.0F * (float) (Math.PI / 180.0)) * swayIntensity;
            float swayScaleFast = Mth.sin(time * 30.0F * (float) (Math.PI / 180.0)) * swayIntensity;
            return new BayonetAnimations.UseParams(
                    raiseProgress,
                    raiseProgressStart,
                    raiseProgressMiddle,
                    raiseProgressEnd,
                    swayProgress,
                    lowerProgress,
                    raiseBackProgress,
                    swayIntensity,
                    swayScaleSlow,
                    swayScaleFast
            );
        }
    }

    private static float outQuart(float value) { return 1 - (float) Math.pow(1 - value, 4); }
    private static float inOutSine(float value) { return -(float) (Math.cos(Math.PI * value) - 1) / 2; }
    private static float outCubic(float value) { return 1 - (float) Math.pow(1 - value, 3); }
    private static float outCirc(float value) { return (float) Math.sqrt(1 - Math.pow(value - 1, 2)); }
    private static float inOutBack(float value) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;
        return value < 0.5f
                ? (float) (Math.pow(2 * value, 2) * ((c2 + 1) * 2 * value - c2) / 2)
                : (float) ((Math.pow(2 * value - 2, 2) * ((c2 + 1) * (value * 2 - 2) + c2) + 2) / 2);
    }
    private static float inOutElastic(float value) {
        if (value == 0 || value == 1) return value;
        double c = 2 * Math.PI / 4.5;
        return value < 0.5f
                ? (float) (-(Math.pow(2, 20 * value - 10) * Math.sin((20 * value - 11.125) * c)) / 2)
                : (float) ((Math.pow(2, -20 * value + 10) * Math.sin((20 * value - 11.125) * c)) / 2 + 1);
    }
}
