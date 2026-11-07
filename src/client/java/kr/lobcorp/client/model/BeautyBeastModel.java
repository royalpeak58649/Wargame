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

/** 초록 눈이 잔뜩 박힌 곤충과 짐승의 뒤섞인 몸. 상처 입으면 낮게 웅크린다. */
public class BeautyBeastModel extends EntityModel<BeautyBeastModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("beauty_and_the_beast"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean wounded;
    }

    private final ModelPart body, head;
    private final ModelPart[] legs = new ModelPart[6];

    public BeautyBeastModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        for (int i = 0; i < 6; i++) legs[i] = root.getChild("leg" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통: 갈색 털에 보랏빛 반점
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -3.5f, -5, 8, 7, 10), PartPose.offset(0, 15, 0));

        // 머리: 앞면이 온통 작은 초록 눈
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-3.5f, -3, -3, 7, 6, 6), PartPose.offset(0, 13, -6));

        // 큰 뿔 두 개: 위로 솟다가 바깥으로 휘어진다
        for (int side = -1; side <= 1; side += 2) {
            String name = side < 0 ? "horn_l" : "horn_r";
            PartDefinition lower = head.addOrReplaceChild(name, CubeListBuilder.create().texOffs(28, 18)
                            .addBox(-1, -5, -1, 2, 5, 2),
                    PartPose.offsetAndRotation(side * 2.5f, -2.5f, 0, 0, 0, side * 0.25f));
            PartDefinition upper = lower.addOrReplaceChild(name + "_tip", CubeListBuilder.create().texOffs(36, 18)
                            .addBox(-1, -5, -1, 2, 5, 2),
                    PartPose.offsetAndRotation(side * 0.6f, -4.6f, 0, 0, 0, side * 0.45f));
            // 뿔에 핀 작은 보랏빛 꽃
            upper.addOrReplaceChild(name + "_flower", CubeListBuilder.create().texOffs(44, 18)
                    .addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(side * 0.8f, -3.5f, 0));
        }

        // 다리 여섯 개: 가늘고 길게 바깥으로 뻗는다. 맨 뒤 한 쌍은 더 길다
        float[] yaw = {-0.9f, -0.45f, 0f, 0.45f, 0.9f, 1.35f};
        for (int i = 0; i < 6; i++) {
            float side = i < 3 ? -1f : 1f;
            float y = yaw[i % 3];
            boolean back = i % 3 == 2;
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 32)
                            .addBox(-0.5f, 0, -0.5f, 1, back ? 9 : 6, 1),
                    PartPose.offsetAndRotation(side * 3.5f, 17, back ? 3 : -2, 0.35f, y, side * 0.55f));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 느린 호흡
        float breath = 1f + Mth.sin(t * 0.05f) * 0.02f;
        body.yScale = breath;
        body.zScale = breath;

        if (s.wounded) {
            body.y = 19;
            head.y = 17;
            head.xRot = 0.5f;
            for (int i = 0; i < 6; i++) {
                float side = i < 3 ? -1f : 1f;
                legs[i].zRot = side * 1.05f;
            }
        } else {
            body.y = 15;
            head.y = 13;
            head.xRot = Mth.sin(t * 0.03f) * 0.06f;
            for (int i = 0; i < 6; i++) {
                float side = i < 3 ? -1f : 1f;
                legs[i].zRot = side * 0.55f;
            }
        }
    }
}
