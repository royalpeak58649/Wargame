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

/** 텅 빈 갑옷. 투구 안쪽에서 푸른 빛이 새어 나온다. 오른쪽 손등가죽은 바닥에 떨어져 있다. */
public class CrumblingArmorModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("crumbling_armor"), "main");

    private final ModelPart crestLeft, crestRight;

    public CrumblingArmorModel(ModelPart root) {
        super(root);
        ModelPart helmet = root.getChild("helmet");
        crestLeft = helmet.getChild("crest_left");
        crestRight = helmet.getChild("crest_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 투구 + 투구깃
        PartDefinition helmet = root.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -7, -4, 8, 7, 8), PartPose.offset(0, 8, 0));
        helmet.addOrReplaceChild("crest_left", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-0.5f, -5, -0.5f, 1, 5, 1), PartPose.offsetAndRotation(-1, -7, 0, 0, 0, 0.45f));
        helmet.addOrReplaceChild("crest_right", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-0.5f, -5, -0.5f, 1, 5, 1), PartPose.offsetAndRotation(1, -7, 0, 0, 0, -0.45f));

        // 흉갑
        root.addOrReplaceChild("cuirass", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-5, 0, -3, 10, 10, 6), PartPose.offset(0, 8, 0));

        // 초하부 (풍기)
        root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-5.5f, 0, -3.5f, 11, 5, 7), PartPose.offset(0, 18, 0));

        // 어깨 판
        root.addOrReplaceChild("shoulder_right", CubeListBuilder.create().texOffs(36, 16)
                .addBox(-4, -2, -3, 4, 2, 6), PartPose.offset(-5, 9, 0));
        root.addOrReplaceChild("shoulder_left", CubeListBuilder.create().texOffs(36, 16)
                .addBox(0, -2, -3, 4, 2, 6), PartPose.offset(5, 9, 0));

        // 왼팔 (오른쪽 손등가죽은 떨어져 나갔다)
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 24)
                .addBox(-1.5f, 0, -1.5f, 3, 10, 3), PartPose.offset(6.5f, 9, 0));

        // 다리
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(44, 24)
                .addBox(-2, 0, -2, 4, 8, 4), PartPose.offset(-2.5f, 16, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(44, 24)
                .addBox(-2, 0, -2, 4, 8, 4), PartPose.offset(2.5f, 16, 0));

        // 바닥에 떨어진 오른쪽 손등가죽
        root.addOrReplaceChild("gauntlet", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.5f, 0, -2.5f, 3, 3, 5), PartPose.offsetAndRotation(3, 22, -6, 0, 0.5f, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float sway = Mth.sin(t * 0.05f) * 0.03f;
        crestLeft.zRot = 0.45f + sway;
        crestRight.zRot = -0.45f - sway;
    }
}
