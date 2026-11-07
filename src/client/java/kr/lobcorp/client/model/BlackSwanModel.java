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
 * 흑조의 꿈. 격리 중에는 호수 위에 선 여인과 여섯 마리의 백조,
 * 탈출하면 양산을 앞으로 든 여인만, 변이하면 검은 날개를 펼친다.
 */
public class BlackSwanModel extends EntityModel<BlackSwanModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("black_swan"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public int changed;
        public boolean transformed;
    }

    private final ModelPart lake;
    private final ModelPart swan0, swan1, swan2, swan3, swan4, swan5;
    private final ModelPart gown, body, rightArm, leftArm, head, crown;
    private final ModelPart parasolStick, parasolCanopy;
    private final ModelPart rightWing, leftWing;

    public BlackSwanModel(ModelPart root) {
        super(root);
        lake = root.getChild("lake");
        swan0 = root.getChild("swan0");
        swan1 = root.getChild("swan1");
        swan2 = root.getChild("swan2");
        swan3 = root.getChild("swan3");
        swan4 = root.getChild("swan4");
        swan5 = root.getChild("swan5");
        gown = root.getChild("gown");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        head = root.getChild("head");
        crown = head.getChild("crown");
        parasolStick = root.getChild("parasol_stick");
        parasolCanopy = root.getChild("parasol_canopy");
        rightWing = root.getChild("right_wing");
        leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 호수
        root.addOrReplaceChild("lake", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-10, 0, -10, 20, 1, 20), PartPose.offset(0, 24, 0));

        // 여섯 마리의 백조: 호숫가를 따라
        float[][] spots = {
                {-7f, -7f}, {0f, -8f}, {7f, -7f}, {7f, 7f}, {0f, 8f}, {-7f, 7f}
        };
        for (int i = 0; i < 6; i++) {
            PartDefinition swan = root.addOrReplaceChild("swan" + i, CubeListBuilder.create()
                    .texOffs(0, 24).addBox(-1.5f, -2, -2, 3, 2, 4)
                    .texOffs(14, 24).addBox(-0.5f, -5, -1.5f, 1, 3, 1)
                    .texOffs(18, 24).addBox(-1, -6, -2.5f, 2, 1, 2),
                    PartPose.offset(spots[i][0], 24, spots[i][1]));
            swan.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(26, 24)
                    .addBox(-0.5f, -6, -3.5f, 1, 1, 1), PartPose.ZERO);
        }

        // 드레스
        root.addOrReplaceChild("gown", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-4, 0, -4, 8, 10, 8), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 32)
                .addBox(-2.5f, 0, -1.5f, 5, 6, 3), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(48, 32)
                .addBox(-1, 0, -1, 2, 7, 2), PartPose.offset(-3.5f, 9, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 32)
                .addBox(-1, 0, -1, 2, 7, 2), PartPose.offset(3.5f, 9, 0));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 50)
                .addBox(-2.5f, -5, -2.5f, 5, 5, 5), PartPose.offset(0, 8, 0));
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(20, 50)
                .addBox(-3, -7, -3, 6, 2, 6), PartPose.ZERO);

        // 검은 양산
        root.addOrReplaceChild("parasol_stick", CubeListBuilder.create().texOffs(40, 50)
                .addBox(-0.5f, -10, -0.5f, 1, 10, 1), PartPose.offset(3.5f, 14, -2));
        root.addOrReplaceChild("parasol_canopy", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, -2, -5, 10, 2, 10), PartPose.offset(3.5f, 4, -2));

        // 변이: 검은 날개
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-10, 0, 0, 10, 12, 1), PartPose.offset(-2, 9, 2));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, 0, 0, 10, 12, 1), PartPose.offset(2, 9, 2));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;

        lake.visible = !s.breached;
        swan0.visible = !s.breached;
        swan1.visible = !s.breached;
        swan2.visible = !s.breached;
        swan3.visible = !s.breached;
        swan4.visible = !s.breached;
        swan5.visible = !s.breached;

        // 백조들은 호수 위를 천천히 떠다닌다
        ModelPart[] swans = {swan0, swan1, swan2, swan3, swan4, swan5};
        for (int i = 0; i < swans.length; i++) {
            ModelPart sw = swans[i];
            sw.y = 24 + Mth.sin(t * 0.03f + i) * 0.15f;
            sw.yRot = Mth.sin(t * 0.01f + i * 1.3f) * 0.5f;
        }

        // 여인
        gown.xRot = 0;
        body.xRot = 0;
        head.xRot = 0.05f;
        head.yRot = Mth.sin(t * 0.02f) * 0.08f;
        crown.yRot = 0;
        rightArm.xRot = -0.2f;
        leftArm.xRot = -0.2f;
        rightArm.zRot = 0.1f;
        leftArm.zRot = -0.1f;

        // 양산: 격리 중에는 어깨에 기대고, 탈출하면 앞으로 펼친다
        parasolStick.xRot = 0.15f;
        parasolCanopy.xRot = 0.15f;
        parasolCanopy.visible = !s.breached;
        if (s.breached) {
            parasolStick.xRot = -1.35f;
            parasolCanopy.xRot = -1.35f;
            parasolCanopy.visible = true;
            rightArm.xRot = -1.2f;
            rightArm.zRot = 0.25f;
        }

        // 변이: 날개를 펼친다
        rightWing.visible = s.transformed;
        leftWing.visible = s.transformed;
        if (s.transformed) {
            float flap = Mth.sin(t * 0.15f) * 0.25f;
            rightWing.zRot = 0.5f + flap;
            leftWing.zRot = -0.5f - flap;
            rightWing.yRot = -0.3f;
            leftWing.yRot = 0.3f;
            head.xRot = -0.15f;
        }
    }
}
