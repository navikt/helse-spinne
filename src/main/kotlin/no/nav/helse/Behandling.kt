package no.nav.helse

import no.nav.helse.streams.Environment

class Behandling {

    fun start() {
        val env = Environment()

        val authHelper = AuthHelper(baseUrl = env.STS_BASE_URL, username = env.username!!, password = env.password!!)
        val aktørregisterClient = AktørregisterClient(baseUrl = env.AKTØRREGISTER_BASE_URL, authHelper = authHelper)

        AktørIdStream(aktørregisterClient).start(env)
    }

}
