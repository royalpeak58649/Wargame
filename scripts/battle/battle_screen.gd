extends Control
## 전투 화면 (관전 모드): 양쪽 모두 AI 가 지휘하는 전투를 그리고 시간을 조절한다.
##
## 구성: World(Node2D, battle_world.gd) + Camera2D + HUD(CanvasLayer).
## 시간: 전장 시간 = 실제 시간 × BASE_TIME_SCALE × 배속. SIM_DT 고정 단계로 누적기를 돌린다.
## 매 단계 순서: 각 진영 ai.update → commander.update → sim.step.
##
## 조작: 휠 확대·축소, 가운데 버튼(또는 Space+왼쪽) 드래그 이동,
##       Space 일시정지/재개, 1·2·3 배속, 왼쪽 클릭 함선 선택.

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

const SIDE_LABELS := ["아군", "적군"]
const SIDE_COLORS := [Color("#5aa0ff"), Color("#ff6a5a")]
const AI_STATE_LABELS := {
	"search": "수색",
	"approach": "접근",
	"engage": "교전",
	"retreat": "퇴각",
}
const GUN_TYPE_LABELS := {"main": "주포", "secondary": "부포", "torpedo": "어뢰"}
const REASON_LABELS := {"annihilation": "격멸", "withdrawal": "적 퇴각", "mutual": "상호 전멸", "time": "시간 판정"}

# --- 상태 -------------------------------------------------------------------

var sim: Variant = null
var commanders: Array = []
var ais: Array = []
var scenario_name := ""
var selected_id := ""

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
var _end_table: RichTextLabel = null

# 입력
var _panning := false
var _space_down := false


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
		ai.update(SIM_DT)
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
				_select_at(event.position)
			else:
				_panning = false


func _handle_mouse_motion(event: InputEventMouseMotion) -> void:
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


## 마우스 위치를 고정한 채 확대·축소한다.
func _zoom_at(screen_pos: Vector2, factor: float) -> void:
	var before := _screen_to_world(screen_pos)
	_world.zoom = clampf(_world.zoom * factor, ZOOM_MIN, ZOOM_MAX)
	var after := _screen_to_world(screen_pos)
	_world.cam_center += before - after


func _screen_to_world(screen_pos: Vector2) -> Vector2:
	return _world.screen_to_world(screen_pos)


## 화면상 PICK_RADIUS_PX 안에서 가장 가까운 함선을 고른다. 빈 곳이면 선택 해제.
func _select_at(screen_pos: Vector2) -> void:
	var best_id := ""
	var best_d := PICK_RADIUS_PX
	for s in sim.ships:
		var sp: Vector2 = _world.view(s.pos)
		var d: float = sp.distance_to(screen_pos)
		if d <= best_d:
			best_d = d
			best_id = String(s.id)
	selected_id = best_id
	_world.selected_id = selected_id
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

	_end_table = RichTextLabel.new()
	_end_table.bbcode_enabled = true
	_end_table.fit_content = true
	_end_table.scroll_active = false
	_end_table.custom_minimum_size = Vector2(560.0, 0.0)
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


func _update_info_panel() -> void:
	var ship: Variant = sim.get_ship(selected_id) if selected_id != "" else null
	if ship == null:
		_info_panel.visible = false
		return
	_info_panel.visible = true
	var side := clampi(int(ship.side), 0, 1)
	var color: String = (SIDE_COLORS[side] as Color).to_html(false)
	var hp_frac := clampf(float(ship.hp) / maxf(float(ship.max_hp), 1.0), 0.0, 1.0)
	var ai: Variant = ais[side]
	var target_id := String(ai.targets.get(String(ship.id), "-"))
	var state := String(ai.state_of(String(ship.id)))

	var lines: Array[String] = []
	lines.append("[b]%s[/b]  (%s)" % [String(ship.design.get("name", "")), String(ship.id)])
	lines.append("[color=#%s]%s[/color]" % [color, SIDE_LABELS[side]])
	lines.append("HP %.0f / %.0f (%.0f%%)" % [maxf(float(ship.hp), 0.0), float(ship.max_hp), hp_frac * 100.0])
	lines.append("속도 %.1f kn   침로 %.0f°" % [float(ship.speed_ms) / ShipState.KNOT_TO_MS, float(ship.heading_deg)])
	lines.append("화재 %d   침수 %d" % [
		int(ship.active_fires(float(sim.time))), int(ship.active_floods(float(sim.time)))])
	lines.append("AI %s   목표 %s" % [String(AI_STATE_LABELS.get(state, state)), target_id])
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
	_end_table.text = _summary_table()
	_end_panel.visible = true


func _summary_table() -> String:
	var sum: Dictionary = sim.summary()
	var lines: Array[String] = []
	lines.append("[b]함선  진영  발사  명중  관통  가한 피해  받은 피해  결과[/b]")
	for s in sim.ships:
		var r: Dictionary = sum[s.id]
		var side := clampi(int(s.side), 0, 1)
		lines.append("%s  %s  %d  %d  %d  %.0f  %.0f  %s" % [
			String(s.id), SIDE_LABELS[side], int(r.fired), int(r.hits), int(r.pen),
			float(r.damageDealt), float(r.damageTaken),
			"침몰" if bool(r.sunk) else ("이탈" if bool(r.withdrawn) else "생존")])
	return "\n".join(lines)


func _format_time(t: float) -> String:
	var total := int(maxf(t, 0.0))
	return "%02d:%02d" % [total / 60, total % 60]
