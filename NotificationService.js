const functions = require('firebase-functions');

const admin = require('firebase-admin');

admin.initializeApp();

exports.notifyMessage = functions.firestore
	.document('AbnormalActivity/{documentID}')
	.onCreate((snap, context) => {
		const newActivity = snap.data();
		const activityID = newActivity.activityID;
		const activityImage = newActivity.activityImage;
		const activityStatus = newActivity.activityStatus;
		const cctvID = newActivity.cctvID;
		const promises=[];
	
	
		return admin.firestore().collection("Users").get().then(userDoc => {
			userDoc.forEach(function (doc){
				const registrationToken = doc.data().registrationToken;
			
				const payload = {
					data:{
						id: ""+activityID,
						img: ""+activityImage,
						stat: ""+activityStatus,
						cctv: ""+cctvID
					}
				}
				
				promises.push(admin.messaging().sendToDevice(registrationToken, payload));
			})
			
			return Promise.all(promises);
		}).then(results => {
			console.log("All notifications sent!");
			return true;
		  })
		  .catch(error => {
			 console.log(error);
			 return false;
		  });
	});

