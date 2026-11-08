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

/** 보라색 정오. 거대한 검은 석판과 밑동에서 흔들리는 네 가닥 촉수. */
public class VioletNoonModel extends EntityModel<VioletNoonModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("violet_noon"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart body, upper, tentacle1, tentacle2, tentacle3, tentacle4;

    public VioletNoonModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        upper = body.getChild("upper");
        tentacle1 = root.getChild("tentacle1");
        tentacle2 = root.getChild("tentacle2");
        tentacle3 = root.getChild("tentacle3");
        tentacle4 = root.getChild("tentacle4");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 석판 아래쪽
        PartDefinition bodyPart = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -2, 8, 12, 4), PartPose.offset(0, 12, 0));
        // 석판 위쪽
        bodyPart.addOrReplaceChild("upper", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4, -12, -2, 8, 12, 4), PartPose.offset(0, 0, 0));
        // 밑동 촉수 네 가닥
        root.addOrReplaceChild("tentacle1", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-4, 16, -2));
        root.addOrReplaceChild("tentacle2", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(4, 16, -2));
        root.addOrReplaceChild("tentacle3", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-4, 16, 2));
        root.addOrReplaceChild("tentacle4", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(4, 16, 2));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 석판은 아주 느리게 흔들린다
        body.zRot = Mth.sin(t * 0.03f) * 0.04f;
        upper.zRot = Mth.sin(t * 0.03f + 0.6f) * 0.05f;
        // 촉수는 물결처럼 흔들린다
        tentacle1.xRot = Mth.sin(t * 0.1f) * 0.4f;
        tentacle2.xRot = Mth.sin(t * 0.1f + 1.6f) * 0.4f;
        tentacle3.xRot = Mth.sin(t * 0.1f + 3.1f) * 0.4f;
        tentacle4.xRot = Mth.sin(t * 0.1f + 4.7f) * 0.4f;
        tentacle1.zRot = Mth.cos(t * 0.08f) * 0.3f;
        tentacle2.zRot = Mth.cos(t * 0.08f + 1.6f) * 0.3f;
        tentacle3.zRot = Mth.cos(t * 0.08f + 3.1f) * 0.3f;
        tentacle4.zRot = Mth.cos(t * 0.08f + 4.7f) * 0.3f;
    }
}
