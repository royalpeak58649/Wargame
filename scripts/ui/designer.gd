extends Control
## 설계 화면 (프로토타입, T2).
##
## 왼쪽: 입력 패널(선체, 기관, 장갑, 무장, 사격 통제, 연료)
## 오른쪽: 결과 패널(무게 배분, 성능, 복원성, 전투 요약, 비용, 문제 목록)
##
## 모든 수치는 scripts/core/ship_calc.gd 만 쓴다. 계산식을 여기서 다시 구현하지 않는다.
## 파생값은 설계 JSON 에 저장하지 않는다. 저장하는 것은 ShipDesign 필드뿐이다.

const ShipCalc = preload("res://scripts/core/ship_calc.gd")
const PartsDB = preload("res://scripts/core/parts_db.gd")
const DesignerOptions = preload("res://scripts/ui/designer_options.gd")

const PARTS_PATH := "res://data/parts_t2.json"
const DESIGN_DIRS := ["res://data/designs", "user://designs"]
const USER_DESIGN_DIR := "user://designs"

const ARMOR_LABELS := {
	"beltMain": "주 벨트",
	"beltUpper": "상부 벨트",
	"deck": "갑판",
	"turretFace": "포탑 전면",
	"barbette": "바벳",
	"conningTower": "사령탑",
	"torpedoBulge": "어뢰 방어 벌지",
}

const GROUP_ORDER := ["structure", "armor", "machinery", "weapons", "fireControl", "fuel", "misc", "ballast"]
const GROUP_LABELS := {
	"structure": "선체 구조",
	"armor": "장갑",
	"machinery": "기관",
	"weapons": "무장",
	"fireControl": "사격 통제",
	"fuel": "연료",
	"misc": "기타",
	"ballast": "밸러스트",
}
const GROUP_COLORS := {
	"structure": Color(0.45, 0.55, 0.70),
	"armor": Color(0.70, 0.45, 0.35),
	"machinery": Color(0.85, 0.70, 0.30),
	"weapons": Color(0.75, 0.30, 0.30),
	"fireControl": Color(0.55, 0.40, 0.70),
	"fuel": Color(0.35, 0.65, 0.45),
	"misc": Color(0.55, 0.55, 0.55),
	"ballast": Color(0.30, 0.40, 0.50),
}
## 기획서 일반 범위(비율). 벗어나면 회색 참고 표시만 한다(경고 아님).
const GROUP_NORMAL_RANGE := {
	"structure": [0.30, 0.40],
	"armor": [0.20, 0.40],
	"machinery": [0.10, 0.20],
	"weapons": [0.10, 0.25],
	"fuel": [0.05, 0.10],
	"misc": [0.05, 0.10],
}

const SLOT_TYPE_LABELS := {"main": "주포", "secondary": "부포", "torpedo": "어뢰"}
const SPECIAL_LABELS := {"reward": "보상형", "buildable": "건조형", "paper": "계획함"}
const STABILITY_LABELS := {"ok": "정상", "warning": "경고", "invalid": "설계 불가"}
const STABILITY_COLORS := {
	"ok": Color(0.55, 0.85, 0.55),
	"warning": Color(0.95, 0.85, 0.35),
	"invalid": Color(0.95, 0.40, 0.40),
}
const EMPTY_LABEL := "비어 있음"
const DASH := "—"

var db: Dictionary = {}
var design: Dictionary = {}
var result: Dictionary = {}
var design_paths: Array = []      # 드롭다운 인덱스 -> 파일 경로
var design_ids: Array = []        # 드롭다운 인덱스 -> 설계 id
var current_path := ""            # res:// 에서 불러온 설계면 그 경로, 아니면 ""
var status_note := ""

var _updating := false            # 프로그램이 값을 넣는 중이면 true (시그널 루프 방지)

# --- 노드 참조 -------------------------------------------------------------
var design_dropdown: OptionButton
var name_edit: LineEdit
var meta_label: Label
var banner: Label
var save_button: Button
var new_button: Button

var hull_dropdown: OptionButton
var length_slider: HSlider
var length_spin: SpinBox
var beam_slider: HSlider
var beam_spin: SpinBox
var draught_label: Label

var machinery_dropdown: OptionButton
var power_slider: HSlider
var power_spin: SpinBox

var armor_spins: Dictionary = {}  # key -> SpinBox

var weapons_box: VBoxContainer
var weapon_rows: Array = []       # [{slot, dropdown, barrels, ids, barrel_ids}]

var fc_dropdown: OptionButton
var fuel_slider: HSlider
var fuel_label: Label

var weight_bar: Control
var weight_rows: Dictionary = {}  # group -> Label
var weight_total_label: Label
var perf_label: Label
var stability_label: Label
var combat_label: Label
var cost_label: Label
var issues_label: RichTextLabel
var status_label: Label


func _ready() -> void:
	db = PartsDB.load_many([PARTS_PATH])
	_build_ui()
	_load_design_list()
	if not design_ids.is_empty():
		_select_design(0)
	else:
		design = _new_design()
		_refresh_all()


