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

/** 밤하늘을 뭉친 검은 구체. 표면에 하트 반점, 네 개의 촉수, 머리 위에 떠 있는 낙서 같은 분홍 하트. */
public class FragmentModel extends EntityModel<FragmentModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("fragment_of_universe"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public boolean singing;
    }

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart heart;
    private final ModelPart[] tentacles = new ModelPart[4];
    private final ModelPart[] tentacleTips = new ModelPart[4];

    public FragmentModel(ModelPart root) {
        super(root);
        this.root = root;
        core = root.getChild("core");
        heart = root.getChild("heart");
        for (int i = 0; i < 4; i++) {
            tentacles[i] = root.getChild("tentacle" + i);
            tentacleTips[i] = tentacles[i].getChild("tip");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 검은 구체 (8x8x8)
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, 12, 0));

        // 떠 있는 분홍 하트: 3x3x1 두 개 + 2x2x1 끝
        PartDefinition heart = root.addOrReplaceChild("heart", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-3, -1, 0, 3, 3, 1)
                .texOffs(32, 0).addBox(0, -1, 0, 3, 3, 1)
                .texOffs(40, 0).addBox(-1, 2, 0, 2, 2, 1), PartPose.offset(0, 2, 0));

        // 촉수 네 개: 위쪽 1.5x5x1.5 + 아래쪽 1.5x5x1.5
        float[][] spots = {{-4.5f, -4.5f}, {4.5f, -4.5f}, {-4.5f, 4.5f}, {4.5f, 4.5f}};
        for (int i = 0; i < 4; i++) {
            PartDefinition t = root.addOrReplaceChild("tentacle" + i, CubeListBuilder.create()
                    .texOffs(0, 16).addBox(-0.75f, 0, -0.75f, 1.5f, 5, 1.5f),
                    PartPose.offset(spots[i][0], 14, spots[i][1]));
            t.addOrReplaceChild("tip", CubeListBuilder.create()
                    .texOffs(8, 16).addBox(-0.75f, 0, -0.75f, 1.5f, 5, 1.5f),
                    PartPose.offset(0, 5, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;

        if (s.breached) {
            // 탈출: 구체가 위로 솟구치고 촉수가 사방으로 뻗는다
            core.y = 10f + Mth.sin(t * 0.08f) * 0.8f;
            core.yScale = 1.4f;
            core.xScale = 0.85f;
            core.zScale = 0.85f;
            for (int i = 0; i < 4; i++) {
                float phase = i * Mth.HALF_PI;
                tentacles[i].xRot = -0.9f + Mth.sin(t * 0.25f + phase) * 0.35f;
                tentacles[i].zRot = Mth.cos(t * 0.22f + phase) * 0.5f;
                tentacleTips[i].xRot = 0.6f + Mth.sin(t * 0.3f + phase) * 0.4f;
            }
        } else {
            // 격리 중: 낮게 떠서 느릿하게 흔들린다
            core.y = 12f + Mth.sin(t * 0.04f) * 0.6f;
            core.yScale = 1f;
            core.xScale = 1f;
            core.zScale = 1f;
            for (int i = 0; i < 4; i++) {
                float phase = i * Mth.HALF_PI;
                tentacles[i].xRot = 0.15f + Mth.sin(t * 0.05f + phase) * 0.12f;
                tentacles[i].zRot = Mth.cos(t * 0.045f + phase) * 0.1f;
                tentacleTips[i].xRot = 0.25f + Mth.sin(t * 0.06f + phase) * 0.15f;
            }
        }

        // 떠 있는 하트
        float bob = Mth.sin(t * 0.1f) * 0.5f;
        heart.y = (s.breached ? 1.5f : 2f) + bob;
        if (s.singing) {
            heart.yScale = 1.6f;
            heart.xScale = 1.6f;
            heart.zScale = 1.6f;
            heart.yRot = t * 0.25f;
        } else {
            heart.yScale = 1f;
            heart.xScale = 1f;
            heart.zScale = 1f;
            heart.yRot = Mth.sin(t * 0.03f) * 0.2f;
        }
    }
}
