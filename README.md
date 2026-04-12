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
| 배치 | Spring Batch |
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
- `amount`가 해당 강의의 등록 가격(`course.price`)을 초과하면 `INVALID_SALE_RECORD_AMOUNT` 오류를 반환합니다.
- 동일한 `courseId + studentId` 조합은 DB unique 제약으로 중복 등록이 차단됩니다.

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
- `saleRecordId`에 unique 제약 — 동일한 판매 건에 대한 중복 취소 등록이 DB 수준에서 차단됩니다.

---

### 정산 (크리에이터)

#### 월별 정산 조회

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
  "totalAmount": 300000,
  "refundAmount": 50000,
  "netAmount": 250000,
  "commissionRate": 0.20,
  "commissionAmount": 50000,
  "expectedSettleAmount": 200000,
  "sellCount": 3,
  "cancelCount": 1
}
```

- `PENDING`: 정산 확정 전 — 현재 월 포함, 항상 실시간 계산
- `CONFIRMED`: 정산 확정됨 — confirm 시점 SettlementRecord 스냅샷에서 조회
- `PAID`: 지급 완료 — 동일 SettlementRecord 스냅샷에서 조회
- 금액 필드는 소수점 trailing zeros를 제거한 정수형으로 반환합니다.

---

### 정산 (관리자)

> 정산 생성·확정·지급 처리는 모두 관리자 전용 엔드포인트로 분리되었습니다.

#### 관리자 정산 집계 조회

```
GET /api/v1/admin/settlement?startDate=2025-03-01&endDate=2025-03-31
```

응답:

```json
{
  "entries": [
    {
      "creatorId": "creator-10",
      "creatorName": "홍길동",
      "totalAmount": 500000,
      "refundAmount": 100000,
      "netAmount": 400000,
      "commissionAmount": 80000,
      "expectedSettleAmount": 320000,
      "sellCount": 5,
      "cancelCount": 1
    }
  ],
  "totalSettlementAmount": 320000
}
```

#### 관리자 정산 수동 생성 (PENDING)

```
POST /api/v1/admin/settlement/create
Content-Type: application/json

{
  "creatorId": "creator-10",
  "yearMonth": "2025-03"
}
```

- PENDING 상태의 Settlement를 생성합니다. 배치 실패 등으로 누락된 경우 수동 처리용입니다.

#### 관리자 정산 수동 확정 (PENDING → CONFIRMED)

```
POST /api/v1/admin/settlement/confirm
Content-Type: application/json

{
  "creatorId": "creator-10",
  "yearMonth": "2025-03"
}
```

- 내부적으로 `createPending()` → `confirmPending()` 순서로 실행됩니다.
- 해당 월이 완전히 종료되지 않으면 확정 불가 (`SETTLEMENT_MONTH_NOT_ENDED`)
- 이미 CONFIRMED 또는 PAID 상태면 중복 확정 불가 (`ALREADY_CONFIRMED_SETTLEMENT`)

#### 관리자 지급 처리 (CONFIRMED → PAID)

```
POST /api/v1/admin/settlement/{settlementId}/pay
```

- CONFIRMED 상태의 정산만 PAID로 전환 가능 (`INVALID_STATUS_TO_SETTLE`)
- 이미 PAID 상태이면 `ALREADY_PAID_SETTLEMENT` 오류 반환
- `Settlement.@Version` 낙관적 락으로 동시 호출 시 `CONCURRENT_UPDATE_CONFLICT` 오류 반환
- 금액 스냅샷은 confirm 시점에 이미 생성된 `SettlementRecord`를 그대로 사용

---

## 데이터 모델 설명

```
Creator
  └── Course (1:N, creatorId FK)
        └── SaleRecord (1:N, courseId FK)
              └── CancelRecord (1:1, saleRecordId FK)

Settlement (월별 정산 마커, creator_id + year_month unique)
  └── SettlementRecord (PAID 시점 스냅샷, settlementId FK)
