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

/** 잠들어 있는 거대한 살덩어리. 울 때는 입이 벌어지고 눈이 젖는다. */
public class FetusModel extends EntityModel<FetusModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("nameless_fetus"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public float cry; // 0~1
    }

    private final ModelPart body, jaw, head;

    public FetusModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        jaw = root.getChild("jaw");
        head = root.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 거대한 살덩어리
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8, -10, -6, 16, 10, 12), PartPose.offset(0, 22, 0));
        // 입: 몸통 앞면에 붙은 어두운 판
        root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-5, -3, -1, 10, 6, 1), PartPose.offset(0, 17, -6.05f));
        // 머리: 몸통 위에 얹힌 작은 덩어리
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 24)
                .addBox(-4, -4, -4, 8, 4, 8), PartPose.offset(0, 12, 0));
        head.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(56, 24).addBox(-3, -2, -4.5f, 2, 1, 1)
                .texOffs(56, 24).addBox(1, -2, -4.5f, 2, 1, 1), PartPose.ZERO);
        // 작은 팔다리 네 개
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-1, 0, -1, 2, 3, 2), PartPose.offset(-8, 19, -2));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-1, 0, -1, 2, 3, 2), PartPose.offset(8, 19, -2));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-1, 0, -1, 2, 3, 2), PartPose.offset(-4, 21, 5));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-1, 0, -1, 2, 3, 2), PartPose.offset(4, 21, 5));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 숨쉬기
        float breath = 1f + Mth.sin(t * 0.06f) * 0.03f;
        body.xScale = breath;
        body.zScale = breath;
        body.yScale = 1f + Mth.sin(t * 0.06f) * 0.02f;
        head.y = 12f - Mth.sin(t * 0.06f) * 0.3f;
        if (s.cry > 0) {
            float c = Math.min(1f, s.cry);
            // 입이 벌어진다
            jaw.xRot = -0.9f * c;
            jaw.yScale = 1f + 0.6f * c;
            // 몸이 흐느낀다
            body.zRot = Mth.sin(t * 0.5f) * 0.03f * c;
            head.xRot = 0.25f * c;
            head.zRot = Mth.sin(t * 0.4f) * 0.05f * c;
        } else {
            jaw.xRot = 0f;
            jaw.yScale = 1f;
            body.zRot = 0f;
            head.xRot = 0.1f;
            head.zRot = 0f;
        }
    }
}