# ---------------------------------------------------------------------------
# UI 구성
# ---------------------------------------------------------------------------

func _build_ui() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	var root := VBoxContainer.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	root.add_theme_constant_override("separation", 6)
	add_child(root)

	root.add_child(_build_top_bar())

	banner = Label.new()
	banner.visible = false
	banner.add_theme_color_override("font_color", Color(0.95, 0.80, 0.40))
	root.add_child(banner)

	var split := HSplitContainer.new()
	split.size_flags_vertical = Control.SIZE_EXPAND_FILL
	root.add_child(split)

	var left_scroll := ScrollContainer.new()
	left_scroll.custom_minimum_size = Vector2(560, 0)
	left_scroll.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	split.add_child(left_scroll)
	var left := VBoxContainer.new()
	left.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	left.add_theme_constant_override("separation", 10)
	left_scroll.add_child(left)
	_build_input_panel(left)

	var right_scroll := ScrollContainer.new()
	right_scroll.custom_minimum_size = Vector2(560, 0)
	right_scroll.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	split.add_child(right_scroll)
	var right := VBoxContainer.new()
	right.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	right.add_theme_constant_override("separation", 10)
	right_scroll.add_child(right)
	_build_result_panel(right)

	status_label = Label.new()
	root.add_child(status_label)


func _build_top_bar() -> Control:
	var bar := HBoxContainer.new()
	bar.add_theme_constant_override("separation", 8)

	bar.add_child(_label("설계"))
	design_dropdown = OptionButton.new()
	design_dropdown.custom_minimum_size = Vector2(260, 0)
	design_dropdown.item_selected.connect(_on_design_selected)
	bar.add_child(design_dropdown)

	new_button = Button.new()
	new_button.text = "새 설계"
	new_button.pressed.connect(_on_new_pressed)
	bar.add_child(new_button)

	save_button = Button.new()
	save_button.text = "저장"
	save_button.pressed.connect(_on_save_pressed)
	bar.add_child(save_button)

	bar.add_child(_label("이름"))
	name_edit = LineEdit.new()
	name_edit.custom_minimum_size = Vector2(220, 0)
	name_edit.text_changed.connect(_on_name_changed)
	bar.add_child(name_edit)

	meta_label = Label.new()
	bar.add_child(meta_label)
	return bar


func _build_input_panel(parent: VBoxContainer) -> void:
	# 1. 선체
	parent.add_child(_section("1. 선체"))
	var hull_row := HBoxContainer.new()
	hull_row.add_child(_label("템플릿"))
	hull_dropdown = OptionButton.new()
	hull_dropdown.custom_minimum_size = Vector2(240, 0)
	hull_dropdown.item_selected.connect(_on_hull_selected)
	hull_row.add_child(hull_dropdown)
	parent.add_child(hull_row)

	var length_row := _slider_row("길이", 0.5, _on_length_changed)
	length_slider = length_row.slider
	length_spin = length_row.spin
	parent.add_child(length_row.box)

	var beam_row := _slider_row("폭", 0.1, _on_beam_changed)
	beam_slider = beam_row.slider
	beam_spin = beam_row.spin
	parent.add_child(beam_row.box)

	var draught_row := HBoxContainer.new()
	draught_row.add_child(_label("흘수"))
	draught_label = Label.new()
	draught_row.add_child(draught_label)
	parent.add_child(draught_row)

	# 2. 기관
	parent.add_child(_section("2. 기관"))
	var mach_row := HBoxContainer.new()
	mach_row.add_child(_label("부품"))
	machinery_dropdown = OptionButton.new()
	machinery_dropdown.custom_minimum_size = Vector2(300, 0)
	machinery_dropdown.item_selected.connect(_on_machinery_selected)
	mach_row.add_child(machinery_dropdown)
	parent.add_child(mach_row)

	var power_row := _slider_row("출력(shp)", 500.0, _on_power_changed)
	power_slider = power_row.slider
	power_spin = power_row.spin
	parent.add_child(power_row.box)

	# 3. 장갑
	parent.add_child(_section("3. 장갑"))
	var armor_grid := GridContainer.new()
	armor_grid.columns = 2
	for key in ShipCalc.ARMOR_KEYS:
		armor_grid.add_child(_label(ARMOR_LABELS.get(key, key)))
		var spin := SpinBox.new()
		spin.step = 1.0
		spin.suffix = "mm"
		spin.custom_minimum_size = Vector2(140, 0)
		spin.value_changed.connect(_on_armor_changed.bind(String(key)))
		armor_grid.add_child(spin)
		armor_spins[key] = spin
	parent.add_child(armor_grid)

	# 4. 무장 (선체 슬롯 순서대로 코드로 생성)
	parent.add_child(_section("4. 무장"))
	weapons_box = VBoxContainer.new()
	weapons_box.add_theme_constant_override("separation", 4)
	parent.add_child(weapons_box)

	# 5. 사격 통제
	parent.add_child(_section("5. 사격 통제"))
	var fc_row := HBoxContainer.new()
	fc_dropdown = OptionButton.new()
	fc_dropdown.custom_minimum_size = Vector2(300, 0)
	fc_dropdown.item_selected.connect(_on_fc_selected)
	fc_row.add_child(fc_dropdown)
	parent.add_child(fc_row)

	# 6. 연료
	parent.add_child(_section("6. 연료"))
	var fuel_row := HBoxContainer.new()
	fuel_row.add_child(_label("연료 비율"))
	fuel_slider = HSlider.new()
	fuel_slider.step = 0.005
	fuel_slider.custom_minimum_size = Vector2(320, 0)
	fuel_slider.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	fuel_slider.value_changed.connect(_on_fuel_changed)
	fuel_row.add_child(fuel_slider)
	fuel_label = Label.new()
	fuel_label.custom_minimum_size = Vector2(70, 0)
	fuel_row.add_child(fuel_label)
	parent.add_child(fuel_row)


