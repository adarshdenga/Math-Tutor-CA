### General imports ###
from __future__ import division
import numpy as np
import time

### Image processing ###
import cv2
from scipy.ndimage import zoom
from scipy.spatial import distance
import dlib
from imutils import face_utils

### Model ###
from tensorflow.keras.models import load_model

for i in range(1,11):
    print(i)

    # Capture frame-by-frame the video_capture initiated above
    # video_capture = cv2.VideoCapture(0)
    # ret, frame = video_capture.read()
    image_path = "./dataset/surprising_faces/"+str(i)+".jpg"

    # Image shape
    shape_x = 48
    shape_y = 48
    input_shape = (shape_x, shape_y, 1)

    # We have 7 emotions
    nClasses = 7

    # Count number of eye blinks (not used in model prediction)
    def eye_aspect_ratio(eye):

        A = distance.euclidean(eye[1], eye[5])
        B = distance.euclidean(eye[2], eye[4])
        C = distance.euclidean(eye[0], eye[3])
        ear = (A + B) / (2.0 * C)

        return ear

    # Initiate Landmarks
    (lStart, lEnd) = face_utils.FACIAL_LANDMARKS_IDXS["left_eye"]
    (rStart, rEnd) = face_utils.FACIAL_LANDMARKS_IDXS["right_eye"]

    (nStart, nEnd) = face_utils.FACIAL_LANDMARKS_IDXS["nose"]
    (mStart, mEnd) = face_utils.FACIAL_LANDMARKS_IDXS["mouth"]
    (jStart, jEnd) = face_utils.FACIAL_LANDMARKS_IDXS["jaw"]

    (eblStart, eblEnd) = face_utils.FACIAL_LANDMARKS_IDXS["left_eyebrow"]
    (ebrStart, ebrEnd) = face_utils.FACIAL_LANDMARKS_IDXS["right_eyebrow"]

    # Load the pre-trained X-Ception model
    model = load_model('Models/video.h5')

    # Load the face detector
    face_detect = dlib.get_frontal_face_detector()

    # Load the facial landmarks predictor
    predictor_landmarks  = dlib.shape_predictor("Models/face_landmarks.dat")

    # Face index, face by face
    face_index = 0

    # Image to gray scale
    gray = cv2.cvtColor(cv2.imread(image_path), cv2.COLOR_BGR2GRAY)

    # All faces detected
    rects = face_detect(gray, 1)

    #gray, detected_faces, coord = detect_face(frame)

    # For each detected face
    for (i, rect) in enumerate(rects):

        # Identify face coordinates
        (x, y, w, h) = face_utils.rect_to_bb(rect)
        face = gray[y:y+h,x:x+w]

        # Identify landmarks and cast to numpy
        shape = predictor_landmarks(gray, rect)
        shape = face_utils.shape_to_np(shape)

        # Zoom on extracted face
        face = zoom(face, (shape_x / face.shape[0],shape_y / face.shape[1]))

        # Cast type float
        face = face.astype(np.float32)

        # Scale the face
        face /= float(face.max())
        face = np.reshape(face.flatten(), (1, 48, 48, 1))

        # Make Emotion prediction on the face, outputs probabilities
        prediction = model.predict(face)

        # Most likely emotion
        prediction_result = np.argmax(prediction)

        # Emotion mapping
        emotion_map = {0:'Angry', 1:'Disgust', 2:'Fear', 3:'Happy', 4:'Sad', 5:'Surprise', 6:'Neutral',}
        emotion = emotion_map[prediction_result]

        print(emotion)
