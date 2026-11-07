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

/** 가시관에 묶인 해골 + 십자가. 앞면 = -Z */
public class OneSinModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("one_sin"), "main");

    private final ModelPart body;
    private final ModelPart jaw;

    public OneSinModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = body.getChild("skull").getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // y=24 이 지면. 전체가 지면에서 살짝 떠 있다.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 24, 0));

        body.addOrReplaceChild("cross_v", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.5f, -40, -1, 3, 30, 2), PartPose.ZERO);
        body.addOrReplaceChild("cross_h", CubeListBuilder.create().texOffs(12, 0)
                .addBox(-9, -33, -1, 18, 3, 2), PartPose.ZERO);

        PartDefinition skull = body.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(12, 8)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, -29, -1.5f));
        skull.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(12, 24)
                .addBox(-3, 0, -5, 6, 2, 5), PartPose.offset(0, 0, 1.5f));

        // 가시관: 해골 이마를 두르는 사각 고리 + 가시
        PartDefinition crown = skull.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(36, 24).addBox(-5, -7, -5, 10, 2, 1)
                .texOffs(36, 24).addBox(-5, -7, 4, 10, 2, 1)
                .texOffs(36, 28).addBox(-5, -7, -4, 1, 2, 8)
                .texOffs(36, 28).addBox(4, -7, -4, 1, 2, 8), PartPose.ZERO);
        float[][] thorns = {{-4, -8, -6}, {0, -9, -6}, {3, -8, -6}, {-6, -8, -2}, {5, -9, 1}, {-2, -9, 5}, {2, -8, 5}, {-6, -6, 2}, {5, -6, -3}};
        CubeListBuilder t = CubeListBuilder.create();
        for (float[] th : thorns) t.texOffs(0, 40).addBox(th[0], th[1], th[2], 1, 1, 1);
        crown.addOrReplaceChild("thorns", t, PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        body.y = 24 + Mth.sin(t * 0.06f) * 1.2f;
        body.zRot = Mth.sin(t * 0.03f) * 0.03f;
        // 가끔 이빨을 딱딱 부딪친다
        float clack = Mth.sin(t * 0.9f);
        jaw.xRot = (Mth.sin(t * 0.013f) > 0.93f) ? Math.max(0, clack) * 0.35f : 0.05f;
    }
}
