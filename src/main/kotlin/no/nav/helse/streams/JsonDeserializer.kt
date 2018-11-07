package no.nav.helse.streams

import org.apache.kafka.common.serialization.*
import org.json.*
import org.slf4j.*

class JsonDeserializer: Deserializer<JSONObject> {
    private val log = LoggerFactory.getLogger("JsonDeserializer")

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) { }

    override fun deserialize(topic: String?, data: ByteArray?): JSONObject? {
        return data?.let {
            val json = String(it)
            try {
                JSONObject(json)
            } catch (ex: Exception) {
                log.warn("'$json' is not valid json")
                null
            }
        }
    }

    override fun close() { }

}
