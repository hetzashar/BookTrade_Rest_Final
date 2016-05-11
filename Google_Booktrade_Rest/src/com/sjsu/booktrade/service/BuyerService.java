package com.sjsu.booktrade.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.sjsu.booktrade.model.BooksTO;
import com.sjsu.booktrade.model.UserTO;
import com.sjsu.booktrade.util.ConnectionPool;
import com.sjsu.booktrade.util.Constants;
@Path("/buyer")
public class BuyerService {

	@POST
	@Path("/placeOrder")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response placeOrder(String orderString) throws Exception{
		try{
			boolean addStatus = false;

			StringBuilder messageBodyBuyer = new StringBuilder();
			StringBuilder messageBodySeller = new StringBuilder();
			String bookName = null;
			String emailAddressBuyer = null;
			String emailAddressSeller = null;
			String buyerName = null;
			String sellerName = null;
			
			
			JSONObject jsonObject = new JSONObject(orderString);
			String pickUpOrShip = jsonObject.getString("pickUpOrShip");
			int userId = jsonObject.getInt("userId");
			int bookId = jsonObject.getInt("bookId");
			double price = jsonObject.getDouble("price");
			int sellerId = jsonObject.getInt("sellerId");
			
			UserTO userBuyer = UserService.getUserDetailsFromId(userId);
			BooksTO books = BookSearchService.fetchBookDetailsFromId(bookId);
			UserTO userSeller = books.getUser();
			
			emailAddressBuyer = userBuyer.getEmailId();
			emailAddressSeller = userSeller.getEmailId();
			buyerName = userBuyer.getFirstName();
			sellerName = userSeller.getFirstName();
			bookName = books.getBookName();
			
			messageBodyBuyer.append("You have successfully placed and order for Book: ").append(bookName).append(" from the user ").append(sellerName).append(". ");
			messageBodySeller.append("Order has been placed for the ad posted by you in reference to the book: ").append(bookName).append(" by the user ").append(buyerName).append(". ");
			
			Connection connection = ConnectionPool.getConnectionFromPool();

			PreparedStatement preparedStatement = connection
					.prepareStatement("update booktrade.books set isAvailable=0, buyer_id=? where bookId=?");
			preparedStatement.setInt(1, userId);
			preparedStatement.setInt(2, bookId);

			org.json.JSONObject json = new org.json.JSONObject();
			json.put("userId",sellerId);
			json.put("credits",price);
			preparedStatement.executeUpdate();
			ConnectionPool.addConnectionBackToPool(connection);
			PaymentService.removeCreditsOfBoughtBook(json.toString());
			org.json.JSONObject jsonBuyer = new org.json.JSONObject();
			jsonBuyer.put("userId",userId);
			jsonBuyer.put("credits",price);
			PaymentService.addCreditsToSeller(jsonBuyer.toString());
			userBuyer = UserService.getUserDetailsFromId(userId);
			userSeller = UserService.getUserDetailsFromId(sellerId);
			if(pickUpOrShip.equalsIgnoreCase(Constants.PICKUP)){

				String date=null;
				if(jsonObject.has("pickupDate"))
					date = jsonObject.getString("pickupDate");
				SimpleDateFormat formatter = new SimpleDateFormat("mm-dd-yyyy");
				Date pickupDate = formatter.parse(date);
				Calendar c = Calendar.getInstance();
				c.setTime(pickupDate);
				int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

				String day = "";
				switch(dayOfWeek){
				case 1:
					day = "Monday";
					break;
				case 2:
					day = "Tuesday";
					break;
				case 3:
					day = "Wednesday";
					break;
				case 4:
					day = "Thursday";
					break;
				case 5:
					day = "Friday";
					break;
				case 6:
					day = "Saturday";
					break;
				case 7:
					day = "Sunday";
					break;
				}

				StringBuilder pickUpAddress = new StringBuilder();
				if(books.getAddress() != null){
					pickUpAddress.append(books.getAddress().getAddressline1()).append(" ").append(books.getAddress().getAddressline2()).append(" ")
					.append(books.getAddress().getCity()).append(" ").append(books.getAddress().getState()).append(" ").append(books.getAddress().getPincode());
				}
				String collectionTime=null;
				if(jsonObject.has("pickUpTime"))
					collectionTime = jsonObject.getString("pickUpTime");
				messageBodyBuyer.append(". You need to pick up the book on ").append(day).append(", ").append(date).append(" at ").append(collectionTime)
				.append(" on address ").append(pickUpAddress.toString());
				messageBodySeller.append(" Book will be picked up by the buyer on ").append(day).append(", ").append(date).append(" at ").append(collectionTime);
				addStatus = SchedulesAndSlotService.updateBuyersPickupDetails(bookId, pickupDate, collectionTime, day);
			}else{
				String addressLine1 = jsonObject.getString("addressLine1");
				String addressLine2 = jsonObject.getString("addressLine2");
				String city = jsonObject.getString("city");
				String state = jsonObject.getString("state");
				String postalCode = jsonObject.getString("postalCode");
				String address = addressLine1+" "+addressLine2+" "+city+" "+state+" "+postalCode;
				messageBodyBuyer.append(". Book will be shipped on address ").append(address);
				messageBodySeller.append(". Book needs to be shipped on address ").append(address);
				addStatus = AddressService.saveAddress(addressLine1, addressLine2, city, state, postalCode, Constants.PICKUP, null, 
						null, bookId, userId);
			}
			ConnectionPool.addConnectionBackToPool(connection);
			if(addStatus){
				messageBodyBuyer.append(". Contact seller in case of any discrepancies. Contact details of seller are as follows: ").append("Email Id: ")
				.append(emailAddressSeller).append(" Contact Number: ").append(userSeller.getContactNumber());
				messageBodySeller.append(". Contact buyer in case of any discrepancies. Contact details of buyer are as follows: ").append("Email Id: ")
				.append(emailAddressBuyer).append(" Contact Number: ").append(userBuyer.getContactNumber());
				
				messageBodyBuyer.append(". Cost of the book is ").append(books.getPrice()).append(" credits. These credits have been deducted and you have in all ").append(userBuyer.getCredits()).append(" credits.");
				messageBodySeller.append(". Cost of the book is ").append(books.getPrice()).append(" credits. These credits have been added to your account and you have in all ").append(userBuyer.getCredits()).append(" credits.");
				emailToBuyer(messageBodyBuyer.toString(), bookName, emailAddressBuyer, buyerName);
				emailToSeller(messageBodySeller.toString(), bookName, emailAddressSeller, sellerName);
				return Response.status(201).entity(bookId).build();
			}else{
				return Response.status(500).entity(" failed buying the book please try again ").build();
			}
		}catch(Exception e){
			e.printStackTrace();
		}

		return Response.status(400).entity(" Error Encountered!! ").build();
	}	

