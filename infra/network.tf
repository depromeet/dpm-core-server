locals {
  compartment_id = oci_identity_compartment.dpm.id
}

resource "oci_core_vcn" "main" {
  depends_on     = [time_sleep.compartment_propagation]
  compartment_id = local.compartment_id
  display_name   = "dpm-vcn"
  cidr_blocks    = ["10.0.0.0/16"]
  dns_label      = "dpm"
}

resource "oci_core_internet_gateway" "main" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "dpm-igw"
  enabled        = true
}

resource "oci_core_route_table" "public" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "dpm-public-rt"

  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.main.id
  }
}

# security list 는 최소한만 둔다. 포트 허용은 NSG 에서 관리한다.
# (VCN 기본 security list 는 22 를 열어 두므로 서브넷에 붙이지 않는다)
resource "oci_core_security_list" "public" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "dpm-public-sl"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }

  # Path MTU Discovery
  ingress_security_rules {
    source   = "0.0.0.0/0"
    protocol = "1"
    icmp_options {
      type = 3
      code = 4
    }
  }
}

resource "oci_core_subnet" "public" {
  compartment_id    = local.compartment_id
  vcn_id            = oci_core_vcn.main.id
  display_name      = "dpm-public"
  cidr_block        = "10.0.1.0/24"
  dns_label         = "public"
  route_table_id    = oci_core_route_table.public.id
  security_list_ids = [oci_core_security_list.public.id]
}

resource "oci_core_network_security_group" "app" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "dpm-app"
}

# 22 는 GitHub Actions 러너 IP 가 고정되지 않아 전체 허용한다.
# 대신 cloud-init 에서 sshd 를 키 인증 전용으로 설정한다.
resource "oci_core_network_security_group_security_rule" "ingress_tcp" {
  for_each                  = toset(["22", "80", "443"])
  network_security_group_id = oci_core_network_security_group.app.id
  direction                 = "INGRESS"
  protocol                  = "6"
  source_type               = "CIDR_BLOCK"
  source                    = "0.0.0.0/0"
  stateless                 = false
  description               = "tcp/${each.key}"

  tcp_options {
    destination_port_range {
      min = tonumber(each.key)
      max = tonumber(each.key)
    }
  }
}

resource "oci_core_network_security_group_security_rule" "egress_all" {
  network_security_group_id = oci_core_network_security_group.app.id
  direction                 = "EGRESS"
  protocol                  = "all"
  destination_type          = "CIDR_BLOCK"
  destination               = "0.0.0.0/0"
  stateless                 = false
}
