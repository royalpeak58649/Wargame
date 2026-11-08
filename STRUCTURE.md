# LobCorp Fabric Mod — Codebase Overview

A Minecraft Fabric mod (MC 26.3, Fabric Loader 0.20.1, Java, package `kr.lobcorp`) that recreates
Lobotomy Corporation: a facility of containment rooms, abnormalities you perform "work" on to
produce PE-BOXes and energy, E.G.O weapons/suits, qliphoth counters and breaches.

Layout: `src/main/java` (common/server logic), `src/client/java` (client rendering, HUD, screens),
`src/gametest/java` (Fabric client gametests), `src/main/resources/assets/lobcorp` (textures, models,
lang), `tools/` (asset generator).

## Core (src/main/java/kr/lobcorp)

- `LobCorp.java` — `ModInitializer`: registers items/entities/attachments/payloads, wires server tick, join/leave/respawn and damage events (SP/HP/PALE damage, shelter immunity, portrait damage transfer, notes).
- `AbnoProfile.java` — record holding every abnormality's data (id, code, risk level, work chances, PE-BOX thresholds, qliphoth max, E.G.O weapon/suit, gift, resistances, notes) plus the `BY_ID` lookup table.
- `DamageColor.java` — enum of the four damage types (RED=HP, WHITE=SP, BLACK=HP+SP, PALE=% max HP) with display colors.
- `ModAttachments.java` — Fabric attachment types for persistent per-player `EmployeeData` and per-world `FacilityData`.
- `ModItems.java` — registers all E.G.O weapons (`EgoWeaponItem`) and suit items with armor materials, attributes and lore.
- `ModEntities.java` — registers every abnormality `EntityType` (sizes, categories) and default attributes.
- `LobCorpCommands.java` — `/lobcorp` command tree: `stats`, `build <abno>`, `facility [full|fixsigns]`, `extract [id]`, `day next`, `breach`, `clear`, `reset`.

### net/

- `net/Payloads.java` — network payloads: `WorkAction` (client→server button), `OpenWork` (open work screen), `EmployeeSync` (24-int HUD state array).

### item/

- `item/EgoWeaponItem.java` — item subclass carrying a `DamageColor`; damage scaling against abnormalities is derived from that color and the abnormality's resistances.

### work/

- `work/WorkManager.java` — core work loop: opens the work screen, validates and starts sessions, ticks PE-BOX generation, resolves results, grants stats/gifts, extracts E.G.O, and holds suit-resistance helpers.
- `work/WorkSession.java` — mutable per-player work state (abnormality, work type, max boxes, chance, interval, progress).
- `work/WorkType.java` — enum of the four work types (Instinct/Insight/Attachment/Repression) with the stat they train.
- `work/Employees.java` — employee stats/SP/panic handling: attribute application, SP sync, sleep, possession, pale/SP damage, panic, per-tick regen and stat text.
- `work/EmployeeData.java` — persistent per-player data (fortitude/prudence/temperance/justice, SP, panic, gifts, and all temporary status timers) with a codec.
- `work/Facility.java` — facility-wide energy/quota/day logic, boss bar, day rollover and broadcast helper.
- `work/FacilityData.java` — persistent world state: day, energy, quota, containment slots and current extraction offers.
- `work/FacilityBuilder.java` — builds the whole 10-department facility (halls, corridors, containment rooms, stairs, signs) and exposes the abnormality `catalog()` and `fixSephirahSigns`.
- `work/RoomBuilder.java` — builds a single 9x6x9 containment room shell and spawns an abnormality inside it.
- `work/Extraction.java` — abnormality acquisition: risk-gated candidate pool, 3-choice offers, and placing a chosen abnormality into the next empty slot.
- `work/QliphothPanel.java` — floating `TextDisplay` panel above each containment door showing code, name and qliphoth counter.
- `work/ToolState.java` — facility-wide state for "tool" abnormalities (tree sap, shelter, portrait, notes) plus death/next-day hooks.
- `work/AbnoEvents.java` — broadcasts facility events (breach, employee death, panic) to every abnormality and counts active breaches.

