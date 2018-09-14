package investor.reporting.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

data class ServicingState(
                        val recoverableFeeValue: Int,
                        val preservationFeeValue: Int,
                        val legalCostValue: Int,
                        val parcelId: String,
                        val bankAccountNumber: String,
                        val servicingParty: Party,
                        val invParty: Party) : ContractState {
    override val participants get() = listOf(servicingParty,invParty)
}