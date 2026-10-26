extends RefCounted
## 전투 중인 함선 하나의 상태 (순수 로직, 노드 없음).
##
## 성능 수치는 반드시 ShipCalc.compute_all 결과에서 가져온다(공식을 다시 만들지 않는다).
## 좌표: Vector2, 미터. x = 동쪽, y = 북쪽(수학 좌표).
## 침로(heading): 도, 북쪽 0, 시계 방향 증가. 방향 벡터 = Vector2(sin h, cos h).
## 속도: 내부 m/s, 외부 표시 kn. 시간: 전장 초(battle seconds).
##
## 사용법:
##   var ship = ShipState.create("qe1", 0, design, db, Vector2.ZERO, 0.0)

const ShipCalc = preload("res://scripts/core/ship_calc.gd")

# --- 밸런스 상수 (조정 대상) -------------------------------------------------

const KNOT_TO_MS := 0.514444      # m/s per kn, 노트 → m/s
const BIG_TURN_DEG_PER_SEC := 1.0 # °/s, 이보다 빠른 선회는 "큰 기동"(사격 보정 초기화)
const BIG_SPEED_DELTA_MS2 := 0.3  # m/s², 이보다 큰 속도 변화는 "큰 기동"
const MIN_SPEED_MULT := 0.3       # 무차원, 기관 피해로 내려갈 수 있는 속도 배수 하한

# --- 필드 -------------------------------------------------------------------

var id: String = ""
var side: int = 0
var design: Dictionary = {}
var pos := Vector2.ZERO
var heading_deg: float = 0.0
var speed_ms: float = 0.0

# 명령
var order_speed_frac: float = 0.0
var order_heading_deg: float = 0.0

# 성능 (compute_all 결과)
var max_speed_ms: float = 0.0
var turn_radius_m: float = 1.0
var accel_ms2: float = 0.0
var accuracy_mult: float = 1.0

# 내구
var max_hp: float = 0.0
var hp: float = 0.0
var sunk: bool = false
var speed_mult: float = 1.0
var rudder_jam_until: float = 0.0
var fires: Array[float] = []
var floods: Array[float] = []

# 무장: 슬롯마다 하나
var guns: Array[Dictionary] = []

# 사격 통제
var fc: Dictionary = {}
var target_id: String = ""
var shots_on_target: int = 0

# 선체 치수 (design.hull)
var _length_m: float = 0.0
var _beam_m: float = 0.0
var _armor: Dictionary = {}


## 설계로부터 함선 상태를 만든다. 설계가 유효하지 않으면 null.
static func create(id: String, side: int, design: Dictionary, db: Dictionary,
		pos: Vector2, heading_deg: float) -> RefCounted:
	var calc := ShipCalc.compute_all(design, db)
	if not bool(calc.get("valid", false)):
		push_error("함선 생성 실패: 설계가 유효하지 않음 (%s)" % id)
		return null

	var ship := new()
	ship.id = id
	ship.side = side
	ship.design = design
	ship.pos = pos
	ship.heading_deg = wrapf(heading_deg, 0.0, 360.0)
	ship.order_heading_deg = ship.heading_deg
	ship.speed_ms = 0.0
	ship.order_speed_frac = 0.0

	var perf: Dictionary = calc.performance
	ship.max_speed_ms = float(perf.maxSpeedKn) * KNOT_TO_MS
	ship.turn_radius_m = maxf(float(perf.tacticalDiameterM) / 2.0, 1.0)
	var t_full: float = float(perf.timeToFullSpeedSec)
	ship.accel_ms2 = ship.max_speed_ms / t_full if t_full > 0.0 else 0.0
	ship.accuracy_mult = float(calc.combat.accuracyMult)

	var weights: Dictionary = calc.weights
	ship.max_hp = float(weights.groups.structure)
	ship.hp = ship.max_hp

	var dims := ShipCalc.hull_dimensions(design, db)
	ship._length_m = float(dims.length)
	ship._beam_m = float(dims.beam)
	ship._armor = design.get("armor", {}).duplicate()

	ship.fc = db.fireControl[design.fireControl]
	ship._build_guns(design, db)
	return ship


func _build_guns(design: Dictionary, db: Dictionary) -> void:
	var hull: Dictionary = db.hulls[design.hull.templateId]
	var slot_x := {}
	for s in hull.slots:
		slot_x[String(s.id)] = float(s.x)
	for wpn in design.get("weapons", []):
		var slot := String(wpn.get("slot", ""))
		var part_id := String(wpn.get("partId", ""))
		var gun: Dictionary = db.guns[part_id]
		guns.append({
			"slot": slot,
			"partId": part_id,
			"gun": gun,
			"barrels": int(wpn.get("barrels", 0)),
			"type": String(gun.type),
			"slot_x": float(slot_x.get(slot, 0.0)),
			"reload_left": 0.0,
			"disabled": false,
		})


# --- 명령 -------------------------------------------------------------------

func set_target(id: String) -> void:
	if id != target_id:
		target_id = id
		shots_on_target = 0


func set_speed_order(frac: float) -> void:
	order_speed_frac = clampf(frac, 0.0, 1.0)


func set_heading_order(deg: float) -> void:
	order_heading_deg = wrapf(deg, 0.0, 360.0)


# --- 이동 -------------------------------------------------------------------

## 한 단계 이동한다. 큰 기동(급선회/급가속)을 했으면 true.
func step_movement(t: float, dt: float) -> bool:
	if sunk:
		speed_ms = 0.0
		return false

	# 속도: 목표 속도로 accel_ms2 만큼 접근
	var target_speed: float = max_speed_ms * order_speed_frac * speed_mult
	var speed_before := speed_ms
	var dv: float = accel_ms2 * dt
	if speed_ms < target_speed:
		speed_ms = minf(speed_ms + dv, target_speed)
	else:
		speed_ms = maxf(speed_ms - dv, target_speed)

	# 선회: 선회 속도(°/s) = rad_to_deg(v / R), 짧은 방향으로
	var heading_before := heading_deg
	if rudder_jam_until <= t and turn_radius_m > 0.0:
		var turn_rate: float = rad_to_deg(speed_ms / turn_radius_m)
		var diff: float = wrapf(order_heading_deg - heading_deg, -180.0, 180.0)
		var max_turn: float = turn_rate * dt
		heading_deg = wrapf(heading_deg + clampf(diff, -max_turn, max_turn), 0.0, 360.0)

	pos += forward() * speed_ms * dt

	var turned: float = absf(wrapf(heading_deg - heading_before, -180.0, 180.0))
	var speed_delta: float = absf(speed_ms - speed_before)
	return turned > BIG_TURN_DEG_PER_SEC * dt or speed_delta > BIG_SPEED_DELTA_MS2 * dt


func forward() -> Vector2:
	var h := deg_to_rad(heading_deg)
	return Vector2(sin(h), cos(h))


## 현측(우현) 법선 벡터. 침로 +90°.
func starboard() -> Vector2:
	var h := deg_to_rad(heading_deg)
	return Vector2(cos(h), -sin(h))


func length_m() -> float:
	return _length_m


func beam_m() -> float:
	return _beam_m


func armor_mm(key: String) -> float:
	return float(_armor.get(key, 0.0))


## 활성 화재 수 (t 시점).
func active_fires(t: float) -> int:
	var n := 0
	for end_t in fires:
		if end_t > t:
			n += 1
	return n


## 활성 침수 수 (t 시점).
func active_floods(t: float) -> int:
	var n := 0
	for end_t in floods:
		if end_t > t:
			n += 1
	return n
