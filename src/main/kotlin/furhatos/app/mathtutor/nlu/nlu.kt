import furhatos.nlu.EnumEntity
import furhatos.nlu.Intent
import furhatos.nlu.common.Number
import furhatos.nlu.common.PersonName
import furhatos.util.Language

class FurtherDetails(var userName: PersonName? = null, var userAge: Age? = null) :
    Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
            "My name is @userName", "I am @userName", "It's @userName", "@userName", "I'm @userName",
            "I'm @userAge", "I'm @userAge years old", "It's @userAge", "@userAge", "@userAge years old"
        )
    }
}

class Scores(
    var multiScores: ArrayList<Int>? = ArrayList(),
    var divScores: ArrayList<Int>? = ArrayList(),
    var percScores: ArrayList<Int>? = ArrayList()) {
}

class Age : Number() {}

class TutorialSelection(var tutorialType: TutorialType? = null) : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
            "Can I take the @tutorialType tutorial?", "I want to do the @tutorialType tutorial",
            "How about @tutorialType", "@tutorialType"
        )
    }
}

class Quit() : Intent(){
    override fun getExamples(lang: Language): List<String> {
        return listOf(
            "I want to quit", "I don't want to study anymore", "I don't want to do anything", "Can we take a break?",
            "Let's take a break", "I prefer to stop studying", "I prefer to quit", "I don't want to study", "Quit",
            "I prefer to exist the tutorial", "Can we stop the tutorial", "I want to break."
        )
    }
}

class Tutorial() : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
            "tutorial", "I want to do the tutorial", "Let's do the tutorial", "The tutorial, for now",
            "How about the tutorial?", "Can I do the tutorial?"
        )
    }
}

class Exercise() : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf(
            "exercise", "I want to do the exercise", "Let's do some exercises", "The exercises first, I think",
            "How about some exercises?", "Can I do an exercise"
        )
    }
}

class TutorialType : EnumEntity(stemming = true, speechRecPhrases = true) {
    override fun getEnum(lang: Language): List<String> {
        return listOf("multiplication", "division", "percentage")
    }
}


