package investor.reporting.flow

import co.paralleluniverse.fibers.Suspendable
import investor.reporting.state.*
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.serialization.CordaSerializable
import net.corda.finance.contracts.asset.Cash

/**
 * Gets a summary of all the transactions on the node.
 */
@CordaSerializable
@StartableByRPC
class GetTransactionsFlow : FlowLogic<List<TransactionSummary>>() {
    @Suspendable
    override fun call(): List<TransactionSummary> {
        val signedTransactions = serviceHub.validatedTransactions.track().snapshot
        val ledgerTransactions = signedTransactions.map { signedTx -> signedTx.toLedgerTransaction(serviceHub) }
        return ledgerTransactions.map { ledgerTx ->
            val inputStateTypes = ledgerTx.inputStates.map { inputState -> mapToStateSubclass(inputState) }
            val outputStateTypes = ledgerTx.outputStates.map { outputState -> mapToStateSubclass(outputState) }
            val signers = ledgerTx.commands.flatMap { it.signingParties }
            val signersAndNotary = signers + ledgerTx.notary!!
            val signerNames = signersAndNotary.map { it.name.organisation }.toSet()
            TransactionSummary(ledgerTx.id, inputStateTypes, outputStateTypes, signerNames)
        }
    }

    private fun mapToStateSubclass(state: ContractState) = when (state) {
        is EscrowState -> "Escrow (Tax & Insurance)"
        is ServicingState -> "Servicing Fees - (Recoverable,Preservation, LegalCost)"
        is EMIState -> "EMI"
        is BankState -> "Bank balance"
        is InvestorState -> "Investor balance"
        else -> "ContractState"
    }
}

@CordaSerializable
data class TransactionSummary(val hash: SecureHash, val inputs: List<String>, val outputs: List<String>, val signers: Set<String>)