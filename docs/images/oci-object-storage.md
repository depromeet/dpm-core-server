# 이미지 업로드 (OCI Object Storage)

결석 사유서 첨부 등에 쓸 이미지를 비공개 버킷에 저장한다. 첨부 관계와 운영진 열람 권한은 결석 사유 기능에서 붙인다. 지금은 업로드한 본인만 조회할 수 있다.

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/v1/images` | `multipart/form-data` 의 `file` 파트. JPEG/PNG, 10MiB·2,500만 픽셀 이하 → 201 `data: {imageId, contentType, size}` |
| GET | `/v1/images/{imageId}` | 본인 이미지의 원본 바이트. 남의 이미지·없는 이미지 모두 404 |

- 둘 다 로그인 필요(`@PreAuthorize("isAuthenticated()")`, `/v1/**` 가 permitAll 이라 메서드에서 강제).
- 형식은 파일 시그니처로 판별하고 실제로 디코드해 본다. 디코드 중 경고가 나는 손상 파일(잘린 JPEG 등)도 거절한다. 파트의 Content-Type 이 있으면 판별 결과와 같아야 한다. 확장자는 보지 않는다. SVG/WebP/GIF 는 거절.
- 오류: 400(빈 파일, `file` 누락, Content-Type 불일치, 손상, 해상도 초과) / 401 / 404 / 413(10MiB 초과) / 415(JPEG·PNG 아님) / 503(저장소 설정·인증·장애).
- GET 응답 헤더: `Cache-Control: no-store, private`, `X-Content-Type-Options: nosniff`, `Content-Disposition: inline; filename="{임의 UUID}.jpg|png"`.
- 버킷 키, 공개 URL, PAR(사전 인증 요청)은 쓰지 않는다. 프론트는 인증 헤더를 붙여 `fetch` 한 뒤 `URL.createObjectURL(blob)` 으로 표시한다(`<img src>` 직접 지정 불가).

## 설정

| 환경 변수 | 예시 | 설명 |
|---|---|---|
| `OCI_OBJECT_STORAGE_BUCKET` | `depromeet-images` | 비공개 버킷 |
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
   Allow dynamic-group <DG> to manage objects in compartment <C> where all {target.bucket.name = 'depromeet-images', any {request.permission = 'OBJECT_CREATE', request.permission = 'OBJECT_DELETE'}}
   ```
   - read: GET. OBJECT_CREATE: 업로드(`if-none-match: *` 라 덮어쓰기 권한은 필요 없음). OBJECT_DELETE: 메타데이터 저장 실패 시 정리.
   - 권한이 없으면 OCI 는 403 대신 404 를 주기도 한다. 업로드는 503, 조회는 404 로 나가며 둘 다 로그에 status/opc-request-id 가 남는다.
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
```
API 키는 개인 사용자에 발급하고 위와 같은 범위의 정책을 준다. 키 파일과 config 는 커밋하지 않는다. OCI 인스턴스가 아닌 곳에서 instance-principal 로 두면 metadata 탐지가 실패해 503 이 된다.

## 배포 순서

1. `db/pending/2610021200_images.sql` 적용 (`ddl-auto: validate` 라 테이블이 없으면 기동 실패)
2. 버킷(비공개), Dynamic Group, Policy 준비
3. GitHub 저장소 변수(`DEV_OCI_*`, `PROD_OCI_*`) 등록 후 배포. 워크플로가 컨테이너 env 까지 확인하므로 로그의 `> OCI env 확인 완료` 를 보고, 업로드/조회 한 번씩 확인

## 한계

- 업로드 후 메타데이터 저장이 실패하면 객체를 지운다. 그 삭제까지 실패하면 객체가 버킷에 남는다(ERROR 로그에 키가 남음). 별도 정리 작업은 없다.
- 멀티파트는 인증 전에 파싱된다(`/v1/**` permitAll). 비로그인 요청도 최대 11MiB 까지는 임시 파일로 받은 뒤 401 이 된다.
- 요청 본문이 상한을 크게 넘으면 Tomcat 이 413 응답 뒤 연결을 끊을 수 있어 클라이언트에 413 대신 연결 오류로 보일 수 있다.
- CMYK JPEG 등 JDK ImageIO 가 못 읽는 파일은 400 으로 거절된다.
