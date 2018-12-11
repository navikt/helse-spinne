package no.nav.helse

import org.slf4j.LoggerFactory
import kotlin.concurrent.*

private val log = LoggerFactory.getLogger("Spinne")

fun main() {
    log.info("Starting Spinne")

    val spinne = Spinne()

    spinne.start()

    Runtime.getRuntime().addShutdownHook(thread {
        spinne.stop()
    })
}

