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

/** 침묵의 오케스트라. 작은 단상 위의 지휘자와 떠다니는 악사 구체들. */
public class SilentOrchestraModel extends EntityModel<SilentOrchestraModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("silent_orchestra"), "main");

    public static class State extends LivingEntityRenderState {
        public int movement; // 0 대기, 1..5 악장
    }

    private final ModelPart podium, body, head, armL, armR, baton;
    private final ModelPart[] orbs;

    public SilentOrchestraModel(ModelPart root) {
        super(root);
        podium = root.getChild("podium");
        body = root.getChild("body");
        head = body.getChild("head");
        armL = body.getChild("arm_l");
        armR = body.getChild("arm_r");
        baton = armR.getChild("baton");
        orbs = new ModelPart[] {
                root.getChild("orb_1"), root.getChild("orb_2"),
                root.getChild("orb_3"), root.getChild("orb_4")
        };
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 단상 (u0 v0, 12x2x12)
        root.addOrReplaceChild("podium", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6, 0, -6, 12, 2, 12),
                PartPose.offset(0, 22, 0));

        // 검은 연미복 몸통 (u0 v16, 8x12x4)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4, -12, -2, 8, 12, 4),
                PartPose.offset(0, 22, 0));

        // 하얀 머리 (u0 v32, 6x6x6)
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3, -6, -3, 6, 6, 6),
                PartPose.offset(0, -12, 0));

        // 왼팔 (u24 v16, 2x10x2)
        body.addOrReplaceChild("arm_l", CubeListBuilder.create()
                .texOffs(24, 16).addBox(-1, 0, -1, 2, 10, 2),
                PartPose.offset(5, -11, 0));
        // 오른팔 + 얇은 지휘봉 (u32 v16, 1x8x1)
        PartDefinition armR = body.addOrReplaceChild("arm_r", CubeListBuilder.create()
                .texOffs(24, 16).addBox(-1, 0, -1, 2, 10, 2),
                PartPose.offset(-5, -11, 0));
        armR.addOrReplaceChild("baton", CubeListBuilder.create()
                .texOffs(32, 16).addBox(-0.5f, 0, -0.5f, 1, 8, 1),
                PartPose.offset(0, 10, 0));

        // 떠다니는 악사 구체 네 개 (u0 v44, 4x4x4)
        for (int i = 0; i < 4; i++) {
            float a = (float) (Math.PI / 2 * i);
            root.addOrReplaceChild("orb_" + (i + 1), CubeListBuilder.create()
                    .texOffs(0, 44).addBox(-2, -2, -2, 4, 4, 4),
                    PartPose.offset(Mth.cos(a) * 9f, 12, Mth.sin(a) * 9f));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        boolean active = s.movement > 0;
        for (ModelPart orb : orbs) {
            orb.visible = active;
        }
        // 지휘봉을 든 팔이 악장이 오를수록 빠르게 흔들린다
        float wave = Mth.sin(t * (0.15f + 0.05f * s.movement));
        armR.xRot = -0.9f + wave * 0.5f;
        armR.zRot = -0.2f + wave * 0.2f;
        armL.xRot = 0.2f + Mth.sin(t * 0.1f) * 0.1f;
        baton.xRot = 0.1f;
        head.xRot = -0.1f + Mth.sin(t * 0.08f) * 0.1f;
        head.yRot = Mth.sin(t * 0.05f) * 0.3f;
        body.xRot = 0.05f;
        body.y = 22 + Mth.sin(t * 0.1f) * 0.2f;
        for (int i = 0; i < orbs.length; i++) {
            orbs[i].y = 12 + Mth.sin(t * 0.12f + i * 1.6f) * 1.2f;
            orbs[i].yRot = t * 0.05f + i;
        }
    }
}
