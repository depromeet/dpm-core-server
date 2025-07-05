package com.server.dpmcore.team.infrastructure.entity

import com.server.dpmcore.cohort.infrastructure.entity.CohortEntity
import com.server.dpmcore.member.memberTeam.infrastructure.entity.MemberTeamEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "teams")
class TeamEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id", nullable = false, updatable = false)
    val id: Long,

    @Column(nullable = false)
    val number: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val cohort: CohortEntity,

    @Column(nullable = false, updatable = false)
    val createdAt: Long,

    @Column(nullable = false)
    val updatedAt: Long,

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberTeams: MutableList<MemberTeamEntity> = mutableListOf(),
)
