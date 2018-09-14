package investor.reporting

import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.LocalDate
import java.util.*

object IRDataStructures {
    @CordaSerializable
    data class BankEscrowTX(val emiVal: Int,
                               val taxVal: Int,
                               val insuranceVal: Int,
                               val parcelId: String,
                               val invAccountNum: String,
                               val escrowParty: Party,
                               val invParty: Party)

    @CordaSerializable
    data class BankServicingTX(val emiVal: Int,
                                val recoverableFeeValue: Int,
                                val preservationFeeValue: Int,
                                val legalCostValue: Int,
                                val parcelId: String,
                                val invAccountNum: String,
                                val servicingParty: Party,
                                val invParty: Party)
}