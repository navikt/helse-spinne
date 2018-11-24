package no.nav.helse

import no.nav.helse.streams.aktør.AktørIdStream

class Spinne {

    fun start(env: Environment) {
        AktørIdStream(env).start()
    }

}
