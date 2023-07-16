import furhatos.flow.kotlin.*
import furhatos.flow.kotlin.voice.PollyNeuralVoice

val Idle: State = state {

    init {
        furhat.voice = PollyNeuralVoice.Joanna().also { it.style = PollyNeuralVoice.Style.Conversational }
        furhat.voice.volume = "soft"
        furhat.voice.rate = 0.80

        if (users.count > 0) {
            furhat.attend(users.random)
        }
    }

    onEntry {
        furhat.attendNobody()
        //goto(Start)
    }

    onUserEnter {
        furhat.attend(it)
        goto(Start)

    }
}

val Interaction: State = state {

    onUserLeave(instant = true) {
        if (users.count > 0) {
            if (it == users.current) {
                furhat.attend(users.other)
                //goto(Start)
            } else {
                furhat.glance(it)
            }
        } else {
            goto(Idle)
        }
    }

    onUserEnter(instant = true) {
        furhat.glance(it)
    }

//    //2.4
//    onResponse {
//        call(FallbackState)
//        reentry()
//    }
}

