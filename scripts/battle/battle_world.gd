extends Node2D
## 전투 화면의 지도·함선·포탄·물기둥을 그린다 (그리기만 한다, 판정 없음).
##
## 좌표: 화면 좌표 = Vector2(x, −y). 전장 좌표는 미터, x = 동쪽, y = 북쪽.
## 이 노드는 전장 좌표를 그대로 쓰고, y 뒤집기는 _to_screen() 에서 한다.
## 카메라(Camera2D)가 확대·이동을 맡는다.
##
## 사용법:
##   world.setup(sim, commanders, ais, scenario_name)
##   world.selected_id = "a1"
##   world.selected_ids = ["a1", "a2"]
##   world.queue_redraw()

const ShipState = preload("res://scripts/combat/ship_state.gd")
const FleetCommander = preload("res://scripts/combat/fleet_commander.gd")
const EnemyAI = preload("res://scripts/combat/enemy_ai.gd")

# --- 표시 상수 ---------------------------------------------------------------

const MAP_HALF_M := 22500.0        # m, 맵 반폭(45km × 45km, 원점 중심)
const GRID_M := 5000.0             # m, 격자 간격
const SEA_COLOR := Color(0.05, 0.09, 0.18)
const GRID_COLOR := Color(0.20, 0.30, 0.45, 0.45)
const GRID_AXIS_COLOR := Color(0.35, 0.50, 0.70, 0.70)
const GRID_LABEL_COLOR := Color(0.55, 0.70, 0.85, 0.85)
const SIDE_COLORS := [Color("#5aa0ff"), Color("#ff6a5a")]
const SIDE_LABELS := ["아군", "적군"]
const SUNK_COLOR := Color(0.55, 0.55, 0.55, 0.45)
const SELECT_COLOR := Color(1.0, 1.0, 1.0, 0.9)
const RANGE_COLOR := Color(1.0, 1.0, 1.0, 0.18)
const TARGET_LINE_COLOR := Color(1.0, 1.0, 1.0, 0.35)
const SHELL_COLOR := Color(1.0, 0.92, 0.60, 0.95)
const FIRE_DOT_COLOR := Color(1.0, 0.55, 0.15)
const FLOOD_DOT_COLOR := Color(0.45, 0.80, 1.0)
const HP_BACK_COLOR := Color(0.10, 0.10, 0.10, 0.75)
const HP_FRONT_COLOR := Color(0.45, 0.90, 0.45)
const HP_LOW_COLOR := Color(0.95, 0.40, 0.35)
const LABEL_COLOR := Color(0.92, 0.95, 1.0)
const WAYPOINT_COLOR := Color(0.45, 0.95, 0.55, 0.85)
const FAINT_TARGET_LINE_COLOR := Color(1.0, 1.0, 1.0, 0.12)
const FOLLOWER_MARK := "∙"
const DRAG_FILL_COLOR := Color(0.55, 0.80, 1.0, 0.12)
const DRAG_EDGE_COLOR := Color(0.55, 0.80, 1.0, 0.75)
const MOVE_MARK_COLOR := Color(0.45, 0.95, 0.55)
const TARGET_MARK_COLOR := Color(1.0, 0.40, 0.35)
const MARK_LIFE_MS := 600          # ms, 명령 표시가 남는 실제 시간

const MIN_SHIP_LENGTH_PX := 22.0   # px, 화면에서 함선 최소 길이
const MIN_SHIP_BEAM_PX := 4.0      # px, 화면에서 함선 최소 폭
const TAG_OFFSET_PX := 10.0        # px, 함선 중심에서 오른쪽으로 띄운 id 표시 거리
const SHELL_DOT_PX := 2.0          # px, 포탄 점 반지름
const SPLASH_LIFE_SEC := 20.0      # s, 물기둥이 남는 전장 시간
const SPLASH_MAX_RADIUS_PX := 9.0  # px, 물기둥 최대 반지름
const SCALE_BAR_M := 5000.0        # m, 축척 막대 길이
const SCALE_BAR_MARGIN := Vector2(24.0, 24.0)  # px, 화면 왼쪽 아래 여백
const LABEL_FONT_SIZE := 12