	private void emailToBuyer(String messageBody, String bookName, String emailAddressBuyer, String buyerName) {
		try {
			Properties props = new Properties();
			Session session = Session.getDefaultInstance(props, null);
			Message msg = new MimeMessage(session);
			msg.setContent("text", "TEXT/PLAIN");
			msg.setSubject("Book Trade Order for book: "+bookName);
			msg.setFrom(new InternetAddress("hetzashar@gmail.com", "BookTrade App"));
			msg.addRecipient(Message.RecipientType.TO,
					new InternetAddress(emailAddressBuyer, buyerName));
			msg.setText(messageBody);
			
			Transport.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void emailToSeller(String messageBody, String bookName, String emailAddressSeller, String sellerName) {
		try {
			Properties props = new Properties();
			Session session = Session.getDefaultInstance(props, null);
			Message msg = new MimeMessage(session);
			msg.setContent("text", "TEXT/PLAIN");
			msg.setSubject("Book Trade Order Booking for book: "+bookName);
			msg.setFrom(new InternetAddress("hetzashar@gmail.com", "BookTrade App"));
			msg.addRecipient(Message.RecipientType.TO,
					new InternetAddress(emailAddressSeller, sellerName));
			msg.setText(messageBody);
			
			Transport.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@POST
	@Path("/fetchAllPostedAds")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchAllPostedAds(String userIdString) throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();

		JSONObject jsonObject = new JSONObject(userIdString);
		int userId = jsonObject.getInt("userId");
		PreparedStatement preparedStatement = connection
				.prepareStatement("select * from booktrade.books where userId=? and isAvailable=1");

		preparedStatement.setInt(1, userId);
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
			books.setAddress(AddressService.getAddressFromBookIdAndUserId(books.getBookId()));
			if(books.getPickUpOrShip().equalsIgnoreCase(Constants.PICKUP)){
				books.setSchedules(SchedulesAndSlotService.getSchedulesFromBookId(books.getBookId()));
			}
			booksList.add(books);
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(booksList).build();

	}

	@POST
	@Path("/fetchAllPlacedOrders")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchAllPlacedOrders(String userIdString) throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();

		JSONObject jsonObject = new JSONObject(userIdString);
		int userId = jsonObject.getInt("userId");
		PreparedStatement preparedStatement = connection
				.prepareStatement("select * from booktrade.books where buyer_id=? and transaction_complete='N'");

		preparedStatement.setInt(1, userId);
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
			books.setAddress(AddressService.getAddressFromBookIdAndUserId(books.getBookId()));
			if(books.getPickUpOrShip().equalsIgnoreCase(Constants.PICKUP)){
				books.setSchedules(SchedulesAndSlotService.getSchedulesFromBookId(books.getBookId()));
			}
			booksList.add(books);
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(booksList).build();

	}

	@POST
	@Path("/fetchBoughtBooks")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchBoughtBooks(String userIdString) throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();

		JSONObject jsonObject = new JSONObject(userIdString);
		int userId = jsonObject.getInt("userId");

		PreparedStatement preparedStatement = connection
				.prepareStatement("select * from booktrade.books where buyer_id=?");

		preparedStatement.setInt(1, userId);
		ResultSet rs = preparedStatement.executeQuery();
		List<BooksTO> boughtBooksList = new ArrayList<BooksTO>();
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
			boughtBooksList.add(books);
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(boughtBooksList).build();
	}

	@POST
	@Path("/fetchSoldBooks")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fetchSoldBooks(String userIdString) throws Exception{

		Connection connection = ConnectionPool.getConnectionFromPool();

		JSONObject jsonObject = new JSONObject(userIdString);
		int userId = jsonObject.getInt("userId");

		PreparedStatement preparedStatement = connection
				.prepareStatement("select * from booktrade.books where userId=?");
		preparedStatement.setInt(1, userId);
		ResultSet rs = preparedStatement.executeQuery();
		List<BooksTO> soldBookList = new ArrayList<BooksTO>();
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
			soldBookList.add(books);
		}
		ConnectionPool.addConnectionBackToPool(connection);
		return Response.status(201).entity(soldBookList).build();
	}
}
