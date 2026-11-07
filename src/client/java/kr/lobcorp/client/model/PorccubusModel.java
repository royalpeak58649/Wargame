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

/** 가시 돋친 초록 줄기가 S자로 솟아오르고, 끝에 붉은 꽃 머리가 달린 포큐버스. */
public class PorccubusModel extends EntityModel<PorccubusModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("porccubus"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
    }

    private static final int SEGMENTS = 5;
    private final ModelPart[] stem = new ModelPart[SEGMENTS];
    private final ModelPart head;

    public PorccubusModel(ModelPart root) {
        super(root);
        for (int i = 0; i < SEGMENTS; i++) stem[i] = root.getChild("stem" + i);
        head = root.getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 줄기: 3x3x6 조각을 위로 이어 붙인다. 각 조각은 자기 아래 조각의 꼭대기에 붙는다.
        for (int i = 0; i < SEGMENTS; i++) {
            PartDefinition seg = root.addOrReplaceChild("stem" + i, CubeListBuilder.create().texOffs(0, i * 6)
                    .addBox(-1.5f, -6, -1.5f, 3, 6, 3), PartPose.offset(0, 24 - i * 6, 0));
            if (i == SEGMENTS - 1) {
                // 꽃 머리: 노란 중심 + 네 장의 붉은 꽃잎
                PartDefinition flower = seg.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-2, -2, -1, 4, 4, 2)
                        .texOffs(24, 8).addBox(-2, -3, -2, 4, 1, 4)
                        .texOffs(24, 8).addBox(-2, 2, -2, 4, 1, 4)
                        .texOffs(24, 8).addBox(-3, -2, -2, 1, 4, 4)
                        .texOffs(24, 8).addBox(2, -2, -2, 1, 4, 4)
                        .texOffs(40, 0).addBox(-1, -1, -2, 1, 1, 1)
                        .texOffs(40, 0).addBox(0, -1, -2, 1, 1, 1),
                        PartPose.offset(0, -6, 0));
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float pitch = s.breached ? 0.05f : 0.22f; // 탈출하면 줄기가 곧게 뻗는다
        for (int i = 0; i < SEGMENTS; i++) {
            ModelPart seg = stem[i];
            float sway = Mth.sin(t * 0.05f + i) * 0.15f;
            seg.yRot = sway;
            seg.xRot = (i == 0 ? 0f : pitch) + Mth.cos(t * 0.04f + i) * 0.06f;
        }
        head.yRot = Mth.sin(t * 0.03f) * 0.2f;
        head.xRot = s.breached ? 0.35f : 0.1f; // 탈출하면 머리를 들어 바라본다
    }
}
