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

/** 백야. 빛나는 날개와 머리 위의 고리를 가진 존재. */
public class WhiteNightModel extends EntityModel<WhiteNightModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("whitenight"), "main");

    public static class State extends LivingEntityRenderState {
    }

    private final ModelPart body, head, halo, wingL, wingR;

    public WhiteNightModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = root.getChild("head");
        halo = head.getChild("halo");
        wingL = root.getChild("wing_l");
        wingR = root.getChild("wing_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 몸통 (u0 v0, 8x16x6)
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -16, -3, 8, 16, 6),
                PartPose.offset(0, 16, 0));

        // 머리 (u0 v24, 8x8x8)
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, 0, 0));

        // 고리 (u0 v40, 14x1x14)
        head.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-7, -1, -7, 14, 1, 14),
                PartPose.offset(0, -11, 0));

        // 큰 날개 (u0 v56 / u32 v56, 2x20x16)
        root.addOrReplaceChild("wing_l", CubeListBuilder.create()
                .texOffs(0, 56).addBox(0, -10, -8, 2, 20, 16),
                PartPose.offset(4, 4, 0));
        root.addOrReplaceChild("wing_r", CubeListBuilder.create()
                .texOffs(32, 56).addBox(-2, -10, -8, 2, 20, 16),
                PartPose.offset(-4, 4, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 천천히 떠다닌다
        float bob = Mth.sin(t * 0.05f);
        body.y = 16 + bob * 1.5f;
        head.y = bob * 1.5f;
        body.yRot = Mth.sin(t * 0.02f) * 0.1f;
        head.yRot = Mth.sin(t * 0.03f) * 0.15f;
        // 느린 날갯짓
        float flap = Mth.sin(t * 0.06f) * 0.35f;
        wingL.zRot = -0.2f - flap;
        wingR.zRot = 0.2f + flap;
        wingL.xRot = Mth.sin(t * 0.04f) * 0.1f;
        wingR.xRot = Mth.sin(t * 0.04f) * 0.1f;
        halo.yRot = t * 0.04f;
        halo.y = -11 + Mth.sin(t * 0.08f) * 0.4f;
    }
}
