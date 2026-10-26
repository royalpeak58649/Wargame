extends RefCounted
## 전투 시뮬레이션 코어: 함선 상태를 시간 단계로 진행시킨다 (순수 로직, 노드 없음).
##
## 탄도·관통·분산은 Ballistics 함수만 쓰고, 함선 성능은 ShipState(=ShipCalc)에서 가져온다.
## 난수는 이 객체가 가진 rng 하나만 쓴다. 같은 seed + 같은 명령이면 결과가 완전히 같다.
##
## 좌표: Vector2, 미터. x = 동쪽, y = 북쪽. 침로: 도, 북쪽 0, 시계 방향 증가.
## 시간: 전장 초(battle seconds). 속도: 내부 m/s.
##
## 사용법:
##   var sim = BattleSim.new(db, 1234)
##   sim.add_ship(ShipState.create("qe1", 0, qe_design, db, Vector2.ZERO, 0.0))
##   sim.order_target("qe1", "bb1")
##   sim.order_speed("qe1", 1.0)
##   sim.step(1.0)

const Ballistics = preload("res://scripts/combat/ballistics.gd")
const ShipState = preload("res://scripts/combat/ship_state.gd")

# --- 밸런스 상수 (조정 대상) -------------------------------------------------

const PEN_DAMAGE_PER_KG := 0.6      # HP per kg, 관통탄 1kg 당 피해
const NONPEN_FRAC := 0.1            # 무차원, 불관통탄 피해 = 관통 피해 × 이 값
const MAGAZINE_CHANCE := 0.005      # 무차원, 포탑 관통 시 탄약고 폭발 확률
const MACHINERY_CHANCE := 0.25      # 무차원, 중앙부 관통 시 기관 피해 확률
const FLOOD_CHANCE := 0.3           # 무차원, 수선부 관통 시 침수 확률
const RUDDER_CHANCE := 0.1          # 무차원, 선미 관통 시 조타 장치 고장 확률
const RUDDER_JAM_SEC := 60.0        # s, 조타 장치 고장 지속
const FIRE_CHANCE_PEN := 0.08       # 무차원, 관통탄 화재 확률
const FIRE_CHANCE_NONPEN := 0.04    # 무차원, 불관통탄 화재 확률
const MAX_FIRES := 4                # 개, 동시 화재 최대 수
const FIRE_DURATION := 90.0         # s, 화재 지속(±30)
const FIRE_DURATION_JITTER := 30.0  # s
const FLOOD_DURATION := 150.0       # s, 침수 지속(±50)
const FLOOD_DURATION_JITTER := 50.0 # s
const FIRE_DPS_FRAC := 0.0003       # 무차원, 화재 1개당 초당 max_hp 비율 피해
const FLOOD_DPS_FRAC := 0.0005      # 무차원, 침수 1개당 초당 max_hp 비율 피해
const FLOOD_LIST_SPREAD := 0.05     # 무차원, 활성 침수 1개당 분산 증가
const LATERAL_SPREAD_FRAC := 0.3    # 무차원, 횡방향 분산 = 사거리 방향 분산 × 이 값
const TURRET_HALF_WIDTH := 0.08     # 무차원, 포탑 구역 반폭(전후 비율 u 기준)
const BOW_STERN_FRAC := 0.6         # 무차원, |u| 가 이보다 크면 선수/선미
const MACHINERY_SPEED_MULT := 0.85  # 무차원, 기관 피해 1회당 속도 배수
const BOW_STERN_DECK_FRAC := 0.5    # 무차원, 선수/선미 갑판 장갑 = deck × 이 값
const TIME_DRAW_MARGIN := 0.05      # 무차원, 시간 판정 점수 차가 이보다 작으면 무승부

const ZONE_TURRET := "turret"
const ZONE_MIDDLE := "middle"
const ZONE_BOW := "bow"
const ZONE_STERN := "stern"

# --- 필드 -------------------------------------------------------------------

var db: Dictionary = {}
var time: float = 0.0
var ships: Array = []
var ships_by_id: Dictionary = {}
var shells: Array = []
var log: Array[Dictionary] = []
var rng := RandomNumberGenerator.new()
## 맵 반폭(m). |x| 또는 |y| 가 이 값을 넘은 함선은 전장을 이탈한다. 0 이면 경계 없음.
var map_half_m: float = 0.0
## 전장 시간 제한(s). 이 시각이 되면 남은 전력으로 판정한다. 0 이면 제한 없음.
var time_limit_s: float = 0.0


