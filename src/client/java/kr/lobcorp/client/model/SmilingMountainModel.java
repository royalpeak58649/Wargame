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

/** 웃는 시체 산. 시체가 쌓여 산이 되고, 단계마다 무더기가 커진다. */
public class SmilingMountainModel extends EntityModel<SmilingMountainModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("smiling_mountain"), "main");

    public static class State extends LivingEntityRenderState {
        public int parts = 1; // 1..3
    }

    private final ModelPart base, mid, top;

    public SmilingMountainModel(ModelPart root) {
        super(root);
        base = root.getChild("base");
        mid = root.getChild("mid");
        top = root.getChild("top");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1단계: 넓은 시체 무더기 (u0 v0, 24x14x20) + 머리 네 개 (u0 v36, 4x4x4)
        PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12, -14, -10, 24, 14, 20)
                .texOffs(0, 36).addBox(-9, -18, -6, 4, 4, 4)
                .texOffs(0, 36).addBox(-2, -18, -8, 4, 4, 4)
                .texOffs(0, 36).addBox(5, -18, -5, 4, 4, 4)
                .texOffs(0, 36).addBox(-4, -18, 3, 4, 4, 4),
                PartPose.offset(0, 24, 0));

        // 2단계: 위에 얹힌 무더기 (u0 v48, 16x10x14) + 머리 두 개
        root.addOrReplaceChild("mid", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-8, -10, -7, 16, 10, 14)
                .texOffs(0, 36).addBox(-6, -14, -3, 4, 4, 4)
                .texOffs(0, 36).addBox(2, -14, -1, 4, 4, 4),
                PartPose.offset(0, 10, 0));

        // 3단계: 거대한 아가리 얼굴 무더기 (u0 v76, 12x10x12)
        root.addOrReplaceChild("top", CubeListBuilder.create()
                .texOffs(0, 76).addBox(-6, -10, -6, 12, 10, 12),
                PartPose.offset(0, 0, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        mid.visible = s.parts >= 2;
        top.visible = s.parts >= 3;
        // 느린 숨쉬기
        float breath = Mth.sin(t * 0.06f) * 0.4f;
        base.y = 24 + breath;
        mid.y = 10 + breath * 0.8f;
        top.y = breath * 0.6f;
        base.xRot = Mth.sin(t * 0.04f) * 0.02f;
        mid.xRot = Mth.sin(t * 0.05f + 1f) * 0.03f;
        top.xRot = Mth.sin(t * 0.07f + 2f) * 0.04f;
        top.yRot = Mth.sin(t * 0.03f) * 0.1f;
    }
}
