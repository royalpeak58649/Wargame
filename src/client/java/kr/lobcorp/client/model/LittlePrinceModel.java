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

/** 망토를 두른 아이. 머리 대신 거대한 버섯 갓이 천천히 흔들린다. */
public class LittlePrinceModel extends EntityModel<LittlePrinceModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("little_prince"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart cloak, stem, cap;

    public LittlePrinceModel(ModelPart root) {
        super(root);
        cloak = root.getChild("cloak");
        stem = root.getChild("stem");
        cap = root.getChild("cap");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 아래로 갈수록 넓어지는 망토: 상체
        root.addOrReplaceChild("cloak_top", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 0, -2, 6, 5, 4), PartPose.offset(0, 9, 0));
        // 망토: 허리
        root.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-4, 0, -3, 8, 5, 6), PartPose.offset(0, 14, 0));
        // 망토: 밑단
        root.addOrReplaceChild("cloak_hem", CubeListBuilder.create().texOffs(0, 26)
                .addBox(-5, 0, -4, 10, 5, 8), PartPose.offset(0, 19, 0));
        // 가는 목(버섯 대)
        root.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-1.5f, -3, -1.5f, 3, 3, 3), PartPose.offset(0, 9, 0));
        // 거대한 버섯 갓
        PartDefinition cap = root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-7, -3, -7, 14, 3, 14), PartPose.offset(0, 6, 0));
        // 갓 아래 주름(아가미)
        cap.addOrReplaceChild("gills", CubeListBuilder.create()
                .texOffs(32, 12).addBox(-5, 0, -5, 10, 1, 10), PartPose.ZERO);
        // 갓 위 볼록한 정수리
        cap.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(32, 24).addBox(-4, -2, -4, 8, 2, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 느린 호흡: 갓이 좌우로 살짝 기운다
        cap.yRot = Mth.sin(t * 0.05f) * 0.12f;
        cap.xRot = Mth.sin(t * 0.05f + 1.2f) * 0.06f;
        stem.yRot = cap.yRot * 0.5f;
        cloak.xRot = Mth.sin(t * 0.05f) * 0.02f;
    }
}