func _init(db: Dictionary, seed: int) -> void:
	self.db = db
	rng.seed = seed


# --- 함선 관리 ---------------------------------------------------------------

func add_ship(ship: Variant) -> void:
	if ship == null:
		return
	ships.append(ship)
	ships_by_id[ship.id] = ship


func get_ship(id: String) -> Variant:
	return ships_by_id.get(id, null)


func side_alive(side: int) -> int:
	var n := 0
	for s in ships:
		if int(s.side) == side and s.is_active():
			n += 1
	return n


## 전투 판정. {"winner": int, "reason": String}
## winner: 0/1 = 그 진영 승리, 2 = 무승부, -1 = 진행 중.
## reason: "" | "annihilation"(격멸) | "withdrawal"(퇴각) | "mutual"(상호 전멸) | "time"(시간 판정)
func result() -> Dictionary:
	var alive0 := side_alive(0)
	var alive1 := side_alive(1)
	if alive0 == 0 and alive1 == 0:
		return {"winner": 2, "reason": "mutual"}
	if alive0 == 0:
		return {"winner": 1, "reason": _defeat_reason(0)}
	if alive1 == 0:
		return {"winner": 0, "reason": _defeat_reason(1)}
	if time_limit_s > 0.0 and time >= time_limit_s:
		return _time_result()
	return {"winner": -1, "reason": ""}


## 한쪽 생존 수가 0이면 다른 쪽 side, 아니면 -1. 무승부는 2.
func winner() -> int:
	return int(result().winner)


## 패한 진영이 침몰한 배를 하나라도 남겼으면 격멸, 전부 이탈이면 퇴각.
func _defeat_reason(loser_side: int) -> String:
	for s in ships:
		if int(s.side) == loser_side and s.sunk:
			return "annihilation"
	return "withdrawal"


## 시간 제한 판정: 진영 점수 = 활성 함선 hp 합 / 그 진영 전체 함선 max_hp 합.
func _time_result() -> Dictionary:
	var score0 := _side_score(0)
	var score1 := _side_score(1)
	if absf(score0 - score1) < TIME_DRAW_MARGIN:
		return {"winner": 2, "reason": "time"}
	return {"winner": 0 if score0 > score1 else 1, "reason": "time"}


func _side_score(side: int) -> float:
	var hp_sum := 0.0
	var max_sum := 0.0
	for s in ships:
		if int(s.side) != side:
			continue
		max_sum += float(s.max_hp)
		if s.is_active():
			hp_sum += maxf(float(s.hp), 0.0)
	if max_sum <= 0.0:
		return 0.0
	return hp_sum / max_sum


# --- 명령 API (UI·AI 가 쓴다) ------------------------------------------------

func order_target(ship_id: String, target_id: String) -> void:
	var ship: Variant = get_ship(ship_id)
	if ship == null:
		return
	ship.set_target(target_id)


func order_speed(ship_id: String, frac: float) -> void:
	var ship: Variant = get_ship(ship_id)
	if ship == null:
		return
	ship.set_speed_order(frac)


func order_heading(ship_id: String, deg: float) -> void:
	var ship: Variant = get_ship(ship_id)
	if ship == null:
		return
	ship.set_heading_order(deg)


# --- 시간 단계 ---------------------------------------------------------------

func step(dt: float) -> void:
	_step_movement(dt)
	_step_fire(dt)
	_step_shells(dt)
	_step_attrition(dt)
	_step_sinking()
	time += dt


func _step_movement(dt: float) -> void:
	var big_movers := {}
	for s in ships:
		if not s.is_active():
			continue
		if s.step_movement(time, dt):
			big_movers[s.id] = true
		_check_withdrawal(s)
	# 어느 쪽이든 크게 기동하면 사격 보정(n)이 0으로 돌아간다.
	for s in ships:
		if not s.is_active():
			continue
		if big_movers.has(s.id) or big_movers.has(s.target_id):
			s.shots_on_target = 0


