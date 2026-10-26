extends SceneTree
## 전투 시뮬레이션 한 판을 돌려 결과를 출력한다.
## 사용법: godot --headless --path . --script res://tools/battle_report.gd [-- seed]
##
## 시나리오: 영국 퀸 엘리자베스 2척(side 0) vs 플레이어 고속 전함안 2척(side 1).
## 18km 떨어져 마주 보고 시작, 양쪽 전속, 가장 가까운 적을 목표로 한다.
## 9km 이내가 되면 침로를 상대 방향 +90°로 바꿔 현측을 보인다.
## 이 기동 로직은 도구 안의 간단한 스크립트다(AI 아님).

const PartsDB = preload("res://scripts/core/parts_db.gd")
const ShipState = preload("res://scripts/combat/ship_state.gd")
const BattleSim = preload("res://scripts/combat/battle_sim.gd")

const DT := 1.0                 # s, 시간 단계
const MAX_TIME := 3600.0        # s, 최대 전투 시간
const START_DISTANCE_M := 18000.0  # m, 시작 거리
const TURN_RANGE_M := 9000.0    # m, 이 거리 안이면 현측을 보인다
const LOG_TAIL := 20            # 개, 마지막 로그 줄 수


func _init() -> void:
	var args := OS.get_cmdline_user_args()
	var seed_value := 12345
	if not args.is_empty():
		seed_value = int(args[0])

	var db := PartsDB.load_db("res://data/parts_t2.json")
	var qe := PartsDB.load_design("res://data/designs/uk_bb_queen_elizabeth.json")
	var player := PartsDB.load_design("res://data/designs/player_bb_fast.json")

	var sim := BattleSim.new(db, seed_value)
	# side 0: 남쪽에서 북쪽으로, side 1: 북쪽에서 남쪽으로
	var half := START_DISTANCE_M / 2.0
	sim.add_ship(ShipState.create("qe1", 0, qe, db, Vector2(-400.0, -half), 0.0))
	sim.add_ship(ShipState.create("qe2", 0, qe, db, Vector2(400.0, -half), 0.0))
	sim.add_ship(ShipState.create("bb1", 1, player, db, Vector2(-400.0, half), 180.0))
	sim.add_ship(ShipState.create("bb2", 1, player, db, Vector2(400.0, half), 180.0))

	for s in sim.ships:
		sim.order_speed(s.id, 1.0)

	var turned := {}
	while sim.time < MAX_TIME and sim.winner() == -1:
		_script_orders(sim, turned)
		sim.step(DT)

	print("")
	print("=== 전투 보고 (seed %d) ===" % seed_value)
	print("경과 시간: %.0f초 (%.1f분)" % [sim.time, sim.time / 60.0])
	var w := sim.winner()
	print("승자: %s" % ("무승부/미결" if w == -1 else "side %d" % w))
	print("")
	_print_summary(sim)
	print("")
	_print_log_tail(sim)
	quit(0)


## 간단한 기동 스크립트: 가장 가까운 적을 목표로, 9km 이내면 현측을 보인다.
func _script_orders(sim: BattleSim, turned: Dictionary) -> void:
	for s in sim.ships:
		if s.sunk:
			continue
		var target: Variant = _nearest_enemy(sim, s)
		if target == null:
			continue
		sim.order_target(s.id, target.id)
		var dist: float = s.pos.distance_to(target.pos)
		if dist <= TURN_RANGE_M and not turned.has(s.id):
			turned[s.id] = true
			var bearing: float = rad_to_deg(atan2(target.pos.x - s.pos.x, target.pos.y - s.pos.y))
			sim.order_heading(s.id, wrapf(bearing + 90.0, 0.0, 360.0))


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


func _print_summary(sim: BattleSim) -> void:
	var sum := sim.summary()
	print("%-6s %-4s %6s %6s %5s %6s %6s %5s %10s %10s %5s" % [
		"함선", "side", "발사", "명중", "관통", "불관통", "도탄", "빗나감", "가한 피해", "받은 피해", "침몰"])
	for s in sim.ships:
		var r: Dictionary = sum[s.id]
		print("%-6s %-4d %6d %6d %5d %6d %6d %5d %10.0f %10.0f %5s" % [
			s.id, int(s.side), int(r.fired), int(r.hits), int(r.pen), int(r.nonpen),
			int(r.ricochet), int(r.miss), float(r.damageDealt), float(r.damageTaken),
			"예" if bool(r.sunk) else "아니오"])
	print("")
	for s in sim.ships:
		print("%-6s HP %.0f / %.0f, 속도 %.1fkn, 화재 %d, 침수 %d" % [
			s.id, maxf(s.hp, 0.0), s.max_hp, s.speed_ms / ShipState.KNOT_TO_MS,
			s.fires.size(), s.floods.size()])


func _print_log_tail(sim: BattleSim) -> void:
	print("마지막 %d개 로그:" % LOG_TAIL)
	var start: int = maxi(sim.log.size() - LOG_TAIL, 0)
	for i in range(start, sim.log.size()):
		var e: Dictionary = sim.log[i]
		var effects: Array = e.effects
		print("  %6.0fs %-8s %-5s → %-5s %-8s %-6s %-5s %-8s %5.1fkm 관통 %5.0f / 유효 %5.0f 피해 %5.0f %s" % [
			float(e.t), String(e.kind), String(e.shooter), String(e.target), String(e.gun),
			String(e.zone), String(e.plate), String(e.result), float(e.rangeKm),
			float(e.penMm), float(e.effArmorMm), float(e.damage), ",".join(effects)])
