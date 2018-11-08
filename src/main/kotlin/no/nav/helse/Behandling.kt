package no.nav.helse

import no.nav.helse.streams.Environment
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLING
import no.nav.helse.streams.consumeTopic
import no.nav.helse.streams.streamConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory

class Behandling {

    private val appId = "sykepengebehandling"

    private val log = LoggerFactory.getLogger("Behandling")

    fun start() {
        val env = Environment()
        StreamConsumer(appId, env, KafkaStreams(behandling(), streamConfig(appId, env))).start()
    }

    fun behandling(): Topology {
        val builder = StreamsBuilder()

        builder.consumeTopic(SYKEPENGEBEHANDLING)
                .peek { key, value -> log.info("Processing {} with key {}", value, key) }

        return builder.build()
    }

}
