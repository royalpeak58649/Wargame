extends RefCounted
## 한 진영(side)의 명령을 들고 매 단계 BattleSim 에 조타 명령을 내린다 (순수 로직, 노드 없음).
##
## 플레이어 UI 와 적 AI 가 같은 클래스를 쓴다. 함선 필드는 직접 바꾸지 않고
## BattleSim.order_heading / order_speed / order_target 만 쓴다.
## 난수를 쓰지 않는다: 같은 명령 열이면 항상 같은 결과가 나온다.
##
## 좌표: Vector2, 미터. x = 동쪽, y = 북쪽. 침로: 도, 북쪽 0, 시계 방향 증가.
##
## 사용법:
##   var cmd = FleetCommander.new(sim, 0)
##   cmd.form_column(["qe1", "qe2", "qe3"])
##   cmd.order_move("qe1", [Vector2(5000.0, 5000.0)], 1.0)
##   cmd.update(1.0)   # sim.step(dt) 전에 호출

const Navigation = preload("res://scripts/combat/navigation.gd")

# --- 밸런스 상수 (조정 대상) -------------------------------------------------

const ARRIVE_RADIUS_M := 300.0     # m, 경유점 도착 판정 반경
const TRAIL_SAMPLE_M := 100.0      # m, 항적 기록 간격
const TRAIL_MAX := 200             # 점, 함선마다 보관하는 항적 최대 점 수
const COLUMN_SPACING_M := 600.0    # m, 종진에서 앞 배와의 간격
const SPACING_GAIN_M := 1500.0     # m, 간격 오차 1m 당 속도 비율 변화
const SPEED_STEP := 0.25           # 무차원, 허용 속도 단계
const SPEED_MIN := 0.25            # 무차원, 종진 속도 하한
const SPEED_MAX := 1.0             # 무차원, 종진 속도 상한

const MODE_HOLD := "hold"          # 현재 침로 유지
const MODE_MOVE := "move"          # 경유점 목록을 차례로 항해
const MODE_HEADING := "heading"    # 지정 방위 유지
const MODE_FOLLOW := "follow"      # 종진에서 앞 배를 따라감

# --- 필드 -------------------------------------------------------------------

var sim: Variant = null
var side: int = 0
## ship id → 명령 {mode, waypoints, heading_deg, speed_frac, leader_id}
var orders: Dictionary = {}
## ship id → 항적(Array[Vector2]), 오래된 점이 앞
var trails: Dictionary = {}


func _init(sim_ref: Variant, side_id: int) -> void:
	sim = sim_ref
	side = side_id


# --- 명령 API (플레이어 UI·AI 공용) ------------------------------------------

## 경유점을 차례로 항해한다. 마지막 경유점에 도착하면 "hold" 로 바뀐다.
func order_move(ship_id: String, waypoints: Array[Vector2], speed_frac: float) -> void:
	var o := _order(ship_id)
	o.mode = MODE_MOVE
	o.waypoints = waypoints.duplicate()
	o.speed_frac = _snap_speed(speed_frac)
	o.leader_id = ""


## 지정 방위를 유지한다.
func order_heading(ship_id: String, deg: float, speed_frac: float) -> void:
	var o := _order(ship_id)
	o.mode = MODE_HEADING
	o.heading_deg = wrapf(deg, 0.0, 360.0)
	o.speed_frac = _snap_speed(speed_frac)
	o.leader_id = ""


## 속도만 바꾼다. 0, 0.25, 0.5, 0.75, 1.0 중 가장 가까운 값으로 맞춘다.
func order_speed(ship_id: String, frac: float) -> void:
	var o := _order(ship_id)
	o.speed_frac = _snap_speed(frac)


## 사격 목표 지정. BattleSim 에 그대로 넘긴다.
func order_target(ship_id: String, target_id: String) -> void:
	sim.order_target(ship_id, target_id)


## 종진 진형. 첫 배가 기함이고 나머지는 바로 앞 배를 따라간다.
## 기함의 기존 명령은 유지된다.
func form_column(ship_ids: Array[String]) -> void:
	var leader_id := ""
	for id in ship_ids:
		if sim.get_ship(id) == null:
			continue
		var o := _order(id)
		if leader_id == "":
			leader_id = id
			continue
		o.mode = MODE_FOLLOW
		o.leader_id = leader_id
		o.waypoints = []
		leader_id = id


## 진형에서 빠져 현재 침로를 유지한다.
func break_formation(ship_id: String) -> void:
	var o := _order(ship_id)
	o.mode = MODE_HOLD
	o.leader_id = ""
	o.waypoints = []


## 함선의 현재 명령 모드("hold"/"move"/"heading"/"follow"). 없으면 "hold".
func mode_of(ship_id: String) -> String:
	if not orders.has(ship_id):
		return MODE_HOLD
	var o: Dictionary = orders[ship_id]
	return String(o.mode)


## 함선의 현재 명령 Dictionary(읽기용). 없으면 빈 Dictionary.
func order_of(ship_id: String) -> Dictionary:
	if not orders.has(ship_id):
		return {}
	var o: Dictionary = orders[ship_id]
	return o


# --- 시간 단계 ---------------------------------------------------------------

## 매 단계 sim.step 전에 호출. 각 함선의 명령을 조타 명령으로 바꾼다.
func update(dt: float) -> void:
	_update_trails()
	for s in sim.ships:
		if int(s.side) != side or s.sunk:
			continue
		var o := _order(String(s.id))
		match String(o.mode):
			MODE_MOVE:
				_apply_move(s, o)
			MODE_HEADING:
				sim.order_heading(String(s.id), float(o.heading_deg))
				sim.order_speed(String(s.id), float(o.speed_frac))
			MODE_FOLLOW:
				_apply_follow(s, o)
			_:
				_apply_hold(s, o)


