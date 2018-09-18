package investor.reporting.plugin

import investor.reporting.api.InvestorReportingApi
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class InvestorReportingPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis = listOf(Function(::InvestorReportingApi))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    override val staticServeDirs = mapOf(
            // This will serve the exampleWeb directory in resources to /web/example
            "bank" to javaClass.classLoader.getResource("bankWeb").toExternalForm(),
            "investor" to javaClass.classLoader.getResource("investorWeb").toExternalForm(),
            "template" to javaClass.classLoader.getResource("templateWeb").toExternalForm()
    )
}