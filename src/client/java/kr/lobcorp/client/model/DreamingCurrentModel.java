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

/** 꿈꾸는 조류. 길고 낮은 무지개빛 물고기 — 마디 세 개, 크게 벌어지는 턱, 꼬리지느러미와 옆지느러미. */
public class DreamingCurrentModel extends EntityModel<DreamingCurrentModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("dreaming_current"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart head, jaw, seg1, seg2, seg3, tail, finL, finR;

    public DreamingCurrentModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        jaw = head.getChild("jaw");
        seg1 = root.getChild("seg1");
        seg2 = root.getChild("seg2");
        seg3 = root.getChild("seg3");
        tail = root.getChild("tail");
        finL = root.getChild("fin_l");
        finR = root.getChild("fin_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 머리: 위턱 (u0 v0, 6x3x7)
        PartDefinition headPart = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, -3, -7, 6, 3, 7), PartPose.offset(0, 20, -6));
        // 아래턱: 천천히 벌어진다 (u0 v10, 6x2x7)
        headPart.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-3, 0, -7, 6, 2, 7), PartPose.offset(0, 0, 0));
        // 몸통 마디 3개 (u0 v20 / u0 v32 / u0 v44, 각 6x5x7)
        root.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-3, -2.5f, 0, 6, 5, 7), PartPose.offset(0, 20, -6));
        root.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3, -2.5f, 0, 6, 5, 7), PartPose.offset(0, 20, 1));
        root.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -2.5f, 0, 6, 5, 7), PartPose.offset(0, 20, 8));
        // 꼬리지느러미 (u26 v0, 1x6x6)
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(26, 0)
                .addBox(-0.5f, -3, 0, 1, 6, 6), PartPose.offset(0, 20, 15));
        // 옆지느러미 (u26 v14 / u26 v24, 1x3x5)
        root.addOrReplaceChild("fin_l", CubeListBuilder.create().texOffs(26, 14)
                .addBox(0, -1.5f, 0, 1, 3, 5), PartPose.offset(3, 20, -4));
        root.addOrReplaceChild("fin_r", CubeListBuilder.create().texOffs(26, 24)
                .addBox(-1, -1.5f, 0, 1, 3, 5), PartPose.offset(-3, 20, -4));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 헤엄치는 물결: 마디마다 위상이 늦게 따라온다
        head.yRot = Mth.sin(t * 0.2f) * 0.15f;
        seg1.yRot = Mth.sin(t * 0.2f + 0.6f) * 0.15f;
        seg2.yRot = Mth.sin(t * 0.2f + 1.2f) * 0.15f;
        seg3.yRot = Mth.sin(t * 0.2f + 1.8f) * 0.15f;
        tail.yRot = Mth.sin(t * 0.2f + 2.4f) * 0.15f;
        // 턱은 천천히 벌렸다 닫힌다
        jaw.xRot = 0.15f + Mth.sin(t * 0.05f) * 0.35f;
        // 옆지느러미는 살랑인다
        finL.zRot = Mth.sin(t * 0.2f) * 0.25f;
        finR.zRot = -Mth.sin(t * 0.2f) * 0.25f;
    }
}
