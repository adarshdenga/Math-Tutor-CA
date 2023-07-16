import furhatos.flow.kotlin.NullSafeUserDataDelegate
import furhatos.flow.kotlin.UserDataDelegate
import furhatos.records.User

var User.tutorialType : String by NullSafeUserDataDelegate{"None"}
val User.details by NullSafeUserDataDelegate{FurtherDetails()}
var User.scores by NullSafeUserDataDelegate{Scores()}
