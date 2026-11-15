provider "oci" {
  config_file_profile = "DEFAULT"
  region              = var.region
}

# API 토큰은 환경변수 CLOUDFLARE_API_TOKEN 으로 주입한다 (Zone:DNS:Edit 권한만).
provider "cloudflare" {}
