# 크리에이터 정산 시스템

크리에이터가 운영하는 강의의 판매·취소 내역을 집계하여 월별 정산을 처리하는 백엔드 API 서버입니다.

---

## 프로젝트 개요

크리에이터(강의 제작자)가 보유한 강의의 판매 내역과 취소(환불) 내역을 기반으로 월별 정산 금액을 계산하고, 정산 확정 및 지급 처리를 수행합니다. 관리자는 특정 기간 내 전체 크리에이터의 정산 현황을 집계하여 조회할 수 있습니다.

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.5 |
| ORM | Spring Data JPA / Hibernate |
| DB (운영) | PostgreSQL |
| DB (테스트) | H2 (PostgreSQL 호환 모드) |
| 캐시 | Caffeine Cache |
| 동시성 | Virtual Threads (Spring Boot 기본 활성화) |
| 검증 | Bean Validation (jakarta.validation) |
| 빌드 | Gradle |
| 기타 | Lombok, spring-dotenv |

---

## 실행 방법

### 사전 준비

`.env.example`을 복사하여 `.env`를 생성하고 값을 채웁니다.

```
DB_URL=jdbc:postgresql://...
DB_USERNAME=
DB_PASSWORD=
COMMISSION_RATE=0.20
```

PostgreSQL이 로컬에서 실행 중이어야 합니다.

### 빌드

```bash
./gradlew build
```

### 실행

```bash
./gradlew bootRun
```

기본 활성 프로파일은 `local`입니다. PostgreSQL 연결이 필요합니다.

---

## 테스트 실행 방법

테스트는 H2 인메모리 DB(PostgreSQL 호환 모드)를 사용합니다. 외부 인프라 없이 실행 가능합니다.

```bash
# 전체 테스트
./gradlew test

# 특정 클래스만 실행
./gradlew test --tests "liveclass.creator_settlement.domain.saleRecord.SaleRecordRepositoryTest"
```

테스트 레이어별 구성:

| 레이어 | 어노테이션 | 설명 |
|---|---|---|
| Repository | `@DataJpaTest` | H2 + JPA 슬라이스, 실제 쿼리 검증 |
| Service (Query) | `@ExtendWith(MockitoExtension)` | Mock 기반 단위 테스트 |
| Cache | `@SpringBootTest` (슬라이스) | Caffeine 실제 동작 검증 |

---

## API 목록 및 예시

모든 API는 URL 버전(`version = "v1"`, path segment = 1)을 사용합니다.

### 판매 내역

#### 판매 등록

```
POST /api/v1/sale-record
Content-Type: application/json

{
  "courseId": "course-10",
  "studentId": "student-1",
  "amount": 99000.00,
  "paidAt": "2025-03-15T14:00:00"
}
```

- `amount`: 최대 6자리 정수 + 소수점 2자리 (최대 999999.99). 실질적으로 강의료 제약(precision=8, scale=2)에 따라 최대 99만원.

#### 판매 내역 조회

```
GET /api/v1/sale-record?creatorId=creator-10&startDate=2025-03-01&endDate=2025-03-31
```

- `startDate`, `endDate`는 선택값입니다. 없으면 해당 크리에이터의 전체 내역을 반환합니다.

---

### 취소 내역

#### 취소(환불) 등록

```
POST /api/v1/cancel-record
Content-Type: application/json

{
  "saleRecordId": "sale-10",
  "refundAmount": 99000.00,
  "cancelledAt": "2025-03-20T10:00:00"
}
```

- 이미 취소된 판매 건에 대해 재취소 시도 시 `ALREADY_CANCELLED` 오류를 반환합니다.
- 취소 시 비관적 락(Pessimistic Write Lock)으로 동시 취소 요청을 제어합니다.

---

### 정산

#### 월별 정산 조회 (크리에이터)

```
GET /api/v1/settlement/creator/{creatorId}?yearMonth=2025-03
```

응답:

