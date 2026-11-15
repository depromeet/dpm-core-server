# 기본값(dns_cutover_envs = [])에서는 아무 레코드도 관리하지 않는다.
# 이미 Cloudflare 에 있는 레코드를 Terraform 으로 넘겨받으려면, 켜기 전에 import 가 필요하다 (README 참고).
resource "cloudflare_dns_record" "app" {
  for_each = var.dns_cutover_envs

  zone_id = var.cloudflare_zone_id
  name    = var.envs[each.key].domain
  type    = "A"
  content = oci_core_public_ip.app[each.key].ip_address
  ttl     = 300
  proxied = false

  lifecycle {
    precondition {
      condition     = var.cloudflare_zone_id != ""
      error_message = "dns_cutover_envs 를 쓰려면 cloudflare_zone_id 가 필요하다."
    }
  }
}