## 맵 밖으로 나간 함선은 전장을 이탈한다(침몰이 아니다).
func _check_withdrawal(s: Variant) -> void:
	if map_half_m <= 0.0:
		return
	if absf(s.pos.x) <= map_half_m and absf(s.pos.y) <= map_half_m:
		return
	s.withdrawn = true
	s.speed_ms = 0.0
	_log_event("withdrawn", String(s.id))


func _step_fire(dt: float) -> void:
	for s in ships:
		# hp 가 이미 0 이하인 배는 이번 단계에 침몰 처리되므로 쏘지 않는다.
		if not s.is_active() or float(s.hp) <= 0.0:
			continue
		var target: Variant = get_ship(s.target_id)
		if target == null or not target.is_active():
			continue
		for g in s.guns:
			if bool(g.disabled):
				continue
			var gtype := String(g.type)
			if gtype != "main" and gtype != "secondary":
				continue
			g.reload_left = float(g.reload_left) - dt
			if float(g.reload_left) > 0.0:
				continue
			var gun: Dictionary = g.gun
			var range_km: float = s.pos.distance_to(target.pos) / 1000.0
			if range_km > float(gun.maxRangeKm):
				continue
			_fire_salvo(s, target, g, range_km)
			g.reload_left = float(gun.reloadSec)
			if gtype == "main":
				s.shots_on_target += 1


func _fire_salvo(s: Variant, target: Variant, g: Dictionary, range_km: float) -> void:
	var gun: Dictionary = g.gun
	var flight: float = Ballistics.flight_time_sec(gun, range_km)
	var aim: Vector2 = target.pos + target.forward() * target.speed_ms * flight
	var to_target: Vector2 = target.pos - s.pos
	var dir: Vector2 = to_target.normalized() if to_target.length() > 0.001 else s.forward()
	var lateral := Vector2(-dir.y, dir.x)

	var sigma: float = Ballistics.salvo_spread_m(s.fc, range_km, s.shots_on_target, s.accuracy_mult)
	sigma *= 1.0 + FLOOD_LIST_SPREAD * float(target.active_floods(time))

	for i in int(g.barrels):
		var along: float = rng.randfn(0.0, sigma)
		var across: float = rng.randfn(0.0, sigma * LATERAL_SPREAD_FRAC)
		shells.append({
			"shooter_id": s.id,
			"target_id": target.id,
			"gun_key": String(g.slot),
			"gun": gun,
			"impact_pos": aim + dir * along + lateral * across,
			"impact_time": time + flight,
			"range_km": range_km,
			# 렌더링용(판정에 쓰지 않는다): 사격자 위치와 발사 시각
			"launch_pos": s.pos,
			"launch_time": time,
		})


func _step_shells(dt: float) -> void:
	var remaining: Array = []
	for shell in shells:
		if float(shell.impact_time) > time + dt:
			remaining.append(shell)
			continue
		_resolve_shell(shell)
	shells = remaining


func _resolve_shell(shell: Dictionary) -> void:
	var target: Variant = get_ship(String(shell.target_id))
	if target == null or not target.is_active():
		return  # 이미 침몰했거나 이탈한 목표는 버린다(로그 없음)
	var shooter: Variant = get_ship(String(shell.shooter_id))
	var gun: Dictionary = shell.gun
	var range_km: float = float(shell.range_km)
	var impact: Vector2 = shell.impact_pos

	# 국소 좌표: 전후 축 = 목표 forward, 좌우 축 = 목표 현측
	var rel: Vector2 = impact - target.pos
	var fwd: Vector2 = target.forward()
	var lat: Vector2 = target.starboard()
	var along: float = rel.dot(fwd)
	var across: float = rel.dot(lat)
	var half_l: float = target.length_m() / 2.0
	var half_b: float = target.beam_m() / 2.0

	var entry := {
		"t": time,
		"kind": "hit",
		"shooter": String(shell.shooter_id),
		"target": String(shell.target_id),
		"gun": String(shell.gun_key),
		"rangeKm": range_km,
		"zone": "",
		"plate": "",
		"result": "miss",
		"penMm": 0.0,
		"effArmorMm": 0.0,
		"damage": 0.0,
		"effects": [],
		# 렌더링용(판정에 쓰지 않는다): 탄착점 좌표
		"x": impact.x,
		"y": impact.y,
	}

	if absf(along) > half_l or absf(across) > half_b:
		log.append(entry)
		return

	var u: float = along / half_l
	var zone := _zone_at(target, u)
	var fall_deg: float = Ballistics.fall_angle_deg(gun, range_km)
	var plate: String = Ballistics.PLATE_DECK if rng.randf() < Ballistics.deck_hit_chance(fall_deg) else Ballistics.PLATE_BELT
	var thickness: float = _plate_thickness(target, zone, plate)
	var horizontal_deg: float = _horizontal_deg(shooter, target)
	var hit: Dictionary = Ballistics.resolve_hit(gun, range_km, plate, thickness, horizontal_deg)

	entry.zone = zone
	entry.plate = plate
	entry.result = String(hit.result)
	entry.penMm = float(hit.penMm)
	entry.effArmorMm = float(hit.effArmorMm)
	log.append(entry)

	_apply_damage(target, gun, zone, plate, entry)


