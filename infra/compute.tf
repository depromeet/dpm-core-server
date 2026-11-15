data "oci_identity_availability_domains" "ad" {
  compartment_id = var.tenancy_ocid
}

data "oci_core_images" "ubuntu" {
  compartment_id           = var.tenancy_ocid
  operating_system         = "Canonical Ubuntu"
  operating_system_version = var.ubuntu_version
  shape                    = var.shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

locals {
  # 오사카는 AD 가 1개다.
  availability_domain = data.oci_identity_availability_domains.ad.availability_domains[0].name
  is_flex             = var.shape == "VM.Standard.A1.Flex"
}

resource "oci_core_instance" "app" {
  for_each = var.envs

  compartment_id      = local.compartment_id
  availability_domain = local.availability_domain
  display_name        = "core-${each.key}"
  shape               = var.shape

  dynamic "shape_config" {
    for_each = local.is_flex ? [1] : []
    content {
      ocpus         = 1
      memory_in_gbs = 6
    }
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.ubuntu.images[0].id
    # 크기를 명시하면 최소 50GB 다. 생략하면 이미지 기본값(47GB)을 쓴다.
    # 스토리지 예산: 부트 47×2 + 데이터 50×2 = 194GB / 200GB
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.public.id
    nsg_ids          = [oci_core_network_security_group.app.id]
    assign_public_ip = false # 아래 예약 IP 를 붙인다
    hostname_label   = "core-${each.key}"
  }

  agent_config {
    is_monitoring_disabled = false
    is_management_disabled = true

    plugins_config {
      name          = "Compute Instance Monitoring"
      desired_state = "ENABLED"
    }
  }

  metadata = {
    ssh_authorized_keys = trimspace(file(pathexpand(var.ssh_public_key_path)))
    user_data = base64encode(templatefile("${path.module}/cloud-init/base.yaml.tftpl", {
      env              = each.key
      domain           = each.value.domain
      backup_bucket    = var.backup_bucket_name
      backup_namespace = var.object_storage_namespace
      region           = var.region
    }))
  }

  lifecycle {
    prevent_destroy = true
    # 새 이미지 출시나 cloud-init 수정으로 인스턴스(부트 볼륨)가 교체되지 않게 한다.
    # cloud-init 변경은 기존 서버에는 반영되지 않는다(신규 생성 시에만 적용).
    ignore_changes = [source_details[0].source_id, metadata]
  }
}

data "oci_core_vnic_attachments" "app" {
  for_each       = var.envs
  compartment_id = local.compartment_id
  instance_id    = oci_core_instance.app[each.key].id
}

data "oci_core_private_ips" "app" {
  for_each = var.envs
  vnic_id  = data.oci_core_vnic_attachments.app[each.key].vnic_attachments[0].vnic_id
}

resource "oci_core_public_ip" "app" {
  for_each       = var.envs
  compartment_id = local.compartment_id
  display_name   = "core-${each.key}-ip"
  lifetime       = "RESERVED"
  private_ip_id  = data.oci_core_private_ips.app[each.key].private_ips[0].id

  lifecycle {
    prevent_destroy = true # DNS 가 이 IP 를 가리킨다
  }
}

resource "oci_core_volume" "data" {
  for_each            = var.envs
  compartment_id      = local.compartment_id
  availability_domain = local.availability_domain
  display_name        = "core-${each.key}-data"
  size_in_gbs         = 50

  lifecycle {
    prevent_destroy = true # MySQL 데이터
  }
}

resource "oci_core_volume_attachment" "data" {
  for_each        = var.envs
  attachment_type = "paravirtualized"
  instance_id     = oci_core_instance.app[each.key].id
  volume_id       = oci_core_volume.data[each.key].id
  device          = "/dev/oracleoci/oraclevdb" # cloud-init 이 이 경로를 기다린다
}
