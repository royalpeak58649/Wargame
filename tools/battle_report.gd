extends SceneTree
## 전투 시뮬레이션 한 판을 돌려 결과를 출력한다.
## 사용법: godot --headless --path . --script res://tools/battle_report.gd [-- seed]
##
## 시나리오: 영국 퀸 엘리자베스 3척(side 0) vs 플레이어 고속 전함안 3척(side 1).
## 25km 떨어져 마주 보고 시작한다. 양쪽 모두 EnemyAI + FleetCommander 가 지휘한다.
## 시작할 때 각 진영은 종진을 만들고, 교전(engage)에 들어간 함선은 진형을 푼다.

const PartsDB = preload("res://scripts/core/parts_db.gd")
const ShipState = preload("res://scripts/combat/ship_state.gd")
const BattleSim = preload("res://scripts/combat/battle_sim.gd")
const FleetCommander = preload("res://scripts/combat/fleet_commander.gd")
const EnemyAI = preload("res://scripts/combat/enemy_ai.gd")

const DT := 1.0                    # s, 시간 단계
const MAX_TIME := 5400.0           # s, 최대 전투 시간
const START_DISTANCE_M := 25000.0  # m, 시작 거리
const COLUMN_GAP_M := 600.0        # m, 종진 함선 간격
const LOG_TAIL := 20               # 개, 마지막 로그 줄 수


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
	var ids0: Array[String] = ["qe1", "qe2", "qe3"]
	var ids1: Array[String] = ["bb1", "bb2", "bb3"]
	for i in ids0.size():
		var off := float(i) * COLUMN_GAP_M
		sim.add_ship(ShipState.create(ids0[i], 0, qe, db, Vector2(0.0, -half - off), 0.0))
		sim.add_ship(ShipState.create(ids1[i], 1, player, db, Vector2(0.0, half + off), 180.0))

	var cmd0 := FleetCommander.new(sim, 0)
	var cmd1 := FleetCommander.new(sim, 1)
	cmd0.form_column(ids0)
	cmd1.form_column(ids1)
	var ai0 := EnemyAI.new(sim, cmd0, 0)
	var ai1 := EnemyAI.new(sim, cmd1, 1)

	var broken := {}
	while sim.time < MAX_TIME and sim.winner() == -1:
		ai0.update(DT)
		ai1.update(DT)
		_break_engaged(ai0, cmd0, broken)
		_break_engaged(ai1, cmd1, broken)
		cmd0.update(DT)
		cmd1.update(DT)
		sim.step(DT)

	print("")
	print("=== 전투 보고 (seed %d) ===" % seed_value)
	print("경과 시간: %.0f초 (%.1f분)" % [sim.time, sim.time / 60.0])
	var w := sim.winner()
	print("승자: %s" % ("무승부/미결" if w == -1 else "side %d" % w))
	print("")
	_print_summary(sim)
	print("")
	_print_ai_states(sim, ai0, ai1)
	print("")
	_print_log_tail(sim)
	quit(0)


## 교전에 들어간 함선은 종진에서 빠진다.
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


func _print_ai_states(sim: BattleSim, ai0: Variant, ai1: Variant) -> void:
	print("함선별 최종 AI 상태:")
	for s in sim.ships:
		var ai: Variant = ai0 if int(s.side) == 0 else ai1
		print("  %-6s side %d  상태 %-8s  목표 %-6s  선호 거리 %.1fkm" % [
			s.id, int(s.side), ai.state_of(String(s.id)),
			String(ai.targets.get(String(s.id), "-")),
			ai.preferred_range_of(String(s.id)) / 1000.0])


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
