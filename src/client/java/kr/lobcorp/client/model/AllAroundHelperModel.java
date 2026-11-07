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

/** 웃는 화면 얼굴을 단 하얀 가사 로봇. 회전할 때는 몸 전체가 돌고, 탈출하면 칼날이 펼쳐진다. */
public class AllAroundHelperModel extends EntityModel<AllAroundHelperModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("all_around_helper"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public int phase;      // 0 휴식, 1 회전, 2 돌진
        public boolean breached;
    }

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart faceHappy;
    private final ModelPart faceAngry;
    private final ModelPart armMop;
    private final ModelPart armBrush;
    private final ModelPart armBladeRight;
    private final ModelPart armBladeLeft;

    public AllAroundHelperModel(ModelPart root) {
        super(root);
        this.root = root;
        body = root.getChild("body");
        faceHappy = body.getChild("face_happy");
        faceAngry = body.getChild("face_angry");
        armMop = root.getChild("arm_mop");
        armBrush = root.getChild("arm_brush");
        armBladeRight = root.getChild("arm_blade_right");
        armBladeLeft = root.getChild("arm_blade_left");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, -10, -5, 10, 10, 10), PartPose.offset(0, 12, 0));
        // 이음매
        body.addOrReplaceChild("seam", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-5.05f, -5.5f, -5.05f, 10.1f, 1, 10.1f), PartPose.ZERO);
        // 웃는 화면
        body.addOrReplaceChild("face_happy", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-3, -7, -5.5f, 6, 4, 1), PartPose.ZERO);
        // 화난 화면 (탈출 중에만 보인다)
        body.addOrReplaceChild("face_angry", CubeListBuilder.create().texOffs(40, 6)
                .addBox(-3, -7, -5.6f, 6, 4, 1), PartPose.ZERO);

        // 바퀴 세 개
        root.addOrReplaceChild("wheel_left", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(-3, 23, 0));
        root.addOrReplaceChild("wheel_right", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(3, 23, 0));
        root.addOrReplaceChild("wheel_back", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, 23, 3));

        // 청소 도구를 든 팔 두 개
        PartDefinition mop = root.addOrReplaceChild("arm_mop", CubeListBuilder.create().texOffs(8, 32)
                .addBox(-0.5f, 0, -0.5f, 1, 7, 1), PartPose.offsetAndRotation(-5.5f, 13, 0, 0, 0, 0.35f));
        mop.addOrReplaceChild("mop_head", CubeListBuilder.create().texOffs(12, 32)
                .addBox(-1.5f, 7, -0.5f, 3, 2, 1), PartPose.ZERO);
        PartDefinition brush = root.addOrReplaceChild("arm_brush", CubeListBuilder.create().texOffs(8, 32)
                .addBox(-0.5f, 0, -0.5f, 1, 7, 1), PartPose.offsetAndRotation(5.5f, 13, 0, 0, 0, -0.35f));
        brush.addOrReplaceChild("brush_head", CubeListBuilder.create().texOffs(12, 32)
                .addBox(-1.5f, 7, -0.5f, 3, 2, 1), PartPose.ZERO);

        // 숨겨진 칼날 팔 두 개
        PartDefinition bladeR = root.addOrReplaceChild("arm_blade_right", CubeListBuilder.create().texOffs(20, 32)
                .addBox(-0.5f, 0, -1.5f, 1, 5, 3), PartPose.offsetAndRotation(5.5f, 14, 1.5f, 0, 0, -0.2f));
        bladeR.addOrReplaceChild("blade_tip_r", CubeListBuilder.create().texOffs(28, 32)
                .addBox(-0.5f, 5, -1.5f, 1, 2, 3), PartPose.ZERO);
        PartDefinition bladeL = root.addOrReplaceChild("arm_blade_left", CubeListBuilder.create().texOffs(20, 32)
                .addBox(-0.5f, 0, -1.5f, 1, 5, 3), PartPose.offsetAndRotation(-5.5f, 14, 1.5f, 0, 0, 0.2f));
        bladeL.addOrReplaceChild("blade_tip_l", CubeListBuilder.create().texOffs(28, 32)
                .addBox(-0.5f, 5, -1.5f, 1, 2, 3), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;

        // 회전 중에는 몸 전체가 돈다
        if (s.phase == 1) {
            root.yRot = t * 0.7f;
        } else {
            root.yRot = 0f;
        }

        // 화면 얼굴 토글
        boolean angry = s.breached && s.phase != 0;
        faceHappy.visible = !angry;
        faceAngry.visible = angry;

        // 칼날: 격리 중에는 접혀 있고, 탈출하면 펼쳐진다
        float spread = s.breached ? 1f : 0f;
        armBladeRight.zRot = Mth.lerp(spread, -0.2f, -1.4f);
        armBladeLeft.zRot = Mth.lerp(spread, 0.2f, 1.4f);
        armBladeRight.xRot = Mth.lerp(spread, 0.9f, 0f);
        armBladeLeft.xRot = Mth.lerp(spread, 0.9f, 0f);

        // 청소 팔은 살짝 흔들린다
        armMop.zRot = 0.35f + Mth.sin(t * 0.08f) * 0.05f;
        armBrush.zRot = -0.35f - Mth.sin(t * 0.08f) * 0.05f;
    }
}
