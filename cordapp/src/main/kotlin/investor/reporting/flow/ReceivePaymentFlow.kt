package investor.reporting.flow

import co.paralleluniverse.fibers.Suspendable
import investor.reporting.contract.PreLoad.TEMPLATE_CONTRACT_ID
import investor.reporting.contract.PreLoad.TemplateContract
import investor.reporting.state.BankState
import investor.reporting.state.EscrowState
import investor.reporting.state.InvestorState
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ReceivePaymentFlow(val amount:Int) : FlowLogic<Unit>() {
    /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
    override val progressTracker = ProgressTracker()

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call() {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val investorParty = serviceHub.identityService.partiesFromName("InvestorPartyB", false).singleOrNull()
                ?: throw IllegalArgumentException("No exact match found for buyer name InvestorPartyB.")
        val bankState: BankState
        //get the default Tax & Insurance values from vault
        val bankStates = serviceHub.vaultService.queryBy<BankState>().states
        if (bankStates.isEmpty())
        {
           bankState = BankState(amount,ourIdentity)
        }
        else{
            val lastBankState = bankStates.last().state.data
            bankState = BankState(lastBankState.bankBalanceValue+amount, ourIdentity)
        }
        val cmd = Command(TemplateContract.Commands.Action(), ourIdentity.owningKey)

        // We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(bankState, TEMPLATE_CONTRACT_ID)
                .addCommand(cmd)

        // We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We finalise the transaction.
        subFlow(FinalityFlow(signedTx))
    }
}