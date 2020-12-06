import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class TeamResultController {
    fun getAllTeamResults(): List<TeamResults> {
        val teamResultsList: ArrayList<TeamResults> = arrayListOf()
        transaction {
            TeamResultsObject.selectAll().map {
                teamResultsList.add(TeamResults(
                    teamName = it[TeamResultsObject.teamName],
                    durationInMinutesFirstLevel = it[TeamResultsObject.durationInMinutesFirstLevel],
                    passedFirstLevel = it[TeamResultsObject.passedFirstLevel],
                    durationInMinutesSecondLevel = it[TeamResultsObject.durationInMinutesSecondLevel],
                    passedSecondLevel = it[TeamResultsObject.passedSecondLevel]
                ))
            }
        }
        return teamResultsList
    }

    fun upsertTeamResults(teamResults: TeamResults) {
        transaction {
            if (TeamResultsObject.select { TeamResultsObject.teamName eq teamResults.teamName }.count() == 1) {
                TeamResultsObject.update({ TeamResultsObject.teamName eq teamResults.teamName }) {
                    it[passedFirstLevel] = teamResults.passedFirstLevel
                    it[durationInMinutesFirstLevel] = teamResults.durationInMinutesFirstLevel
                    it[passedSecondLevel] = teamResults.passedSecondLevel
                    it[durationInMinutesSecondLevel] = teamResults.durationInMinutesSecondLevel
                }
            } else {
                TeamResultsObject.insert {
                    it[teamName] = teamResults.teamName
                    it[passedFirstLevel] = teamResults.passedFirstLevel
                    it[durationInMinutesFirstLevel] = teamResults.durationInMinutesFirstLevel
                    it[passedSecondLevel] = teamResults.passedSecondLevel
                    it[durationInMinutesSecondLevel] = teamResults.durationInMinutesSecondLevel
                }
            }
        }
    }
}