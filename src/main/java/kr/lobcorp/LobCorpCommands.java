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
                        .then(Commands.literal("portrait").executes(c -> build(c, ModEntities.PORTRAIT, false)))
                        .then(Commands.literal("notes").executes(c -> build(c, ModEntities.NOTES, false)))
                        .then(Commands.literal("child_galaxy").executes(c -> build(c, ModEntities.CHILD_GALAXY, false)))
                        .then(Commands.literal("freischutz").executes(c -> build(c, ModEntities.FREISCHUTZ, false)))
                        .then(Commands.literal("nameless_fetus").executes(c -> build(c, ModEntities.NAMELESS_FETUS, false)))
                        .then(Commands.literal("snow_queen").executes(c -> build(c, ModEntities.SNOW_QUEEN, false)))
                        .then(Commands.literal("woodsman").executes(c -> build(c, ModEntities.WOODSMAN, false)))
                        .then(Commands.literal("scarecrow").executes(c -> build(c, ModEntities.SCARECROW, false)))
                        .then(Commands.literal("rudolta").executes(c -> build(c, ModEntities.RUDOLTA, false)))
                        .then(Commands.literal("funeral").executes(c -> build(c, ModEntities.FUNERAL, false)))
                        .then(Commands.literal("schadenfreude").executes(c -> build(c, ModEntities.SCHADENFREUDE, false)))
                        .then(Commands.literal("big_bird").executes(c -> build(c, ModEntities.BIG_BIRD, false)))
                        .then(Commands.literal("judgement_bird").executes(c -> build(c, ModEntities.JUDGEMENT_BIRD, false)))
                        .then(Commands.literal("queen_of_hatred").executes(c -> build(c, ModEntities.QUEEN_HATRED, false)))
                        .then(Commands.literal("knight_of_despair").executes(c -> build(c, ModEntities.KNIGHT_DESPAIR, false)))
                        .then(Commands.literal("king_of_greed").executes(c -> build(c, ModEntities.KING_GREED, false)))
                        .then(Commands.literal("little_red").executes(c -> build(c, ModEntities.LITTLE_RED, false)))
                        .then(Commands.literal("big_bad_wolf").executes(c -> build(c, ModEntities.BIG_BAD_WOLF, false)))
                        .then(Commands.literal("alriune").executes(c -> build(c, ModEntities.ALRIUNE, false)))
                        .then(Commands.literal("queen_bee").executes(c -> build(c, ModEntities.QUEEN_BEE, false)))
                        .then(Commands.literal("black_swan").executes(c -> build(c, ModEntities.BLACK_SWAN, false)))
                        .then(Commands.literal("yin").executes(c -> build(c, ModEntities.YIN, false)))
                        .then(Commands.literal("yang").executes(c -> build(c, ModEntities.YANG, false)))
                        .then(Commands.literal("snow_white_apple").executes(c -> build(c, ModEntities.SNOW_WHITE_APPLE, false)))
                        .then(Commands.literal("little_prince").executes(c -> build(c, ModEntities.LITTLE_PRINCE, false)))
                        .then(Commands.literal("parasite_tree").executes(c -> build(c, ModEntities.PARASITE_TREE, false)))
                        .then(Commands.literal("naked_nest").executes(c -> build(c, ModEntities.NAKED_NEST, false)))
                        .then(Commands.literal("dreaming_current").executes(c -> build(c, ModEntities.DREAMING_CURRENT, false)))
                        .then(Commands.literal("burrowing_heaven").executes(c -> build(c, ModEntities.BURROWING_HEAVEN, false)))
                        .then(Commands.literal("firebird").executes(c -> build(c, ModEntities.FIREBIRD, false)))
                        .then(Commands.literal("clouded_monk").executes(c -> build(c, ModEntities.CLOUDED_MONK, false)))
                        .then(Commands.literal("il_pianto").executes(c -> build(c, ModEntities.IL_PIANTO, false)))
                        .then(Commands.literal("backward_clock").executes(c -> build(c, ModEntities.BACKWARD_CLOCK, false))))
                .then(Commands.literal("facility").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> facility(c, false))
                        .then(Commands.literal("full").executes(c -> facility(c, true)))
                        .then(Commands.literal("fixsigns").executes(c -> {
                            var server = c.getSource().getServer();
                            if (!kr.lobcorp.work.Extraction.hasFacility(server)) {
                                c.getSource().sendFailure(Component.literal("시설이 없습니다."));
                                return 0;
                            }
                            int n = kr.lobcorp.work.FacilityBuilder.fixSephirahSigns(c.getSource().getLevel(), Facility.get(server).slots);
                            c.getSource().sendSuccess(() -> Component.literal("세피라 표지판 " + n + "개를 고쳤습니다."), true);
                            return 1;
                        })))
                .then(Commands.literal("extract").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> {
                            var server = c.getSource().getServer();
                            if (!kr.lobcorp.work.Extraction.hasFacility(server)) {
                                c.getSource().sendFailure(Component.literal("먼저 /lobcorp facility 로 시설을 지으세요."));
                                return 0;
                            }
                            if (Facility.get(server).offers.isEmpty()) kr.lobcorp.work.Extraction.offer(server);
                            else kr.lobcorp.work.Extraction.announce(server);
                            return 1;
                        })
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> {
                                    var have = kr.lobcorp.work.Extraction.placed(c.getSource().getServer());
                                    for (String id : kr.lobcorp.work.FacilityBuilder.catalog().keySet())
                                        if (!have.contains(id) && id.startsWith(b.getRemainingLowerCase())) b.suggest(id);
                                    return b.buildFuture();
                                })
                                .executes(c -> {
                                    String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id");
                                    var out = kr.lobcorp.work.Extraction.place(c.getSource().getServer(), id);
                                    switch (out) {
                                        case OK -> { return 1; }
                                        case NO_FACILITY -> c.getSource().sendFailure(Component.literal("먼저 /lobcorp facility 로 시설을 지으세요."));
                                        case FULL -> c.getSource().sendFailure(Component.literal("빈 격리실이 없습니다."));
                                        case ALREADY -> c.getSource().sendFailure(Component.literal("이미 수용한 환상체입니다."));
                                        default -> c.getSource().sendFailure(Component.literal("알 수 없는 환상체입니다: " + id));
                                    }
                                    return 0;
                                })))
                .then(Commands.literal("day").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("next").executes(c -> {
                            Facility.nextDay(c.getSource().getServer());
                            for (ServerLevel l : c.getSource().getServer().getAllLevels())
                                for (Entity e : l.getAllEntities())
                                    if (e instanceof AbnormalityEntity a) a.resetQliphoth();
                            discardFriends(c.getSource().getServer());
                            discardBees(c.getSource().getServer());
                            kr.lobcorp.work.Extraction.offer(c.getSource().getServer());
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

    private static int facility(CommandContext<CommandSourceStack> c, boolean full) {
        ServerLevel level = c.getSource().getLevel();
        var server = c.getSource().getServer();
        if (kr.lobcorp.work.Extraction.hasFacility(server)) {
            c.getSource().sendFailure(Component.literal("이미 시설이 있습니다. (한 월드에 하나만 지을 수 있습니다)"));
            return 0;
        }
        BlockPos o = BlockPos.containing(c.getSource().getPosition());
        c.getSource().sendSuccess(() -> Component.literal("시설을 짓는 중입니다… (잠시 멈출 수 있습니다)").withStyle(s -> s.withColor(0xAAAAAA)), true);
        var r = kr.lobcorp.work.FacilityBuilder.build(level, o, full);
        Facility.get(server).slots = new java.util.ArrayList<>(r.slots());
        Facility.get(server).offers.clear();
        if (!full) kr.lobcorp.work.Extraction.place(server, "one_sin");
        if (c.getSource().getEntity() instanceof ServerPlayer p)
            p.teleportTo(level, r.spawn().getX() + 0.5, r.spawn().getY(), r.spawn().getZ() + 0.5, java.util.Set.of(), -90f, 0f, false);
        c.getSource().sendSuccess(() -> Component.literal("로보토미 시설 완공: 부서 10개, 격리실 " + r.slots().size() + "칸. 상층 바닥 높이 Y=" + r.topY()
                + ". 중앙제어팀 홀로 이동했습니다." + (full ? " (환상체 전부 수용)" : " 첫 환상체를 수용했습니다. 하루가 끝나면(/lobcorp day next) 다음 후보가 나옵니다."))
                .withStyle(s -> s.withColor(0x55FF55)), true);
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

    private static void discardBees(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel l : server.getAllLevels()) {
            for (Entity e : l.getAllEntities()) {
                if (e instanceof kr.lobcorp.entity.WorkerBeeEntity) e.discard();
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
        discardBees(c.getSource().getServer());
        kr.lobcorp.work.QliphothPanel.discardAll(c.getSource().getServer());
        int count = n;
        c.getSource().sendSuccess(() -> Component.literal("환상체 " + count + "개를 제거했습니다."), true);
        return n;
    }
}
