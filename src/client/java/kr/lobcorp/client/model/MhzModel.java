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

/** 잡음만 흘러나오는 낡은 라디오. 왜곡 중에는 화면이 지직거리듯 몸체가 떨린다. */
public class MhzModel extends EntityModel<MhzModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("mhz"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean distorting;
    }

    private final ModelPart root;

    public MhzModel(ModelPart root) {
        super(root);
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 본체: 10x8x6
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, -8, -3, 10, 8, 6), PartPose.offset(0, 24, 0));
        // 스피커 그릴
        root.addOrReplaceChild("grille", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4, -7, -3.1f, 5, 6, 0), PartPose.offset(0, 24, 0));
        // 다이얼
        root.addOrReplaceChild("dial", CubeListBuilder.create().texOffs(32, 0)
                .addBox(2, -6, -3.2f, 2, 2, 1), PartPose.offset(0, 24, 0));
        // 안테나 두 개
        root.addOrReplaceChild("antenna_right", CubeListBuilder.create().texOffs(32, 4)
                .addBox(-0.5f, -8, -0.5f, 1, 8, 1), PartPose.offsetAndRotation(-3, 16, 0, 0, 0, 0.35f));
        root.addOrReplaceChild("antenna_left", CubeListBuilder.create().texOffs(32, 4)
                .addBox(-0.5f, -8, -0.5f, 1, 8, 1), PartPose.offsetAndRotation(3, 16, 0, 0, 0, -0.35f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float amp = s.distorting ? 0.5f : 0.12f;
        // ±0.5px 지터 (1px = 1/16 블록)
        root.x = Mth.sin(t * 2.7f) * amp / 16f;
        root.z = Mth.cos(t * 3.1f) * amp / 16f;
        root.yRot = Mth.sin(t * 0.9f) * (s.distorting ? 0.06f : 0.02f);
    }
}
