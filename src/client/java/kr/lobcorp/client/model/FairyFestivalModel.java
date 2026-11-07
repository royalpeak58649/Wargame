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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** 외다리로 선 요정 여왕과 그 주위를 맴도는 작은 요정들 */
public class FairyFestivalModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("fairy_festival"), "main");
    private static final int FAIRIES = 7;

    private final ModelPart queen, wingL, wingR, longArmL, longArmR;
    private final ModelPart[] fairies = new ModelPart[FAIRIES];
    private final ModelPart[] fWingL = new ModelPart[FAIRIES];
    private final ModelPart[] fWingR = new ModelPart[FAIRIES];

    public FairyFestivalModel(ModelPart root) {
        super(root);
        queen = root.getChild("queen");
        wingL = queen.getChild("wing_l");
        wingR = queen.getChild("wing_r");
        longArmL = queen.getChild("long_arm_l");
        longArmR = queen.getChild("long_arm_r");
        for (int i = 0; i < FAIRIES; i++) {
            fairies[i] = root.getChild("fairy" + i);
            fWingL[i] = fairies[i].getChild("wl");
            fWingR[i] = fairies[i].getChild("wr");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 여왕: 외다리 위에 가는 몸통, 긴 팔 한 쌍 + 짧은 팔 한 쌍, 곤충 날개 두 장
        PartDefinition queen = root.addOrReplaceChild("queen", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1, -10, -1, 2, 10, 2)          // 몸통
                .texOffs(8, 0).addBox(-2, -14, -2, 4, 4, 4)           // 머리
                .texOffs(28, 0).addBox(-2, -9, -0.5f, 1, 5, 1)        // 짧은 팔
                .texOffs(28, 0).addBox(1, -9, -0.5f, 1, 5, 1),
                PartPose.offset(0, 14, 0));
        queen.addOrReplaceChild("leg", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 10, 1), PartPose.ZERO);
        queen.addOrReplaceChild("long_arm_l", CubeListBuilder.create().texOffs(24, 0)
                .addBox(0, 0, -0.5f, 1, 8, 1), PartPose.offsetAndRotation(1, -9, 0, 0, 0, -0.5f));
        queen.addOrReplaceChild("long_arm_r", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -0.5f, 1, 8, 1), PartPose.offsetAndRotation(-1, -9, 0, 0, 0, 0.5f));
        queen.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 16)
                .addBox(0, -10, 0, 8, 10, 0), PartPose.offsetAndRotation(0.5f, -6, 1, 0, -0.4f, 0));
        queen.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-8, -10, 0, 8, 10, 0), PartPose.offsetAndRotation(-0.5f, -6, 1, 0, 0.4f, 0));

        // 작은 요정들
        for (int i = 0; i < FAIRIES; i++) {
            PartDefinition f = root.addOrReplaceChild("fairy" + i, CubeListBuilder.create().texOffs(32, 0)
                    .addBox(-1, -1.5f, -1, 2, 3, 2), PartPose.ZERO);
            f.addOrReplaceChild("wl", CubeListBuilder.create().texOffs(32, 8)
                    .addBox(0, -3, 0, 3, 3, 0), PartPose.offset(0.5f, -0.5f, 1));
            f.addOrReplaceChild("wr", CubeListBuilder.create().texOffs(32, 8).mirror()
                    .addBox(-3, -3, 0, 3, 3, 0), PartPose.offset(-0.5f, -0.5f, 1));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        queen.y = 14 + Mth.sin(t * 0.07f) * 1.2f;
        queen.yRot = Mth.sin(t * 0.02f) * 0.3f;
        float flap = Mth.sin(t * 0.9f) * 0.35f;
        wingL.yRot = -0.4f - flap;
        wingR.yRot = 0.4f + flap;
        longArmL.zRot = -0.5f + Mth.sin(t * 0.05f) * 0.15f;
        longArmR.zRot = 0.5f - Mth.sin(t * 0.05f + 1f) * 0.15f;

        for (int i = 0; i < FAIRIES; i++) {
            float a = t * 0.06f + i * (Mth.TWO_PI / FAIRIES);
            float r = 10 + (i % 3) * 3;
            ModelPart f = fairies[i];
            f.x = Mth.cos(a) * r;
            f.z = Mth.sin(a) * r;
            f.y = 2 + (i % 4) * 3 + Mth.sin(t * 0.15f + i) * 2;
            f.yRot = -a;
            float ff = Mth.sin(t * 1.6f + i) * 0.8f;
            fWingL[i].yRot = -ff;
            fWingR[i].yRot = ff;
        }
    }
}
