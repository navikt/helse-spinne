FROM navikt/java:10

ENV APP_BINARY=sykepengebehandling
COPY build/install/sykepengebehandling/ .
