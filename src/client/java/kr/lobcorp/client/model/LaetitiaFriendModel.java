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

/** 창백한 분홍 몸통에 꿰맨 자국, 앞면 가득 벌어진 아가리, 길고 가는 다리 여섯 개. */
public class LaetitiaFriendModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("laetitia_friend"), "main");

    private final ModelPart body, mouth, bow;
    private final ModelPart[] legs = new ModelPart[6];

    public LaetitiaFriendModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        mouth = root.getChild("mouth");
        bow = root.getChild("bow");
        for (int i = 0; i < 6; i++) legs[i] = root.getChild("leg" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -5, -4, 8, 5, 8), PartPose.offset(0, 16, 0));
        root.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -4, -1, 6, 4, 1), PartPose.offset(0, 16, -4.05f));
        root.addOrReplaceChild("bow", CubeListBuilder.create().texOffs(32, 16)
                .addBox(-2, -1, -1, 4, 2, 2), PartPose.offset(0, 10.5f, 0));
        // 다리 여섯 개: 좌우 3쌍, 각각 위/아래 두 마디
        float[] xs = {-4.2f, -4.2f, -4.2f, 4.2f, 4.2f, 4.2f};
        float[] zs = {-3f, 0f, 3f, -3f, 0f, 3f};
        for (int i = 0; i < 6; i++) {
            PartDefinition leg = root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 24)
                    .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(xs[i], 16, zs[i]));
            leg.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(4, 24)
                    .addBox(-0.5f, 0, -0.5f, 1, 6, 1), PartPose.offset(0, 6, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        body.yRot = Mth.sin(t * 0.06f) * 0.08f;
        mouth.xRot = 0.15f + Mth.sin(t * 0.2f) * 0.12f;
        bow.zRot = Mth.sin(t * 0.1f) * 0.2f;
        for (int i = 0; i < 6; i++) {
            float phase = i * 1.05f;
            float swing = Mth.cos(s.walkAnimationPos * 0.6f + phase) * 0.5f * s.walkAnimationSpeed;
            legs[i].xRot = swing;
            legs[i].zRot = (i < 3 ? 0.5f : -0.5f) + swing * 0.3f;
        }
    }
}