```json
{
  "creatorId": "creator-10",
  "creatorName": "홍길동",
  "yearMonth": "2025-03",
  "status": "PENDING",
  "totalAmount": 300000.00,
  "refundAmount": 50000.00,
  "netAmount": 250000.00,
  "commissionRate": 0.20,
  "commissionAmount": 50000.00,
  "expectedSettleAmount": 200000.00,
  "sellCount": 3,
  "cancelCount": 1
}
```

- `PENDING`: 정산 확정 전 — 현재 월 포함, 항상 실시간 계산
- `CONFIRMED`: 정산 확정됨 — confirm 시점 SettlementLog 스냅샷에서 조회
- `PAID`: 지급 완료 — 동일 SettlementLog 스냅샷에서 조회

#### 정산 확정

```
POST /api/v1/settlement/confirm?creatorId=creator-10&yearMonth=2025-03
```

- 해당 월이 완전히 종료되지 않으면 확정 불가 (`SETTLEMENT_MONTH_NOT_ENDED`)
- 이미 CONFIRMED 또는 PAID 상태면 중복 확정 불가 (`SETTLEMENT_ALREADY_EXISTS`)

#### 지급 처리

```
POST /api/v1/settlement/{settlementId}/pay
```

- CONFIRMED 상태의 정산만 PAID로 전환 가능
- 금액 스냅샷은 confirm 시점에 이미 생성된 `SettlementLog`를 그대로 사용

#### 관리자 집계 조회

```
GET /api/v1/admin/settlement/admin?startDate=2025-03-01&endDate=2025-03-31
```

응답:

```json
{
  "entries": [
    {
      "creatorId": "creator-10",
      "creatorName": "홍길동",
      "totalAmount": 500000.00,
      "refundAmount": 100000.00,
      "netAmount": 400000.00,
      "commissionAmount": 80000.00,
      "expectedSettleAmount": 320000.00,
      "sellCount": 5,
      "cancelCount": 1
    }
  ],
  "totalSettlementAmount": 320000.00
}
```

---

## 데이터 모델 설명

```
Creator
  └── Course (1:N, creatorId FK)
        └── SaleRecord (1:N, courseId FK)
              └── CancelRecord (1:1, saleRecordId FK)

Settlement (월별 정산 마커, creator_id + year_month unique)
  └── SettlementLog (PAID 시점 스냅샷, settlementId FK)
```

### 주요 엔티티

| 엔티티 | 테이블 | 설명 |
|---|---|---|
| `Creator` | `creators` | 크리에이터. id, name |
| `Course` | `courses` | 강의. creatorId FK, price (최대 99만원) |
| `SaleRecord` | `sale_records` | 판매 내역. amount, paidAt, status(PAID/CANCELLED), version(낙관적 락) |
| `CancelRecord` | `cancel_records` | 취소 내역. saleRecordId FK, refundAmount, cancelledAt |
| `Settlement` | `settlements` | 정산 이벤트 마커. status(PENDING/CONFIRMED/PAID), creator_id+year_month unique |
| `SettlementLog` | `settlement_logs` | confirm 시점 금액 스냅샷. totalAmount·refundAmount·netAmount 등 보관. 사후 환불 발생 시 수정 가능 |

### Money 값 객체

`BigDecimal`을 래핑한 불변 레코드입니다. scale=2(HALF_UP)를 강제하며, 음수 결과가 나오는 연산 시 `BusinessException`을 발생시킵니다.

### 수수료율

`app.commission-rate` 환경변수로 관리합니다(기본값 0.20). 변경 이력은 git 태그로 추적하는 것을 전제로 합니다.

---

## 요구사항 해석 및 가정

