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

/** 거꾸로 가는 시계. 꼭대기의 등불을 다 밝히면 하루 동안 사라진다. */
public class BackwardClockModel extends EntityModel<BackwardClockModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("backward_clock"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        /** 켜진 등불 수 (0-4) */
        public int lights;
        /** 오늘 이미 멈춘 시계인가 */
        public boolean gone;
    }

    private final ModelPart base, caseBody, head, face, hourHand, minuteHand, pendulum;
    private final ModelPart[] lamps = new ModelPart[4];
    private final ModelPart[] lampsLit = new ModelPart[4];

    public BackwardClockModel(ModelPart root) {
        super(root);
        base = root.getChild("base");
        caseBody = root.getChild("case");
        head = root.getChild("head");
        face = root.getChild("face");
        hourHand = root.getChild("hour");
        minuteHand = root.getChild("minute");
        pendulum = root.getChild("pendulum");
        for (int i = 0; i < 4; i++) {
            lamps[i] = root.getChild("lamp" + i);
            lampsLit[i] = root.getChild("lamp_lit" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 밑동 10x4x6 (u0 v0)
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, -4, -3, 10, 4, 6), PartPose.offset(0, 24, 0));
        // 긴 몸통 8x18x5 (u0 v12)
        root.addOrReplaceChild("case", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-4, -18, -2.5f, 8, 18, 5), PartPose.offset(0, 20, 0));
        // 머리 10x8x6 (u0 v36)
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-5, -8, -3, 10, 8, 6), PartPose.offset(0, 2, 0));
        // 앞면의 납작한 문자판 8x8x1 (u28 v0)
        root.addOrReplaceChild("face", CubeListBuilder.create().texOffs(28, 0)
                .addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offset(0, -2, -3));
        // 시침 1x3x1 (u28 v12)
        root.addOrReplaceChild("hour", CubeListBuilder.create().texOffs(28, 12)
                .addBox(-0.5f, -3, -0.5f, 1, 3, 1), PartPose.offset(0, -2, -3.5f));
        // 분침 1x4x1 (u34 v12)
        root.addOrReplaceChild("minute", CubeListBuilder.create().texOffs(34, 12)
                .addBox(-0.5f, -4, -0.5f, 1, 4, 1), PartPose.offset(0, -2, -3.5f));
        // 몸통 안의 진자 2x8x1 (u28 v20)
        root.addOrReplaceChild("pendulum", CubeListBuilder.create().texOffs(28, 20)
                .addBox(-1, 0, -0.5f, 2, 8, 1), PartPose.offset(0, 4, 0));
        // 머리 위의 등불 네 개 (u40 v0 / u40 v8) — 꺼진 것과 켜진 것
        float[] lampX = {-3.5f, -1.5f, 1.5f, 3.5f};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("lamp" + i, CubeListBuilder.create().texOffs(40, 0)
                    .addBox(-1, -2, -1, 2, 2, 2), PartPose.offset(lampX[i], -6, 0));
            root.addOrReplaceChild("lamp_lit" + i, CubeListBuilder.create().texOffs(40, 8)
                    .addBox(-1.25f, -2.25f, -1.25f, 2.5f, 2.5f, 2.5f), PartPose.offset(lampX[i], -6, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        boolean show = !s.gone;
        base.visible = show;
        caseBody.visible = show;
        head.visible = show;
        face.visible = show;
        hourHand.visible = show;
        minuteHand.visible = show;
        pendulum.visible = show;
        for (int i = 0; i < 4; i++) {
            lamps[i].visible = show && i >= s.lights;
            lampsLit[i].visible = show && i < s.lights;
        }
        if (!show) {
            return;
        }
        // 바늘이 거꾸로 돈다
        minuteHand.zRot = -t * 0.05f;
        hourHand.zRot = -t * 0.005f;
        // 진자가 흔들린다
        pendulum.zRot = Mth.sin(t * 0.1f) * 0.3f;
    }
}
