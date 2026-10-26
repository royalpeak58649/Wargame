extends RefCounted
## 함선 설계 공용 계산 모듈 (기획서 "공통 데이터 형식" 섹션 구현).
##
## 설계 화면과 전투 시스템이 모두 이 파일의 함수만 사용한다.
## 그래서 설계 화면에 보이는 숫자와 전투 중 실제 성능이 절대 어긋나지 않는다.
## 모든 함수는 순수 함수다: 같은 (design, db)를 넣으면 항상 같은 결과가 나온다.
## 파생값(속도, GM 등)은 ShipDesign에 저장하지 않고 필요할 때마다 계산한다.
##
## 단위: 길이 m, 무게 t, 장갑 mm, 구경 mm, 속도 kn, 출력 shp, 시간 s.

const STEEL_DENSITY := 7.85        # t/m^3
const SEAWATER := 1.025            # t/m^3
const SHP_TO_WATT := 745.7
const KNOT_TO_MS := 0.514444
const ADDED_MASS := 1.1            # 가속 계산용 부가 질량 계수

const OVERLOAD_LIMIT := 0.05       # 설계 배수량 대비 허용 과적 비율
const UNDERWEIGHT_WARN := 0.10     # 설계 배수량 대비 이만큼 가벼우면 경고
const GM_RATIO_WARN := 0.03        # GM / 폭 이 값 미만이면 경고
const GM_RATIO_FAIL := 0.015       # GM / 폭 이 값 미만이면 설계 불가
const BELT_FREEBOARD_WARN := 0.5   # 벨트 상단이 수면 위 이 높이(m) 미만이면 경고
const DRAUGHT_TOLERANCE := 0.05    # 템플릿 흘수 대비 허용 오차

const ARMOR_KEYS := ["beltMain", "beltUpper", "deck", "turretFace", "barbette", "conningTower", "torpedoBulge"]
const TIER_COST_MULT := [1.0, 1.0, 1.2, 1.45, 1.75, 2.1]  # 인덱스 = 티어
const SPECIAL_COST_MULT := {"buildable": 1.1, "paper": 1.15}
const SPECIAL_TIME_MULT := {"buildable": 0.85, "paper": 0.9}


# ---------------------------------------------------------------------------
# 공개 API
# ---------------------------------------------------------------------------

## 전부 한 번에 계산한다. 설계 화면은 보통 이것만 호출하면 된다.
static func compute_all(design: Dictionary, db: Dictionary) -> Dictionary:
	var issues := validate_parts(design, db)
	if not issues.errors.is_empty():
		# 부품 단계에서 깨진 설계는 수치 계산을 하지 않는다.
		return {"valid": false, "errors": issues.errors, "warnings": issues.warnings}

	var weights := compute_weights(design, db)
	var stability := compute_stability(design, db)
	var performance := compute_performance(design, db)
	var combat := compute_combat(design, db)
	var cost := compute_cost(design, db)

	var errors: Array = issues.errors.duplicate()
	var warnings: Array = issues.warnings.duplicate()
	_check_numbers(design, db, weights, stability, errors, warnings)

	return {
		"valid": errors.is_empty(),
		"errors": errors,
		"warnings": warnings,
		"weights": weights,
		"stability": stability,
		"performance": performance,
		"combat": combat,
		"cost": cost,
	}


## 설계 화면에서 부품을 바꿀 수 있는가. 특별 군함(locked)은 열람만 가능하다.
static func can_edit(design: Dictionary) -> bool:
	return not bool(design.get("locked", false))


