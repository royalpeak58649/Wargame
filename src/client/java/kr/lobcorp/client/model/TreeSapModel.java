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

/** 점토 항아리에 담긴 짙은 호박색 수액과 국자. */
public class TreeSapModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("tree_sap"), "main");

    private final ModelPart sap;
    private final ModelPart ladle;

    public TreeSapModel(ModelPart root) {
        super(root);
        sap = root.getChild("sap");
        ladle = root.getChild("ladle");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, 0, -4, 8, 2, 8), PartPose.offset(0, 22, 0));
        root.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-5, 0, -5, 10, 8, 10), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-3, 0, -3, 6, 3, 6), PartPose.offset(0, 11, 0));
        root.addOrReplaceChild("sap", CubeListBuilder.create().texOffs(0, 38)
                .addBox(-2.5f, 0, -2.5f, 5, 1, 5), PartPose.offset(0, 11, 0));
        PartDefinition ladle = root.addOrReplaceChild("ladle", CubeListBuilder.create().texOffs(24, 38)
                .addBox(-0.5f, -8, -0.5f, 1, 8, 1), PartPose.offsetAndRotation(1.5f, 11, 0.5f, 0.35f, 0, -0.45f));
        ladle.addOrReplaceChild("bowl", CubeListBuilder.create().texOffs(30, 38)
                .addBox(-1, 0, -1, 2, 1, 2), PartPose.offset(0, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        sap.y = 11 + Mth.sin(t * 0.05f) * 0.08f;
        ladle.zRot = -0.45f + Mth.sin(t * 0.03f) * 0.03f;
    }
}
