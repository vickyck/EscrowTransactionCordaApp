package investor.reporting.contract

import investor.reporting.state.EscrowState
import investor.reporting.state.InvestorState
import investor.reporting.state.ServicingState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.*

class RemittanceContract : Contract {
    companion object {
        @JvmStatic
        val CONTRACT_ID = "investor.reporting.contract.RemittanceContract"
    }

    interface Commands : CommandData
    {
        class RemitEscrow() : TypeOnlyCommandData(), Commands
        class RemitServicing() : TypeOnlyCommandData(), Commands
    }
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when(command.value)
        {
            is Commands.RemitEscrow -> {
                val issueOutput = tx.outputsOfType<EscrowState>().single()
                requireThat {
                    //                    "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
//                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<EscrowState>().single()

                }
            }
            is Commands.RemitServicing ->{
                // TODO: Add checking here.
                val issueOutput = tx.outputsOfType<ServicingState>().single()
                requireThat {
                    //                    "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
//                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<ServicingState>().single()

                }
            }
        }
    }
}