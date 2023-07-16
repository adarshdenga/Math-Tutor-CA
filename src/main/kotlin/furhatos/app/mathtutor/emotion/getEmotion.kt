package furhatos.app.spacereceptionist.emotion

import Quit
import TakeABreak
import com.google.gson.Gson
import furhatos.flow.kotlin.*
import furhatos.nlu.common.Yes
import org.json.JSONObject
import java.net.ConnectException

val frustrationEmotions = listOf("Anger","Disgust","Fear","Sad")

//response: {
//    "Angry": "0.102",
//    "Disgust": "0.0",
//    "Emotion": "Surprise",
//    "Fear": "0.13",
//    "FrustrationLevel": "0.55799997",
//    "Happy": "0.013",
//    "Neutral": "0.409",
//    "Sad": "0.326",
//    "Surprise": "0.02"
//}
fun getEmotionFromApi(): JSONObject {
    try {
        val response = khttp.get("http://127.0.0.1:5000/get_emotion")
        println("response: ${response.toString()} ")
        return response.jsonObject
    }
    catch (e: ConnectException) {
        println("No connection to http://127.0.0.1:5000.")
        return JSONObject("{}");
    }
}

//val detectEmotion: State = state {
//
//    onEntry {
//        val emotionResponse = getEmotionFromApi();
//        println(emotionResponse);
//        if(frustrationEmotions.contains(emotionResponse["Emotion"]) || emotionResponse["FrustrationLevel"].toString().toFloat() > 0.4){
//            if(emotionResponse["FrustrationLevel"].toString().toFloat() > 0.4){
//                val takeabreak = furhat.askYN("You look frustrated. I understand this is hard. Do you want to take a break?")
//                if (takeabreak == true) {
//                    goto(TakeABreak)
//                }
//            }else{
//                random(
//                    furhat.say("Don't give up."),
//                    furhat.say("Don't worry. I will help you.")
//                )
//            }
//        }else if(emotionResponse["Emotion"]=="Neutral"){
//            furhat.say("You did good.")
//        }
//
//        if(emotionResponse["Emotion"]=="Happy"){
//            // mirroring
//            onTime(0, 1000) {
//                furhat.gesture(furhatos.gestures.Gestures.BigSmile())
//            }
//        }
//    }
//
//}

//val frustrationState: State = state {
//    onEntry{
//        furhat.say("""
//            You look frustrated. I understand this is hard. Do you want to take a break?
//        """.trimIndent())
//    }
//
//    onResponse<Quit>{
//        goto(TakeABreak)
//    }
//    onResponse<Yes>{
//        goto(TakeABreak)
//    }
//}