- **정산 대상 기간**: 판매 기준 `paidAt`, 취소 기준 `cancelledAt`을 사용하여 해당 월의 시작~끝(LocalTime.MAX)으로 범위 조회합니다.
- **판매 금액 제약**: `@Digits(integer=6, fraction=2)` + DB `precision=8, scale=2`로 최대 999,999.99원으로 제한합니다. 실제 강의료는 price 컬럼 기준 최대 99만원으로 간주합니다.
- **크리에이터 이름 조회**: 정산 엔티티에 이름을 저장하지 않고, 응답 시점에 캐시(`Caffeine`)를 통해 조회합니다.
- **정산 PENDING 상태**: DB에 별도 레코드를 생성하지 않고, 조회 시 실시간으로 계산하여 반환합니다.
- **ID 생성**: `AtomicLong` 기반 `IdGenerator`를 사용하며 재시작 시 초기화됩니다. 운영 환경에서는 UUID 또는 별도 채번 전략으로 교체가 필요합니다.

---

## 설계 결정과 이유

### 1. Settlement 아키텍처 진화 — 금액 직접 저장 → 마커 + SettlementLog 스냅샷

#### 초기 설계

`Settlement` 엔티티 하나에 `totalAmount`, `refundAmount`, `netAmount`, `commissionAmount`, `expectedSettleAmount`, `sellCount`, `cancelCount`를 모두 저장.  
PENDING 상태 계산 결과는 Redis에 TTL 1시간으로 캐싱.

```
문제점
- 판매·취소가 발생할 때마다 Settlement를 UPDATE해야 하는 구조적 결합
- PENDING은 언제든 새 거래가 발생하면 달라지므로 1시간 TTL 캐시는 일관성 보장 불가
- 정산 확정(confirm) 시점과 금액 마지막 갱신 시점이 달라 불일치 가능
```

#### 중간 과정

`Settlement`를 `(id, creatorId, yearMonth, status)` 만 보유하는 이벤트 마커로 축소.  
PAID 전환(`markAsPaid`) 시점에 계산값을 `SettlementLog`에 스냅샷으로 저장.  
Redis 의존성 전체 제거. PENDING·CONFIRMED는 실시간 계산, PAID만 SettlementLog에서 반환.

```
남은 문제
- CONFIRMED 상태도 조회할 때마다 sale_records를 다시 집계
- 배치 스케줄러가 confirm()을 일괄 처리하는 구조를 고려하면,
  confirm 이후 금액이 고정되어야 하는데 매번 재계산은 불필요하고 부정확할 수 있음
```

#### 최종 설계

`confirm()` 시점에 `Settlement`(마커) + `SettlementLog`(금액 스냅샷)를 동시 생성.  
`markAsPaid()`는 Settlement 상태만 PAID로 전환하고 기존 SettlementLog를 그대로 사용.

```
조회 분기 기준 (getMonthlySettlement)
- yearMonth >= 현재 월  →  항상 실시간 계산 (PENDING 반환, 배치 대상 아님)
- yearMonth <  현재 월  →  Settlement 있으면 SettlementLog에서 반환
                           Settlement 없으면 실시간 계산 (배치 전 fallback)
```

사후 환불이 발생하면 `SettlementLog`의 refundAmount·netAmount 등만 수정하는 방식으로 이력 관리.

### 2. UI용 creatorName 캐싱 이용

#### 초기 설계

userName을 엔티티에 필드값으로 입력

#### 문제

userName은 조회 메서드 등 필요한 변수가 아님에도 매개인자에 추가되고, 엔티티에서 들고 있어야함. 

필요시 프론트엔드 Store에 name 저장해야할 수도 있음. -> 또는 id로 레포지토리 조회 (db 접근 해야함)

### 최종 설계

userName **Spring Caffeine Cache** 이용한 캐싱 전략으로 변경
이름 변경 시 **evictCreatorName()** 메서드 호출 추가하여 캐시된 값 비우기



### 3. 월말 배치 스케줄러 설계 고려 (미구현, 구조 반영)

