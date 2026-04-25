# Tiggle Backend — 개발 주요 내역

---

## 1. 프로젝트 기술 스택

| 항목 | 내용 |
|------|------|
| Framework | Spring Boot 3.5.x |
| Language | Java 17 |
| DB | PostgreSQL (`jdbc:postgresql://192.168.77.9:15432/tiggle`) |
| ORM | Spring Data JPA / Hibernate |
| 인증 | JWT (Access Token + Refresh Token) |
| 빌드 | Gradle |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |

---

## 2. DB 테이블 목록

| 테이블명 | 엔티티 | 설명 |
|----------|--------|------|
| `tb_user_info` | `User` | 사용자 계정 |
| `tb_refreshtoken_info` | `RefreshToken` | JWT Refresh Token |
| `tb_user_log` | `UserLog` | 사용자 행동 로그 |
| `tb_user_access_log` | `UserAccessLog` | 접근 로그 |
| `tb_kiwoom_credential` | `KiwoomCredential` | 키움 API 인증정보 (암호화 저장) |

### tb_kiwoom_credential DDL
```sql
CREATE TABLE tb_kiwoom_credential (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL UNIQUE,
    appkey     VARCHAR(500) NOT NULL,   -- AES-256-GCM 암호화값
    secretkey  VARCHAR(500) NOT NULL,   -- AES-256-GCM 암호화값
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_kc_user FOREIGN KEY (user_id)
        REFERENCES tb_user_info(id) ON DELETE CASCADE
);
```

---

## 3. 인증 구조 (JWT)

- **Access Token**: 1시간 유효 (`jwt.expiration-ms=3600000`)
- **Refresh Token**: DB 저장, 로테이션 방식
- **로그인**: `POST /api/v1/login`
- **토큰 갱신**: `POST /api/v1/auth/refresh`
- **로그아웃**: `POST /api/v1/auth/logout`
- 관리자 계정은 앱 시작 시 `DataInitializer`가 자동 생성 (`@Profile("local")`)

---

## 4. 키움증권 REST API 연동

### 공통 호출 규격

| 항목 | 내용 |
|------|------|
| Base URL (운영) | `https://api.kiwoom.com` |
| Base URL (모의) | `https://mockapi.kiwoom.com` |
| Method | POST |
| Content-Type | `application/json;charset=UTF-8` |
| 인증 헤더 | `authorization: Bearer {접근토큰}` |
| TR 지정 헤더 | `api-id: {TR코드}` |
| 페이지네이션 헤더 | `cont-yn: Y`, `next-key: {키}` (응답 헤더로 수신 후 다음 요청에 사용) |

### 4-0. 종목정보 조회

키움 REST API 경로: `POST /api/dostk/stkinfo`

**단일 종목 기본정보 (ka10001)**

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| `stk_cd` | 종목코드 (예: 005930) | Y |

**응답 주요 필드**

| 필드 | 설명 |
|------|------|
| `stk_nm` | 종목명 |
| `mrkt_tp` | 시장구분 (1=코스피, 2=코스닥, 3=코넥스) |
| `upjong_cd` | 업종코드 |
| `upjong_nm` | 업종명 |
| `face_val` | 액면가 |
| `lst_stk_qty` | 상장주식수 |
| `capital` | 자본금 |
| `ipo_dt` | 상장일 YYYYMMDD |
| `settle_month` | 결산월 |

**시장별 전체 종목 리스트**

> ⚠️ TR코드 미확정 — 키움 포털(openapi.kiwoom.com) 종목정보(01) 카테고리에서 확인 후
> `KiwoomStockService.java` 의 `TR_STOCK_LIST` 상수를 교체할 것.

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| `mrkt_tp` | 시장구분 (1=코스피, 2=코스닥) | Y |

응답에 `cont_yn=Y` 이면 `next_key` 를 다음 요청 헤더에 담아 페이지네이션 진행.

---


### 4-1. 인증 구조

키움 REST API는 **OAuth2 Client Credentials** 방식을 사용한다.

```
appkey + secretkey  →  접근토큰(Bearer) 발급
접근토큰  →  주문 API 호출 시 Authorization 헤더에 사용
```

- appkey/secretkey는 **계좌 보유자 본인**이 키움 OpenAPI 사이트에서 발급
- 한 appkey로는 해당 계좌에서만 거래 가능 (타인 계좌 접근 불가)
- 우리 시스템은 **사용자마다 개별 appkey/secretkey를 등록**하여 각자의 계좌로 거래

### 4-2. 토큰 관리

| 항목 | 내용 |
|------|------|
| 발급 API | `POST https://api.kiwoom.com/oauth2/token` |
| 폐기 API | `POST https://api.kiwoom.com/oauth2/revoke` |
| 토큰 저장 | 서버 메모리 (ConcurrentHashMap, userId 키) |
| 자동 갱신 | 30분마다 만료 1시간 이내 토큰 자동 재발급 (`@Scheduled`) |
| 서버 재시작 | 재시작 시 토큰 소멸 → 주문 호출 시 자동 재발급 |

**우리 서버 엔드포인트**

```
POST   /api/v1/kiwoom/token/issue    # 수동 발급 (인증 필요)
DELETE /api/v1/kiwoom/token/revoke   # 토큰 폐기 (인증 필요)
```

