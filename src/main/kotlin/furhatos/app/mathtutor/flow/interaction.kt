import furhatos.app.spacereceptionist.emotion.frustrationEmotions
import furhatos.app.spacereceptionist.emotion.getEmotionFromApi
import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures
import furhatos.nlu.common.DontKnow
import furhatos.nlu.common.No
import furhatos.nlu.common.Number
import furhatos.nlu.common.Yes
import furhatos.records.Location
import kotlin.math.roundToInt
import kotlin.random.Random

fun gaussianInt(mean: Double, stdev: Double): Double {
    var random: Double = java.util.Random().nextGaussian()
    random = random * stdev + mean
    return random
}

val gazeHandler: State = state(Interaction) {
    val delta = 10

    var lookTime = 0
    var atUser = false

    var lookAtMean = 3.35
    var lookAwayMean = 3.46
    val stdevPercentage = 0.3

    onEntry {
        // When furhat starts attending the user look at them
        furhat.glance(users.current)
    }

    onTime(repeat = delta) {
        lookTime -= delta
        if (lookTime <= 0) {
            if (atUser) {
                lookTime = (gaussianInt(lookAwayMean, stdevPercentage * lookAwayMean) * 1000).roundToInt()
                if (lookTime > 130) {
                    println("Looking away")
                    furhat.glance(Location(0.5, 0.5, 0.0), lookTime)
                }

                println("Looking for $lookTime milliseconds")
            } else {
                lookTime = (gaussianInt(lookAtMean, stdevPercentage * lookAtMean) * 1000).roundToInt()
                if (lookTime > 130) {
                    println("Looking at user")
                    furhat.glance(users.current)
                }
                println("Looking for $lookTime milliseconds")
            }
            atUser = !atUser
        }
    }

    onEvent("StartTalkingEvent") {
        lookAtMean = 2.68
        lookAwayMean = 2.85

        val rand: Double = Random.nextDouble(0.0, 1.0)
        if (!atUser && rand < 0.6) {
            lookTime = 0
        }
    }

    onEvent("StartListeningEvent") {
        lookAtMean = 7.93
        lookAwayMean = 2.61

        val rand: Double = Random.nextDouble(0.0, 1.0)
        if (!atUser && rand < 0.75) {
            lookTime = 0
        }
    }

    onEvent("InterruptedEvent") {
        furhat.glance(Location.RIGHT)
        lookTime = 200
        atUser = false
    }
}

val Start: State = state(Interaction) {

    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        furhat.gesture(Gestures.Smile)
        send("StartTalkingEvent")
        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        random(
            { furhat.ask("Hello, I am Pi the Math Tutor! Do you want to learn new math skills?") },
            { furhat.ask("Hello, ${furhat.voice.emphasis("welcome!")} I am Pi the Math Tutor. Are you ready to learn some math skills?") }
        )
        send("StartListeningEvent")
    }

    onReentry {
        send("StartTalkingEvent")
        furhat.ask("Would you like to learn new math skills?")
        send("StartListeningEvent")
    }

    onResponse(cond = { it.interrupted }) {
        send("InterruptedEvent")
        furhat.ask("Sorry, I missed that, can you repeat?")
        send("StartListeningEvent")
    }

    onResponse<Yes> {
        goto(CheckFurtherDetails)
    }

    onResponse<No> {
        goto(TakeABreak)
    }

}

val CheckFurtherDetails: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        val details = users.current.details
        // Makes sure the intro line get said only once
        if (details.userName == null && details.userAge == null) {
            send("StartTalkingEvent")
            furhat.say("let's get started, then!")
        }
        when {
            details.userName == null -> goto(FurtherDetailsName)
            details.userAge == null -> goto(FurtherDetailsAge)
            else -> goto(TutorialChoice)
        }
    }
}

