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

/** 바닥에 박힌 강철 해치. 손잡이가 천천히 돌아가고, 우리에 갇힌 등불이 희미하게 깜빡인다. */
public class ShelterModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("shelter"), "main");

    private final ModelPart handle, bulb;

    public ShelterModel(ModelPart root) {
        super(root);
        handle = root.getChild("handle");
        bulb = root.getChild("bulb");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 바닥 테두리
        root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-7, 23, -7, 14, 1, 14), PartPose.ZERO);
        // 둥근 해치
        root.addOrReplaceChild("hatch", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-5, 22, -5, 10, 1, 10), PartPose.ZERO);
        // 손잡이 링: 네 개의 막대
        PartDefinition handle = root.addOrReplaceChild("handle", CubeListBuilder.create(), PartPose.offset(0, 21.5f, 0));
        handle.addOrReplaceChild("bar_n", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2, -0.5f, -2, 4, 1, 1), PartPose.ZERO);
        handle.addOrReplaceChild("bar_s", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2, -0.5f, 1, 4, 1, 1), PartPose.ZERO);
        handle.addOrReplaceChild("bar_w", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2, -0.5f, -1, 1, 1, 2), PartPose.ZERO);
        handle.addOrReplaceChild("bar_e", CubeListBuilder.create().texOffs(0, 32)
                .addBox(1, -0.5f, -1, 1, 1, 2), PartPose.ZERO);
        // 등불 기둥
        root.addOrReplaceChild("post", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-0.5f, -6, -0.5f, 1, 6, 1), PartPose.offset(6, 23, -6));
        // 우리에 갇힌 희미한 전구
        root.addOrReplaceChild("bulb", CubeListBuilder.create().texOffs(8, 40)
                .addBox(-1, -8, -1, 2, 2, 2), PartPose.offset(6, 23, -6));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        handle.yRot = t * 0.02f;
        bulb.xScale = 1f + Mth.sin(t * 0.15f) * 0.06f;
        bulb.yScale = 1f + Mth.sin(t * 0.15f) * 0.06f;
        bulb.zScale = 1f + Mth.sin(t * 0.15f) * 0.06f;
    }
}
