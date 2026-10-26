extends SceneTree
## 전투 탄도·관통 계산 모듈 단위 테스트.
## 실행: godot --headless --path . --script res://tests/run_combat_tests.gd
## 실패가 하나라도 있으면 종료 코드 1.

const Ballistics = preload("res://scripts/combat/ballistics.gd")
const PartsDB = preload("res://scripts/core/parts_db.gd")
const ShipCalc = preload("res://scripts/core/ship_calc.gd")
const ShipState = preload("res://scripts/combat/ship_state.gd")
const BattleSim = preload("res://scripts/combat/battle_sim.gd")
const Navigation = preload("res://scripts/combat/navigation.gd")
const FleetCommander = preload("res://scripts/combat/fleet_commander.gd")
const EnemyAI = preload("res://scripts/combat/enemy_ai.gd")
const BattleScenario = preload("res://scripts/battle/battle_scenario.gd")

var db: Dictionary
var gun_381: Dictionary
var gun_152: Dictionary
var fc: Dictionary
var qe: Dictionary
var player: Dictionary
var passed := 0
var failed := 0


func _init() -> void:
	db = PartsDB.load_many(["res://data/parts_t2.json"])
	gun_381 = db.guns["gun_381mm_t2"]
	gun_152 = db.guns["gun_152mm_t2"]
	fc = db.fireControl["fc_t2_director"]
	qe = PartsDB.load_design("res://data/designs/uk_bb_queen_elizabeth.json")
	player = PartsDB.load_design("res://data/designs/player_bb_fast.json")

	for m in get_method_list():
		var name: String = m.name
		if name.begins_with("test_"):
			var before := failed
			call(name)
			print("%s %s" % ["PASS" if failed == before else "FAIL", name])

	print("\n%d개 검사 통과, %d개 실패" % [passed, failed])
	quit(1 if failed > 0 else 0)


# --- 도우미 ---------------------------------------------------------------

func check(cond: bool, msg: String) -> void:
	if cond:
		passed += 1
	else:
		failed += 1
		print("    실패: " + msg)


func in_range(v: float, lo: float, hi: float, msg: String) -> void:
	check(v >= lo and v <= hi, "%s = %.3f (기대 %.3f~%.3f)" % [msg, v, lo, hi])


# --- 탄도 -----------------------------------------------------------------

func test_velocity_decays() -> void:
	var v0: float = Ballistics.remaining_velocity(gun_381, 0.0)
	var v10: float = Ballistics.remaining_velocity(gun_381, 10.0)
	var v20: float = Ballistics.remaining_velocity(gun_381, 20.0)
	in_range(v0, 731.9, 732.1, "381mm 0km 잔존 속도(m/s)")
	check(v10 < v0, "10km 속도가 0km보다 낮아야 함 (%.1f → %.1f)" % [v0, v10])
	check(v20 < v10, "20km 속도가 10km보다 낮아야 함 (%.1f → %.1f)" % [v10, v20])


func test_heavy_shell_keeps_velocity() -> void:
	var keep_381: float = Ballistics.remaining_velocity(gun_381, 10.0) / float(gun_381.muzzleVelocity)
	var keep_152: float = Ballistics.remaining_velocity(gun_152, 10.0) / float(gun_152.muzzleVelocity)
	check(keep_381 > keep_152, "10km 속도 보존율 381mm > 152mm (%.3f vs %.3f)" % [keep_381, keep_152])


func test_pen_381_at_10km() -> void:
	in_range(Ballistics.penetration_mm(gun_381, 10.0), 280.0, 380.0, "381mm 10km 관통력(mm)")


func test_pen_decreases_with_range() -> void:
	var p0: float = Ballistics.penetration_mm(gun_381, 0.0)
	var p10: float = Ballistics.penetration_mm(gun_381, 10.0)
	var p20: float = Ballistics.penetration_mm(gun_381, 20.0)
	check(p0 > p10, "관통력 0km > 10km (%.1f vs %.1f)" % [p0, p10])
	check(p10 > p20, "관통력 10km > 20km (%.1f vs %.1f)" % [p10, p20])


func test_fall_angle_grows() -> void:
	var f0: float = Ballistics.fall_angle_deg(gun_381, 0.0)
	var f5: float = Ballistics.fall_angle_deg(gun_381, 5.0)
	var f10: float = Ballistics.fall_angle_deg(gun_381, 10.0)
	var fmax: float = Ballistics.fall_angle_deg(gun_381, float(gun_381.maxRangeKm))
	in_range(f0, -0.001, 0.001, "0km 낙각(°)")
	check(f5 > f0 and f10 > f5, "낙각은 거리에 따라 증가 (%.2f → %.2f → %.2f)" % [f0, f5, f10])
	in_range(fmax, 29.999, 30.001, "최대 사거리 낙각(°)")