val FurtherDetailsName: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        furhat.ask("So, what's your name?")
        send("StartListeningEvent")
    }

    onReentry {
        send("StartTalkingEvent")
        random(
            { furhat.ask("Sorry, what was your name again?") },
            { furhat.ask("Could you repeat that?") }
        )
        send("StartListeningEvent")
    }

    onResponse<FurtherDetails> {
        users.current.details.userName = it.intent.userName
        goto(CheckFurtherDetails)
    }
}

val FurtherDetailsAge: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        furhat.ask("And your age?")
        send("StartListeningEvent")
    }

    onReentry {
        send("StartTalkingEvent")
        random(
            { furhat.ask("Sorry, I didn't get that, how old are you?") },
            { furhat.ask("Could you repeat that?") }
        )
        send("StartListeningEvent")
    }

    onResponse<FurtherDetails> {
        users.current.details.userAge = it.intent.userAge
        goto(CheckFurtherDetails)
    }
}

val TutorialChoice: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
//        // emotion detection
        val emotionResponse = getEmotionFromApi()
        println(emotionResponse)
        send("StartTalkingEvent")
        if (frustrationEmotions.contains(emotionResponse["Emotion"]) || emotionResponse["FrustrationLevel"].toString()
                .toFloat() > 0.3
        ) {
            furhat.say("You seem stressed, don't worry, this lesson will be fun.")
        }

        if (emotionResponse["Emotion"] == "Happy") {
            // mirroring
            onTime(0, 1000) {
                furhat.gesture(Gestures.BigSmile())
            }
        }

        furhat.ask("Okay, do you want to learn multiplication, division, or percentage today?")
        send("StartListeningEvent")
    }

    onReentry {
        send("StartTalkingEvent")
        furhat.ask("Do you want to learn multiplication, division, or percentage today?")
        send("StartListeningEvent")
    }

    onResponse<TutorialSelection> {
        users.current.tutorialType = it.intent.tutorialType!!.toText().toString()
        goto(ExerciseChoice)
    }

    onResponse<Quit> {
        quitRepsonse()
    }
}

val ExerciseChoice: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        send("StartTalkingEvent")
        furhat.ask("Should we start with some tutorials or exercises")
        send("StartListeningEvent")
    }

    onResponse<Tutorial> {
        when (users.current.tutorialType) {
            "multiplication" -> goto(MultiTutorial)
            "division" -> goto(DivTutorial)
            "percentage" -> goto(PercTutorial)
            else -> {
                send("StartTalkingEvent")
                furhat.say("Sorry, let's try that again!")
                goto(TutorialChoice)
            }
        }
    }

    onResponse<Exercise> {
        when (users.current.tutorialType) {
            "multiplication" -> goto(QuizExercise)
            "division" -> goto(QuizExercise)
            "percentage" -> goto(QuizExercise)
            else -> {
                send("StartTalkingEvent")
                furhat.say("Sorry, let's try that again!")
                goto(TutorialChoice)
            }
        }
    }
}

val MultiTutorial: State = state(Interaction) {
    val explanation: String = "We want 1 times 4 which means we want 1 + 1 + 1 + 1 and we know that is 4! Actually, " +
            "any number multiplied by 1 is itself! Meanwhile any number multiplied by 0 is 0!"
    val correctAnswer: Int = 4
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        furhat.gesture(Gestures.Thoughtful)
        furhat.say(interruptable = true) {
            +"Multiplication is a close friend of addition! We know 2 + 2 is 4, and 2 + 2 + 2 is 6. If we say 2 times 2 is 4, and 2 times 3 is 6, then you can spot a pattern; multiplication is repeated addition!"
        }

        send("StartTalkingEvent")
        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state,
            "Let's do an example, what is, hmm, ${furhat.voice.pause("500ms")} 1 times 4?",
            explanation, correctAnswer, MultiTutorialAlt
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel {
            goto(gazeHandler)
        }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What is 1 times 4?", explanation, correctAnswer, MultiTutorialAlt)
    }
}

