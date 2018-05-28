package com.example.state

import com.example.schema.LoanSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording Loan agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param loanAmount the value of the loan.
 * @param lender the party lending the IOU.
 * @param borrower the party receiving and approving the loan.
 */
data class LoanState(val loanAmount: Int,
                     val borrower: Party,
                     val interestRate: Int,
                     val lender: List<Party>,
                     val status: String,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(borrower)+lender

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LoanSchemaV1-> LoanSchemaV1.PersistentLoan(
                    this.lender.toString(),
                    this.borrower.name.toString(),
                    this.loanAmount,
                    this.interestRate,
                    this.status,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(LoanSchemaV1)
}
