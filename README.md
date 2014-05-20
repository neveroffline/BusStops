Android Google Maps v2 Example using Open Civic Data for the National Day of Civic Hacking

Uses Bus Stops KML (Keyhole Markup Language) for the city of Albuquerque, NM USA

Also uses KML for the Northern New Mexico Bus Routes

More options @ http://cabq.gov/abq-data

========


TO SETUP:

Setup new Google API Project, name it w/e you want

https://console.developers.google.com/project?authuser=0


	Under API's and AUTH - API's turn on the following:


	Google Maps Android API v2 	

	Google Maps Geolocation API 	

	Google Maps JavaScript API v3


From ECLIPSE, grab your SH1 fingerprint DEBUG KEY:

ECLIPSE - WINDOW - PREFERENCES - ANDROID - BUILD

	it will look something like this

	 74:73:70:7F:3B:F8:43:77:87:77:77:77:55:E7:74:7D:CF:77:77:79
	 
	 
	 Highlight and Cut/Copy your SH1 fingerprint.
	 
	 Go back to your API Console
	 
	 Select CREATE NEW KEY
	 
	 SELECT ANDROID
	 
	 paste your SH1 fingerprint and add a semicolon ;yourpackagename
	 
	 it will look something like this
	 
	 74:73:70:7F:3B:F8:43:77:87:77:77:77:55:E7:74:7D:CF:77:77:79;amo.busstops.abq
	 
	 
	 Click OK and paste your new KEY in the AndroidManifest.xml
	 
	 Also make sure you right click project, rename application package
	 
	 AND
	 
	 that you import the Google Play Services lib from your android_sdk directory.
