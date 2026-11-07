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

/** 천장에 실을 드리우고 매달린 어미 거미. 고치가 늘어날수록 주변에 고치가 매달린다. */
public class SpiderBudModel extends EntityModel<SpiderBudModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("spider_bud"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public int cocoons;
    }

    private final ModelPart[] legs = new ModelPart[8];
    private final ModelPart[] cocoonParts = new ModelPart[5];

    public SpiderBudModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 8; i++) legs[i] = root.getChild("leg" + i);
        for (int i = 0; i < 5; i++) cocoonParts[i] = root.getChild("cocoon" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 배 (둥근 몸통) — y=8 근처에 매달린다
        root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -3.5f, -4.5f, 8, 7, 9), PartPose.offset(0, 8, 2));
        // 두흉부 (앞쪽)
        root.addOrReplaceChild("cephalothorax", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3, -2.5f, -3, 6, 5, 6), PartPose.offset(0, 8, -3));
        // 천장으로 올라가는 실
        root.addOrReplaceChild("thread", CubeListBuilder.create().texOffs(34, 0)
                .addBox(-0.5f, -16, -0.5f, 1, 16, 1), PartPose.offset(0, 5, 2));

        // 다리 8개: 위쪽 마디는 위로, 아래쪽 마디는 아래로 뻗는다
        float[] yaw = {-1.15f, -0.75f, -0.35f, 0.05f, 0.45f, 0.85f, 1.25f, 1.65f};
        for (int i = 0; i < 8; i++) {
            float side = i < 4 ? -1f : 1f;
            float y = yaw[i % 4];
            PartDefinition upper = root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(40, 0)
                            .addBox(-0.5f, -0.5f, -4, 1, 1, 8),
                    PartPose.offsetAndRotation(side * 2.5f, 8, -2, 0.55f, y, 0));
            upper.addOrReplaceChild("lower" + i, CubeListBuilder.create().texOffs(40, 10)
                    .addBox(-0.5f, -0.5f, -4, 1, 1, 8), PartPose.offsetAndRotation(0, 0, -4, -1.1f, 0, 0));
        }

        // 고치 5개: 어미 주변에 실에 매달려 있다
        float[][] cocoonPos = {
                {-3.5f, 14, -1}, {3.5f, 14, -1}, {-4.5f, 16, 3}, {4.5f, 16, 3}, {0, 18, 5}
        };
        for (int i = 0; i < 5; i++) {
            float[] cp = cocoonPos[i];
            PartDefinition c = root.addOrReplaceChild("cocoon" + i, CubeListBuilder.create().texOffs(0, 32)
                    .addBox(-1.5f, 0, -1.5f, 3, 5, 3), PartPose.offset(cp[0], cp[1], cp[2]));
            c.addOrReplaceChild("string" + i, CubeListBuilder.create().texOffs(34, 0)
                    .addBox(-0.5f, -8, -0.5f, 1, 8, 1), PartPose.offset(0, 0, 0));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        for (int i = 0; i < 8; i++) {
            legs[i].xRot = 0.55f + Mth.sin(t * 0.3f + i) * 0.05f;
            legs[i].yRot = (i < 4 ? -1f : 1f) * (0.35f + (i % 4) * 0.4f) + Mth.sin(t * 0.2f + i * 1.7f) * 0.04f;
        }
        for (int i = 0; i < 5; i++) {
            cocoonParts[i].visible = i < s.cocoons;
            if (cocoonParts[i].visible) {
                cocoonParts[i].zRot = Mth.sin(t * 0.08f + i * 1.3f) * 0.12f;
            }
        }
    }
}
