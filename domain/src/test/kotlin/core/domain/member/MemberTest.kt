package core.domain.member

import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class MemberTest : BehaviorSpec({

    Given("Member가 비활성 상태일 때") {
        val member = Member(
            name = "이한음",
            email = "ummdev03@kakao.com",
            status = MemberStatus.INACTIVE
        )

        When("활성화하면") {
            member.activate()

            Then("상태가 ACTIVE로 바뀌어야 한다") {
                member.status shouldBe MemberStatus.ACTIVE
            }

            Then("updatedAt이 현재 시각으로 변경되어야 한다") {
                member.updatedAt?.isAfter(Instant.now().minusSeconds(1)) shouldBe true
            }
        }
    }

    Given("Member가 존재할 때") {
        val member = Member(
            name = "정준원",
            email = "wjdwnsdnjs@test.com",
            status = MemberStatus.ACTIVE
        )

        When("softDelete를 호출하면") {
            member.softDelete()

            Then("상태가 WITHDRAWN으로 변경된다") {
                member.status shouldBe MemberStatus.WITHDRAWN
            }

            Then("deletedAt이 현재 시각으로 설정되어야 한다") {
                member.deletedAt?.isAfter(Instant.now().minusSeconds(1)) shouldBe true
            }
        }
    }

    Given("Member.create() 팩토리를 사용할 때") {
        When("local 환경에서 생성하면") {
            val member = Member.create("ummdev03@kakao.com", "이한음", "local")

            Then("status가 ACTIVE여야 한다") {
                member.status shouldBe MemberStatus.ACTIVE
            }
        }

        When("dev 환경에서 생성하면") {
            val member = Member.create("ummdev03@kakao.com", "이한음", "dev")

            Then("status가 ACTIVE여야 한다") {
                member.status shouldBe MemberStatus.ACTIVE
            }
        }

        When("prod 환경에서 생성하면") {
            val member = Member.create("ummdev03@kakao.com", "이한음", "prod")

            Then("status가 PENDING이어야 한다") {
                member.status shouldBe MemberStatus.PENDING
            }
        }

        When("매칭되지 않는 환경 이름으로 생성하면") {
            val member = Member.create("ummdev03@kakao.com", "이한음", "staging")

            Then("status가 WITHDRAWN이어야 한다") {
                member.status shouldBe MemberStatus.WITHDRAWN
            }
        }
    }
})