# --- 판정 -----------------------------------------------------------------

func test_belt_vs_deck_by_range() -> void:
	var belt_5: Dictionary = Ballistics.resolve_hit(gun_381, 5.0, "belt", 300.0, 0.0)
	check(String(belt_5.result) == "pen", "5km 벨트 300mm 관통: %s (관통 %.1f / 유효 %.1f)" % [belt_5.result, belt_5.penMm, belt_5.effArmorMm])
	var deck_5: Dictionary = Ballistics.resolve_hit(gun_381, 5.0, "deck", 75.0, 0.0)
	var deck_20: Dictionary = Ballistics.resolve_hit(gun_381, 20.0, "deck", 75.0, 0.0)
	check(float(deck_20.effArmorMm) < float(deck_5.effArmorMm),
		"20km 갑판 유효 장갑 < 5km 갑판 유효 장갑 (%.1f vs %.1f)" % [deck_20.effArmorMm, deck_5.effArmorMm])


func test_ricochet() -> void:
	var deck_5: Dictionary = Ballistics.resolve_hit(gun_381, 5.0, "deck", 75.0, 0.0)
	check(String(deck_5.result) == "ricochet", "5km 갑판(낙각 %.1f°)은 도탄: %s" % [deck_5.fallDeg, deck_5.result])
	var belt_75: Dictionary = Ballistics.resolve_hit(gun_381, 10.0, "belt", 300.0, 75.0)
	check(String(belt_75.result) == "ricochet", "벨트 수평 75°(입사각 %.1f°)는 도탄: %s" % [belt_75.obliquityDeg, belt_75.result])


func test_zero_armor_is_pen() -> void:
	var r: Dictionary = Ballistics.resolve_hit(gun_381, 20.0, "deck", 0.0, 0.0)
	check(String(r.result) == "pen", "장갑 0mm 는 관통: %s" % r.result)


# --- 사격 통제 -------------------------------------------------------------

func test_spread_converges() -> void:
	var s0: float = Ballistics.salvo_spread_m(fc, 15.0, 0, 1.0)
	var s3: float = Ballistics.salvo_spread_m(fc, 15.0, 3, 1.0)
	var s10: float = Ballistics.salvo_spread_m(fc, 15.0, 10, 1.0)
	var s50: float = Ballistics.salvo_spread_m(fc, 15.0, 50, 1.0)
	check(s0 > s3, "분산 n=0 > n=3 (%.2f vs %.2f)" % [s0, s3])
	check(s3 > s10, "분산 n=3 > n=10 (%.2f vs %.2f)" % [s3, s10])
	var floor_m: float = float(fc.minSpreadPerKm) * 15.0
	check(absf(s50 - floor_m) < 1.0, "n=50 분산이 minSpreadPerKm × 거리에 수렴 (%.3f vs %.1f)" % [s50, floor_m])


func test_bad_stability_spreads_more() -> void:
	var good: float = Ballistics.salvo_spread_m(fc, 15.0, 3, 1.0)
	var bad: float = Ballistics.salvo_spread_m(fc, 15.0, 3, 0.9)
	check(bad > good, "명중률 배수 0.9 는 분산이 더 큼 (%.2f vs %.2f)" % [bad, good])


# --- 함선 상태 -------------------------------------------------------------

func test_ship_from_design() -> void:
	var ship: Variant = ShipState.create("p1", 0, player, db, Vector2.ZERO, 0.0)
	check(ship != null, "player_bb_fast 함선 생성")
	if ship == null:
		return
	var calc := ShipCalc.compute_all(player, db)
	var expect_speed: float = float(calc.performance.maxSpeedKn) * 0.514444
	in_range(ship.max_speed_ms, expect_speed - 0.001, expect_speed + 0.001, "max_speed_ms")
	in_range(ship.max_hp, float(calc.weights.groups.structure) - 0.001,
		float(calc.weights.groups.structure) + 0.001, "max_hp")
	check(ship.guns.size() == player.weapons.size(), "무장 슬롯 수 %d == %d" % [ship.guns.size(), player.weapons.size()])


