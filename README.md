Small Ktor test service to store results from an escape game.an

## Build

`./gradlew assemble`

## Run

find fat jar in `./buld/libs/highscores-1.0.0-all.jar`

run with `java -jar /path/to/fat-jar`

You can configure the database URI and driver via environment variables `DBURI` and `DBDRIVER`.
You'll find supported databases at https://github.com/JetBrains/Exposed
