variable "tenancy_ocid" {
  description = "테넌시 OCID (dpm_core_2)"
  type        = string
}

variable "region" {
  description = "홈 리전. Always Free 인스턴스는 홈 리전에만 만들 수 있다."
  type        = string
  default     = "ap-osaka-1"
}

variable "object_storage_namespace" {
  description = "Object Storage namespace (콘솔 Tenancy details 또는 `oci os ns get`). terraform.tfvars 에 둔다."
  type        = string
}

variable "shape" {
  description = "VM.Standard.E2.1.Micro(기본) 또는 VM.Standard.A1.Flex"
  type        = string
  default     = "VM.Standard.E2.1.Micro"

  validation {
    condition     = contains(["VM.Standard.E2.1.Micro", "VM.Standard.A1.Flex"], var.shape)
    error_message = "Always Free shape만 허용한다."
  }
}

variable "ubuntu_version" {
  description = "oci_core_images 의 operating_system_version 값"
  type        = string
  default     = "24.04 Minimal"
}

variable "ssh_public_key_path" {
  type    = string
  default = "~/.ssh/dpm_oci_ed25519.pub"
}

variable "envs" {
  description = "환경별 설정. domain은 nginx server_name 과 DNS 레코드에 쓴다."
  type = map(object({
    domain = string
  }))
  default = {
    prod = { domain = "api.depromeet.com" }
    dev  = { domain = "dev-api.depromeet.com" }
  }
}

variable "backup_bucket_name" {
  type    = string
  default = "dpm-db-backup"
}

variable "backup_retention_days" {
  type    = number
  default = 30
}

# ---- DNS 컷오버 ----

variable "cloudflare_zone_id" {
  description = "depromeet.com 존 ID. DNS 컷오버를 켤 때만 필요하다."
  type        = string
  default     = ""
}

variable "dns_cutover_envs" {
  description = "A레코드를 새 서버로 전환할 환경 목록. 예: [\"dev\"] → 검증 후 [\"dev\", \"prod\"]"
  type        = set(string)
  default     = []

  validation {
    condition     = alltrue([for e in var.dns_cutover_envs : contains(["prod", "dev"], e)])
    error_message = "prod, dev 만 허용한다."
  }
}