func test_locked_design_ok() -> void:
	var ship: Variant = ShipState.create("qe1", 0, qe, db, Vector2.ZERO, 0.0)
	check(ship != null, "특별 군함(퀸 엘리자베스) 생성")
	if ship == null:
		return
	check(ship.max_hp > 0.0, "max_hp > 0 (%.0f)" % ship.max_hp)
	check(ship.max_speed_ms > 0.0, "max_speed_ms > 0 (%.2f)" % ship.max_speed_ms)


func test_accelerates_and_turns() -> void:
	var ship: Variant = ShipState.create("p1", 0, player, db, Vector2.ZERO, 0.0)
	if ship == null:
		check(false, "함선 생성 실패")
		return
	ship.set_speed_order(1.0)
	for i in 300:
		ship.step_movement(float(i), 1.0)
	check(ship.speed_ms >= ship.max_speed_ms * 0.95,
		"300초 후 속도 %.2f m/s >= 최고의 95%% (%.2f)" % [ship.speed_ms, ship.max_speed_ms * 0.95])

	ship.set_heading_order(90.0)
	for i in 600:
		ship.step_movement(300.0 + float(i), 1.0)
	in_range(ship.heading_deg, 89.0, 91.0, "침로(°)")


func test_big_turn_resets_correction() -> void:
	var sim := BattleSim.new(db, 7)
	var a: Variant = ShipState.create("a", 0, qe, db, Vector2.ZERO, 0.0)
	var b: Variant = ShipState.create("b", 1, qe, db, Vector2(12000.0, 0.0), 270.0)
	sim.add_ship(a)
	sim.add_ship(b)
	sim.order_target("a", "b")
	sim.order_target("b", "a")
	# 정지한 배는 선회하지 못하므로 먼저 속도를 낸다.
	sim.order_speed("a", 1.0)
	for i in 200:
		sim.step(1.0)
	check(a.shots_on_target > 0, "사격 후 shots_on_target > 0 (%d)" % a.shots_on_target)
	sim.order_heading("a", wrapf(a.heading_deg + 90.0, 0.0, 360.0))
	sim.step(1.0)
	check(a.shots_on_target == 0, "급선회 후 shots_on_target == 0 (%d)" % a.shots_on_target)


func test_deterministic() -> void:
	var r1 := _duel(11)
	var r2 := _duel(11)
	check(r1.log_size == r2.log_size, "로그 크기 동일 (%d vs %d)" % [r1.log_size, r2.log_size])
	check(r1.last == r2.last, "마지막 로그 동일")
	check(absf(r1.hp0 - r2.hp0) < 0.0001 and absf(r1.hp1 - r2.hp1) < 0.0001,
		"양측 hp 동일 (%.3f/%.3f vs %.3f/%.3f)" % [r1.hp0, r1.hp1, r2.hp0, r2.hp1])


func test_different_seed_differs() -> void:
	var r1 := _duel(11)
	var r2 := _duel(99)
	check(absf(r1.hp0 - r2.hp0) > 0.0001 or absf(r1.hp1 - r2.hp1) > 0.0001,
		"다른 seed 는 hp 결과가 다름 (%.3f/%.3f vs %.3f/%.3f)" % [r1.hp0, r1.hp1, r2.hp0, r2.hp1])


func test_close_range_penetrates_more() -> void:
	var close := _fixed_range_pen_ratio(6.0)
	var far := _fixed_range_pen_ratio(18.0)
	check(close > far, "6km 관통 비율 %.3f > 18km %.3f" % [close, far])


func test_battle_ends() -> void:
	var sim := BattleSim.new(db, 2024)
	var half := 9000.0
	sim.add_ship(ShipState.create("qe1", 0, qe, db, Vector2(-400.0, -half), 0.0))
	sim.add_ship(ShipState.create("qe2", 0, qe, db, Vector2(400.0, -half), 0.0))
	sim.add_ship(ShipState.create("bb1", 1, player, db, Vector2(-400.0, half), 180.0))
	sim.add_ship(ShipState.create("bb2", 1, player, db, Vector2(400.0, half), 180.0))
	for s in sim.ships:
		sim.order_speed(s.id, 1.0)
	var turned := {}
	while sim.time < 3600.0 and sim.winner() == -1:
		for s in sim.ships:
			if s.sunk:
				continue
			var target: Variant = _nearest_enemy(sim, s)
			if target == null:
				continue
			sim.order_target(s.id, target.id)
			if s.pos.distance_to(target.pos) <= 9000.0 and not turned.has(s.id):
				turned[s.id] = true
				var bearing: float = rad_to_deg(atan2(target.pos.x - s.pos.x, target.pos.y - s.pos.y))
				sim.order_heading(s.id, wrapf(bearing + 90.0, 0.0, 360.0))
		sim.step(1.0)
	var damaged := false
	for s in sim.ships:
		if s.hp < s.max_hp:
			damaged = true
	check(sim.winner() != -1 or damaged, "3600초 안에 승부 또는 피해 (승자 %d, 경과 %.0f초)" % [sim.winner(), sim.time])


