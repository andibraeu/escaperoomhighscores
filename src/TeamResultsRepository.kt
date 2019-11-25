import org.jetbrains.exposed.sql.Table

object TeamResultsObject : Table() {
    val teamName = varchar("teamName", 255).primaryKey()
    val passedFirstLevel = bool("passedFirstLevel")
    val durationInMinutesFirstLevel = integer("durationInMinutesFirstLevel")
    val passedSecondLevel = bool("passedSecondLevel")
    val durationInMinutesSecondLevel = integer("durationInMinutesSecondLevel")
}