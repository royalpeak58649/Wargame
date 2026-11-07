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

/** 뿔이 크리스마스 트리처럼 장식된 사슴. 등 뒤로 썰매와 자루를 끌고 다닌다. */
public class RudoltaModel extends EntityModel<RudoltaModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("rudolta"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public float walk; // 다리 흔들림
    }

    private final ModelPart head, nose;
    private final ModelPart legFL, legFR, legBL, legBR;

    public RudoltaModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        ModelPart neck = body.getChild("neck");
        head = neck.getChild("head");
        nose = head.getChild("nose");
        legFL = root.getChild("leg_fl");
        legFR = root.getChild("leg_fr");
        legBL = root.getChild("leg_bl");
        legBR = root.getChild("leg_br");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (6x6x12)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, -3, -6, 6, 6, 12), PartPose.offset(0, 14, 0));

        // 목 + 머리 (4x4x7)
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-2, -8, -2, 4, 8, 4), PartPose.offsetAndRotation(0, -2, -5, -0.35f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 18)
                .addBox(-2, -4, -7, 4, 4, 7), PartPose.offset(0, -7, 0));
        // 붉게 빛나는 코 (2x2x2)
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(38, 18)
                .addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, -1, -7));
        // 귀
        head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(46, 18)
                .addBox(-1, -1, -1, 1, 2, 2), PartPose.offset(-2, -3, -2));
        head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(46, 18)
                .addBox(0, -1, -1, 1, 2, 2), PartPose.offset(2, -3, -2));

        // 뿔: 여러 개의 가는 막대가 갈라진다
        PartDefinition antlerL = head.addOrReplaceChild("antler_l", CubeListBuilder.create().texOffs(52, 18)
                .addBox(-0.5f, -6, -0.5f, 1, 6, 1), PartPose.offsetAndRotation(-1.5f, -4, -3, 0, 0, 0.35f));
        antlerL.addOrReplaceChild("branch_l1", CubeListBuilder.create().texOffs(52, 18)
                .addBox(-0.5f, -4, -0.5f, 1, 4, 1), PartPose.offsetAndRotation(-1.5f, -4, 0, 0, 0, 0.7f));
        antlerL.addOrReplaceChild("branch_l2", CubeListBuilder.create().texOffs(52, 18)
                .addBox(-0.5f, -3, -0.5f, 1, 3, 1), PartPose.offsetAndRotation(1.5f, -5, 0, 0, 0, -0.5f));
        PartDefinition antlerR = head.addOrReplaceChild("antler_r", CubeListBuilder.create().texOffs(52, 18)
                .addBox(-0.5f, -6, -0.5f, 1, 6, 1), PartPose.offsetAndRotation(1.5f, -4, -3, 0, 0, -0.35f));
        antlerR.addOrReplaceChild("branch_r1", CubeListBuilder.create().texOffs(52, 18)
                .addBox(-0.5f, -4, -0.5f, 1, 4, 1), PartPose.offsetAndRotation(1.5f, -4, 0, 0, 0, -0.7f));
        antlerR.addOrReplaceChild("branch_r2", CubeListBuilder.create().texOffs(52, 18)
                .addBox(-0.5f, -3, -0.5f, 1, 3, 1), PartPose.offsetAndRotation(-1.5f, -5, 0, 0, 0, 0.5f));

        // 뿔에 매달린 작은 전구들
        antlerL.addOrReplaceChild("light_l1", CubeListBuilder.create().texOffs(56, 18)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(0, -6, 0));
        antlerL.addOrReplaceChild("light_l2", CubeListBuilder.create().texOffs(56, 18)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(-2, -8, 0));
        antlerR.addOrReplaceChild("light_r1", CubeListBuilder.create().texOffs(56, 18)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(0, -6, 0));
        antlerR.addOrReplaceChild("light_r2", CubeListBuilder.create().texOffs(56, 18)
                .addBox(-0.5f, -0.5f, -0.5f, 1, 1, 1), PartPose.offset(2, -8, 0));

        // 다리 (2x8x2)
        root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-2, 16, -4));
        root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(2, 16, -4));
        root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-2, 16, 4));
        root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(2, 16, 4));

        // 썰매 (6x4x8) — 붉은 몸체 + 금빛 날
        PartDefinition sleigh = root.addOrReplaceChild("sleigh", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -4, 0, 6, 4, 8), PartPose.offset(0, 20, 6));
        sleigh.addOrReplaceChild("runner_l", CubeListBuilder.create().texOffs(28, 44)
                .addBox(-0.5f, 0, 0, 1, 1, 9), PartPose.offset(-3, 0, -0.5f));
        sleigh.addOrReplaceChild("runner_r", CubeListBuilder.create().texOffs(28, 44)
                .addBox(-0.5f, 0, 0, 1, 1, 9), PartPose.offset(3, 0, -0.5f));
        // 자루 (5x5x5)
        sleigh.addOrReplaceChild("sack", CubeListBuilder.create().texOffs(40, 44)
                .addBox(-2.5f, -5, 0, 5, 5, 5), PartPose.offset(0, -4, 1.5f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float w = s.walk;
        legFL.xRot = Mth.cos(w * 0.6662f) * 0.7f;
        legFR.xRot = Mth.cos(w * 0.6662f + Mth.PI) * 0.7f;
        legBL.xRot = Mth.cos(w * 0.6662f + Mth.PI) * 0.7f;
        legBR.xRot = Mth.cos(w * 0.6662f) * 0.7f;
        head.xRot = Mth.sin(t * 0.03f) * 0.06f;
        head.yRot = Mth.sin(t * 0.017f) * 0.12f;
        nose.xScale = 1f + Mth.sin(t * 0.15f) * 0.12f;
        nose.yScale = 1f + Mth.sin(t * 0.15f) * 0.12f;
        nose.zScale = 1f + Mth.sin(t * 0.15f) * 0.12f;
    }
}
