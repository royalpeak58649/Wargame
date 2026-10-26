extends Control
## 전투 화면: 아군(side 0)은 플레이어가, 적군은 AI 가 지휘한다.
## 시나리오의 control 이 "ai" 인 진영만 AI 가 맡는다(둘 다 ai 면 관전 모드).
##
## 구성: World(Node2D, battle_world.gd, 시점 변환을 직접 한다) + HUD(CanvasLayer).
## 시간: 전장 시간 = 실제 시간 × BASE_TIME_SCALE × 배속. SIM_DT 고정 단계로 누적기를 돌린다.
## 매 단계 순서: 각 진영 ai.update → commander.update → sim.step.
##
## 조작: 휠 확대·축소, 가운데 버튼(또는 Space+왼쪽) 드래그 이동,
##       Space 일시정지/재개, 1·2·3 배속, 왼쪽 클릭/Shift+클릭/드래그 선택,
##       오른쪽 클릭 이동(빈 바다)·목표 지정(적 함선), Shift+오른쪽 경유점 추가,
##       Z X C V B 속도(정지~전속), F 종진, G 진형 해제.

const PartsDB = preload("res://scripts/core/parts_db.gd")
const ShipState = preload("res://scripts/combat/ship_state.gd")
const BattleScenario = preload("res://scripts/battle/battle_scenario.gd")
const BattleWorld = preload("res://scripts/battle/battle_world.gd")

const PARTS_PATH := "res://data/parts_t2.json"
const SCENARIO_PATH := "res://data/scenarios/test_battle.json"
const DESIGNER_SCENE := "res://scenes/designer/designer.tscn"

const BASE_TIME_SCALE := 4.0       # 무차원, 화면 1분 = 전장 4분
const SIM_DT := 0.5                # s, 고정 시간 단계
const MAX_STEPS_PER_FRAME := 64    # 개, 한 프레임에 돌리는 최대 단계(폭주 방지)
const SPEEDS := [1, 2, 4]          # 배속
const MAP_HALF_M := 22500.0        # m, 맵 반폭(45km)
const ZOOM_MIN := 0.00002          # px/m, 최소 확대
const ZOOM_MAX := 0.02             # px/m, 최대 확대
const ZOOM_STEP := 1.15            # 무차원, 휠 한 칸 배율
const PICK_RADIUS_PX := 20.0       # px, 함선 선택 반경
const PAN_SPEED_PX := 900.0        # px/s, 키보드 이동 속도
const PLAYER_SIDE := 0             # 플레이어가 지휘하는 진영
const DRAG_THRESHOLD_PX := 8.0     # px, 이 이상 움직이면 드래그 선택
const LINE_SPACING_M := 600.0      # m, 여러 척 이동 명령 시 횡렬 간격
const LOG_LINES := 8               # 줄, 전투 로그 패널에 보이는 사건 수
const SPEED_FRACS := [0.0, 0.25, 0.5, 0.75, 1.0]
const SPEED_LABELS := ["정지", "1/4", "1/2", "3/4", "전속"]
const SPEED_KEYS := [KEY_Z, KEY_X, KEY_C, KEY_V, KEY_B]
const RESULT_LABELS := {"pen": "관통", "nonpen": "불관통", "ricochet": "도탄"}
const RESULT_COLORS := {"pen": "#ff9a40", "nonpen": "#ffe066", "ricochet": "#a0a0a0"}

const SIDE_LABELS := ["아군", "적군"]
const SIDE_COLORS := [Color("#5aa0ff"), Color("#ff6a5a")]
const AI_STATE_LABELS := {
	"search": "수색",
	"approach": "접근",
	"engage": "교전",
	"retreat": "퇴각",
}
const GUN_TYPE_LABELS := {"main": "주포", "secondary": "부포", "torpedo": "어뢰"}
const MODE_LABELS := {"hold": "침로 유지", "move": "이동", "heading": "방위 유지", "follow": "종진"}
const REASON_LABELS := {"annihilation": "격멸", "withdrawal": "적 퇴각", "mutual": "상호 전멸", "time": "시간 판정"}

