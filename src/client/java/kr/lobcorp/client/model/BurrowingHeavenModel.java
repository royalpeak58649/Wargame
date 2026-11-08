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

/** 땅을 파는 천국. 창백한 나무 몸통에 뿌리, 꼭대기에는 핏발 선 눈 덩어리. */
public class BurrowingHeavenModel extends EntityModel<BurrowingHeavenModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("burrowing_heaven"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
    }

    private final ModelPart trunk, knot, eye1, eye2, eye3, eye4, eye5, eye6, eye7, eye8;

    public BurrowingHeavenModel(ModelPart root) {
        super(root);
        trunk = root.getChild("trunk");
        knot = trunk.getChild("knot");
        eye1 = knot.getChild("eye1");
        eye2 = knot.getChild("eye2");
        eye3 = knot.getChild("eye3");
        eye4 = knot.getChild("eye4");
        eye5 = knot.getChild("eye5");
        eye6 = knot.getChild("eye6");
        eye7 = knot.getChild("eye7");
        eye8 = knot.getChild("eye8");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 몸통: 창백한 마디 세 개 (u0 v0 / u0 v16 / u0 v32, 각 4x10x4)
        PartDefinition trunkPart = root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -10, -2, 4, 10, 4), PartPose.offset(0, 24, 0));
        trunkPart.addOrReplaceChild("trunk_mid", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, -10, -2, 4, 10, 4), PartPose.offset(0, -10, 0));
        trunkPart.addOrReplaceChild("trunk_top", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2, -10, -2, 4, 10, 4), PartPose.offset(0, -20, 0));
        // 뿌리 네 개 (u16 v0 / u16 v8 / u16 v16 / u16 v24, 각 3x2x8)
        root.addOrReplaceChild("root_n", CubeListBuilder.create().texOffs(16, 0)
                .addBox(-1.5f, -2, 0, 3, 2, 8), PartPose.offset(0, 24, 0));
        root.addOrReplaceChild("root_s", CubeListBuilder.create().texOffs(16, 8)
                .addBox(-1.5f, -2, -8, 3, 2, 8), PartPose.offset(0, 24, 0));
        root.addOrReplaceChild("root_e", CubeListBuilder.create().texOffs(16, 16)
                .addBox(0, -2, -1.5f, 8, 2, 3), PartPose.offset(0, 24, 0));
        root.addOrReplaceChild("root_w", CubeListBuilder.create().texOffs(16, 24)
                .addBox(-8, -2, -1.5f, 8, 2, 3), PartPose.offset(0, 24, 0));
        // 눈 덩어리의 중심 매듭 (u40 v0, 4x4x4)
        PartDefinition knotPart = trunkPart.addOrReplaceChild("knot", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-2, -4, -2, 4, 4, 4), PartPose.offset(0, -30, 0));
        // 매듭을 둘러싼 눈 여덟 개 (3x3x3)
        knotPart.addOrReplaceChild("eye1", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(0, -2, -4));
        knotPart.addOrReplaceChild("eye2", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(4, -2, 0));
        knotPart.addOrReplaceChild("eye3", CubeListBuilder.create().texOffs(32, 48)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(0, -2, 4));
        knotPart.addOrReplaceChild("eye4", CubeListBuilder.create().texOffs(48, 48)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(-4, -2, 0));
        knotPart.addOrReplaceChild("eye5", CubeListBuilder.create().texOffs(0, 56)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(3, 2, -3));
        knotPart.addOrReplaceChild("eye6", CubeListBuilder.create().texOffs(16, 56)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(3, 2, 3));
        knotPart.addOrReplaceChild("eye7", CubeListBuilder.create().texOffs(32, 56)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(-3, 2, 3));
        knotPart.addOrReplaceChild("eye8", CubeListBuilder.create().texOffs(48, 56)
                .addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.offset(-3, 2, -3));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 눈 덩어리는 천천히 좌우를 살핀다
        knot.yRot = Mth.sin(t * 0.03f) * 0.4f;
        knot.xRot = Mth.sin(t * 0.017f) * 0.1f;
        // 눈알마다 조금씩 떨린다
        eye1.yRot = Mth.sin(t * 0.31f) * 0.12f;
        eye2.yRot = Mth.sin(t * 0.27f + 1.1f) * 0.12f;
        eye3.yRot = Mth.sin(t * 0.35f + 2.2f) * 0.12f;
        eye4.yRot = Mth.sin(t * 0.23f + 3.3f) * 0.12f;
        eye5.yRot = Mth.sin(t * 0.29f + 4.4f) * 0.12f;
        eye6.yRot = Mth.sin(t * 0.33f + 5.5f) * 0.12f;
        eye7.yRot = Mth.sin(t * 0.25f + 0.7f) * 0.12f;
        eye8.yRot = Mth.sin(t * 0.37f + 1.9f) * 0.12f;
        eye1.xRot = Mth.sin(t * 0.41f) * 0.08f;
        eye3.xRot = Mth.sin(t * 0.43f + 1.5f) * 0.08f;
        eye5.xRot = Mth.sin(t * 0.39f + 3.0f) * 0.08f;
        eye7.xRot = Mth.sin(t * 0.45f + 4.5f) * 0.08f;
        // 몸통은 아주 미세하게 흔들린다
        trunk.zRot = Mth.sin(t * 0.02f) * 0.03f;
    }
}
