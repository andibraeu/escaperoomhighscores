data class TeamResults(
    val teamName: String,
    val passedFirstLevel: Boolean,
    val durationInMinutesFirstLevel: Int,
    val passedSecondLevel: Boolean,
    val durationInMinutesSecondLevel: Int
)