const SPLASH_COLORS := {
	"miss": Color(1.0, 1.0, 1.0, 0.9),
	"pen": Color(1.0, 0.55, 0.15, 0.9),
	"nonpen": Color(1.0, 0.90, 0.30, 0.9),
	"ricochet": Color(0.70, 0.70, 0.70, 0.9),
}

# --- 상태 -------------------------------------------------------------------

var sim: Variant = null
var commanders: Array = []
var ais: Array = []
var scenario_name := ""
## 정보 패널용으로 마지막에 고른 함선 id.
var selected_id := ""
## 플레이어가 선택한 아군 함선 id 목록(선택한 순서). 모두에게 흰 고리를 그린다.
var selected_ids: Array[String] = []
## 드래그 선택 중인 화면 사각형(크기 0 이면 그리지 않는다).
var drag_rect := Rect2()
## 명령 표시: {pos: Vector2(전장 좌표), kind: "move"|"target", until: int(실제 ms)}
var marks: Array[Dictionary] = []

## 물기둥 목록: {pos: Vector2, t: float(전장 시각), result: String}
var _splashes: Array[Dictionary] = []
## 마지막으로 처리한 sim.log 위치
var _log_cursor := 0

## 시점: 화면 가운데에 오는 전장 좌표(m)와 확대율(px/m).
## Camera2D 를 쓰지 않고 그리기 코드가 직접 변환한다. 그래서 px 로 정한 크기
## (선 두께, 함선 최소 길이, 글자)는 확대율과 무관하게 언제나 그 크기로 보인다.
var cam_center := Vector2.ZERO
var zoom := 1.0


func setup(sim_ref: Variant, commanders_ref: Array, ais_ref: Array, name: String) -> void:
	sim = sim_ref
	commanders = commanders_ref
	ais = ais_ref
	scenario_name = name
	selected_id = ""
	selected_ids.clear()
	_splashes.clear()
	_log_cursor = 0
	queue_redraw()


## 매 프레임 호출: 새 로그를 물기둥으로 바꾸고 다시 그린다.
func refresh() -> void:
	_collect_splashes()
	queue_redraw()


## 전장 좌표 → 화면 좌표(y 뒤집기).
static func to_screen(p: Vector2) -> Vector2:
	return Vector2(p.x, -p.y)


## 화면 좌표 → 전장 좌표.
static func to_world(p: Vector2) -> Vector2:
	return Vector2(p.x, -p.y)


## 전장 좌표(m) → 화면 픽셀 좌표.
func view(p: Vector2) -> Vector2:
	return (to_screen(p) - to_screen(cam_center)) * zoom + get_viewport_rect().size * 0.5


## 화면 픽셀 좌표 → 전장 좌표(m).
func screen_to_world(sp: Vector2) -> Vector2:
	return to_world((sp - get_viewport_rect().size * 0.5) / zoom + to_screen(cam_center))


func _collect_splashes() -> void:
	if sim == null:
		return
	var log: Array = sim.log
	if _log_cursor > log.size():
		_log_cursor = 0
		_splashes.clear()
	while _log_cursor < log.size():
		var e: Dictionary = log[_log_cursor]
		_log_cursor += 1
		if String(e.get("kind", "")) != "hit":
			continue
		if not e.has("x") or not e.has("y"):
			continue
		_splashes.append({
			"pos": Vector2(float(e.x), float(e.y)),
			"t": float(e.get("t", 0.0)),
			"result": String(e.get("result", "miss")),
		})
	# 오래된 물기둥은 버린다.
	var now: float = float(sim.time)
	var kept: Array[Dictionary] = []
	for s in _splashes:
		if now - float(s.t) <= SPLASH_LIFE_SEC:
			kept.append(s)
	_splashes = kept


# --- 그리기 -----------------------------------------------------------------

func _draw() -> void:
	_draw_sea()
	_draw_grid()
	if sim == null:
		return
	_draw_waypoints()
	_draw_target_lines()
	_draw_selection()
	_draw_ships()
	_draw_shells()
	_draw_splashes()
	_draw_marks()
	_draw_drag_rect()
	_draw_scale_bar()


