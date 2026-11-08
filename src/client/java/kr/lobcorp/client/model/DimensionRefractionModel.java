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

/** 차원 굴절 변이체. 몸통 없이 가슴 높이의 빈 중심을 느슨한 고리로 떠도는 수정 파편들. */
public class DimensionRefractionModel extends EntityModel<DimensionRefractionModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("dimension_refraction"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private static final int SHARDS = 9;

    private final ModelPart ring, core;
    private final ModelPart[] shards = new ModelPart[SHARDS];

    public DimensionRefractionModel(ModelPart root) {
        super(root);
        ring = root.getChild("ring");
        core = ring.getChild("core");
        for (int i = 0; i < SHARDS; i++) {
            shards[i] = ring.getChild("shard" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 빈 중심을 감싸는 고리 (가슴 높이)
        PartDefinition ringPart = root.addOrReplaceChild("ring", CubeListBuilder.create(), PartPose.offset(0, 12, 0));
        // 중심의 작은 핵 (u0 v0, 2x2x2)
        ringPart.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, 0, 0));
        // 고리를 따라 떠 있는 얇은 수정 파편 아홉 개
        float[] radius = {7.0f, 6.0f, 7.5f, 6.5f, 7.0f, 6.0f, 7.5f, 6.5f, 7.0f};
        float[] height = {-2.0f, 1.5f, -1.0f, 2.5f, 0.0f, -2.5f, 1.0f, 2.0f, -1.5f};
        for (int i = 0; i < SHARDS; i++) {
            float a = (float) (Math.PI * 2.0 * i / SHARDS);
            float x = Mth.cos(a) * radius[i];
            float z = Mth.sin(a) * radius[i];
            CubeListBuilder cube;
            if (i % 3 == 0) {
                // 1x6x1 (u0 v8)
                cube = CubeListBuilder.create().texOffs(0, 8).addBox(-0.5f, -3, -0.5f, 1, 6, 1);
            } else if (i % 3 == 1) {
                // 2x5x1 (u8 v8)
                cube = CubeListBuilder.create().texOffs(8, 8).addBox(-1, -2.5f, -0.5f, 2, 5, 1);
            } else {
                // 1x4x2 (u16 v8)
                cube = CubeListBuilder.create().texOffs(16, 8).addBox(-0.5f, -2, -1, 1, 4, 2);
            }
            ringPart.addOrReplaceChild("shard" + i, cube, PartPose.offsetAndRotation(x, height[i], z, 0, -a, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 고리 전체가 천천히 돈다
        ring.yRot = t * 0.02f;
        // 핵은 아주 미세하게 맥동한다
        core.yRot = Mth.sin(t * 0.05f) * 0.3f;
        // 파편마다 제 높이에서 위아래로 떠다니고 살짝 흔들린다
        for (int i = 0; i < SHARDS; i++) {
            ModelPart shard = shards[i];
            shard.y = Mth.sin(t * 0.1f + i) * 1.2f;
            shard.xRot = Mth.sin(t * 0.07f + i * 1.7f) * 0.25f;
            shard.zRot = Mth.cos(t * 0.06f + i * 1.3f) * 0.2f;
        }
    }
}
