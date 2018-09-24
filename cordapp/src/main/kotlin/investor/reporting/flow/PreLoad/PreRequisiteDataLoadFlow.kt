package investor.reporting.flow.PreLoad
import co.paralleluniverse.fibers.Suspendable
import investor.reporting.contract.PreLoad.TEMPLATE_CONTRACT_ID
import investor.reporting.contract.PreLoad.TemplateContract
import investor.reporting.state.BankState
import investor.reporting.state.EscrowState
import investor.reporting.state.InvestorState
import investor.reporting.state.ServicingState
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.queryBy
import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@InitiatingFlow
@StartableByRPC
class PreRequisiteDataLoadFlow() : FlowLogic<Unit>() {

    /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
    override val progressTracker = ProgressTracker()

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call() {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val investorParty = serviceHub.identityService.partiesFromName("InvestorPartyB", false).singleOrNull()
                ?: throw IllegalArgumentException("No exact match found for buyer name InvestorPartyB.")
        //get the default Tax & Insurance values from vault
        val baseEscrowStates = serviceHub.vaultService.queryBy<EscrowState>().states
        if (baseEscrowStates.size > 1)
            throw Exception("base escrow state already exist")

        // We create the transaction components.
        val escrowState = EscrowState(10, 10,"e0adbf57-eb1e-4455-956e-f30a4f43006c","5098674354",ourIdentity,investorParty)
        val servicingState = ServicingState(10, 10, 10, "47314fb0-5dc0-46ad-afd6-76dd5916d884","2549863072",ourIdentity, investorParty)
        val investorState = InvestorState(10, "e0adbf57-eb1e-4455-956e-f30a4f43006d", ourIdentity, investorParty)
        val bankState = BankState(10000,ourIdentity)
        val cmd = Command(TemplateContract.Commands.Action(), ourIdentity.owningKey)

        // We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(escrowState, TEMPLATE_CONTRACT_ID)
                .addOutputState(servicingState, TEMPLATE_CONTRACT_ID)
                .addOutputState(investorState, TEMPLATE_CONTRACT_ID)
                .addOutputState(bankState, TEMPLATE_CONTRACT_ID)
                .addCommand(cmd)

        // We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We finalise the transaction.
        subFlow(FinalityFlow(signedTx))
    }
}