func test_sunk_ship_stops() -> void:
	var sim := BattleSim.new(db, 5)
	var a: Variant = ShipState.create("a", 0, qe, db, Vector2.ZERO, 0.0)
	var b: Variant = ShipState.create("b", 1, qe, db, Vector2(10000.0, 0.0), 270.0)
	sim.add_ship(a)
	sim.add_ship(b)
	sim.order_target("a", "b")
	sim.order_target("b", "a")
	sim.order_speed("a", 1.0)
	a.hp = 0.0
	sim.step(1.0)
	check(a.sunk, "hp 0 → sunk")
	check(a.speed_ms == 0.0, "침몰 함선 속도 0 (%.3f)" % a.speed_ms)
	# 첫 단계에도 쏘지 않았어야 하므로, 이 뒤로 a 가 쏜 포탄은 하나도 없어야 한다.
	var a_shells := 0
	for sh in sim.shells:
		if String(sh.shooter_id) == "a":
			a_shells += 1
	check(a_shells == 0, "hp 0 인 함선은 사격하지 않음 (비행 중 포탄 %d)" % a_shells)
	for i in 300:
		sim.step(1.0)
	var fired_after := false
	for e in sim.log:
		if String(e.kind) == "hit" and String(e.shooter) == "a":
			fired_after = true
	check(not fired_after, "침몰한 함선이 쏜 hit 로그 없음")


# --- 항해·지휘·AI ----------------------------------------------------------

func test_bearing() -> void:
	in_range(Navigation.bearing_deg(Vector2.ZERO, Vector2(0.0, 1.0)), -0.001, 0.001, "북쪽 침로(°)")
	in_range(Navigation.bearing_deg(Vector2.ZERO, Vector2(1.0, 0.0)), 89.999, 90.001, "동쪽 침로(°)")
	in_range(Navigation.bearing_deg(Vector2.ZERO, Vector2(0.0, -1.0)), 179.999, 180.001, "남쪽 침로(°)")
	in_range(Navigation.bearing_deg(Vector2.ZERO, Vector2(-1.0, 0.0)), 269.999, 270.001, "서쪽 침로(°)")
	in_range(Navigation.angle_diff_deg(350.0, 10.0), 19.999, 20.001, "angle_diff(350, 10)(°)")


func test_move_to_waypoint() -> void:
	var sim := BattleSim.new(db, 3)
	sim.add_ship(ShipState.create("qe1", 0, qe, db, Vector2.ZERO, 0.0))
	var cmd := FleetCommander.new(sim, 0)
	var wps: Array[Vector2] = [Vector2(5000.0, 5000.0)]
	cmd.order_move("qe1", wps, 1.0)
	var ship: Variant = sim.get_ship("qe1")
	var t := 0.0
	while t < 1800.0 and cmd.mode_of("qe1") == FleetCommander.MODE_MOVE:
		cmd.update(1.0)
		sim.step(1.0)
		t += 1.0
	check(cmd.mode_of("qe1") == FleetCommander.MODE_HOLD,
		"경유점 도착 후 명령이 hold (%.0f초, 거리 %.0fm)" % [t, ship.pos.distance_to(Vector2(5000.0, 5000.0))])
	check(Navigation.arrived(ship, Vector2(5000.0, 5000.0), FleetCommander.ARRIVE_RADIUS_M),
		"경유점 300m 안 도착 (거리 %.0fm)" % ship.pos.distance_to(Vector2(5000.0, 5000.0)))


func test_column_keeps_spacing() -> void:
	var sim := BattleSim.new(db, 4)
	var ids: Array[String] = ["qe1", "qe2", "qe3"]
	for i in ids.size():
		sim.add_ship(ShipState.create(ids[i], 0, qe, db, Vector2(0.0, -600.0 * float(i)), 0.0))
	var cmd := FleetCommander.new(sim, 0)
	cmd.form_column(ids)
	cmd.order_heading("qe1", 90.0, 1.0)
	for i in 600:
		cmd.update(1.0)
		sim.step(1.0)
	cmd.order_heading("qe1", 0.0, 1.0)
	for i in 900:
		cmd.update(1.0)
		sim.step(1.0)
	var s1: Variant = sim.get_ship("qe1")
	var s2: Variant = sim.get_ship("qe2")
	var s3: Variant = sim.get_ship("qe3")
	in_range(s1.pos.distance_to(s2.pos), 400.0, 900.0, "1·2번 간격(m)")
	in_range(s2.pos.distance_to(s3.pos), 400.0, 900.0, "2·3번 간격(m)")
	in_range(absf(Navigation.angle_diff_deg(0.0, s3.heading_deg)), 0.0, 10.0,
		"3번 배 침로가 기함 침로(0°) ±10° (%.1f°)" % s3.heading_deg)


