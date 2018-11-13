package no.nav.helse

import no.nav.helse.streams.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.ValueMapper
import org.json.JSONObject
import org.slf4j.LoggerFactory

class AktørIdStream(val aktørregisterClient: AktørregisterClient) {

    private val appId = "sykepengebehandling-aktorid"

    private val log = LoggerFactory.getLogger("AktørIdStream")

    fun start(env: Environment) {
        StreamConsumer(appId, env, KafkaStreams(aktørId(), streamConfig(appId, env))).start()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun harFnr(key: String?, value: JSONObject): Boolean {
        return value.has("fnr")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun harAktørId(key: String?, value: JSONObject): Boolean {
        return value.has("aktoerId")
    }

    fun aktørId(): Topology {
        val builder = StreamsBuilder()

        val stream: KStream<String, JSONObject> = builder.consumeTopic(Topics.SYKEPENGEBEHANDLING)

        stream.filter(this::harFnr)
                .filterNot(this::harAktørId)
                .peek { key, value -> log.info("Processing {} ({}) with key {}", value, value::class.java, key) }
                .mapValues(ValueMapper<JSONObject, JSONObject> {
                    it.put("aktoerId", aktørregisterClient.gjeldendeIdent(it.getString("fnr")))
                })
                .peek {key, value -> log.info("Producing {} ({}) with key {}", value, value::class.java, key) }
                .toTopic(Topics.SYKEPENGEBEHANDLING)

        return builder.build()
    }
}