```

### 주요 엔티티

| 엔티티 | 테이블 | 설명 |
|---|---|---|
| `Creator` | `creators` | 크리에이터. id, name |
| `Course` | `courses` | 강의. creatorId FK, price (최대 99만원) |
| `SaleRecord` | `sale_records` | 판매 내역. amount, paidAt, status(PAID/CANCELLED). courseId+studentId 복합 unique. 취소 시 비관적 락 |
| `CancelRecord` | `cancel_records` | 취소 내역. saleRecordId FK unique — 동일 판매 건 중복 취소 불가. refundAmount, cancelledAt |
| `Settlement` | `settlements` | 정산 이벤트 마커. status(PENDING/CONFIRMED/PAID), creator_id+year_month unique. `@Version` 낙관적 락으로 markAsPaid 동시 호출 방지 |
| `SettlementRecord` | `settlement_records` | confirm 시점 금액 스냅샷. creator_id+year_month unique, settlement_id unique. 사후 환불 발생 시 수정 가능 |

### Money 값 객체

`BigDecimal`을 래핑한 불변 레코드입니다. scale=2(HALF_UP)를 강제하며, 음수 결과가 나오는 연산 시 `BusinessException`을 발생시킵니다.

### 수수료율

`app.commission-rate` 환경변수로 관리합니다(기본값 0.20). 변경 이력은 git 태그로 추적하는 것을 전제로 합니다.

---

## 요구사항 해석 및 가정

- **정산 대상 기간**: 판매 기준 `paidAt`, 취소 기준 `cancelledAt`을 사용하여 해당 월의 시작~끝(LocalTime.MAX)으로 범위 조회합니다.
- **판매 금액 제약**: `@Digits(integer=6, fraction=2)` + DB `precision=8, scale=2`로 최대 999,999.99원으로 제한합니다. 실제 강의료는 price 컬럼 기준 최대 99만원으로 간주합니다.
- **수수료 소수점 절사 방식 채택**: 소수점 단위의 수수료율(ex. 12.5%) 또한 상관 없이 적용 가능합니다. 현재 프로젝트에서는 계산된 수수료에서 소수점을 절사하는 방식을 선택하여 정산금을 계산합니다.
- **크리에이터 이름 조회**: 정산 엔티티에 이름을 저장하지 않고, 응답 시점에 캐시(`Caffeine`)를 통해 조회합니다.
- **정산 PENDING 상태**: DB에 별도 레코드를 생성하지 않고, 조회 시 실시간으로 계산하여 반환합니다.
- **ID 생성**: `AtomicLong` 기반 `IdGenerator`를 사용하여 String값을 생성합니다. 재시작 시 초기화됩니다. 운영 환경에서는 UUID로 변경필요합니다.

---

## 설계 결정과 이유

### 1. Settlement 아키텍처 진화 — 금액 직접 저장 → 마커 + SettlementRecord 스냅샷
#### * 아키텍쳐 전환 규모가 커서 branch 분리하여 개발 후 병합하였습니다.

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
PAID 전환(`markAsPaid`) 시점에 계산값을 `SettlementRecord`에 스냅샷으로 저장.  
Redis 의존성 전체 제거. PENDING·CONFIRMED는 실시간 계산, PAID만 SettlementRecord에서 반환.

```
남은 문제
- CONFIRMED 상태도 조회할 때마다 sale_records를 다시 집계
- 배치 스케줄러가 confirm()을 일괄 처리하는 구조를 고려하면,
  confirm 이후 금액이 고정되어야 하는데 매번 재계산은 불필요하고 부정확할 수 있음
```

#### 최종 설계

`confirm()` 시점에 `Settlement`(마커) + `SettlementRecord`(금액 스냅샷)를 동시 생성.  
`markAsPaid()`는 Settlement 상태만 PAID로 전환하고 기존 SettlementRecord를 그대로 사용.

```
조회 분기 기준 (getMonthlySettlement)
- yearMonth >= 현재 월  →  항상 실시간 계산 (PENDING 반환, 배치 대상 아님)
- yearMonth <  현재 월  →  Settlement 있으면 SettlementRecord에서 반환
                           Settlement 없으면 실시간 계산 (배치 전 fallback)
