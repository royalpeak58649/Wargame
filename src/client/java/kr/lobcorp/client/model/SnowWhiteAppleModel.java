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

/** 백설공주의 사과. 한 입 베어 문 붉은 사과에서 초록 덩굴이 뻗어 나온다. */
public class SnowWhiteAppleModel extends EntityModel<SnowWhiteAppleModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("snow_white_apple"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
    }

    private final ModelPart apple, bite, stem, leaf, vine0, vine1, vine2, vine3;

    public SnowWhiteAppleModel(ModelPart root) {
        super(root);
        apple = root.getChild("apple");
        bite = root.getChild("bite");
        stem = root.getChild("stem");
        leaf = root.getChild("leaf");
        vine0 = root.getChild("vine0");
        vine1 = root.getChild("vine1");
        vine2 = root.getChild("vine2");
        vine3 = root.getChild("vine3");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 붉은 사과
        root.addOrReplaceChild("apple", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-7, -7, -7, 14, 14, 14), PartPose.offset(0, 15, 0));
        // 한 입 베어 문 자리
        root.addOrReplaceChild("bite", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3, -3, -8, 6, 6, 2), PartPose.offset(3, 12, 0));
        // 꼭지
        root.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(32, 32)
                .addBox(-0.5f, -4, -0.5f, 1, 4, 1), PartPose.offset(0, 8, 0));
        // 잎
        root.addOrReplaceChild("leaf", CubeListBuilder.create().texOffs(40, 32)
                .addBox(0, -1, -2, 5, 1, 4), PartPose.offset(0.5f, 6, 0));
        // 바닥으로 뻗은 덩굴
        root.addOrReplaceChild("vine0", CubeListBuilder.create().texOffs(0, 48)
                .addBox(0, -1, -1, 10, 1, 1), PartPose.offset(6, 23, 0));
        root.addOrReplaceChild("vine1", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-10, -1, -1, 10, 1, 1), PartPose.offset(-6, 23, 0));
        root.addOrReplaceChild("vine2", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-1, -1, 0, 1, 1, 10), PartPose.offset(0, 23, 6));
        root.addOrReplaceChild("vine3", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-1, -1, -10, 1, 1, 10), PartPose.offset(0, 23, -6));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        apple.yRot = Mth.sin(t * 0.02f) * 0.1f;
        apple.y = 15 + Mth.sin(t * 0.05f) * 0.2f;
        bite.yRot = apple.yRot;
        bite.y = apple.y - 3;
        stem.yRot = apple.yRot;
        leaf.zRot = Mth.sin(t * 0.06f) * 0.2f;
        // 덩굴은 탈출했을 때만 바닥을 기어 다닌다
        vine0.visible = s.breached;
        vine1.visible = s.breached;
        vine2.visible = s.breached;
        vine3.visible = s.breached;
        vine0.yRot = Mth.sin(t * 0.05f) * 0.3f;
        vine1.yRot = -Mth.sin(t * 0.05f + 1f) * 0.3f;
        vine2.yRot = Mth.sin(t * 0.05f + 2f) * 0.3f;
        vine3.yRot = -Mth.sin(t * 0.05f + 3f) * 0.3f;
    }
}
