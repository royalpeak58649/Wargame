extends SceneTree
## 전투 탄도·관통 계산 모듈 단위 테스트.
## 실행: godot --headless --path . --script res://tests/run_combat_tests.gd
## 실패가 하나라도 있으면 종료 코드 1.

const Ballistics = preload("res://scripts/combat/ballistics.gd")
const PartsDB = preload("res://scripts/core/parts_db.gd")

var db: Dictionary
var gun_381: Dictionary
var gun_152: Dictionary
var fc: Dictionary
var passed := 0
var failed := 0


func _init() -> void:
	db = PartsDB.load_many(["res://data/parts_t2.json"])
	gun_381 = db.guns["gun_381mm_t2"]
	gun_152 = db.guns["gun_152mm_t2"]
	fc = db.fireControl["fc_t2_director"]

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


# --- 탄도 -----------------------------------------------------------------

func test_velocity_decays() -> void:
	var v0: float = Ballistics.remaining_velocity(gun_381, 0.0)
	var v10: float = Ballistics.remaining_velocity(gun_381, 10.0)
	var v20: float = Ballistics.remaining_velocity(gun_381, 20.0)
	in_range(v0, 731.9, 732.1, "381mm 0km 잔존 속도(m/s)")
	check(v10 < v0, "10km 속도가 0km보다 낮아야 함 (%.1f → %.1f)" % [v0, v10])
	check(v20 < v10, "20km 속도가 10km보다 낮아야 함 (%.1f → %.1f)" % [v10, v20])


func test_heavy_shell_keeps_velocity() -> void:
	var keep_381: float = Ballistics.remaining_velocity(gun_381, 10.0) / float(gun_381.muzzleVelocity)
	var keep_152: float = Ballistics.remaining_velocity(gun_152, 10.0) / float(gun_152.muzzleVelocity)
	check(keep_381 > keep_152, "10km 속도 보존율 381mm > 152mm (%.3f vs %.3f)" % [keep_381, keep_152])


func test_pen_381_at_10km() -> void:
	in_range(Ballistics.penetration_mm(gun_381, 10.0), 280.0, 380.0, "381mm 10km 관통력(mm)")


func test_pen_decreases_with_range() -> void:
	var p0: float = Ballistics.penetration_mm(gun_381, 0.0)
	var p10: float = Ballistics.penetration_mm(gun_381, 10.0)
	var p20: float = Ballistics.penetration_mm(gun_381, 20.0)
	check(p0 > p10, "관통력 0km > 10km (%.1f vs %.1f)" % [p0, p10])
	check(p10 > p20, "관통력 10km > 20km (%.1f vs %.1f)" % [p10, p20])


func test_fall_angle_grows() -> void:
	var f0: float = Ballistics.fall_angle_deg(gun_381, 0.0)
	var f5: float = Ballistics.fall_angle_deg(gun_381, 5.0)
	var f10: float = Ballistics.fall_angle_deg(gun_381, 10.0)
	var fmax: float = Ballistics.fall_angle_deg(gun_381, float(gun_381.maxRangeKm))
	in_range(f0, -0.001, 0.001, "0km 낙각(°)")
	check(f5 > f0 and f10 > f5, "낙각은 거리에 따라 증가 (%.2f → %.2f → %.2f)" % [f0, f5, f10])
	in_range(fmax, 29.999, 30.001, "최대 사거리 낙각(°)")


# --- 판정 -----------------------------------------------------------------

func test_belt_vs_deck_by_range() -> void:
	var belt_5: Dictionary = Ballistics.resolve_hit(gun_381, 5.0, "belt", 300.0, 0.0)
	check(String(belt_5.result) == "pen", "5km 벨트 300mm 관통: %s (관통 %.1f / 유효 %.1f)" % [belt_5.result, belt_5.penMm, belt_5.effArmorMm])
	var deck_5: Dictionary = Ballistics.resolve_hit(gun_381, 5.0, "deck", 75.0, 0.0)
	var deck_20: Dictionary = Ballistics.resolve_hit(gun_381, 20.0, "deck", 75.0, 0.0)
	check(float(deck_20.effArmorMm) < float(deck_5.effArmorMm),
		"20km 갑판 유효 장갑 < 5km 갑판 유효 장갑 (%.1f vs %.1f)" % [deck_20.effArmorMm, deck_5.effArmorMm])


func test_ricochet() -> void:
	var deck_5: Dictionary = Ballistics.resolve_hit(gun_381, 5.0, "deck", 75.0, 0.0)
	check(String(deck_5.result) == "ricochet", "5km 갑판(낙각 %.1f°)은 도탄: %s" % [deck_5.fallDeg, deck_5.result])
	var belt_75: Dictionary = Ballistics.resolve_hit(gun_381, 10.0, "belt", 300.0, 75.0)
	check(String(belt_75.result) == "ricochet", "벨트 수평 75°(입사각 %.1f°)는 도탄: %s" % [belt_75.obliquityDeg, belt_75.result])


func test_zero_armor_is_pen() -> void:
	var r: Dictionary = Ballistics.resolve_hit(gun_381, 20.0, "deck", 0.0, 0.0)
	check(String(r.result) == "pen", "장갑 0mm 는 관통: %s" % r.result)


# --- 사격 통제 -------------------------------------------------------------

func test_spread_converges() -> void:
	var s0: float = Ballistics.salvo_spread_m(fc, 15.0, 0, 1.0)
	var s3: float = Ballistics.salvo_spread_m(fc, 15.0, 3, 1.0)
	var s10: float = Ballistics.salvo_spread_m(fc, 15.0, 10, 1.0)
	var s50: float = Ballistics.salvo_spread_m(fc, 15.0, 50, 1.0)
	check(s0 > s3, "분산 n=0 > n=3 (%.2f vs %.2f)" % [s0, s3])
	check(s3 > s10, "분산 n=3 > n=10 (%.2f vs %.2f)" % [s3, s10])
	check(absf(s50 - float(fc.minSpreadM)) < 1.0, "n=50 분산이 minSpreadM 에 수렴 (%.3f vs %.1f)" % [s50, fc.minSpreadM])


func test_bad_stability_spreads_more() -> void:
	var good: float = Ballistics.salvo_spread_m(fc, 15.0, 3, 1.0)
	var bad: float = Ballistics.salvo_spread_m(fc, 15.0, 3, 0.9)
	check(bad > good, "명중률 배수 0.9 는 분산이 더 큼 (%.2f vs %.2f)" % [bad, good])
