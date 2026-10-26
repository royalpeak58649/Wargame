extends SceneTree
## 공용 계산 모듈 단위 테스트.
## 실행: godot --headless --path . --script res://tests/run_tests.gd
## 실패가 하나라도 있으면 종료 코드 1.

const ShipCalc = preload("res://scripts/core/ship_calc.gd")
const PartsDB = preload("res://scripts/core/parts_db.gd")
const DesignerOptions = preload("res://scripts/ui/designer_options.gd")

var db: Dictionary
var qe: Dictionary
var vw: Dictionary
var player: Dictionary
var passed := 0
var failed := 0


func _init() -> void:
	db = PartsDB.load_db("res://data/parts_t2.json")
	qe = PartsDB.load_design("res://data/designs/uk_bb_queen_elizabeth.json")
	vw = PartsDB.load_design("res://data/designs/uk_dd_vw.json")
	player = PartsDB.load_design("res://data/designs/player_bb_fast.json")

	for m in get_method_list():
		var name: String = m.name
		if name.begins_with("test_"):
			var before := failed
			call(name)
			print("%s %s" % ["PASS" if failed == before else "FAIL", name])

	print("\n%d개 검사 통과, %d개 실패" % [passed, failed])
	quit(1 if failed > 0 else 0)


# --- 도우미 ---------------------------------------------------------------

func check(cond: bool, msg: String) -> void:
	if cond:
		passed += 1
	else:
		failed += 1
		print("    실패: " + msg)


func in_range(v: float, lo: float, hi: float, msg: String) -> void:
	check(v >= lo and v <= hi, "%s = %.3f (기대 %.3f~%.3f)" % [msg, v, lo, hi])


func has_code(issues: Array, code: String) -> bool:
	for i in issues:
		if i.code == code:
			return true
	return false


func variant(base: Dictionary) -> Dictionary:
	return base.duplicate(true)


# --- 실제 함선 수치 재현 (보정 기준) -----------------------------------------

func test_queen_elizabeth_matches_history() -> void:
	var r := ShipCalc.compute_all(qe, db)
	check(r.valid, "QE는 설계 가능해야 함: %s" % [r.errors])
	in_range(r.performance.maxSpeedKn, 23.5, 25.5, "QE 최고 속도(kn), 실제 약 24")
	in_range(r.weights.total, 28000, 33500, "QE 배수량(t), 실제 약 32,500")
	in_range(r.stability.gm, 1.2, 2.5, "QE GM(m), 실제 약 1.9")
	check(r.combat.mainBarrels == 8, "QE 주포 8문")


func test_vw_destroyer_matches_history() -> void:
	var r := ShipCalc.compute_all(vw, db)
	check(r.valid, "V&W는 설계 가능해야 함: %s" % [r.errors])
	in_range(r.performance.maxSpeedKn, 32.0, 36.0, "V&W 최고 속도(kn), 실제 약 34")
	in_range(r.weights.total, 1300, 1650, "V&W 배수량(t), 실제 약 1,500")
	check(r.combat.torpedoTubes == 6, "V&W 어뢰 6문")


# --- 계산 성질 ----------------------------------------------------------------

func test_deterministic() -> void:
	var a := ShipCalc.compute_all(qe, db)
	var b := ShipCalc.compute_all(qe, db)
	check(a.performance.maxSpeedKn == b.performance.maxSpeedKn and a.stability.gm == b.stability.gm,
		"같은 입력은 같은 결과")


func test_weights_and_stability_share_items() -> void:
	var w := ShipCalc.compute_weights(qe, db)
	var group_sum := 0.0
	for g in w.groups:
		group_sum += float(w.groups[g])
	in_range(group_sum - w.total, -0.001, 0.001, "그룹 합 = 총중량")
	var armor_sum := 0.0
	for k in w.armor:
		armor_sum += float(w.armor[k])
	in_range(armor_sum - w.groups.armor, -0.001, 0.001, "장갑 구역 합 = 장갑 그룹")


func test_longer_hull_is_faster() -> void:
	var longer := variant(qe)
	longer.hull.length = 205.0
	var v0: float = ShipCalc.compute_performance(qe, db).maxSpeedKn
	var v1: float = ShipCalc.compute_performance(longer, db).maxSpeedKn
	check(v1 > v0, "같은 출력이면 긴 선체가 빨라야 함 (%.2f → %.2f)" % [v0, v1])


