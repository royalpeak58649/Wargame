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

/** 창백한 뱀. 땅에 납작하게 붙어 기어다닌다. */
public class NakedWormModel extends EntityModel<NakedWormModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("naked_worm"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, seg1, seg2, seg3;

    public NakedWormModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        seg1 = root.getChild("seg1");
        seg2 = root.getChild("seg2");
        seg3 = root.getChild("seg3");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 작은 머리
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -2, -4, 4, 4, 4), PartPose.offset(0, 21, -6));
        // 몸통 마디 3개: 땅에 낮게 붙는다
        root.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-2, -2, 0, 4, 4, 5), PartPose.offset(0, 21, -2));
        root.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-1.5f, -1.5f, 0, 3, 3, 5), PartPose.offset(0, 21, 3));
        root.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-1, -1, 0, 2, 2, 5), PartPose.offset(0, 21, 8));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 기어가는 물결: 마디마다 위상이 늦게 따라온다
        head.yRot = Mth.sin(t * 0.2f) * 0.25f;
        head.xRot = Mth.sin(t * 0.1f) * 0.1f;
        seg1.yRot = Mth.sin(t * 0.2f - 0.6f) * 0.3f;
        seg2.yRot = Mth.sin(t * 0.2f - 1.2f) * 0.35f;
        seg3.yRot = Mth.sin(t * 0.2f - 1.8f) * 0.4f;
        // 몸이 살짝 들썩인다
        seg1.y = 21 + Mth.sin(t * 0.2f) * 0.4f;
        seg2.y = 21 + Mth.sin(t * 0.2f - 0.6f) * 0.4f;
        seg3.y = 21 + Mth.sin(t * 0.2f - 1.2f) * 0.4f;
    }
}
