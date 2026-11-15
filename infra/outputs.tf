output "public_ips" {
  description = "환경별 예약 공인 IP"
  value       = { for k, ip in oci_core_public_ip.app : k => ip.ip_address }
}

output "ssh" {
  value = {
    for k, ip in oci_core_public_ip.app :
    k => "ssh -i ${trimsuffix(var.ssh_public_key_path, ".pub")} ubuntu@${ip.ip_address}"
  }
}

output "image" {
  description = "생성 시점에 선택된 이미지 (이후 apply 에서는 무시된다)"
  value       = data.oci_core_images.ubuntu.images[0].display_name
}
