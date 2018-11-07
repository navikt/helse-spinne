package no.nav.helse.streams

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.prometheus.client.*
import io.prometheus.client.exporter.common.*
import io.prometheus.client.hotspot.*
import org.apache.kafka.streams.*
import org.slf4j.*

class StreamConsumer(val consumerName: String,
                     val env: Environment,
                     val streams: KafkaStreams) {

    private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val log = LoggerFactory.getLogger(consumerName)

    fun start() {
        DefaultExports.initialize()
        naisHttpChecks()
        streams.start()
        log.info("Started stream consumer $consumerName")
        addShutdownHook()
    }

    private fun naisHttpChecks() {
        embeddedServer(Netty, env.httpPort ?: 8080) {
            routing {

                get("/isAlive") {
                    call.respondText("ALIVE", ContentType.Text.Plain)
                }

                get("/isReady") {
                    call.respondText("READY", ContentType.Text.Plain)
                }

                get("/metrics") {
                    val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
                    call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                        TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                    }
                }
            }
        }.start(wait = false)
    }

    private fun addShutdownHook() {
        Thread.currentThread().setUncaughtExceptionHandler { _, ex ->
            log.error("Caught exception, exiting", ex)
            streams.close()
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            streams.close()
        })
    }

}