func test_follow_after_leader_sunk() -> void:
	var sim := BattleSim.new(db, 6)
	var ids: Array[String] = ["qe1", "qe2", "qe3"]
	for i in ids.size():
		sim.add_ship(ShipState.create(ids[i], 0, qe, db, Vector2(0.0, -600.0 * float(i)), 0.0))
	var cmd := FleetCommander.new(sim, 0)
	cmd.form_column(ids)
	cmd.order_heading("qe1", 90.0, 1.0)
	for i in 300:
		cmd.update(1.0)
		sim.step(1.0)
	var leader: Variant = sim.get_ship("qe1")
	leader.hp = 0.0
	sim.step(1.0)
	check(leader.sunk, "기함 침몰")
	for i in 300:
		cmd.update(1.0)
		sim.step(1.0)
	check(cmd.mode_of("qe2") != FleetCommander.MODE_FOLLOW,
		"2번 배가 기함이 됨 (mode %s)" % cmd.mode_of("qe2"))
	in_range(absf(Navigation.angle_diff_deg(90.0, sim.get_ship("qe2").heading_deg)), 0.0, 10.0,
		"2번 배가 기존 침로(90°) 유지 (%.1f°)" % sim.get_ship("qe2").heading_deg)


func test_ai_approaches_then_engages() -> void:
	var sim := BattleSim.new(db, 8)
	sim.add_ship(ShipState.create("qe1", 0, qe, db, Vector2.ZERO, 0.0))
	sim.add_ship(ShipState.create("bb1", 1, player, db, Vector2(0.0, 25000.0), 180.0))
	var cmd := FleetCommander.new(sim, 0)
	var ai := EnemyAI.new(sim, cmd, 0)
	ai.update(1.0)
	check(ai.state_of("qe1") == EnemyAI.STATE_APPROACH,
		"첫 판단은 approach (%s)" % ai.state_of("qe1"))
	var pref: float = ai.preferred_range_of("qe1")
	var engage_t := -1.0
	var in_band := 0
	var engage_steps := 0
	var t := 0.0
	while t < 1200.0:
		ai.update(1.0)
		cmd.update(1.0)
		sim.step(1.0)
		t += 1.0
		if ai.state_of("qe1") == EnemyAI.STATE_ENGAGE:
			if engage_t < 0.0:
				engage_t = t
			engage_steps += 1
			var d: float = sim.get_ship("qe1").pos.distance_to(sim.get_ship("bb1").pos)
			if absf(d - pref) <= 3000.0:
				in_band += 1
	check(engage_t >= 0.0 and engage_t <= 1200.0, "1200초 안에 engage 진입 (%.0f초)" % engage_t)
	check(engage_steps > 0 and float(in_band) / float(maxi(engage_steps, 1)) >= 0.5,
		"engage 중 선호 거리 ±3000m 유지 비율 %.2f" % (float(in_band) / float(maxi(engage_steps, 1))))


func test_ai_retreats_when_damaged() -> void:
	var sim := BattleSim.new(db, 9)
	var a: Variant = ShipState.create("qe1", 0, qe, db, Vector2.ZERO, 0.0)
	var b: Variant = ShipState.create("bb1", 1, player, db, Vector2(0.0, 12000.0), 180.0)
	sim.add_ship(a)
	sim.add_ship(b)
	var cmd := FleetCommander.new(sim, 0)
	var ai := EnemyAI.new(sim, cmd, 0)
	for i in 60:
		ai.update(1.0)
		cmd.update(1.0)
		sim.step(1.0)
	a.hp = a.max_hp * 0.25
	ai.update(1.0)
	check(ai.state_of("qe1") == EnemyAI.STATE_RETREAT,
		"hp 25%% → retreat (%s)" % ai.state_of("qe1"))
	var d0: float = a.pos.distance_to(b.pos)
	for i in 300:
		ai.update(1.0)
		cmd.update(1.0)
		sim.step(1.0)
	var d1: float = a.pos.distance_to(b.pos)
	check(d1 > d0, "퇴각 후 적과 거리 증가 (%.0fm → %.0fm)" % [d0, d1])


