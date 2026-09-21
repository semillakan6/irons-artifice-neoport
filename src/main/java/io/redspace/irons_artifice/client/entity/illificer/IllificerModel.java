package io.redspace.irons_artifice.client.entity.illificer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.IllagerModel;
import io.redspace.irons_artifice.entity.Illificer;

public class IllificerModel extends IllagerModel<Illificer> {
    protected final ModelPart rightArm;
    protected final ModelPart leftArm;

    public IllificerModel(ModelPart root) {
        super(root);
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.getHat().visible = true;
    }

}
