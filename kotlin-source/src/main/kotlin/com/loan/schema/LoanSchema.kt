package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for LoanState.
 */
object LoanSchema

/**
 * An LoanState schema.
 */
object LoanSchemaV1 : MappedSchema(
        schemaFamily = LoanSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentLoan::class.java)) {
    @Entity
    @Table(name = "loan_states")
    class PersistentLoan(
            @Column(name = "lender")
            var lenderName: String,

            @Column(name = "borrower")
            var borrowerName: String,

            @Column(name = "loanAmount")
            var loanAmount: Int,

            @Column(name = "interestRate")
            var interestRate: Int,

            @Column(name = "status")
            var status: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "",0, 0,"", UUID.randomUUID())
    }
}