# --- 상태 -------------------------------------------------------------------

var sim: Variant = null
var commanders: Array = []
var ais: Array = []
var scenario_name := ""
var selected_id := ""
## 플레이어가 선택한 아군 함선(선택 순서). 명령은 이 배들에게 간다.
var selected_ids: Array[String] = []
## 플레이어가 직접 지정한 목표(함선 id → 목표 id). 자동 목표는 이것을 덮어쓰지 않는다.
var _manual_targets: Dictionary = {}

var paused := false
var speed_index := 0
var _accum := 0.0
var _finished := false

var _world: Node2D = null
var _hud: CanvasLayer = null

# HUD 위젯
var _top_bar: PanelContainer = null
var _title_label: Label = null
var _time_label: Label = null
var _alive_label: Label = null
var _pause_button: Button = null
var _speed_buttons: Array[Button] = []
var _info_panel: PanelContainer = null
var _info_label: RichTextLabel = null
var _end_panel: PanelContainer = null
var _end_title: Label = null
var _end_time: Label = null
var _end_table: GridContainer = null
var _command_bar: PanelContainer = null
var _speed_order_buttons: Array[Button] = []
var _log_panel: PanelContainer = null
var _log_label: RichTextLabel = null
var _log_lines: Array[String] = []
var _log_cursor := 0

# 입력
var _panning := false
var _space_down := false
var _left_down := false
var _dragging := false
var _drag_start := Vector2.ZERO


func _ready() -> void:
	_build_ui()
	_start_battle()


# --- 전투 시작 ---------------------------------------------------------------

func _start_battle() -> void:
	var db := PartsDB.load_db(PARTS_PATH)
	var scenario := BattleScenario.load_scenario(SCENARIO_PATH)
	var built := BattleScenario.build(scenario, db)
	sim = built.sim
	commanders = built.commanders
	ais = built.ais
	scenario_name = String(scenario.get("name", "전투"))

	selected_id = ""
	selected_ids.clear()
	_manual_targets.clear()
	_log_lines.clear()
	_log_cursor = 0
	paused = false
	speed_index = 0
	_accum = 0.0
	_finished = false
	_end_panel.visible = false

	_world.setup(sim, commanders, ais, scenario_name)
	_reset_camera()
	_update_hud()


func _reset_camera() -> void:
	var view := get_viewport_rect().size
	_world.zoom = view.y / (MAP_HALF_M * 2.0)
	_world.cam_center = Vector2.ZERO


# --- 시간 진행 ---------------------------------------------------------------

func _process(delta: float) -> void:
	if sim == null:
		return
	_handle_pan_keys(delta)
	if not paused and not _finished:
		_accum += delta * BASE_TIME_SCALE * float(SPEEDS[speed_index])
		var steps := 0
		while _accum >= SIM_DT and steps < MAX_STEPS_PER_FRAME:
			_accum -= SIM_DT
			_step_once()
			steps += 1
			if int(sim.result().winner) != -1:
				_finish_battle()
				break
		if steps >= MAX_STEPS_PER_FRAME:
			_accum = 0.0
	_world.refresh()
	_update_hud()


## 한 단계: 각 진영 ai → commander → sim.
func _step_once() -> void:
	for ai in ais:
		if ai != null:
			ai.update(SIM_DT)
	_auto_target_player()
	for cmd in commanders:
		cmd.update(SIM_DT)
	sim.step(SIM_DT)


## WASD·방향키로 화면을 옮긴다(가운데 버튼 드래그의 대안).
func _handle_pan_keys(delta: float) -> void:
	var dir := Vector2.ZERO
	if Input.is_key_pressed(KEY_A) or Input.is_key_pressed(KEY_LEFT):
		dir.x -= 1.0
	if Input.is_key_pressed(KEY_D) or Input.is_key_pressed(KEY_RIGHT):
		dir.x += 1.0
	if Input.is_key_pressed(KEY_W) or Input.is_key_pressed(KEY_UP):
		dir.y -= 1.0
	if Input.is_key_pressed(KEY_S) or Input.is_key_pressed(KEY_DOWN):
		dir.y += 1.0
	if dir == Vector2.ZERO:
		return
	# 화면 아래쪽(+y)이 전장 남쪽(−y)이므로 y 를 뒤집는다.
	var d: Vector2 = dir.normalized() * PAN_SPEED_PX * delta / float(_world.zoom)
	_world.cam_center += Vector2(d.x, -d.y)