정산 시스템의 실무 흐름은 **월초 배치로 전월 전체 크리에이터를 일괄 confirm**하는 방식이 적합합니다.  
0원 크리에이터도 포함해야 감사 추적이 가능하고, 관리자가 "이번 달 정산 대상 전체 목록"을 뽑을 수 있습니다.

현재 `confirm()` API는 배치에서 반복 호출하는 것을 전제로 설계되어 있으며, 스케줄러 코드만 추가하면 됩니다. 현재 월 조회는 배치와 무관하게 항상 실시간 계산을 반환합니다.

### 4. 정산 확정(confirm) 시 당월 종료 여부 체크

`YearMonth.now().isAfter(targetYearMonth)` 조건으로 당월 또는 미래 월 확정을 차단. 아직 판매가 발생할 수 있는 월을 확정하면 이후 거래가 누락될 수 있기 때문.

### 5. SettlementQueryService.calculate()를 SettlementService에서 직접 호출

`calculate()`는 패키지 프라이빗 메서드로 선언. `SettlementService`와 동일 패키지에 위치하므로 접근 가능. confirm과 markAsPaid 모두 계산 로직을 재사용하기 위해 서비스 레이어 간 직접 호출.

### 6. Object[] 대신 JPQL 생성자 표현식으로 타입 안전한 집계 DTO 사용

초기 구현에서 JPQL 집계 쿼리 결과를 `Object[]`로 처리. `CreatorAggregationDto(String creatorId, BigDecimal totalAmount, Long count)` 레코드를 선언하고 JPQL `SELECT new ...()` 생성자 표현식으로 직접 매핑하도록 변경. 캐스팅 오류 위험 제거.

### 7. 크리에이터 이름 조회 — 루프 내 단건 호출 → 벌크 조회로 변경

관리자 집계 API(`getAdminAggregate`)에서 크리에이터별로 루프를 돌며 `getCreatorName(creatorId)`를 반복 호출하는 N+1 구조를 `getCreatorNames(Set<String>)`로 교체. `findAllById`로 한 번에 조회 후 Map으로 변환.

### 8. Caffeine 캐시와 @EnableCaching 위치

`@DataJpaTest` 슬라이스 컨텍스트에서 `@SpringBootApplication`에 `@EnableCaching`이 붙어 있으면 캐시 관련 빈이 로드되지 않아 오류가 발생. `@EnableCaching`을 `CacheConfig`로 이동하여 슬라이스 테스트와 캐시 설정을 분리.

### 9. SettlementReq DTO 제거 — @RequestParam 직접 수신

`confirm()` API가 `@RequestBody`로 DTO를 받는 구조에서 `@RequestParam`으로 변경. `yearMonth` 같은 단일 값들을 위해 별도 DTO를 만드는 것은 과한 추상화이며, `@RequestParam`으로 직접 받아 `YearMonth.parse()`로 변환하는 것이 더 간결.

---

## 미구현 / 제약사항
- **관리자 전체 정산 내역 조회 대상 - 모든 크리에이터(탈퇴 여부 필터링 X)**: 크리에이터를 findAll()로 조회하도록 구현. 추후 getAllCreatorNames()의 메서드명과 내부 findAll만 변경하면 softDelete 조건 추가 가능
- **크리에이터/강의 등록 API 없음**: 초기 데이터는 `DataInitializer`(서버 시작 시 더미 데이터 삽입)로만 제공됩니다.
- **SaleRecord PAID, CANCELLED로 상태 제한**: 결제 실패로 인한 판매 시도 이력을 저장 하려면 FAILED와 같은 값을 추가하면 좋겠으나, 결제 로직의 구조가 제시되지 않아 결제 확정 시 호출로 가정하고 paidAt을 생성시점에 stamp 되도록 하였습니다. 
- **IdGenerator 재시작 시 초기화**: 운영 환경에서는 UUID 또는 DB 시퀀스 기반 ID 전략 교체가 필요합니다.
- **SettlementLog 환불 후 수정 API 없음**: 설계는 되어 있으나 실제 수정 API는 구현되지 않았습니다.
- **커미션율 변경 이력 관리**: 환경변수로만 관리하며, git 태그 기반 이력 추적은 운영 규약으로만 존재합니다.

