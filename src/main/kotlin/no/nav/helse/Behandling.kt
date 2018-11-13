package no.nav.helse

import no.nav.helse.streams.Environment

class Behandling {

    fun start(env: Environment) {
        AktørIdStream(env).start()
    }

}
