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
 * 하얀 털 덩어리와 그 위의 빛나는 꽃.
 * 탈출하면 덩어리는 땅속으로 사라지고 줄기와 꽃, 바닥의 흰 털만 남는다.
 * 물 때는 덩어리가 땅에서 솟아오르며 아가리를 벌린다.
 */
public class MeatLanternModel extends EntityModel<MeatLanternModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("meat_lantern"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        /** 0 = 닫힌 아가리, 1 = 완전히 벌어짐 */
        public float bite;
    }

    private final ModelPart mound;
    private final ModelPart moundTop;
    private final ModelPart eyes;
    private final ModelPart jaw;
    private final ModelPart stalk;
    private final ModelPart bulb;
    private final ModelPart flowers;
    private final ModelPart hairs;

    public MeatLanternModel(ModelPart root) {
        super(root);
        mound = root.getChild("mound");
        moundTop = root.getChild("mound_top");
        eyes = root.getChild("eyes");
        jaw = root.getChild("jaw");
        stalk = root.getChild("stalk");
        bulb = root.getChild("bulb");
        flowers = root.getChild("flowers");
        hairs = root.getChild("hairs");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 큰 털 덩어리 14x9x14
        root.addOrReplaceChild("mound", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-7, -9, -7, 14, 9, 14), PartPose.offset(0, 24, 0));
        // 위에 얹힌 작은 덩어리 10x4x10
        root.addOrReplaceChild("mound_top", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-5, -4, -5, 10, 4, 10), PartPose.offset(0, 15, 0));
        // 작은 검은 눈 두 개
        root.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-3.5f, -7, -7.1f, 1, 1, 1)
                .texOffs(56, 0).addBox(2.5f, -7, -7.1f, 1, 1, 1), PartPose.offset(0, 24, 0));
        // 앞면 아가리 판 (이빨은 텍스처)
        root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-4, -3, -7.6f, 8, 3, 1), PartPose.offset(0, 24, 0));
        // 줄기: 위로 뻗다 앞으로 꺾인다
        root.addOrReplaceChild("stalk", CubeListBuilder.create().texOffs(40, 40)
                .addBox(-0.5f, -7, -0.5f, 1, 7, 1), PartPose.offsetAndRotation(0, 11, -2, 0.9f, 0, 0));
        // 빛나는 열매 3x3x3
        root.addOrReplaceChild("bulb", CubeListBuilder.create().texOffs(44, 40)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(0, 6, -7));
        // 작은 꽃 두 송이
        root.addOrReplaceChild("flowers", CubeListBuilder.create()
                .texOffs(56, 40).addBox(-2.5f, -0.5f, -1, 1, 1, 1)
                .texOffs(58, 40).addBox(1.5f, -0.5f, -1, 1, 1, 1), PartPose.offset(0, 6, -7));
        // 바닥의 흰 털 판 30x0.1x30
        root.addOrReplaceChild("hairs", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-15, 0, -15, 30, 0.1f, 30), PartPose.offset(0, 24, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        boolean hidden = s.breached && s.bite <= 0f;

        mound.visible = !hidden;
        moundTop.visible = !hidden;
        eyes.visible = !hidden;
        jaw.visible = !hidden;
        hairs.visible = s.breached;

        if (hidden) {
            // 땅속에 숨은 상태: 줄기와 꽃만 지면 가까이에 보인다
            stalk.y = 23;
            stalk.xRot = 1.35f;
            bulb.y = 21.5f;
            bulb.z = -2.5f;
            flowers.y = 21.5f;
            flowers.z = -2.5f;
            bulb.xRot = Mth.sin(t * 0.05f) * 0.1f;
            flowers.xRot = bulb.xRot;
            jaw.xRot = 0f;
            mound.y = 24;
            moundTop.y = 15;
        } else {
            stalk.y = 11;
            stalk.xRot = 0.9f + Mth.sin(t * 0.04f) * 0.05f;
            bulb.y = 6;
            bulb.z = -7;
            flowers.y = 6;
            flowers.z = -7;
            bulb.xRot = 0f;
            flowers.xRot = 0f;
            if (s.bite > 0f) {
                // 땅에서 솟아오르며 아가리를 벌린다
                float rise = Mth.clamp(s.bite, 0f, 1f);
                float off = Mth.lerp(rise, -14f, 0f);
                mound.y = 24 + off;
                moundTop.y = 15 + off;
                eyes.y = 24 + off;
                jaw.y = 24 + off;
                jaw.xRot = -0.9f * rise;
                stalk.y = 11 + off;
                bulb.y = 6 + off;
                flowers.y = 6 + off;
            } else {
                jaw.xRot = 0f;
                mound.y = 24;
                moundTop.y = 15;
                eyes.y = 24;
                jaw.y = 24;
            }
        }
    }
}
