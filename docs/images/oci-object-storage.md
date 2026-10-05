# 이미지 업로드 (OCI Object Storage)

결석 사유서 첨부 등에 쓸 이미지를 비공개 버킷에 저장한다. 첨부 관계는 결석 사유 기능에서 붙인다. 조회·다운로드는 업로드한 본인과 운영진(`update:attendance`)만 할 수 있다.

## API

바이트는 서버를 거치지 않는다. 프론트가 PAR(사전 인증 요청) URL 로 OCI 에 직접 올리고, 서버는 검증·확정만 한다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/v3/images/uploads` | JSON `{contentType, size}` → 201 `data: {uploadId, uploadUrl, expiresAt}`. 업로드 URL 수명 10분 |
| POST | `/v3/images/uploads/{uploadId}/complete` | 검증 후 확정 → 200 `data: {imageId, contentType, size}`. 처리 중이면 202 + `Retry-After` |
| GET | `/v3/images/{imageId}` | 본인 이미지의 읽기 URL → 200 `data: {url, expiresAt}`. 수명 1분. 남의 이미지·없는 이미지 모두 404 |

- 모두 로그인 필요(`@PreAuthorize("isAuthenticated()")`, `/v3/**` 가 permitAll 이라 메서드에서 강제). URL 이 담긴 응답은 `Cache-Control: no-store`.
- 기존 `POST /v1/images`(multipart) 와 바이트를 돌려주던 `GET /v1/images/{imageId}` 는 없어졌다(404). `/v1` 에는 이미지 경로가 없다. 이미 저장된 이미지(`images/{UUID}`)는 GET 이 같은 방식(읽기 URL)으로 그대로 준다.

### 프론트 흐름

```text
1. POST /v3/images/uploads {"contentType":"image/png","size":12345}
   → {uploadId, uploadUrl, expiresAt}
2. PUT {uploadUrl}  본문 = 파일 원본, 헤더 Content-Type: image/png (1번과 같은 값), Content-Encoding 없음
   (인증 헤더·쿠키 불필요. 10분 안에)
3. POST /v3/images/uploads/{uploadId}/complete
   → 200 {imageId,...}                 끝
   → 202 + Retry-After: 1              Retry-After 초 뒤 3번 반복
   → 409 IMAGE-409-01                  PUT 이 아직 안 됨. 올린 뒤 3번 반복
   → 429 + Retry-After                 서버가 다른 이미지를 검증 중. 기다렸다 3번 반복
   → 503                               잠시 후 3번 반복
   → 400/413/415, 409(IMAGE-409-02/03), 410   1번부터 새로 시작
4. 표시할 때 GET /v3/images/{imageId} → url 을 <img src> 에 넣는다. 1분 뒤에는 다시 받는다.
```

- complete 는 몇 번을 불러도 안전하다. 완료 뒤에는 같은 `imageId`, 거절 뒤에는 같은 오류를 준다. 응답을 못 받았으면 같은 요청을 다시 보내면 된다.
- 업로드 URL 이 만료되면 새 검증을 시작하지 않는다(410). 이미 확정 저장 단계(202)에 들어간 업로드는 만료와 무관하게 끝까지 진행된다.
- 검증: 시그니처로 형식 판별, 실제 디코드(손상·잘린 파일 경고 포함 거절), 10MiB·2,500만 픽셀 이하. 업로드된 크기와 PUT 의 Content-Type 이 1번 요청과 같아야 한다(확정 객체가 그 Content-Type 으로 읽기 URL 응답에 실리기 때문). SVG/WebP/GIF 는 거절.
- 오류: 400(빈 파일, 형식·크기 불일치, 손상, 해상도 초과) / 401 / 404(업로드·이미지 없음 또는 남의 것) / 409 / 410 / 413(10MiB 초과) / 415(JPEG·PNG 아님) / 429 / 503(저장소 설정·인증·장애).

### 보안 메모

- PAR URL 은 그 자체가 권한이다. 가진 사람은 누구나 만료 전까지 쓰기(업로드 URL, 해당 업로드 객체 하나) 또는 읽기(조회 URL, 이미지 하나)를 할 수 있다. 서버는 URL 을 저장하거나 로그에 남기지 않는다(`par_id` 만 저장, 응답 DTO 의 `toString` 에서도 제외).
- PAR 는 업로드 크기 상한을 강제하지 못한다. 10MiB 를 넘게 올릴 수는 있지만, 서버는 상한 + 1 바이트까지만 내려받아 거절하고 업로드 객체를 지운다.
- 업로드 URL 은 `uploads/{uploadId}` 한 객체에만 걸린다. 확정 이미지는 `images/{uploadId}` 이며 여기에는 쓰기 PAR 을 만들지 않는다.

### 서버 동작

- 상태는 `image_uploads` 한 테이블에 둔다: `PENDING → VERIFYING → COPYING → COMPLETED` (끝 상태: `COMPLETED`, `REJECTED`, `FAILED`, `EXPIRED`).
- 검증: 업로드 객체를 상한 + 1 바이트까지 임시 파일로 받아(어떤 경로로 끝나도 삭제) 파일 기반 ImageIO 로 디코드한다. 바이트 배열·메모리 캐시를 쓰지 않는다. 검증은 서버 인스턴스당 동시에 1건이며 넘치면 대기열 없이 429.
- 확정: GET 응답의 ETag 를 고정하고, OCI CopyObject 를 `sourceObjectIfMatchETag = 그 ETag`, `destinationObjectIfNoneMatchETag = *` 로 낸다. 검증 후 원본이 바뀌면 복사가 거절되고(409 `IMAGE-409-02`), 확정 객체는 덮어써지지 않는다. 복사는 비동기 work request 이며 id 를 저장해 다음 complete 호출이 이어서 확인한다.
- 복사 완료(확정 객체 존재 + 크기 일치)를 확인한 뒤에만 한 트랜잭션에서 `images` 행을 넣고 세션을 `COMPLETED` 로 바꾼다. `images.object_key`, `image_uploads.image_id` UNIQUE 로 이미지가 둘 생기지 않는다.
- 클라우드 호출 동안 DB 트랜잭션·잠금을 잡지 않는다. 처리 주체는 행의 lease(`lease_token`, 2분)로 정하고 모든 상태 변경은 token 조건부 UPDATE 다. 서버가 죽어도 lease 가 끝나면 다음 complete 호출이 이어받으며, lease 를 잃은 처리자는 결과를 반영하지 못한다.
- 복사 응답을 못 받은 경우(work request id 없음): 다음 호출이 확정 키를 확인하고, 없으면 같은 조건부 복사를 다시 낸다. 확정 키에는 서버의 조건부 복사만 쓰므로 확정 키에 같은 크기의 객체가 있으면 검증한 바이트다.
- 쓰기 PAR 은 검증이 끝나면(통과·거절 모두) 회수하고, 완료·거절 뒤 업로드 객체를 지운다. 실패하면 아래 정리 작업이 다시 한다.
- 정리 작업(30분마다, 한 번에 50건): 만든 지 24시간이 지났고 정리할 것이 남은(`par_id` 가 남은) 세션만 본다.
  - `PENDING`/`VERIFYING`(lease 없음) → `EXPIRED` 로 바꾼 뒤 PAR 회수, 업로드 객체 삭제, 행 삭제.
  - `COPYING` → 결과를 모른 채 지우지 않는다. complete 와 같은 경로로 진행시키고(진행 중이면 그대로 둠) 끝난 상태는 다음 실행에서 정리한다.
  - `COMPLETED` → PAR 회수, 업로드 객체 삭제 후 `par_id = NULL`. 행은 `uploadId → imageId` 기록으로 남는다. 확정 이미지는 지우지 않는다.
  - `REJECTED`/`EXPIRED` → PAR 회수, 업로드 객체 삭제, 행 삭제. `FAILED` 는 늦게 끝난 복사가 남긴 확정 객체도 지운다(이미지 행은 `COMPLETED` 에서만 생기므로 참조되지 않음).
  - 저장소 호출이 실패한 행은 그대로 남아 다음 실행에서 다시 시도된다. 여러 인스턴스가 동시에 돌아도 조건부 UPDATE 와 멱등 삭제라 안전하다.

## 설정

| 환경 변수 | 예시 | 설명 |
|---|---|---|
| `OCI_OBJECT_STORAGE_BUCKET` | `depromeet-images` | 비공개 버킷 (dev/prod) |
| `LOCAL_OCI_OBJECT_STORAGE_BUCKET` | (예: `depromeet-images-local`) | local 프로필 전용 비공개 버킷. local 은 `OCI_OBJECT_STORAGE_BUCKET` 을 읽지 않으며, 비면 dev/prod 버킷으로 가지 않고 503 |
| `OCI_REGION` | `ap-seoul-1` | |
| `OCI_OBJECT_STORAGE_NAMESPACE` | `ax8dilxsaxsr` | 테넌시 Object Storage 네임스페이스 |
| `OCI_AUTH_MODE` | `instance-principal`(기본) / `config-file` | |
| `OCI_CONFIG_FILE`, `OCI_CONFIG_PROFILE` | `~/.oci/config`, `DEFAULT` | `config-file` 모드만 |

- 값이 비어 있어도 서버는 뜬다. 클라이언트는 첫 이미지 요청에서 만들며 실패하면 503 과 ERROR 로그(디스코드 알림)를 남긴다. 다음 요청이 다시 시도한다.
- 타임아웃: 연결 3초, 읽기 15초, SDK 재시도 없음. Instance Principal 의 metadata 탐지는 1회 재시도·2초.
- 세 값은 GitHub 저장소 변수(Variables, 시크릿 아님) `DEV_OCI_OBJECT_STORAGE_BUCKET`, `DEV_OCI_REGION`, `DEV_OCI_OBJECT_STORAGE_NAMESPACE` 와 `PROD_` 버전에서 온다. `OCI_AUTH_MODE` 는 워크플로에서 `instance-principal` 로 고정한다. 값이 비었거나 `A-Za-z0-9._-` 밖의 문자가 있으면 배포 job 이 처음에 실패한다.
- prod(`prod-cd.yml`): `~/.env` heredoc 에 네 값을 쓰고, `~/server-stack.oci.yml`(`spring-app` 의 `environment` 만 담은 오버라이드)을 만들어 `docker stack deploy -c server-stack.yml -c server-stack.oci.yml server` 로 배포한다. 그래서 서버의 `server-stack.yml` 에 매핑이 없어도 값이 들어간다.
- dev(`dev-cd.yml`): 서버 `~/.env` 의 OCI 키 네 개만 교체(다른 키·파일 권한 유지)하고 export 한 뒤 `deploy.sh` 를 실행한다. 배포 후 새 이미지 태그로 뜬 컨테이너를 찾아 env 를 비교하고, 다르면 `com.docker.swarm.service.name` 라벨의 서비스에 `docker service update --env-add` 로 넣는다.
- 두 워크플로 모두 마지막에 새 컨테이너의 OCI env 네 개가 기대값과 정확히 같은지 확인하고, 다르면 실패한다. dev 컨테이너가 Swarm 서비스가 아니면 env 를 바꿀 수 없으므로 실패 메시지대로 `deploy.sh` 가 `~/.env` 를 `--env-file` 로 넘기게 고쳐야 한다.

### Instance Principal (dev/prod 기본)

서버 인스턴스 자체가 자격 증명이 된다. 키 파일이나 시크릿이 필요 없다.

1. Dynamic Group 생성 (Identity → Domains → Dynamic groups)
   ```
   ANY {instance.id = 'ocid1.instance.oc1.ap-seoul-1.<서버 인스턴스>'}
   ```
2. Policy (버킷이 있는 컴파트먼트)
   ```
   Allow dynamic-group <DG> to read objects in compartment <C> where target.bucket.name = 'depromeet-images'
   Allow dynamic-group <DG> to manage objects in compartment <C> where all {target.bucket.name = 'depromeet-images', any {request.permission = 'OBJECT_CREATE', request.permission = 'OBJECT_OVERWRITE', request.permission = 'OBJECT_DELETE'}}
   Allow dynamic-group <DG> to manage buckets in compartment <C> where all {target.bucket.name = 'depromeet-images', request.permission = 'PAR_MANAGE'}
   Allow service objectstorage-ap-seoul-1 to manage object-family in compartment <C>
   ```
   - read(OBJECT_READ): 검증 내려받기, HEAD, 복사 원본, 읽기 PAR 발급.
   - PAR_MANAGE: PAR 발급·회수. 쓰기(ObjectWrite) PAR 은 발급자에게 OBJECT_CREATE 와 OBJECT_OVERWRITE 가 함께 있어야 만들 수 있다(같은 업로드 URL 로 다시 PUT 하면 덮어쓰기가 된다).
   - OBJECT_CREATE: 확정 키로 복사. OBJECT_DELETE: 업로드 객체·실패한 확정 객체 정리.
   - 복사(CopyObject)는 Object Storage 서비스가 대신 수행하므로 서비스 정책(마지막 줄)이 필요하다. 필요하면 `where target.bucket.name = 'depromeet-images'` 로 좁힌다.
   - 복사 work request 조회(GetWorkRequest)가 위 정책만으로 허용되는지는 실제 테넌시에서 확인하지 않았다. dev 에서 첫 complete 가 503 이면 로그의 status/opc-request-id 로 권한을 확인한다.
   - 권한이 없으면 OCI 는 403 대신 404 를 주기도 한다. 로그에 status/opc-request-id 가 남는다.
3. 컨테이너에서 metadata 서비스에 닿는지 확인 (Docker 브리지/swarm 네트워크)
   ```bash
   docker exec <컨테이너> curl -s -H 'Authorization: Bearer Oracle' http://169.254.169.254/opc/v2/instance/region
   ```
   응답이 없으면 호스트 방화벽(iptables)이나 네트워크 설정을 확인한다. `auth.ap-seoul-1.oraclecloud.com`, `objectstorage.ap-seoul-1.oraclecloud.com` 으로의 아웃바운드 HTTPS 도 필요하다.

### 로컬 (config-file)

```bash
OCI_AUTH_MODE=config-file
OCI_CONFIG_FILE=~/.oci/config
OCI_CONFIG_PROFILE=DEFAULT
LOCAL_OCI_OBJECT_STORAGE_BUCKET=<로컬 전용 버킷 이름>
```
로컬은 dev/prod 버킷(`depromeet-images`)을 쓰지 않는다. local 프로필의 버킷은 `LOCAL_OCI_OBJECT_STORAGE_BUCKET` 에서만 오고, 비어 있으면 503 이 된다(`.env` 에 `OCI_OBJECT_STORAGE_BUCKET` 이 있어도 쓰지 않는다). region, namespace 는 위의 `OCI_*` 값을 같이 쓴다.

API 키는 개인 사용자에 발급하고, 위 Instance Principal 정책과 같은 권한을 로컬 버킷에만 준다(`target.bucket.name = '<로컬 버킷>'`). dev/prod 버킷 권한은 주지 않는다. 키 파일과 config 는 커밋하지 않는다. OCI 인스턴스가 아닌 곳에서 instance-principal 로 두면 metadata 탐지가 실패해 503 이 된다.

테스트 객체 자동 삭제가 필요하면 로컬 버킷에만 OCI Object Lifecycle 삭제 규칙과 보존 기간을 설정한다.

## 배포 순서

1. `db/pending/2610021200_images.sql`(아직이면), `db/pending/2610041200_image_uploads.sql` 적용. 둘 다 추가만 하며 `ddl-auto: validate` 라 테이블이 없으면 기동 실패
2. 버킷(비공개), Dynamic Group, 위 Policy(PAR_MANAGE, OBJECT_OVERWRITE, 서비스 정책 포함) 준비
3. GitHub 저장소 변수(`DEV_OCI_*`, `PROD_OCI_*`) 등록 후 배포. 워크플로가 컨테이너 env 까지 확인하므로 로그의 `> OCI env 확인 완료` 를 보고, 프론트 origin 에서 업로드(PUT 포함)·조회를 한 번씩 확인
4. 기존 multipart 업로드(`POST /v1/images`)와 바이트 GET(`GET /v1/images/{imageId}`)은 이 배포에서 없어진다. 그 API 를 쓰는 프론트가 있다면 새 흐름과 함께 배포한다

### 브라우저 CORS

OCI Object Storage 는 버킷별 CORS 설정을 제공하지 않으며 PAR 요청에 대한 CORS 동작은 Oracle 이 고정해 둔 것이다(Oracle Object Storage FAQ). 서버나 버킷 설정으로 바꿀 수 없으므로, 프론트 origin 에서 PAR 로의 PUT/GET(preflight 포함)이 되는지 dev 에서 실제로 확인한다. 이 저장소에서는 확인하지 않았다.

## 한계

- PAR URL 을 가진 사람은 만료 전까지 접근할 수 있다(업로드 10분, 조회 1분). 회수는 검증 직후·정리 작업에서 하지만 조회 PAR 은 회수하지 않고 만료에 맡긴다. 조회마다 PAR 이 하나씩 생긴다.
- PAR 는 업로드 크기를 미리 막지 못한다. 상한을 넘는 업로드는 서버가 상한 + 1 바이트까지만 읽고 거절·삭제한다.
- 업로드 URL 만료 뒤에는 검증을 새로 시작하지 않는다. 검증 도중 서버가 죽고 그 사이 URL 이 만료되면 그 업로드는 410 이 되고 다시 올려야 한다.
- 정리 작업은 24시간이 지난 세션만 본다. 실패한 정리 대상(업로드 객체, PAR)은 그때까지 남는다.
- 서버당 동시 검증 1건이라 여러 명이 동시에 올리면 429 로 재시도하게 된다.
- CMYK JPEG 등 JDK ImageIO 가 못 읽는 파일은 400 으로 거절된다.