func _draw_sea() -> void:
	var half := MAP_HALF_M
	draw_rect(Rect2(view(Vector2(-half, half)), Vector2(half * 2.0, half * 2.0) * zoom), SEA_COLOR, true)


func _draw_grid() -> void:
	var half := MAP_HALF_M
	var font := ThemeDB.fallback_font
	var n := int(half / GRID_M)
	for i in range(-n, n + 1):
		var v := float(i) * GRID_M
		var axis := i == 0
		var color: Color = GRID_AXIS_COLOR if axis else GRID_COLOR
		# 세로선(x = v)
		draw_line(view(Vector2(v, -half)), view(Vector2(v, half)), color, 1.0)
		# 가로선(y = v)
		draw_line(view(Vector2(-half, v)), view(Vector2(half, v)), color, 1.0)
		if i != 0:
			var km := int(v / 1000.0)
			# x 축 눈금(맵 아래쪽 가장자리)
			draw_string(font, view(Vector2(v, -half)) + Vector2(3.0, -4.0), "%d" % km,
				HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_FONT_SIZE, GRID_LABEL_COLOR)
			# y 축 눈금(맵 왼쪽 가장자리)
			draw_string(font, view(Vector2(-half, v)) + Vector2(3.0, -4.0), "%d" % km,
				HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_FONT_SIZE, GRID_LABEL_COLOR)


func _draw_selection() -> void:
	# 선택된 아군 함선 모두에게 흰 고리
	for id in selected_ids:
		var sel: Variant = sim.get_ship(id)
		if sel == null or not sel.is_active():
			continue
		var sc := view(sel.pos)
		var sel_r := maxf(_ship_length_px(sel) * 0.75, 14.0)
		draw_arc(sc, sel_r, 0.0, TAU, 32, SELECT_COLOR, 1.5, true)

	var ship: Variant = sim.get_ship(selected_id) if selected_id != "" else null
	if ship == null:
		return
	var c := view(ship.pos)
	var ring_r := maxf(_ship_length_px(ship) * 0.75, 14.0)
	draw_arc(c, ring_r, 0.0, TAU, 32, SELECT_COLOR, 1.5, true)

	# 주포 최대 사거리 원
	var range_m := _main_range_m(ship)
	if range_m > 0.0:
		draw_arc(c, range_m * zoom, 0.0, TAU, 96, RANGE_COLOR, 1.0, true)

	# 목표까지 선
	var target: Variant = sim.get_ship(String(ship.target_id)) if String(ship.target_id) != "" else null
	if target != null:
		draw_line(c, view(target.pos), TARGET_LINE_COLOR, 1.0)


## 명령을 내린 위치에 짧게 남기는 표시: 이동은 녹색 ×, 목표 지정은 빨간 고리.
func add_mark(pos: Vector2, kind: String) -> void:
	marks.append({"pos": pos, "kind": kind, "until": Time.get_ticks_msec() + MARK_LIFE_MS})


func _draw_marks() -> void:
	var now := Time.get_ticks_msec()
	var kept: Array[Dictionary] = []
	for m in marks:
		var left: int = int(m.until) - now
		if left <= 0:
			continue
		kept.append(m)
		var a: float = float(left) / float(MARK_LIFE_MS)
		var p: Vector2 = view(m.pos)
		if String(m.kind) == "target":
			var c := TARGET_MARK_COLOR
			c.a = a
			draw_arc(p, 16.0 - 6.0 * a, 0.0, TAU, 24, c, 2.0, true)
		else:
			var c := MOVE_MARK_COLOR
			c.a = a
			draw_line(p + Vector2(-6.0, -6.0), p + Vector2(6.0, 6.0), c, 2.0)
			draw_line(p + Vector2(-6.0, 6.0), p + Vector2(6.0, -6.0), c, 2.0)
	marks = kept


func _draw_drag_rect() -> void:
	if drag_rect.size == Vector2.ZERO:
		return
	var r := drag_rect.abs()
	draw_rect(r, DRAG_FILL_COLOR, true)
	draw_rect(r, DRAG_EDGE_COLOR, false, 1.0)


