extends RefCounted
## 항해 계산 (순수 함수, 노드 없음).
##
## 좌표: Vector2, 미터. x = 동쪽, y = 북쪽.
## 침로(heading): 도, 북쪽 0, 시계 방향 증가. 방향 벡터 = Vector2(sin h, cos h).
## 모든 함수는 순수 함수다: 같은 입력이면 항상 같은 결과가 나온다(난수 없음).
##
## 사용법:
##   var h := Navigation.steer_to_point(ship, Vector2(5000.0, 5000.0))

# --- 밸런스 상수 (조정 대상) -------------------------------------------------

const FULL_TURN_DEG := 360.0      # °, 침로 정규화 범위


## from 에서 to 로 가는 침로(°). 북 0, 시계 방향, 0~360.
static func bearing_deg(from: Vector2, to: Vector2) -> float:
	var d: Vector2 = to - from
	if d.length() < 0.001:
		return 0.0
	return wrapf(rad_to_deg(atan2(d.x, d.y)), 0.0, FULL_TURN_DEG)


## b − a 를 −180~180 으로 접은 값(°).
static func angle_diff_deg(a: float, b: float) -> float:
	return wrapf(b - a, -180.0, 180.0)


## 그 점으로 가는 침로(°).
static func steer_to_point(ship: Variant, point: Vector2) -> float:
	return bearing_deg(ship.pos, point)


## 그 점에 radius_m 안으로 들어왔는가.
static func arrived(ship: Variant, point: Vector2, radius_m: float) -> bool:
	return ship.pos.distance_to(point) <= radius_m
