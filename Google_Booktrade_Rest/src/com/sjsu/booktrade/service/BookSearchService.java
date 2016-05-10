package com.sjsu.booktrade.service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
import com.sjsu.booktrade.model.AddressTO;
import com.sjsu.booktrade.model.BooksTO;
import com.sjsu.booktrade.util.ConnectionPool;
import com.sjsu.booktrade.util.Constants;

@Path("/books")
public class BookSearchService {
	
	private static final Logger log = Logger.getLogger(BookSearchService.class.getName());
	
	@POST
	@Path("/tradeabook")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response tradeabook(String booksString) throws Exception{
		try{
			boolean addStatus = false;

			JSONObject jsonObject = new JSONObject(booksString);
			String bookName = jsonObject.getString("bookname");
			String author = jsonObject.getString("author");
			int edition = jsonObject.getInt("edition");
			String pickUpOrShip = jsonObject.getString("pickUpOrShip");
			double price = jsonObject.getDouble("price");
			int userId = jsonObject.getInt("userId");
			String categories = jsonObject.getString("category");
			String imageURLSmall = null;
			String imageURLLarge = null;
			String notes = jsonObject.getString("notes");
			
			if(jsonObject.has("small_image_url"))
				imageURLSmall = jsonObject.getString("small_image_url");
			
			if(jsonObject.has("large_image_url"))
				imageURLLarge = jsonObject.getString("large_image_url");

			if(bookName.length() > 0 && author.length() > 0 && price > 0){

				Connection connection = ConnectionPool.getConnectionFromPool();

				PreparedStatement preparedStatement = connection
						.prepareStatement("insert into booktrade.books values(null, ? ,? ,? ,? ,? ,? ,?, ?, ?, ?, ? ,? , 'N', null)");
				preparedStatement.setString(1, bookName);
				preparedStatement.setString(2, author);
				preparedStatement.setInt(3, edition);
				preparedStatement.setString(4, pickUpOrShip);
				preparedStatement.setDouble(5, price);
				preparedStatement.setString(6, "N");
				preparedStatement.setString(7, categories);
				preparedStatement.setInt(8, userId);
				preparedStatement.setBoolean(9, true);
				preparedStatement.setString(10, imageURLSmall);
				preparedStatement.setString(11, imageURLLarge);
				preparedStatement.setString(12, notes);
				//TODO
				//Add columns transactionComplete

				int registerUpdate = preparedStatement.executeUpdate();
				if(registerUpdate==1){
					preparedStatement = connection.prepareStatement("select books.bookId from booktrade.books where books.book_name = ? and books.userId=?");
					preparedStatement.setString(1, bookName);
					preparedStatement.setInt(2, userId);
					ResultSet resultSet = preparedStatement.executeQuery();
					int bookId = 0;
					if(resultSet.next()){
						bookId = resultSet.getInt("bookId");
						addStatus = true;
					}

					ConnectionPool.addConnectionBackToPool(connection);
					if(pickUpOrShip.equalsIgnoreCase(Constants.PICKUP)){
						String addressLine1 = jsonObject.getString("addressLine1");
						String addressLine2 = jsonObject.getString("addressLine2");
						String city = jsonObject.getString("city");
						String state = jsonObject.getString("state");
						String postalCode = jsonObject.getString("postalCode");

						StringBuilder strAddress = new StringBuilder(addressLine1);
						strAddress.append(" ").append(addressLine2).append(" ").append(city).append(" ").append(state).append(" ").append(postalCode);
						HashMap<String, Double> latLongMap = findLatLong(strAddress);
						
						//latLongMap.put(Constants.LONGITUDE, 122.122222);
						addStatus = AddressService.saveAddress(addressLine1, addressLine2, city, state, postalCode, Constants.PICKUP, latLongMap.get(Constants.LATITUDE), 
								latLongMap.get(Constants.LONGITUDE), bookId, userId);
						String fromDay=null;
						if(jsonObject.has("day_from"))
						 fromDay = jsonObject.getString("day_from");
						
						String toDay=null;
						if(jsonObject.has("day_to"))
						toDay = jsonObject.getString("day_to");
						
						String timeFrom=null;
						if(jsonObject.has("time_from"))
						 timeFrom = jsonObject.getString("time_from");
						
						String timeTo=null;
						if(jsonObject.has("time_to"))
						 timeTo = jsonObject.getString("time_to");

						addStatus = SchedulesAndSlotService.saveDaysAvailableAndTime(bookId, userId, fromDay, toDay, timeFrom, timeTo);
					}
					
					if(addStatus){
						return Response.status(201).entity(bookId).build();
					}else{
						return Response.status(500).entity(" failed adding the book please try again "+bookName).build();
					}
				}else{
					return Response.status(500).entity(" failed adding the book please try again "+bookName).build();
				}
			}else{
				return Response.status(400).entity(" All fields should be entered, please try again").build();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return Response.status(400).entity(" Error Encountered!! ").build();
	}

	@POST
	@Path("/deleteBook")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteBook(int bookId) throws Exception{

		boolean deleteStatus = false;

		Connection connection = ConnectionPool.getConnectionFromPool();

		PreparedStatement preparedStatement = connection
				.prepareStatement("DELETE FROM BOOKTRADE.BOOKS WHERE BOOKID=?");
		preparedStatement.setInt(1, bookId);

		deleteStatus = preparedStatement.execute();

		if(deleteStatus){
			return Response.status(201).entity(bookId).build();
		}else{
			return Response.status(400).entity(" Error occured in deleting a book. Please try again ").build();
		}

	}

	@POST
	@Path("/fetchBookDetails")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchBookDetails(String bookString) throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();
		JSONObject jsonObject = new JSONObject(bookString);
		int bookId = jsonObject.getInt("bookId");
		PreparedStatement preparedStatement = connection
				.prepareStatement("SELECT * FROM booktrade.books WHERE BOOKID=?");
		preparedStatement.setInt(1, bookId);

		ResultSet rs = preparedStatement.executeQuery();
		BooksTO books = new BooksTO();
		while(rs.next()){
			books.setBookName(rs.getString("book_name"));
			books.setBookId(bookId);
			books.setCategory(rs.getString("category"));
			books.setEdition(rs.getInt("edition"));
			books.setPrice(rs.getDouble("price"));
			books.setPickUpOrShip(rs.getString("pickUpOrShip"));
			books.setAuthor(rs.getString("author"));
			books.setUser(UserService.getUserDetailsFromId(rs.getInt("userId")));
			if(books.getPickUpOrShip().equalsIgnoreCase(Constants.PICKUP)){
				books.setAddress(AddressService.getAddressFromBookIdAndUserId(bookId));
				books.setSchedules(SchedulesAndSlotService.getSchedulesFromBookId(bookId));
			}
			books.setImageURLLarge(rs.getString("image_url_small"));
			books.setImageURLSmall(rs.getString("image_url_large"));
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(books).build();
	}
	
	@POST
	@Path("/fetchAllAvailableBooks")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchAllAvailableBooks() throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();

		PreparedStatement preparedStatement = connection
				.prepareStatement("SELECT * FROM booktrade.books WHERE ISAVAILABLE=1");

		ResultSet rs = preparedStatement.executeQuery();
		List<BooksTO> booksList = new ArrayList<BooksTO>();
		while(rs.next()){
			BooksTO books = new BooksTO();
			books.setBookName(rs.getString("book_name"));
			books.setBookId(rs.getInt("bookId"));
			books.setCategory(rs.getString("category"));
			books.setEdition(rs.getInt("edition"));
			books.setPrice(rs.getDouble("price"));
			books.setPickUpOrShip(rs.getString("pickUpOrShip"));
			books.setAuthor(rs.getString("author"));
			books.setImageURLSmall(rs.getString("image_url_small"));
			books.setImageURLLarge(rs.getString("image_url_large"));
			books.setUser(UserService.getUserDetailsFromId(rs.getInt("userId")));
			books.setAddress(AddressService.getAddressFromBookIdAndUserId(rs.getInt("bookId")));
			books.setSchedules(SchedulesAndSlotService.getSchedulesFromBookId(rs.getInt("bookId")));
			booksList.add(books);
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(booksList).build();
	}
	
	@POST
	@Path("/searchBooks")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response searchBooks(String keywordStr) throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();

		StringBuilder query = new StringBuilder();
		JSONObject json = new JSONObject(keywordStr);
		String keyword = json.getString("query");
		query.append("SELECT * FROM booktrade.books WHERE upper(book_name) LIKE upper('%")
					.append(keyword).append("%') OR upper(category) ")
					.append("LIKE upper('%").append(keyword).append("%') OR upper(author) LIKE upper('%").append(keyword).append("%')");
		PreparedStatement preparedStatement = connection
				.prepareStatement(query.toString());
		ResultSet rs = preparedStatement.executeQuery();
		List<BooksTO> booksList = new ArrayList<BooksTO>();
		while(rs.next()){
			BooksTO books = new BooksTO();
			books.setBookName(rs.getString("book_name"));
			books.setBookId(rs.getInt("bookId"));
			books.setCategory(rs.getString("category"));
			books.setEdition(rs.getInt("edition"));
			books.setPrice(rs.getDouble("price"));
			books.setPickUpOrShip(rs.getString("pickUpOrShip"));
			books.setAuthor(rs.getString("author"));
			books.setImageURLSmall(rs.getString("image_url_small"));
			books.setImageURLLarge(rs.getString("image_url_large"));
			books.setUser(UserService.getUserDetailsFromId(rs.getInt("userId")));
			books.setAddress(AddressService.getAddressFromBookIdAndUserId(rs.getInt("bookId")));
			books.setSchedules(SchedulesAndSlotService.getSchedulesFromBookId(rs.getInt("bookId")));
			booksList.add(books);
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(booksList).build();
	}
	
	@POST
	@Path("/fetchBooksWithinFiftyMiles")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchBooksWithinFiftyMiles(String inputString) throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();

		JSONObject jsonObject = new JSONObject(inputString);
		int userId = jsonObject.getInt("userId");
		double latitude = jsonObject.getDouble("latitude");
		double longitude = jsonObject.getDouble("longitude");
		
		PreparedStatement preparedStatement = connection
				.prepareStatement("SELECT books.book_name, books.bookId, books.category, books.edition, books.price, books.pickUpOrShip, books.author, books.userId, "
						+ "address.latitude, address.longitude, books.image_url_small, books.image_url_large, books.notes FROM booktrade.books, booktrade.address WHERE ISAVAILABLE=1 AND books.bookId=address.bookId");

		preparedStatement.setInt(1, userId);
		ResultSet rs = preparedStatement.executeQuery();
		List<BooksTO> booksList = new ArrayList<BooksTO>();
		while(rs.next()){
			Double distance = distanceBetweenTwoPoints(latitude, longitude, rs.getDouble("latitude"), rs.getDouble("longitude"));
			System.out.println("distance:: "+distance);
			if(distance < 50){
				BooksTO books = new BooksTO();
				books.setBookName(rs.getString("book_name"));
				books.setBookId(rs.getInt("bookId"));
				books.setCategory(rs.getString("category"));
				books.setEdition(rs.getInt("edition"));
				books.setPrice(rs.getDouble("price"));
				books.setPickUpOrShip(rs.getString("pickUpOrShip"));	
				books.setAuthor(rs.getString("author"));
				books.setUser(UserService.getUserDetailsFromId(rs.getInt("userId")));
				books.setImageURLSmall(rs.getString("image_url_small"));
				books.setImageURLLarge(rs.getString("image_url_large"));
				books.setNotes(rs.getString("notes"));
				books.setUser(UserService.getUserDetailsFromId(rs.getInt("userId")));
				books.setAddress(AddressService.getAddressFromBookIdAndUserId(rs.getInt("bookId")));
				books.setSchedules(SchedulesAndSlotService.getSchedulesFromBookId(rs.getInt("bookId")));
				booksList.add(books);
			}
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(booksList).build();
	}
	
	
	 /* @Prefix Params latitude and longtitude of the two points
	 * @Postfix Distance in miles between the two points
	 * This method uses haversine cosine formulae to calculate distance between two points.
	 */
	public Double distanceBetweenTwoPoints(double lat1, double long1, double lat2, double long2){
		final int R = 3959; //Radius in miles
		Double latDistance = toRadians(lat2-lat1);
        Double lonDistance = toRadians(long2-long1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + 
                   Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * 
                   Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        Double distance = R * c;
        return distance;
	}
	
	private static Double toRadians(Double value) {
        return value * Math.PI / 180;
    }

	/**
	 * @param addresses are all the addresses whose latitude and longitude is to be calculated
	 * This method calculates latitude and longitude of a given address and updates in the corresponding bean
	 **/
	private void latitudeLongtitude(List<AddressTO> addresses){
		StringBuilder strAddress = null;
		for(AddressTO address: addresses){
			strAddress = new StringBuilder(address.getAddressline1()+" "+address.getAddressline2()+" "+address.getCity()+" "+address.getPincode());
			HashMap<String, Double> latLongMap = findLatLong(strAddress);
			address.setLatitude(latLongMap.get(Constants.LATITUDE));
			address.setLongitude(latLongMap.get(Constants.LONGITUDE));
		}
	}

	private HashMap<String, Double> findLatLong(StringBuilder strAddress) {
		try{
			final String GEOCODE_REQUEST_URL = "http://maps.googleapis.com/maps/api/geocode/xml?sensor=false&";
			HttpTransport httpTransport = new NetHttpTransport();
		    
			HashMap<String, Double> latLongMap = new HashMap<String, Double>();

			StringBuilder urlStrBuilder = new StringBuilder(GEOCODE_REQUEST_URL);
			if (strAddress != null && strAddress.length() > 0) {
				urlStrBuilder.append("&address=").append(URLEncoder.encode(strAddress.toString(), "UTF-8"));
			}
			HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
			GenericUrl urlInfo = new GenericUrl(urlStrBuilder.toString());
			HttpRequest request=requestFactory.buildGetRequest(urlInfo);
			HttpResponse response =  request.execute();
			
			try {
				Reader reader = new InputStreamReader(response.getContent());
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

				String strLongitude = getXpathValue(doc,"//GeocodeResponse/result/geometry/location/lng/text()");
				System.out.println("Longitude:" + strLongitude);
				
				latLongMap.put(Constants.LATITUDE, Double.parseDouble(strLatitude));
				latLongMap.put(Constants.LONGITUDE, Double.parseDouble(strLongitude));
				return latLongMap;
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				response.disconnect();
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}

		return null;
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
	
	@POST
	@Path("/getBooksFromISBN")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getBooksFromISBN(String isbnString){
        try {
			HttpTransport httpTransport = new NetHttpTransport();
			JSONObject jsonObject = new JSONObject(isbnString);
			String isbn = jsonObject.getString("isbn");
			final String ISBN_REQUEST_URL = "https://www.googleapis.com/books/v1/volumes?key=AIzaSyCeePI9-ohjbLSBm-vxDBYm5oIyTcANCPU&country=US";
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
                //System.out.println(response.getContent()); 
                int data = 0;
                char[] buffer = new char[1024];
                Writer writer = new StringWriter();
                while ((data = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, data);
                }
                String result = writer.toString();
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
                
               BooksTO books = new BooksTO();
               books.setBookName(bookName);
               books.setAuthor(authors.toString());
               books.setImageURLSmall(smallImageURL);
               books.setImageURLLarge(largeImageURL);
               books.setCategory(category.toString());
               
               return Response.status(201).entity(books).build();
            } finally {
            	response.disconnect();
            }
        } catch (Exception e) {
        	log.severe(e.getMessage());
             e.printStackTrace();
        }
        return Response.status(400).entity("Error Encountered").build();
    }
}
