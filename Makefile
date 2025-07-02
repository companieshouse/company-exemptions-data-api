artifact_name       := company-exemptions-data-api
version             := "unversioned"

.PHONY: all
all: build

.PHONY: clean
clean:
	mvn clean
	rm -f $(artifact_name)-*.zip
	rm -f $(artifact_name).jar
	rm -rf ./build-*
	rm -f ./build.log

.PHONY: build
build:
	mvn org.codehaus.mojo:versions-maven-plugin:2.17.1:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar

.PHONY: test
test: test-integration test-unit

.PHONY: test-unit
test-unit:
	mvn test -Dskip.integration.tests=true

.PHONY: test-integration
test-integration:
	mvn integration-test verify -Dskip.unit.tests=true failsafe:verify

.PHONY: docker-image
docker-image: clean
	mvn package -Dskip.unit.tests=true -Dskip.integration.tests=true jib:dockerBuild

.PHONY: package
package:
ifndef version
	$(error No version given. Aborting)
endif
	$(info Packaging version: $(version))
	mvn org.codehaus.mojo:versions-maven-plugin:2.17.1:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	$(eval tmpdir:=$(shell mktemp -d build-XXXXXXXXXX))
	cp ./target/$(artifact_name)-$(version).jar $(tmpdir)/$(artifact_name).jar
	cd $(tmpdir); zip -r ../$(artifact_name)-$(version).zip *
	rm -rf $(tmpdir)

.PHONY: dist
dist: clean build package

.PHONY: sonar
sonar:
	mvn sonar:sonar

.PHONY: sonar-pr-analysis
sonar-pr-analysis:
	mvn sonar:sonar -P sonar-pr-analysis

