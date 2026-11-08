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

/** 음. 눈이 하나뿐인 검은 잉어. 생명이 다하면 작은 검은 조각(태극의 반쪽)이 된다. */
public class YinModel extends EntityModel<YinModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("yin"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public boolean pendant;
    }

    private final ModelPart body, tail, finTop, finBottom, eye, pendant;

    public YinModel(ModelPart root) {
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
        // 검은 몸통
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, -3, -7, 6, 6, 14), PartPose.offset(0, 18, 0));
        // 꼬리지느러미
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-4, -4, 0, 8, 8, 1), PartPose.offset(0, 18, 7));
        // 등지느러미
        root.addOrReplaceChild("fin_top", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-0.5f, -4, -4, 1, 4, 8), PartPose.offset(0, 15, 0));
        // 배지느러미
        root.addOrReplaceChild("fin_bottom", CubeListBuilder.create().texOffs(20, 20)
                .addBox(-0.5f, 0, -4, 1, 4, 8), PartPose.offset(0, 21, 0));
        // 하나뿐인 눈
        root.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-1.5f, -1.5f, -1, 3, 3, 1), PartPose.offset(0, 17, -7));
        // 조각: 태극의 검은 반쪽
        root.addOrReplaceChild("pendant", CubeListBuilder.create().texOffs(40, 8)
                .addBox(-2, -2, -0.5f, 4, 4, 1), PartPose.offset(0, 22, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        body.visible = !s.pendant;
        tail.visible = !s.pendant;
        finTop.visible = !s.pendant;
        finBottom.visible = !s.pendant;
        eye.visible = !s.pendant;
        pendant.visible = s.pendant;

        if (s.pendant) {
            pendant.yRot = t * 0.05f;
            pendant.y = 22 + Mth.sin(t * 0.1f) * 0.2f;
            return;
        }
        // 물속을 헤엄치듯 흔들린다
        body.yRot = Mth.sin(t * 0.1f) * 0.12f;
        tail.yRot = Mth.sin(t * 0.25f) * 0.5f;
        finTop.zRot = Mth.sin(t * 0.2f) * 0.2f;
        finBottom.zRot = -Mth.sin(t * 0.2f) * 0.2f;
        eye.xRot = 0;
        if (s.breached) {
            body.y = 18 + Mth.sin(t * 0.08f) * 0.6f;
        } else {
            body.y = 18;
        }
    }
}