func _build_result_panel(parent: VBoxContainer) -> void:
	parent.add_child(_section("무게 배분"))
	weight_bar = Control.new()
	weight_bar.custom_minimum_size = Vector2(0, 34)
	weight_bar.draw.connect(_on_weight_bar_draw)
	parent.add_child(weight_bar)

	var grid := GridContainer.new()
	grid.columns = 3
	for g in GROUP_ORDER:
		var swatch := ColorRect.new()
		swatch.color = GROUP_COLORS[g]
		swatch.custom_minimum_size = Vector2(14, 14)
		grid.add_child(swatch)
		var name_label := Label.new()
		name_label.text = GROUP_LABELS[g]
		name_label.custom_minimum_size = Vector2(110, 0)
		grid.add_child(name_label)
		var value_label := Label.new()
		grid.add_child(value_label)
		weight_rows[g] = value_label
	parent.add_child(grid)

	weight_total_label = Label.new()
	parent.add_child(weight_total_label)

	parent.add_child(_section("성능"))
	perf_label = Label.new()
	parent.add_child(perf_label)

	parent.add_child(_section("복원성"))
	stability_label = Label.new()
	stability_label.add_theme_color_override("font_color", STABILITY_COLORS["ok"])
	parent.add_child(stability_label)

	parent.add_child(_section("전투 요약"))
	combat_label = Label.new()
	parent.add_child(combat_label)

	parent.add_child(_section("비용"))
	cost_label = Label.new()
	parent.add_child(cost_label)

	parent.add_child(_section("문제 목록"))
	issues_label = RichTextLabel.new()
	issues_label.bbcode_enabled = true
	issues_label.fit_content = true
	issues_label.scroll_active = false
	issues_label.custom_minimum_size = Vector2(0, 120)
	parent.add_child(issues_label)


func _section(title: String) -> Label:
	var l := Label.new()
	l.text = title
	l.add_theme_font_size_override("font_size", 18)
	return l


func _label(text: String) -> Label:
	var l := Label.new()
	l.text = text
	l.custom_minimum_size = Vector2(90, 0)
	return l


## 슬라이더 + 숫자 입력 한 줄. 둘은 서로 값을 맞춘다.
func _slider_row(title: String, step: float, on_changed: Callable) -> Dictionary:
	var box := HBoxContainer.new()
	box.add_child(_label(title))
	var slider := HSlider.new()
	slider.step = step
	slider.custom_minimum_size = Vector2(260, 0)
	slider.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	box.add_child(slider)
	var spin := SpinBox.new()
	spin.step = step
	spin.custom_minimum_size = Vector2(120, 0)
	box.add_child(spin)
	slider.value_changed.connect(on_changed)
	spin.value_changed.connect(on_changed)
	return {"box": box, "slider": slider, "spin": spin}


# ---------------------------------------------------------------------------
# 설계 목록 / 선택
# ---------------------------------------------------------------------------

func _load_design_list() -> void:
	design_paths.clear()
	design_ids.clear()
	design_dropdown.clear()
	for dir_path in DESIGN_DIRS:
		var dir := DirAccess.open(dir_path)
		if dir == null:
			continue
		var files: Array = []
		for f in dir.get_files():
			if f.ends_with(".json"):
				files.append(f)
		files.sort()
		for f in files:
			var path := dir_path.path_join(f)
			var d := PartsDB.load_design(path)
			if d.is_empty():
				continue
			design_paths.append(path)
			design_ids.append(String(d.get("id", f.get_basename())))
			var tag := "특별" if bool(d.get("locked", false)) else "편집"
			design_dropdown.add_item("%s [%s]" % [String(d.get("name", f)), tag])
	if design_dropdown.item_count > 0:
		design_dropdown.select(0)


func _on_design_selected(index: int) -> void:
	if _updating:
		return
	_select_design(index)


func _select_design(index: int) -> void:
	if index < 0 or index >= design_paths.size():
		return
	current_path = String(design_paths[index])
	design = PartsDB.load_design(current_path)
	status_note = ""
	_refresh_all()


func _on_new_pressed() -> void:
	design = _new_design()
	current_path = ""
	status_note = ""
	_refresh_all()


