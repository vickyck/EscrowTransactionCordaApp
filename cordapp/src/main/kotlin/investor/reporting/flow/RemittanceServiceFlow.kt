package investor.reporting.flow

import co.paralleluniverse.fibers.Suspendable
import investor.reporting.IRDataStructures
import investor.reporting.contract.RemittanceContract
import investor.reporting.contract.RemittanceContract.Companion.CONTRACT_ID
import investor.reporting.state.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.filterStatesOfType
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object RemittanceServiceFlow {
    @InitiatingFlow
    @StartableByRPC
    class BankEscrowTXFlow(val props: IRDataStructures.BankEscrowTX) : FlowLogic<SignedTransaction>() {
        override val progressTracker: ProgressTracker = ProgressTracker(GETTING_NOTARY, GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION, SIGNING_TRANSACTION, FINALISING_TRANSACTION)


        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = GETTING_NOTARY
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            progressTracker.currentStep = GENERATING_TRANSACTION

            //validate to see enough bank balance is available to transfer escrow to investor

            val bankStates = serviceHub.vaultService.queryBy<BankState>().states
            if (bankStates.isEmpty())
                throw Exception("BankEscrowTXFlow:: No BankStates were found")
            val lastBankState = bankStates.last().state.data
            if (lastBankState.bankBalanceValue <= 0)
                throw Exception("BankEscrowTXFlow:: Not enough funds available, Bank balance is < 0")

            //get the default Tax & Insurance values from vault
            val escrowState: EscrowState
            val escrowStates = serviceHub.vaultService.queryBy<EscrowState>().states
            if (escrowStates.isEmpty())
            {
                escrowState = EscrowState(props.taxVal,props.insuranceVal,props.parcelId,props.invAccountNum,props.taxVal,props.insuranceVal,props.escrowParty,props.invParty)
            }
            else
            {
                val lastEscrowState = escrowStates.last().state.data;
                //prepare the new escrow state
                escrowState = EscrowState(lastEscrowState.taxValue + props.taxVal,
                        lastEscrowState.insuranceValue + props.insuranceVal,
                        props.parcelId,props.invAccountNum,props.taxVal,props.insuranceVal,props.escrowParty,props.invParty)
            }


            //prepare emi state
            val EMI_EscrowDeducted = props.emiVal - (props.taxVal + props.insuranceVal)

            //validate the emi to see if it can be serviceable
            if (EMI_EscrowDeducted <= 0)
                throw Exception("BankEscrowTXFlow:: Escrow can not be serviced as Emi -(tax + insurance) is <= 0")

            if (lastBankState.bankBalanceValue < (EMI_EscrowDeducted + props.taxVal + props.insuranceVal))
                throw Exception("BankEscrowTXFlow:: Not enough balance to serve Escrow TX to Investor")

            val emiStates = serviceHub.vaultService.queryBy<EMIState>().states
            val emiState: EMIState
            if(emiStates.isEmpty())
            {
                emiState = EMIState(EMI_EscrowDeducted,EMI_EscrowDeducted, props.parcelId, props.invAccountNum, ourIdentity, props.invParty)
            }
            else{
                val lastEMIState = emiStates.last().state.data
                emiState = EMIState(EMI_EscrowDeducted+lastEMIState.value,EMI_EscrowDeducted,props.parcelId, props.invAccountNum, ourIdentity, props.invParty)
            }

            //prepare the bank state with new available balance

            val newBankState = BankState(lastBankState.bankBalanceValue - (EMI_EscrowDeducted + props.taxVal + props.insuranceVal), ourIdentity)

            //update the investor balance
            val newInvState: InvestorState
            val invStates = serviceHub.vaultService.queryBy<InvestorState>().states

            if (invStates.isEmpty())
            {
                newInvState = InvestorState(EMI_EscrowDeducted, props.parcelId, ourIdentity,props.invParty)
            }
            else
            {
                val lastInvState = invStates.last().state.data;
                newInvState = InvestorState(lastInvState.investorBalanceValue + EMI_EscrowDeducted, props.parcelId, ourIdentity,props.invParty)
            }

            val builder = TransactionBuilder(notary = notary)
                    .addOutputState(emiState, CONTRACT_ID)
                    .addOutputState(escrowState, CONTRACT_ID)
                    .addOutputState(newBankState, CONTRACT_ID)
                    .addOutputState(newInvState, CONTRACT_ID)
                    .addCommand(RemittanceContract.Commands.RemitEscrow(), ourIdentity.owningKey)

            progressTracker.currentStep = VERIFYING_TRANSACTION
            builder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = serviceHub.signInitialTransaction(builder)

            // Send the state to the counterparty (investor), and receive it back with their signature.
            val otherPartyFlow = initiateFlow(props.invParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION

            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }

        @InitiatedBy(BankEscrowTXFlow::class)
        class BankEscrowTXAcceptorFlow(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
            @Suspendable
            override fun call(): SignedTransaction {
                val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        val output = stx.tx.outputs.single().data
                        //"This must be an Trade transaction." using (output is EscrowState)
                    }
                }
                return subFlow(signTransactionFlow)
            }
        }
    }

    @InitiatingFlow
    @StartableByRPC
    class BankServicingTXFlow(val props: IRDataStructures.BankServicingTX) : FlowLogic<SignedTransaction>() {
        override val progressTracker: ProgressTracker = ProgressTracker(GETTING_NOTARY, GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION, SIGNING_TRANSACTION, FINALISING_TRANSACTION)


        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = GETTING_NOTARY
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            progressTracker.currentStep = GENERATING_TRANSACTION
            val bankStates = serviceHub.vaultService.queryBy<BankState>().states
            if (bankStates.isEmpty())
                throw Exception("BankServicingTXFlow:: No base BankStates found")
            val lastBankState = bankStates.last().state.data
            if (lastBankState.bankBalanceValue <= 0)
                throw Exception("BankServicingTXFlow:: Not enough funds available, Bank balance is < 0")



            //get the default recoverableFeeValue, preservationFeeValue & legalCostValue values from vault
            val baseServicingStates = serviceHub.vaultService.queryBy<ServicingState>().states
            //if (baseServicingStates.isEmpty())
           //     throw Exception("No base states found for ServicingState")
           // val baseServicingState = baseServicingStates.last().state.data;

            //prepare emi state
            val EMI_ServicingDeducted = props.emiVal - (props.recoverableFeeValue + props.preservationFeeValue + props.legalCostValue)

            //validate the emi to see if it can be serviceable
            if (EMI_ServicingDeducted <= 0)
                throw Exception("BankServicingTXFlow:: Escrow can not be serviced as Emi -(recoverableFeeValue + preservationFeeValue + legalCostValue) is <= 0")

            if (lastBankState.bankBalanceValue < EMI_ServicingDeducted)
                throw Exception("BankServicingTXFlow:: Not enough balance to serve Servicing TX to Investor")

            val emiStates = serviceHub.vaultService.queryBy<EMIState>().states
            val emiState: EMIState
            if(emiStates.isEmpty())
            {
                emiState = EMIState(EMI_ServicingDeducted,EMI_ServicingDeducted, props.parcelId, props.invAccountNum, ourIdentity, props.invParty)
            }
            else{
                val lastEMIState = emiStates.last().state.data
                emiState = EMIState(EMI_ServicingDeducted+lastEMIState.value,EMI_ServicingDeducted, props.parcelId, props.invAccountNum, ourIdentity, props.invParty)
            }

            val servicingState : ServicingState
            if (baseServicingStates.isEmpty())
            {
                servicingState = ServicingState(props.recoverableFeeValue, props.preservationFeeValue, props.legalCostValue, props.parcelId,props.invAccountNum,ourIdentity, props.invParty)
            }else
            {
                val baseServicingState = baseServicingStates.last().state.data;
                //prepare the new servicing state
                servicingState = ServicingState(baseServicingState.recoverableFeeValue + props.recoverableFeeValue,
                        baseServicingState.preservationFeeValue + props.preservationFeeValue,
                        baseServicingState.legalCostValue + props.legalCostValue,
                        props.parcelId,props.invAccountNum,props.servicingParty,props.invParty)
            }

            //prepare the bank state with new available balance

            val newBankState = BankState((lastBankState.bankBalanceValue - (EMI_ServicingDeducted+props.recoverableFeeValue+props.preservationFeeValue+props.legalCostValue )), ourIdentity)

            //update the investor balance
            val newInvState: InvestorState
            val invStates = serviceHub.vaultService.queryBy<InvestorState>().states
            if (invStates.isEmpty())
            {
                newInvState = InvestorState(EMI_ServicingDeducted , props.parcelId, ourIdentity,props.invParty)
            }
            else{
                val lastInvState = invStates.last().state.data;
                newInvState = InvestorState(lastInvState.investorBalanceValue + EMI_ServicingDeducted , props.parcelId, ourIdentity,props.invParty)
            }
            val builder = TransactionBuilder(notary = notary)
                    .addOutputState(emiState, CONTRACT_ID)
                    .addOutputState(servicingState, CONTRACT_ID)
                    .addOutputState(newBankState, CONTRACT_ID)
                    .addOutputState(newInvState, CONTRACT_ID)
                    .addCommand(RemittanceContract.Commands.RemitServicing(), ourIdentity.owningKey)

            progressTracker.currentStep = VERIFYING_TRANSACTION
            builder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = serviceHub.signInitialTransaction(builder)

            // Send the state to the counterparty (investor), and receive it back with their signature.
            val otherPartyFlow = initiateFlow(props.invParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow)))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION

            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }

        @InitiatedBy(BankServicingTXFlow::class)
        class BankServicingTXAcceptorFlow(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
            @Suspendable
            override fun call(): SignedTransaction {
                val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        val output = stx.tx.outputs.single().data
                        //"This must be an Trade transaction." using (output is EscrowState)
                    }
                }
                return subFlow(signTransactionFlow)
            }
        }
    }
}