### entity/

- `entity/AbnormalityEntity.java` — abstract base for all abnormalities: profile, PE storage, qliphoth counter, home position, work hooks and interaction.
- `entity/BreachingAbnormality.java` — base for abnormalities that escape at qliphoth 0: door opening, alarm, boss bar, chase AI, suppression and return home.
- `entity/OneSinEntity.java` — O-03-03 One Sin: non-breaching ZAYIN that restores SP on good work.
- `entity/ForsakenMurdererEntity.java` — T-01-54 Forsaken Murderer: breaches on bad results and chases employees.
- `entity/ScorchedGirlEntity.java` — F-01-02 Scorched Girl: breaches and self-destructs next to the nearest employee.
- `entity/HappyTeddyBearEntity.java` — T-04-06 Happy Teddy Bear: hugs and kills an employee who works twice in a row.
- `entity/FairyFestivalEntity.java` — F-04-83 Fairy Festival: blesses workers with regeneration, devours them if they work elsewhere.
- `entity/PunishingBirdEntity.java` — O-02-56 Punishing Bird: lowers counters on other works/panics, flies and enrages when hit.
- `entity/TodaysShyLookEntity.java` — O-01-92 Today's Shy Look: rotating faces that change success rate and failure damage.
- `entity/LadyFacingTheWallEntity.java` — F-01-18 Lady Facing the Wall: attachment work drains the counter, spreading sobbing at 0.
- `entity/CrumblingArmorEntity.java` — O-05-61 Crumbling Armor: beheads low-fortitude workers and grants the "Courage" gift.
- `entity/OldLadyEntity.java` — O-01-12 Old Lady: counter decays with other works, loneliness debuff at 0.
- `entity/SpiderBudEntity.java` — T-02-43 Spider Bud: turns workers into cocoons that add extra PE-BOXes.
- `entity/VoidDreamEntity.java` — T-02-99 Void Dream: breaches, puts employees to sleep, mutates into a rooster form when hit.
- `entity/BeautyBeastEntity.java` — F-02-44 Beauty and the Beast: repression wounds it, other works heal it, curses the worker.
- `entity/MeatLanternEntity.java` — O-04-84 Meat Lantern: counter drops on bad/abandoned work, hides under the floor when breached.
- `entity/FragmentEntity.java` — O-03-60 Fragment of the Universe: breaches, stabs with tentacles and sings to break minds.
- `entity/MhzEntity.java` — T-06-27 1.76 MHz: radio noise that drains SP in a radius and distorts.
- `entity/RedShoesEntity.java` — O-04-08 Red Shoes: possesses the worker who works attachment on it.
- `entity/SingingMachineEntity.java` — O-05-30 Singing Machine: music that kills low-temperance workers on completion.
- `entity/PorccubusEntity.java` — O-02-98 Porccubus: breaches and attacks with SP-draining thorns.
- `entity/LaetitiaEntity.java` — O-01-67 Laetitia: gives a "gift" heart that spawns a friend monster when carried elsewhere.
- `entity/LaetitiaFriendEntity.java` — the spider-like monster born from Laetitia's gift; not an abnormality, only E.G.O weapons hurt it properly.
- `entity/AllAroundHelperEntity.java` — T-05-41 All Around Helper: multi-phase cleaning robot that breaches with blades.
- `entity/TreeSapEntity.java` — T-09-80 Giant Tree Sap: tool-type jar whose sap heals but may explode inside the drinker.
- `entity/ShelterEntity.java` — T-09-82 Shelter from the 27th of March: tool-type hatch that shelters one employee from all damage.
- `entity/PortraitEntity.java` — O-09-91 Portrait of Another World: tool-type that makes one employee "etched" and another absorb their damage.
- `entity/NotesEntity.java` — T-09-78 Notes from a Crazed Researcher: tool-type notes that boost work but explode when damage accumulates.
- `entity/ChildGalaxyEntity.java` — O-01-55 Child of the Galaxy: gives a friendship pebble; betrayal or the holder's death collapses it.
- `entity/FreischutzEntity.java` — F-01-69 Der Freischütz: requests energy for magic bullets with a cooldown.
- `entity/FetusEntity.java` — O-01-15 Nameless Fetus: cries and drains SP, demands a volunteer.
- `entity/SnowQueenEntity.java` — F-01-37 Snow Queen: frost curse that freezes and kills at day end.
- `entity/WoodsmanEntity.java` — F-05-32 Warm-Hearted Woodsman: empty chest that arms itself with a heart from a nearby death.
- `entity/ScarecrowEntity.java` — F-01-87 Scarecrow Searching for Wisdom: collects brains and leaves its post.
- `entity/RudoltaEntity.java` — F-02-49 Rudolta of the Sleigh: Christmas-tree antlered deer that breaches.
- `entity/FuneralEntity.java` — T-01-68 Funeral of the Dead Butterflies: coffin-headed mourner that breaches.
- `entity/SchadenfreudeEntity.java` — O-05-76 Schadenfreude: only moves while unobserved; being watched keeps it contained.
- `entity/BigBirdEntity.java` — O-02-40 Big Bird: eye-covered bird that breaches and hunts in the dark.
- `entity/JudgementBirdEntity.java` — O-02-62 Judgement Bird: scale-carrying bird that breaches and judges employees.
- `entity/QueenHatredEntity.java` — O-01-04 Queen of Hatred: magical girl that breaches when the facility is "unjust".
- `entity/KnightDespairEntity.java` — O-01-73 Knight of Despair: grants a blessing, then breaches.
- `entity/KingGreedEntity.java` — O-01-64 King of Greed: golden-dressed magical girl that breaches.
- `entity/LittleRedEntity.java` — F-01-57 Little Red Riding Hooded Mercenary: axe/gun mercenary that breaches.
- `entity/BigBadWolfEntity.java` — F-02-58 Big and Will be Bad Wolf: swallows employees (devoured state) when breached.
- `entity/AlriuneEntity.java` — T-04-53 Alriune: petal woman whose flowers bloom as it breaches.
- `entity/QueenBeeEntity.java` — T-04-50 Queen Bee: infects workers with spores that hatch into worker bees.
- `entity/WorkerBeeEntity.java` — worker bee spawned from a spore-infected corpse; not an abnormality, only E.G.O weapons hurt it properly.
- `entity/BlackSwanEntity.java` — F-02-70 Dream of a Black Swan: lake-bound woman with cursed brothers that breaches.