val DivTutorial: State = state(Interaction) {
    val explanation: String =
        "We want 4 by 1 which means we want 1 + 1 + 1 + 1 which makes for 4 groups! In fact, any number divided by " +
                "1 is itself. Meanwhile we cannot divide by 0 because there is no way to combine 0s to make " +
                "in this case, a 4."
    val correctAnswer: Int = 4

    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        furhat.gesture(Gestures.Thoughtful)
        send("StartTalkingEvent")

        furhat.say(interruptable = true) {
            +"We can think of division as making equal groups. If we want to divide 10 by 5, how many groups of 5 fit in 10? We know 5 + 5 is 10 , so that means we need 2 groups of 5 to make 10! So 10 divided by 5 is 2."
        }
        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state,
            "For example, uummm, ${furhat.voice.pause("500ms")} do you know what 4 divided by 1 is?",
            explanation, correctAnswer, DivTutorialAlt
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(
            this@state, "Do you know what 4 divided by 1 is?",
            explanation, correctAnswer, DivTutorialAlt
        )
    }
}

val PercTutorial: State = state(Interaction) {
    val explanation: String = " We first divide 8 by 10 we get 0.8, multiplied by 100, that gives us 80% blue pens."
    val correctAnswer = 80
    onEntry {
        // Run gaze handling in the background
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        furhat.say(interruptable = true) {
            +"Percentages refer to a part of a group of 100. For example, I have 100 pens, 80 of those pens are blue and 20 are red. Then we can say 20% of my pens are red. If I have 10 pens, and 2 are red, I still have 20% red pens because I can relate the ratio of 2 to 10 to a ratio out 100 by dividing 2 by 10 and multiplying by 100."
        }
        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state,
            "If in that group of 10 I have 8 blue pens, what percent of my pens are blue?",
            explanation, correctAnswer, PercTutorialAlt
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What percent is 8 of 10?", explanation, correctAnswer, PercTutorialAlt)
    }
}

val MultiTutorialAlt: State = state(Interaction) {
    val correctAnswer: Int = 6
    val explanation: String = "We want 3 times 2 which means we want 3 + 3 or  2 + 2 + 2 either way that is 6! " +
            "In the furture, it can help to visualise our problem, you can draw out 2 groups of 3 stars, or 3 " +
            "groups of 2 stars, and count the total stars to get the correct answer."

    onEntry {
        // Run gaze handling in the background
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        furhat.say(interruptable = true) {
            +"Alright, let's try this. Multiplication is commutative. That means 10 times 3, where 10 is added 3 times, is the same as 3 times 10, where 3 is added 10 times. In both cases this is 30."
        }

        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state,
            "Hhmmm, ${furhat.voice.pause("500ms")} Since we already know what 2 time 3 is, what is 3 times 2?",
            explanation,
            correctAnswer, MultiTutorialAlt2
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What is 3 times 2?", explanation, correctAnswer, MultiTutorialAlt2)
    }
}

val DivTutorialAlt: State = state(Interaction) {
    val correctAnswer: Int = 2
    val explanation: String =
        "We want 10 by 2 which means we want 5 + 5 which makes for 2 groups! Meanwhile it can help " +
                "to remember that when A divided by B is equal to C, B multiplied by C will be equal to A!"
    onEntry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        furhat.say(interruptable = true) {
            +"Alright, different approach, Let's look at a real world example. I have 4 friends and 8 slices of cake to distribute. If I want everyone to have an equal share, I can divide 8 by 4 and get 2 slices of cake for each friend."
        }

        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state,
            "If you want to split, uummm, ${furhat.voice.pause("500ms")} 10 pieces of cake into equal " +
                    "parts between 2 friends how many pieces does each friend get? ",
            explanation,
            correctAnswer,
            DivTutorialAlt2
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What is 10 divided by 2?", explanation, correctAnswer, DivTutorialAlt2)
    }
}

