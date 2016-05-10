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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.sjsu.booktrade.model.BooksTO;
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

			JSONObject jsonObject = new JSONObject(orderString);
			String pickUpOrShip = jsonObject.getString("pickUpOrShip");
			int userId = jsonObject.getInt("userId");
			int bookId = jsonObject.getInt("bookId");
			double price = jsonObject.getDouble("price");
			int sellerId = jsonObject.getInt("sellerId");

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

				String collectionTime=null;
				if(jsonObject.has("pickupTime"))
					collectionTime = jsonObject.getString("pickupTime");

				addStatus = SchedulesAndSlotService.updateBuyersPickupDetails(bookId, pickupDate, collectionTime, day);
			}else{
				String addressLine1 = jsonObject.getString("addressLine1");
				String addressLine2 = jsonObject.getString("addressLine2");
				String city = jsonObject.getString("city");
				String state = jsonObject.getString("state");
				String postalCode = jsonObject.getString("postalCode");

				addStatus = AddressService.saveAddress(addressLine1, addressLine2, city, state, postalCode, Constants.PICKUP, null, 
						null, bookId, userId);
			}
			ConnectionPool.addConnectionBackToPool(connection);
			if(addStatus){
				return Response.status(201).entity(bookId).build();
			}else{
				return Response.status(500).entity(" failed buying the book please try again ").build();
			}
		}catch(Exception e){
			e.printStackTrace();
		}

		return Response.status(400).entity(" Error Encountered!! ").build();
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
