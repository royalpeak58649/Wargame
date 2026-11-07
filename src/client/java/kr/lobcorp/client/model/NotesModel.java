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

/** 작은 받침대 위에 펼쳐진, 얼룩진 노트. 누군가 들고 있으면 책이 사라진다. */
public class NotesModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LobCorp.id("notes"), "main");

    private final ModelPart book;

    public NotesModel(ModelPart root) {
        super(root);
        book = root.getChild("book");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 바닥 받침
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3, 0, -3, 6, 1, 6), PartPose.offset(0, 23, 0));
        // 기둥
        root.addOrReplaceChild("post", CubeListBuilder.create().texOffs(0, 8)
                .addBox(-1, -10, -1, 2, 10, 2), PartPose.offset(0, 23, 0));
        // 비스듬한 상판
        root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-4, -1, -3, 8, 1, 6), PartPose.offsetAndRotation(0, 13, 0, -0.4f, 0, 0));
        // 펼쳐진 노트
        PartDefinition book = root.addOrReplaceChild("book", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-3.5f, -1, -2.5f, 7, 1, 5), PartPose.offsetAndRotation(0, 12.4f, 0, -0.4f, 0, 0));
        book.addOrReplaceChild("cover", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-3.5f, 0, -2.5f, 7, 1, 5), PartPose.offset(0, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState s) {
        super.setupAnim(s);
        book.visible = true;
    }
}