## Client (src/client/java/kr/lobcorp/client)

- `client/LobCorpClient.java` — `ClientModInitializer`: registers every model layer and entity renderer, the work-screen/sync packet receivers and the HUD element.
- `client/ClientState.java` — static mirror of the last `EmployeeSync` array with typed accessors for the HUD.
- `client/LobCorpHud.java` — draws the SP bar, panic/status timers and PE-BOX progress in the top-left corner.
- `client/screen/WorkScreen.java` — the abnormality management screen (work buttons, PE-BOX/qliphoth display, E.G.O extraction).
- `client/render/NewAbnoRenderers.java` — one nested `MobRenderer` per abnormality (textures, render states, special effects).
- `client/render/OneSinRenderer.java` — renderer for One Sin.
- `client/render/ForsakenMurdererRenderer.java` — renderer for the Forsaken Murderer (breach/attack animation state).
- `client/model/MurdererRenderState.java` — extra render state (breached flag, attack animation) for the murderer.
- `client/model/*Model.java` — one `EntityModel` per abnormality (48 files: `AllAroundHelperModel`, `AlriuneModel`, `BeautyBeastModel`, `BigBadWolfModel`, `BigBirdModel`, `BlackSwanModel`, `ChildGalaxyModel`, `CrumblingArmorModel`, `FairyFestivalModel`, `FetusModel`, `ForsakenMurdererModel`, `FragmentModel`, `FreischutzModel`, `FuneralModel`, `HappyTeddyBearModel`, `JudgementBirdModel`, `KingGreedModel`, `KnightDespairModel`, `LadyFacingTheWallModel`, `LaetitiaFriendModel`, `LaetitiaModel`, `LittleRedModel`, `MeatLanternModel`, `MhzModel`, `NotesModel`, `OldLadyModel`, `OneSinModel`, `PorccubusModel`, `PortraitModel`, `PunishingBirdModel`, `QueenBeeModel`, `QueenHatredModel`, `RedShoesModel`, `RudoltaModel`, `ScarecrowModel`, `SchadenfreudeModel`, `ScorchedGirlModel`, `ShelterModel`, `SingingMachineModel`, `SnowQueenModel`, `SpiderBudModel`, `TodaysShyLookModel`, `TreeSapModel`, `VoidDreamModel`, `WoodsmanModel`, `WorkerBeeModel`), each defining its model layer, box mesh and idle/state animation.

