resource "oci_identity_compartment" "dpm" {
  compartment_id = var.tenancy_ocid
  name           = "dpm"
  description    = "dpm-core 서버 리소스"
  enable_delete  = false # destroy 해도 compartment 는 남긴다

  lifecycle {
    prevent_destroy = true
  }
}

# 새 compartment 는 전파에 시간이 걸린다. 바로 리소스를 만들면 NotAuthorizedOrNotFound 가 날 수 있다.
resource "time_sleep" "compartment_propagation" {
  depends_on      = [oci_identity_compartment.dpm]
  create_duration = "60s"
}

resource "oci_identity_dynamic_group" "instances" {
  compartment_id = var.tenancy_ocid
  name           = "dpm-instances"
  description    = "dpm compartment 의 compute 인스턴스"
  matching_rule  = "ALL {instance.compartment.id = '${oci_identity_compartment.dpm.id}'}"
}

resource "oci_identity_policy" "instances" {
  compartment_id = var.tenancy_ocid
  name           = "dpm-instances"
  description    = "인스턴스의 DB 백업 업로드 + 백업 버킷 lifecycle"
  statements = [
    # 인스턴스(instance-principal)는 백업 버킷에만 접근한다.
    "Allow dynamic-group ${oci_identity_dynamic_group.instances.name} to manage objects in compartment ${oci_identity_compartment.dpm.name} where target.bucket.name = '${var.backup_bucket_name}'",
    # lifecycle 정책(오래된 백업 자동 삭제)을 Object Storage 서비스가 실행하기 위한 권한
    "Allow service objectstorage-${var.region} to manage object-family in compartment ${oci_identity_compartment.dpm.name}",
  ]
}
