# infra

dpm-core 서버를 Oracle Cloud(OCI) Always Free 위에 띄우는 Terraform 구성이다.
환경별(prod / dev)로 인스턴스를 하나씩 두고, 각 인스턴스에 앱과 MySQL 을 함께 올린다.

> 이 디렉터리에는 계정 식별 값, 비밀번호, 키를 넣지 않는다.
> 실제 값은 gitignore 된 `terraform.tfvars`, `~/.oci/config`, 서버의 파일에만 둔다.

## 구성

```
Cloudflare DNS (프록시 OFF)
  ├─ api.depromeet.com      → 예약 공인 IP → core-prod
  └─ dev-api.depromeet.com  → 예약 공인 IP → core-dev

OCI (홈 리전) / compartment `dpm` / VCN 10.0.0.0/16 / public subnet 10.0.1.0/24
  ├─ core-prod   nginx:80,443 → dpm@prod(127.0.0.1:8080) → MySQL(127.0.0.1:3306)
  └─ core-dev    nginx:80,443 → dpm@dev(127.0.0.1:8080)  → MySQL(127.0.0.1:3306)
  NSG: 22, 80, 443 만 허용. 3306 / 8080 은 외부에 열지 않는다.
  Object Storage: MySQL 일일 백업 버킷
```

| 항목 | 값 |
|---|---|
| Shape | `VM.Standard.E2.1.Micro` (x86, 1GB RAM) × 2 — Always Free |
| OS | Ubuntu 24.04 Minimal |
| 스토리지 | 부트 47GB + 데이터 50GB, 인스턴스별 (합계 194GB / 무료 200GB) |
| 앱 | systemd `dpm@{env}`, JDK 21, `/opt/dpm/{env}/app.jar` |
| DB | MySQL 8.4 LTS (공식 저장소), 데이터는 블록 볼륨 |
| 이미지 업로드 | Cloudflare R2 (이 구성과 무관) |
| state | 로컬 `infra/terraform.tfstate` (git-ignore) |

## 파일

| 파일 | 내용 |
|---|---|
| `versions.tf` | Terraform / provider 버전 고정 |
| `providers.tf` | OCI(`~/.oci/config` 프로파일), Cloudflare(환경변수 토큰) |
| `variables.tf` | 입력 변수. 계정 식별 값은 기본값 없이 tfvars 로 받는다 |
| `iam.tf` | compartment, dynamic group, 백업 버킷 전용 policy |
| `network.tf` | VCN, 인터넷 게이트웨이, 라우트, 서브넷, NSG |
| `compute.tf` | 인스턴스, 예약 공인 IP, 데이터 볼륨 |
| `storage.tf` | 백업 버킷과 보존 기간(lifecycle) |
| `dns.tf` | Cloudflare A 레코드. 기본값에서는 아무것도 만들지 않는다 |
| `outputs.tf` | 공인 IP, SSH 명령 |
| `cloud-init/base.yaml.tftpl` | 서버 첫 부팅 시 런타임 준비 |
| `terraform.tfvars.example` | 입력 값 예시 (자리표시자만) |

## 준비물

- Terraform 1.6 이상
- OCI CLI, `~/.oci/config` 의 API 키 프로파일 (`DEFAULT`)
- 서버 접속용 SSH 키 쌍 (기본 경로 `~/.ssh/dpm_oci_ed25519.pub`, `ssh_public_key_path` 로 변경 가능)

## 사용법

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars   # 값 채우기 (커밋되지 않는다)
terraform init
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

- `Out of host capacity` 가 나오면 잠시 후 다시 `apply` 한다. 이미 만든 리소스는 유지된다.
- IAM(dynamic group, policy)은 테넌시 관리자 권한이 필요하다.

## 안전장치

- 인스턴스, 데이터 볼륨, 예약 IP, 백업 버킷, compartment 에 `prevent_destroy` 가 걸려 있다. `terraform destroy` 가 실패하는 것이 정상이다.
- 인스턴스는 이미지와 `metadata` 변경을 무시한다. 새 Ubuntu 이미지가 나오거나 cloud-init 을 고쳐도 서버가 교체되지 않는다.
  **cloud-init 수정은 새로 만드는 서버에만 적용된다.** 기존 서버는 SSH 로 직접 반영한다.
- MySQL 은 데이터 볼륨이 마운트되지 않으면 시작하지 않는다 (`RequiresMountsFor`). 부트 디스크에 빈 DB 가 생기는 것을 막는다.

## state 관리

state 는 로컬 파일이다. Terraform 이 만든 리소스의 ID 가 평문으로 들어 있다.

- git 에 올리지 않는다 (`infra/.gitignore`).
- `apply` 뒤에는 개인 백업 위치에 복사해 둔다. 잃어버리면 리소스마다 `terraform import` 가 필요하다.
- 잠금이 없으므로 한 곳에서만 `apply` 한다.
- 원격으로 옮기려면 `backend "oci"` 블록을 추가하고 `terraform init -migrate-state` 를 실행한다.

## cloud-init 이 하는 일

런타임만 준비한다. JAR 와 앱 `.env` 는 CD 가, DB 계정과 데이터는 사람이 넣는다.

1. 타임존 `Asia/Seoul`, swap 2GB
2. sshd 키 인증만 허용 (비밀번호·root 로그인 차단)
3. OS 방화벽(iptables)에 80/443 허용
4. 데이터 볼륨을 기다렸다가 `/data` 에 마운트하고 `/var/lib/mysql` 로 bind mount
5. MySQL 8.4 LTS 설치 (서명 키 fingerprint 확인 후)
6. JRE 21, `dpm` 사용자, `dpm@.service`
7. nginx (HTTP 로 시작, 인증서 발급 후 `sudo dpm-enable-tls` 로 HTTPS 전환), certbot
8. 백업 스크립트 `dpm-backup` 과 매일 04:00 cron, 백업 업로드용 OCI CLI

