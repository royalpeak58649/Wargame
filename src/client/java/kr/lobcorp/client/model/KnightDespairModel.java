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

/** 푸른 머리카락이 검게 물들어 가는 여성 기사. 등 뒤로 수많은 검이 떠 있다. */
public class KnightDespairModel extends EntityModel<KnightDespairModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("knight_of_despair"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public float throwAnim; // 0~1
    }

    private final ModelPart head, rightArm, leftArm, cape, hair;
    private final ModelPart[] swords;

    public KnightDespairModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        cape = root.getChild("cape");
        hair = root.getChild("hair");
        swords = new ModelPart[]{
                root.getChild("sword0"), root.getChild("sword1"), root.getChild("sword2"),
                root.getChild("sword3"), root.getChild("sword4")
        };
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 다리: 어두운 갑옷
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(-1.5f, 24, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 0)
                .addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(1.5f, 24, 0));
        // 치마
        root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3.5f, 0, -2.5f, 7, 5, 5), PartPose.offset(0, 14, 0));
        // 몸통: 은빛 푸른 갑옷
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-3, 0, -1.5f, 6, 7, 3), PartPose.offset(0, 7, 0));
        // 팔
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(42, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-4, 7.5f, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(50, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(4, 7.5f, 0));
        // 머리: 창백한 얼굴
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 7, 0));
        head.addOrReplaceChild("tear", CubeListBuilder.create().texOffs(24, 24)
                .addBox(0, 0, 0, 1, 2, 0), PartPose.offset(1, -3, -3.05f));
        // 긴 머리카락: 위는 푸르고 아래로 갈수록 검어진다
        root.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-3, 0, -1, 6, 12, 2), PartPose.offset(0, 7, 3));
        // 찢어진 망토
        root.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(16, 36)
                .addBox(-4, 0, 0, 8, 14, 1), PartPose.offset(0, 7, 2.5f));
        // 등 뒤로 떠 있는 다섯 자루의 검
        for (int i = 0; i < 5; i++) {
            root.addOrReplaceChild("sword" + i, CubeListBuilder.create().texOffs(34, 36)
                    .addBox(-0.5f, -8, -0.5f, 1, 8, 1), PartPose.offset(0, 12, 4));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        head.xRot = 0.12f + Mth.sin(t * 0.03f) * 0.02f;
        head.yRot = Mth.sin(t * 0.02f) * 0.06f;
        rightArm.xRot = -0.1f;
        leftArm.xRot = -0.1f;
        rightArm.zRot = 0.08f;
        leftArm.zRot = -0.08f;
        cape.xRot = 0.12f + Mth.sin(t * 0.05f) * 0.05f;
        hair.xRot = 0.06f + Mth.sin(t * 0.04f) * 0.03f;
        if (s.breached) {
            // 검을 던지기 직전에는 팔을 들어 올린다
            float a = Math.min(1f, s.throwAnim * 2f);
            rightArm.xRot = Mth.lerp(a, -0.1f, -2.2f);
            rightArm.zRot = Mth.lerp(a, 0.08f, 0.3f);
            head.xRot = Mth.lerp(a, 0.12f, -0.1f);
        }
        for (int i = 0; i < swords.length; i++) {
            ModelPart sw = swords[i];
            float phase = i * 1.2566f; // 2π/5
            float bob = Mth.sin(t * 0.08f + phase) * 0.25f;
            sw.x = Mth.cos(phase) * 1.6f;
            sw.z = 4 + Mth.sin(phase) * 0.6f;
            sw.y = 12 + bob;
            sw.zRot = Mth.cos(phase) * 0.5f;
            sw.xRot = -0.25f + Mth.sin(t * 0.06f + phase) * 0.08f;
        }
    }
}
