# Wargame — 함선 설계 + 함대 지휘 (Godot 4 프로토타입)

함선 설계 + 함대 지휘 2D 실시간 전술 게임의 첫 번째 코드. 기획서 "공통 데이터 형식" 섹션을 구현했다.
설계 화면과 전투 시스템은 이 모듈의 함수만 써서 숫자가 절대 어긋나지 않게 한다.

## 구성

| 경로 | 내용 |
| --- | --- |
| `scripts/core/ship_calc.gd` | 계산 모듈: 무게, 복원성, 성능, 전투 요약, 비용, 검증 |
| `scripts/core/parts_db.gd` | PartsDB, ShipDesign JSON 로더 |
| `data/parts_t2.json` | T2 부품: 선체 2, 기관 2, 주포 3, 부포 1, 구축함포 1, 어뢰 1, 사격 통제 1 |
| `data/designs/*.json` | 샘플 설계: Queen Elizabeth급, V&W급 (건조형 특별 군함), 플레이어 고속 전함안 |
| `scripts/combat/ballistics.gd` | 전투 탄도·관통 계산 모듈: 잔존 속도, 관통력, 낙각, 입사각, 유효 장갑, 일제사격 분산 (순수 함수) |
| `scripts/combat/ship_state.gd` | 전투 중 함선 상태: 성능·내구·무장·사격 통제, 이동 단계 (순수 로직) |
| `scripts/combat/battle_sim.gd` | 전투 시뮬레이션 코어: 시간 단계, 사격, 피해, 전투 로그, 명령 API |
| `tests/run_tests.gd` | 단위 테스트 24개 |
| `tests/run_combat_tests.gd` | 전투 탄도·관통 계산 테스트 10개 + 전투 시뮬레이션 테스트 9개 |
| `tools/report.gd` | 설계 파일을 계산해 출력하는 도구 |
| `tools/battle_report.gd` | 전투 시뮬레이션 한 판을 돌려 결과를 출력하는 도구 |

## 실행 (Godot 4.7)

```
godot --headless --path . --script res://tests/run_tests.gd
godot --headless --path . --script res://tests/run_combat_tests.gd
godot --headless --path . --script res://tools/report.gd
godot --headless --path . --script res://tools/report.gd -- res://data/designs/uk_dd_vw.json
godot --headless --path . --script res://tools/battle_report.gd
godot --headless --path . --script res://tools/battle_report.gd -- 12345
```

## 사용법 (다른 코드에서)

```gdscript
const ShipCalc = preload("res://scripts/core/ship_calc.gd")
const PartsDB = preload("res://scripts/core/parts_db.gd")

var db := PartsDB.load_db("res://data/parts_t2.json")
var design := PartsDB.load_design("res://data/designs/uk_bb_queen_elizabeth.json")
var r := ShipCalc.compute_all(design, db)
# r.valid, r.errors, r.warnings, r.weights, r.stability, r.performance, r.combat, r.cost
```

## 계산 규칙 요약

- 설계 배수량 = L × B × T × Cb × 1.025
- 무게 항목마다 (무게, 높이)를 만들고, 무게 계산과 복원성 계산이 같은 목록을 쓴다.
- 장갑 무게 = 면적 × 두께 × 7.85 t/m³. 면적은 선체 치수에서 자동 계산.
- 속도 V = (C × P / Δ^(2/3))^(1/3) × √(L / L_템플릿). C는 선체 템플릿의 해군 계수.
- GM = KB + BM − KG. GM/폭 < 0.03 경고(명중률 −10%), < 0.015 설계 불가.
- 과적 5% 초과 시 설계 불가. 설계 배수량의 90% 미만이면 밸러스트를 자동 적재.
- 특별 군함: `locked: true`면 편집 불가. 건조형 비용 ×1.1, 기간 ×0.85. 보상형 비용 0.
- 슬롯 필드 `minCaliber`(선택, 기본 0): 무장 구경이 이 값 미만이면 `slot_min_caliber` 오류.

## 보정 결과

| 함선 | 항목 | 계산값 | 실제(근사) |
| --- | --- | --- | --- |
| Queen Elizabeth급 | 최고 속도 | 24.4 kn | 약 24 kn |
| Queen Elizabeth급 | GM | 1.71 m | 약 1.9 m |
| V&W급 | 최고 속도 | 34.1 kn | 약 34 kn |
| V&W급 | 배수량 | 1,462 t | 약 1,500 t |

부품 수치는 실제 함선을 참고한 근사치이며 밸런스 조정 대상이다.

## 다음 작업

- 워커 A: 설계 화면 UI (`compute_all` 결과를 실시간 표시)
- 워커 B: 전투 시뮬레이션 (`compute_performance`, `compute_combat` 결과로 함선 생성)
