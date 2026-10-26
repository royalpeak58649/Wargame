extends RefCounted
## 전투 탄도·관통 계산 모듈 (순수 함수).
##
## 전투는 2D 평면에서 진행되지만, 포탄의 낙각과 장갑 경사는 수치로 계산해
## 3D 판정을 흉내 낸다. 노드/씬/렌더링은 이 파일에 없다.
## 모든 함수는 순수 함수다: 같은 입력이면 항상 같은 결과가 나온다(난수 없음).
##
## 대상 부품은 data/parts_t2.json 의 guns 항목(Dictionary)이다.
## 사용하는 필드: caliber(mm), shellMassKg, muzzleVelocity(m/s), maxRangeKm.
##
## 단위: 거리 m(인자 이름이 _km 이면 km), 속도 m/s, 질량 kg, 장갑 mm, 각도 도(°), 시간 s.

# --- 밸런스 상수 (조정 대상) -------------------------------------------------

const VEL_DECAY_KM := 74.0        # km, 잔존 속도 감쇠 거리 상수 (탄도계수 대용)
const PEN_K := 0.17               # 무차원, De Marre 형태 관통 계수
const FALL_MAX_DEG := 30.0        # °, 최대 사거리에서의 낙각
const FALL_EXP := 1.6             # 무차원, 거리비 → 낙각 곡선 지수
const FLIGHT_SPEED_FRAC := 0.8    # 무차원, 평균 비행 속도 / 초속
const DECK_CHANCE_DEG := 25.0     # °, 갑판 피탄 확률이 최대가 되는 낙각
const DECK_CHANCE_MIN := 0.05     # 무차원, 갑판 피탄 확률 하한
const DECK_CHANCE_MAX := 0.85     # 무차원, 갑판 피탄 확률 상한
const RICOCHET_DEG := 60.0        # °, 벨트(수직 판): 이 이상 입사각이면 도탄
const DECK_RICOCHET_DEG := 80.0   # °, 갑판(수평 판): 이 이상 입사각이면 도탄.
                                  #    낙각이 최대 30°라 갑판 입사각은 늘 60° 이상이므로 따로 둔다.
const MAX_OBLIQUITY_DEG := 89.9   # °, 이 이상 입사각이면 유효 장갑 무한대

const PLATE_BELT := "belt"        # 수직 판(현측 장갑)
const PLATE_DECK := "deck"        # 수평 판(갑판 장갑)


# ---------------------------------------------------------------------------
# 탄도
# ---------------------------------------------------------------------------

## 단면 밀도: shellMassKg / (caliber_cm)^2. 클수록 속도가 덜 줄어든다.
static func sectional_density(gun: Dictionary) -> float:
	var caliber_cm: float = float(gun.caliber) / 10.0
	return float(gun.shellMassKg) / (caliber_cm * caliber_cm)


## 거리 range_km 에서의 잔존 속도(m/s).
static func remaining_velocity(gun: Dictionary, range_km: float) -> float:
	var decay: float = VEL_DECAY_KM * sectional_density(gun)
	return float(gun.muzzleVelocity) * exp(-range_km / decay)


## 거리 range_km 에서의 관통력(mm). De Marre 형태 단순화.
static func penetration_mm(gun: Dictionary, range_km: float) -> float:
	var v: float = remaining_velocity(gun, range_km)
	var m: float = float(gun.shellMassKg)
	var caliber_mm: float = float(gun.caliber)
	return PEN_K * pow(v, 1.43) * pow(m, 0.71) / pow(caliber_mm, 1.07)


## 거리 range_km 에서의 낙각(°). 최대 사거리에서 FALL_MAX_DEG.
static func fall_angle_deg(gun: Dictionary, range_km: float) -> float:
	var max_range_km: float = float(gun.maxRangeKm)
	if max_range_km <= 0.0:
		return 0.0
	var ratio: float = clampf(range_km / max_range_km, 0.0, 1.0)
	return FALL_MAX_DEG * pow(ratio, FALL_EXP)


## 거리 range_km 의 비행 시간(s). 평균 속도를 초속의 일정 비율로 본다.
static func flight_time_sec(gun: Dictionary, range_km: float) -> float:
	return range_km * 1000.0 / (FLIGHT_SPEED_FRAC * float(gun.muzzleVelocity))


