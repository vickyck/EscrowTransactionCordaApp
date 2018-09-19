package investor.reporting.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

class InvestorState(val investorBalanceValue: Int,
                    val parcelId: String,
                    val bankParty: Party,
                    val InvestorParty: Party) : ContractState {
    override val participants get() = listOf(bankParty, InvestorParty)
}