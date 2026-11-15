resource "oci_objectstorage_bucket" "backup" {
  depends_on     = [time_sleep.compartment_propagation]
  compartment_id = local.compartment_id
  namespace      = var.object_storage_namespace
  name           = var.backup_bucket_name
  access_type    = "NoPublicAccess"
  storage_tier   = "Standard"
  versioning     = "Disabled"

  lifecycle {
    prevent_destroy = true
  }
}

resource "oci_objectstorage_object_lifecycle_policy" "backup" {
  # lifecycle 실행 권한(service policy)이 먼저 있어야 생성된다.
  depends_on = [oci_identity_policy.instances]

  namespace = var.object_storage_namespace
  bucket    = oci_objectstorage_bucket.backup.name

  rules {
    name        = "delete-old-backups"
    action      = "DELETE"
    is_enabled  = true
    target      = "objects"
    time_amount = var.backup_retention_days
    time_unit   = "DAYS"
  }
}
