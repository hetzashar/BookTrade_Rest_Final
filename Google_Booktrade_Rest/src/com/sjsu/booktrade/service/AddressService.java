package com.sjsu.booktrade.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.sjsu.booktrade.model.AddressTO;
import com.sjsu.booktrade.model.AddressType;
import com.sjsu.booktrade.util.ConnectionPool;
import com.sjsu.booktrade.util.Constants;

public class AddressService {
	public static boolean saveAddress(String addressLine1, String addressLine2, 
			String city, String state, String postalCode, String type, Double latitude, Double longitude,
			int bookId, int userId){

		try{
			Connection connection = ConnectionPool.getConnectionFromPool();

			{
				PreparedStatement preparedStatement = connection
						.prepareStatement("insert into booktrade.address values(null, ? ,? ,? ,? ,? ,?, ?, ?, ?, ? )");
				preparedStatement.setString(1, addressLine1);
				preparedStatement.setString(2, addressLine2);
				preparedStatement.setString(3, city);
				preparedStatement.setString(4, state);
				preparedStatement.setString(5, postalCode);
				preparedStatement.setString(6, type);
				preparedStatement.setDouble(7, latitude);
				preparedStatement.setDouble(8, longitude);
				preparedStatement.setInt(9, bookId);
				preparedStatement.setInt(10, userId);

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

	public static AddressTO getAddressFromBookIdAndUserId(int bookId) {
		
		try{
			AddressTO address = new AddressTO();
			Connection connection = ConnectionPool.getConnectionFromPool();

			PreparedStatement preparedStatement = connection
					.prepareStatement("select * from booktrade.address where address.bookId = ?");
			preparedStatement.setInt(1, bookId);

			ResultSet resultSet = preparedStatement.executeQuery();

			while(resultSet.next()) {
				address.setAddressId(resultSet.getInt("addressId"));
				address.setAddressline1(resultSet.getString("address_line_1"));
				address.setAddressline2(resultSet.getString("address_line_2"));
				address.setCity(resultSet.getString("city"));
				address.setState(resultSet.getString("state"));
				address.setPincode(resultSet.getString("postal_code"));
				address.setLatitude(resultSet.getDouble("latitude"));
				address.setLongitude(resultSet.getDouble("longitude"));
				address.setUserId(resultSet.getInt("userId"));
				address.setBookId(resultSet.getInt("bookId"));
				if(resultSet.getString("type").equals(Constants.PICKUP)){
					address.setAddresstype(AddressType.PICKUP);
				}else{
					address.setAddresstype(AddressType.SHIPPING);	
				}
			}
			ConnectionPool.addConnectionBackToPool(connection);
			return address;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
