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

/** 창백하게 뒤틀린 나무. 가지마다 하얀 봉오리가 달리고, 다 열리면 해골이 맺힌다. */
public class ParasiteTreeModel extends EntityModel<ParasiteTreeModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("parasite_tree"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        /** 열린 봉오리 수 (0-5) */
        public int bulbs;
    }

    private final ModelPart trunk, trunk_top, branch_right, branch_left, branch_back;
    private final ModelPart[] bulbs = new ModelPart[5];
    private final ModelPart[] skulls = new ModelPart[5];

    public ParasiteTreeModel(ModelPart root) {
        super(root);
        trunk = root.getChild("trunk");
        trunk_top = root.getChild("trunk_top");
        branch_right = root.getChild("branch_right");
        branch_left = root.getChild("branch_left");
        branch_back = root.getChild("branch_back");
        for (int i = 0; i < 5; i++) {
            bulbs[i] = root.getChild("bulb" + i);
            skulls[i] = root.getChild("skull" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 뒤틀린 밑동
        root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -16, -4, 8, 16, 8), PartPose.offset(0, 24, 0));
        // 비뚤어진 상체
        root.addOrReplaceChild("trunk_top", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-3, -14, -3, 6, 14, 6), PartPose.offset(1, 8, 0));
        // 가지: 오른쪽
        root.addOrReplaceChild("branch_right", CubeListBuilder.create().texOffs(24, 0)
                .addBox(0, -1, -1, 10, 2, 2), PartPose.offset(3, 2, 0));
        // 가지: 왼쪽
        root.addOrReplaceChild("branch_left", CubeListBuilder.create().texOffs(24, 4)
                .addBox(-10, -1, -1, 10, 2, 2), PartPose.offset(-2, 6, 0));
        // 가지: 뒤쪽
        root.addOrReplaceChild("branch_back", CubeListBuilder.create().texOffs(24, 8)
                .addBox(-1, -1, 0, 2, 2, 8), PartPose.offset(0, 10, 2));
        // 봉오리와 해골: 가지 끝마다 하나씩
        float[][] spots = {
                {12, 3, 0}, {-11, 7, 0}, {0, 11, 9}, {6, 0, 0}, {-6, 4, 0},
        };
        for (int i = 0; i < 5; i++) {
            float x = spots[i][0], y = spots[i][1], z = spots[i][2];
            root.addOrReplaceChild("bulb" + i, CubeListBuilder.create().texOffs(24, 20)
                    .addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offset(x, y, z));
            PartDefinition skull = root.addOrReplaceChild("skull" + i, CubeListBuilder.create().texOffs(40, 20)
                    .addBox(-2, 0, -2, 4, 4, 4), PartPose.offset(x, y, z));
            // 해골의 눈구멍
            skull.addOrReplaceChild("eye" + i, CubeListBuilder.create().texOffs(40, 30)
                    .addBox(-1.5f, 1, -2.5f, 1, 1, 1)
                    .addBox(0.5f, 1, -2.5f, 1, 1, 1), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        // 가지가 아주 느리게 흔들린다
        branch_right.zRot = Mth.sin(t * 0.04f) * 0.08f;
        branch_left.zRot = -Mth.sin(t * 0.04f + 0.8f) * 0.08f;
        branch_back.xRot = Mth.sin(t * 0.04f + 1.6f) * 0.06f;
        trunk_top.zRot = Mth.sin(t * 0.04f) * 0.03f;
        trunk.zRot = trunk_top.zRot * 0.5f;
        // 봉오리가 다 열리면 해골이 맺힌다
        for (int i = 0; i < 5; i++) {
            bulbs[i].visible = s.bulbs < 5 && i < s.bulbs;
            skulls[i].visible = s.bulbs >= 5;
            bulbs[i].xRot = Mth.sin(t * 0.04f + i) * 0.1f;
            skulls[i].xRot = Mth.sin(t * 0.04f + i) * 0.1f;
        }
    }
}