## Gametests (src/gametest/java/kr/lobcorp/test)

All are Fabric `FabricClientGameTest`s that spin up a singleplayer world, run commands, drive work
sessions and log `[LOBCORP-TEST]` lines (some also take screenshots).

- `FacilityTest.java` — builds the facility, checks walkability/reachability, extraction offers, duplicate refusal and risk progression.
- `FixSignsTest.java` — verifies `FacilityBuilder.fixSephirahSigns` repairs deliberately corrupted department signs.
- `HeTest.java` — HE-tier behaviour: 1.76 MHz, Red Shoes possession, Singing Machine, Porccubus, Laetitia/friend, All Around Helper.
- `He2Test.java` — second HE batch: Tree Sap, Shelter, Portrait, Notes, Child of the Galaxy, Freischütz, Fetus, Snow Queen, Woodsman, Scarecrow, Schadenfreude.
- `TethTest.java` — TETH batch: Today's Shy Look, Lady Facing the Wall, Crumbling Armor.
- `Teth2Test.java` — TETH batch: Old Lady, Spider Bud, Void Dream (with Fairy Festival).
- `Teth3Test.java` — TETH batch: Beauty and the Beast, Meat Lantern, Fragment of the Universe.
- `WawTest.java` — WAW batch: Big Bird, Judgement Bird, Queen of Hatred, Knight of Despair, King of Greed, Little Red, Big Bad Wolf, Alriune, Queen Bee, Black Swan.
- `PunishingBirdTest.java` — Punishing Bird containment, breach, enrage and suppression with screenshots.
- `PanelTest.java` — qliphoth `TextDisplay` panel content before/after lowering the counter and breaching, with screenshots.
- `PvpWhiteTest.java` — player-vs-player E.G.O damage: WHITE/BLACK/PALE weapons against another employee.
- `LobCorpClientTest.java` — real-client regression run for Fairy Festival, Happy Teddy Bear, Scorched Girl and Forsaken Murderer with screenshots.

## tools/gen_assets.py

A standalone Python script (Pillow) that procedurally generates the mod's art and resource JSON:
it paints 64x64 entity textures box-by-box (one function per abnormality, e.g. `one_sin()`,
`black_swan()`), 16x16 item icons from ASCII art palettes (`icon()`), 64x32 worn-armor textures
(`armor()`), and writes the matching `items/`, `models/item/` and `equipment/` JSON files
(`wjson()`). `merge_lang()` merges new translation keys into `tools/lang_ko_kr.json` /
`tools/lang_en_us.json` and copies them to `assets/lobcorp/lang/`. Note: the output root `R` is a
hard-coded absolute path, so it must be adjusted before running elsewhere.
