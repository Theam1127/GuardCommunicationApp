The database used by this app is firebase-firestore with the support of node.js for client-server communication. Please refer link below for node.js installation
https://firebase.google.com/docs/functions/firestore-events

The node.js file "NotificationService" is to be copy and paste in firebase project functions folder.
After that, please deploy it using "firebase deploy --only functions" in node.js cli

Once everything is set up, the app will be able to receive notification whenever there is an addition of document in AbnormalActivity collection.


Next thing to do: indoor positioning
