# 크리에이터 정산 시스템

크리에이터가 운영하는 강의의 판매·취소 내역을 집계하여 월별 정산을 처리하는 백엔드 API 서버입니다.

---

## 프로젝트 개요

크리에이터(강사)가 보유한 강의의 판매 내역과 취소(환불) 내역을 기반으로 **월별 정산 금액을 계산하고, 데이터를 제공**합니다.
매월 1일과 15일에 각 정산 확정 및 지급 상태 관리를 자동으로 수행합니다.
**구매한 강의 취소 가능 기간을 15일로 채택**하여, 정산 확정 후에는 추가적인 정산금 변동이 없도록 정책을 설계하였습니다.

운영자는 특정 기간 내 **전체 크리에이터의 정산 현황을 집계하여 조회**할 수 있습니다.

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

### 방법 1. Docker Compose

PostgreSQL과 Spring Boot를 한 번에 실행합니다.

```bash
docker compose up --build
```

서버가 `http://localhost:8080`에서 기동됩니다. PostgreSQL은 `5432` 포트로 노출됩니다.

종료:

```bash
docker compose down
```

데이터 초기화:

```bash
docker compose down -v
```

### 방법 2. 로컬 직접 실행

#### 사전 준비

`.env.example`을 복사하여 `.env`를 생성하고 값을 채웁니다.

```
DB_URL=jdbc:postgresql://localhost:5432/creator_settlement
DB_USERNAME=postgres
DB_PASSWORD=postgres
COMMISSION_RATE=0.20
```

PostgreSQL이 로컬에서 실행 중이어야 합니다 (기본 포트: 5432).


## API 목록 및 예시

### 판매 내역 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/sale-record` | 판매 내역 등록 |
| `GET` | `/api/v1/sale-record?creatorId=creator-3&startDate=2025-01-31&endDate=2025-03-31` | 크리에이터 기간별 판매 내역 조회 |

### 취소 내역 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/cancel-record` | 취소 내역 등록 |

### 정산 (크리에이터용) API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/settlement/creator/{creatorId}?yearMonth=2026-03` | 크리에이터 월별 정산 조회 |

### 정산 (운영자용) API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/operator/settlement?startDate=2026-03-01&endDate=2026-03-31` | 기간 내 크리에이터 정산 현황 집계 조회 |
| `POST` | `/api/v1/operator/settlement/{settlementId}/pay` | 단건 정산 지급 처리 (CONFIRMED → PAID) |
| `POST` | `/api/v1/operator/settlement/bulk-pay?yearMonth=2026-03` | 월별 일괄 정산 지급 처리 (CONFIRMED → PAID) |

