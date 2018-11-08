package no.nav.helse

import no.nav.helse.streams.JsonSerializer
import no.nav.helse.streams.Topics
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.json.JSONObject
import org.junit.Test
import java.util.*


class BehandlingTest {

    @Test
    fun `that message is consumed`() {
        val config = Properties()
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "sykepengebehandling")
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
        val testDriver = TopologyTestDriver(Behandling().behandling(), config)

        val factory = ConsumerRecordFactory<String, JSONObject>(Topics.SYKEPENGEBEHANDLING.name, StringSerializer(), JsonSerializer())

        val record = JSONObject("{\"name\": \"foo\"}")
        testDriver.pipeInput(factory.create(record))
    }
}
