package investor.reporting.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

data class BankState(val bankBalanceValue: Int,
                    val bankParty: Party) : ContractState {
    override val participants get() = listOf(bankParty)
}