func _new_design() -> Dictionary:
	var hull: Dictionary = db.hulls.get("hull_bb_t2_a", {})
	var armor := {}
	for k in ShipCalc.ARMOR_KEYS:
		armor[k] = 0
	return {
		"id": "player_%d" % int(Time.get_unix_time_from_system()),
		"name": "새 설계",
		"tier": 2,
		"nation": "UK",
		"locked": false,
		"special": null,
		"shipClass": String(hull.get("shipClass", "BB")),
		"hull": {
			"templateId": "hull_bb_t2_a",
			"length": float(hull.get("length", 190.0)),
			"beam": float(hull.get("beam", 27.6)),
			"draught": float(hull.get("draught", 9.3)),
		},
		"machinery": {"partId": "turbine_t2_oil_heavy", "powerShp": 40000},
		"armor": armor,
		"weapons": [],
		"fireControl": "fc_t2_director",
		"fuelFraction": 0.07,
	}


func _on_save_pressed() -> void:
	if not ShipCalc.can_edit(design):
		return
	var id := String(design.get("id", ""))
	if id == "":
		id = "player_%d" % int(Time.get_unix_time_from_system())
		design["id"] = id
	# 기본 제공 설계(res://)를 수정해 저장하면 새 id 로 user:// 에 저장한다.
	if current_path.begins_with("res://"):
		id = "player_%d" % int(Time.get_unix_time_from_system())
		design["id"] = id
		current_path = ""
	DirAccess.make_dir_recursive_absolute(USER_DESIGN_DIR)
	var path := USER_DESIGN_DIR.path_join("%s.json" % id)
	var f := FileAccess.open(path, FileAccess.WRITE)
	if f == null:
		status_note = "저장 실패: %s" % path
		_update_status()
		return
	f.store_string(JSON.stringify(design, "  "))
	f.close()
	status_note = "설계 불가 상태로 저장됨" if not bool(result.get("valid", false)) else "저장됨: %s" % path
	_reload_keep_current(id)


## 저장 후 목록을 다시 읽고 같은 id 를 선택한다.
func _reload_keep_current(id: String) -> void:
	_load_design_list()
	var idx := design_ids.find(id)
	if idx >= 0:
		design_dropdown.select(idx)
		current_path = String(design_paths[idx])
	_update_status()


# ---------------------------------------------------------------------------
# 입력 갱신
# ---------------------------------------------------------------------------

func _refresh_all() -> void:
	_updating = true
	_refresh_header()
	_refresh_hull()
	_refresh_machinery()
	_refresh_armor()
	_refresh_weapons()
	_refresh_fire_control()
	_refresh_fuel()
	_updating = false
	_apply_editability()
	_recompute()


func _refresh_header() -> void:
	name_edit.text = String(design.get("name", ""))
	var special: Variant = design.get("special", null)
	var special_text := "없음"
	if special is String:
		special_text = "%s(%s)" % [SPECIAL_LABELS.get(special, special), special]
	meta_label.text = "국가 %s · T%d · 함종 %s · 특별 %s" % [
		String(design.get("nation", "")), int(design.get("tier", 0)),
		String(design.get("shipClass", "")), special_text]
	var locked := not ShipCalc.can_edit(design)
	banner.visible = locked
	banner.text = "특별 군함 — 열람 전용 (%s)" % special_text
	save_button.visible = not locked


func _hull() -> Dictionary:
	return db.hulls.get(String(design.get("hull", {}).get("templateId", "")), {})


func _refresh_hull() -> void:
	var hull := _hull()
	var ids := DesignerOptions.hull_options(db, design)
	var labels: Array = []
	for id in ids:
		labels.append(String(db.hulls[id].get("name", id)))
	var current := String(design.get("hull", {}).get("templateId", ""))
	labels = DesignerOptions.with_current(labels, current, db.hulls)
	_fill_dropdown(hull_dropdown, labels, ids, current)

	var lr: Array = hull.get("lengthScaleRange", [1.0, 1.0])
	var br: Array = hull.get("beamScaleRange", [1.0, 1.0])
	var base_l := float(hull.get("length", 1.0))
	var base_b := float(hull.get("beam", 1.0))
	length_slider.min_value = snappedf(base_l * float(lr[0]), 0.5)
	length_slider.max_value = snappedf(base_l * float(lr[1]), 0.5)
	length_spin.min_value = length_slider.min_value
	length_spin.max_value = length_slider.max_value
	beam_slider.min_value = snappedf(base_b * float(br[0]), 0.1)
	beam_slider.max_value = snappedf(base_b * float(br[1]), 0.1)
	beam_spin.min_value = beam_slider.min_value
	beam_spin.max_value = beam_slider.max_value

	var length := float(design.get("hull", {}).get("length", base_l))
	var beam := float(design.get("hull", {}).get("beam", base_b))
	length_slider.set_value_no_signal(length)
	length_spin.set_value_no_signal(length)
	beam_slider.set_value_no_signal(beam)
	beam_spin.set_value_no_signal(beam)
	draught_label.text = "%.2f m (템플릿 고정)" % float(design.get("hull", {}).get("draught", 0.0))


