package investor.reporting.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

data class EscrowState(
                        val taxValue: Int,
                        val insuranceValue: Int,
                        val parcelId: String,
                        val invAccountNumber: String,
                        val escrowParty: Party,
                        val investorParty: Party) : ContractState {
    override val participants get() = listOf(escrowParty,investorParty)
}