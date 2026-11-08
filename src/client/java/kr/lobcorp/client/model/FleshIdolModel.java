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

/** 살점 우상. 누군가 제단 앞에 무릎을 꿇으면 우상이 느리게 꿈틀거린다. */
public class FleshIdolModel extends EntityModel<FleshIdolModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("flesh_idol"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        /** 누군가 기도하고 있는가 */
        public boolean praying;
    }

    private final ModelPart altar, lumpLow, lumpMid, lumpTop, head, armLeft, armRight;
    private final ModelPart[] spikes = new ModelPart[3];

    public FleshIdolModel(ModelPart root) {
        super(root);
        altar = root.getChild("altar");
        lumpLow = root.getChild("lump_low");
        lumpMid = root.getChild("lump_mid");
        lumpTop = root.getChild("lump_top");
        head = root.getChild("head");
        armLeft = root.getChild("arm_left");
        armRight = root.getChild("arm_right");
        for (int i = 0; i < 3; i++) {
            spikes[i] = root.getChild("spike" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 낮은 돌 제단 12x4x8 (u0 v0)
        root.addOrReplaceChild("altar", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6, -4, -4, 12, 4, 8), PartPose.offset(0, 24, 0));
        // 제단 위에 쌓아 올린 살점 덩어리 세 개 (u0 v16 / u0 v28 / u0 v38)
        root.addOrReplaceChild("lump_low", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-4, -5, -3, 8, 5, 6), PartPose.offset(0, 20, 0));
        root.addOrReplaceChild("lump_mid", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-3, -4, -2.5f, 6, 4, 5), PartPose.offset(0, 15, 0));
        root.addOrReplaceChild("lump_top", CubeListBuilder.create().texOffs(0, 38)
                .addBox(-2, -3, -2, 4, 3, 4), PartPose.offset(0, 11, 0));
        // 맨 위의 머리 덩어리 4x4x4 (u0 v46)
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 46)
                .addBox(-2, -4, -2, 4, 4, 4), PartPose.offset(0, 8, 0));
        // 치켜든 두 팔 토막 2x4x2 (u16 v46 / u24 v46)
        root.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(16, 46)
                .addBox(-1, -4, -1, 2, 4, 2), PartPose.offset(-3, 9, 0));
        root.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(24, 46)
                .addBox(-1, -4, -1, 2, 4, 2), PartPose.offset(3, 9, 0));
        // 제단에 박힌 뼈 가시 1x3x1 (u32 v46 / u36 v46 / u40 v46)
        float[] spikeX = {-5, 0, 5};
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("spike" + i, CubeListBuilder.create().texOffs(32 + i * 4, 46)
                    .addBox(-0.5f, -3, -0.5f, 1, 3, 1), PartPose.offset(spikeX[i], 20, -3.5f));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 기도 중이면 우상이 훨씬 빠르게 꿈틀거린다
        float pulse = Mth.sin(t * (s.praying ? 0.3f : 0.08f)) * 0.5f;
        lumpMid.y = 15 + pulse * 0.5f;
        lumpTop.y = 11 + pulse;
        head.y = 8 + pulse * 1.5f;
        armLeft.y = 9 + pulse * 1.5f;
        armRight.y = 9 + pulse * 1.5f;
        armLeft.zRot = 0.3f + pulse * 0.1f;
        armRight.zRot = -0.3f - pulse * 0.1f;
        for (int i = 0; i < 3; i++) {
            spikes[i].xRot = Mth.sin(t * 0.05f + i) * 0.1f;
        }
    }
}
