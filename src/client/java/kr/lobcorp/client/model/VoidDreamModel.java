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

/** 잠든 양. 때리면 수탉의 볏과 부리, 뜬 눈이 드러난다. */
public class VoidDreamModel extends EntityModel<VoidDreamModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("void_dream"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean breached;
        public boolean rooster;
    }

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart comb;
    private final ModelPart beak;
    private final ModelPart eyesOpen;
    private final ModelPart tail;

    public VoidDreamModel(ModelPart root) {
        super(root);
        this.root = root;
        head = root.getChild("head");
        comb = head.getChild("comb");
        beak = head.getChild("beak");
        eyesOpen = head.getChild("eyes_open");
        tail = root.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -3, -5, 8, 6, 10), PartPose.offset(0, 16, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2.5f, -2.5f, -5, 5, 5, 5), PartPose.offset(0, 14, -5));
        head.addOrReplaceChild("horns", CubeListBuilder.create()
                .texOffs(20, 16).addBox(-3.5f, -3.5f, -1, 1, 2, 1)
                .texOffs(20, 16).addBox(2.5f, -3.5f, -1, 1, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("comb", CubeListBuilder.create().texOffs(24, 16)
                .addBox(-0.5f, -4.5f, -3, 1, 4, 5), PartPose.ZERO);
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(36, 16)
                .addBox(-1, -0.5f, -7, 2, 1, 2), PartPose.ZERO);
        head.addOrReplaceChild("eyes_open", CubeListBuilder.create()
                .texOffs(44, 16).addBox(-2, -1, -5.1f, 2, 2, 0)
                .texOffs(44, 16).addBox(0, -1, -5.1f, 2, 2, 0), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            float x = (i % 2 == 0) ? -2.5f : 2.5f;
            float z = (i < 2) ? -3f : 3f;
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(20, 20)
                    .addBox(-0.5f, 0, -0.5f, 1, 5, 1), PartPose.offset(x, 19, z));
        }
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 20)
                .addBox(-0.5f, 0, 0, 1, 1, 10), PartPose.offsetAndRotation(0, 14, 5, 0.5f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        root.y = Mth.sin(t * 0.06f) * 0.6f;
        head.xRot = 0.15f + Mth.sin(t * 0.03f) * 0.03f;
        tail.xRot = 0.5f + Mth.sin(t * 0.05f) * 0.08f;
        comb.visible = s.rooster;
        beak.visible = s.rooster;
        eyesOpen.visible = s.rooster;
    }
}
