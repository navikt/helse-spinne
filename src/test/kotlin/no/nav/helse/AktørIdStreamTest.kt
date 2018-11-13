package no.nav.helse

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.streams.Environment
import no.nav.helse.streams.JsonDeserializer
import no.nav.helse.streams.JsonSerializer
import no.nav.helse.streams.Topics
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*


class AktørIdStreamTest {

    @Test
    fun `that aktørId is added to message`() {
        val aktørregisterClientMock = mockk<AktørregisterClient>()
        every {
            aktørregisterClientMock.gjeldendeIdent("12345678911")
        } returns "1573082186699"

        val config = Properties()
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "sykepengebehandling")
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234")
        val testDriver = TopologyTestDriver(AktørIdStream(Environment(), aktørregisterClientMock).aktørId(), config)

        val factory = ConsumerRecordFactory<String, JSONObject>(Topics.SYKEPENGEBEHANDLING.name, StringSerializer(), JsonSerializer())

        val record = JSONObject("{\"name\": \"Ole Hansen\", \"fnr\": \"12345678911\"}")
        testDriver.pipeInput(factory.create(record))

        val outputRecord = testDriver.readOutput(Topics.SYKEPENGEBEHANDLING.name, StringDeserializer(), JsonDeserializer())

        Assertions.assertEquals("1573082186699", outputRecord.value().getString("aktoerId"))
    }
}
