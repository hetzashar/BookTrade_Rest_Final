package com.sjsu.booktrade.service;

public class Haversinecosine {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Double distanceInMiles = distanceBetweenTwoPoints(37.340851, -121.898492, 37.340851, -121.898492);
		System.out.println("Distance in miles:: "+distanceInMiles);

	}
	
	public static Double distanceBetweenTwoPoints(double lat1, double long1, double lat2, double long2){
		final int R = 3959; //Radius in miles
		Double latDistance = toRad(lat2-lat1);
        Double lonDistance = toRad(long2-long1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
                   Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * 
                   Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        Double distance = R * c;
         
        System.out.println("The distance between two lat and long is::" + distance);
        return distance;
	}
	
	private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }
	

}
