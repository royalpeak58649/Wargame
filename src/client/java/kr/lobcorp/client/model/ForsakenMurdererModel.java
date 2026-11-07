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
 * 버려진 살인마. 격리 중에는 구석에서 고개를 숙이고 서 있고,
 * 탈출하면 무릎으로 기어다니며 쇠붙이가 된 머리로 내리찍는다.
 */
public class ForsakenMurdererModel extends EntityModel<MurdererRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("forsaken_murderer"), "main");

    private final ModelPart head, cap, body, rightArm, leftArm, rightLeg, leftLeg;

    public ForsakenMurdererModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        cap = head.getChild("cap");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(16, 32)
                .addBox(-5, -9, -5, 10, 5, 10), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-2, -2, -1.5f, 3, 12, 3), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(52, 16)
                .addBox(-1, -2, -1.5f, 3, 12, 3), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MurdererRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float walk = s.walkAnimationPos, speed = Math.min(1f, s.walkAnimationSpeed);

        if (!s.breached) {
            // 조각상처럼 서서 고개를 숙이고 있다. 아주 가끔 미세하게 흔들린다.
            head.xRot = 0.55f + Mth.sin(t * 0.02f) * 0.02f;
            head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.2f;
            body.xRot = 0.08f;
            rightArm.xRot = 0.05f;
            leftArm.xRot = 0.05f;
            rightArm.zRot = 0.04f;
            leftArm.zRot = -0.04f;
            cap.zScale = 1f;
            return;
        }

        // ── 탈출: 무릎으로 기어간다 ──
        float lean = 0.5f;
        body.setPos(0, 11, -5);
        body.xRot = lean;
        head.setPos(0, 11, -5);
        head.xRot = 0.35f + Mth.sin(t * 0.25f) * 0.08f;
        head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.5f;

        // 머리 박치기
        if (s.attackAnim > 0) {
            float a = Mth.sin(s.attackAnim * Mth.PI);
            head.xRot += a * 1.1f;
            body.xRot += a * 0.35f;
        }
        cap.zScale = 1.45f; // 망치처럼 길어진 쇠머리

        float hipY = 11 + 12 * Mth.cos(lean), hipZ = -5 + 12 * Mth.sin(lean);
        rightLeg.setPos(-2, hipY, hipZ);
        leftLeg.setPos(2, hipY, hipZ);
        rightLeg.xRot = Mth.HALF_PI + Mth.cos(walk * 0.6f) * 0.15f * speed;
        leftLeg.xRot = Mth.HALF_PI - Mth.cos(walk * 0.6f) * 0.15f * speed;

        rightArm.setPos(-5, 12.5f, -4.5f);
        leftArm.setPos(5, 12.5f, -4.5f);
        rightArm.xRot = -0.15f + Mth.cos(walk * 0.6f) * 0.8f * speed;
        leftArm.xRot = -0.15f - Mth.cos(walk * 0.6f) * 0.8f * speed;
        rightArm.zRot = 0.1f;
        leftArm.zRot = -0.1f;
    }
}