val PercTutorialAlt: State = state(Interaction) {
    val correctAnswer: Int = 50
    val explanation: String = "Let's first look at our fraction, if 2 out of 4 questions are done, then two fourths " +
            "of our homework is done. This is the same as one half our homework, which we know to be 50%. We can " +
            "also divide 2 by 4 to get 0.5 then multiply by 100, and again get 50%."

    onEntry {
        // Run gaze handling in the background
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        furhat.say(interruptable = true) {
            +"Alright. Let's look at some common fractions and their respective percentages. If one half of my homework is done then 50% of my homework is done. If one quarter of my homework is done, that is 25%, for one third it is 33%. Other fractions with the same ratio, will have the same associated percentage!"
        }

        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state, "If you have 4 homework questions and have completed 2, what percent of your " +
                    "homework is left?", explanation, correctAnswer, PercTutorialAlt2
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What percent is 2 of 4?", explanation, correctAnswer, PercTutorialAlt2)
    }
}

val MultiTutorialAlt2: State = state(Interaction) {
    val explanation: String = "You need 2 + 2 + 2 + 2 + 2 pieces of cake and that means 10 pieces total."
    val correctAnswer: Int = 10
    onEntry {
        // Run gaze handling in the background
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        furhat.say(interruptable = true) {
            +"Alright, different approach. Think of a real world example. If I have 3 friends and they each want 3 slices of cake, I need a total of 9 slices to feed everyone. ${users.current.details.userName}, say you have, hhmmm, ${
                furhat.voice.pause(
                    "500ms"
                )
            } 5 friends, each of them asks for 2 slices of cake."
        }

        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state,
            "How many pieces of cake do you need to feed all your friends?",
            explanation,
            correctAnswer,
            ChangeTopic
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What is 5 times 2?", explanation, correctAnswer, ChangeTopic)
    }
}

val DivTutorialAlt2: State = state(Interaction) {
    val explanation: String = "We want 3 divided by 3, so we subtract 3 from 3 until we reach 0, which we " +
            "only need to do 1. So 3 by 3 is 1. Actually, any number divided by itself is 1"
    val correctAnswer: Int = 1

    onEntry {
        // Run gaze handling in the background
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        furhat.say(interruptable = true) {
            +"Let's try this. Let's think of division as repeated subtraction for a moment. For example, how many times do I need subtract 2 from 6 to get to zero? Well, 6 - 2 is 4 then 4 - 2 is 2 and 2 - 2 is 0 so we perform subtraction 3 times. So we know that 6 divided by 2 is 3."
        }

        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state, "Now ${users.current.details.userName}, it is your turn, " +
                    "can you tell me what 3 divided by 3 is?", explanation, correctAnswer, ChangeTopic
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What is 3 divided by 3?", explanation, correctAnswer, ChangeTopic)
    }
}

val PercTutorialAlt2: State = state(Interaction) {
    val explanation: String = "We have one quarter of our whole 100% then we have 1 by 4 multiplied by 100, or " +
            "simpler we have 100 by 4 to get 25% either way!"
    val correctAnswer: Int = 25

    onEntry {
        parallel {
            goto(gazeHandler)
        }
        send("StartTalkingEvent")
        furhat.say(interruptable = true) {
            +"Okay, let's draw a circle, then let us draw two intersecting lines in our circle. Now we've made 4 quarters, if our whole circle is 100%"
        }

        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
        tutorialResponse(
            this@state,
            "What percent does one of our quarters represent?",
            explanation,
            correctAnswer,
            ChangeTopic
        )
    }

    onResponse(cond = { it.interrupted }) {
        interruptedTutorial()
    }

    onReentry {
        parallel { goto(gazeHandler) }
        send("StartTalkingEvent")
        tutorialResponse(this@state, "What percent is 1 of 4?", explanation, correctAnswer, ChangeTopic)
    }
}