# ---------------------------------------------------------------------------
# 입사각과 관통 판정
# ---------------------------------------------------------------------------

## 판 법선과 탄도 사이의 입사각(°). 0이면 수직 입사.
## plate: "belt"(수직 판) 또는 "deck"(수평 판).
## horizontal_deg: 수평면에서 탄도와 벨트 법선 사이 각도. 0 = 현측 정면, 90 = 정선수/정선미.
static func impact_obliquity_deg(plate: String, fall_deg: float, horizontal_deg: float) -> float:
	var fall_rad: float = deg_to_rad(fall_deg)
	var cos_theta: float = 0.0
	match plate:
		PLATE_BELT:
			cos_theta = cos(fall_rad) * cos(deg_to_rad(horizontal_deg))
		PLATE_DECK:
			cos_theta = sin(fall_rad)
		_:
			# 알 수 없는 판 종류는 수직 입사로 본다.
			cos_theta = 1.0
	return rad_to_deg(acos(clampf(cos_theta, 0.0, 1.0)))


## 경사 장갑의 유효 두께(mm). 입사각이 커질수록 두꺼워진다.
static func effective_armor_mm(thickness_mm: float, obliquity_deg: float) -> float:
	if obliquity_deg >= MAX_OBLIQUITY_DEG:
		return INF
	return thickness_mm / cos(deg_to_rad(obliquity_deg))


## 낙각 fall_deg 일 때 갑판을 맞을 확률(0~1).
static func deck_hit_chance(fall_deg: float) -> float:
	return clampf(fall_deg / DECK_CHANCE_DEG, DECK_CHANCE_MIN, DECK_CHANCE_MAX)


## 판 종류별 도탄 기준 입사각(°).
static func ricochet_threshold_deg(plate: String) -> float:
	return DECK_RICOCHET_DEG if plate == PLATE_DECK else RICOCHET_DEG


## 한 발의 판정을 결정적으로 계산한다.
## 반환: {"result", "penMm", "effArmorMm", "obliquityDeg", "fallDeg"}
## result: "ricochet" | "pen" | "nonpen"
static func resolve_hit(gun: Dictionary, range_km: float, plate: String, thickness_mm: float,
		horizontal_deg: float) -> Dictionary:
	var fall_deg: float = fall_angle_deg(gun, range_km)
	var obliquity_deg: float = impact_obliquity_deg(plate, fall_deg, horizontal_deg)
	var pen_mm: float = penetration_mm(gun, range_km)
	var eff_armor_mm: float = effective_armor_mm(thickness_mm, obliquity_deg)

	var result := "nonpen"
	if thickness_mm <= 0.0:
		# 장갑 없는 구역은 입사각과 무관하게 관통.
		result = "pen"
	elif obliquity_deg >= ricochet_threshold_deg(plate):
		result = "ricochet"
	elif pen_mm >= eff_armor_mm:
		result = "pen"

	return {
		"result": result,
		"penMm": pen_mm,
		"effArmorMm": eff_armor_mm,
		"obliquityDeg": obliquity_deg,
		"fallDeg": fall_deg,
	}


# ---------------------------------------------------------------------------
# 사격 통제
# ---------------------------------------------------------------------------

## 일제사격 분산(m). σ_n = σ0 · (1 − c)^n + σ_min, 명중률 배수로 나눈다.
## σ0 와 σ_min 모두 거리에 비례한다(가까울수록 잘 맞는다).
## fc: parts_t2.json 의 fireControl 항목.
static func salvo_spread_m(fc: Dictionary, range_km: float, shots_on_target: int,
		accuracy_mult: float) -> float:
	var sigma0: float = float(fc.initialSpreadPerKm) * range_km
	var c: float = float(fc.correctionRate)
	var sigma_min: float = float(fc.minSpreadPerKm) * range_km
	var n: int = maxi(shots_on_target, 0)
	var spread: float = sigma0 * pow(1.0 - c, float(n)) + sigma_min
	if accuracy_mult <= 0.0:
		return spread
	return spread / accuracy_mult