## 플레이어 진영 편의: 목표가 없거나 비활성이면 주포 사거리 안의 가장 가까운 적을 지정한다.
## 플레이어가 직접 지정한 목표는 그 목표가 비활성이 될 때까지 유지한다.
func _auto_target_player() -> void:
	if ais.size() > PLAYER_SIDE and ais[PLAYER_SIDE] != null:
		return  # 이 진영은 AI 가 맡는다
	var cmd: Variant = commanders[PLAYER_SIDE]
	for s in sim.ships:
		if int(s.side) != PLAYER_SIDE or not s.is_active():
			continue
		var id := String(s.id)
		var current: Variant = sim.get_ship(String(s.target_id)) if String(s.target_id) != "" else null
		if current != null and current.is_active():
			continue
		_manual_targets.erase(id)
		var best: Variant = null
		var best_d: float = _main_range_m(s)
		for e in sim.ships:
			if int(e.side) == PLAYER_SIDE or not e.is_active():
				continue
			var d: float = s.pos.distance_to(e.pos)
			if d <= best_d:
				best_d = d
				best = e
		if best != null:
			cmd.order_target(id, String(best.id))


func _main_range_m(ship: Variant) -> float:
	var km := 0.0
	for g in ship.guns:
		if String(g.type) == "main":
			km = maxf(km, float((g.gun as Dictionary).maxRangeKm))
	return km * 1000.0


func _finish_battle() -> void:
	_finished = true
	paused = true
	_show_end_panel()


# --- 입력 -------------------------------------------------------------------

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseButton:
		_handle_mouse_button(event)
	elif event is InputEventMouseMotion:
		_handle_mouse_motion(event)
	elif event is InputEventKey:
		_handle_key(event)


func _handle_mouse_button(event: InputEventMouseButton) -> void:
	match event.button_index:
		MOUSE_BUTTON_WHEEL_UP:
			if event.pressed:
				_zoom_at(event.position, ZOOM_STEP)
		MOUSE_BUTTON_WHEEL_DOWN:
			if event.pressed:
				_zoom_at(event.position, 1.0 / ZOOM_STEP)
		MOUSE_BUTTON_MIDDLE:
			_panning = event.pressed
		MOUSE_BUTTON_LEFT:
			if event.pressed and _space_down:
				_panning = true
			elif event.pressed:
				_left_down = true
				_dragging = false
				_drag_start = event.position
			else:
				_panning = false
				if _left_down:
					if _dragging:
						_box_select(Rect2(_drag_start, event.position - _drag_start).abs(), event.shift_pressed)
					else:
						_click_select(event.position, event.shift_pressed)
				_left_down = false
				_dragging = false
				_world.drag_rect = Rect2()
		MOUSE_BUTTON_RIGHT:
			if event.pressed:
				_command_at(event.position, event.shift_pressed)


func _handle_mouse_motion(event: InputEventMouseMotion) -> void:
	if _left_down and not _panning:
		if not _dragging and event.position.distance_to(_drag_start) >= DRAG_THRESHOLD_PX:
			_dragging = true
		if _dragging:
			_world.drag_rect = Rect2(_drag_start, event.position - _drag_start)
		return
	if _panning:
		var d: Vector2 = event.relative / _world.zoom
		_world.cam_center += Vector2(-d.x, d.y)


