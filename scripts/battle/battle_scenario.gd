extends RefCounted
## 전투 시나리오 JSON 을 읽어 BattleSim·FleetCommander·EnemyAI 를 만든다 (순수 로직, 노드 없음).
##
## 시나리오 형식:
##   {
##     "name": "시험 전투",
##     "seed": 1,
##     "sides": [
##       {"label": "아군", "designs": ["res://...json", ...],
##        "start": [x, y], "heading": 0, "column": true},
##       ...
##     ]
##   }
## sides 는 정확히 2개(side 0, side 1)다. 함선 id 는 side 0 이 "a1","a2",…, side 1 이 "e1","e2",…
## 같은 진영 배는 침로 반대 방향으로 COLUMN_GAP_M 간격으로 늘어선다(battle_report 와 같은 방식).
##
## 사용법:
##   var sc := BattleScenario.load_scenario("res://data/scenarios/test_battle.json")
##   var built := BattleScenario.build(sc, db)
##   var sim: Variant = built.sim

const PartsDB = preload("res://scripts/core/parts_db.gd")
const ShipState = preload("res://scripts/combat/ship_state.gd")
const BattleSim = preload("res://scripts/combat/battle_sim.gd")
const FleetCommander = preload("res://scripts/combat/fleet_commander.gd")
const EnemyAI = preload("res://scripts/combat/enemy_ai.gd")

const COLUMN_GAP_M := 600.0        # m, 종진 함선 간격
const SIDE_ID_PREFIX := ["a", "e"] # side 0 → "a1", side 1 → "e1"
const DEFAULT_SEED := 1


## 시나리오 JSON 을 읽는다. 실패하면 빈 Dictionary.
static func load_scenario(path: String) -> Dictionary:
	var data: Variant = PartsDB.load_json(path)
	if typeof(data) != TYPE_DICTIONARY:
		push_error("시나리오 형식 오류: %s" % path)
		return {}
	return data


## 시나리오로부터 {"sim", "commanders": [c0, c1], "ais": [ai0, ai1]} 를 만든다.
static func build(scenario: Dictionary, db: Dictionary) -> Dictionary:
	var seed_value := int(scenario.get("seed", DEFAULT_SEED))
	var sim := BattleSim.new(db, seed_value)
	var commanders: Array = []
	var ais: Array = []
	var sides: Array = scenario.get("sides", [])
	for side in 2:
		var cmd := FleetCommander.new(sim, side)
		var ids: Array[String] = []
		if side < sides.size():
			ids = _add_side_ships(sim, db, side, sides[side])
		if bool(_side_field(sides, side, "column", false)):
			cmd.form_column(ids)
		commanders.append(cmd)
		ais.append(EnemyAI.new(sim, cmd, side))
	return {"sim": sim, "commanders": commanders, "ais": ais}


## 한 진영의 함선을 만들고 id 목록을 돌려준다.
static func _add_side_ships(sim: Variant, db: Dictionary, side: int, side_data: Dictionary) -> Array[String]:
	var ids: Array[String] = []
	var designs: Array = side_data.get("designs", [])
	var start_arr: Array = side_data.get("start", [0, 0])
	var start := Vector2(float(start_arr[0]), float(start_arr[1]))
	var heading := float(side_data.get("heading", 0.0))
	var prefix := SIDE_ID_PREFIX[side] if side < SIDE_ID_PREFIX.size() else "s%d_" % side
	# 침로 반대 방향으로 늘어선다.
	var h := deg_to_rad(heading)
	var back := -Vector2(sin(h), cos(h))
	for i in designs.size():
		var design: Dictionary = PartsDB.load_design(String(designs[i]))
		if design.is_empty():
			continue
		var id := "%s%d" % [prefix, i + 1]
		var pos: Vector2 = start + back * (COLUMN_GAP_M * float(i))
		var ship: Variant = ShipState.create(id, side, design, db, pos, heading)
		if ship == null:
			continue
		sim.add_ship(ship)
		ids.append(id)
	return ids


static func _side_field(sides: Array, side: int, key: String, default_value: Variant) -> Variant:
	if side >= sides.size():
		return default_value
	var side_data: Dictionary = sides[side]
	return side_data.get(key, default_value)
