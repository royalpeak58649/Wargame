extends SceneTree
## 전투 탄도·관통 계산 모듈 단위 테스트.
## 실행: godot --headless --path . --script res://tests/run_combat_tests.gd
## 실패가 하나라도 있으면 종료 코드 1.

const Ballistics = preload("res://scripts/combat/ballistics.gd")
const PartsDB = preload("res://scripts/core/parts_db.gd")
const ShipCalc = preload("res://scripts/core/ship_calc.gd")
const ShipState = preload("res://scripts/combat/ship_state.gd")
const BattleSim = preload("res://scripts/combat/battle_sim.gd")

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
	check(absf(s50 - float(fc.minSpreadM)) < 1.0, "n=50 분산이 minSpreadM 에 수렴 (%.3f vs %.1f)" % [s50, fc.minSpreadM])


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


# --- 전투 도우미 -----------------------------------------------------------

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
	var b: Variant = ShipState.create("b", 1, qe, db, Vector2(range_km * 1000.0, 0.0), 270.0)
	sim.add_ship(a)
	sim.add_ship(b)
	sim.order_target("a", "b")
	sim.order_target("b", "a")
	for i in 600:
		sim.step(1.0)
	var pen := 0
	var total := 0
	for e in sim.log:
		if String(e.kind) != "hit" or String(e.result) == "miss":
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