func _handle_key(event: InputEventKey) -> void:
	if event.keycode == KEY_SPACE:
		_space_down = event.pressed
		if event.pressed and not event.echo:
			_toggle_pause()
		return
	if not event.pressed or event.echo:
		return
	match event.keycode:
		KEY_1:
			_set_speed(0)
		KEY_2:
			_set_speed(1)
		KEY_3:
			_set_speed(2)
		KEY_F:
			_order_column()
		KEY_G:
			_order_break()
		_:
			var i := SPEED_KEYS.find(event.keycode)
			if i >= 0:
				_order_speed(float(SPEED_FRACS[i]))


## 마우스 위치를 고정한 채 확대·축소한다.
func _zoom_at(screen_pos: Vector2, factor: float) -> void:
	var before := _screen_to_world(screen_pos)
	_world.zoom = clampf(_world.zoom * factor, ZOOM_MIN, ZOOM_MAX)
	var after := _screen_to_world(screen_pos)
	_world.cam_center += before - after


func _screen_to_world(screen_pos: Vector2) -> Vector2:
	return _world.screen_to_world(screen_pos)


## 화면상 PICK_RADIUS_PX 안에서 가장 가까운 활성 함선 id. side 가 −1 이 아니면 그 진영만.
func _pick_ship(screen_pos: Vector2, side: int = -1) -> String:
	var best_id := ""
	var best_d := PICK_RADIUS_PX
	for s in sim.ships:
		if not s.is_active():
			continue
		if side != -1 and int(s.side) != side:
			continue
		var sp: Vector2 = _world.view(s.pos)
		var d: float = sp.distance_to(screen_pos)
		if d <= best_d:
			best_d = d
			best_id = String(s.id)
	return best_id


## 이전 호환: 클릭 한 번으로 선택(Shift 없음).
func _select_at(screen_pos: Vector2) -> void:
	_click_select(screen_pos, false)


## 왼쪽 클릭: 함선 하나 선택(아군이면 명령 대상). Shift: 아군을 선택에 추가/제거.
func _click_select(screen_pos: Vector2, additive: bool) -> void:
	var id := _pick_ship(screen_pos)
	var ship: Variant = sim.get_ship(id) if id != "" else null
	if additive:
		if ship != null and int(ship.side) == PLAYER_SIDE:
			if selected_ids.has(id):
				selected_ids.erase(id)
			else:
				selected_ids.append(id)
			selected_id = id
	else:
		selected_ids.clear()
		if ship != null and int(ship.side) == PLAYER_SIDE:
			selected_ids.append(id)
		selected_id = id
	_sync_selection()


## 드래그 사각형 안의 아군 활성 함선을 선택한다. Shift 면 기존 선택에 더한다.
func _box_select(rect: Rect2, additive: bool) -> void:
	if not additive:
		selected_ids.clear()
	for s in sim.ships:
		if int(s.side) != PLAYER_SIDE or not s.is_active():
			continue
		if rect.has_point(_world.view(s.pos)) and not selected_ids.has(String(s.id)):
			selected_ids.append(String(s.id))
	if not selected_ids.is_empty():
		selected_id = selected_ids[selected_ids.size() - 1]
	_sync_selection()


func _sync_selection() -> void:
	# 침몰·이탈한 배는 선택에서 뺀다.
	var kept: Array[String] = []
	for id in selected_ids:
		var s: Variant = sim.get_ship(id)
		if s != null and s.is_active():
			kept.append(id)
	selected_ids = kept
	_world.selected_ids = selected_ids
	_world.selected_id = selected_id
	_update_hud()


func _player_cmd() -> Variant:
	return commanders[PLAYER_SIDE] if commanders.size() > PLAYER_SIDE else null


func _can_command() -> bool:
	if _finished or selected_ids.is_empty():
		return false
	return ais.size() <= PLAYER_SIDE or ais[PLAYER_SIDE] == null


