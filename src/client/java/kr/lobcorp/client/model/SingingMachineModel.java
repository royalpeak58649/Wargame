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

/** 뚜껑이 달린 낡은 나무 오르골 상자. 연주 중에는 뚜껑이 열리고 안쪽 드럼이 돌아간다. */
public class SingingMachineModel extends EntityModel<SingingMachineModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("singing_machine"), "main");

    public static class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
        public boolean open;
    }

    private final ModelPart lid, drum;

    public SingingMachineModel(ModelPart root) {
        super(root);
        lid = root.getChild("lid");
        drum = root.getChild("drum");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 본체: 광택 나는 갈색 나무 상자
        root.addOrReplaceChild("cabinet", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-7, 0, -5, 14, 12, 10), PartPose.offset(0, 12, 0));
        // 금장 테두리
        root.addOrReplaceChild("trim", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-7.2f, 10, -5.2f, 14.4f, 1, 10.4f), PartPose.offset(0, 12, 0));
        // 뚜껑: 뒤쪽 경첩
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-7, -1, -5, 14, 1, 10), PartPose.offset(0, 12, 5));
        // 옆에 달린 나팔 스피커
        root.addOrReplaceChild("horn", CubeListBuilder.create().texOffs(40, 0)
                .addBox(7, 4, -3, 4, 4, 6), PartPose.offset(0, 12, 0));
        // 안쪽의 어두운 붉은 드럼
        root.addOrReplaceChild("drum", CubeListBuilder.create().texOffs(40, 12)
                .addBox(-5, -1.5f, -1.5f, 10, 3, 3), PartPose.offset(0, 19, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        if (s.open) {
            lid.xRot = -1.1f;
            drum.xRot = t * 0.5f;
        } else {
            lid.xRot = 0f;
            drum.xRot = Mth.sin(t * 0.02f) * 0.05f;
        }
    }
}
