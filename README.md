The database used by this app is firebase-firestore with the support of node.js to listen to changes in collections of firebase-firestore. Please refer link below for node.js installation
https://firebase.google.com/docs/functions/firestore-events

The node.js file "NotificationService" is to be copy and paste in firebase project functions folder.
After that, please deploy it using "firebase deploy --only functions" in node.js cli

Once everything is set up, the app will be able to receive notification whenever there is an addition of document in AbnormalActivity collection.

Successfully accomplished positioning of abnormal activity and security guard. Required Google's Direction API which is free to use. Ignore credential restriction for API key as it will cause error when executing the API for second time.

Restricted range of guards who will receive notification when abnormal activity detected. It is based on guard's duty floor level and schedule. Guard will receive notification only when he/she is in the floor level of abnormal activity and is on duty. 

Notification will also be sent to guards in other floor level when the feedback of the abnormal activity is "Require Further Action" which mean require more guards to help resolving the abnormal activity.