func test_ai_battle_finishes() -> void:
	var r := _ai_battle(2024)
	check(int(r.winner) != -1, "5400초 안에 승부 (승자 %d, 경과 %.0f초)" % [int(r.winner), float(r.time)])


func test_deterministic_ai() -> void:
	var r1 := _ai_battle(777)
	var r2 := _ai_battle(777)
	check(absf(float(r1.time) - float(r2.time)) < 0.0001,
		"경과 시간 동일 (%.0f vs %.0f)" % [float(r1.time), float(r2.time)])
	check(int(r1.winner) == int(r2.winner), "승자 동일 (%d vs %d)" % [int(r1.winner), int(r2.winner)])
	var hp1: Array = r1.hp
	var hp2: Array = r2.hp
	var same := hp1.size() == hp2.size()
	if same:
		for i in hp1.size():
			if absf(float(hp1[i]) - float(hp2[i])) > 0.0001:
				same = false
	check(same, "함선별 hp 동일")


# --- 시나리오 -------------------------------------------------------------

func test_scenario_builds() -> void:
	var scenario := BattleScenario.load_scenario("res://data/scenarios/test_battle.json")
	check(not scenario.is_empty(), "시나리오 로드")
	if scenario.is_empty():
		return
	var built := BattleScenario.build(scenario, db)
	var sim: Variant = built.sim
	var commanders: Array = built.commanders
	var ais: Array = built.ais
	check(sim.ships.size() == 6, "함선 6척 (%d)" % sim.ships.size())
	check(sim.side_alive(0) == 3 and sim.side_alive(1) == 3,
		"진영별 3척 (아군 %d, 적군 %d)" % [sim.side_alive(0), sim.side_alive(1)])
	check(sim.get_ship("a2") != null and sim.get_ship("e3") != null, "id a2·e3 존재")
	var cmd0: Variant = commanders[0]
	check(cmd0.mode_of("a2") == FleetCommander.MODE_FOLLOW,
		"a2 가 follow 모드 (%s)" % cmd0.mode_of("a2"))
	var ai0: Variant = ais[0]
	var ai1: Variant = ais[1]
	while sim.time < 300.0:
		if ai0 != null:
			ai0.update(1.0)  # 아군이 플레이어 지휘면 null
		ai1.update(1.0)
		cmd0.update(1.0)
		commanders[1].update(1.0)
		sim.step(1.0)
	in_range(sim.time, 299.999, 300.001, "300초 진행 후 시간")


# --- 전투 도우미 -----------------------------------------------------------
## battle_report 와 같은 3대3 AI 시나리오. {time, winner, hp}
func _ai_battle(seed_value: int) -> Dictionary:
	var sim := BattleSim.new(db, seed_value)
	var half := 12500.0
	var ids0: Array[String] = ["qe1", "qe2", "qe3"]
	var ids1: Array[String] = ["bb1", "bb2", "bb3"]
	for i in ids0.size():
		var off := 600.0 * float(i)
		sim.add_ship(ShipState.create(ids0[i], 0, qe, db, Vector2(0.0, -half - off), 0.0))
		sim.add_ship(ShipState.create(ids1[i], 1, player, db, Vector2(0.0, half + off), 180.0))
	var cmd0 := FleetCommander.new(sim, 0)
	var cmd1 := FleetCommander.new(sim, 1)
	cmd0.form_column(ids0)
	cmd1.form_column(ids1)
	var ai0 := EnemyAI.new(sim, cmd0, 0)
	var ai1 := EnemyAI.new(sim, cmd1, 1)
	var broken := {}
	while sim.time < 5400.0 and sim.winner() == -1:
		ai0.update(1.0)
		ai1.update(1.0)
		_break_engaged(ai0, cmd0, broken)
		_break_engaged(ai1, cmd1, broken)
		cmd0.update(1.0)
		cmd1.update(1.0)
		sim.step(1.0)
	var hp: Array = []
	for s in sim.ships:
		hp.append(float(s.hp))
	return {"time": sim.time, "winner": sim.winner(), "hp": hp}


func _break_engaged(ai: Variant, cmd: Variant, broken: Dictionary) -> void:
	for s in ai.sim.ships:
		if int(s.side) != int(ai.side) or s.sunk:
			continue
		var id := String(s.id)
		if broken.has(id):
			continue
		if ai.state_of(id) == EnemyAI.STATE_ENGAGE:
			broken[id] = true
			cmd.break_formation(id)


