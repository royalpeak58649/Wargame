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

/** 온몸이 노란 눈으로 뒤덮인 거대한 검은 새. 긴 목 끝에 새장 같은 부리와 등불이 매달려 있다. */
public class BigBirdModel extends EntityModel<BigBirdModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("big_bird"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public float lamp; // 0~1 흔들림
    }

    private final ModelPart body, neck, head, lamp;

    public BigBirdModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        lamp = head.getChild("lamp");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8, -16, -7, 16, 16, 14), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-1.5f, 0, -1.5f, 3, 6, 3), PartPose.offset(-4, 18, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-1.5f, 0, -1.5f, 3, 6, 3), PartPose.offset(4, 18, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-2, -14, -2, 4, 14, 4), PartPose.offsetAndRotation(0, -15, -2, -0.35f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 40)
                .addBox(-3, -6, -4, 6, 6, 8), PartPose.offsetAndRotation(0, -14, 0, 0.35f, 0, 0));
        head.addOrReplaceChild("beak_top", CubeListBuilder.create().texOffs(44, 40)
                .addBox(-2, -1, -6, 4, 1, 6), PartPose.offset(0, -2, -4));
        head.addOrReplaceChild("beak_bottom", CubeListBuilder.create().texOffs(44, 48)
                .addBox(-2, 0, -6, 4, 1, 6), PartPose.offset(0, 1, -4));
        head.addOrReplaceChild("lamp", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.5f, 0, -1.5f, 3, 4, 3), PartPose.offset(0, 2, -8));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        body.y = 18 + Mth.sin(t * 0.05f) * 0.4f;
        neck.xRot = -0.35f + Mth.sin(t * 0.03f) * 0.04f;
        head.xRot = 0.35f + Mth.sin(t * 0.04f) * 0.05f;
        head.yRot = Mth.sin(t * 0.02f) * 0.15f;
        lamp.zRot = Mth.sin(t * 0.12f) * (0.15f + s.lamp * 0.5f);
        lamp.xRot = Mth.cos(t * 0.1f) * (0.1f + s.lamp * 0.3f);
    }
}
