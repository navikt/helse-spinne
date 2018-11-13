package no.nav.helse

import no.nav.helse.streams.Environment
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("App")

fun main() {
    val heapSize = Runtime.getRuntime().totalMemory()
    val heapMaxSize = Runtime.getRuntime().maxMemory()
    val heapFreeSize = Runtime.getRuntime().freeMemory()

    log.info("totalMemory = {}, maxMemory = {}, freeMemory = {}", heapSize, heapMaxSize, heapFreeSize)

    Behandling().start(Environment())
}
