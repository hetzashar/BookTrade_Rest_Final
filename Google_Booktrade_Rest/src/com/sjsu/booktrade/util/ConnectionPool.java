package com.sjsu.booktrade.util;

import java.sql.Connection;  
import java.sql.DriverManager;  
import java.util.Vector;

import com.google.appengine.api.utils.SystemProperty;  

public class ConnectionPool implements Runnable   
{       
	
	public static void main(String[] args) throws Exception{
		getConnection();
	}
	// Number of initial connections to make.   
	private static int initialConnectionCount = 5;       

	// A list of available connections for use.   
	private static Vector<Connection> availableConnections = new Vector<Connection>();   

	// A list of connections being used currently.   
	private static Vector<Connection> usedConnections = new Vector<Connection>();   

	// The URL string used to connect to the database   
	//private static String urlString = "jdbc:google:mysql://forward-lead-91022:booktrade?user=root";   
	/*private static String urlString = "jdbc:mysql://173.194.237.249:3306?user=root";
	// The username used to connect to the database   
	private static String userName = "app";       

	// The password used to connect to the database   
	private static String password = "tiger";*/       

	// The cleanup thread   
	private Thread cleanupThread = null;   


	//Constructor   
	public ConnectionPool() throws Exception   
	{   

		for(int cnt=0; cnt<initialConnectionCount; cnt++)   
		{   
			availableConnections.addElement(getConnection());   
		}   

		cleanupThread = new Thread(this);   
		cleanupThread.start();   
	}       

	static int val =0;
	private static Connection getConnection() throws Exception   
	{   
		String url = null;
		if (SystemProperty.environment.value() ==
			    SystemProperty.Environment.Value.Production) {
			  // Connecting from App Engine.
			  // Load the class that provides the "jdbc:google:mysql://"
			  // prefix.
			  Class.forName("com.mysql.jdbc.GoogleDriver");
			  url =
			    "jdbc:google:mysql://booktrade-1272:booktrade?user=root";
			} else {
			 // Connecting from an external network.
			  Class.forName("com.mysql.jdbc.Driver");
			  url = "jdbc:mysql://173.194.231.106:3306?user=root";
			}
		return DriverManager.getConnection(url);   
	}   

	public static synchronized Connection getConnectionFromPool() throws Exception   
	{   
		Connection newConnxn = null;   

		if(availableConnections.size() == 0)   
		{   
			newConnxn = getConnection();   
			usedConnections.addElement(newConnxn);   
		}   
		else   
		{   
			newConnxn = (Connection)availableConnections.lastElement();   
			availableConnections.removeElement(newConnxn);   
			usedConnections.addElement(newConnxn);               
		}           

		return newConnxn;   
	}   


	public static synchronized void addConnectionBackToPool(Connection c)   
	{   
		if(c != null)   
		{   
			usedConnections.removeElement(c);   
			availableConnections.addElement(c);           
		}   
	}               

	public int availableCount()   
	{   
		return availableConnections.size();   
	}   

	public void run()   
	{   
		try   
		{   
			while(true)   
			{   
				synchronized(this)   
				{   
					while(availableConnections.size() > initialConnectionCount)   
					{   
						Connection c = (Connection)availableConnections.lastElement();   
						availableConnections.removeElement(c);   

						c.close();   
					}   

				}   

				System.out.println("CLEANUP : Available Connections : " + availableCount());   

				// Sleep for 1 minute   
				Thread.sleep(60000 * 1);   
			}       
		}   
		catch(Exception sqle)   
		{   
			sqle.printStackTrace();   
		}   
	}   
}