func test_more_armor_is_slower() -> void:
	var heavy := variant(qe)
	heavy.armor.deck = 150
	var v0: float = ShipCalc.compute_performance(qe, db).maxSpeedKn
	var v1: float = ShipCalc.compute_performance(heavy, db).maxSpeedKn
	check(v1 < v0, "장갑을 늘리면 느려져야 함 (%.2f → %.2f)" % [v0, v1])


func test_raising_weight_lowers_gm() -> void:
	var tall := variant(qe)
	tall.weapons.append({"slot": "main_Q", "partId": "gun_381mm_t2", "barrels": 2})
	tall.armor.beltMain = 250  # 무게는 비슷하게 맞추고 상부로 옮김
	var gm0: float = ShipCalc.compute_stability(qe, db).gm
	var gm1: float = ShipCalc.compute_stability(tall, db).gm
	check(gm1 < gm0, "아래 무게를 위로 옮기면 GM이 낮아져야 함 (%.2f → %.2f)" % [gm0, gm1])


# --- 검증 규칙 ----------------------------------------------------------------

func test_overload_is_rejected() -> void:
	var d := variant(qe)
	d.weapons.append({"slot": "main_Q", "partId": "gun_381mm_t2", "barrels": 2})
	d.armor.beltMain = 350
	d.armor.beltUpper = 300
	d.armor.deck = 150
	var r := ShipCalc.compute_all(d, db)
	check(not r.valid and has_code(r.errors, "overload"), "과적 설계는 거부: %.1f%%" % [r.weights.overloadRatio * 100.0])


func test_wrong_slot_type() -> void:
	var d := variant(qe)
	d.weapons[0].partId = "gun_152mm_t2"
	var r := ShipCalc.compute_all(d, db)
	check(has_code(r.errors, "slot_type"), "주포 슬롯에 부포를 놓으면 오류")


func test_caliber_limit() -> void:
	var d := variant(vw)
	d.weapons[0].partId = "gun_381mm_t2"
	d.weapons[0].barrels = 2
	var r := ShipCalc.compute_all(d, db)
	check(has_code(r.errors, "slot_caliber"), "구축함 슬롯에 381mm는 오류")


func test_turret_barrel_options() -> void:
	var d := variant(qe)
	d.weapons[0].barrels = 1
	var r := ShipCalc.compute_all(d, db)
	check(has_code(r.errors, "mount_barrels"), "단장 381mm 포탑은 없음")


func test_nation_restriction() -> void:
	var d := variant(player)
	d.nation = "DE"
	var r := ShipCalc.compute_all(d, db)
	check(has_code(r.errors, "nation_part"), "독일 설계에 영국 전용 343mm는 오류")
	for w in d.weapons:
		if w.partId == "gun_343mm_t2":
			w.partId = "gun_305mm_t2"
	r = ShipCalc.compute_all(d, db)
	check(not has_code(r.errors, "nation_part"), "독일 30.5cm 포로 바꾸면 국가 오류 없음: %s" % [r.errors])


func test_machinery_class_restriction() -> void:
	var d := variant(qe)
	d.machinery.partId = "turbine_t2_oil_light"
	var r := ShipCalc.compute_all(d, db)
	check(has_code(r.errors, "machinery_class"), "전함에 구축함 기관은 오류")


func test_hull_scale_range() -> void:
	var d := variant(qe)
	d.hull.length = 230.0
	var r := ShipCalc.compute_all(d, db)
	check(has_code(r.errors, "hull_length"), "길이 +21%는 범위 밖")


func test_stability_failure() -> void:
	# 깊이를 크게 키운 시험용 선체: 무게중심이 높아져 GM이 무너져야 한다.
	var test_db := db.duplicate(true)
	test_db.hulls.hull_bb_t2_a.depth = 26.0
	var r := ShipCalc.compute_all(qe, test_db)
	check(has_code(r.errors, "stability_fail"), "GM %.2fm 이면 설계 불가" % r.stability.gm)