### 정산 (관리자용) API — 배치 스케줄러 실패 수동 처리용

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/admin/settlement/create` | 관리자 수동 Settlement 생성 (PENDING) |
| `POST` | `/api/v1/admin/settlement/confirm` | 관리자 수동 정산 확정 (PENDING → CONFIRMED) |

---

### API 호출 권한 관리

- 실서비스 사용 시 권한별 prefix로 Spring Security Config에서 정의하여 SecurityContext에 저장된 역할 체크가 필요합니다.
- 예) SecurityConfig.java: `.requestMatchers("/api/v1/operator/**").hasRole("OPERATOR")`

**Operator(운영자)** prefix: `/api/v1/operator/**` 
**Admin(관리자)** prefix: `/api/v1/admin/**` 

---

## API 상세 설명

### 판매 등록

```json
POST /api/v1/sale-record
Content-Type: application/json

{
  "courseId": "course-10",
  "studentId": "student-1",
  "amount": 99000,
  "paidAt": "2025-03-15T14:00:00"
}
```

응답 — 201 Created:

```json
{
  "id": "sale-10"
}
```

- `amount`는 최대 6자리 정수로 제한. 실질적으로 강의 가격(`course.price`) 이하여야 합니다.
- `amount`가 해당 강의의 등록 가격을 초과하면 `INVALID_SALE_RECORD_AMOUNT` 오류를 반환합니다.
- 동일한 `courseId + studentId` 조합은 DB unique 제약으로 중복 등록이 차단됩니다.

---

### 판매 내역 조회

```
GET /api/v1/sale-record?creatorId=creator-10&startDate=2025-03-01&endDate=2025-03-31
```

응답:

```json
{
  "content": [
    {
      "id": "sale-1",
      "courseId": "course-1",
      "studentId": "student-3",
      "amount": 30000,
      "paidAt": "2026-03-10T10:00:00",
      "status": "PAID"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

- `startDate`, `endDate`는 선택값입니다. 둘 다 없으면 해당 크리에이터의 전체 내역을 반환합니다.
- `startDate`만 있으면 해당일 이후 판매 내역을 반환합니다.
- `endDate`만 있으면 해당일까지의 판매 내역을 반환합니다.

---

### 취소(환불) 등록

```json
POST /api/v1/cancel-record
Content-Type: application/json

{
  "saleRecordId": "sale-10",
  "refundAmount": 50000,
  "cancelledAt": "2025-03-20T10:00:00"
}
```

응답 — 201 Created:

```json
{
  "id": "cancel-5",
  "saleRecordId": "sale-10",
  "refundAmount": 50000,
  "cancelledAt": "2025-03-20T10:00:00"
}
```

- 이미 취소된 판매 건에 재취소 시도 시 `ALREADY_CANCELLED` 오류를 반환합니다.
- `saleRecordRepository.findByIdWithPessimisticLock()` 비관적 락으로 동시 취소 요청을 제어합니다.
- `saleRecordId`에 unique 제약 — 동일한 판매 건에 대한 중복 취소 등록이 DB 수준에서 차단됩니다.
- 판매일(`paidAt`)이 현재 월보다 이전인 경우: 해당 월 `Settlement`의 `refundAmount`, `netAmount`, `settleAmount`를 재계산합니다.
- 이미 PAID 처리된 정산 건의 판매 내역은 취소 불가 (`ALREADY_PAID_SETTLEMENT`).

---

### 크리에이터 월별 정산 조회

```
GET /api/v1/settlement/creator/{creatorId}?yearMonth=2025-03
```

응답:

```json
{
  "creatorId": "creator-10",
  "creatorName": "강강사",
  "yearMonth": "2025-03",
  "status": "CONFIRMED",
  "totalAmount": 300000,
  "refundAmount": 50000,
  "netAmount": 250000,
  "commissionRate": 0.20,
  "commissionAmount": 50000,
  "expectedSettleAmount": 200000,
  "sellCount": 5,
  "cancelCount": 1
}
```

- `yearMonth` 형식: `yyyy-MM` (예: `2025-03`)
- 현재 월 조회 시: 항상 실시간 계산하여 `PENDING` 상태로 반환합니다 (DB 조회 없음).
- 과거 월 조회 시: DB에 `Settlement`가 있으면 해당 값을 반환, 없으면 실시간 계산하여 `PENDING`으로 반환합니다.

---

### 운영자 정산 집계 조회

```
GET /api/v1/operator/settlement?startDate=2025-03-01&endDate=2025-03-31
```

응답:

```json
{
  "entries": {
    "content": [
      {
        "creatorId": "creator-10",
        "creatorName": "윤강사",
        "settleAmount": 320000
      },
      {
        "creatorId": "creator-14",
        "creatorName": "마강사",
        "settleAmount": 480000
      }
    ],
    "page": 0,
    "size": 2,
    "totalElements": 2,
    "totalPages": 1
  },
  "totalSettlementAmount": 800000
}
```

---

### 운영자 단건 지급 처리 (CONFIRMED → PAID)

```
POST /api/v1/operator/settlement/{settlementId}/pay
```

- CONFIRMED 상태의 정산만 PAID로 전환 가능. PENDING 상태면 `INVALID_SETTLEMENT_STATUS` 에러 반환.
- 이미 PAID 상태이면 `ALREADY_PAID_SETTLEMENT` 에러 반환.
- `Settlement.@Version` 낙관적 락으로 동시 호출 시 `CONCURRENT_UPDATE_CONFLICT` 에러 반환.

---

### 운영자 일괄 지급 처리 (CONFIRMED → PAID)

```
POST /api/v1/operator/settlement/bulk-pay?yearMonth=2025-03
```

- `@Modifying(clearAutomatically = true)`로 해당 월 전체 `CONFIRMED` 정산을 `PAID`로 벌크 업데이트합니다.
- `updateCount = 0`이면 `NO_CONFIRMED_SETTLEMENTS` 에러 반환.
- 쿼리 타임아웃 발생 시 `QUERY_TIMEOUT` 에러 반환.
- DB 제약 조건 위반 시 `DB_CONSTRAINT_VIOLATION` 에러 반환.

---

### 관리자 정산 수동 생성 (PENDING)

```
POST /api/v1/admin/settlement/create
Content-Type: application/json

{
  "creatorId": "creator-10",
  "yearMonth": "2025-03"
}
```

응답 — 204 No Content

- PENDING 상태의 Settlement를 생성합니다. 배치 실패 등으로 누락된 경우 수동 처리용입니다.
- 현재 월 또는 미래 월 요청 시 `YEAR_MONTH_BAD_REQUEST` 에러 반환.

---

### 관리자 정산 수동 확정 (PENDING → CONFIRMED)

```
POST /api/v1/admin/settlement/confirm
Content-Type: application/json

{
  "settlementId": "settlement-10"
}
```

응답 — 204 No Content

- PENDING 상태의 Settlement를 CONFIRMED로 전환합니다.
- 이미 CONFIRMED 또는 PAID 상태면 `ALREADY_CONFIRMED_SETTLEMENT` 에러 반환.

---

## 데이터 모델

### 엔티티 관계

```
Creator
  └── Course (creatorId 참조)
        └── SaleRecord (courseId, studentId 참조)
              └── CancelRecord (saleRecordId 참조)

Settlement (월별 정산, creator_id + year_month unique 제약)
```

### 주요 엔티티

| 엔티티 | 테이블 | 설명 |
|---|---|---|
| `Creator` | `creators` | 크리에이터. id, name |
| `Course` | `courses` | 강의. id, creatorId, title, price (최대 99만원) |
| `SaleRecord` | `sale_records` | 판매 내역. id, courseId, studentId, amount, paidAt, status(PAID/CANCELLED). courseId+studentId 복합 unique. 취소 시 비관적 락 |
| `CancelRecord` | `cancel_records` | 취소 내역. id, saleRecordId unique — 동일 판매 건 중복 취소 불가. refundAmount, paidAt, cancelledAt |
| `Settlement` | `settlements` | 정산 내역. id, status(PENDING/CONFIRMED/PAID), totalAmount, refundAmount, netAmount, commissionRate, commissionAmount, settleAmount, sellCount, cancelCount. creator_id + year_month unique. `@Version` 낙관적 락 |

- `Settlement`는 수수료와 정산금의 실제 계산값(소수점 이하 4자리)을 저장합니다. API 응답 시에는 정수로 반환됩니다.

### Money 값 객체

- 모든 금액 계산은 `BigDecimal` 기반의 불변 `Money` 레코드를 사용합니다 (부동소수점 오차 방지).
- Null 체크 및 음수 결과 발생 시 `INVALID_MONEY_VALUE` 예외를 발생시킵니다.

### 정산 라이프사이클

```
PENDING  →  CONFIRMED  →  PAID
```

| 상태 | 생성 시점 | 설명 |
|---|---|---|
| `PENDING` | 매월 1일 00:05 배치 또는 수동 생성 | 정산 대기. 조회 시 항상 실시간 계산 반환 |
| `CONFIRMED` | 매월 15일 00:05 배치 또는 수동 확정 | 정산 확정. DB 저장값 반환 |
| `PAID` | 단건/일괄 지급 API 호출 | 지급 완료 |

### 수수료율

`COMMISSION_RATE` 환경변수로 관리합니다 (기본값 `0.20`). 변경 이력은 git 태그로 추적합니다.

수수료 계산 시 소수점 이하는 **버림(FLOOR)** 처리하여 크리에이터에게 유리하게 산정합니다.

---

## 요구사항 해석 및 설계 결정

### 정산 조회 분기 기준

```
yearMonth == 현재 월  →  항상 실시간 계산 (PENDING 반환, 배치 대상 아님)
yearMonth <  현재 월  →  Settlement 있으면 DB에서 반환
                         Settlement 없으면 실시간 계산 (배치 전 fallback)
```

### 월별 정산 대상 기간

- 판매 기준: `paidAt`, 취소 기준: `cancelledAt`으로 해당 월 시작~끝(LocalTime.MAX) 범위 조회.
- 취소 내역의 환불은 강의가 판매(결제)된 월의 Settlement에서 차감됩니다.

### 운영자/관리자 권한 분리

- **OPERATOR**: 정산 집계 조회, 지급 처리 담당
- **ADMIN**: 배치 실패 시 수동 생성/확정 담당
- 향후 Spring Security 적용 시 컨트롤러 단위로 권한 적용 가능하도록 분리되어 있습니다.

### 크리에이터 이름 캐싱

- 정산 엔티티에 이름을 저장하지 않고, 응답 시점에 **Caffeine Cache**를 통해 조회합니다.
- 이름 변경 시 `evictCreatorName()` 메서드로 캐시를 무효화합니다.

### 월별 배치 스케줄러 (Spring Batch)

`Reader → Processor → Writer` 구조로 전체 크리에이터 대상 전월 정산을 일괄 처리합니다.

| 컴포넌트 | 역할 |
|---|---|
| `MonthlySettlementScheduler` | `@Scheduled(cron = "0 5 0 1 * *")` — 매월 1일 00:05 KST에 PENDING 생성 Job 기동 |
| `MonthlySettlementBatchConfig` | `JpaPagingItemReader<Creator>` (chunk=50) + `faultTolerant().skip(Exception)` |
| `SettlementItemProcessor` | 이미 CONFIRMED/PAID 상태이면 `null` 반환으로 스킵 |
| `SettlementItemWriter` | `createPending()` → `confirmPending()` 순차 호출 |

`spring.batch.job.enabled=false`로 서버 기동 시 자동 실행을 방지하고 스케줄러 트리거에 의해서만 실행됩니다.

---

## 테스트

테스트는 H2 인메모리 DB(PostgreSQL 호환 모드)를 사용합니다. 외부 인프라 없이 실행 가능합니다.

### 테스트 레이어 구성

| 레이어 | 어노테이션 | 설명 |
|---|---|---|
| Repository | `@DataJpaTest` | H2 + JPA 슬라이스, 실제 쿼리 검증 |
| Service | `@ExtendWith(MockitoExtension)` | Mock 기반 단위 테스트, 비즈니스 로직 및 예외 시나리오 |
| Cache | `@SpringBootTest` (슬라이스) | Caffeine 실제 동작 검증 |

### 추가 시나리오

1)**판매 내역에 대한 중복 취소 등록 실패**: 같은 판매 내역에 대한 취소가 처리될 시 금액적 문제로 이어질 수 있으므로, unique 제약이 있긴하지만, 1차 방어선인 서비스 로직의 예외 처리를 검증하였습니다.

2)**결제일로부터 15일 초과 시 취소 기한 만료 - 취소 실패**: 가정한 전제에서 취소 가능 기간을 설정해놓았기 때문에, 15일 초과 시 기한 만료 예외가 발생하는 것을 검증하였습니다. 

3)**월경계 지난 취소 내역의 정산금 반영**: 지난달 발생한 판매 내역에 대한 취소도 15일 이내라면, 지난달 정산에서 환불금 반영을 검증했습니다.

4)**15일 이내 취소지만 정산이 이미 결제된 경우 취소 실패**: 배치 스케쥴러에 의해 정산 시스템이 정상 동작할 시 발생하면 안되는 케이스지만, 15일 전에 정산이 수동으로 지급된 경우(혹은 수동으로 취소 API를 호출했을 경우) `ALREADY_PAID_SETTLEMENT` 예외를 발생시키며 취소가 실패하는 걸 검증합니다. 사용자가 발생시킨 취소 요청일 경우 확인이 필요하기에 WARN 레벨 로그로 플래그를 남겼습니다. 

5)**전월 판매내역 취소 시 정산이 없으면 예외**: 배치 스케줄러에서 정산 생성이 실패한 건에 대하여, 관리자 수동 후처리 조치가 이루어지지 않았거나 진행 중일 시 들어오는 요청에 대해 `SETTLEMENT_NOT_FOUND`에러가 발생합니다. 마찬가지로, WARN 레벨 적용한 로그를 작성하여 모니터링 가능한 구조로 설계하였습니다.

6)**CreatorName 카페인 캐싱 정상 동작 확인**: UI에 필요한 월별 정산에서의 크리에이터명을 필드로 저장하거나 레포지토리로 조회하는 대신 Caffeine Cache를 이용하여 서버 가동 후 최초 1회만 레포지토리에서 조회, 이후는 캐시된 값을 불러오는 전략을 사용하였습니다. 테스트 코드로 CreatorQueryService의 getCreatorName을 2회 조회 시 레포지토리 접근 횟수가 1회인지 검증하였습니다.

7)**강의료보다 큰 판매 내역의 결제 금액 요청된 경우 실패**: 등록된 Course의 강의료와 비교하여 Request가 큰 값이 들어올 시 잘못된 요청이므로 `INVALID_SALE_RECORD_AMOUNT` 예외 발생을 검증합니다.

8)**현재 진행 중인 월별 정산 요청 시, 월초 ~ 오늘까지의 내역으로 실시간 계산**: 지난달 뿐만 아니라 이번 달로 크리에이터 월별 정산 API 호출 시 월 1일 0시부터의 판매 내역과 취소 내역을 집계하여 계산 후 반환합니다.

9)**미래 년월 요청 - 크리에이터 월별 정산**: 미래 년월로 요청 시 `YEAR_MONTH_BAD_REQUEST` 예외가 발생하는 것을 검증합니다. 잘못된 형식의 경우, 컨트롤러 단의 `YearMonth.parse()`에서 바로 `INVALID_DATE_TIME_FORM` 에러를 반환합니다.

10)**현재월 이후 요청 정산 생성 실패**: 정책이 매월 1일에 이전달 정산 생성이므로, `createPending()`에 현재/미래월이 요청으로 들어올 시 정산 생성 실패 & 예외 발생을 검증합니다. 실서비스에서 탈퇴하는 크리에이터 등 중간 정산이 필요할 경우 조건문을 수정하여 현재월도 포함가능합니다.

11)**이미 결제된 정산에 markAsPaid() - 실패**: 정산금 지급이 이루어져 PAID로 상태 변경된 경우, `markAsPaid()` 호출 시 `ALREADY_PAID_SETTLEMENT` 예외 발생을 검증합니다.

12)**확정되지 않은 정산에 markAsPaid() - 실패**: 아직 확정 처리가 되지 않은 정산에 `markAsPaid()` 메서드 호출 시 `INVALID_SETTLEMENT_STATUS` 예외 발생을 검증합니다.

13)**일괄 정산금 PAID 변경 - 확정 상태 정산 존재 X 시 실패**: UI에서 Settlement의 status를 따로 표시하지 않은 경우, 운영자가 브라우저에서 정산되지 않은 정산금이 존재하여 일괄 지급을 호출한 시나리오입니다. 확정된 상태의 정산이 없다면 `NO_CONFIRMED_SETTLEMENTS`를 반환합니다. 이는 결국 정산 생성/확정이 안된 상태를 의미합니다. (실시간 계산에서 SettlementStatus.PENDING 반환) 서버 데이터 정합성 이슈 발견 지점을 곳곳에 넣어두었습니다.

---

## 미구현 / 제약사항

- **creatorId 요청 파라미터 수신**: Spring Security 적용 시 `@AuthenticationPrincipal`로 대체 필요합니다.
- **크리에이터/강의 등록 API 없음**: 초기 데이터는 `DataInitializer`(서버 시작 시 더미 데이터 삽입)로만 제공됩니다.
- **IdGenerator 재시작 시 초기화**: `AtomicLong` 기반으로 재시작 시 초기화됩니다. 운영 환경에서는 UUID 또는 DB 시퀀스 기반 ID 전략 교체가 필요합니다.
- **배치 스킵 정책**: `skip(BusinessException.class).skipLimit(10)`으로 비즈니스 레벨에서 예외가 발생하는 개별 크리에이터 실패가 전체 배치를 중단시키지 않습니다.
- **정산 확정 이후 취소 불가**: 정산금 결제 이후 취소/환불 가능 시 환불금이 이월되어야 하거나 크리에이터 -> 플랫폼으로 출금하는 상황이 필요해지므로, 확정 레벨 이후 추가 환불이 불가한 정책을 기반으로 개발했습니다.

---

## 아키텍처 진화 기록

### Settlement 아키텍처 — 금액 직접 저장 → 마커 + SettlementRecord → 금액 직접 저장

> 아키텍처 전환 규모가 커서 branch 분리 후 병합하였습니다.

**초기 설계**

`Settlement` 하나에 `totalAmount`, `refundAmount`, `netAmount` 등 금액을 모두 저장. PENDING 계산 결과는 Redis TTL 1시간 캐싱.

```
문제점
- 판매·취소 발생마다 Settlement를 UPDATE해야 하는 구조적 결합
- PENDING은 언제든 달라지므로 1시간 TTL 캐시는 일관성 보장 불가
- 정산 확정 시점과 금액 마지막 갱신 시점 불일치 가능
```

**중간 과정**

`Settlement`를 `(id, creatorId, yearMonth, status)` 이벤트 마커로 축소. PAID 전환 시 계산값을 `SettlementRecord`에 스냅샷 저장. Redis 의존성 전체 제거.

```
남은 문제
- CONFIRMED 상태도 조회마다 sale_records를 다시 집계
- confirm 이후 금액이 고정되어야 하는데 매번 재계산은 불필요하고 부정확
```

**최종 설계 (현재)**

`Settlement`에 금액 스냅샷을 직접 저장하는 방식으로 회귀. `confirm()` 시점에 계산값을 Settlement에 기록하고, 이후에는 DB 저장값을 반환. `SettlementRecord`를 별도 테이블로 유지하던 구조를 제거하여 단순화.

```
조회 분기 (getMonthlySettlement)
- yearMonth >= 현재 월  →  항상 실시간 계산 (PENDING 반환)
- yearMonth <  현재 월  →  Settlement 있으면 DB 저장값 반환
                           Settlement 없으면 실시간 계산 (배치 전 fallback)
```

### PENDING 두 단계 생성 — 확정 실패 시 이력 보존

기존에는 Settlement 생성과 동시에 CONFIRMED로 전환했습니다. confirm 과정에서 예외 발생 시 이력이 남지 않는 문제가 있어, `create()` 시점에 PENDING으로 생성하고 `confirmPending()`으로 상태를 전환하는 두 단계 구조로 변경했습니다. 확정 실패 시에도 PENDING 레코드가 남아 재처리 추적이 가능합니다.

### N+1 크리에이터 이름 조회 개선

운영자 집계 API에서 크리에이터별 단건 조회 구조를 `getAllCreatorNames()`로 교체하여 `findAll()`로 한 번에 조회 후 Map으로 변환합니다.

### JPQL 생성자 표현식으로 타입 안전한 집계 DTO

JPQL 집계 결과를 `Object[]`로 처리하던 구조를 `CreatorAggregationDto` 레코드 + `SELECT new ...()` 생성자 표현식으로 교체하여 캐스팅 오류 위험을 제거했습니다.

---

## AI 활용 범위

이 프로젝트는 Claude(Anthropic)와의 페어 프로그래밍으로 개발되었습니다.

프로젝트 Directory를 DDD 스타일로 미리 생성 후 요구사항을 정리하여 초기 파일을 작성하고, 결과물에서 개선점과 아키텍처 구조를 고려하여 방향성을 제시하며 코드 리팩토링을 진행하였습니다.

모든 설계에 주도적으로 지시하고 사용 기술 채택과 방식을 결정하였습니다.