## 항목별 무게, 총중량, 과적량, 실제 흘수.
static func compute_weights(design: Dictionary, db: Dictionary) -> Dictionary:
	var dims := hull_dimensions(design, db)
	var items := _weight_items(design, db)

	var groups := {"structure": 0.0, "armor": 0.0, "machinery": 0.0, "weapons": 0.0,
		"fireControl": 0.0, "fuel": 0.0, "misc": 0.0, "ballast": 0.0}
	var armor := {}
	for k in ARMOR_KEYS:
		armor[k] = 0.0
	var weapons := {}
	var total := 0.0

	for it in items:
		groups[it.group] += it.w
		total += it.w
		if it.group == "armor":
			armor[it.key] = float(armor.get(it.key, 0.0)) + it.w
		elif it.group == "weapons":
			weapons[it.key] = float(weapons.get(it.key, 0.0)) + it.w

	var design_disp: float = dims.displacement
	var overload := total - design_disp
	var awp: float = dims.length * dims.beam * dims.cwp * SEAWATER  # t per m 침하
	var draught_actual: float = dims.draught + overload / awp

	return {
		"groups": groups,
		"armor": armor,
		"weapons": weapons,
		"total": total,
		"designDisplacement": design_disp,
		"overload": overload,
		"overloadRatio": overload / design_disp,
		"draughtActual": draught_actual,
		"fractions": _fractions(groups, total),
	}


## 무게중심(KG), 메타센터 높이(GM), 복원성 등급.
static func compute_stability(design: Dictionary, db: Dictionary) -> Dictionary:
	var dims := hull_dimensions(design, db)
	var items := _weight_items(design, db)

	var w_sum := 0.0
	var m_sum := 0.0
	for it in items:
		w_sum += it.w
		m_sum += it.w * it.z
	var kg := m_sum / w_sum

	var overload := w_sum - float(dims.displacement)
	var t_act: float = dims.draught + overload / (dims.length * dims.beam * dims.cwp * SEAWATER)
	var kb := 0.53 * t_act
	# 수선면 횡관성모멘트 근사: C_I = (0.13 Cwp + 0.87 Cwp^2) / 12
	var c_i: float = (0.13 * dims.cwp + 0.87 * dims.cwp * dims.cwp) / 12.0
	var inertia: float = c_i * dims.length * pow(dims.beam, 3)
	var volume := w_sum / SEAWATER
	var bm := inertia / volume
	var gm := kb + bm - kg
	var ratio: float = gm / dims.beam

	var status := "ok"
	if ratio < GM_RATIO_FAIL:
		status = "invalid"
	elif ratio < GM_RATIO_WARN:
		status = "warning"

	var hull := _hull(design, db)
	var belt_freeboard: float = float(hull.beltTopAboveWL) - (t_act - dims.draught)

	return {
		"kg": kg,
		"kb": kb,
		"bm": bm,
		"gm": gm,
		"gmRatio": ratio,
		"status": status,
		# 전투에서 쓰는 명중률 배수: 복원성이 나쁘면 선회 중 횡경사가 커진다.
		"accuracyMult": 1.0 if status == "ok" else 0.9,
		"beltFreeboard": belt_freeboard,
	}


## 최고 속도, 선회 지름, 가속 시간.
static func compute_performance(design: Dictionary, db: Dictionary) -> Dictionary:
	var dims := hull_dimensions(design, db)
	var hull := _hull(design, db)
	var w := compute_weights(design, db)
	var disp: float = w.total
	var power := float(design.machinery.powerShp)

	# 해군 계수식 + 길이 보정 (기획서 "속도 공식")
	# V = (C * P / Δ^(2/3))^(1/3) * sqrt(L / L_template)
	var c := float(hull.admiralty)
	var v := pow(c * power / pow(disp, 2.0 / 3.0), 1.0 / 3.0)
	v *= sqrt(dims.length / float(hull.length))

	var v_ms := v * KNOT_TO_MS
	var mass_kg := disp * 1000.0 * ADDED_MASS
	var t_full := mass_kg * v_ms * v_ms / (power * SHP_TO_WATT)

	return {
		"maxSpeedKn": v,
		"tacticalDiameterM": float(hull.tacticalDiameterRatio) * dims.length,
		"timeToFullSpeedSec": t_full,
		"displacement": disp,
	}


