Small Ktor test service to store results from an escape game.an

## Build

`./gradlew assemble`

## Run

find fat jar in `./buld/libs/highscores-1.0.0-all.jar`

run with `java -jar /path/to/fat-jar`

You can configure the database URI and driver via environment variables `DBURI` and `DBDRIVER`.
You'll find supported databases at https://github.com/JetBrains/Exposed

## Operations

### systemd

You can use the service file in misc to run this application via systemd on a server.

### docker

build docker image using `docker build  -t escaperoomhighscores .`

run local in foreground: `docker run -it -p 8090:8090 --rm escaperoomhighscores:latest`