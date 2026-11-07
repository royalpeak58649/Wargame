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

        ClientPlayNetworking.registerGlobalReceiver(Payloads.OpenWork.TYPE,
                (payload, ctx) -> ctx.client().gui.setScreen(new WorkScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.EmployeeSync.TYPE, (payload, ctx) -> {
            ClientState.v = payload.v();
            ClientState.received = true;
        });

        HudElementRegistry.addLast(LobCorp.id("hud"), LobCorpHud::draw);
    }
}
