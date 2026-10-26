extends RefCounted
## 적 함대 AI (순수 로직, 노드 없음).
##
## 함선마다 상태 기계를 돌린다: search → approach → engage, 그리고 retreat.
## AI_THINK_SEC 마다 판단하고 그 사이에는 이전 명령을 유지한다.
## 조작은 FleetCommander.order_heading / order_speed / order_target 으로만 한다.
## 난수를 쓰지 않는다: 같은 전장이면 항상 같은 판단이 나온다.
##
## 좌표: Vector2, 미터. x = 동쪽, y = 북쪽. 침로: 도, 북쪽 0, 시계 방향 증가.
##
## 사용법:
##   var ai = EnemyAI.new(sim, commander, 1)
##   ai.update(1.0)   # commander.update(dt) 전에 호출

const Navigation = preload("res://scripts/combat/navigation.gd")

# --- 밸런스 상수 (조정 대상) -------------------------------------------------

const AI_THINK_SEC := 5.0          # s, 판단 주기
const PREFERRED_RANGE_FRAC := 0.55 # 무차원, 선호 거리 = 주포 최대 사거리 × 이 값
const RANGE_BAND_M := 1500.0       # m, 선호 거리 허용 폭
const TARGET_SWITCH_M := 2000.0    # m, 목표 교체 최소 거리 차(사격 보정 유지)
const CROSS_T_DEG := 30.0          # °, 적이 나를 향해 오는 것으로 보는 각
const TURN_STEP_DEG := 30.0        # °, 거리 유지 기동 꺾는 각
const RETREAT_HP_FRAC := 0.3       # 무차원, 이 비율 미만이면 퇴각
const SEARCH_SPEED_FRAC := 0.75    # 무차원, 수색 중 속도
const FULL_SPEED_FRAC := 1.0       # 무차원, 교전·퇴각 속도
const EDGE_MARGIN_M := 3000.0      # m, 맵 경계에서 이만큼 안쪽에 들어오면 안쪽으로 꺾는다

const STATE_SEARCH := "search"
const STATE_APPROACH := "approach"
const STATE_ENGAGE := "engage"
const STATE_RETREAT := "retreat"


# --- 필드 -------------------------------------------------------------------

var sim: Variant = null
var commander: Variant = null
var side: int = 0
## ship id → 상태 문자열
var states: Dictionary = {}
## ship id → 선호 거리(m)
var preferred_range: Dictionary = {}
## ship id → 현재 목표 id
var targets: Dictionary = {}
## ship id → 다음 판단 시각(s)
var next_think: Dictionary = {}


func _init(sim_ref: Variant, commander_ref: Variant, side_id: int) -> void:
	sim = sim_ref
	commander = commander_ref
	side = side_id


## 함선의 현재 AI 상태. 없으면 "search".
func state_of(ship_id: String) -> String:
	return String(states.get(ship_id, STATE_SEARCH))


## 함선의 선호 거리(m). 주포가 없으면 부포 기준.
func preferred_range_of(ship_id: String) -> float:
	if preferred_range.has(ship_id):
		return float(preferred_range[ship_id])
	var ship: Variant = sim.get_ship(ship_id)
	var r := _compute_preferred_range(ship)
	preferred_range[ship_id] = r
	return r


# --- 시간 단계 ---------------------------------------------------------------

## 매 단계 commander.update 전에 호출.
func update(dt: float) -> void:
	for s in sim.ships:
		if int(s.side) != side or not s.is_active():
			continue
		var id := String(s.id)
		if not states.has(id):
			states[id] = STATE_SEARCH
			next_think[id] = 0.0
		if sim.time + 0.0001 >= float(next_think[id]):
			next_think[id] = sim.time + AI_THINK_SEC
			_think(s)


func _think(s: Variant) -> void:
	var id := String(s.id)
	var state := state_of(id)

	# 퇴각 판정이 가장 우선한다.
	if float(s.hp) / maxf(float(s.max_hp), 1.0) < RETREAT_HP_FRAC:
		states[id] = STATE_RETREAT
		_do_retreat(s)
		return

	var enemy: Variant = _nearest_enemy(s)
	if enemy == null:
		states[id] = STATE_SEARCH
		_do_search(s)
		return

	var dist: float = s.pos.distance_to(enemy.pos)
	var pref: float = preferred_range_of(id)

	if state == STATE_RETREAT:
		# 피해가 회복되지 않으므로 계속 퇴각한다.
		_do_retreat(s)
		return

	if state == STATE_ENGAGE:
		_do_engage(s, enemy, dist, pref)
		return

	if dist <= pref:
		states[id] = STATE_ENGAGE
		_do_engage(s, enemy, dist, pref)
		return

	states[id] = STATE_APPROACH
	_do_approach(s, enemy)


func _do_search(s: Variant) -> void:
	commander.order_heading(String(s.id), _edge_heading(s, float(s.heading_deg)), SEARCH_SPEED_FRAC)


func _do_approach(s: Variant, enemy: Variant) -> void:
	commander.order_target(String(s.id), String(enemy.id))
	targets[String(s.id)] = String(enemy.id)
	var heading: float = Navigation.steer_to_point(s, enemy.pos)
	commander.order_heading(String(s.id), _edge_heading(s, heading), FULL_SPEED_FRAC)


