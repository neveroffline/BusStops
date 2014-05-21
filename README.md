Android Google Maps v2 Example using Open Civic Data for the National Day of Civic Hacking

Uses Bus Stops KML (Keyhole Markup Language) for the city of Albuquerque, NM USA

Also uses KML for the Northern New Mexico Bus Routes

More options @ http://cabq.gov/abq-data

========


TO SETUP:

	Please make sure you follow these instructions.
	 
	 AND
	 
	 keep calm...

If you do not already have the EXTRA Google Play Services Library (17), please grab it from the Android SDK Manager.
While you are in the SDK Manager, please click OBSOLETE and also include API 9 in your download/install - you will need both.


Once this is completed, import the Google Play Services library using File-Import-Existing Android - and point it to the SDK folder for android /extras/google/... - You only need this library, so deselect anything else.


You should now be ready to download this ZIP and import it as an existing Android Source.  Simply point it at the zip file extracted and copy the project to your workspace (checkbox).  You may use the same File - Import - Android Existing Source sequence.


Once Downloaded and imported, Right click the project tree in Eclipse, select properties, and then Android.
Remove the previous google_play_services_lib reference and ADD the new one you just added in the other step above.


After you have this "BusStops" project imported, please Right Click it in the project tree and select Android Tools - Rename Application Package.  Remember this new packagename as we will need it for the Google API Key setup.


Now Setup a new Google API Project, name it w/e you want

https://console.developers.google.com/project?authuser=0


	Under API's and AUTH - API's turn on the following:

	Google Maps Android API v2 	



From ECLIPSE, make sure you have your SH1 fingerprint DEBUG KEY:

ECLIPSE - WINDOW - PREFERENCES - ANDROID - BUILD

	it will look something like this

	 74:73:70:7F:3B:F8:43:77:87:77:77:77:55:E7:74:7D:CF:77:77:79
	 
	 
	 Highlight and Cut/Copy your SH1 fingerprint.
	 
	 Go back to your API Console
	 
	 Select CREATE NEW KEY under CREDENTIALS
	 
	 SELECT ANDROID
	 
	 paste your SH1 fingerprint and add a semicolon ;yourpackagenameyoucreatedabove
	 
	 it will look something like this (no whitespace)
	 
	 74:73:70:7F:3B:F8:43:77:87:77:77:77:55:E7:74:7D:CF:77:77:79;amo.busstops.abq
	 
	 
	 Click CREATE and paste your new KEY in the AndroidManifest.xml


	 
You should be good to go.  Whew.

	 
	 