func test_underweight_gets_ballast() -> void:
	var d := variant(player)
	var r := ShipCalc.compute_all(d, db)
	check(r.weights.groups.ballast > 0.0, "가벼운 설계는 밸러스트 적재")
	in_range(r.weights.overloadRatio, -0.1001, -0.0999, "밸러스트 후 무게 = 설계 배수량의 90%")
	check(has_code(r.warnings, "ballast"), "밸러스트 경고 표시")


# --- 특별 군함과 비용 ----------------------------------------------------------

func test_special_ships_locked() -> void:
	check(not ShipCalc.can_edit(qe), "특별 군함(QE)은 편집 불가")
	check(ShipCalc.can_edit(player), "플레이어 설계는 편집 가능")


func test_special_cost_rules() -> void:
	var as_player := variant(qe)
	as_player.special = null
	as_player.locked = false
	var c_player: Dictionary = ShipCalc.compute_cost(as_player, db)
	var c_build: Dictionary = ShipCalc.compute_cost(qe, db)
	check(c_build.cost > c_player.cost, "건조형 특별 군함은 더 비쌈")
	check(c_build.buildMonths < c_player.buildMonths, "건조형 특별 군함은 더 빨리 건조")
	var reward := variant(qe)
	reward.special = "reward"
	check(ShipCalc.compute_cost(reward, db).cost == 0.0, "보상형은 건조 비용 0")


# --- 설계 화면 선택지 (designer_options.gd) ------------------------------------

func _options_db() -> Dictionary:
	return PartsDB.load_many(["res://data/parts_t2.json"])


func _slot(hull_id: String, slot_id: String) -> Dictionary:
	var odb := _options_db()
	for s in odb.hulls[hull_id].slots:
		if String(s.id) == slot_id:
			return s
	return {}


func test_options_uk_bb_main_guns() -> void:
	var odb := _options_db()
	var d := {"nation": "UK", "tier": 2, "shipClass": "BB"}
	var opts := DesignerOptions.gun_options(odb, d, _slot("hull_bb_t2_a", "main_A"))
	check(opts.has("gun_381mm_t2"), "영국 T2 전함 주포에 381mm 포함: %s" % [opts])
	check(opts.has("gun_343mm_t2"), "영국 T2 전함 주포에 343mm 포함: %s" % [opts])
	# 102mm 는 type main 이고 구경 102 ≤ 381, nations 없음 → 실제로 포함된다.
	check(opts.has("gun_102mm_t2"), "102mm 는 type main 이라 포함: %s" % [opts])
	check(not opts.has("gun_305mm_t2"), "305mm 는 독일 전용이라 제외: %s" % [opts])
	check(not opts.has("gun_152mm_t2"), "152mm 는 부포라 제외: %s" % [opts])


func test_options_torpedo_barrels() -> void:
	var odb := _options_db()
	var opts := DesignerOptions.barrel_options(odb.guns["torp_533mm_t2"], _slot("hull_dd_t2_a", "torp_1"))
	check(opts == [2, 3], "533mm 어뢰 발사관은 2, 3문: %s" % [opts])


func test_options_secondary_barrels() -> void:
	var odb := _options_db()
	var opts := DesignerOptions.barrel_options(odb.guns["gun_152mm_t2"], _slot("hull_bb_t2_a", "sec_P"))
	check(opts == [1, 2, 3, 4, 5, 6, 7, 8], "152mm 부포는 1~8문: %s" % [opts])


func test_options_dd_machinery() -> void:
	var odb := _options_db()
	var d := {"nation": "UK", "tier": 2, "shipClass": "DD"}
	var opts := DesignerOptions.machinery_options(odb, d)
	check(opts == ["turbine_t2_oil_light"], "구축함 기관은 경량 터빈만: %s" % [opts])


func test_options_tier_filter() -> void:
	var odb := _options_db()
	var d := {"nation": "UK", "tier": 1, "shipClass": "BB"}
	var opts := DesignerOptions.gun_options(odb, d, _slot("hull_bb_t2_a", "main_A"))
	check(not opts.has("gun_381mm_t2"), "T1 설계에 T2 381mm 는 없음: %s" % [opts])
	check(opts.has("gun_343mm_t2"), "T1 설계에 T1 343mm 는 있음: %s" % [opts])
