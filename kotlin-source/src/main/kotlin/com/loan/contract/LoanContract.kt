package com.example.contract

import com.example.state.LoanState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contract for loan example in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [LoanState], which in turn encapsulates an [Loan].
 *
 * For a new [Loan] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [Loantate].
 * - Borrow() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class LoanContract : Contract {
    companion object {
        @JvmStatic
        val Loan_CONTRACT_ID = "com.example.contract.LoanContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        if(command.value is Commands.Borrow ){
            requireThat {
                // Generic constraints around the IOU transaction.
                "No inputs should be consumed when creating a borrow request." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                val out = tx.outputsOfType<LoanState>().single()
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                // Loan-specific constraints.
                "The Loan amount must be non-negative." using (out.loanAmount > 0)
            }
        } else if(command.value is Commands.Lend) {
            requireThat {
                "Only one output state should be created." using (tx.outputs.size == 1)
                val out = tx.outputsOfType<LoanState>().single()
                "The lender and the borrower cannot be the same entity." using (out.borrower != out.lender[0])
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                // Loan-specific constraints.
                "The Loan amount must be non-negative." using (out.loanAmount > 0)

                // Loan-specific constraints.
                "The Maximum Interest rate should be 20%" using(out.interestRate <= 20);
            }
        }else if(command.value is Commands.Confirm){
            requireThat {
                // Generic constraints around the IOU transaction.
                "Inputs should be consumed when creating a borrow request." using (tx.inputs.isNotEmpty())
                val out = tx.outputsOfType<LoanState>().single()
                "The lender and the borrower cannot be the same entity." using (out.borrower != out.lender[0])
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                // Loan-specific constraints.
                "The Loan amount must be non-negative." using (out.loanAmount > 0)
            }
        }

    }

    /**
     * This contract only implements Borrow, Lend and Confirm.
     */
    interface Commands : CommandData {
        class Borrow : Commands
        class Lend : Commands
        class Confirm: Commands
    }
}