func _refresh_machinery() -> void:
	var ids := DesignerOptions.machinery_options(db, design)
	var labels: Array = []
	for id in ids:
		labels.append(String(db.machinery[id].get("name", id)))
	var current := String(design.get("machinery", {}).get("partId", ""))
	labels = DesignerOptions.with_current(labels, current, db.machinery)
	_fill_dropdown(machinery_dropdown, labels, ids, current)

	var m: Dictionary = db.machinery.get(current, {})
	var max_shp := float(m.get("maxShp", 1000))
	power_slider.min_value = 1000.0
	power_slider.max_value = maxf(max_shp, 1000.0)
	power_spin.min_value = 1000.0
	power_spin.max_value = maxf(max_shp, 1000.0)
	var shp := float(design.get("machinery", {}).get("powerShp", 1000))
	power_slider.set_value_no_signal(shp)
	power_spin.set_value_no_signal(shp)


func _refresh_armor() -> void:
	var max_mm := float(_hull().get("maxArmorMm", 0))
	var armor: Dictionary = design.get("armor", {})
	for key in ShipCalc.ARMOR_KEYS:
		var spin: SpinBox = armor_spins[key]
		spin.min_value = 0.0
		spin.max_value = max_mm
		spin.set_value_no_signal(float(armor.get(key, 0)))


func _refresh_weapons() -> void:
	for child in weapons_box.get_children():
		child.queue_free()
	weapon_rows.clear()

	var hull := _hull()
	var slots: Array = hull.get("slots", [])
	var by_slot := {}
	for w in design.get("weapons", []):
		by_slot[String(w.get("slot", ""))] = w

	for slot in slots:
		var sid := String(slot.get("id", ""))
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 6)

		var title := Label.new()
		title.text = "%s (%s)" % [sid, SLOT_TYPE_LABELS.get(String(slot.get("type", "")), slot.get("type", ""))]
		title.custom_minimum_size = Vector2(150, 0)
		row.add_child(title)

		var gun_ids := DesignerOptions.gun_options(db, design, slot)
		var gun_labels: Array = [EMPTY_LABEL]
		for id in gun_ids:
			gun_labels.append(String(db.guns[id].get("name", id)))
		var current := ""
		if by_slot.has(sid):
			current = String(by_slot[sid].get("partId", ""))
		if current != "" and not gun_ids.has(current):
			gun_labels = DesignerOptions.with_current(gun_labels, current, db.guns)
		# 드롭다운 항목 -> 부품 ID (첫 항목 "비어 있음" 은 "")
		var gun_map: Array = [""]
		gun_map.append_array(gun_ids)
		if current != "" and not gun_ids.has(current):
			gun_map.append(current)

		var gun_dd := OptionButton.new()
		gun_dd.custom_minimum_size = Vector2(260, 0)
		for l in gun_labels:
			gun_dd.add_item(String(l))
		var gun_idx := 0
		if current != "":
			gun_idx = gun_map.find(current)
			if gun_idx < 0:
				gun_idx = gun_labels.size() - 1
		gun_dd.select(gun_idx)
		gun_dd.item_selected.connect(_on_weapon_gun_selected.bind(sid))
		row.add_child(gun_dd)

		var barrel_dd := OptionButton.new()
		barrel_dd.custom_minimum_size = Vector2(90, 0)
		barrel_dd.item_selected.connect(_on_weapon_barrels_selected.bind(sid))
		row.add_child(barrel_dd)

		var pos := Label.new()
		pos.text = "x %+.2f" % float(slot.get("x", 0.0))
		pos.custom_minimum_size = Vector2(70, 0)
		row.add_child(pos)

		weapons_box.add_child(row)
		var entry := {"slot": slot, "dropdown": gun_dd, "barrels": barrel_dd,
			"ids": gun_ids, "labels": gun_labels, "map": gun_map}
		weapon_rows.append(entry)
		_refresh_barrels(entry, current, int(by_slot.get(sid, {}).get("barrels", 0)))


func _refresh_barrels(entry: Dictionary, gun_id: String, current_barrels: int) -> void:
	var dd: OptionButton = entry.barrels
	dd.clear()
	var slot: Dictionary = entry.slot
	var barrel_ids: Array = []
	if gun_id != "" and db.guns.has(gun_id):
		barrel_ids = DesignerOptions.barrel_options(db.guns[gun_id], slot)
	if barrel_ids.is_empty():
		dd.disabled = true
		dd.add_item(DASH)
		dd.select(0)
		entry["barrel_ids"] = []
		return
	dd.disabled = false
	for n in barrel_ids:
		dd.add_item("%d문" % int(n))
	var idx := barrel_ids.find(current_barrels)
	if idx < 0:
		idx = 0
	dd.select(idx)
	entry["barrel_ids"] = barrel_ids


func _refresh_fire_control() -> void:
	var ids := DesignerOptions.fire_control_options(db, design)
	var labels: Array = []
	for id in ids:
		labels.append(String(db.fireControl[id].get("name", id)))
	var current := String(design.get("fireControl", ""))
	labels = DesignerOptions.with_current(labels, current, db.fireControl)
	_fill_dropdown(fc_dropdown, labels, ids, current)


