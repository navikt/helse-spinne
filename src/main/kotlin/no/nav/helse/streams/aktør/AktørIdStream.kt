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

    private val acceptCounter = Counter.build()
            .name("aktor_id_stream_counter")
            .labelNames("state")
            .help("Antall meldinger som 'AktørIdStream' har godtatt og forsøkt behandlet")
            .register()

    private val appId = "spinne-aktorid"

    private val log = LoggerFactory.getLogger("AktørIdStream")

    private val consumer:StreamConsumer

    init {
        val props = streamConfig(appId, env.bootstrapServersUrl,
                env.username to env.password,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(aktørId(), props))
    }

    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun harNorskIdent(key: String?, value: JSONObject): Boolean {
        return value.has("norskIdent")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun harAktørId(key: String?, value: JSONObject): Boolean {
        return value.has("aktorId")
    }

    fun aktørId(): Topology {
        val builder = StreamsBuilder()

        val stream: KStream<String, JSONObject> = builder.consumeTopic(Topics.SYKEPENGEBEHANDLING)

        stream.peek { key, value -> log.info("Processing {} ({}) with key {}", value, value::class.java, key) }
                .filterNot(this::harNorskIdent)
                .peek { key, value -> log.info("Message {} ({}) with key {} does not have norskIdent", value, value::class.java, key) }
                .filter(this::harAktørId)
                .peek { _, _ -> acceptCounter.labels("accepted").inc()}
                .mapValues(ValueMapper<JSONObject, JSONObject> {
                    it.put("norskIdent", aktørregisterClient.gjeldendeNorskIdent(it.getString("aktorId")))
                })
                .peek {key, value -> log.info("Producing {} ({}) with key {}", value, value::class.java, key) }
                .peek { _, _ -> acceptCounter.labels("success").inc()}
                .toTopic(Topics.SYKEPENGEBEHANDLING)

        return builder.build()
    }
}
