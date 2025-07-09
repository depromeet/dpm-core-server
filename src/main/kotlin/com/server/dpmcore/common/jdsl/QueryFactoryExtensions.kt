package com.server.dpmcore.common.jdsl

import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.querydsl.SpringDataCriteriaQueryDsl
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import jakarta.persistence.NoResultException

inline fun <reified T : Any> SpringDataQueryFactory.singleQueryOrNull(
    noinline dsl: SpringDataCriteriaQueryDsl<T>.() -> Unit,
): T? =
    try {
        this.singleQuery(dsl)
    } catch (e: NoResultException) {
        null
    }