### 4-3. 키움 인증정보 관리

사용자가 자신의 appkey/secretkey를 등록·관리하는 엔드포인트.

```
PUT    /api/v1/kiwoom/credential      # 등록 또는 수정
DELETE /api/v1/kiwoom/credential      # 삭제
GET    /api/v1/kiwoom/credential/status  # 등록 여부 확인
```

- appkey/secretkey는 **AES-256-GCM으로 암호화**하여 DB에 저장
- 조회 시 자동 복호화 (JPA `AttributeConverter` 적용)

### 4-4. 신용주문 API

| 기능 | TR 코드 | 우리 서버 엔드포인트 |
|------|---------|-------------------|
| 신용 매수주문 | `kt10006` | `POST /api/v1/credit-orders/buy` |
| 신용 매도주문 | `kt10007` | `POST /api/v1/credit-orders/sell` |
| 신용 정정주문 | `kt10008` | `POST /api/v1/credit-orders/modify` |
| 신용 취소주문 | `kt10009` | `POST /api/v1/credit-orders/cancel` |

**키움 서버 엔드포인트**: `POST https://api.kiwoom.com/api/dostk/ordr`

**신용 매수 요청 파라미터 (kt10006)**

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| `dmstStexTp` | 국내거래소구분 (KRX/NXT/SOR) | Y |
| `stkCd` | 종목코드 | Y |
| `ordQty` | 주문수량 | Y |
| `trdeTp` | 매매구분 (0=보통, 3=시장가 등) | Y |
| `ordUv` | 주문단가 | N |
| `condUv` | 조건단가 | N |

**신용 매도 추가 파라미터 (kt10007)**

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| `crdDealTp` | 신용거래구분 (33=융자, 99=융자합) | Y |
| `crdLoanDt` | 대출일 YYYYMMDD (융자=33 시 필수) | 조건부 |

**정정 파라미터 (kt10008)**

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| `origOrdNo` | 원주문번호 | Y |
| `mdfyQty` | 정정수량 | Y |
| `mdfyUv` | 정정단가 | Y |

**취소 파라미터 (kt10009)**

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| `origOrdNo` | 원주문번호 | Y |
| `cnclQty` | 취소수량 (0=잔량전체) | Y |

---

## 5. AES-256-GCM 암호화

| 항목 | 내용 |
|------|------|
| 알고리즘 | AES-256-GCM (인증 암호화) |
| 키 길이 | 32 bytes |
| IV | 암호화마다 랜덤 12 bytes 생성 |
| 저장 포맷 | `Base64(IV[12] \|\| CipherText \|\| GCM_TAG[16])` |
| 적용 대상 | `tb_kiwoom_credential.appkey`, `secretkey` |
| 자동 처리 | `EncryptedStringConverter` (JPA AttributeConverter) |

**키 생성**
```bash
openssl rand -base64 32
```

**환경변수**
```bash
export AES_ENCRYPTION_KEY=<생성된_32바이트_Base64_키>
```

> 키 분실 시 DB의 모든 암호화된 자격증명 복구 불가. 키는 반드시 안전하게 보관.

---

## 6. 필수 환경변수

| 환경변수 | 설명 | 예시 |
|----------|------|------|
| `AES_ENCRYPTION_KEY` | DB 암호화 키 (Base64, 32bytes) | `openssl rand -base64 32` 출력값 |

---

## 7. API 엔드포인트 전체 목록

### 인증
| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/api/v1/login` | 누구나 | 로그인 |
| POST | `/api/v1/auth/refresh` | 누구나 | 토큰 갱신 |
| POST | `/api/v1/auth/logout` | 누구나 | 로그아웃 |

### 사용자 관리 (ADMIN)
| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/api/v1/signup` | ADMIN | 사용자 등록 |
| GET | `/api/v1/users` | ADMIN | 사용자 목록 |

### 키움 인증정보
| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| PUT | `/api/v1/kiwoom/credential` | 인증 | appkey/secretkey 등록·수정 |
| DELETE | `/api/v1/kiwoom/credential` | 인증 | appkey/secretkey 삭제 |
| GET | `/api/v1/kiwoom/credential/status` | 인증 | 등록 여부 확인 |

### 키움 토큰
| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/api/v1/kiwoom/token/issue` | 인증 | 접근토큰 수동 발급 |
| DELETE | `/api/v1/kiwoom/token/revoke` | 인증 | 접근토큰 폐기 |

### 종목정보
| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| GET | `/api/v1/stocks/{stkCd}` | 인증 | 단일 종목 기본정보 (TR: ka10001) |
| GET | `/api/v1/stocks?mrktTp={1\|2}` | 인증 | 시장별 전체 종목 리스트 (TR: 포털 확인 필요) |

### 신용주문
| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/api/v1/credit-orders/buy` | 인증 | 신용 매수주문 |
| POST | `/api/v1/credit-orders/sell` | 인증 | 신용 매도주문 |
| POST | `/api/v1/credit-orders/modify` | 인증 | 신용 정정주문 |
| POST | `/api/v1/credit-orders/cancel` | 인증 | 신용 취소주문 |

---

## 8. 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# Swagger UI
http://localhost:8080/swagger-ui.html
```
