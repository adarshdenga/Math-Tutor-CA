import furhatos.app.spacereceptionist.emotion.frustrationEmotions
import furhatos.app.spacereceptionist.emotion.getEmotionFromApi
import furhatos.flow.kotlin.*
import furhatos.gestures.Gestures
import furhatos.nlu.common.DontKnow
import furhatos.nlu.common.Number
import furhatos.records.Location
import kotlin.random.Random

fun FlowControlRunner.interruptedGaze() {
    // look away during hesitant speech
    val rand: Double = Random.nextDouble(0.0, 1.0)
    furhat.gesture(Gestures.BrowFrown)
    if (rand < 0.35) {
        furhat.glance(users.current)
    } else {
        furhat.glance(Location(0.0, 0.0, 0.5), 1000)
    }
}

fun FlowControlRunner.askingGaze() {
    furhat.glance(users.current)
}

fun FlowControlRunner.stopSpeakingGaze() {
    val rand: Double = Random.nextDouble(0.0, 1.0)
    if (rand < 0.85) {
        // stop speaking
        // look at user with high probability
        furhat.glance(users.current, 2000)
        furhat.gesture(Gestures.Thoughtful)
    } else {
        furhat.glance(Location(0.5, 0.0, 0.0), 1000)
    }
}

fun FlowControlRunner.startSpeakingGaze() {
    val rand: Double = Random.nextDouble(0.0, 1.0)
    if (rand < 0.75) {
        // glance away when starting to speak with 75% probability
        furhat.glance(Location(0.5, 0.5, 0.0), 1000)
    } else {
        furhat.glance(users.current, 500)
    }
}

fun FlowControlRunner.whileSpeakingGaze() {
    val rand: Double = Random.nextDouble(0.0, 1.0)
    // glance at and away for equal time, more likely ot look away
    if (rand < 0.75) {
        furhat.glance(Location(0.3, 0.3, 0.0), 2000)
    } else {
        furhat.glance(users.current, 2000)
    }
}

fun FlowControlRunner.whileListeningGaze() {
    val rand: Double = Random.nextDouble(0.0, 1.0)
    // glance at longer, glance at with slightly higher probability
    if (rand < 0.65) {
        furhat.glance(users.current, 3000)
    } else {
        furhat.glance(Location(0.0, 0.0, 0.5), 1000)

    }
}

fun FlowControlRunner.interruptedTutorial() {
    interruptedGaze()
    val exercise = furhat.askYN("Ah, you seem eager, shall we move on to exercises already?")
    if (exercise!!) {
        goto(QuizExercise)
    } else {
        furhat.say("Okay, let's go back to our example!")
        reentry()
    }
}

fun FlowControlRunner.quitRepsonse() {
    // emotion detection
    val emotionResponse = getEmotionFromApi()
    println(emotionResponse)
    if (frustrationEmotions.contains(emotionResponse["Emotion"]) || emotionResponse["FrustrationLevel"].toString()
            .toFloat() > 0.2
    ) {
        furhat.say("I understand you are frustrated.")
    }
    goto(TakeABreak)
}

fun FlowControlRunner.correctAnswerReaction() {
    random(
        { furhat.gesture(Gestures.Smile) },
        { furhat.gesture(Gestures.BigSmile) },
        { furhat.say(furhat.voice.emphasis("Yes!")) },
        { furhat.say("Well done, ${users.current.details.userName}!") }
    )

    val emotionResponse = getEmotionFromApi();
    if (emotionResponse["Emotion"] == "Happy") {
        // mirroring
        furhat.gesture(furhatos.gestures.Gestures.BigSmile())
    }
}

fun FlowControlRunner.quiz(
    answerList: ArrayList<Int>,
    correctAnswerList: ArrayList<Int>,
    scoreList: ArrayList<Int>,
    type: String
) {

    if (answerList.size < 8) {
        quizQuestion(type, correctAnswerList);
        println(answerList)
        println(correctAnswerList)
    } else {
        println(answerList)
        println(correctAnswerList)
        var score: Int = 0
        for (i in answerList.indices) {
            if (answerList[i] == correctAnswerList[i]) {
                score++
            }
        }

        furhat.say("You scored: $score")
        when (score) {
            in 5..8 -> furhat.say("${furhat.voice.emphasis("Great")} job!")
            in 3..5 -> furhat.say("Pretty good!")
            in 0..3 -> furhat.say("Don't worry!")
        }

        //  emotion detection
        val emotionResponse = getEmotionFromApi();
        println(emotionResponse);
        if (frustrationEmotions.contains(emotionResponse["Emotion"]) || emotionResponse["FrustrationLevel"].toString()
                .toFloat() > 0.4
        ) {
            if (emotionResponse["FrustrationLevel"].toString().toFloat() > 0.4) {
                val takeabreak =
                    furhat.askYN("You look frustrated. I understand this is hard. Do you want to take a break?")
                if (takeabreak == true) {
                    goto(TakeABreak)
                }
            } else {
                random(
                    { furhat.say("Don't give up.") },
                    { furhat.say("I will help you.") }
                )
            }
        }

        if (emotionResponse["Emotion"] == "Happy") {
            // mirroring
            furhat.gesture(furhatos.gestures.Gestures.BigSmile())
        }

        improvement(score, scoreList)
        scoreList.add(score)
        println("scoreList")
        println(scoreList)

        furhat.say("Okay, good work today, thanks for working with me! Bye!")
        furhat.gesture(Gestures.Smile)
        goto(Idle)
    }
}