## 오른쪽 클릭: 적 함선 위면 목표 지정, 빈 바다면 이동(Shift: 경유점 추가).
func _command_at(screen_pos: Vector2, append: bool) -> void:
	_sync_selection()
	if not _can_command():
		return
	var cmd: Variant = _player_cmd()
	var enemy_id := _pick_ship(screen_pos, 1 - PLAYER_SIDE)
	if enemy_id != "":
		for id in selected_ids:
			cmd.order_target(id, enemy_id)
			_manual_targets[id] = enemy_id
		_world.add_mark(sim.get_ship(enemy_id).pos, "target")
		return
	var point: Vector2 = _world.screen_to_world(screen_pos)
	var spots := _line_spots(point)
	for i in selected_ids.size():
		var id: String = selected_ids[i]
		var ship: Variant = sim.get_ship(id)
		var frac: float = float(ship.order_speed_frac)
		if frac <= 0.0:
			frac = 1.0
		var wps: Array[Vector2] = []
		if append:
			var o: Dictionary = cmd.order_of(id)
			if String(o.get("mode", "")) == "move":
				for w in o.get("waypoints", []):
					wps.append(w)
		wps.append(spots[i])
		cmd.order_move(id, wps, frac)
	_world.add_mark(point, "move")


## 여러 척 이동: 선택 무리 중심 → 목표점 방향의 직각으로 LINE_SPACING_M 간격 횡렬 위치.
func _line_spots(point: Vector2) -> Array[Vector2]:
	var out: Array[Vector2] = []
	var n := selected_ids.size()
	if n <= 1:
		out.append(point)
		return out
	var center := Vector2.ZERO
	for id in selected_ids:
		center += sim.get_ship(id).pos
	center /= float(n)
	var dir := (point - center).normalized()
	if dir == Vector2.ZERO:
		dir = Vector2.UP
	var side_dir := Vector2(dir.y, -dir.x)
	for i in n:
		var offset := (float(i) - float(n - 1) * 0.5) * LINE_SPACING_M
		out.append(point + side_dir * offset)
	return out


func _order_speed(frac: float) -> void:
	_sync_selection()
	if not _can_command():
		return
	for id in selected_ids:
		_player_cmd().order_speed(id, frac)
	_update_hud()


func _order_column() -> void:
	_sync_selection()
	if not _can_command() or selected_ids.size() < 2:
		return
	_player_cmd().form_column(selected_ids)
	_update_hud()


func _order_break() -> void:
	_sync_selection()
	if not _can_command():
		return
	for id in selected_ids:
		_player_cmd().break_formation(id)
	_update_hud()


func _toggle_pause() -> void:
	if _finished:
		return
	paused = not paused
	_update_hud()


func _set_speed(index: int) -> void:
	speed_index = clampi(index, 0, SPEEDS.size() - 1)
	_update_hud()


# --- HUD 구성 ---------------------------------------------------------------

func _build_ui() -> void:
	_world = BattleWorld.new()
	_world.name = "World"
	add_child(_world)


	_hud = CanvasLayer.new()
	_hud.name = "HUD"
	add_child(_hud)

	_build_top_bar()
	_build_info_panel()
	_build_command_bar()
	_build_log_panel()
	_build_end_panel()


func _build_top_bar() -> void:
	_top_bar = PanelContainer.new()
	_top_bar.name = "TopBar"
	_top_bar.set_anchors_preset(Control.PRESET_TOP_WIDE)
	_top_bar.offset_bottom = 44.0
	_hud.add_child(_top_bar)

	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 12)
	_top_bar.add_child(row)

	_title_label = Label.new()
	row.add_child(_title_label)

	_time_label = Label.new()
	row.add_child(_time_label)

	_pause_button = Button.new()
	_pause_button.focus_mode = Control.FOCUS_NONE
	_pause_button.pressed.connect(_toggle_pause)
	row.add_child(_pause_button)

	for i in SPEEDS.size():
		var b := Button.new()
		b.text = "%d×" % int(SPEEDS[i])
		b.focus_mode = Control.FOCUS_NONE
		b.pressed.connect(_set_speed.bind(i))
		row.add_child(b)
		_speed_buttons.append(b)

	_alive_label = Label.new()
	row.add_child(_alive_label)


