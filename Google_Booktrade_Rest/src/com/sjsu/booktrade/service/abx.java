package com.sjsu.booktrade.service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
 
public class abx {
     
    private static final String GEOCODE_REQUEST_URL = "http://maps.googleapis.com/maps/api/geocode/xml?sensor=false&";
    private static HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
     
    public static void main(String[] args) throws Exception {
        abx tDirectionService = new abx();
        tDirectionService.getBooksFromISBN("9780984782802");
        //tDirectionService.getLongitudeLatitude("1833 13th Avenue Seattle");
        //tDirectionService.insertInAddress();
    }
     
    public void getLongitudeLatitude(String address) {
        try {
            StringBuilder urlBuilder = new StringBuilder(GEOCODE_REQUEST_URL);
            if (address!= null && address.length() > 0) {
                urlBuilder.append("&address=").append(URLEncoder.encode(address, "UTF-8"));
            }
 
            final GetMethod getMethod = new GetMethod(urlBuilder.toString());
            try {
                httpClient.executeMethod(getMethod);
                Reader reader = new InputStreamReader(getMethod.getResponseBodyAsStream(), getMethod.getResponseCharSet());
                 
                int data = reader.read();
                char[] buffer = new char[1024];
                Writer writer = new StringWriter();
                while ((data = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, data);
                }
                
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader("<"+writer.toString().trim()));
                Document doc = db.parse(is);
             
                String strLatitude = getXpathValue(doc, "//GeocodeResponse/result/geometry/location/lat/text()");
                System.out.println("Latitude:" + strLatitude);
                 
                String strLongtitude = getXpathValue(doc,"//GeocodeResponse/result/geometry/location/lng/text()");
                System.out.println("Longitude:" + strLongtitude);
                 
                 
            } finally {
                getMethod.releaseConnection();
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
    }
 
    private String getXpathValue(Document doc, String strXpath) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xPath.compile(strXpath);
        String resultData = null;
        Object result4 = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result4;
        for (int i = 0; i < nodes.getLength(); i++) {
            resultData = nodes.item(i).getNodeValue();
        }
        return resultData;
    }
    
    public void getBooksFromISBN(String isbn) {
        try {
//        	final String ISBN_REQUEST_URL = "https://www.googleapis.com/books/v1/volumes?key=AIzaSyCeePI9-ohjbLSBm-vxDBYm5oIyTcANCPU";
//            StringBuilder urlBuilder = new StringBuilder(ISBN_REQUEST_URL);
//            if (isbn!= null && isbn.length() > 0) {
//                urlBuilder.append("&q=isbn:").append(URLEncoder.encode(isbn, "UTF-8"));
//            }
// 
//            final GetMethod getMethod = new GetMethod(urlBuilder.toString());
        	HttpTransport httpTransport = new NetHttpTransport();
		    
//			JSONObject jsonObject = new JSONObject(isbnString);
//			String isbn = jsonObject.getString("isbn");
			
			final String ISBN_REQUEST_URL = "https://www.googleapis.com/books/v1/volumes?key=AIzaSyCeePI9-ohjbLSBm-vxDBYm5oIyTcANCPU";
            StringBuilder urlBuilder = new StringBuilder(ISBN_REQUEST_URL);
            if (isbn!= null && isbn.length() > 0) {
                urlBuilder.append("&q=isbn:").append(URLEncoder.encode(isbn, "UTF-8"));
            }
			
			HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
			GenericUrl urlInfo = new GenericUrl(urlBuilder.toString());
			HttpRequest request=requestFactory.buildGetRequest(urlInfo);
			HttpResponse response =  request.execute();
        	
            try {
            	Reader reader = new InputStreamReader(response.getContent());
                //Reader reader = new InputStreamReader(getMethod.getResponseBodyAsStream(), getMethod.getResponseCharSet());
                 
                int data = 0;
                char[] buffer = new char[1024];
                Writer writer = new StringWriter();
                while ((data = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, data);
                }
                String result = writer.toString();
                System.out.println(result.toString());
                
                JSONObject json = new JSONObject(result);
                JSONArray arr = (JSONArray)json.get("items");
                JSONObject jsonInner = (JSONObject) arr.get(0);
                JSONObject bookNameJSON = (JSONObject) jsonInner.get("volumeInfo");
                String bookName = bookNameJSON.getString("title");
                StringBuilder authors = new StringBuilder();
                
                JSONArray arrAuthors = bookNameJSON.getJSONArray("authors");
                for(int i=0; i<arrAuthors.length(); i++){
                	authors.append(arrAuthors.get(i));
                	if(i+1 != arrAuthors.length())
                		authors.append(", ");
                }
                
                StringBuilder category = new StringBuilder();
                JSONArray categories = bookNameJSON.getJSONArray("categories");
                for(int i=0; i<categories.length(); i++){
                	category.append(categories.get(i));
                	if(i+1 != categories.length())
                		category.append(", ");
                }
                
                JSONObject imageJSON = (JSONObject) bookNameJSON.get("imageLinks");
                String smallImageURL = imageJSON.getString("smallThumbnail");
                String largeImageURL = imageJSON.getString("thumbnail");
                
                System.out.println("---------------------------"+bookName);
                System.out.println("---------------------------"+authors.toString());
                System.out.println("---------------------------"+category.toString());
                System.out.println("---------------------------"+smallImageURL.toString());
                System.out.println("---------------------------"+largeImageURL.toString());
             
                /*String strLatitude = getXpathValue(doc, "//GeocodeResponse/result/geometry/location/lat/text()");
                System.out.println("Latitude:" + strLatitude);
                 
                String strLongtitude = getXpathValue(doc,"//GeocodeResponse/result/geometry/location/lng/text()");
                System.out.println("Longitude:" + strLongtitude);*/
                 
                 
            } finally {
               // getMethod.releaseConnection();
            	response.disconnect();
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
    }
     
}