extends RefCounted
## PartsDB 로더. JSON 파일을 읽어 Dictionary로 돌려준다.
## 여러 티어 파일을 합칠 수 있게 load_many()도 제공한다.

static func load_json(path: String) -> Variant:
	if not FileAccess.file_exists(path):
		push_error("파일 없음: %s" % path)
		return null
	var text := FileAccess.get_file_as_string(path)
	var data: Variant = JSON.parse_string(text)
	if data == null:
		push_error("JSON 파싱 실패: %s" % path)
	return data


static func load_db(path: String) -> Dictionary:
	var data: Variant = load_json(path)
	if typeof(data) != TYPE_DICTIONARY:
		return {}
	return data


## 여러 PartsDB 파일을 하나로 합친다. 같은 ID가 있으면 뒤 파일이 이긴다.
static func load_many(paths: Array) -> Dictionary:
	var merged := {"hulls": {}, "machinery": {}, "guns": {}, "fireControl": {}, "costPerTon": {}}
	for p in paths:
		var db := load_db(p)
		for section in merged.keys():
			if db.has(section):
				merged[section].merge(db[section], true)
	return merged


static func load_design(path: String) -> Dictionary:
	var data: Variant = load_json(path)
	if typeof(data) != TYPE_DICTIONARY:
		return {}
	return data