func _build_info_panel() -> void:
	_info_panel = PanelContainer.new()
	_info_panel.name = "InfoPanel"
	_info_panel.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	_info_panel.offset_left = -320.0
	_info_panel.offset_top = 52.0
	_info_panel.offset_right = -8.0
	_info_panel.offset_bottom = 52.0
	_info_panel.grow_vertical = Control.GROW_DIRECTION_END
	_info_panel.visible = false
	_hud.add_child(_info_panel)

	_info_label = RichTextLabel.new()
	_info_label.bbcode_enabled = true
	_info_label.fit_content = true
	_info_label.scroll_active = false
	_info_label.custom_minimum_size = Vector2(300.0, 0.0)
	_info_panel.add_child(_info_label)


func _build_command_bar() -> void:
	_command_bar = PanelContainer.new()
	_command_bar.name = "CommandBar"
	_command_bar.set_anchors_preset(Control.PRESET_CENTER_BOTTOM)
	_command_bar.grow_horizontal = Control.GROW_DIRECTION_BOTH
	_command_bar.grow_vertical = Control.GROW_DIRECTION_BEGIN
	_command_bar.offset_bottom = -8.0
	_command_bar.visible = false
	_hud.add_child(_command_bar)

	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 6)
	_command_bar.add_child(row)
	var keys := ["Z", "X", "C", "V", "B"]
	for i in SPEED_FRACS.size():
		var b := Button.new()
		b.text = "%s (%s)" % [SPEED_LABELS[i], keys[i]]
		b.toggle_mode = true
		b.focus_mode = Control.FOCUS_NONE
		b.pressed.connect(_order_speed.bind(float(SPEED_FRACS[i])))
		row.add_child(b)
		_speed_order_buttons.append(b)
	row.add_child(VSeparator.new())
	var col := Button.new()
	col.text = "종진 (F)"
	col.focus_mode = Control.FOCUS_NONE
	col.pressed.connect(_order_column)
	row.add_child(col)
	var brk := Button.new()
	brk.text = "진형 해제 (G)"
	brk.focus_mode = Control.FOCUS_NONE
	brk.pressed.connect(_order_break)
	row.add_child(brk)


func _build_log_panel() -> void:
	_log_panel = PanelContainer.new()
	_log_panel.name = "LogPanel"
	_log_panel.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	_log_panel.grow_vertical = Control.GROW_DIRECTION_BEGIN
	_log_panel.offset_left = 8.0
	_log_panel.offset_bottom = -56.0
	_hud.add_child(_log_panel)
	_log_label = RichTextLabel.new()
	_log_label.bbcode_enabled = true
	_log_label.fit_content = true
	_log_label.scroll_active = false
	_log_label.custom_minimum_size = Vector2(340.0, 0.0)
	_log_label.add_theme_font_size_override("normal_font_size", 13)
	_log_label.add_theme_font_size_override("bold_font_size", 13)
	_log_panel.add_child(_log_label)


func _build_end_panel() -> void:
	_end_panel = PanelContainer.new()
	_end_panel.name = "EndPanel"
	_end_panel.set_anchors_preset(Control.PRESET_CENTER)
	_end_panel.grow_horizontal = Control.GROW_DIRECTION_BOTH
	_end_panel.grow_vertical = Control.GROW_DIRECTION_BOTH
	_end_panel.visible = false
	_hud.add_child(_end_panel)

	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 8)
	_end_panel.add_child(box)

	_end_title = Label.new()
	_end_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_end_title.add_theme_font_size_override("font_size", 32)
	box.add_child(_end_title)

	_end_time = Label.new()
	_end_time.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	box.add_child(_end_time)

	_end_table = GridContainer.new()
	_end_table.columns = 8
	_end_table.add_theme_constant_override("h_separation", 18)
	box.add_child(_end_table)

	var buttons := HBoxContainer.new()
	buttons.alignment = BoxContainer.ALIGNMENT_CENTER
	buttons.add_theme_constant_override("separation", 12)
	box.add_child(buttons)

	var retry := Button.new()
	retry.text = "다시 하기"
	retry.focus_mode = Control.FOCUS_NONE
	retry.pressed.connect(_on_retry)
	buttons.add_child(retry)

	var to_designer := Button.new()
	to_designer.text = "설계 화면으로"
	to_designer.focus_mode = Control.FOCUS_NONE
	to_designer.pressed.connect(_on_to_designer)
	buttons.add_child(to_designer)


