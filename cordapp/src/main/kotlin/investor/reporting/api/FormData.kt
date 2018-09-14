package investor.reporting.api

import investor.reporting.IRDataStructures
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.finance.DOLLARS
import java.io.InputStream
import java.time.LocalDate
import java.time.Period
import java.util.*

/** These classes capture the JSON form data passed from the front-end. */

/**
 * The escrow transaction that is initiated by the front-end to create an
 * escrow state, update bank and investor states along with emi state.
 */
data class BankEscrowTXInputData(
        val emiVal: Int,
        val taxVal: Int,
        val insuranceVal: Int,
        val parcelId: String,
        val invAccountNum: String,
        val escrowParty: Party,
        val invParty: Party) {

    /**
     * Converts the [BankEscrowTXInputData] submitted from the front-end into the
     * properties
     */
    fun toBankEscrowTXProperties() = IRDataStructures.BankEscrowTX(
            emiVal = emiVal,
            taxVal = taxVal,
            insuranceVal = insuranceVal,
            parcelId = parcelId,
            invAccountNum = invAccountNum,
            escrowParty = escrowParty,
            invParty = invParty)
}

data class BankServicingTXInputData(
        val emiVal: Int,
        val recoverableFeeValue: Int,
        val preservationFeeValue: Int,
        val legalCostValue: Int,
        val parcelId: String,
        val invAccountNum: String,
        val servicingParty: Party,
        val invParty: Party) {

    /**
     * Converts the [BankInvestorTXInputData] submitted from the front-end into the
     * properties
     */
    fun toBankServicingTXProperties() = IRDataStructures.BankServicingTX(
            emiVal = emiVal,
            recoverableFeeValue = recoverableFeeValue,
            preservationFeeValue = preservationFeeValue,
            legalCostValue = legalCostValue,
            parcelId = parcelId,
            invAccountNum = invAccountNum,
            servicingParty = servicingParty,
            invParty = invParty)
}