func _refresh_fuel() -> void:
	var r: Array = _hull().get("fuelFractionRange", [0.0, 1.0])
	fuel_slider.min_value = float(r[0])
	fuel_slider.max_value = float(r[1])
	var ff := float(design.get("fuelFraction", 0.0))
	fuel_slider.set_value_no_signal(ff)
	fuel_label.text = "%.1f%%" % (ff * 100.0)


## 드롭다운을 (표시 문자열, ID) 목록으로 채우고 현재 값을 선택한다.
func _fill_dropdown(dd: OptionButton, labels: Array, ids: Array, current: String) -> void:
	dd.clear()
	if labels.is_empty():
		dd.add_item(DASH)
		dd.select(0)
		return
	for l in labels:
		dd.add_item(String(l))
	var idx := ids.find(current)
	if idx < 0:
		# 규칙 밖 부품은 목록 끝에 "(사용 불가)" 로 붙어 있다.
		idx = labels.size() - 1 if labels.size() > ids.size() else 0
	dd.select(maxi(idx, 0))


func _apply_editability() -> void:
	var editable := ShipCalc.can_edit(design)
	for c in [design_dropdown, new_button, hull_dropdown, length_slider, length_spin,
			beam_slider, beam_spin, machinery_dropdown, power_slider, power_spin,
			fc_dropdown, fuel_slider]:
		(c as Control).set("disabled", not editable)
	name_edit.editable = editable
	for key in armor_spins:
		(armor_spins[key] as SpinBox).editable = editable
	for entry in weapon_rows:
		(entry.dropdown as OptionButton).disabled = not editable
		(entry.barrels as OptionButton).disabled = not editable or (entry.barrel_ids as Array).is_empty()
	save_button.visible = editable


# ---------------------------------------------------------------------------
# 입력 시그널
# ---------------------------------------------------------------------------

func _on_name_changed(text: String) -> void:
	if _updating:
		return
	design["name"] = text
	_refresh_header()


func _on_hull_selected(index: int) -> void:
	if _updating:
		return
	var ids := DesignerOptions.hull_options(db, design)
	var current := String(design.get("hull", {}).get("templateId", ""))
	var labels: Array = []
	for id in ids:
		labels.append(String(db.hulls[id].get("name", id)))
	labels = DesignerOptions.with_current(labels, current, db.hulls)
	var new_id := DesignerOptions.id_from_label(hull_dropdown.get_item_text(index), labels, ids)
	if new_id == current or not db.hulls.has(new_id):
		return
	var hull: Dictionary = db.hulls[new_id]
	design["hull"] = {
		"templateId": new_id,
		"length": float(hull.length),
		"beam": float(hull.beam),
		"draught": float(hull.draught),
	}
	design["shipClass"] = String(hull.shipClass)
	# 새 선체에 없는 슬롯의 무장은 지운다.
	var slot_ids := {}
	for s in hull.slots:
		slot_ids[String(s.id)] = true
	var kept: Array = []
	for w in design.get("weapons", []):
		if slot_ids.has(String(w.get("slot", ""))):
			kept.append(w)
	design["weapons"] = kept
	# 장갑 상한이 줄면 값을 맞춘다.
	var max_mm := float(hull.maxArmorMm)
	for k in ShipCalc.ARMOR_KEYS:
		design.armor[k] = minf(float(design.armor.get(k, 0)), max_mm)
	# 연료 범위 밖이면 범위 안으로 맞춘다.
	var fr: Array = hull.fuelFractionRange
	design["fuelFraction"] = clampf(float(design.get("fuelFraction", fr[0])), float(fr[0]), float(fr[1]))
	# 기관이 새 함종에 쓸 수 없으면 첫 선택지로 바꾼다.
	var mach_ids := DesignerOptions.machinery_options(db, design)
	if not mach_ids.is_empty() and not mach_ids.has(String(design.machinery.partId)):
		design.machinery.partId = String(mach_ids[0])
		design.machinery.powerShp = minf(float(design.machinery.powerShp),
			float(db.machinery[mach_ids[0]].maxShp))
	_refresh_all()


func _on_length_changed(value: float) -> void:
	if _updating:
		return
	design.hull.length = snappedf(value, 0.5)
	_updating = true
	length_slider.set_value_no_signal(design.hull.length)
	length_spin.set_value_no_signal(design.hull.length)
	_updating = false
	_recompute()


func _on_beam_changed(value: float) -> void:
	if _updating:
		return
	design.hull.beam = snappedf(value, 0.1)
	_updating = true
	beam_slider.set_value_no_signal(design.hull.beam)
	beam_spin.set_value_no_signal(design.hull.beam)
	_updating = false
	_recompute()


