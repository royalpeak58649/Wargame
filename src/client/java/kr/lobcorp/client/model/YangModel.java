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

/** 양. 눈이 하나뿐인 흰 잉어. 누군가 몸에 두르면 모습을 감추고, 생명이 다하면 흰 조각이 된다. */
public class YangModel extends EntityModel<YangModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("yang"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public boolean taken;
        public boolean pendant;
    }

    private final ModelPart body, tail, finTop, finBottom, eye, pendant;

    public YangModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        tail = root.getChild("tail");
        finTop = root.getChild("fin_top");
        finBottom = root.getChild("fin_bottom");
        eye = root.getChild("eye");
        pendant = root.getChild("pendant");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 흰 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, -3, -7, 6, 6, 14), PartPose.offset(0, 18, 0));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-4, -4, 0, 8, 8, 1), PartPose.offset(0, 18, 7));
        root.addOrReplaceChild("fin_top", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-0.5f, -4, -4, 1, 4, 8), PartPose.offset(0, 15, 0));
        root.addOrReplaceChild("fin_bottom", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-0.5f, 0, -4, 1, 4, 8), PartPose.offset(0, 21, 0));
        root.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-1.5f, -1.5f, -1, 3, 3, 1), PartPose.offset(0, 17, -7));
        // 조각: 태극의 흰 반쪽
        root.addOrReplaceChild("pendant", CubeListBuilder.create().texOffs(40, 8)
                .addBox(-2, -2, -0.5f, 4, 4, 1), PartPose.offset(0, 22, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        boolean hidden = s.taken && !s.breached && !s.pendant;
        body.visible = !s.pendant && !hidden;
        tail.visible = !s.pendant && !hidden;
        finTop.visible = !s.pendant && !hidden;
        finBottom.visible = !s.pendant && !hidden;
        eye.visible = !s.pendant && !hidden;
        pendant.visible = s.pendant;

        if (s.pendant) {
            pendant.yRot = -t * 0.05f;
            pendant.y = 22 + Mth.sin(t * 0.1f) * 0.2f;
            return;
        }
        body.yRot = Mth.sin(t * 0.08f) * 0.1f;
        tail.yRot = Mth.sin(t * 0.18f) * 0.4f;
        finTop.zRot = Mth.sin(t * 0.15f) * 0.15f;
        finBottom.zRot = -Mth.sin(t * 0.15f) * 0.15f;
        body.y = s.breached ? 18 + Mth.sin(t * 0.06f) * 0.5f : 18;
    }
}