## 전후 비율 u(−1 선미 ~ +1 선수)에서의 피탄 구역.
func _zone_at(target: Variant, u: float) -> String:
	for g in target.guns:
		if String(g.type) != "main" or bool(g.disabled):
			continue
		if absf(u - float(g.slot_x) * 2.0) <= TURRET_HALF_WIDTH:
			return ZONE_TURRET
	if u > BOW_STERN_FRAC:
		return ZONE_BOW
	if u < -BOW_STERN_FRAC:
		return ZONE_STERN
	return ZONE_MIDDLE


func _plate_thickness(target: Variant, zone: String, plate: String) -> float:
	var is_deck := plate == Ballistics.PLATE_DECK
	match zone:
		ZONE_TURRET:
			return target.armor_mm("barbette" if is_deck else "turretFace")
		ZONE_MIDDLE:
			return target.armor_mm("deck" if is_deck else "beltMain")
		_:
			# 선수/선미: 끝단은 얇다
			if is_deck:
				return target.armor_mm("deck") * BOW_STERN_DECK_FRAC
			return target.armor_mm("beltUpper")


## 탄도 방향(사격자→목표)과 목표 현측 법선 사이 각도(0~90°).
func _horizontal_deg(shooter: Variant, target: Variant) -> float:
	if shooter == null:
		return 0.0
	var dir: Vector2 = target.pos - shooter.pos
	if dir.length() < 0.001:
		return 0.0
	dir = dir.normalized()
	var normal: Vector2 = target.starboard()
	var c: float = absf(dir.dot(normal))
	return rad_to_deg(acos(clampf(c, 0.0, 1.0)))


func _apply_damage(target: Variant, gun: Dictionary, zone: String, plate: String, entry: Dictionary) -> void:
	var result := String(entry.result)
	if result == "miss" or result == "ricochet":
		return
	var mass: float = float(gun.shellMassKg)
	var damage := 0.0
	if result == "pen":
		damage = mass * PEN_DAMAGE_PER_KG
	else:
		damage = mass * PEN_DAMAGE_PER_KG * NONPEN_FRAC
	target.hp -= damage
	entry.damage = damage

	var effects: Array = entry.effects
	var is_belt := plate == Ballistics.PLATE_BELT

	if result == "pen":
		match zone:
			ZONE_TURRET:
				_disable_turret(target, entry)
				if rng.randf() < MAGAZINE_CHANCE:
					target.hp = 0.0
					effects.append("magazine")
			ZONE_MIDDLE:
				if rng.randf() < MACHINERY_CHANCE:
					target.speed_mult = maxf(target.speed_mult * MACHINERY_SPEED_MULT, ShipState.MIN_SPEED_MULT)
					effects.append("machinery")
				if is_belt and rng.randf() < FLOOD_CHANCE:
					_add_flood(target)
					effects.append("flood")
			ZONE_STERN:
				if rng.randf() < RUDDER_CHANCE:
					target.rudder_jam_until = time + RUDDER_JAM_SEC
					effects.append("rudder")
			ZONE_BOW:
				if is_belt and rng.randf() < FLOOD_CHANCE:
					_add_flood(target)
					effects.append("flood")

	var fire_chance := FIRE_CHANCE_PEN if result == "pen" else FIRE_CHANCE_NONPEN
	if rng.randf() < fire_chance and target.fires.size() < MAX_FIRES:
		_add_fire(target)
		effects.append("fire")


