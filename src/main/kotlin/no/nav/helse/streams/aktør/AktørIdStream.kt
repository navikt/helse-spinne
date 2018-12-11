package no.nav.helse.streams.aktør

import io.prometheus.client.Counter
import no.nav.helse.AuthHelper
import no.nav.helse.Environment
import no.nav.helse.streams.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.ValueMapper
import org.json.JSONObject
import org.slf4j.LoggerFactory

class AktørIdStream(val env: Environment,
                    val aktørregisterClient: AktørregisterClient = AktørregisterClient(baseUrl = env.aktørregisterUrl, authHelper = AuthHelper(baseUrl = env.stsBaseUrl, username = env.username!!, password = env.password!!))) {

    private val counter = Counter.build()
            .name("sykepenger_mottatte_soknader")
            .labelNames("type", "status")
            .help("Antall mottatte søknader til behandling")
            .register()

    private val appId = "spinne-aktorid"

    private val log = LoggerFactory.getLogger("AktørIdStream")

    private val consumer:StreamConsumer

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

    @Suppress("UNUSED_PARAMETER")
    private fun harAktørId(key: String?, value: JSONObject): Boolean {
        return value.has("aktorId")
    }

    fun fromSyfo(): Topology {
        val builder = StreamsBuilder()

        val stream: KStream<String, JSONObject> = builder.consumeTopic(Topics.SYKEPENGESØKNADER_INN)

        stream.peek { key, value -> log.info("Processing {} ({}) with key {}", value, value::class.java, key) }
                .peek { _, value -> counter.labels(value["soknadstype"].toString(), value["status"].toString()).inc() }
                .filter(this::harAktørId)
                .mapValues(ValueMapper<JSONObject, JSONObject> {
                    it.put("norskIdent", aktørregisterClient.gjeldendeNorskIdent(it.getString("aktorId")))
                })
                .peek {key, value -> log.info("Producing {} ({}) with key {}", value, value::class.java, key) }
                .toTopic(Topics.SYKEPENGEBEHANDLING)

        return builder.build()
    }
}
