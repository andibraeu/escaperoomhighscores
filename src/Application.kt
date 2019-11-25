import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.locations.*
import io.ktor.features.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.jackson.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val teamResultController = TeamResultController()
    Database
        .connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1","org.h2.Driver")
    transaction { createMissingTablesAndColumns(TeamResultsObject) }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        get("/") {
            call.respondHtml {
                head {
                    link {
                        href = "/styles.css"
                        rel = "stylesheet"
                        type = "text/css"
                    }
                }
                body {
                    h1 { +"Escape Room High Scores" }
                    div {
                        table {
                            thead {
                                tr {
                                    th { +"team name" }
                                    th { +"level 1" }
                                    th { +"duration" }
                                    th { +"level 2" }
                                    th { +"duration" }
                                }
                            }
                            tbody {
                                transaction {
                                    TeamResultsObject.selectAll().map { result ->
                                        tr {
                                            td { +result[TeamResultsObject.teamName] }
                                            td {
                                                checkBoxInput() {
                                                    checked = result[TeamResultsObject.passedFirstLevel]
                                                    disabled = true
                                                }
                                            }
                                            td { +result[TeamResultsObject.durationInMinutesFirstLevel].toString() }
                                            td {
                                                checkBoxInput() {
                                                    checked = result[TeamResultsObject.passedSecondLevel]
                                                    disabled = true
                                                }
                                            }
                                            td { +result[TeamResultsObject.durationInMinutesSecondLevel].toString() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    div {
                        h2 { +"Add your team results" }
                        p {
                            form {
                                encType = FormEncType.multipartFormData
                                method = FormMethod.post
                                p {
                                    label { +"Team name: " }
                                    input {
                                        type = InputType.text
                                        id = "teamName"
                                        name = "teamName"
                                    }
                                }
                                p {
                                    label { +"Passed level 1:  " }
                                    input {
                                        type = InputType.checkBox
                                        id = "passedFirstLevel"
                                        name = "passedFirstLevel"
                                    }
                                }
                                p {
                                    label { +"Duration level 1:  " }
                                    input {
                                        type = InputType.number
                                        id = "durationInMinutesFirstLevel"
                                        name = "durationInMinutesFirstLevel"
                                        value = "0"
                                    }
                                }
                                p {
                                    label { +"Passed level 2:  " }
                                    input {
                                        type = InputType.checkBox
                                        id = "passedSecondLevel"
                                        name = "passedSecondLevel"
                                    }
                                }
                                p {
                                    label { +"Duration level 2:  " }
                                    input {
                                        type = InputType.number
                                        id = "durationInMinutesSecondLevel"
                                        name = "durationInMinutesSecondLevel"
                                        value = "0"
                                    }
                                }
                                button {
                                    type = ButtonType.submit
                                    +"Send"}
                            }
                        }
                    }
                }
            }
        }

        post("/") {
            val multiPart = call.receiveMultipart()
            var teamName = ""
            var passedFirstLevel = false
            var durationInMinutesFirstLevel = 0
            var passedSecondLevel = false
            var durationInMinutesSecondLevel = 0
            multiPart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "teamName")
                            teamName = part.value
                        if (part.name == "passedFirstLevel")
                            passedFirstLevel = part.value.toBoolean()
                        if (part.name == "durationInMinutesFirstLevel")
                            durationInMinutesFirstLevel = part.value.toInt()
                        if (part.name == "passedSecondLevel")
                            passedSecondLevel = part.value.toBoolean()
                        if (part.name == "durationInMinutesSecondLevel")
                            durationInMinutesSecondLevel = part.value.toInt()
                    }
                }
            }
            val teamResults = TeamResults(
                teamName = teamName,
                passedFirstLevel = passedFirstLevel,
                durationInMinutesFirstLevel = durationInMinutesFirstLevel,
                passedSecondLevel = passedSecondLevel,
                durationInMinutesSecondLevel = durationInMinutesSecondLevel
            )
            teamResultController.upsertTeamResults(teamResults)
            call.respondRedirect("/", true)
        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.gray
                }
                p {
                    fontSize = 1.em
                }
                rule("p.myclass") {
                    color = Color.blue
                }
            }
        }

        post("/teamResults") {
            val teamResults = call.receive<TeamResults>()
            teamResultController.upsertTeamResults(teamResults)
            call.respond(HttpStatusCode.Created, teamResults)
        }

        get("/teamResults") {
            call.respond(teamResultController.getAllTeamResults())
        }

        install(StatusPages) {
             exception<MissingKotlinParameterException> { cause ->
                call.respond(HttpStatusCode.BadRequest, cause.localizedMessage)
            }

        }

    }
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
