package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.identity.Party

// *****************
// * Contract Code *
// *****************

val TEMPLATE_CONTRACT_ID = "com.template.TemplateContract"
val IOU_CONTRACT_ID = "com.template.IOUContract"
class TemplateContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val ID = "com.template.TemplateContract"
    }
    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Action : Commands
    }
}

// *********
// * State *
// *********
class EMIState(val value: Int,
               val bank: Party,
               val investor: Party) : ContractState {
    override val participants get() = listOf(bank, investor)
}

class EscrowState(val taxValue: Int,
                  val insuranceValue: Int,
                  val EscrowParty: Party) : ContractState {
    override val participants get() = listOf(EscrowParty)
}

class ServicingState(val recoverableFeeValue: Int,
                     val preservationFeeValue: Int,
                     val legalCostValue: Int,
                     val ServicingParty: Party) : ContractState {
    override val participants get() = listOf(ServicingParty)
}

class InvestorState(val investorBalanceValue: Int,
                    val InvestorParty: Party) : ContractState {
    override val participants get() = listOf(InvestorParty)
}