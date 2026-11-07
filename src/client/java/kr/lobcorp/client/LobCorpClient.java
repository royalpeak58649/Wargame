package kr.lobcorp.client;

import kr.lobcorp.LobCorp;
import kr.lobcorp.ModEntities;
import kr.lobcorp.client.model.ForsakenMurdererModel;
import kr.lobcorp.client.model.OneSinModel;
import kr.lobcorp.client.render.ForsakenMurdererRenderer;
import kr.lobcorp.client.render.OneSinRenderer;
import kr.lobcorp.client.screen.WorkScreen;
import kr.lobcorp.net.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class LobCorpClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(OneSinModel.LAYER, OneSinModel::createLayer);
        ModelLayerRegistry.registerModelLayer(ForsakenMurdererModel.LAYER, ForsakenMurdererModel::createLayer);
        EntityRendererRegistry.register(ModEntities.ONE_SIN, OneSinRenderer::new);
        EntityRendererRegistry.register(ModEntities.FORSAKEN_MURDERER, ForsakenMurdererRenderer::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.ScorchedGirlModel.LAYER, kr.lobcorp.client.model.ScorchedGirlModel::createLayer);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.HappyTeddyBearModel.LAYER, kr.lobcorp.client.model.HappyTeddyBearModel::createLayer);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.FairyFestivalModel.LAYER, kr.lobcorp.client.model.FairyFestivalModel::createLayer);
        EntityRendererRegistry.register(ModEntities.SCORCHED_GIRL, kr.lobcorp.client.render.NewAbnoRenderers.ScorchedGirl::new);
        EntityRendererRegistry.register(ModEntities.HAPPY_TEDDY, kr.lobcorp.client.render.NewAbnoRenderers.Teddy::new);
        EntityRendererRegistry.register(ModEntities.FAIRY_FESTIVAL, kr.lobcorp.client.render.NewAbnoRenderers.Fairy::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.PunishingBirdModel.LAYER, kr.lobcorp.client.model.PunishingBirdModel::createLayer);
        EntityRendererRegistry.register(ModEntities.PUNISHING_BIRD, kr.lobcorp.client.render.NewAbnoRenderers.PunishingBird::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.TodaysShyLookModel.LAYER, kr.lobcorp.client.model.TodaysShyLookModel::createLayer);
        EntityRendererRegistry.register(ModEntities.TODAYS_SHY_LOOK, kr.lobcorp.client.render.NewAbnoRenderers.ShyLook::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.LadyFacingTheWallModel.LAYER, kr.lobcorp.client.model.LadyFacingTheWallModel::createLayer);
        EntityRendererRegistry.register(ModEntities.LADY_FACING_THE_WALL, kr.lobcorp.client.render.NewAbnoRenderers.WallLady::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.CrumblingArmorModel.LAYER, kr.lobcorp.client.model.CrumblingArmorModel::createLayer);
        EntityRendererRegistry.register(ModEntities.CRUMBLING_ARMOR, kr.lobcorp.client.render.NewAbnoRenderers.Armor::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.OldLadyModel.LAYER, kr.lobcorp.client.model.OldLadyModel::createLayer);
        EntityRendererRegistry.register(ModEntities.OLD_LADY, kr.lobcorp.client.render.NewAbnoRenderers.OldLady::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.SpiderBudModel.LAYER, kr.lobcorp.client.model.SpiderBudModel::createLayer);
        EntityRendererRegistry.register(ModEntities.SPIDER_BUD, kr.lobcorp.client.render.NewAbnoRenderers.SpiderBud::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.VoidDreamModel.LAYER, kr.lobcorp.client.model.VoidDreamModel::createLayer);
        EntityRendererRegistry.register(ModEntities.VOID_DREAM, kr.lobcorp.client.render.NewAbnoRenderers.VoidDream::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.BeautyBeastModel.LAYER, kr.lobcorp.client.model.BeautyBeastModel::createLayer);
        EntityRendererRegistry.register(ModEntities.BEAUTY_BEAST, kr.lobcorp.client.render.NewAbnoRenderers.BeautyBeast::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.MeatLanternModel.LAYER, kr.lobcorp.client.model.MeatLanternModel::createLayer);
        EntityRendererRegistry.register(ModEntities.MEAT_LANTERN, kr.lobcorp.client.render.NewAbnoRenderers.MeatLantern::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.FragmentModel.LAYER, kr.lobcorp.client.model.FragmentModel::createLayer);
        EntityRendererRegistry.register(ModEntities.FRAGMENT, kr.lobcorp.client.render.NewAbnoRenderers.Fragment::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.MhzModel.LAYER, kr.lobcorp.client.model.MhzModel::createLayer);
        EntityRendererRegistry.register(ModEntities.MHZ, kr.lobcorp.client.render.NewAbnoRenderers.Mhz::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.RedShoesModel.LAYER, kr.lobcorp.client.model.RedShoesModel::createLayer);
        EntityRendererRegistry.register(ModEntities.RED_SHOES, kr.lobcorp.client.render.NewAbnoRenderers.RedShoes::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.SingingMachineModel.LAYER, kr.lobcorp.client.model.SingingMachineModel::createLayer);
        EntityRendererRegistry.register(ModEntities.SINGING_MACHINE, kr.lobcorp.client.render.NewAbnoRenderers.SingingMachine::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.PorccubusModel.LAYER, kr.lobcorp.client.model.PorccubusModel::createLayer);
        EntityRendererRegistry.register(ModEntities.PORCCUBUS, kr.lobcorp.client.render.NewAbnoRenderers.Porccubus::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.LaetitiaModel.LAYER, kr.lobcorp.client.model.LaetitiaModel::createLayer);
        EntityRendererRegistry.register(ModEntities.LAETITIA, kr.lobcorp.client.render.NewAbnoRenderers.Laetitia::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.LaetitiaFriendModel.LAYER, kr.lobcorp.client.model.LaetitiaFriendModel::createLayer);
        EntityRendererRegistry.register(ModEntities.LAETITIA_FRIEND, kr.lobcorp.client.render.NewAbnoRenderers.LaetitiaFriend::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.AllAroundHelperModel.LAYER, kr.lobcorp.client.model.AllAroundHelperModel::createLayer);
        EntityRendererRegistry.register(ModEntities.ALL_AROUND_HELPER, kr.lobcorp.client.render.NewAbnoRenderers.AllAroundHelper::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.TreeSapModel.LAYER, kr.lobcorp.client.model.TreeSapModel::createLayer);
        EntityRendererRegistry.register(ModEntities.TREE_SAP, kr.lobcorp.client.render.NewAbnoRenderers.TreeSap::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.ShelterModel.LAYER, kr.lobcorp.client.model.ShelterModel::createLayer);
        EntityRendererRegistry.register(ModEntities.SHELTER, kr.lobcorp.client.render.NewAbnoRenderers.Shelter::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.PortraitModel.LAYER, kr.lobcorp.client.model.PortraitModel::createLayer);
        EntityRendererRegistry.register(ModEntities.PORTRAIT, kr.lobcorp.client.render.NewAbnoRenderers.Portrait::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.NotesModel.LAYER, kr.lobcorp.client.model.NotesModel::createLayer);
        EntityRendererRegistry.register(ModEntities.NOTES, kr.lobcorp.client.render.NewAbnoRenderers.Notes::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.ChildGalaxyModel.LAYER, kr.lobcorp.client.model.ChildGalaxyModel::createLayer);
        EntityRendererRegistry.register(ModEntities.CHILD_GALAXY, kr.lobcorp.client.render.NewAbnoRenderers.ChildGalaxy::new);
        ModelLayerRegistry.registerModelLayer(kr.lobcorp.client.model.FreischutzModel.LAYER, kr.lobcorp.client.model.FreischutzModel::createLayer);
        EntityRendererRegistry.register(ModEntities.FREISCHUTZ, kr.lobcorp.client.render.NewAbnoRenderers.Freischutz::new);

        ClientPlayNetworking.registerGlobalReceiver(Payloads.OpenWork.TYPE,
                (payload, ctx) -> ctx.client().gui.setScreen(new WorkScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.EmployeeSync.TYPE, (payload, ctx) -> {
            ClientState.v = payload.v();
            ClientState.received = true;
        });

        HudElementRegistry.addLast(LobCorp.id("hud"), LobCorpHud::draw);
    }
}
