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
 * 천에 걸린 다섯 개의 얼굴과, 그중 하나 뒤에 숨어 있는 작은 형체.
 * 얼굴 판은 각각 다른 텍스처 영역을 쓴다.
 */
public class TodaysShyLookModel extends EntityModel<TodaysShyLookModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("todays_shy_look"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public int face = 2;
    }

    private final ModelPart figure;
    private final ModelPart head;

    public TodaysShyLookModel(ModelPart root) {
        super(root);
        figure = root.getChild("figure");
        head = figure.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 얼굴들이 매달린 밧줄
        root.addOrReplaceChild("rope", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-7, -0.5f, -0.5f, 14, 1, 1), PartPose.offset(0, 4, 0));

        // 다섯 개의 얼굴 판 (각각 다른 텍스처 영역)
        int[] xs = {-6, -3, 0, 3, 6};
        for (int i = 0; i < 5; i++) {
            root.addOrReplaceChild("face" + i, CubeListBuilder.create().texOffs(i * 4, 8)
                    .addBox(-1.5f, 0, -0.5f, 3, 4, 1), PartPose.offset(xs[i], 4.5f, 0));
        }

        // 천 뒤에 숨은 형체
        PartDefinition fig = root.addOrReplaceChild("figure", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -1.5f, 4, 8, 3), PartPose.offset(0, 12, 2));
        fig.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 16)
                .addBox(-1.5f, -3, -1.5f, 3, 3, 3), PartPose.offset(0, 0, 0));
        fig.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 16)
                .addBox(-0.5f, 0, -0.5f, 1, 5, 1), PartPose.offset(-1, 8, 0));
        fig.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(32, 16)
                .addBox(-0.5f, 0, -0.5f, 1, 5, 1), PartPose.offset(1, 8, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        int face = Math.max(0, Math.min(4, s.face));
        float[] xs = {-6, -3, 0, 3, 6};
        // 형체는 현재 얼굴 판 뒤로 이동한다
        figure.x = xs[face];
        figure.z = 2;
        figure.y = 12 + Mth.sin(t * 0.05f) * 0.15f;
        head.yRot = Mth.sin(t * 0.03f) * 0.15f;
        head.xRot = 0.1f;
    }
}