func _on_retry() -> void:
	get_tree().reload_current_scene()


func _on_to_designer() -> void:
	get_tree().change_scene_to_file(DESIGNER_SCENE)


# --- HUD 갱신 ---------------------------------------------------------------

func _update_hud() -> void:
	if sim == null:
		return
	_title_label.text = scenario_name
	var limit: float = float(sim.time_limit_s)
	if limit > 0.0:
		_time_label.text = "경과 %s · 남은 %s" % [_format_time(float(sim.time)), _format_time(limit - float(sim.time))]
	else:
		_time_label.text = "경과 %s" % _format_time(float(sim.time))
	_pause_button.text = "재개" if paused else "일시정지"
	for i in _speed_buttons.size():
		var b: Button = _speed_buttons[i]
		b.disabled = i == speed_index
	_alive_label.text = "아군 %d / 적군 %d" % [sim.side_alive(0), sim.side_alive(1)]
	_update_info_panel()
	_update_command_bar()
	_update_log()


func _update_command_bar() -> void:
	var show := _can_command()
	_command_bar.visible = show
	if not show:
		return
	# 선택된 배들의 속도 명령이 모두 같으면 그 버튼을 눌린 상태로 보인다.
	var common := -1.0
	for id in selected_ids:
		var o: Dictionary = _player_cmd().order_of(id)
		var f: float = float(o.get("speed_frac", sim.get_ship(id).order_speed_frac))
		if common < 0.0:
			common = f
		elif absf(common - f) > 0.001:
			common = -2.0
	for i in _speed_order_buttons.size():
		_speed_order_buttons[i].set_pressed_no_signal(absf(float(SPEED_FRACS[i]) - common) < 0.001)


## 아군이 관련된 새 사건을 로그 줄로 바꿔 최근 LOG_LINES 개만 보인다.
func _update_log() -> void:
	var log: Array = sim.log
	while _log_cursor < log.size():
		var e: Dictionary = log[_log_cursor]
		_log_cursor += 1
		var line := _log_line(e)
		if line != "":
			_log_lines.append(line)
	while _log_lines.size() > LOG_LINES:
		_log_lines.remove_at(0)
	_log_label.text = "\n".join(_log_lines)
	_log_panel.visible = not _log_lines.is_empty()


func _log_line(e: Dictionary) -> String:
	var kind := String(e.get("kind", ""))
	var t := _format_time(float(e.get("t", 0.0)))
	var target_id := String(e.get("target", ""))
	var target: Variant = sim.get_ship(target_id)
	match kind:
		"hit":
			var result := String(e.get("result", "miss"))
			if result == "miss":
				return ""
			var shooter: Variant = sim.get_ship(String(e.get("shooter", "")))
			if shooter == null or target == null:
				return ""
			if int(shooter.side) != PLAYER_SIDE and int(target.side) != PLAYER_SIDE:
				return ""
			var gun_key := String(e.get("gun", ""))
			var gun_label := "주포" if gun_key.begins_with("main") else ("부포" if gun_key.begins_with("sec") else gun_key)
			# 부포 불관통·도탄은 피해가 거의 없고 수가 많아 로그를 덮으므로 뺀다.
			if gun_label == "부포" and result != "pen":
				return ""
			var dmg := float(e.get("damage", 0.0))
			return "[color=%s]%s %s → %s %s %s −%.0f[/color]" % [
				String(RESULT_COLORS.get(result, "#ffffff")), t, String(shooter.id), target_id,
				gun_label, String(RESULT_LABELS.get(result, result)), dmg]
		"sunk":
			return "[b]%s %s 침몰[/b]" % [t, target_id]
		"withdrawn":
			return "[b]%s %s 전장 이탈[/b]" % [t, target_id]
	return ""