## QE 1척 vs player 1척, 12km, 600초 고정 시나리오.
func _duel(seed_value: int) -> Dictionary:
	var sim := BattleSim.new(db, seed_value)
	var a: Variant = ShipState.create("a", 0, qe, db, Vector2.ZERO, 0.0)
	var b: Variant = ShipState.create("b", 1, player, db, Vector2(12000.0, 0.0), 270.0)
	sim.add_ship(a)
	sim.add_ship(b)
	sim.order_target("a", "b")
	sim.order_target("b", "a")
	for i in 600:
		sim.step(1.0)
	var last := ""
	if not sim.log.is_empty():
		last = str(sim.log[sim.log.size() - 1])
	return {"log_size": sim.log.size(), "last": last, "hp0": a.hp, "hp1": b.hp}


## QE 대 QE, 고정 거리에서 600초. 관통 비율을 돌려준다.
func _fixed_range_pen_ratio(range_km: float) -> float:
	var sim := BattleSim.new(db, 31)
	var a: Variant = ShipState.create("a", 0, qe, db, Vector2.ZERO, 0.0)
	# 양쪽 모두 북쪽을 향해 나란히 놓아 서로 현측을 보인다(관통 판정만 비교하려는 배치).
	var b: Variant = ShipState.create("b", 1, qe, db, Vector2(range_km * 1000.0, 0.0), 0.0)
	sim.add_ship(a)
	sim.add_ship(b)
	sim.order_target("a", "b")
	sim.order_target("b", "a")
	for i in 600:
		sim.step(1.0)
	var pen := 0
	var total := 0
	for e in sim.log:
		# 부포는 가까운 거리에서만 쏘고 전함 벨트를 뚫지 못하므로 주포만 센다.
		if String(e.kind) != "hit" or String(e.result) == "miss" or not String(e.gun).begins_with("main"):
			continue
		total += 1
		if String(e.result) == "pen":
			pen += 1
	return float(pen) / float(maxi(total, 1))


func _nearest_enemy(sim: BattleSim, ship: Variant) -> Variant:
	var best: Variant = null
	var best_d := INF
	for other in sim.ships:
		if int(other.side) == int(ship.side) or other.sunk:
			continue
		var d: float = ship.pos.distance_to(other.pos)
		if d < best_d:
			best_d = d
			best = other
	return best


# --- 전장 이탈과 시간 제한 (C2b-1) ------------------------------------------

func test_withdraw_at_edge() -> void:
	var sim := BattleSim.new(db, 41)
	sim.map_half_m = 5000.0
	var a: Variant = ShipState.create("a", 0, qe, db, Vector2(4500.0, 0.0), 90.0)
	var b: Variant = ShipState.create("b", 1, qe, db, Vector2(-3000.0, 0.0), 90.0)
	sim.add_ship(a)
	sim.add_ship(b)
	sim.order_speed("a", 1.0)
	a.speed_ms = a.max_speed_ms  # 이미 전속으로 달리는 중
	for i in 120:
		sim.step(1.0)
	check(bool(a.withdrawn) and not a.is_active(), "경계를 넘으면 이탈 (x = %.0f)" % a.pos.x)
	check(sim.side_alive(0) == 0, "이탈한 배는 생존 수에서 빠짐")
	var r := sim.result()
	check(int(r.winner) == 1 and String(r.reason) == "withdrawal", "전원 이탈 → 상대 승, 사유 퇴각 (%s)" % [r])


func test_withdrawn_not_targeted() -> void:
	var sim := BattleSim.new(db, 42)
	sim.map_half_m = 13000.0
	# a 는 동쪽 경계 근처에서 동쪽으로 달려 이탈한다. b 는 서로 사거리 안에서 정지.
	var a: Variant = ShipState.create("a", 0, qe, db, Vector2(12000.0, 0.0), 90.0)
	var b: Variant = ShipState.create("b", 1, qe, db, Vector2(0.0, 0.0), 0.0)
	sim.add_ship(a)
	sim.add_ship(b)
	sim.order_target("a", "b")
	sim.order_target("b", "a")
	sim.order_speed("a", 1.0)
	a.speed_ms = a.max_speed_ms
	var t_out := -1.0
	for i in 600:
		sim.step(1.0)
		if t_out < 0.0 and bool(a.withdrawn):
			t_out = sim.time
	check(t_out > 0.0, "a 이탈 (%.0f초)" % t_out)
	var bad_target := false
	var bad_shooter := false
	for e in sim.log:
		if String(e.kind) != "hit":
			continue
		if String(e.target) == "a" and float(e.t) > t_out:
			bad_target = true
		if String(e.shooter) == "a" and float(e.t) > t_out + 40.0:
			bad_shooter = true
	check(not bad_target, "이탈 뒤 그 배를 맞힌 기록 없음")
	check(not bad_shooter, "이탈 뒤 그 배가 쏜 기록 없음")


