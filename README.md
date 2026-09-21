
Morago Backend
통역사 예약 플랫폼의 백엔드 애플리케이션입니다. 사용자가 통역사를 선택해 음성/영상 통화(WebRTC) 로 통역 서비스를 이용하고, 시스템은 결제, 통역사 정산, 출금 신청을 처리합니다.
Habsida 교육 과정에서 진행한 프로젝트이며, 배포용으로 별도 저장소에 정리했습니다.
데모: https://morago-backend-production.up.railway.app (Railway 배포, MySQL 포함)
기술 스택
구분	기술
언어 / 프레임워크	Java 25, Spring Boot 4.1, Java/Groovy 혼합 프로젝트 (gmavenplus-plugin)
보안	Spring Security 6, JWT (jjwt 0.12.7) — access/refresh 토큰
데이터	Spring Data JPA, Hibernate, MySQL
실시간 통신	netty-socketio 2.0.13 (WebSocket) — WebRTC 시그널링
API 문서	springdoc-openapi 2.8.9 (Swagger UI)
테스트	JUnit 5, Testcontainers (실제 MySQL 컨테이너)
기타	Lombok, Maven
주요 기능
인증/인가: 회원가입, 로그인, JWT + Refresh 토큰, 역할 기반 접근 제어(RBAC)
통역사 프로필: 프로필 생성·관리, 언어 및 전문 분야
통화: 통역사와 통화 예약, WebSocket(Socket.IO) 기반 시그널링, 통화 상태(`CallStatus`) 관리
거래/출금: 통화 결제, 거래 내역, 출금 신청(`WithdrawalRequest`)과 상태 관리
리뷰: 통화 후 통역사 평가 및 리뷰
관리자: 사용자 및 플랫폼 관리용 관리자 전용 엔드포인트
감사 로그: 주요 작업 이력 기록
담당 업무
Spring Security 6 + JWT(access/refresh) 인증·인가 및 RBAC 구현
netty-socketio 기반 WebRTC 시그널링 구현 (JWT 헤더 인증, 통화방 입장 시 상대방 알림, 차단된 사용자 접근 제한)
통화·결제 상태 변경을 실시간으로 알리는 알림 서비스 구현 (순환 의존성을 `@Lazy`로 해결)
`@EntityGraph`로 N+1 문제 해결, `Pageable` 기반 페이지네이션 적용
전역 예외 처리, Bean Validation, Swagger API 문서화
Testcontainers 기반 통합 테스트 작성
Railway에 MySQL과 함께 배포
feature 브랜치 → `dev` PR → 코드 리뷰 → squash & merge 방식으로 협업
프로젝트 구조
```
src/main/groovy/org/morago/
├── config/       Spring 설정 (Security, WebSocket, OpenAPI 등)
├── controller/   REST 컨트롤러 (Auth, Call, Admin, Transaction, Review ...)
├── dto/          도메인별 DTO
├── event/        도메인 이벤트
├── exception/    사용자 정의 예외, 전역 예외 처리
├── model/        JPA 엔티티 (User, Call, Transaction, TranslatorProfile ...)
├── repository/   Spring Data 리포지토리
├── scheduler/    예약 작업
├── security/     JWT, 인증 필터, RBAC
├── service/      비즈니스 로직
├── signaling/    Socket.IO 기반 WebRTC 시그널링
└── util/         유틸리티
```
로컬 실행 방법
요구 사항
JDK 25
MySQL (로컬 또는 Docker)
Maven (`./mvnw` / `mvnw.cmd` 사용, 별도 설치 불필요)
환경 변수
변수	설명
`DB_PASSWORD`	MySQL 사용자 비밀번호
`JWT_SECRET`	JWT 서명용 비밀키 (32자 이상)
`SPRING_PROFILES_ACTIVE`	활성 프로필 (기본값 `dev`)
`SOCKETIO_HOST` / `SOCKETIO_PORT`	Socket.IO 서버 주소와 포트 (기본값 `localhost:9092`)
빌드 및 실행
```bash
./mvnw clean package -DskipTests
java -jar target/morago-0.0.1-SNAPSHOT.jar
```
기본적으로 http://localhost:8080 에서 실행됩니다.
API 문서
실행 후 Swagger UI에서 확인할 수 있습니다.
```
http://localhost:8080/swagger-ui/index.html
```
테스트
```bash
./mvnw test
```
통합 테스트는 Testcontainers를 사용하므로 Docker가 실행 중이어야 합니다.
