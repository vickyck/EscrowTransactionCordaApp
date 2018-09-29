package investor.reporting.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

data class EMIState(
                val value: Int,
                val receivedAmount: Int,
                val parcelId: String,
                val invAccountNumber: String,
                val bank: Party,
                val investor: Party) : ContractState {
    override val participants get() = listOf(bank, investor)
}