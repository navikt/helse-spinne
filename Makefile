DOCKER   := docker
GRADLE   := ./gradlew -Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000
VERSION  := $(shell git rev-parse --short HEAD)
IMG_NAME := navikt/sykepengebehandling

.PHONY: all build test docker docker-push release

all: build test docker
release: docker-push
	git clone https://github.com/navikt/helse-iac.git

	cd helse-iac; \
	./set-image.sh preprod/sykepengebehandling/naiserator.yaml $(IMG_NAME):$(VERSION); \
	./set-image.sh prod/sykepengebehandling/naiserator.yaml $(IMG_NAME):$(VERSION); \
	git config user.name team-helse[bot]; \
	git config user.email team-helse[bot]@users.noreply.github.com; \
	git add preprod/sykepengebehandling/naiserator.yaml; \
	git add prod/sykepengebehandling/naiserator.yaml; \
	git commit -m "Bump version" -m "Caused by $(TRAVIS_BUILD_WEB_URL)" --author "$(shell git log -1 --pretty=format:'%an <%ae>')"; \
	git push https://x-access-token:$(INSTALLATION_TOKEN)@github.com/navikt/helse-iac.git master;

build:
	$(GRADLE) installDist

test:
	$(GRADLE) check

docker:
	$(DOCKER) build --pull -t $(IMG_NAME) -t $(IMG_NAME):$(VERSION) .

docker-push:
	$(DOCKER) push $(IMG_NAME):$(VERSION)

