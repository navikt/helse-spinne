package no.nav.helse

import no.nav.helse.streams.Environment
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLING
import no.nav.helse.streams.consumeTopic
import no.nav.helse.streams.streamConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory

class Behandling {

    private val appId = "sykepengebehandling"
    private val env: Environment = Environment()

    private val log = LoggerFactory.getLogger("Behandling")

    fun start() {
        StreamConsumer(appId, Environment(), behandling()).start()
    }

    private fun behandling(): KafkaStreams {
        val builder = StreamsBuilder()

        builder.consumeTopic(SYKEPENGEBEHANDLING)
                .peek { key, value -> log.info("Processing ${value.javaClass} with key $key") }

        return KafkaStreams(builder.build(), streamConfig(appId, env))
    }

}