## 포탑 관통: 그 슬롯 무장을 비활성화한다.
func _disable_turret(target: Variant, entry: Dictionary) -> void:
	var gun_key := String(entry.gun)
	for g in target.guns:
		if String(g.slot) == gun_key:
			g.disabled = true
			entry.effects.append("turret_disabled")
			return


func _add_fire(target: Variant) -> void:
	var dur: float = FIRE_DURATION + rng.randf_range(-FIRE_DURATION_JITTER, FIRE_DURATION_JITTER)
	target.fires.append(time + dur)


func _add_flood(target: Variant) -> void:
	var dur: float = FLOOD_DURATION + rng.randf_range(-FLOOD_DURATION_JITTER, FLOOD_DURATION_JITTER)
	target.floods.append(time + dur)


func _step_attrition(dt: float) -> void:
	for s in ships:
		if not s.is_active():
			continue
		var fires: Array[float] = []
		for end_t in s.fires:
			if end_t > time:
				fires.append(end_t)
			else:
				_log_event("fire_end", s.id)
		s.fires = fires
		var floods: Array[float] = []
		for end_t in s.floods:
			if end_t > time:
				floods.append(end_t)
			else:
				_log_event("flood_end", s.id)
		s.floods = floods

		var dmg: float = float(s.fires.size()) * FIRE_DPS_FRAC * s.max_hp * dt
		dmg += float(s.floods.size()) * FLOOD_DPS_FRAC * s.max_hp * dt
		s.hp -= dmg


func _step_sinking() -> void:
	for s in ships:
		if not s.is_active() or s.hp > 0.0:
			continue
		s.sunk = true
		s.speed_ms = 0.0
		s.hp = 0.0
		var cause := "magazine" if _has_magazine_effect(s.id) else "damage"
		_log_event("sunk", s.id, cause)


func _has_magazine_effect(ship_id: String) -> bool:
	for i in range(log.size() - 1, -1, -1):
		var e: Dictionary = log[i]
		if String(e.target) != ship_id:
			continue
		if String(e.kind) != "hit":
			continue
		return (e.effects as Array).has("magazine")
	return false


func _log_event(kind: String, ship_id: String, cause: String = "") -> void:
	var entry := {
		"t": time,
		"kind": kind,
		"shooter": "",
		"target": ship_id,
		"gun": "",
		"rangeKm": 0.0,
		"zone": "",
		"plate": "",
		"result": cause,
		"penMm": 0.0,
		"effArmorMm": 0.0,
		"damage": 0.0,
		"effects": [],
	}
	log.append(entry)


# --- 요약 -------------------------------------------------------------------

## 함선별 전투 요약.
## {fired, hits, pen, nonpen, ricochet, miss, damageDealt, damageTaken, sunk, withdrawn}
func summary() -> Dictionary:
	var out := {}
	for s in ships:
		out[s.id] = {
			"fired": 0, "hits": 0, "pen": 0, "nonpen": 0, "ricochet": 0, "miss": 0,
			"damageDealt": 0.0, "damageTaken": 0.0, "sunk": bool(s.sunk),
			"withdrawn": bool(s.withdrawn),
		}
	for e in log:
		if String(e.kind) != "hit":
			continue
		var shooter := String(e.shooter)
		var target := String(e.target)
		var result := String(e.result)
		if out.has(shooter):
			var so: Dictionary = out[shooter]
			so.fired = int(so.fired) + 1
			if result == "miss":
				so.miss = int(so.miss) + 1
			else:
				so.hits = int(so.hits) + 1
				if result == "pen":
					so.pen = int(so.pen) + 1
				elif result == "nonpen":
					so.nonpen = int(so.nonpen) + 1
				elif result == "ricochet":
					so.ricochet = int(so.ricochet) + 1
			so.damageDealt = float(so.damageDealt) + float(e.damage)
		if out.has(target):
			var to: Dictionary = out[target]
			to.damageTaken = float(to.damageTaken) + float(e.damage)
	return out
