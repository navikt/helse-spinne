package no.nav.helse

import io.prometheus.client.*
import no.nav.helse.streams.*
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.*
import org.json.*
import org.slf4j.*

class Spinne(env: Environment = Environment()) {

    private val counter = Counter.build()
            .name("sykepenger_mottatte_soknader")
            .labelNames("type", "status")
            .help("Antall mottatte søknader til behandling")
            .register()

    private val appId = "spinne"

    private val log = LoggerFactory.getLogger("Spinne")

    private val consumer: StreamConsumer

    init {
        val props = streamConfig(appId, env.bootstrapServersUrl,
                env.username to env.password,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(fromSyfo(), props))
    }

    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    fun fromSyfo(): Topology {
        val builder = StreamsBuilder()

        val stream: KStream<String, JSONObject> = builder.consumeTopic(Topics.SYKEPENGESØKNADER_INN)

        stream.peek {key, value -> log.info("Consuming msg ({}) with key {}", value::class.java, key) }
              .peek { _, value -> counter.labels(value["soknadstype"].toString(), value["status"].toString()).inc() }
              .filter{ _, value -> (value["status"] as String).toUpperCase() == "SENDT" }
              .toTopic(Topics.SYKEPENGEBEHANDLING)

        return builder.build()
    }

}