func _on_machinery_selected(index: int) -> void:
	if _updating:
		return
	var ids := DesignerOptions.machinery_options(db, design)
	var current := String(design.get("machinery", {}).get("partId", ""))
	var labels: Array = []
	for id in ids:
		labels.append(String(db.machinery[id].get("name", id)))
	labels = DesignerOptions.with_current(labels, current, db.machinery)
	var new_id := DesignerOptions.id_from_label(machinery_dropdown.get_item_text(index), labels, ids)
	if new_id == current or not db.machinery.has(new_id):
		return
	design.machinery.partId = new_id
	design.machinery.powerShp = minf(float(design.machinery.powerShp),
		float(db.machinery[new_id].maxShp))
	_refresh_all()


func _on_power_changed(value: float) -> void:
	if _updating:
		return
	design.machinery.powerShp = snappedf(value, 500.0)
	_updating = true
	power_slider.set_value_no_signal(design.machinery.powerShp)
	power_spin.set_value_no_signal(design.machinery.powerShp)
	_updating = false
	_recompute()


func _on_armor_changed(value: float, key: String) -> void:
	if _updating:
		return
	design.armor[key] = int(round(value))
	_recompute()


func _on_weapon_gun_selected(index: int, slot_id: String) -> void:
	if _updating:
		return
	var entry := _weapon_entry(slot_id)
	if entry.is_empty():
		return
	var gun_map: Array = entry.map
	if index < 0 or index >= gun_map.size():
		return
	_set_weapon(slot_id, String(gun_map[index]), 0)
	_refresh_all()


func _on_weapon_barrels_selected(index: int, slot_id: String) -> void:
	if _updating:
		return
	var entry := _weapon_entry(slot_id)
	if entry.is_empty():
		return
	var barrel_ids: Array = entry.barrel_ids
	if index < 0 or index >= barrel_ids.size():
		return
	var w := _find_weapon(slot_id)
	if w.is_empty():
		return
	w["barrels"] = int(barrel_ids[index])
	_recompute()


func _on_fc_selected(index: int) -> void:
	if _updating:
		return
	var ids := DesignerOptions.fire_control_options(db, design)
	var current := String(design.get("fireControl", ""))
	var labels: Array = []
	for id in ids:
		labels.append(String(db.fireControl[id].get("name", id)))
	labels = DesignerOptions.with_current(labels, current, db.fireControl)
	var new_id := DesignerOptions.id_from_label(fc_dropdown.get_item_text(index), labels, ids)
	if new_id == current or not db.fireControl.has(new_id):
		return
	design["fireControl"] = new_id
	_recompute()


func _on_fuel_changed(value: float) -> void:
	if _updating:
		return
	design["fuelFraction"] = snappedf(value, 0.005)
	_updating = true
	fuel_slider.set_value_no_signal(design.fuelFraction)
	fuel_label.text = "%.1f%%" % (float(design.fuelFraction) * 100.0)
	_updating = false
	_recompute()


func _weapon_entry(slot_id: String) -> Dictionary:
	for e in weapon_rows:
		if String(e.slot.get("id", "")) == slot_id:
			return e
	return {}


func _find_weapon(slot_id: String) -> Dictionary:
	for w in design.get("weapons", []):
		if String(w.get("slot", "")) == slot_id:
			return w
	return {}


## 슬롯 무장을 바꾼다. gun_id 가 비면 슬롯을 비운다.
func _set_weapon(slot_id: String, gun_id: String, barrels: int) -> void:
	var weapons: Array = design.get("weapons", [])
	var out: Array = []
	for w in weapons:
		if String(w.get("slot", "")) != slot_id:
			out.append(w)
	if gun_id != "":
		var n := barrels
		if n <= 0:
			var slot := {}
			for s in _hull().get("slots", []):
				if String(s.id) == slot_id:
					slot = s
			var opts := DesignerOptions.barrel_options(db.guns[gun_id], slot)
			n = int(opts[0]) if not opts.is_empty() else 1
		out.append({"slot": slot_id, "partId": gun_id, "barrels": n})
	design["weapons"] = out


# ---------------------------------------------------------------------------
# 계산 / 결과 표시
# ---------------------------------------------------------------------------

func _recompute() -> void:
	result = ShipCalc.compute_all(design, db)
	_update_results()
	_update_status()