진행 상황은 서버에서 `sudo cloud-init status --wait`, 로그는 `/var/log/cloud-init-output.log` 로 확인한다.

## 서버 초기 설정 (환경별 1회)

### MySQL 계정

root 는 `auth_socket` 이라 서버에서 `sudo` 로만 접속한다. 비밀번호는 서버에서 만들고 저장소에 남기지 않는다.

앱은 `127.0.0.1`(TCP)로 접속하고 `skip-name-resolve` 가 켜져 있으므로 `'dpm'@'127.0.0.1'` 계정이 필요하다.

```bash
sudo mysql --no-defaults -uroot <<'SQL'
CREATE DATABASE IF NOT EXISTS <schema> CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'dpm'@'127.0.0.1' IDENTIFIED BY '<app-password>';
CREATE USER 'dpm'@'localhost' IDENTIFIED BY '<app-password>';
GRANT ALL PRIVILEGES ON <schema>.* TO 'dpm'@'127.0.0.1', 'dpm'@'localhost';
CREATE USER 'backup'@'localhost' IDENTIFIED BY '<backup-password>';
GRANT SELECT, LOCK TABLES, SHOW VIEW, TRIGGER, EVENT, PROCESS ON *.* TO 'backup'@'localhost';
SQL
```

스키마 이름은 `application-{env}.yml` 의 `*_DB_SCHEMA` 와 맞춘다.

백업 계정 정보는 root 만 읽을 수 있는 `/root/.my.cnf`(권한 600)에 둔다. 이 파일이 있으면 `sudo mysql` 이 backup 계정으로 접속하므로, root 로 들어갈 때는 `--no-defaults -uroot` 를 쓴다.

### 앱 `.env`

`/opt/dpm/{env}/.env` (소유 `dpm:dpm`, 권한 600). 앱(Spring)이 작업 디렉터리에서 직접 읽는다.
DB 는 `*_DB_HOST=127.0.0.1`, `*_DB_PORT=3306`.

### HTTPS

nginx 는 HTTP 설정으로 시작한다. 인증서를 받은 뒤 HTTPS 설정으로 바꾼다.

```bash
# DNS 가 이 서버를 가리킨 뒤 (HTTP-01)
sudo certbot certonly --webroot -w /var/www/html -d <domain>
sudo dpm-enable-tls
```

DNS 전환 전에 미리 받으려면 `--manual --preferred-challenges dns` 로 TXT 레코드 인증을 쓴다.
이렇게 받은 인증서는 자동 갱신되지 않으므로, DNS 전환 뒤 위의 `--webroot` 명령에
`--cert-name <domain> --force-renewal` 을 붙여 한 번 다시 발급한다.

HTTPS 설정의 80 포트는 `/.well-known/acme-challenge/` 만 리다이렉트하지 않는다.
갱신은 `certbot.timer` 가 하고, 갱신 후 nginx 를 다시 읽는다. `sudo certbot renew --dry-run` 으로 확인한다.

Cloudflare 프록시(오렌지 구름)를 쓰는 호스트는 HTTPS 를 Cloudflare 가 처리한다.
SSL 모드가 Flexible 이면 서버는 HTTP 설정 그대로 두고 `dpm-enable-tls` 를 실행하지 않는다 (리다이렉트 루프).

### 백업 확인

```bash
sudo dpm-backup
```

버킷의 `{env}/` 아래에 `<schema>-<timestamp>.sql.gz` 가 올라간다. 보존 기간이 지난 객체는 자동 삭제된다.

## 배포 (GitHub Actions)

| 워크플로 | 트리거 | 서버 시크릿 |
|---|---|---|
| `dev-cd.yml` | `develop` push, 수동 | `DEV_SERVER_HOST` / `_USERNAME` / `_SSH_KEY` |
| `prod-cd.yml` | `main` push, 수동 | `PROD_SERVER_HOST` / `_USERNAME` / `_SSH_KEY` |

흐름: `bootJar` → SCP → `.env` 의 저장소 키 갱신 → `systemctl` 재시작 → health check.
원격 스크립트는 접속 계정이 root 가 아니면 `sudo` 로 실행한다.
health URL 은 repo 변수 `DEV_HEALTH_URL` / `PROD_HEALTH_URL` 로 지정한다 (이 구성에서는 둘 다 `127.0.0.1:8080`).

## DNS 전환

Cloudflare 콘솔에서 A 레코드를 직접 바꾸거나, Terraform 으로 관리한다.
Terraform 으로 할 때는 이미 있는 레코드를 먼저 import 한다.

```bash
export CLOUDFLARE_API_TOKEN=<Zone:DNS:Edit 토큰>
# terraform.tfvars: cloudflare_zone_id, dns_cutover_envs = ["dev"]
terraform import 'cloudflare_dns_record.app["dev"]' '<zone_id>/<record_id>'
terraform plan    # A 레코드 값만 바뀌어야 한다
terraform apply
```

레코드의 Proxy 상태는 DNS only 로 둔다.

## A1(ARM)으로 바꿀 때

`shape = "VM.Standard.A1.Flex"` 로 바꾸면 인스턴스가 교체된다. 데이터 볼륨은 남지만,
백업 → `prevent_destroy` 해제 → `apply` → 복원 순서로 따로 계획한다.
