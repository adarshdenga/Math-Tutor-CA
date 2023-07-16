import os

from flask import Flask, Response

from library.speech_emotion_recognition import speechEmotionRecognition
from library.video_emotion_recognition import *
import logging
import pandas as pd
import json
from os.path import expanduser
home = expanduser("~")

logging.basicConfig(level=logging.DEBUG)

app = Flask(__name__)
app.config["DEBUG"] = True

# Display the video flow (face, landmarks, emotion)
@app.route('/display', methods=['GET'])
def display_video_flow() :
    try :
        # Response is used to display a flow of information
        return Response(gen(),mimetype='multipart/x-mixed-replace; boundary=frame')
    except :
        return None

# Get emotion for furhat
@app.route('/get_emotion', methods=['GET'])
def get_emotion():
    try:
        logging.info('Getting gen2 emotion')
        result = gen2()
        logging.info(f'{result}')
        audio_add = 0
        try:
            audio_result = audio_emotion_recognition()
            if(str(audio_result) in ["Angry","Disgust","Fear","Sad"]):
                print("added")
                audio_add = 1
                result["FrustrationLevel"] = result["FrustrationLevel"] * (1+0.2*audio_add)
                logging.info(f'{result}')
                return result
        except Exception:
            logging.warning('Exception for audio ocurred')
            logging.info(f'{result}')
            return result

    except Exception:
        logging.warning('Exception for general ocurred')
        return {
        "Angry": -1,
        "Disgust": -1,
        "Fear": -1,
        "Happy": -1,
        "Sad": -1,
        "Surprise": -1,
        "Neutral": -1,
        "Emotion": 'None',
        "FrustrationLevel": -1
    }

def audio_emotion_recognition():

    # Sub dir to speech emotion recognition model
    model_sub_dir = os.path.join('Models', 'audio.hdf5')

    # Instanciate new SpeechEmotionRecognition object
    SER = speechEmotionRecognition(model_sub_dir)

    print(home+"/.furhat/logs/test/dialog.json")

    # Voice Record sub dir
    f = open(home+"/.furhat/logs/test/dialog.json", 'r+')
    content = f.read()
    f.seek(0, 0)
    f.write('['+content+']')
    f.close()
    with open(home+"/.furhat/logs/test/dialog.json") as json_file:
        dialogJson = json.load(json_file)
        print(dialogJson)
    audio_name = ""
    for i in range(len(dialogJson) - 1, -1,-1):
        if(dialogJson[i]["type"] == "user.speech"):
            audio_name = dialogJson[i]["audio"]

    # rec_sub_dir = os.path.join('tmp','voice_recording.wav')
    rec_sub_dir = home+"/.furhat/logs/test/"+audio_name
    print("rec_sub_dir:"+str(rec_sub_dir))

    # Predict emotion in voice at each time step
    step = 1 # in sec
    sample_rate = 16000 # in kHz
    emotions, timestamp = SER.predict_emotion_from_file(rec_sub_dir, chunk_step=step*sample_rate)

    # Export predicted emotions to .txt format
    SER.prediction_to_csv(emotions, os.path.join("tmp", "audio_emotions.txt"), mode='w')
    SER.prediction_to_csv(emotions, os.path.join("tmp", "audio_emotions_other.txt"), mode='a')

    # Get most common emotion during the interview
    major_emotion = max(set(emotions), key=emotions.count)

    print("major_emotion:"+str(major_emotion))

    return major_emotion



if __name__ == '__main__':
    logging.info('Starting Flask Server')
    app.run()
