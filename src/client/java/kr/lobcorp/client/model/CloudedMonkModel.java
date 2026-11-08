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

/** 구름 낀 수도승. 네 발로 기어다니는, 후드를 쓴 승복 차림의 형체. 어깨와 머리 둘레에 구름 덩어리가 떠 있다. */
public class CloudedMonkModel extends EntityModel<CloudedMonkModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("clouded_monk"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg;
    private final ModelPart cloud1, cloud2, cloud3, cloud4;

    public CloudedMonkModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        cloud1 = root.getChild("cloud1");
        cloud2 = root.getChild("cloud2");
        cloud3 = root.getChild("cloud3");
        cloud4 = root.getChild("cloud4");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 앞으로 기울어진 승복 몸통 (u0 v0, 8x10x5)
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -10, -2.5f, 8, 10, 5), PartPose.offset(0, 14, 0));
        // 후드를 쓴 머리 (u0 v16, 6x6x6) — 앞면은 어두운 후드 안쪽
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 5, -3));
        // 바닥까지 닿는 긴 팔 (u0 v32, 2x12x2)
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(-4, 12, -2));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offset(4, 12, -2));
        // 짧은 다리 (u8 v32, 2x5x2)
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 32)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(-2, 19, 1));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(8, 32)
                .addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(2, 19, 1));
        // 어깨와 머리 둘레의 구름 덩어리 (u16 v32 / u24 v32 / u32 v32 / u40 v32)
        root.addOrReplaceChild("cloud1", CubeListBuilder.create().texOffs(16, 32)
                .addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(-5, 6, -1));
        root.addOrReplaceChild("cloud2", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(5, 6, -1));
        root.addOrReplaceChild("cloud3", CubeListBuilder.create().texOffs(32, 32)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(0, 1, -4));
        root.addOrReplaceChild("cloud4", CubeListBuilder.create().texOffs(40, 32)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(0, 8, 3));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 걷기 스윙
        float swing = Mth.cos(s.walkAnimationPos * 0.6662f) * 0.8f * s.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.6f - 0.3f;
        leftArm.xRot = swing * 0.6f - 0.3f;
        // 네 발로 기는 자세
        body.xRot = 0.55f;
        head.xRot = -0.35f + Mth.sin(t * 0.08f) * 0.05f;
        // 구름은 천천히 떠다닌다
        cloud1.y = 6 + Mth.sin(t * 0.06f) * 0.6f;
        cloud2.y = 6 + Mth.sin(t * 0.06f + 1.7f) * 0.6f;
        cloud3.y = 1 + Mth.sin(t * 0.09f + 0.8f) * 0.5f;
        cloud4.y = 8 + Mth.sin(t * 0.07f + 2.4f) * 0.5f;
        cloud1.yRot = Mth.sin(t * 0.05f) * 0.2f;
        cloud2.yRot = -Mth.sin(t * 0.05f) * 0.2f;
    }
}
