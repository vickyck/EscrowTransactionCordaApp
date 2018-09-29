package investor.reporting.api

import investor.reporting.IRDataStructures
import investor.reporting.flow.GetTransactionsFlow
import investor.reporting.flow.PreLoad.PreRequisiteDataLoadFlow
import investor.reporting.flow.ReceivePaymentFlow
import investor.reporting.flow.RemittanceServiceFlow.BankEscrowTXFlow
import investor.reporting.flow.RemittanceServiceFlow.BankServicingTXFlow
import investor.reporting.state.*
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.security.PublicKey
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/invreporting. All paths specified below are relative to it.
@Path("invreporting")
class InvestorReportingApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<InvestorReportingApi>()
    }

    // Accessible at /api/invreporting/templateGetEndpoint.
    @GET
    @Path("templateGetEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    fun templateGetEndpoint(): Response {
        return Response.ok("Template GET endpoint.").build()
    }
    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all Escrow states that exist in the node's vault.
     */
    @GET
    @Path("escrow-all")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllEscrow() = rpcOps.vaultQueryBy<EscrowState>().states

    /**
     * Displays all Servicing states that exist in the node's vault.
     */
    @GET
    @Path("servicing-all")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllServicing() = rpcOps.vaultQueryBy<ServicingState>().states

    /**
     * Displays all Investor states that exist in the node's vault.
     */
    @GET
    @Path("investor-all")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllInvestor() = rpcOps.vaultQueryBy<InvestorState>().states

    /**
     * Displays all EMI states that exist in the node's vault.
     */
    @GET
    @Path("investor-payments")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllInvestorReceivedPayments() = rpcOps.vaultQueryBy<EMIState>().states

    /**
     * Displays all Escrow states that exist in the node's vault.
     */
    @GET
    @Path("bank-balance")
    @Produces(MediaType.APPLICATION_JSON)
    fun getBankBalance() = rpcOps.vaultQueryBy<BankState>().states.last().state.data

    /**
     * Displays all Investor states that exist in the node's vault.
     */
    @GET
    @Path("investor-balance")
    @Produces(MediaType.APPLICATION_JSON)
    fun getInvestorBalance() = rpcOps.vaultQueryBy<InvestorState>().states.last().state.data

    /**
     * Initiates a flow to create master data for tax, insurance, servicing, investor balance, bank balance
     */
    @PUT
    @Path("create-default-data")
    fun createMasterData(): Response {

        //val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?:
        //return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()

        return try {
            val signedTx = rpcOps.startTrackedFlow(::PreRequisiteDataLoadFlow).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }
    /**
     * Initiates a flow to receive payment and updates bank balance
     */
    @PUT
    @Path("receive-payment-post")
    fun postReceivePayment(@QueryParam("amount") amount: Int): Response {

        //val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?:
        //return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()

        return try {
            val signedTx = rpcOps.startFlow(::ReceivePaymentFlow,amount).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }
    @PUT
    @Path("create-escrow")
    fun createEscrowTX(bankTXInputData : BankEscrowTXInputData): Response {

        //val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?:
        //return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()

        return try {
            val signedTx = rpcOps.startFlow(::BankEscrowTXFlow, bankTXInputData.toBankEscrowTXProperties()).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction ${signedTx.id} id committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @PUT
    @Path("create-servicing")
    fun createServicingTX(bankTXInputData : BankServicingTXInputData): Response {

        //val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?:
        //return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()

        return try {
            val signedTx = rpcOps.startFlow(::BankServicingTXFlow, bankTXInputData.toBankServicingTXProperties()).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction ${signedTx.id} id committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }
    /**
     * Fetches all transactions and returns them as a list of <ID, input types, output types> triples.
     */
    @GET
    @Path("transactions")
    fun transactions(): Response {
        val flowFuture = rpcOps.startFlow(::GetTransactionsFlow).returnValue
        val result = try {
            flowFuture.getOrThrow()
        } catch (e: Exception) {
            return Response.status(BAD_REQUEST).entity(e.message).build()
        }

        return Response.ok(result, MediaType.APPLICATION_JSON).build()
    }

}
data class TxSummary(val first: String, val second: List<ByteArray>, val third: ContractState?, val fourth: List<Party?>)