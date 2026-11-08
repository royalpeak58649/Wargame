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

/** 살덩어리 둥지. 구멍 사이로 가느다란 뱀들이 꿈틀거린다. */
public class NakedNestModel extends EntityModel<NakedNestModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("naked_nest"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart mass, lump, lump2, lump3;
    private final ModelPart[] serpents;

    public NakedNestModel(ModelPart root) {
        super(root);
        mass = root.getChild("mass");
        lump = root.getChild("lump");
        lump2 = root.getChild("lump2");
        lump3 = root.getChild("lump3");
        serpents = new ModelPart[] {
                root.getChild("serpent0"),
                root.getChild("serpent1"),
                root.getChild("serpent2"),
                root.getChild("serpent3"),
                root.getChild("serpent4"),
        };
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 바닥에 퍼진 살덩어리 본체
        root.addOrReplaceChild("mass", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-10, 0, -10, 20, 8, 20), PartPose.offset(0, 16, 0));
        // 위로 솟은 혹
        root.addOrReplaceChild("lump", CubeListBuilder.create().texOffs(0, 30)
                .addBox(-7, -6, -7, 14, 6, 14), PartPose.offset(0, 16, 0));
        // 옆으로 삐져나온 혹
        root.addOrReplaceChild("lump2", CubeListBuilder.create().texOffs(0, 52)
                .addBox(-5, -4, -5, 10, 4, 10), PartPose.offset(-6, 16, 4));
        // 뒤쪽 혹
        root.addOrReplaceChild("lump3", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-4, -3, -4, 8, 3, 8), PartPose.offset(6, 16, -5));
        // 구멍들: 살덩어리 위에 박힌 작고 어두운 상자
        root.addOrReplaceChild("hole0", CubeListBuilder.create().texOffs(44, 14)
                .addBox(-2, -1, -2, 4, 1, 4), PartPose.offset(-3, 10, 2));
        root.addOrReplaceChild("hole1", CubeListBuilder.create().texOffs(44, 14)
                .addBox(-2, -1, -2, 4, 1, 4), PartPose.offset(4, 10, -3));
        root.addOrReplaceChild("hole2", CubeListBuilder.create().texOffs(44, 14)
                .addBox(-1.5f, -1, -1.5f, 3, 1, 3), PartPose.offset(0, 12, 6));
        root.addOrReplaceChild("hole3", CubeListBuilder.create().texOffs(44, 14)
                .addBox(-1.5f, -1, -1.5f, 3, 1, 3), PartPose.offset(-7, 12, -4));
        // 구멍에서 뻗어 나온 가느다란 뱀들
        root.addOrReplaceChild("serpent0", CubeListBuilder.create().texOffs(44, 20)
                .addBox(-1, -12, -1, 2, 12, 2), PartPose.offset(-3, 10, 2));
        root.addOrReplaceChild("serpent1", CubeListBuilder.create().texOffs(44, 20)
                .addBox(-1, -14, -1, 2, 14, 2), PartPose.offset(4, 10, -3));
        root.addOrReplaceChild("serpent2", CubeListBuilder.create().texOffs(44, 20)
                .addBox(-1, -10, -1, 2, 10, 2), PartPose.offset(0, 12, 6));
        root.addOrReplaceChild("serpent3", CubeListBuilder.create().texOffs(44, 20)
                .addBox(-1, -11, -1, 2, 11, 2), PartPose.offset(-7, 12, -4));
        root.addOrReplaceChild("serpent4", CubeListBuilder.create().texOffs(44, 20)
                .addBox(-1, -9, -1, 2, 9, 2), PartPose.offset(7, 13, 5));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 살덩어리가 느리게 부풀었다 꺼진다
        float breathe = Mth.sin(t * 0.06f) * 0.04f;
        mass.xScale = 1f + breathe;
        mass.zScale = 1f + breathe;
        lump.yRot = Mth.sin(t * 0.04f) * 0.08f;
        lump2.yRot = Mth.sin(t * 0.05f + 1.1f) * 0.1f;
        lump3.yRot = Mth.sin(t * 0.05f + 2.3f) * 0.1f;
        // 뱀들이 각자 다른 위상으로 흔들린다
        for (int i = 0; i < serpents.length; i++) {
            ModelPart p = serpents[i];
            p.zRot = Mth.sin(t * 0.15f + i) * 0.35f;
            p.xRot = Mth.sin(t * 0.15f + i + 1.7f) * 0.25f;
        }
    }
}