val QuizExercise: State = state(Interaction) {
    val answerList: ArrayList<Int> = ArrayList()
    val correctAnswerList: ArrayList<Int> = ArrayList()

    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        send("StartTalkingEvent")
        furhat.say(
            "Okay, we are going to do 8 little questions and then score our answers, " +
                    "if you don't know the answer, you can just say so, it's no problem!"
        )
        quiz(answerList, correctAnswerList, users.current.scores.multiScores!!, users.current.tutorialType)
        dialogLogger.startSession("test", 15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
    }

    onReentry {
        quiz(answerList, correctAnswerList, users.current.scores.multiScores!!, users.current.tutorialType)
    }

    onResponse<Number> {
        answerList.add(it.intent.value!!.toInt())
        reentry()
    }

    onResponse<DontKnow> {
        send("StartTalkingEvent")
        // emotion detection
        val emotionResponse = getEmotionFromApi()
        println(emotionResponse)
        if (frustrationEmotions.contains(emotionResponse["Emotion"]) || emotionResponse["FrustrationLevel"].toString()
                .toFloat() > 0.2
        ) {
            if (frustrationEmotions.contains(emotionResponse["Emotion"]) && emotionResponse["FrustrationLevel"].toString()
                    .toFloat() > 0.4
            ) {
                val takeabreak =
                    furhat.askYN("You look frustrated. I understand this is hard. Do you want to take a break?")
                if (takeabreak == true) {
                    goto(TakeABreak)
                }
            } else {
                random(
                    { furhat.say("I know you are trying your best!") },
                    { furhat.say("Don't give up.") },
                    { furhat.say("Don't worry. I will help you.") }
                )
            }
        } else if (emotionResponse["Emotion"] == "Neutral") {
            furhat.say("You did good.")
        }

        if (emotionResponse["Emotion"] == "Happy") {
            // mirroring
            onTime(0, 1000) {
                furhat.gesture(Gestures.BigSmile())
            }
        }

        furhat.say("Don't worry! Let's keep moving.")
        answerList.add(-1)
        reentry()
    }

    onResponse<Quit> {
        quitRepsonse()
    }

    onResponse {
        println("Caught response not matching any of my intents")
        answerList.add(-1)
        reentry()
    }
}

val ChangeTopic: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        furhat.gesture(Gestures.Smile)
        send("StartTalkingEvent")
        val topic = furhat.askYN("Why don't we try working on a different topic for a change?")
        if (topic!!) {
            goto(FeelBetterTopic)
        } else {
            goto(TakeABreak)
        }
        // dialogLogger.startSession("test",15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
    }

    onResponse<Quit> {
        quitRepsonse()
    }
}


val FeelBetterTopic: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        val goodTopic = if (users.current.scores.multiScores!!.average() > users.current.scores.divScores!!.average()) {
            if (users.current.scores.multiScores!!.average() > users.current.scores.percScores!!.average()) {
                "multiplication"
            } else {
                "percentage"
            }
        } else {
            if (users.current.scores.divScores!!.average() > users.current.scores.percScores!!.average()) {
                "division"
            } else {
                "percentage"
            }
        }

        send("StartTalkingEvent")
        val yes =
            furhat.askYN("You know, you are quite good at ${goodTopic}, shall we do some exercises in that instead?")
        if (yes!!) {
            users.current.tutorialType = goodTopic
            goto(QuizExercise)
        } else {
            // furhat.gesture(Gestures.ExpressSad)
            furhat.say("No?")
            goto(TakeABreak)
        }
        // dialogLogger.startSession("test",15, "ee90b1ef-7a3c-4acf-82a5-a77c668a08a4")
    }

    onResponse<Quit> {
        quitRepsonse()
    }
}


val TakeABreak: State = state(Interaction) {
    onEntry {
        // Run gaze handling in the background
        parallel {
            goto(gazeHandler)
        }
        send("StartTalkingEvent")
        furhat.say("That's okay! It's good to take a break every once in a while! Come back any time!")
        furhat.gesture(Gestures.BigSmile)
        goto(Idle)
    }
}
