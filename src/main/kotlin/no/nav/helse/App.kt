package no.nav.helse

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.prometheus.client.*
import io.prometheus.client.exporter.common.*
import kotlinx.coroutines.experimental.*
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.slf4j.*
import java.util.concurrent.TimeUnit

private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
private val søknadCounter = Counter.build().name("soknader").help("Antall soknader mottatt").register()
private val log = LoggerFactory.getLogger("no.nav.helse.App")


fun main(args: Array<String>) = runBlocking(block = {
    log.info("Starter sykepengebehandling")

    val builder = StreamsBuilder()

    val sykepengesøknader = builder.stream<String, String>("sykepengesoknader")

    startWebserver()
})

private fun startWebserver() {
    embeddedServer(Netty, 8080) {
        routing {
            get("/isalive") {
                call.respond(HttpStatusCode.OK)
            }
            get("/isready") {
                call.respond(HttpStatusCode.OK)
            }
            get("/metrics") {
                val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
                call.respondWrite(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                    TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                }
            }
        }
    }.start(wait = true)
}
