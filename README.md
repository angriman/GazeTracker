# GazeTracker
# TeamInfernale project - ESP1516 - University of Padova

This project implements two algorithms for Gaze Traking in an Android application.

The app activity flow is structured in this way:

1 - AskPermissionActivity: ask the camera permissions to the user only the first time the app is opened.

2 - MenuActivity: ask the user to choose the eye recognition algorithm to try.

3 - CalibrationActivity: calibration of the user eyes. The user has to start the calibration and follow with the eyes the red circle
 while it moves in the corners of the screen. 

4 - RecognitionActivity: simulation of the recognition algorithm. It uses the data collected during the calibration to calculate the 
region of the screen currently watched by the user.

Both CalibrationActivity and RecognitionActivity extends the MainActivity, that is an abstract class that starts the opencv camera and 
recognize the eyes in the frames taken.


PAY ATTENTION: in 'MainActivity.java' set the 'cameraIndex' variable to 0 if you are using the simulator to run the app.
