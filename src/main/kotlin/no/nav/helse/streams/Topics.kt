package no.nav.helse.streams

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

object Topics {
    val SYKEPENGEBEHANDLING = Topic(
            name = "privat-sykepengebehandling",
            keySerde = Serdes.String(),
            valueSerde = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())
    )
}

fun <K: Any, V: Any> StreamsBuilder.consumeTopic(topic: Topic<K, V>): KStream<K, V> {
    return stream<K, V>(
            topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
    )
}

fun <K, V> KStream<K, V>.toTopic(topic: Topic<K, V>) {
    return to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}
