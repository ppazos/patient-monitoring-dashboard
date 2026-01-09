# patient-monitoring-dashboard

Dashboard for Atomik Server (atomik.app) to show vital signs readings from patients.

## Structure

This repo contains:

1. load-atomik-app: simple Groovy program that generates vital signs data and stores that in Atomik, to have some data to play with.
2. dashboard-app: Grails 5 app, a thin client for Atomik that gets vital signs data and displays it in a nice web dashboard.
3. ideas: contains some ideas and tests related to the dashboard.

## Want to test it?

You need a running Atomik instance. Check the [Getting Atomik Guide](https://atomik.app/documentation/getting_atomik) for more information.

Then generate some data using the `load-atomik-app`. This app is built with Gradle, so you need Gradle to build it. The simplest way to install Gradle is via [SDKMAN!](https://sdkman.io/)

```sh
cd load-atomik-app
gradle build
gradle run
```

Then run the `dashboard-app`. You'll need Grails 5.3.6 installed. You can install it also via SDKMAN!

```sh
cd dashboard-app
./gradlew bootRun --console=plain
```

## Condiguration

In `load-atomik-app` the configuration of the connection to Atomik is in `app/src/main/resources/application.properties`. There you will need to update the URLs and API credentials.

```properties
api_auth=custom
base_url=http://localhost:8090/api/v1
api_auth_url=http://localhost:8090/api/v1/auth
api_username=your@email.com
api_password=yourSuperSecretPassword
```

In `dashboard-app` the connection to Atomik is configured in `grails-app/conf/application.yml`. The same fields should be updated, it's just in another format:

```yaml
atomik:
    api_auth: custom
    base_url: http://localhost:8090/api/v1
    api_auth_url: http://localhost:8090/api/v1/auth
    api_username: your@email.com
    api_password: yourSuperSecretPassword
```

# Feedback

If you have any questions, comments or suggestions, please contact us: info@cabolabs.com
