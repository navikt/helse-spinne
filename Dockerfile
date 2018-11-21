FROM navikt/java:10

ENV APP_BINARY=spinne
COPY build/install/spinne/ .