## 전투 요약 수치: 주포 일제사격 무게, 사거리, 장전, 부포, 어뢰.
static func compute_combat(design: Dictionary, db: Dictionary) -> Dictionary:
	var broadside := 0.0
	var main_barrels := 0
	var main_range := 0.0
	var main_reload := 0.0
	var main_caliber := 0
	var sec_barrels := 0
	var torp_tubes := 0
	var torp_range := 0.0

	for wpn in design.get("weapons", []):
		var g: Dictionary = db.guns[wpn.partId]
		var n := int(wpn.barrels)
		match String(g.type):
			"main":
				main_barrels += n
				broadside += n * float(g.shellMassKg)
				main_range = maxf(main_range, float(g.maxRangeKm))
				main_reload = maxf(main_reload, float(g.reloadSec))
				main_caliber = maxi(main_caliber, int(g.caliber))
			"secondary":
				sec_barrels += n
			"torpedo":
				torp_tubes += n
				torp_range = maxf(torp_range, float(g.torpedoRangeKm))

	var fc: Dictionary = db.fireControl[design.fireControl]
	var stab := compute_stability(design, db)
	return {
		"mainBarrels": main_barrels,
		"mainCaliberMm": main_caliber,
		"broadsideKg": broadside,
		"mainRangeKm": main_range,
		"mainReloadSec": main_reload,
		"secondaryBarrels": sec_barrels,
		"torpedoTubes": torp_tubes,
		"torpedoRangeKm": torp_range,
		"fireControl": {
			"initialSpreadPerKm": float(fc.initialSpreadPerKm),
			"correctionRate": float(fc.correctionRate),
			"minSpreadPerKm": float(fc.minSpreadPerKm),
		},
		"accuracyMult": stab.accuracyMult,
	}


## 건조 비용과 건조 기간(개월).
static func compute_cost(design: Dictionary, db: Dictionary) -> Dictionary:
	var w := compute_weights(design, db)
	var per_ton: Dictionary = db.costPerTon
	var base := 0.0
	for g in w.groups:
		base += float(w.groups[g]) * float(per_ton.get(g, 1.0))

	var tier := clampi(int(design.tier), 1, TIER_COST_MULT.size() - 1)
	var cost: float = base * TIER_COST_MULT[tier]
	var months: float = 8.0 + 0.12 * sqrt(float(w.total))

	var special: Variant = design.get("special", null)
	if special is String:
		cost *= float(SPECIAL_COST_MULT.get(special, 1.0))
		months *= float(SPECIAL_TIME_MULT.get(special, 1.0))
	if special is String and special == "reward":
		cost = 0.0  # 보상형은 지급되므로 건조 비용이 없다

	return {"cost": round(cost), "buildMonths": snappedf(months, 0.1)}


## 설계 치수와 설계 배수량.
static func hull_dimensions(design: Dictionary, db: Dictionary) -> Dictionary:
	var hull := _hull(design, db)
	var L := float(design.hull.get("length", hull.length))
	var B := float(design.hull.get("beam", hull.beam))
	var T := float(design.hull.get("draught", hull.draught))
	var cb := float(hull.cb)
	return {
		"length": L,
		"beam": B,
		"draught": T,
		"depth": float(hull.depth),
		"cb": cb,
		"cwp": float(hull.cwp),
		"displacement": L * B * T * cb * SEAWATER,
	}