func _update_results() -> void:
	var has_weights := result.has("weights")
	var groups: Dictionary = result.get("weights", {}).get("groups", {})
	var fractions: Dictionary = result.get("weights", {}).get("fractions", {})
	var total := float(result.get("weights", {}).get("total", 0.0))

	for g in GROUP_ORDER:
		var w := float(groups.get(g, 0.0))
		var frac := float(fractions.get(g, 0.0))
		var text := "%s t (%.1f%%)" % [_fmt(w), frac * 100.0] if has_weights else "%s t (%s)" % [DASH, DASH]
		if has_weights and GROUP_NORMAL_RANGE.has(g):
			var r: Array = GROUP_NORMAL_RANGE[g]
			if frac < float(r[0]) or frac > float(r[1]):
				text += "  [참고: 일반 %d~%d%%]" % [int(float(r[0]) * 100.0), int(float(r[1]) * 100.0)]
		(weight_rows[g] as Label).text = text

	if has_weights:
		var disp := float(result.weights.designDisplacement)
		weight_total_label.text = "총중량 %s t / 설계 배수량 %s t / 과적률 %+.1f%%" % [
			_fmt(total), _fmt(disp), float(result.weights.overloadRatio) * 100.0]
	else:
		weight_total_label.text = "총중량 %s / 설계 배수량 %s / 과적률 %s" % [DASH, DASH, DASH]
	weight_bar.queue_redraw()

	if result.has("performance"):
		var p: Dictionary = result.performance
		perf_label.text = "최고 속도 %.1f kn · 선회 지름 %s m · 전속까지 %.0f s" % [
			float(p.maxSpeedKn), _fmt(float(p.tacticalDiameterM)), float(p.timeToFullSpeedSec)]
	else:
		perf_label.text = "최고 속도 %s · 선회 지름 %s · 전속까지 %s" % [DASH, DASH, DASH]

	if result.has("stability"):
		var s: Dictionary = result.stability
		var status := String(s.status)
		stability_label.text = "GM %.2f m · GM/폭 %.3f · 상태 %s · 벨트 상단 수면 위 %.2f m" % [
			float(s.gm), float(s.gmRatio), STABILITY_LABELS.get(status, status), float(s.beltFreeboard)]
		stability_label.add_theme_color_override("font_color",
			STABILITY_COLORS.get(status, Color.WHITE))
	else:
		stability_label.text = "GM %s · GM/폭 %s · 상태 %s · 벨트 상단 수면 위 %s" % [DASH, DASH, DASH, DASH]
		stability_label.add_theme_color_override("font_color", STABILITY_COLORS["invalid"])

	if result.has("combat"):
		var c: Dictionary = result.combat
		combat_label.text = "주포 %d문(%dmm) · 일제사격 %s kg · 사거리 %.1f km · 장전 %.0f s · 부포 %d문 · 어뢰 발사관 %d" % [
			int(c.mainBarrels), int(c.mainCaliberMm), _fmt(float(c.broadsideKg)),
			float(c.mainRangeKm), float(c.mainReloadSec), int(c.secondaryBarrels), int(c.torpedoTubes)]
	else:
		combat_label.text = "주포 %s · 일제사격 %s · 사거리 %s · 장전 %s · 부포 %s · 어뢰 %s" % [
			DASH, DASH, DASH, DASH, DASH, DASH]

	if result.has("cost"):
		cost_label.text = "건조 비용 %s · 건조 기간 %.1f개월" % [
			_fmt(float(result.cost.cost)), float(result.cost.buildMonths)]
	else:
		cost_label.text = "건조 비용 %s · 건조 기간 %s" % [DASH, DASH]

	var lines: Array = []
	for e in result.get("errors", []):
		lines.append("[color=#ff6b6b]오류: %s[/color]" % String(e.message))
	for w in result.get("warnings", []):
		lines.append("[color=#ffd166]경고: %s[/color]" % String(w.message))
	if lines.is_empty():
		lines.append("문제 없음")
	issues_label.text = "\n".join(lines)


func _update_status() -> void:
	var errors: Array = result.get("errors", [])
	var ok := bool(result.get("valid", false))
	var text := "설계 가능" if ok else "설계 불가 (오류 %d건)" % errors.size()
	if status_note != "":
		text += " · " + status_note
	status_label.text = text
	status_label.add_theme_color_override("font_color",
		Color(0.55, 0.85, 0.55) if ok else Color(0.95, 0.40, 0.40))


func _on_weight_bar_draw() -> void:
	var rect := Rect2(Vector2.ZERO, weight_bar.size)
	weight_bar.draw_rect(rect, Color(0.15, 0.15, 0.18))
	if not result.has("weights"):
		return
	var groups: Dictionary = result.weights.groups
	var total := float(result.weights.total)
	var disp := float(result.weights.designDisplacement)
	if total <= 0.0:
		return
	# 막대 전체 길이는 총중량과 설계 배수량 중 큰 쪽을 기준으로 한다.
	var scale := maxf(total, disp)
	var x := 0.0
	for g in GROUP_ORDER:
		var w := float(groups.get(g, 0.0))
		if w <= 0.0:
			continue
		var w_px := rect.size.x * (w / scale)
		weight_bar.draw_rect(Rect2(Vector2(x, 0), Vector2(w_px, rect.size.y)), GROUP_COLORS[g])
		x += w_px
	# 설계 배수량 위치에 세로선
	var dx := rect.size.x * (disp / scale)
	weight_bar.draw_line(Vector2(dx, 0), Vector2(dx, rect.size.y), Color(1, 1, 1), 2.0)


## 천 단위 구분 기호를 넣은 정수 문자열.
func _fmt(v: float) -> String:
	var n := int(round(v))
	var neg := n < 0
	var s := str(absi(n))
	var out := ""
	var count := 0
	for i in range(s.length() - 1, -1, -1):
		out = s[i] + out
		count += 1
		if count % 3 == 0 and i > 0:
			out = "," + out
	return ("-" if neg else "") + out
