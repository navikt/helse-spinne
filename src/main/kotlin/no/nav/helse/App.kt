package no.nav.helse

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Spinne")

fun main() {
    log.info("Starting Spinne")

    Spinne().start()
}

