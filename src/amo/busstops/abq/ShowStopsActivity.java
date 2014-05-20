package amo.busstops.abq;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
//import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class ShowStopsActivity extends FragmentActivity implements android.location.LocationListener {
	
	ArrayList<ArrayList<LatLng>> bsLatLngs; // coordinates of stops
	ArrayList<String> bsStrings; // descriptions of stops
	ArrayList<ArrayList<LatLng>> polyLatLngs; // coordinates of lines
	ArrayList<String> polyStrings; // descriptions of placemarks

	public ProgressDialog computeDialog;
	public GoogleMap mMap;
	public boolean showCurrentLocation = false; // do this once
    // Lat/Lngs
	private double latC = 0;
    private double lonC = 0;
    private double oldLat = 0;
    private double oldLon = 0;
    private double lastDist = 0;

    // GPS location
    public LocationManager locationManager;
    public String provider; // location provider
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.showstops_main);
		//bsLatLngs = getCoordinateArrays();
		//System.out.println("Total stops: " + bsLatLngs.size());
		
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        if(status!=ConnectionResult.SUCCESS){ // Google Play Services are not available

        	int requestCode = 10;
	        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
	        dialog.show();

	    }else { // Google Play Services are available

            // Getting reference to the SupportMapFragment of activity_main.xml
            SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            // Getting GoogleMap object from the fragment
            mMap = fm.getMap();

            // Enabling MyLocation Layer of Google Map
            mMap.setMyLocationEnabled(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
	       //     mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
	           
            // Zoom in the Google Map
            mMap.animateCamera(CameraUpdateFactory.zoomTo(20));
         /*
	            mMap.addMarker(new MarkerOptions()
	            .position(latLng)
	            .title("Photo taken here."));
	           */
            // Getting LocationManager object from System Service LOCATION_SERVICE
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Creating a criteria object to retrieve provider
            Criteria criteria = new Criteria();

            // Getting the name of the best provider
            provider = locationManager.getBestProvider(criteria, true);

            // Getting Current Location
            Location location = locationManager.getLastKnownLocation(provider);

            if(location!=null){
                onLocationChanged(location);
            }
            // every 20secs request
            locationManager.requestLocationUpdates(provider, 20000, 0, this);
        }
		
		ComputeStopsTask ct = new ComputeStopsTask();
		ct.execute("");
		
		//mMap.
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
    public void onLocationChanged(Location location) {
        // Getting latitude of the current location
        double latitude = location.getLatitude();
        // Getting longitude of the current location
        double longitude = location.getLongitude();

        latC = latitude; // currentposition
        lonC = longitude;
        boolean updateLocation = false;
    	if(!showCurrentLocation) {  // first coordinate lock
    		updateLocation = true;
    	} else {
    		double distF = distFrom(oldLat, oldLon, latC, lonC);
    		// approx 8-12 blocks in a mile depending on city...
    		// using 0.15 mile as the update
    		if(distF >= 0.15 && oldLat != 0) {
    			updateLocation = true;
    			System.out.println("Moved: " + distF);
    			// setup a new draw
    			addMarkersWithinX(0.5); // ~ 1/2 mile
    		}
    	}
    	if(updateLocation) {
    		// storing old coordinates to see if we moved x
    		oldLat = latC;
    		oldLon = lonC;
    		// Creating a LatLng object for the current location
    		LatLng latLng1 = new LatLng(latitude, longitude);
    		// Showing the current location in Google Map
    		mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng1));
    		// Zoom in the Google Map
    		mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    		showCurrentLocation = true; // dist updates only on moving x
    		// Setting latitude and longitude in the TextView tv_location
    		System.out.println("Latitude:" +  latitude  + ", Longitude:"+ longitude );
    	}
    }

	    
	public void clearMarkers() {
		try {
			mMap.clear();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/*
	 * Simple bool to compare current location with other within X miles
	 */
	public boolean isNearby(LatLng otherLocation, double xMiles) {
		double otherLat = otherLocation.latitude;
		double otherLon = otherLocation.longitude;
		// latC and lonC are set automatically by the LocationManager
		double resD = distFrom(latC, lonC, otherLat, otherLon);
		if(resD >= xMiles) {
			return false;
		} else {
			return true;
		}
	}
	
	
	/*
	 * GEO Fence our locations to within X miles
	 * only adds map markers for locations within x
	 */
	public void addMarkersWithinX(double xMiles) {
		clearMarkers();
		// current full Coordinate Set is...
		//		bsLatLngs
		int llc = 0;
		while (bsLatLngs.size() > llc) {
			ArrayList<LatLng> oneTrack = new ArrayList<LatLng>();
			oneTrack = bsLatLngs.get(llc);
			LatLng lln = oneTrack.get(0);
			try {
				// test location distance from current...
				if(isNearby(lln, xMiles)) {
					mMap.addMarker(new MarkerOptions()
					.position(lln)
					.title(bsStrings.get(llc)));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			llc++;
		}

	}

	/*
	 *  Calculate the distance between 2 LatLng points using the Earths median Radius
	 */
	public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double sindLat = Math.sin(dLat / 2);
	    double sindLng = Math.sin(dLng / 2);
	    double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
	            * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;
        return dist;
	}
	

	/*
	 * Return an Array of an Array of LatLngs from the KML.
	 * Supports multiple coordinates per result set.
	 * Current ABQ Data only provides one set of coordinates per result.
	 */
	public ArrayList<ArrayList<LatLng>> getCoordinateArrays() {
	    ArrayList<ArrayList<LatLng>> allTracks = new ArrayList<ArrayList<LatLng>>();

	    try {
	        StringBuilder buf = new StringBuilder();
	        // online copy is @
	        // http://data.cabq.gov/transit/realtime/busstops/busstops.kml
	        // using stored copy to make this sample easier
	        InputStream json = this.getAssets().open("busstops.kml");
	        BufferedReader in = new BufferedReader(new InputStreamReader(json));
	        String str;
	        String buffer;
	        while ((str = in.readLine()) != null) {
	            buf.append(str);
	        }

	        in.close();
	        String html = buf.toString();
	        
	        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
	        ArrayList<String> tracksString = new ArrayList<String>();
	        ArrayList<String> tracksName = new ArrayList<String>();
	        for (Element e : doc.select("coordinates")) {
	            tracksString.add(e.toString().replace("<coordinates>", "").replace("</coordinates>", ""));
	        }
	        
	        for (Element e : doc.select("description")) {
	        	/*
	        	 * 
	        	 *     <table border="1">
          <tr>
            <td>ID</td>
            <td>2013</td>
          </tr>
          <tr>
            <td>Name</td>
            <td>Academy @ Eubank</td>
          </tr>
          <tr>
            <td>Serving</td>
            <td>
            1 - Juan Tabo - Southbound<br/>
            93 - Academy Commuter - North and East-PM<br/>
            </td>
          </tr>
        </table>
	        	 * 
	        	 * 
	        	 */
	        	
	        	String tmpStore = e.toString().replace("<description>", "").replace("</description>", ""); 
	            // Find the third <TR>
	        	int ltr = tmpStore.lastIndexOf("<tr>");
	        	if(ltr > -1) {
	        		String tmp = tmpStore.substring(ltr + 4);
	        		// now find the last <TD>
	        		int ltd = tmp.lastIndexOf("<td>");
	        		if(ltd > -1) {
	        			tmp = tmp.substring(ltd + 4);
	        			// strip out the </td>
	        			tmp = tmp.replace("</td>", "");
	        			// strip out the tr...
	        			tmp = tmp.replace("</tr>", "");
	        			// strip out the table...
	        			tmp = tmp.replace("</table>", "");
	        			// replace <br>'s
	        			tmp = tmp.replace("<br />", "\r\n");
	        			tmp = tmp.replace("<br />", "\r\n");
	        			tmp = tmp.replace("<br />", "\r\n");
	        			tmp = tmp.replace("<br />", "\r\n");
	        			// add the tracksName
	        			tracksName.add(tmp);
	        		}
	        	}
	        	
	        	//tracksName.add();
	        }
	        bsStrings = tracksName;
	        System.out.println("DESC: " + tracksName.size());
	        // find the 2nd <TD>...
	        
	        
	        for (int i = 0; i < tracksString.size(); i++) {
	            ArrayList<LatLng> oneTrack = new ArrayList<LatLng>();
	            ArrayList<String> oneTrackString = new ArrayList<String>(Arrays.asList(tracksString.get(i).split("\\s+")));
	            for (int k = 1; k < oneTrackString.size(); k++) {
	            	// reverse order since from ABQ, its LNG / LAT
	            	//System.out.println("LatLng: " + Double.parseDouble(oneTrackString.get(k).split(",")[1]) + " : " +  Double.parseDouble(oneTrackString.get(k).split(",")[0]));
	                LatLng latLng = new LatLng(Double.parseDouble(oneTrackString.get(k).split(",")[1]),
	                        Double.parseDouble(oneTrackString.get(k).split(",")[0]));
	                oneTrack.add(latLng);
	            }
	            allTracks.add(oneTrack);
	        }
	    } catch (Exception ex) {
	    	ex.printStackTrace();
	    }
	    return allTracks;
	}
		
	/*
	 * ASYNC Task for reading ABQ Bus Stops KML
	 * Reads and Parses/Stores route Data to LatLng and DESC Arrays
	 * Only needs to run once.
	 * 2,882 stops in Albuquerque - so needs to be in it's own thread
	 * Currently takes approx 30 seconds to analyze... ~1.6MB file
	 * 
	 * HINT: Possible to speed this up by precompiling the Array/s.
	 */
	private class ComputeStopsTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			 try {
		        computeDialog = ProgressDialog.show(ShowStopsActivity.this, "",
		                "Analyzing 2,882 Bus Stops...", true);
		        computeDialog.setCancelable(true);
		     } catch (Exception e) {
		       	e.printStackTrace();
		     }
		}
		
		@Override
		protected String doInBackground(String... urls) {
			// TODO Auto-generated method stub
			String response = "";
			try {
				for (String url : urls) {
					bsLatLngs = getCoordinateArrays();
					System.out.println("Total stops: " + bsLatLngs.size());
					polyLatLngs = getPolyCoordinateArrays();
					System.out.println("Total routes: " + polyLatLngs.size());
					//mMap.m
					return "200";
				}
			} catch (Exception ex) {
				//return "501";
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if(result == null || result.equals("Error")) {

			} else if(result.equals("200")) {
				computeDialog.setTitle("Plotting...");
				// ** shows both ways to do this, with or without GeoFencing...
				
				
				// with GEO Fencing - within .5 miles
				addMarkersWithinX(0.5);
	
				// with Northern NM Bus Routes as lines...
				addPolylines();
				
				// without...
				/*
				int llc = 0;
				while (bsLatLngs.size() > llc) {
					ArrayList<LatLng> oneTrack = new ArrayList<LatLng>();
					oneTrack = bsLatLngs.get(llc);
					LatLng lln = oneTrack.get(0);
					
					
					try {
					//System.out.println(bsLatLngs.get(llc));
					//LatLng lln = new LatLng(bsLatLngs.get(llc));
					mMap.addMarker(new MarkerOptions()
		            .position(lln)
		            .title(bsStrings.get(llc)));
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					llc++;
				}
				*/

			}
			try {
				computeDialog.dismiss();
			} catch (Exception ex) { 
				ex.printStackTrace();
			}
		}
    }

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onStop()
	 * Prevent the locationManager from still being active when Activity isn't visible
	 */
	@Override
	public void onStop() {
	    super.onStop();
	    try{
	        locationManager.removeUpdates(this);
	    } catch (Exception ex) {
	        //locationManager.re
	    }
	}

	/*
	 * (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onResume()
	 * 	 * Make sure locationManager keeps running while visible...
	 */
	@Override
	protected void onResume()
	{
	    super.onResume();
	    try{
	        // 20 seconds...
	        locationManager.requestLocationUpdates(provider, 20000, 0, this); // 180k = 3 mins
	    } catch (Exception ex) {
	        // null or otherwise...
	    }
	   
	}

	/*
	 * Return an Array of an Array of LatLngs from the KML.
	 * Supports multiple coordinates per result set.
	 * Used specifically to plot drawn lines on a map.
	 */
	public ArrayList<ArrayList<LatLng>> getPolyCoordinateArrays() {
	    ArrayList<ArrayList<LatLng>> allTracks = new ArrayList<ArrayList<LatLng>>();

	    try {
	        StringBuilder buf = new StringBuilder(); 
	        // using stored copy to make this sample easier
	        InputStream json = this.getAssets().open("northnmbusroutes.kml");
	        BufferedReader in = new BufferedReader(new InputStreamReader(json));
	        String str;
	        String buffer;
	        while ((str = in.readLine()) != null) {
	            buf.append(str);
	        }

	        in.close();
	        String html = buf.toString();
	        Document doc = Jsoup.parse(html, "", Parser.xmlParser());
	        ArrayList<String> tracksString = new ArrayList<String>();
	        ArrayList<String> tracksName = new ArrayList<String>();
	        // this is all we really care about here...
	        for (Element e : doc.select("coordinates")) {
	            tracksString.add(e.toString().replace("<coordinates>", "").replace("</coordinates>", ""));
	        }
	        // grab the route name:
	        for (Element e : doc.select("name")) {
	        	String tmpName = e.toString().replace("<name>", "").replace("</name>", "");
	        	tracksName.add(tmpName);
	        }
	        
	        
	        //for (Element e : doc.select("description")) {
	        	/*
	        	 * ignoring desc here, it's useless...
	        	 * Better to pull the following:
	        	 *     <name>599</name>
    					<Snippet></Snippet>
    					<Placemark id="ID_00001">

	        	 * 
	        	 */
	        	/*
	        	String tmpStore = e.toString().replace("<description>", "").replace("</description>", ""); 
	        	*/
	        	//tracksName.add();
	        //}
	        polyStrings = tracksName;
	        //System.out.println("DESC: " + tracksName.size());
	        for (int i = 0; i < tracksString.size(); i++) {
	            ArrayList<LatLng> oneTrack = new ArrayList<LatLng>();
	            ArrayList<String> oneTrackString = new ArrayList<String>(Arrays.asList(tracksString.get(i).split("\\s+")));
	            for (int k = 1; k < oneTrackString.size(); k++) {
	            	// reverse order since from ABQ, its LNG / LAT
	            	System.out.println("LatLng: " + Double.parseDouble(oneTrackString.get(k).split(",")[1]) + " : " +  Double.parseDouble(oneTrackString.get(k).split(",")[0]));
	                LatLng latLng = new LatLng(Double.parseDouble(oneTrackString.get(k).split(",")[1]),
	                        Double.parseDouble(oneTrackString.get(k).split(",")[0]));
	                oneTrack.add(latLng);
	            }
	            allTracks.add(oneTrack);
	        }
	    } catch (Exception ex) {
	    	ex.printStackTrace();
	    }
	    return allTracks;
	}

	/*
	 * Add's drawn lines for the routes...
	 * Could be buses, trains, bike routes, etc.
	 */
	public void addPolylines() {
		int llc = 0;
		while (polyLatLngs.size() > llc) {
			ArrayList<LatLng> oneTrack = new ArrayList<LatLng>();
			oneTrack = polyLatLngs.get(llc);
			PolylineOptions plo = new PolylineOptions();
			plo.geodesic(true);
			plo.color(getRandomColor());
			int ot = 0;
			while (oneTrack.size() > ot) {
				try {
					LatLng lln = oneTrack.get(ot);
					plo.add(lln);
				} catch (Exception ex) {};
				ot++;
			}
			mMap.addPolyline(plo);
			llc++;
		}

		
	}
	
	/*
	 * Generates a random color for our drawn lines on the map
	 */
	public int getRandomColor() {
		Random rnd = new Random(); 
		int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)); 
		return color;
	}
	
}
