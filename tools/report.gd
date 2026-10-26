extends SceneTree
## 설계 파일 하나를 계산해 사람이 읽기 좋게 출력한다.
## 사용법: godot --headless --path . --script res://tools/report.gd -- res://data/designs/uk_bb_queen_elizabeth.json

const ShipCalc = preload("res://scripts/core/ship_calc.gd")
const PartsDB = preload("res://scripts/core/parts_db.gd")


func _init() -> void:
	var args := OS.get_cmdline_user_args()
	var paths: Array = args if not args.is_empty() else [
		"res://data/designs/uk_bb_queen_elizabeth.json",
		"res://data/designs/uk_dd_vw.json",
		"res://data/designs/player_bb_fast.json",
	]
	var db := PartsDB.load_db("res://data/parts_t2.json")
	for p in paths:
		print_report(PartsDB.load_design(p), db)
	quit(0)


static func print_report(design: Dictionary, db: Dictionary) -> void:
	var r := ShipCalc.compute_all(design, db)
	print("")
	print("=== %s (%s, T%d, %s) ===" % [design.name, design.nation, design.tier, design.shipClass])
	print("편집 가능: %s   특별 군함: %s" % [ShipCalc.can_edit(design), design.get("special")])
	print("결과: %s" % ("설계 가능" if r.valid else "설계 불가"))
	for e in r.errors:
		print("  [오류] %s" % e.message)
	for w in r.warnings:
		print("  [경고] %s" % w.message)
	if not r.has("weights"):
		return
	var wt: Dictionary = r.weights
	print("배수량: 설계 %.0ft / 실제 %.0ft (과적 %+.1f%%), 흘수 %.2fm" % [
		wt.designDisplacement, wt.total, wt.overloadRatio * 100.0, wt.draughtActual])
	var parts: Array = []
	for g in ["structure", "armor", "machinery", "weapons", "fireControl", "fuel", "misc", "ballast"]:
		parts.append("%s %.0ft(%.0f%%)" % [g, wt.groups[g], wt.fractions[g] * 100.0])
	print("무게: " + ", ".join(parts))
	var s: Dictionary = r.stability
	print("복원성: KG %.2fm, GM %.2fm (GM/B %.3f, %s), 벨트 상단 수면 위 %.2fm" % [
		s.kg, s.gm, s.gmRatio, s.status, s.beltFreeboard])
	var p: Dictionary = r.performance
	print("성능: 최고 %.1fkn, 선회 지름 %.0fm, 전속까지 %.0f초" % [
		p.maxSpeedKn, p.tacticalDiameterM, p.timeToFullSpeedSec])
	var c: Dictionary = r.combat
	print("전투: 주포 %d문(%dmm), 일제사격 %.0fkg, 사거리 %.1fkm, 장전 %.0f초, 부포 %d문, 어뢰 %d문" % [
		c.mainBarrels, c.mainCaliberMm, c.broadsideKg, c.mainRangeKm, c.mainReloadSec,
		c.secondaryBarrels, c.torpedoTubes])
	print("비용: %d, 건조 %.1f개월" % [r.cost.cost, r.cost.buildMonths])