## 부품 조합 검증 (수치 계산 전에 하는 검사).
static func validate_parts(design: Dictionary, db: Dictionary) -> Dictionary:
	var errors: Array = []
	var warnings: Array = []
	var nation := String(design.get("nation", ""))
	var tier := int(design.get("tier", 0))

	# 선체
	var hull_id := String(design.get("hull", {}).get("templateId", ""))
	if not db.hulls.has(hull_id):
		errors.append(_issue("hull_missing", "선체 템플릿을 찾을 수 없음: %s" % hull_id))
		return {"errors": errors, "warnings": warnings}
	var hull: Dictionary = db.hulls[hull_id]
	_check_part_access(hull, "선체", hull_id, nation, tier, errors)
	if String(hull.shipClass) != String(design.get("shipClass", "")):
		errors.append(_issue("hull_class", "선체 함종(%s)과 설계 함종(%s)이 다름" % [hull.shipClass, design.get("shipClass")]))

	var L := float(design.hull.get("length", hull.length))
	var B := float(design.hull.get("beam", hull.beam))
	var T := float(design.hull.get("draught", hull.draught))
	if not _in_range(L / float(hull.length), hull.lengthScaleRange):
		errors.append(_issue("hull_length", "선체 길이 %.1fm 는 허용 범위 밖" % L))
	if not _in_range(B / float(hull.beam), hull.beamScaleRange):
		errors.append(_issue("hull_beam", "선체 폭 %.1fm 는 허용 범위 밖" % B))
	if absf(T / float(hull.draught) - 1.0) > DRAUGHT_TOLERANCE:
		errors.append(_issue("hull_draught", "흘수 %.2fm 는 템플릿 흘수와 너무 다름" % T))

	# 기관
	var mach_id := String(design.get("machinery", {}).get("partId", ""))
	if not db.machinery.has(mach_id):
		errors.append(_issue("machinery_missing", "기관을 찾을 수 없음: %s" % mach_id))
	else:
		var m: Dictionary = db.machinery[mach_id]
		_check_part_access(m, "기관", mach_id, nation, tier, errors)
		if not m.allowedClasses.has(design.shipClass):
			errors.append(_issue("machinery_class", "%s 은(는) %s 에 쓸 수 없음" % [m.name, design.shipClass]))
		var shp := float(design.machinery.get("powerShp", 0))
		if shp <= 0.0 or shp > float(m.maxShp):
			errors.append(_issue("machinery_power", "기관 출력 %d shp 는 0 초과 %d 이하여야 함" % [shp, m.maxShp]))

	# 장갑
	var armor: Dictionary = design.get("armor", {})
	for k in ARMOR_KEYS:
		var mm := float(armor.get(k, 0))
		if mm < 0.0 or mm > float(hull.maxArmorMm):
			errors.append(_issue("armor_range", "%s 장갑 %dmm 는 0~%dmm 범위 밖" % [k, mm, hull.maxArmorMm]))

	# 무장
	var slots := {}
	for s in hull.slots:
		slots[s.id] = s
	var used := {}
	var main_count := 0
	for wpn in design.get("weapons", []):
		var sid := String(wpn.get("slot", ""))
		var pid := String(wpn.get("partId", ""))
		var n := int(wpn.get("barrels", 0))
		if not slots.has(sid):
			errors.append(_issue("slot_missing", "선체에 %s 슬롯이 없음" % sid))
			continue
		if used.has(sid):
			errors.append(_issue("slot_duplicate", "%s 슬롯에 무장이 두 번 배치됨" % sid))
			continue
		used[sid] = true
		if not db.guns.has(pid):
			errors.append(_issue("weapon_missing", "무장을 찾을 수 없음: %s" % pid))
			continue
		var g: Dictionary = db.guns[pid]
		var slot: Dictionary = slots[sid]
		_check_part_access(g, "무장", pid, nation, tier, errors)
		if String(g.type) != String(slot.type):
			errors.append(_issue("slot_type", "%s(%s) 슬롯에 %s 무장을 놓을 수 없음" % [sid, slot.type, g.type]))
		if int(g.caliber) > int(slot.maxCaliber):
			errors.append(_issue("slot_caliber", "%s 슬롯 최대 구경 %dmm 초과 (%dmm)" % [sid, slot.maxCaliber, g.caliber]))
		if int(g.caliber) < int(slot.get("minCaliber", 0)):
			errors.append(_issue("slot_min_caliber", "%s 슬롯 최소 구경 %dmm 미만 (%dmm)" % [sid, slot.minCaliber, g.caliber]))
		if n < 1 or n > int(slot.maxBarrels):
			errors.append(_issue("slot_barrels", "%s 슬롯은 1~%d문까지" % [sid, slot.maxBarrels]))
		elif g.has("mountWeight") and not g.mountWeight.has(str(n)):
			errors.append(_issue("mount_barrels", "%s 은(는) %d연장 포탑이 없음 (가능: %s)" % [g.name, n, ", ".join(g.mountWeight.keys())]))
		if String(g.type) == "main":
			main_count += 1

	if main_count == 0:
		warnings.append(_issue("no_main_guns", "주포가 하나도 없음"))

	# 사격 통제
	var fc_id := String(design.get("fireControl", ""))
	if not db.fireControl.has(fc_id):
		errors.append(_issue("fc_missing", "사격 통제 장치를 찾을 수 없음: %s" % fc_id))
	else:
		_check_part_access(db.fireControl[fc_id], "사격 통제", fc_id, nation, tier, errors)

	# 연료
	var ff := float(design.get("fuelFraction", 0))
	if not _in_range(ff, hull.fuelFractionRange):
		errors.append(_issue("fuel_range", "연료 비율 %.2f 는 %.2f~%.2f 범위 밖" % [ff, hull.fuelFractionRange[0], hull.fuelFractionRange[1]]))

	return {"errors": errors, "warnings": warnings}