## 선택된 아군 함선의 경유점: 현재 위치 → 경유점들을 얇은 녹색 선과 작은 점으로.
func _draw_waypoints() -> void:
	if commanders.is_empty():
		return
	var cmd: Variant = commanders[0]
	for id in selected_ids:
		var ship: Variant = sim.get_ship(id)
		if ship == null or not ship.is_active():
			continue
		var order: Dictionary = cmd.order_of(id)
		if order.is_empty():
			continue
		var wps: Array = order.get("waypoints", [])
		if wps.is_empty():
			continue
		var prev: Vector2 = view(ship.pos)
		for w in wps:
			var p: Vector2 = view(w)
			draw_line(prev, p, WAYPOINT_COLOR, 1.0)
			draw_circle(p, 2.5, WAYPOINT_COLOR)
			prev = p


## 모든 아군 활성 함선의 목표까지 아주 옅은 선. 선택된 배는 _draw_selection 이 진하게 긋는다.
func _draw_target_lines() -> void:
	for s in sim.ships:
		if int(s.side) != 0 or not s.is_active():
			continue
		if selected_ids.has(String(s.id)):
			continue
		var target_id := String(s.target_id)
		if target_id == "":
			continue
		var target: Variant = sim.get_ship(target_id)
		if target == null or not target.is_active():
			continue
		draw_line(view(s.pos), view(target.pos), FAINT_TARGET_LINE_COLOR, 1.0)


func _draw_ships() -> void:
	var font := ThemeDB.fallback_font
	for s in sim.ships:
		if s.withdrawn:
			continue  # 전장을 이탈한 배는 그리지 않는다
		var c := view(s.pos)
		var color: Color = SUNK_COLOR if s.sunk else SIDE_COLORS[clampi(int(s.side), 0, 1)]
		var length_px := _ship_length_px(s)
		var beam_px: float = maxf(length_px * (s.beam_m() / maxf(s.length_m(), 0.001)), MIN_SHIP_BEAM_PX)
		# 침로: 북 0, 시계 방향. 화면은 y 가 뒤집혀 있으므로 각도도 뒤집는다.
		var angle := -deg_to_rad(float(s.heading_deg))
		draw_set_transform(c, angle, Vector2.ONE)
		draw_colored_polygon(_hull_points(length_px, beam_px), color)
		draw_set_transform(Vector2.ZERO, 0.0, Vector2.ONE)

		if s.sunk:
			continue

		# id 표시와 HP 막대: 함선 오른쪽에 한 줄로 둔다(종진 간격이 화면에서 좁아 위에 두면 겹친다).
		# 전체 이름은 선택했을 때 정보 패널에 나온다.
		var tag_pos := c + Vector2(TAG_OFFSET_PX, 4.0)
		draw_string(font, tag_pos, String(s.id), HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_FONT_SIZE, LABEL_COLOR)
		# 종진(follow) 상태인 배는 id 옆에 작은 점을 붙여 기함과 구분한다.
		if _is_follower(s):
			var id_w: float = font.get_string_size(String(s.id), HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_FONT_SIZE).x
			draw_string(font, tag_pos + Vector2(id_w + 2.0, 0.0), FOLLOWER_MARK,
				HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_FONT_SIZE, LABEL_COLOR)
		var bar_w := 20.0
		var bar_pos := tag_pos + Vector2(26.0, -5.0)
		var frac := clampf(float(s.hp) / maxf(float(s.max_hp), 1.0), 0.0, 1.0)
		draw_rect(Rect2(bar_pos, Vector2(bar_w, 3.0)), HP_BACK_COLOR, true)
		var hp_color := HP_FRONT_COLOR if frac > 0.35 else HP_LOW_COLOR
		draw_rect(Rect2(bar_pos, Vector2(bar_w * frac, 3.0)), hp_color, true)

		# 화재·침수 점: HP 막대 오른쪽
		var dot := bar_pos + Vector2(bar_w + 4.0, 1.5)
		var fires := int(s.active_fires(float(sim.time)))
		var floods := int(s.active_floods(float(sim.time)))
		for i in fires:
			draw_circle(dot + Vector2(float(i) * 5.0, 0.0), 2.0, FIRE_DOT_COLOR)
		for i in floods:
			draw_circle(dot + Vector2(float(fires + i) * 5.0, 0.0), 2.0, FLOOD_DOT_COLOR)