func _do_engage(s: Variant, enemy: Variant, dist: float, pref: float) -> void:
	var id := String(s.id)
	var target: Variant = _pick_target(s, enemy)
	commander.order_target(id, String(target.id))
	targets[id] = String(target.id)
	dist = s.pos.distance_to(target.pos)

	var bearing: float = Navigation.steer_to_point(s, target.pos)
	var heading: float = float(s.heading_deg)

	# 기본 침로: 적 방위 ±90° 중 현재 침로에 가까운 쪽(현측을 보인다).
	var base_a: float = wrapf(bearing + 90.0, 0.0, 360.0)
	var base_b: float = wrapf(bearing - 90.0, 0.0, 360.0)
	var base: float = base_a
	if absf(Navigation.angle_diff_deg(heading, base_b)) < absf(Navigation.angle_diff_deg(heading, base_a)):
		base = base_b

	# T자 가로지르기: 적이 나를 향해 오면 적 침로에 수직으로 가로지른다.
	var enemy_to_me: float = Navigation.bearing_deg(target.pos, s.pos)
	if absf(Navigation.angle_diff_deg(float(target.heading_deg), enemy_to_me)) <= CROSS_T_DEG:
		var cross_a: float = wrapf(float(target.heading_deg) + 90.0, 0.0, 360.0)
		var cross_b: float = wrapf(float(target.heading_deg) - 90.0, 0.0, 360.0)
		base = cross_a
		if absf(Navigation.angle_diff_deg(heading, cross_b)) < absf(Navigation.angle_diff_deg(heading, cross_a)):
			base = cross_b

	# 거리 유지: 너무 멀면 적 쪽으로, 너무 가까우면 반대쪽으로 꺾는다.
	var order_heading := base
	if dist > pref + RANGE_BAND_M:
		order_heading = wrapf(base + TURN_STEP_DEG * _sign(Navigation.angle_diff_deg(base, bearing)), 0.0, 360.0)
	elif dist < pref - RANGE_BAND_M:
		order_heading = wrapf(base - TURN_STEP_DEG * _sign(Navigation.angle_diff_deg(base, bearing)), 0.0, 360.0)

	commander.order_heading(id, _edge_heading(s, order_heading), FULL_SPEED_FRAC)


func _do_retreat(s: Variant) -> void:
	var id := String(s.id)
	var enemy: Variant = _nearest_enemy(s)
	if enemy == null:
		commander.order_heading(id, float(s.heading_deg), FULL_SPEED_FRAC)
		return
	# 사격은 계속한다(목표 유지).
	if not targets.has(id) or sim.get_ship(String(targets[id])) == null or not sim.get_ship(String(targets[id])).is_active():
		commander.order_target(id, String(enemy.id))
		targets[id] = String(enemy.id)
	var away: float = Navigation.bearing_deg(enemy.pos, s.pos)
	# 퇴각은 경계 처리를 하지 않는다: 맵을 벗어나면 이탈이 된다.
	commander.order_heading(id, away, FULL_SPEED_FRAC)


# --- 내부 -------------------------------------------------------------------

## 맵 경계 처리: 경계 안쪽 EDGE_MARGIN_M 안에 들어오면 원래 침로와 맵 중심 방향의
## 가운데 침로로 꺾는다. 경계가 없거나(sim.map_half_m <= 0) 아직 안쪽이면 그대로.
func _edge_heading(s: Variant, heading: float) -> float:
	var half: float = float(sim.map_half_m)
	if half <= 0.0:
		return heading
	var pos: Vector2 = s.pos
	var inner: float = half - EDGE_MARGIN_M
	if absf(pos.x) <= inner and absf(pos.y) <= inner:
		return heading
	var to_center: float = Navigation.bearing_deg(pos, Vector2.ZERO)
	return wrapf(heading + Navigation.angle_diff_deg(heading, to_center) * 0.5, 0.0, 360.0)

## 가장 가까운 적. 기존 목표와 거리 차이가 TARGET_SWITCH_M 안이면 유지한다.
func _pick_target(s: Variant, nearest: Variant) -> Variant:
	var id := String(s.id)
	if not targets.has(id):
		return nearest
	var current: Variant = sim.get_ship(String(targets[id]))
	if current == null or not current.is_active():
		return nearest
	var d_cur: float = s.pos.distance_to(current.pos)
	var d_new: float = s.pos.distance_to(nearest.pos)
	if d_cur - d_new <= TARGET_SWITCH_M:
		return current
	return nearest


func _nearest_enemy(s: Variant) -> Variant:
	var best: Variant = null
	var best_d := INF
	for other in sim.ships:
		if int(other.side) == int(s.side) or not other.is_active():
			continue
		var d: float = s.pos.distance_to(other.pos)
		if d < best_d:
			best_d = d
			best = other
	return best


## 주포 최대 사거리 × PREFERRED_RANGE_FRAC. 주포가 없으면 부포 기준.
func _compute_preferred_range(ship: Variant) -> float:
	if ship == null:
		return 0.0
	var main_km := 0.0
	var sec_km := 0.0
	for g in ship.guns:
		var gtype := String(g.type)
		var gun: Dictionary = g.gun
		var r: float = float(gun.maxRangeKm)
		if gtype == "main":
			main_km = maxf(main_km, r)
		elif gtype == "secondary":
			sec_km = maxf(sec_km, r)
	var km: float = main_km if main_km > 0.0 else sec_km
	return km * 1000.0 * PREFERRED_RANGE_FRAC


func _sign(v: float) -> float:
	if v > 0.0:
		return 1.0
	if v < 0.0:
		return -1.0
	return 0.0