# ---------------------------------------------------------------------------
# 내부 함수
# ---------------------------------------------------------------------------

static func _hull(design: Dictionary, db: Dictionary) -> Dictionary:
	return db.hulls[design.hull.templateId]


## 모든 무게 항목을 (그룹, 키, 무게 w, 높이 z) 목록으로 만든다.
## 무게 계산과 복원성 계산이 같은 목록을 쓰므로 둘이 어긋나지 않는다.
## z는 용골(선저)에서 잰 무게중심 높이(m).
static func _weight_items(design: Dictionary, db: Dictionary) -> Array:
	var hull := _hull(design, db)
	var dims := hull_dimensions(design, db)
	var L: float = dims.length
	var B: float = dims.beam
	var T: float = dims.draught
	var D: float = dims.depth
	var disp: float = dims.displacement
	var armor: Dictionary = design.get("armor", {})
	var items: Array = []

	# 선체 구조
	items.append(_item("structure", "hull", float(hull.structCoeff) * L * B * D, 0.55 * D))

	# 장갑 구역: 두께 × 면적 × 강재 밀도
	var belt_len := L * float(hull.beltLengthFrac)
	var belt_top := T + float(hull.beltTopAboveWL)
	var belt_h := float(hull.beltHeight)
	var upper_h := float(hull.upperBeltHeight)
	items.append(_item("armor", "beltMain",
		_plate(belt_len * belt_h * 2.0, armor.get("beltMain", 0)), belt_top - belt_h / 2.0))
	items.append(_item("armor", "beltUpper",
		_plate(belt_len * upper_h * 2.0, armor.get("beltUpper", 0)), belt_top + upper_h / 2.0))
	items.append(_item("armor", "deck",
		_plate(belt_len * B * float(hull.deckAreaFrac), armor.get("deck", 0)), belt_top))
	items.append(_item("armor", "conningTower",
		_plate(float(hull.conningTowerArea), armor.get("conningTower", 0)), D + float(hull.conningTowerHeight)))
	# 어뢰 방어 벌지: 수선 아래 측면, 벨트 길이 × 흘수 높이 양현
	items.append(_item("armor", "torpedoBulge",
		_plate(belt_len * T * 2.0, armor.get("torpedoBulge", 0)), T * 0.5))

	# 슬롯별 무장 + 포탑 장갑 + 바벳
	var slots := {}
	for s in hull.slots:
		slots[s.id] = s
	for wpn in design.get("weapons", []):
		var g: Dictionary = db.guns[wpn.partId]
		var slot: Dictionary = slots[wpn.slot]
		var n := int(wpn.barrels)
		var z := D + float(slot.z)
		items.append(_item("weapons", wpn.slot, _mount_weight(g, n), z))
		if float(g.turretArmorArea) > 0.0:
			items.append(_item("armor", "turretFace",
				_plate(float(g.turretArmorArea), armor.get("turretFace", 0)), z))
		var bh := float(hull.barbetteHeight)
		if float(g.barbetteDiameter) > 0.0 and bh > 0.0:
			items.append(_item("armor", "barbette",
				_plate(PI * float(g.barbetteDiameter) * bh, armor.get("barbette", 0)), z - bh / 2.0))

	# 기관
	var mach: Dictionary = db.machinery[design.machinery.partId]
	items.append(_item("machinery", "machinery", float(design.machinery.powerShp) / float(mach.shpPerTon), 0.3 * D))

	# 사격 통제: 큰 배일수록 장비가 커진다
	var fc: Dictionary = db.fireControl[design.fireControl]
	items.append(_item("fireControl", "fireControl",
		float(fc.baseWeight) * sqrt(disp / 30000.0), D + float(hull.fireControlHeight)))

	# 연료, 기타(승조원, 보급품, 보트 등)
	items.append(_item("fuel", "fuel", float(design.get("fuelFraction", 0)) * disp, 0.2 * D))
	items.append(_item("misc", "misc", float(hull.miscFraction) * disp, 0.7 * D))

	# 밸러스트: 설계 배수량보다 지나치게 가벼우면 선저에 밸러스트를 자동 적재한다.
	# 가벼운 배가 공짜로 빨라지는 것을 막고, 대신 복원성은 좋아진다.
	var sum := 0.0
	for it in items:
		sum += it.w
	var floor_w := (1.0 - UNDERWEIGHT_WARN) * disp
	if sum < floor_w:
		items.append(_item("ballast", "ballast", floor_w - sum, 0.1 * D))
	return items


