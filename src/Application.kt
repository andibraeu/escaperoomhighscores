import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.flattenForEach
import kotlinx.css.CSSBuilder
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.body
import kotlinx.css.color
import kotlinx.css.em
import kotlinx.css.fontSize
import kotlinx.css.p
import kotlinx.html.ButtonType
import kotlinx.html.FormEncType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.link
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val databaseUri = environment.config.property("database.uri").getString()
    val databaseDriver = environment.config.property("database.driver").getString()
    println(databaseUri)
    val teamResultController = TeamResultController()
    Database
        .connect(databaseUri,databaseDriver)
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
                        p { +"This is just a simple high score tracker for an escaoe game. You may belong to the happy ones that got it passed on." }
                        p { +"Be excellent to each other, do not overwrite results from other teams." }
                    }
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
            val parameters = call.receiveParameters()
            var teamName = ""
            var passedFirstLevel = false
            var durationInMinutesFirstLevel = 0
            var passedSecondLevel = false
            var durationInMinutesSecondLevel = 0
            parameters.flattenForEach { key, value ->
                when (key) {
                    "teamName" -> teamName = value
                    "passedFirstLevel" -> passedFirstLevel = handleCheckbox(value)
                    "durationInMinutesFirstLevel" -> durationInMinutesFirstLevel = value.toInt()
                    "passedSecondLevel" -> passedSecondLevel = handleCheckbox(value)
                    "durationInMinutesSecondLevel" -> durationInMinutesSecondLevel = value.toInt()
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
                    backgroundColor = Color.lightGray
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

fun handleCheckbox(value: String): Boolean {
    return value == "on"
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