func test_time_limit_judgement() -> void:
	var sim := BattleSim.new(db, 43)
	sim.time_limit_s = 60.0
	var a: Variant = ShipState.create("a", 0, qe, db, Vector2(0.0, 0.0), 0.0)
	var b: Variant = ShipState.create("b", 1, qe, db, Vector2(40000.0, 0.0), 0.0)
	sim.add_ship(a)
	sim.add_ship(b)
	b.hp = b.max_hp * 0.5
	for i in 59:
		sim.step(1.0)
	check(sim.winner() == -1, "시간 제한 전에는 진행 중")
	sim.step(1.0)
	var r := sim.result()
	check(int(r.winner) == 0 and String(r.reason) == "time", "남은 전력이 많은 쪽이 시간 판정승 (%s)" % [r])
	b.hp = a.hp
	check(sim.winner() == 2, "남은 전력이 같으면 무승부")


func test_ai_stays_in_map() -> void:
	var sc: Dictionary = BattleScenario.load_scenario("res://data/scenarios/test_battle.json")
	var built: Dictionary = BattleScenario.build(sc, db)
	var sim: Variant = built.sim
	var half: float = float(sim.map_half_m)
	check(half > 0.0, "시나리오에 맵 경계가 있음")
	var outside := 0
	for i in 4800:
		for ai in built.ais:
			if ai != null:
				ai.update(0.5)
		for cmd in built.commanders:
			cmd.update(0.5)
		sim.step(0.5)
		for s in sim.ships:
			if not s.is_active():
				continue
			var ai: Variant = built.ais[int(s.side)]
			if ai == null or String(ai.state_of(String(s.id))) == EnemyAI.STATE_RETREAT:
				continue
			if absf(s.pos.x) > half or absf(s.pos.y) > half:
				outside += 1
	check(outside == 0, "퇴각하지 않는 배는 맵 안에 머묾 (경계 밖 %d 회)" % outside)


func test_battle_resolves() -> void:
	var sc: Dictionary = BattleScenario.load_scenario("res://data/scenarios/test_battle.json")
	var built: Dictionary = BattleScenario.build(sc, db)
	var sim: Variant = built.sim
	var limit: float = float(sim.time_limit_s)
	check(limit > 0.0, "시나리오에 시간 제한이 있음")
	while sim.winner() == -1 and sim.time < limit + 10.0:
		for ai in built.ais:
			if ai != null:
				ai.update(1.0)
		for cmd in built.commanders:
			cmd.update(1.0)
		sim.step(1.0)
	var r: Dictionary = sim.result()
	check(int(r.winner) != -1, "시간 제한 안에 반드시 끝남 (%.0f초, %s)" % [sim.time, r])


# --- 플레이어 지휘 (C2b-2) ----------------------------------------------------

func test_scenario_player_control() -> void:
	var sc: Dictionary = BattleScenario.load_scenario("res://data/scenarios/test_battle.json")
	var built: Dictionary = BattleScenario.build(sc, db)
	var ais: Array = built.ais
	check(ais.size() == 2 and ais[0] == null and ais[1] != null, "아군은 플레이어, 적군만 AI")
	var cmd: Variant = built.commanders[0]
	var m1 := String(cmd.mode_of("a1"))
	check(m1 == "hold" or m1 == "heading", "a1 은 기함 (%s)" % m1)
	check(String(cmd.mode_of("a2")) == "follow" and String(cmd.mode_of("a3")) == "follow", "a2·a3 은 종진")


func test_player_design_override() -> void:
	BattleScenario.player_design_override = qe.duplicate(true)
	var sc: Dictionary = BattleScenario.load_scenario("res://data/scenarios/test_battle.json")
	var built: Dictionary = BattleScenario.build(sc, db)
	BattleScenario.player_design_override = {}
	var sim: Variant = built.sim
	var a1: Variant = sim.get_ship("a1")
	var e1: Variant = sim.get_ship("e1")
	check(a1 != null and String(a1.design.name) == String(qe.name), "아군이 설계 화면의 설계로 만들어짐 (%s)" % String(a1.design.name))
	check(sim.ships.size() == 6, "척 수는 그대로 6척")
	check(String(e1.design.name) == String(qe.name), "적군은 시나리오 설계 그대로")

