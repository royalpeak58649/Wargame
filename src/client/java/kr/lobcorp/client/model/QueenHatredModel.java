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

/**
 * O-01-04 증오의 여왕.
 * 격리 중: 분홍 드레스의 마법소녀. 히스테리면 고개를 떨구고 팔을 늘어뜨린다.
 * 탈출: 거대한 검은 뱀. 레이저를 쏠 때 아가리를 벌린다.
 */
public class QueenHatredModel extends EntityModel<QueenHatredModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("queen_of_hatred"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public boolean hysteric;
        public boolean laser;
    }

    private final ModelPart girl;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightTail;
    private final ModelPart leftTail;
    private final ModelPart wand;
    private final ModelPart snake;
    private final ModelPart snakeHead;
    private final ModelPart jaw;

    public QueenHatredModel(ModelPart root) {
        super(root);
        girl = root.getChild("girl");
        head = girl.getChild("head");
        rightArm = girl.getChild("right_arm");
        leftArm = girl.getChild("left_arm");
        rightTail = head.getChild("right_tail");
        leftTail = head.getChild("left_tail");
        wand = girl.getChild("wand");
        snake = root.getChild("snake");
        snakeHead = snake.getChild("snake_head");
        jaw = snakeHead.getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── 마법소녀 ──
        PartDefinition girl = root.addOrReplaceChild("girl", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        girl.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, -8, -1, 2, 8, 2), PartPose.offset(-2, 0, 0));
        girl.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1, -8, -1, 2, 8, 2), PartPose.offset(2, 0, 0));
        girl.addOrReplaceChild("dress", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-4, -6, -3, 8, 6, 6), PartPose.offset(0, -8, 0));
        girl.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 10)
                .addBox(-2.5f, -5, -1.5f, 5, 5, 3), PartPose.offset(0, -14, 0));
        girl.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(-3.5f, -18, 0));
        girl.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-1, 0, -1, 2, 6, 2), PartPose.offset(3.5f, -18, 0));

        PartDefinition head = girl.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, -19, 0));
        head.addOrReplaceChild("right_tail", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(-3, -4, 0));
        head.addOrReplaceChild("left_tail", CubeListBuilder.create().texOffs(24, 32)
                .addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(3, -4, 0));
        head.addOrReplaceChild("bow", CubeListBuilder.create().texOffs(32, 32)
                .addBox(-4, -8, -1, 8, 3, 2), PartPose.offset(0, 0, 0));

        PartDefinition wand = girl.addOrReplaceChild("wand", CubeListBuilder.create(), PartPose.offset(4.5f, -14, 0));
        wand.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-0.5f, -6, -0.5f, 1, 6, 1), PartPose.offset(0, 0, 0));
        wand.addOrReplaceChild("star", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.5f, -9, -0.5f, 3, 3, 1), PartPose.offset(0, 0, 0));

        // ── 뱀 ──
        PartDefinition snake = root.addOrReplaceChild("snake", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        snake.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 0, 0));
        snake.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, -5, -3));
        snake.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, -10, -5));
        snake.addOrReplaceChild("seg4", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, -15, -6));
        snake.addOrReplaceChild("seg5", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, -20, -6));

        PartDefinition snakeHead = snake.addOrReplaceChild("snake_head", CubeListBuilder.create().texOffs(24, 44)
                .addBox(-4, -7, -4, 8, 7, 8), PartPose.offset(0, -26, -6));
        snakeHead.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(24, 60)
                .addBox(-3, 0, -4, 6, 2, 8), PartPose.offset(0, 0, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;

        girl.visible = !s.breached;
        snake.visible = s.breached;

        if (!s.breached) {
            head.xRot = s.hysteric ? 0.55f : Mth.sin(t * 0.05f) * 0.04f;
            head.zRot = s.hysteric ? 0.18f : 0f;
            rightArm.xRot = s.hysteric ? 0.1f : -0.35f;
            leftArm.xRot = s.hysteric ? 0.1f : -0.35f;
            rightArm.zRot = s.hysteric ? 0.05f : 0.25f;
            leftArm.zRot = s.hysteric ? -0.05f : -0.25f;
            rightTail.xRot = Mth.sin(t * 0.06f) * 0.12f;
            leftTail.xRot = Mth.sin(t * 0.06f + 1.5f) * 0.12f;
            wand.xRot = s.hysteric ? 0.9f : -0.2f;
            return;
        }

        // 뱀: 몸을 크게 흔들며 솟아오른다
        snake.yRot = Mth.sin(t * 0.04f) * 0.25f;
        snakeHead.xRot = -0.15f + Mth.sin(t * 0.07f) * 0.08f;
        jaw.xRot = s.laser ? 0.9f : 0.05f;
    }
}