## 선수가 뾰족한 선체 다각형(로컬 좌표, 선수 = −y 방향).
func _hull_points(length_px: float, beam_px: float) -> PackedVector2Array:
	var hl := length_px * 0.5
	var hb := maxf(beam_px * 0.5, 1.5)
	return PackedVector2Array([
		Vector2(0.0, -hl),                 # 선수
		Vector2(hb, -hl * 0.55),
		Vector2(hb, hl * 0.75),
		Vector2(hb * 0.6, hl),             # 선미
		Vector2(-hb * 0.6, hl),
		Vector2(-hb, hl * 0.75),
		Vector2(-hb, -hl * 0.55),
	])


func _draw_shells() -> void:
	if sim == null:
		return
	var now: float = float(sim.time)
	for shell in sim.shells:
		if not shell.has("launch_pos") or not shell.has("launch_time"):
			continue
		var from: Vector2 = shell.launch_pos
		var to: Vector2 = shell.impact_pos
		var t0: float = float(shell.launch_time)
		var t1: float = float(shell.impact_time)
		var span: float = t1 - t0
		var frac: float = clampf((now - t0) / span, 0.0, 1.0) if span > 0.0001 else 1.0
		draw_circle(view(from.lerp(to, frac)), SHELL_DOT_PX, SHELL_COLOR)


func _draw_splashes() -> void:
	var now: float = float(sim.time)
	for s in _splashes:
		var age: float = now - float(s.t)
		if age < 0.0 or age > SPLASH_LIFE_SEC:
			continue
		var k: float = age / SPLASH_LIFE_SEC
		var color: Color = SPLASH_COLORS.get(String(s.result), SPLASH_COLORS["miss"])
		color.a *= 1.0 - k
		var r: float = lerpf(2.0, SPLASH_MAX_RADIUS_PX, k)
		draw_arc(view(s.pos), r, 0.0, TAU, 16, color, 1.5, true)


func _draw_scale_bar() -> void:
	var view_size := get_viewport_rect().size
	var bar_px := SCALE_BAR_M * zoom
	var origin := Vector2(SCALE_BAR_MARGIN.x, view_size.y - SCALE_BAR_MARGIN.y)
	var color := Color(1.0, 1.0, 1.0, 0.85)
	draw_line(origin, origin + Vector2(bar_px, 0.0), color, 2.0)
	draw_line(origin + Vector2(0.0, -4.0), origin + Vector2(0.0, 4.0), color, 2.0)
	draw_line(origin + Vector2(bar_px, -4.0), origin + Vector2(bar_px, 4.0), color, 2.0)
	draw_string(ThemeDB.fallback_font, origin + Vector2(0.0, -8.0), "%d km" % int(SCALE_BAR_M / 1000.0),
		HORIZONTAL_ALIGNMENT_LEFT, -1, LABEL_FONT_SIZE, color)


# --- 도우미 -----------------------------------------------------------------

func _zoom() -> float:
	return zoom


## 화면에서 최소 길이를 보장한 함선 길이(px).
func _ship_length_px(ship: Variant) -> float:
	return maxf(float(ship.length_m()) * _zoom(), MIN_SHIP_LENGTH_PX)


## 주포 최대 사거리(m). 주포가 없으면 부포 기준.
func _main_range_m(ship: Variant) -> float:
	var main_km := 0.0
	var sec_km := 0.0
	for g in ship.guns:
		var gun: Dictionary = g.gun
		var r: float = float(gun.maxRangeKm)
		if String(g.type) == "main":
			main_km = maxf(main_km, r)
		elif String(g.type) == "secondary":
			sec_km = maxf(sec_km, r)
	var km: float = main_km if main_km > 0.0 else sec_km
	return km * 1000.0


## 종진에서 앞 배를 따라가는 중인가(그 진영 지휘관의 명령 mode 가 follow).
func _is_follower(ship: Variant) -> bool:
	var side := int(ship.side)
	if side < 0 or side >= commanders.size():
		return false
	var cmd: Variant = commanders[side]
	if cmd == null:
		return false
	var o: Dictionary = cmd.order_of(String(ship.id))
	return String(o.get("mode", "")) == "follow"
