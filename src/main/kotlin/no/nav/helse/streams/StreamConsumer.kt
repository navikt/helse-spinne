package no.nav.helse.streams

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

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

    fun stop() {
        streams.close()
    }

    private fun naisHttpChecks() {
        embeddedServer(Netty, env.httpPort ?: 8080) {
            routing {

                get("/isalive") {
                    call.respondText("ALIVE", ContentType.Text.Plain)
                }

                get("/isready") {
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