static func _item(group: String, key: String, w: float, z: float) -> Dictionary:
	return {"group": group, "key": key, "w": w, "z": z}


static func _plate(area_m2: float, thickness_mm: Variant) -> float:
	return area_m2 * float(thickness_mm) / 1000.0 * STEEL_DENSITY


static func _mount_weight(g: Dictionary, barrels: int) -> float:
	var mount := 0.0
	if g.has("mountWeight"):
		mount = float(g.mountWeight.get(str(barrels), 0.0))
	else:
		mount = float(g.get("mountWeightPerBarrel", 0.0)) * barrels
	return mount + float(g.get("ammoPerBarrel", 0.0)) * barrels


static func _fractions(groups: Dictionary, total: float) -> Dictionary:
	var out := {}
	for g in groups:
		out[g] = float(groups[g]) / total
	return out


static func _check_numbers(design: Dictionary, db: Dictionary, w: Dictionary, s: Dictionary,
		errors: Array, warnings: Array) -> void:
	var ratio: float = w.overloadRatio
	if ratio > OVERLOAD_LIMIT:
		errors.append(_issue("overload", "과적 %.1f%% (허용 %d%%): 장갑, 무장, 기관 중 무언가를 줄여야 함" % [ratio * 100.0, OVERLOAD_LIMIT * 100.0]))
	elif ratio > 0.0:
		warnings.append(_issue("overload_minor", "과적 %.1f%%: 흘수가 %.2fm 깊어짐" % [ratio * 100.0, w.draughtActual - float(design.hull.draught)]))
	if float(w.groups.ballast) > 0.0:
		warnings.append(_issue("ballast", "무게 예산이 남아 밸러스트 %.0ft 적재: 장갑이나 무장을 더 실을 여유가 있음" % w.groups.ballast))

	match String(s.status):
		"invalid":
			errors.append(_issue("stability_fail", "복원성 부족 (GM %.2fm): 상부 무게를 줄여야 함" % s.gm))
		"warning":
			warnings.append(_issue("stability_warn", "복원성 낮음 (GM %.2fm): 선회 중 명중률 10%% 감소" % s.gm))

	if float(design.armor.get("beltMain", 0)) > 0.0 and float(s.beltFreeboard) < BELT_FREEBOARD_WARN:
		warnings.append(_issue("belt_submerged", "주 벨트 상단이 수면 위 %.2fm 뿐: 과적으로 벨트가 잠기는 중" % s.beltFreeboard))


static func _check_part_access(part: Dictionary, label: String, id: String, nation: String, tier: int, errors: Array) -> void:
	if part.has("nations") and not part.nations.has(nation):
		errors.append(_issue("nation_part", "%s %s 은(는) %s 에서 쓸 수 없음" % [label, id, nation]))
	if int(part.get("tier", 1)) > tier:
		errors.append(_issue("tier_part", "%s %s 은(는) T%d 부품이라 T%d 설계에 못 씀" % [label, id, part.tier, tier]))


static func _in_range(v: float, r: Variant) -> bool:
	return v >= float(r[0]) - 1e-6 and v <= float(r[1]) + 1e-6


static func _issue(code: String, message: String) -> Dictionary:
	return {"code": code, "message": message}
