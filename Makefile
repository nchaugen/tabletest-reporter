.PHONY: build test format maven-build gradle-plugin-build compatibility-tests

build: maven-build gradle-plugin-build

test: build compatibility-tests

maven-build:
	mvn clean install

gradle-plugin-build:
	cd tabletest-reporter-gradle-plugin && ./gradlew clean build publishToMavenLocal

compatibility-tests:
	./compatibility-tests/run-tests.sh

format:
	mvn spotless:apply -q
	@if [ -f "tabletest-reporter-gradle-plugin/gradlew" ]; then \
		cd tabletest-reporter-gradle-plugin && ./gradlew spotlessApply -q 2>/dev/null || echo "Note: Gradle formatting skipped (build issue)"; \
	else \
		echo "Note: Gradle formatting skipped (gradlew missing)"; \
	fi
