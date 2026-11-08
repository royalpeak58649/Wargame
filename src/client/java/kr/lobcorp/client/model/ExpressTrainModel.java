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

/** 지옥행 급행열차. 매표소 지붕의 등불이 다 켜지면 열차가 도착한다. */
public class ExpressTrainModel extends EntityModel<ExpressTrainModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("express_train"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        /** 켜진 등불 수 (0-4) */
        public int lights;
    }

    private final ModelPart booth, roof, window, railLeft, railRight;
    private final ModelPart[] lamps = new ModelPart[4];
    private final ModelPart[] lampsLit = new ModelPart[4];

    public ExpressTrainModel(ModelPart root) {
        super(root);
        booth = root.getChild("booth");
        roof = root.getChild("roof");
        window = root.getChild("window");
        railLeft = root.getChild("rail_left");
        railRight = root.getChild("rail_right");
        for (int i = 0; i < 4; i++) {
            lamps[i] = root.getChild("lamp" + i);
            lampsLit[i] = root.getChild("lamp_lit" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 매표소 몸통 10x12x8 (u0 v0)
        root.addOrReplaceChild("booth", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, -12, -4, 10, 12, 8), PartPose.offset(0, 24, 0));
        // 지붕 판 12x2x10 (u0 v22)
        root.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-6, -14, -5, 12, 2, 10), PartPose.offset(0, 24, 0));
        // 앞면의 매표 창 6x4x1 (u0 v36)
        root.addOrReplaceChild("window", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-3, -9, -4.5f, 6, 4, 1), PartPose.offset(0, 24, 0));
        // 앞바닥의 레일 두 줄 2x1x14 (u0 v44 / u0 v60)
        root.addOrReplaceChild("rail_left", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-4, -1, -18, 2, 1, 14), PartPose.offset(0, 24, 0));
        root.addOrReplaceChild("rail_right", CubeListBuilder.create().texOffs(0, 60)
                .addBox(2, -1, -18, 2, 1, 14), PartPose.offset(0, 24, 0));
        // 지붕 위의 등불 네 개 (u32 v0 / u32 v8) — 꺼진 것과 켜진 것
        float[] lampX = {-4.5f, -1.5f, 1.5f, 4.5f};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("lamp" + i, CubeListBuilder.create().texOffs(32, 0)
                    .addBox(-1, -2, -1, 2, 2, 2), PartPose.offset(lampX[i], -14, 0));
            root.addOrReplaceChild("lamp_lit" + i, CubeListBuilder.create().texOffs(32, 8)
                    .addBox(-1.25f, -2.25f, -1.25f, 2.5f, 2.5f, 2.5f), PartPose.offset(lampX[i], -14, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        for (int i = 0; i < 4; i++) {
            lamps[i].visible = i >= s.lights;
            lampsLit[i].visible = i < s.lights;
        }
        // 등불이 다 켜지면 불빛이 일렁인다
        if (s.lights == 4) {
            float flicker = Mth.sin(t * 0.8f) * 0.1f;
            for (int i = 0; i < 4; i++) {
                lampsLit[i].y = flicker;
            }
        }
    }
}
