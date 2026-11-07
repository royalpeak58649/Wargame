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

/** 이젤 위에 걸린, 얼굴이 번진 초상화. */
public class PortraitModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("portrait"), "main");

    private final ModelPart canvas;

    public PortraitModel(ModelPart root) {
        super(root);
        canvas = root.getChild("canvas");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 앞다리 두 개 (살짝 벌어지고 뒤로 기울어진다)
        root.addOrReplaceChild("leg_left", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 16, 1), PartPose.offsetAndRotation(-5f, 24f, 2f, 0.18f, 0f, 0.10f));
        root.addOrReplaceChild("leg_right", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 16, 1), PartPose.offsetAndRotation(5f, 24f, 2f, 0.18f, 0f, -0.10f));
        // 뒷다리
        root.addOrReplaceChild("leg_back", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 16, 1), PartPose.offsetAndRotation(0f, 24f, -4f, -0.22f, 0f, 0f));
        // 받침대
        root.addOrReplaceChild("shelf", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-6, 0, -1, 12, 1, 3), PartPose.offset(0, 12, 0));
        // 캔버스 (액자 + 그림)
        PartDefinition canvas = root.addOrReplaceChild("canvas", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-6, -7, -0.5f, 12, 14, 1), PartPose.offsetAndRotation(0, 5f, 0f, -0.12f, 0f, 0f));
        // 금장 액자 네 개
        canvas.addOrReplaceChild("frame_top", CubeListBuilder.create().texOffs(40, 24)
                .addBox(-6.5f, -7.5f, -1, 13, 1, 2), PartPose.ZERO);
        canvas.addOrReplaceChild("frame_bottom", CubeListBuilder.create().texOffs(40, 24)
                .addBox(-6.5f, 6.5f, -1, 13, 1, 2), PartPose.ZERO);
        canvas.addOrReplaceChild("frame_left", CubeListBuilder.create().texOffs(40, 28)
                .addBox(-6.5f, -7.5f, -1, 1, 15, 2), PartPose.ZERO);
        canvas.addOrReplaceChild("frame_right", CubeListBuilder.create().texOffs(40, 28)
                .addBox(5.5f, -7.5f, -1, 1, 15, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 아주 느리게 흔들린다
        canvas.zRot = Mth.sin(t * 0.02f) * 0.012f;
        canvas.xRot = -0.12f + Mth.sin(t * 0.013f) * 0.008f;
    }
}
