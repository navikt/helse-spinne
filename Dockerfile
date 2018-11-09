FROM navikt/java:10

ENV APP_BINARY=sykepengebehandling
COPY build/install/sykepengebehandling/ .
COPY src/main/resources/nav_truststore_nonproduction.jts .
