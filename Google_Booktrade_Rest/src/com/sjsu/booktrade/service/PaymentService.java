package com.sjsu.booktrade.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.sjsu.booktrade.util.ConnectionPool;
@Path("/payments")
public class PaymentService {
	
	@POST
	@Path("/addCreditsToUser")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addCreditsToUser(String strCredits) throws Exception{
		return Response.status(201).entity(addCreditsToSeller(strCredits)).build();
	}
	
	public static boolean removeCreditsOfBoughtBook(String strCredits) throws Exception{
		try{
			Connection connection = ConnectionPool.getConnectionFromPool();
			{
				JSONObject jsonObject = new JSONObject(strCredits);
				int userId = jsonObject.getInt("userId");
				double credits = jsonObject.getDouble("credits");
				PreparedStatement preparedStatement = connection
						.prepareStatement("update booktrade.user set credits=credits-? where userId=?");
				preparedStatement.setDouble(1, credits);
				preparedStatement.setInt(2, userId);

				int registerUpdate = preparedStatement.executeUpdate();
				ConnectionPool.addConnectionBackToPool(connection);
				if(registerUpdate == 1){
					return true;
				}	
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	
	}
	
	public static boolean addCreditsToSeller(String strCredits) throws Exception{
		try{
			Connection connection = ConnectionPool.getConnectionFromPool();
			{
				JSONObject jsonObject = new JSONObject(strCredits);
				int userId = jsonObject.getInt("userId");
				double credits = jsonObject.getDouble("credits");
				PreparedStatement preparedStatement = connection
						.prepareStatement("update booktrade.user set credits=credits+? where userId=?");
				preparedStatement.setDouble(1, credits);
				preparedStatement.setInt(2, userId);

				int registerUpdate = preparedStatement.executeUpdate();
				ConnectionPool.addConnectionBackToPool(connection);
				if(registerUpdate == 1){
					return true;
				}	
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	
	}
	
	@POST
	@Path("/getCredits")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getCredits(String strUser) throws Exception{
		return Response.status(201).entity(getCreditsForUser(strUser)).build();
	}

	private double getCreditsForUser(String strUser) {
		double credits = 0;
		try{
			Connection connection = ConnectionPool.getConnectionFromPool();
			{
				JSONObject jsonObject = new JSONObject(strUser);
				int userId = jsonObject.getInt("userId");
				PreparedStatement preparedStatement = connection
						.prepareStatement("SELECT user.credits FROM booktrade.user where user.userId=?");
				preparedStatement.setInt(1, userId);

				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()){
					credits = rs.getDouble("credits");
				}
				ConnectionPool.addConnectionBackToPool(connection);
				return credits;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return credits;
	}
	
}