---


### 추가 구현 사항

####  1. 수수료 변경 이력 관리
- 환경변수로 관리하도록 만들어, 코드 리팩토링이 필요없고, 데이터는 스냅샷으로 찍어 이후 변경값이 과거 값에 영향이 없습니다.

- 커밋 후 git 태그 기능 이용하여 VCS에서 변경 이력 관찰이 쉽게 가능합니다. 

### 2. 현재 월 중간 정산 금액 확인 가능
- Settlement를 생성하고 계산값을 update하는 방식이 아닌, 계산 후 월말 배치로 Settlement와 SettlementLog를 생성합니다. 

- aggregate는 후생성 방식이 아니어도 필요한 과정이고, calculate 메서드 추가로 성능 저하는 미미하다고 판단하고 get 메서드 호출 시 즉시 계산하므로 현재 진행중인 월의 정산에 필요한 데이터도 조회 가능합니다.
- 관리자 정산 내역 집계도 월초월말이 아닌 상세 월일 지정으로 조회 가능합니다.

### 3. Money VO로 유효성 검증 추가

- 최대 강의 금액을 99만원으로 제한하여, 잘못된 입력 값이나, 강사 입력 실수로 인해 백만원 단위의 값이 요청으로 들어오면 예외를 반환합니다.


---

## AI 활용 범위

이 프로젝트는 Claude(Anthropic)와의 페어 프로그래밍으로 개발되었습니다. 

프로젝트 Directory를 DDD 스타일로 미리 생성 후 프롬프트로 요구사항을 정리하여 초기 파일을 작성하고, 결과물에서 개선점과 아키텍처 구조를 고려하여 방향성을 claude에게 제시하여 코드 리팩토링 진행하였습니다.

chore한 작업은 직접 작성 / 모든 설계에 주도적으로 지시하고 사용 기술 채택과 방식을 결정하였습니다.


아래는 대화를 통해 결정되거나 리팩토링된 주요 사항입니다.

- **Settlement 아키텍처 재설계**: 금액 필드를 Settlement에 저장하는 초기 구조의 문제를 제기하여, Settlement(마커) + SettlementLog(스냅샷) 분리 구조로 전환했습니다.
- **정산 확정 월 종료 체크**: "아직 판매가 발생할 수 있는 달에 정산을 확정하면 안 된다"는 비즈니스 제약을 추가했습니다.
- **N+1 크리에이터 이름 조회 개선**: 루프 내 단건 조회 구조를 발견하여 `getCreatorNames(Set)`으로 벌크 조회 방식으로 수정했습니다.
- **Object[] 타입 불안전 집계 개선**: JPQL 집계 결과를 `Object[]`로 처리하던 것을 `CreatorAggregationDto` 레코드 + 생성자 표현식으로 교체하도록 제안했습니다.
- **SettlementReq DTO 제거**: 단일 파라미터를 위한 불필요한 RequestBody DTO를 제거하고 `@RequestParam` 직접 수신으로 변경했습니다.
- **@EnableCaching 위치 조정**: `@DataJpaTest` 슬라이스 테스트에서 캐시 빈 누락 오류가 발생하여, `@EnableCaching`을 `CacheConfig`로 이동하는 방식으로 해결했습니다.
- **Spring Boot 4.x 패키지 변경 대응**: `@DataJpaTest` 관련 패키지가 변경된 것을 파악하여 테스트 코드를 수정했습니다.
- **코드 생성 범위**: 도메인 엔티티, 서비스 레이어, 레포지토리, 컨트롤러, 단위·슬라이스 테스트 코드 전반에 걸쳐 AI가 초안을 작성하고 사람이 검토·수정하는 방식으로 진행했습니다.
