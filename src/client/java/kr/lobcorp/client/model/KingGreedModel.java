package kr.lobcorp.client.model;

import kr.lobcorp.LobCorp;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * O-01-64 탐욕의 왕.
 * 격리 중에는 황금 드레스를 입은 마법소녀, 탈출하면 거대한 황금빛 아가리가 된다.
 */
public class KingGreedModel extends EntityModel<KingGreedModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("king_of_greed"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
    }

    private final ModelPart girl;
    private final ModelPart orb;
    private final ModelPart jaw;

    public KingGreedModel(ModelPart root) {
        super(root);
        girl = root.getChild("girl");
        orb = root.getChild("orb");
        jaw = orb.getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── 격리 중: 마법소녀 ──
        PartDefinition girl = root.addOrReplaceChild("girl", CubeListBuilder.create(), PartPose.ZERO);
        girl.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-1.5f, 16, 0));
        girl.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(1.5f, 16, 0));
        PartDefinition dress = girl.addOrReplaceChild("dress", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-4, 0, -3, 8, 7, 6), PartPose.offset(0, 9, 0));
        // 드레스의 붉은 보석
        dress.addOrReplaceChild("jewels", CubeListBuilder.create()
                .texOffs(28, 10).addBox(-3, 1, -3.1f, 1, 1, 1)
                .texOffs(28, 10).addBox(2, 1, -3.1f, 1, 1, 1)
                .texOffs(28, 10).addBox(-1, 4, -3.1f, 2, 2, 1), PartPose.ZERO);
        girl.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-2.5f, 0, -1.5f, 5, 5, 3), PartPose.offset(0, 4, 0));
        girl.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(16, 24)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(-3.5f, 4, 0));
        girl.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 24)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(3.5f, 4, 0));
        PartDefinition head = girl.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 4, 0));
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-2, -8, -2, 4, 2, 4), PartPose.ZERO);
        girl.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, 0, 1, 6, 10, 2), PartPose.offset(0, 4, 0));

        // ── 탈출: 거대한 황금빛 아가리 ──
        PartDefinition orb = root.addOrReplaceChild("orb", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8, -16, -8, 16, 16, 16), PartPose.offset(0, 24, 0));
        orb.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-8, -4.5f, -0.5f, 16, 9, 1), PartPose.offset(0, -8, -8));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        girl.visible = !s.breached;
        orb.visible = s.breached;
        if (s.breached) {
            // 아가리가 천천히 씹는다
            jaw.xRot = Mth.sin(s.ageInTicks * 0.4f) * 0.25f;
            orb.yRot = 0f;
        } else {
            jaw.xRot = 0f;
        }
    }
}