func _apply_hold(s: Variant, o: Dictionary) -> void:
	sim.order_heading(String(s.id), float(s.heading_deg))
	sim.order_speed(String(s.id), float(o.speed_frac))


func _apply_move(s: Variant, o: Dictionary) -> void:
	var wps: Array = o.waypoints
	while not wps.is_empty():
		var wp: Vector2 = wps[0]
		if Navigation.arrived(s, wp, ARRIVE_RADIUS_M):
			wps.remove_at(0)
			continue
		sim.order_heading(String(s.id), Navigation.steer_to_point(s, wp))
		sim.order_speed(String(s.id), float(o.speed_frac))
		return
	# 경유점을 모두 지나면 현재 침로 유지
	o.mode = MODE_HOLD
	o.waypoints = []
	_apply_hold(s, o)


func _apply_follow(s: Variant, o: Dictionary) -> void:
	var leader: Variant = sim.get_ship(String(o.leader_id))
	if leader == null or leader.sunk:
		_promote_follower(s, o)
		return
	var target: Vector2 = _trail_point_behind(leader, COLUMN_SPACING_M)
	sim.order_heading(String(s.id), Navigation.steer_to_point(s, target))
	var gap: float = _trail_distance(leader, s.pos)
	var frac: float = float(o.speed_frac) + (gap - COLUMN_SPACING_M) / SPACING_GAIN_M
	sim.order_speed(String(s.id), clampf(frac, SPEED_MIN, SPEED_MAX))


## 앞 배가 침몰했다. 그 앞 배를 따라가고, 없으면 기함이 되어 명령을 이어받는다.
func _promote_follower(s: Variant, o: Dictionary) -> void:
	var old_leader_id := String(o.leader_id)
	var old_leader: Variant = sim.get_ship(old_leader_id)
	var next_id := ""
	if old_leader != null and orders.has(old_leader_id):
		var lo: Dictionary = orders[old_leader_id]
		next_id = String(lo.leader_id)
	if next_id != "" and sim.get_ship(next_id) != null and not sim.get_ship(next_id).sunk:
		o.leader_id = next_id
		o.mode = MODE_FOLLOW
		_apply_follow(s, o)
		return
	# 기함이 된다: 앞 배의 마지막 명령을 이어받는다.
	o.leader_id = ""
	if old_leader != null and orders.has(old_leader_id):
		var lo2: Dictionary = orders[old_leader_id]
		o.mode = String(lo2.mode)
		o.waypoints = (lo2.waypoints as Array).duplicate()
		o.heading_deg = float(lo2.heading_deg)
		o.speed_frac = float(lo2.speed_frac)
	else:
		o.mode = MODE_HOLD
		o.waypoints = []
		o.speed_frac = float(s.order_speed_frac)
	_apply_hold(s, o)


# --- 항적 -------------------------------------------------------------------

func _update_trails() -> void:
	for s in sim.ships:
		if int(s.side) != side or s.sunk:
			continue
		var id := String(s.id)
		var trail: Array = trails.get(id, [])
		if trail.is_empty():
			trail.append(s.pos)
		else:
			var last: Vector2 = trail[trail.size() - 1]
			if last.distance_to(s.pos) >= TRAIL_SAMPLE_M:
				trail.append(s.pos)
		while trail.size() > TRAIL_MAX:
			trail.remove_at(0)
		trails[id] = trail


## 함선 항적에서 그 함선 현재 위치로부터 back_m 뒤쪽 지점.
## 항적이 짧으면 현재 위치 − forward × back_m.
func _trail_point_behind(ship: Variant, back_m: float) -> Vector2:
	var trail: Array = trails.get(String(ship.id), [])
	var acc := 0.0
	var prev: Vector2 = ship.pos
	for i in range(trail.size() - 1, -1, -1):
		var p: Vector2 = trail[i]
		var seg: float = prev.distance_to(p)
		if acc + seg >= back_m:
			var t: float = (back_m - acc) / seg if seg > 0.001 else 0.0
			return prev.lerp(p, t)
		acc += seg
		prev = p
	return ship.pos - ship.forward() * back_m


## 함선 항적을 따라 그 함선 현재 위치까지의 거리(항적 길이).
func _trail_distance(ship: Variant, point: Vector2) -> float:
	var trail: Array = trails.get(String(ship.id), [])
	var acc := 0.0
	var prev: Vector2 = ship.pos
	for i in range(trail.size() - 1, -1, -1):
		var p: Vector2 = trail[i]
		acc += prev.distance_to(p)
		prev = p
	acc += prev.distance_to(point)
	return acc


# --- 내부 -------------------------------------------------------------------

func _order(ship_id: String) -> Dictionary:
	if orders.has(ship_id):
		var existing: Dictionary = orders[ship_id]
		return existing
	var ship: Variant = sim.get_ship(ship_id)
	var heading := 0.0
	var speed := 0.0
	if ship != null:
		heading = float(ship.heading_deg)
		speed = float(ship.order_speed_frac)
	var o := {
		"mode": MODE_HOLD,
		"waypoints": [] as Array[Vector2],
		"heading_deg": heading,
		"speed_frac": speed,
		"leader_id": "",
	}
	orders[ship_id] = o
	return o


## 0, 0.25, 0.5, 0.75, 1.0 중 가장 가까운 값.
func _snap_speed(frac: float) -> float:
	var steps := int(round(clampf(frac, 0.0, 1.0) / SPEED_STEP))
	return float(steps) * SPEED_STEP