```

사후 환불이 발생하면 `SettlementRecord`의 refundAmount·netAmount 등만 수정하는 방식으로 이력 관리.

### 2. UI용 creatorName 캐싱 이용

#### 초기 설계

userName을 엔티티에 필드값으로 입력

#### 문제

userName은 조회 메서드 등 필요한 변수가 아님에도 매개인자에 추가되고, 엔티티에서 들고 있어야함. 

필요시 프론트엔드 Store에 name 저장해야할 수도 있음. -> 또는 id로 레포지토리 조회 (db 접근 해야함)

### 최종 설계

userName **Spring Caffeine Cache** 이용한 캐싱 전략으로 변경
이름 변경 시 **evictCreatorName()** 메서드 호출 추가하여 캐시된 값 비우기



### 3. SettlementRecordService 분리 — 계산·로그 생성 책임 분리

기존 `SettlementService`에서 금액 계산(`calculate`)과 `SettlementRecord` 생성 로직을 `SettlementRecordService`로 분리했습니다.

- `SettlementService`: Settlement 엔티티의 상태 전환(createPending, confirmPending, markAsPaid) 담당
- `SettlementRecordService`: 판매·취소 내역 집계 계산 + SettlementRecord 생성 담당
- `SettlementQueryService`: 조회 전용 (상태별 분기 후 실시간 계산 또는 SettlementRecord 조회)

배치(`SettlementItemWriter`)도 `SettlementService`만 의존하여 호출합니다.

### 4. Settlement.create() 기본 상태 PENDING — 확정 실패 시에도 이력 보존

기존에는 Settlement 생성과 동시에 CONFIRMED로 전환했습니다. confirm 과정에서 예외가 발생하면 이력이 남지 않는 문제가 있어, `create()` 시점에 PENDING으로 생성하고 이후 `confirmPending()`으로 상태를 전환하는 두 단계 구조로 변경했습니다. 확정 실패 시에도 PENDING 레코드가 남아 재처리 추적이 가능합니다.

### 5. 월말 배치 스케줄러 (Spring Batch)

**월초(매월 1일 00:05 KST) 배치로 전월 전체 크리에이터를 일괄 confirm**하는 방식을 사용합니다.
판매금액 0원 크리에이터도 포함해야 감사 추적이 가능하고, 관리자가 "이번 달 정산 대상 전체 목록"을 뽑을 수 있습니다.

Spring Batch `Reader → Processor → Writer` 구조로 구현되었습니다.

| 컴포넌트 | 역할 |
|---|---|
| `MonthlySettlementScheduler` | `@Scheduled(cron = "0 5 0 1 * *")` — 매월 1일 00:05 KST에 Job 기동 |
| `MonthlySettlementBatchConfig` | `JpaPagingItemReader<Creator>` (chunk=50) + `faultTolerant().skip(Exception)` |
| `SettlementItemProcessor` | 이미 CONFIRMED/PAID 상태이면 `null` 반환으로 스킵 |
| `SettlementItemWriter` | `createPending()` → `confirmPending()` 순차 호출 |
| `SettlementBatchItem` | `creatorId + yearMonth` 전달용 DTO |

`spring.batch.job.enabled=false`로 서버 기동 시 자동 실행을 방지하고, 스케줄러 트리거에 의해서만 실행됩니다. 현재 월 조회는 배치와 무관하게 항상 실시간 계산을 반환합니다.

### 6. 정산 생성 시 당월 종료 여부 체크

`!YearMonth.now().isAfter(targetYearMonth)` 조건으로 당월 또는 미래 월 정산 생성을 차단.

### 7. AdminSettlementRes 팩토리 메서드 패턴 — Money → BigDecimal 변환 책임 분리

`AdminSettlementQueryService`에서 직접 `totalAmount.amount()`를 호출해 `BigDecimal`을 뽑아 생성자에 전달하던 방식에서, `AdminSettlementRes.from()` / `CreatorSettlementEntry.of()` 정적 팩토리 메서드로 변환 책임을 DTO 안으로 이동했습니다. `stripTrailingZeros()`로 소수점 trailing zeros를 제거하고, 음수 scale 시 `setScale(0)` 처리를 내부에서 통일합니다.

### 8. Object[] 대신 JPQL 생성자 표현식으로 타입 안전한 집계 DTO 사용

초기 구현에서 JPQL 집계 쿼리 결과를 `Object[]`로 처리. `CreatorAggregationDto(String creatorId, BigDecimal totalAmount, Long count)` 레코드를 선언하고 JPQL `SELECT new ...()` 생성자 표현식으로 직접 매핑하도록 변경. 캐스팅 오류 위험 제거.

### 9. 크리에이터 이름 조회 — 루프 내 단건 호출 → 벌크 조회로 변경

관리자 집계 API(`getAdminAggregate`)에서 크리에이터별로 루프를 돌며 `getCreatorName(creatorId)`를 반복 호출하는 N+1 구조를 `getCreatorNames(Set<String>)`로 교체. `findAllById`로 한 번에 조회 후 Map으로 변환.

### 10. 관리자 정산 컨트롤러 분리 — 책임 경계 명확화

기존 `SettlementController`에 있던 `confirm`, `markAsPaid` 엔드포인트를 `AdminSettlementController`로 이동했습니다. 크리에이터용 컨트롤러는 조회(`GET`)만 담당하고, 상태 전환은 관리자 전용으로 분리하여 향후 Spring Security 권한 적용 범위를 명확히 합니다.

---

## 미구현 / 제약사항
- **SettlementController의 월별 정산 내역 조회, creatorId 값을 요청으로 받아서 처리**: Spring Security 검증 시 @Authentication과 같은 저장된 로그인 컨텍스트 값으로 변경 필요합니다.
- **관리자 권한으로 크리에이터 정산 가능**: 크리에이터에게 정산금을 지불하는 계정은 관리자라고 전제, 혹여 관리자가 아닌 직원 권한이 있다면, 권한 수정 필요
- **관리자 전체 정산 내역 조회 대상 - 모든 크리에이터(탈퇴 여부 필터링 X)**: 크리에이터를 findAll()로 조회하도록 구현. 추후 getAllCreatorNames()의 메서드명과 내부 findAll만 변경하면 softDelete 조건 추가 가능
- **크리에이터/강의 등록 API 없음**: 초기 데이터는 `DataInitializer`(서버 시작 시 더미 데이터 삽입)로만 제공됩니다.
- **SaleRecord PAID, CANCELLED로 상태 제한**: 결제 실패로 인한 강의 구매 시도 이력을 저장 하려면 FAILED와 같은 값을 추가하면 좋겠으나, 결제 로직의 구조가 제시되지 않아 결제 확정 시 판매 내역 생성 메서드 호출로 가정하고 paidAt을 생성시점에 stamp 되도록 하였습니다. 추후 이력을 남기고 싶으면 FAILED status 추가나 별도 Record 추가로 관리할 수 있습니다.
- **IdGenerator 재시작 시 초기화**: 운영 환경에서는 UUID 또는 DB 시퀀스 기반 ID 전략 교체가 필요합니다.
- **수수료 변경 이력 관리**: application.yml에서 불러와서 사용하며, git 태그 기반 이력 추적은 운영 규약으로만 존재합니다.
- **배치 스킵 정책**: `skip(Exception.class).skipLimit(MAX_VALUE)`로 개별 크리에이터 실패가 전체 배치를 중단시키지 않도록 설정. 운영 환경에서는 스킵된 항목에 대한 알림/모니터링 연동 필요합니다.

---


### 추가 구현 사항

#### 1. 수수료 변경 이력 관리
- 환경변수로 관리하도록 만들어, 코드 리팩토링이 필요없고, 데이터는 스냅샷으로 찍어 이후 변경값이 과거 값에 영향이 없습니다.
- 커밋 후 git 태그 기능 이용하여 VCS에서 변경 이력 관찰이 쉽게 가능합니다.

#### 2. 현재 월 중간 정산 금액 확인 가능
- Settlement를 생성하고 계산값을 update하는 방식이 아닌, 계산 후 다음 월초 배치로 Settlement와 SettlementRecord를 생성합니다.
- aggregate는 후생성 방식이 아니어도 필요한 과정이고, calculate 메서드 추가로 성능 저하는 미미하다고 판단하고 get 메서드 호출 시 즉시 계산하므로 현재 진행중인 월의 정산에 필요한 데이터도 조회 가능합니다.
- 관리자 정산 내역 집계도 월초월말이 아닌 상세 월일 지정으로 조회 가능합니다.

#### 3. Money VO로 유효성 검증 추가
- 최대 강의 금액을 99만원으로 제한하여, 잘못된 입력 값이나, 강사 입력 실수로 인해 백만원 단위의 값이 요청으로 들어오면 예외를 반환합니다.

#### 4. Spring Batch 월별 정산 자동화
- `Reader → Processor → Writer` 구조로 전체 크리에이터를 대상으로 전월 정산을 일괄 처리합니다.
- Processor에서 이미 확정/지급된 크리에이터를 필터링(`null` 반환)하여 중복 처리를 방지합니다.
- `faultTolerant().skip(Exception)` 설정으로 개별 크리에이터 실패가 전체 배치를 중단시키지 않습니다.
- `spring.batch.job.enabled=false`로 서버 기동 시 자동 실행을 방지하고, 스케줄러에 의해서만 실행됩니다.


---

## AI 활용 범위

이 프로젝트는 Claude(Anthropic)와의 페어 프로그래밍으로 개발되었습니다. 

프로젝트 Directory를 DDD 스타일로 미리 생성 후 프롬프트로 요구사항을 정리하여 초기 파일을 작성하고, 결과물에서 개선점과 아키텍처 구조를 고려하여 방향성을 claude에게 제시하여 코드 리팩토링 진행하였습니다.

chore한 작업은 직접 작성 / 모든 설계에 주도적으로 지시하고 사용 기술 채택과 방식을 결정하였습니다.


아래는 대화를 통해 결정되거나 리팩토링된 주요 사항입니다.

- **Settlement 아키텍처 재설계**: 금액 필드를 Settlement에 저장하는 초기 구조의 문제를 제기하여, Settlement(마커) + SettlementRecord(스냅샷) 분리 구조로 전환했습니다.
- **정산 확정 월 종료 체크**: "아직 판매가 발생할 수 있는 달에 정산을 확정하면 안 된다"는 비즈니스 제약을 추가했습니다.
- **N+1 크리에이터 이름 조회 개선**: 루프 내 단건 조회 구조를 발견하여 `getCreatorNames(Set)`으로 벌크 조회 방식으로 수정했습니다.
- **Object[] 타입 불안전 집계 개선**: JPQL 집계 결과를 `Object[]`로 처리하던 것을 `CreatorAggregationDto` 레코드 + 생성자 표현식으로 교체하도록 제안했습니다.
- **SettlementReq DTO 제거**: 단일 파라미터를 위한 불필요한 RequestBody DTO를 제거하고 `@RequestParam` 직접 수신으로 변경했습니다.
- **@EnableCaching 위치 조정**: `@DataJpaTest` 슬라이스 테스트에서 캐시 빈 누락 오류가 발생하여, `@EnableCaching`을 `CacheConfig`로 이동하는 방식으로 해결했습니다.
- **Spring Boot 4.x 패키지 변경 대응**: `@DataJpaTest` 관련 패키지가 변경된 것을 파악하여 테스트 코드를 수정했습니다.
- **Spring Batch 월별 정산 자동화**: `Reader → Processor → Writer` 구조 설계 및 구현. Processor의 중복 처리 방지 필터링, `faultTolerant` 스킵 정책, 스케줄러와의 연동 방식을 포함합니다.
- **AdminSettlementRes 팩토리 메서드 도입**: Money→BigDecimal 변환과 trailing zeros 정리 책임을 서비스에서 DTO 안으로 이동하는 리팩토링을 제안하고 구현했습니다.
- **SettlementRecordService 분리**: 금액 계산과 SettlementRecord 생성 로직을 SettlementService에서 분리하여 단일 책임 원칙을 적용했습니다.