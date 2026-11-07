package kr.lobcorp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import kr.lobcorp.entity.AbnormalityEntity;
import kr.lobcorp.entity.BreachingAbnormality;
import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.Employees;
import kr.lobcorp.work.Facility;
import kr.lobcorp.work.FacilityData;
import kr.lobcorp.work.RoomBuilder;
import kr.lobcorp.work.WorkManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class LobCorpCommands {
    private LobCorpCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("lobcorp")
                .then(Commands.literal("stats").executes(LobCorpCommands::stats))
                .then(Commands.literal("build").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("one_sin").executes(c -> build(c, ModEntities.ONE_SIN, false)))
                        .then(Commands.literal("forsaken_murderer").executes(c -> build(c, ModEntities.FORSAKEN_MURDERER, true)))
                        .then(Commands.literal("scorched_girl").executes(c -> build(c, ModEntities.SCORCHED_GIRL, true)))
                        .then(Commands.literal("happy_teddy_bear").executes(c -> build(c, ModEntities.HAPPY_TEDDY, false)))
                        .then(Commands.literal("fairy_festival").executes(c -> build(c, ModEntities.FAIRY_FESTIVAL, false)))
                        .then(Commands.literal("punishing_bird").executes(c -> build(c, ModEntities.PUNISHING_BIRD, false)))
                        .then(Commands.literal("todays_shy_look").executes(c -> build(c, ModEntities.TODAYS_SHY_LOOK, false)))
                        .then(Commands.literal("lady_facing_the_wall").executes(c -> build(c, ModEntities.LADY_FACING_THE_WALL, true)))
                        .then(Commands.literal("crumbling_armor").executes(c -> build(c, ModEntities.CRUMBLING_ARMOR, false)))
                        .then(Commands.literal("old_lady").executes(c -> build(c, ModEntities.OLD_LADY, false)))
                        .then(Commands.literal("spider_bud").executes(c -> build(c, ModEntities.SPIDER_BUD, false)))
                        .then(Commands.literal("void_dream").executes(c -> build(c, ModEntities.VOID_DREAM, false)))
                        .then(Commands.literal("beauty_and_the_beast").executes(c -> build(c, ModEntities.BEAUTY_BEAST, false)))
                        .then(Commands.literal("meat_lantern").executes(c -> build(c, ModEntities.MEAT_LANTERN, false)))
                        .then(Commands.literal("fragment_of_universe").executes(c -> build(c, ModEntities.FRAGMENT, false)))
                        .then(Commands.literal("mhz").executes(c -> build(c, ModEntities.MHZ, false)))
                        .then(Commands.literal("red_shoes").executes(c -> build(c, ModEntities.RED_SHOES, false)))
                        .then(Commands.literal("singing_machine").executes(c -> build(c, ModEntities.SINGING_MACHINE, false)))
                        .then(Commands.literal("porccubus").executes(c -> build(c, ModEntities.PORCCUBUS, false)))
                        .then(Commands.literal("laetitia").executes(c -> build(c, ModEntities.LAETITIA, false)))
                        .then(Commands.literal("all_around_helper").executes(c -> build(c, ModEntities.ALL_AROUND_HELPER, false)))
                        .then(Commands.literal("tree_sap").executes(c -> build(c, ModEntities.TREE_SAP, false)))
                        .then(Commands.literal("shelter").executes(c -> build(c, ModEntities.SHELTER, false)))
                        .then(Commands.literal("portrait").executes(c -> build(c, ModEntities.PORTRAIT, false))))
                .then(Commands.literal("day").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("next").executes(c -> {
                            Facility.nextDay(c.getSource().getServer());
                            for (ServerLevel l : c.getSource().getServer().getAllLevels())
                                for (Entity e : l.getAllEntities())
                                    if (e instanceof AbnormalityEntity a) a.resetQliphoth();
                            discardFriends(c.getSource().getServer());
                            return 1;
                        })))
                .then(Commands.literal("breach").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LobCorpCommands::breachNearest))
                .then(Commands.literal("clear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(LobCorpCommands::clear))
                .then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                            WorkManager.clear(p);
                            p.setAttached(ModAttachments.EMPLOYEE, new EmployeeData());
                            Employees.apply(p);
                            c.getSource().sendSuccess(() -> Component.literal("직원 정보를 초기화했습니다."), true);
                            return 1;
                        }))));
    }

    private static int stats(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        EmployeeData d = Employees.get(p);
        FacilityData f = Facility.get(p.level().getServer());
        p.sendSystemMessage(Component.literal("──── 직원 정보 ────").withStyle(s -> s.withColor(0xAA0000)));
        p.sendSystemMessage(Employees.statsText(d));
        p.sendSystemMessage(Component.literal("SP " + d.sp + " / " + d.maxSp() + "    최대 HP " + d.maxHp()).withStyle(s -> s.withColor(0x55FFFF)));
        if (!d.gifts.isEmpty()) p.sendSystemMessage(Component.literal("E.G.O 기프트: " + String.join(", ", d.gifts)).withStyle(s -> s.withColor(0xF0E6C8)));
        p.sendSystemMessage(Component.literal("Day " + f.day + "   에너지 " + f.energy + " / " + f.quota).withStyle(s -> s.withColor(0x55FF55)));
        return 1;
    }

    private static <T extends AbnormalityEntity> int build(CommandContext<CommandSourceStack> c, EntityType<T> type, boolean corner) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos o = BlockPos.containing(c.getSource().getPosition());
        T a = RoomBuilder.build(level, o, type, corner);
        if (a == null) return 0;
        c.getSource().sendSuccess(() -> Component.literal("격리실 " + a.profile().code() + " " + a.profile().name() + " 설치 완료."), true);
        return 1;
    }

    private static int breachNearest(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BreachingAbnormality best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof BreachingAbnormality m && !m.isBreached()) {
                double dist = m.distanceToSqr(c.getSource().getPosition());
                if (dist < bd) { bd = dist; best = m; }
            }
        }
        if (best == null) {
            c.getSource().sendFailure(Component.literal("탈출 가능한 환상체가 없습니다."));
            return 0;
        }
        best.breach();
        return 1;
    }

    private static void discardFriends(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel l : server.getAllLevels()) {
            for (Entity e : l.getAllEntities()) {
                if (e instanceof kr.lobcorp.entity.LaetitiaFriendEntity) e.discard();
            }
        }
    }

    private static int clear(CommandContext<CommandSourceStack> c) {
        int n = 0;
        for (ServerLevel l : c.getSource().getServer().getAllLevels()) {
            for (Entity e : l.getAllEntities()) {
                if (e instanceof AbnormalityEntity) {
                    e.discard();
                    n++;
                }
            }
        }
        discardFriends(c.getSource().getServer());
        int count = n;
        c.getSource().sendSuccess(() -> Component.literal("환상체 " + count + "개를 제거했습니다."), true);
        return n;
    }
}
