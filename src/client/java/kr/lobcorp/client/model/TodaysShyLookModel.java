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
 * 천에 걸린 다섯 개의 얼굴 판과, 그중 하나 뒤에 숨어 있는 작은 형체.
 * 형체는 항상 판 뒤(z=+1)에 서 있고, 현재 얼굴 판의 x 위치를 따라간다.
 *
 * 모델 좌표: y=24가 바닥, 정면은 -Z.
 *  - 형체: 다리 y24..16, 몸통 y16..6, 머리 y6..2
 *  - 가로대: y=-1, x -12..12, z=-3 (형체보다 앞)
 *  - 얼굴 판: 4x6x1, 위쪽 y=0 → y0..6, x 중심 -10/-5/0/5/10, z=-3
 *  - 기둥: 1x25x1, x=±12.5, 바닥(y24)에서 가로대(y-1)까지
 */
public class TodaysShyLookModel extends EntityModel<TodaysShyLookModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("todays_shy_look"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public int face = 2;
    }

    /** 얼굴 판 중심 x 좌표 */
    private static final float[] FACE_X = {-10f, -5f, 0f, 5f, 10f};

    private final ModelPart figure;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public TodaysShyLookModel(ModelPart root) {
        super(root);
        figure = root.getChild("figure");
        head = figure.getChild("head");
        rightArm = figure.getChild("right_arm");
        leftArm = figure.getChild("left_arm");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── 가로대 (y=-1, z=-3) ──
        root.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12, -0.5f, -0.5f, 24, 1, 1), PartPose.offset(0, -1, -3));

        // ── 기둥 두 개: 바닥(y24)에서 가로대(y-1)까지 25px ──
        root.addOrReplaceChild("post_right", CubeListBuilder.create().texOffs(0, 2)
                .addBox(-0.5f, 0, -0.5f, 1, 25, 1), PartPose.offset(-12.5f, -1, -3));
        root.addOrReplaceChild("post_left", CubeListBuilder.create().texOffs(0, 2)
                .addBox(-0.5f, 0, -0.5f, 1, 25, 1), PartPose.offset(12.5f, -1, -3));

        // ── 얼굴 판 다섯 개: 4x6x1, 위쪽 y=0, z=-3 ──
        for (int i = 0; i < 5; i++) {
            root.addOrReplaceChild("face" + i, CubeListBuilder.create().texOffs(i * 4, 8)
                    .addBox(-2, 0, -0.5f, 4, 6, 1), PartPose.offset(FACE_X[i], 0, -3));
        }

        // ── 천 뒤에 숨은 형체 (z=+1) ──
        PartDefinition fig = root.addOrReplaceChild("figure", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -1.5f, 4, 10, 3), PartPose.offset(0, 6, 1));
        fig.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 16)
                .addBox(-2, -4, -2, 4, 4, 4), PartPose.offset(0, 0, 0));
        fig.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 16)
                .addBox(-1, 0, -1, 2, 9, 2), PartPose.offset(-3, 0, 0));
        fig.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 16)
                .addBox(-1, 0, -1, 2, 9, 2), PartPose.offset(3, 0, 0));
        fig.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(44, 16)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-1, 10, 0));
        fig.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(44, 16)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(1, 10, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        int face = Math.max(0, Math.min(4, s.face));

        // 형체는 현재 얼굴 판 뒤로 이동한다 (판이 머리를 가린다)
        figure.x = FACE_X[face];
        figure.z = 1;
        figure.y = 6 + Mth.sin(t * 0.05f) * 0.15f;

        // 살짝 내미는 머리
        head.x = Mth.sin(t * 0.05f) * 0.5f;
        head.yRot = Mth.sin(t * 0.03f) * 0.12f;
        head.xRot = 0.08f;

        // 팔은 축 늘어뜨린다
        rightArm.xRot = 0.05f;
        leftArm.xRot = 0.05f;
        rightArm.zRot = 0.06f;
        leftArm.zRot = -0.06f;
    }
}