fun FlowControlRunner.tutorialResponse(
    stateBuilder: StateBuilder,
    question: String,
    explanation: String,
    correctAnswer: Int,
    state: State
) {
    val answer = furhat.askFor<Number>(question) {
        onResponse<DontKnow> {
            send("StartTalkingEvent")
            random(
                { furhat.say("I understand.") },
                { furhat.say("It's okay!") },
                { furhat.say("No worries!") }
            )

            // emotion detection
            val emotionResponse = getEmotionFromApi();
            println(emotionResponse);
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
                        { furhat.say("Don't get too discouraged though, we are just starting out, it's normal to make mistakes.") },
                        { furhat.say("Don't give up.") },
                        { furhat.say("Don't worry. I will help you.") }
                    )
                }
            } else if (emotionResponse["Emotion"] == "Neutral") {
                random(
                    { furhat.say("You did good.") },
                    { furhat.say("Good progress!") }
                )
            }

            if (emotionResponse["Emotion"] == "Happy") {
                // mirroring
                onTime(0, 1000) {
                    furhat.gesture(Gestures.BigSmile())
                }
            }

            furhat.say("Remember $explanation, how about another example?")

            goto(state)
        }

        onResponse<Quit> {
            goto(ChangeTopic)
        }
    }

    when (answer!!.value) {
        correctAnswer -> {
            correctAnswerReaction()
            send("StartTalkingEvent")

            random(
                { furhat.say(furhat.voice.emphasis("Correct!")) },
                { furhat.say("That's it!") },
                { furhat.say(furhat.voice.emphasis("Brilliant!")) }
            )

            furhat.say(explanation)

            val exercise = furhat.askYN("Are you ready for some exercises?")

            if (exercise!!) {
                goto(QuizExercise)
            } else {
                goto(state)
            }
        }
        else -> {
            send("StartTalkingEvent")
            random(
                { furhat.say("Sorry, that's incorrect!") },
                { furhat.say("No, sorry!") },
                { furhat.say("Not quite!") }
            )

            furhat.say("Remember, $explanation")

            // emotion detection
            val emotionResponse = getEmotionFromApi();
            println(emotionResponse);
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
                        { furhat.say("Don't give up.") },
                        { furhat.say("Don't worry. I will help you.") }
                    )
                }
            } else if (emotionResponse["Emotion"] == "Neutral") {
                furhat.say("You did good.")
            }

            if (emotionResponse["Emotion"] == "Happy") {
                // mirroring
                stateBuilder.onTime(0, 1000) {
                    furhat.gesture(Gestures.BigSmile())
                }
            }

            val example = furhat.askYN("Should we try another example?")
            if (example!!) {
                goto(state)
            } else {
                goto(QuizExercise)
            }
        }
    }
}

private fun FlowControlRunner.quizQuestion(type: String, correctAnswerList: ArrayList<Int>): Int {
    val a: Int = Random.nextInt(from = 0, until = 10)
    val b: Int = Random.nextInt(from = 1, until = 10)
    var correctAnswer: Int = 0
    when (type) {
        "multiplication" -> {
            correctAnswer = a * b
            correctAnswerList.add(correctAnswer)
            furhat.ask("What is: $a times $b")
        }
        "division" -> {
            correctAnswer = (a * b) / b
            correctAnswerList.add(correctAnswer)
            furhat.ask("What is: ${a * b} divided by $b")
        }
        "percentage" -> {
            correctAnswer = ((a * b) / b) * 100
            correctAnswerList.add(correctAnswer)
            if (a > b) {
                furhat.ask("What percent is $a of ${a * b})")
            } else {
                furhat.ask("What percent is $b of ${a * b} ")
            }

        }
    }
    return correctAnswer
}

private fun FlowControlRunner.improvement(score: Int, scoreList: ArrayList<Int>) {
    scoreList.add(3);

    if (scoreList.size == 0) {
        furhat.say("Good job for the first try!")
    } else {
        if (score > scoreList[scoreList.size - 1]) {
            furhat.say("${users.current.details.userName}, you're ${furhat.voice.emphasis("improving!")}")
        } else {
            furhat.say("It's alright you will do better next time")
        }
    }
}