func _update_info_panel() -> void:
	var ship: Variant = sim.get_ship(selected_id) if selected_id != "" else null
	if ship == null:
		_info_panel.visible = false
		return
	_info_panel.visible = true
	var side := clampi(int(ship.side), 0, 1)
	var color: String = (SIDE_COLORS[side] as Color).to_html(false)
	var hp_frac := clampf(float(ship.hp) / maxf(float(ship.max_hp), 1.0), 0.0, 1.0)
	var ai: Variant = ais[side] if side < ais.size() else null
	var target_id := String(ship.target_id) if String(ship.target_id) != "" else "-"
	var control_text := ""
	if ai != null:
		var state := String(ai.state_of(String(ship.id)))
		control_text = "AI %s" % String(AI_STATE_LABELS.get(state, state))
	else:
		var o: Dictionary = commanders[side].order_of(String(ship.id))
		var mode := String(o.get("mode", "hold"))
		control_text = "명령 %s" % String(MODE_LABELS.get(mode, mode))
		if _manual_targets.has(String(ship.id)):
			target_id += " (지정)"

	var lines: Array[String] = []
	lines.append("[b]%s[/b]  (%s)" % [String(ship.design.get("name", "")), String(ship.id)])
	lines.append("[color=#%s]%s[/color]" % [color, SIDE_LABELS[side]])
	lines.append("HP %.0f / %.0f (%.0f%%)" % [maxf(float(ship.hp), 0.0), float(ship.max_hp), hp_frac * 100.0])
	lines.append("속도 %.1f kn   침로 %.0f°" % [float(ship.speed_ms) / ShipState.KNOT_TO_MS, float(ship.heading_deg)])
	lines.append("화재 %d   침수 %d" % [
		int(ship.active_fires(float(sim.time))), int(ship.active_floods(float(sim.time)))])
	lines.append("%s   목표 %s" % [control_text, target_id])
	lines.append("")
	lines.append("[b]무장[/b]")
	for g in ship.guns:
		var gun: Dictionary = g.gun
		var gtype := String(g.type)
		var status := "작동 불능" if bool(g.disabled) else "장전 %.0fs" % maxf(float(g.reload_left), 0.0)
		lines.append("%s %s  %s  %s" % [
			String(g.slot), String(GUN_TYPE_LABELS.get(gtype, gtype)), String(gun.get("name", "")), status])
	_info_label.text = "\n".join(lines)


func _show_end_panel() -> void:
	var r: Dictionary = sim.result()
	var w: int = int(r.winner)
	_end_title.text = "아군 승리" if w == 0 else ("적군 승리" if w == 1 else "무승부")
	var reason := String(REASON_LABELS.get(String(r.reason), String(r.reason)))
	_end_time.text = "%s · 경과 시간 %s" % [reason, _format_time(float(sim.time))]
	_fill_summary_table()
	_end_panel.visible = true


func _fill_summary_table() -> void:
	for c in _end_table.get_children():
		c.queue_free()
	var headers := ["함선", "진영", "발사", "명중", "관통", "가한 피해", "받은 피해", "결과"]
	for h in headers:
		var l := Label.new()
		l.text = String(h)
		l.add_theme_color_override("font_color", Color(0.75, 0.82, 0.95))
		_end_table.add_child(l)
	var sum: Dictionary = sim.summary()
	for s in sim.ships:
		var r: Dictionary = sum[s.id]
		var side := clampi(int(s.side), 0, 1)
		var cells := [String(s.id), SIDE_LABELS[side], "%d" % int(r.fired), "%d" % int(r.hits), "%d" % int(r.pen),
			"%.0f" % float(r.damageDealt), "%.0f" % float(r.damageTaken),
			"침몰" if bool(r.sunk) else ("이탈" if bool(r.withdrawn) else "생존")]
		for i in cells.size():
			var l := Label.new()
			l.text = String(cells[i])
			if i >= 2 and i <= 6:
				l.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
			if i == 1:
				l.add_theme_color_override("font_color", SIDE_COLORS[side])
			_end_table.add_child(l)


func _format_time(t: float) -> String:
	var total := int(maxf(t, 0.0))
	return "%02d:%02d" % [total / 60, total % 60]
