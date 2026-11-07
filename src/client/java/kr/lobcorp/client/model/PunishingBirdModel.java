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

/** 검은 횃대 위의 작은 흰 새. 탈출하면 날아다니고, 격노하면 거대한 붉은 아가리가 열린다. */
public class PunishingBirdModel extends EntityModel<PunishingBirdModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("punishing_bird"), "main");

    public static class State extends LivingEntityRenderState {
        public boolean breached;
        public boolean angry;
        public float peck; // 0~1
    }

    private final ModelPart perch, body, head, beak, wingL, wingR, tail, mouth;

    public PunishingBirdModel(ModelPart root) {
        super(root);
        perch = root.getChild("perch");
        body = root.getChild("body");
        head = body.getChild("head");
        beak = head.getChild("beak");
        wingL = body.getChild("wing_l");
        wingR = body.getChild("wing_r");
        tail = body.getChild("tail");
        mouth = body.getChild("mouth");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 횃대: 세로 기둥 + 가로 막대 (격리 중에만 보인다)
        PartDefinition perch = root.addOrReplaceChild("perch", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-0.5f, 0, -0.5f, 1, 14, 1)
                .texOffs(0, 40).addBox(-4, 0, -0.5f, 8, 1, 1),
                PartPose.offset(0, 10, 0));

        // 몸통 (탈출 시 중심이 되도록 별도 파트)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, -2, -2.5f, 4, 4, 5),
                PartPose.offset(0, 10, 0));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-1.5f, -3, -1.5f, 3, 3, 3),
                PartPose.offset(0, -2, -2));
        head.addOrReplaceChild("beak", CubeListBuilder.create()
                .texOffs(20, 12).addBox(-0.5f, -0.5f, -2, 1, 1, 2),
                PartPose.offset(0, -0.5f, -1.5f));

        body.addOrReplaceChild("wing_l", CubeListBuilder.create()
                .texOffs(24, 0).addBox(0, -0.5f, -2, 5, 1, 4),
                PartPose.offset(2, -0.5f, 0));
        body.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(24, 0).mirror()
                .addBox(-5, -0.5f, -2, 5, 1, 4),
                PartPose.offset(-2, -0.5f, 0));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(24, 8).addBox(-1, -0.5f, 0, 2, 1, 3),
                PartPose.offset(0, 0, 2.5f));

        // 격노 시 열리는 거대한 아가리
        body.addOrReplaceChild("mouth", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-3, -2.5f, -5, 6, 5, 5),
                PartPose.offset(0, 0, -2.5f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        perch.visible = !s.breached;
        mouth.visible = s.angry;

        if (!s.breached) {
            // 횃대 위에 앉아 가만히 있다
            body.y = 10;
            body.xRot = 0;
            head.xRot = Mth.sin(t * 0.05f) * 0.1f;
            head.yRot = Mth.sin(t * 0.03f) * 0.4f;
            wingL.yRot = 0;
            wingR.yRot = 0;
            wingL.zRot = 0;
            wingR.zRot = 0;
            tail.xRot = 0;
            return;
        }

        // 공중에 떠서 날갯짓
        body.y = 20 + Mth.sin(t * 0.2f) * 0.6f;
        body.xRot = 0.15f;
        float flap = Mth.sin(t * 1.8f) * 0.9f;
        wingL.zRot = -flap;
        wingR.zRot = flap;
        wingL.yRot = 0;
        wingR.yRot = 0;
        tail.xRot = 0.2f + Mth.sin(t * 0.3f) * 0.1f;
        head.xRot = 0.1f + s.peck * 0.9f;
        head.yRot = 0;
        if (s.angry) {
            head.xRot = -0.2f;
            body.xRot = 0.3f;
            float shake = Mth.sin(t * 2.4f) * 0.15f;
            mouth.x = shake;
        }
    }
}
