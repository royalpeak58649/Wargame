extends RefCounted
## 설계 화면이 쓰는 순수 함수 모음 (선택지 규칙).
##
## 화면(designer.gd)은 여기 있는 함수만 써서 드롭다운 항목을 만든다.
## 판정 규칙은 scripts/core/ship_calc.gd 의 _check_part_access 와 같다.
## 그 함수는 내부 함수라 부를 수 없으므로 같은 규칙을 이 파일에 둔다.
## 모든 함수는 static 이고 부작용이 없다.

const ShipCalc = preload("res://scripts/core/ship_calc.gd")


## 부품을 이 설계에 쓸 수 있는가.
## - part.nations 가 있으면 design.nation 이 포함돼야 한다.
## - part.tier(없으면 1) 가 design.tier 이하여야 한다.
static func part_allowed(part: Dictionary, design: Dictionary) -> bool:
	if part.has("nations") and not part.nations.has(String(design.get("nation", ""))):
		return false
	if int(part.get("tier", 1)) > int(design.get("tier", 0)):
		return false
	return true


## 기관 선택지: 접근 가능 + allowedClasses 에 설계 함종이 있는 것.
static func machinery_options(db: Dictionary, design: Dictionary) -> Array:
	var out: Array = []
	var ship_class := String(design.get("shipClass", ""))
	for id in db.machinery:
		var m: Dictionary = db.machinery[id]
		if not part_allowed(m, design):
			continue
		if not m.get("allowedClasses", []).has(ship_class):
			continue
		out.append(String(id))
	out.sort()
	return out


## 무장 선택지: 접근 가능 + 슬롯 종류 일치 + 구경 상한 이하.
static func gun_options(db: Dictionary, design: Dictionary, slot: Dictionary) -> Array:
	var out: Array = []
	for id in db.guns:
		var g: Dictionary = db.guns[id]
		if not part_allowed(g, design):
			continue
		if String(g.get("type", "")) != String(slot.get("type", "")):
			continue
		if int(g.get("caliber", 0)) > int(slot.get("maxCaliber", 0)):
			continue
		out.append(String(id))
	out.sort()
	return out


## 포문 수 선택지.
## mountWeight 가 있으면 그 키 중 슬롯 상한 이하인 값을 오름차순으로,
## 없으면 1..슬롯 상한을 돌려준다.
static func barrel_options(gun: Dictionary, slot: Dictionary) -> Array:
	var max_barrels := int(slot.get("maxBarrels", 1))
	var out: Array = []
	if gun.has("mountWeight"):
		var keys: Array = []
		for k in gun.mountWeight.keys():
			var n := int(String(k))
			if n >= 1 and n <= max_barrels:
				keys.append(n)
		keys.sort()
		out = keys
	else:
		for n in range(1, max_barrels + 1):
			out.append(n)
	return out


## 사격 통제 선택지: 접근 가능한 것.
static func fire_control_options(db: Dictionary, design: Dictionary) -> Array:
	var out: Array = []
	for id in db.fireControl:
		if part_allowed(db.fireControl[id], design):
			out.append(String(id))
	out.sort()
	return out


## 설계 티어 이하의 선체 템플릿 ID 목록.
static func hull_options(db: Dictionary, design: Dictionary) -> Array:
	var out: Array = []
	var tier := int(design.get("tier", 0))
	for id in db.hulls:
		if int(db.hulls[id].get("tier", 1)) <= tier:
			out.append(String(id))
	out.sort()
	return out


## 드롭다운에 넣을 항목 목록을 만든다.
## 현재 값이 규칙 밖이면 "(사용 불가)" 를 붙여 그대로 보여 준다.
## 오류 메시지는 ShipCalc.compute_all 이 낸다.
static func with_current(options: Array, current: String, db_section: Dictionary) -> Array:
	var out: Array = options.duplicate()
	if current == "" or out.has(current):
		return out
	var label := current
	if db_section.has(current):
		label = "%s (사용 불가)" % String(db_section[current].get("name", current))
	else:
		label = "%s (사용 불가)" % current
	out.append(label)
	return out


## 드롭다운 항목 문자열에서 부품 ID 를 되찾는다.
## "(사용 불가)" 가 붙은 항목은 ID 그대로 들어 있으므로 그대로 돌려준다.
static func id_from_label(label: String, options: Array, ids: Array) -> String:
	var idx := options.find(label)
	if idx >= 0 and idx < ids.size():
		return String(ids[idx])
	return label
