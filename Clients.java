import java.util.*;
import java.net.*;
import java.io.*;

//Object describing the client which is passed around

class ClientRequestAndResponseInformation implements Serializable{
	boolean searchQuery;
	boolean getFile;
	boolean hitQuery;
	boolean invalidate;
	String messageId;
	int tTl;
	String fileName;
	int sourcePort;
	int destPort;
	
	public void setQuery(int serverId, int tTl,String fileName,String messageId,int flag){
		if(flag==1){
			this.searchQuery=true;
			this.hitQuery= false;
		}else{
			this.invalidate=true;
			this.searchQuery=false;
			this.hitQuery=false;
		}
		
		this.messageId= messageId;
		this.tTl= tTl;
		this.sourcePort= serverId;
		this.fileName=fileName;
	}
	
	
	
}




public class Clients {
	static int port; 												//Client Port
	static String fileToGet;										//The requested file	
	static List<Integer> otherClients;								//List of other clients presenr
	static Map<String,ClientRequestAndResponseInformation> map;		// Internal Data structure to check if the query has been seen
	static int ttl=3;												//Time to live
	static List<String> filesPresent;								//List of files present in the current directory
	
	//Call this method when u want your client to act as server
	public static void makeServer(int port) {
		
		System.out.println("Listeing on port" + port);
		try{
			ServerSocket server= new ServerSocket(port);
				//Listen to client request till true "indefinetly"
			while(true){
				Socket threadClient= server.accept();
					//On accepting client request spawn a new Thread
				new Thread(){
					public  void run(){
						try{
							long start = System.currentTimeMillis();
							//Read the object stream
							ObjectInputStream in = new ObjectInputStream(threadClient.getInputStream());
							
							//Get the file to send to the other client
							ClientRequestAndResponseInformation clientRequestAndResponseInformation= (ClientRequestAndResponseInformation) in.readObject();
							
							//Print to console that serving one object
							System.out.println("Got Object");
							
							//Seeing this request for first time
							if(map.get(clientRequestAndResponseInformation.messageId)==null){
								//If it is for the first time see if the query is for file searching or invalidate
								System.out.println("Status of the request"+clientRequestAndResponseInformation.searchQuery + " "+clientRequestAndResponseInformation.invalidate);
								if(clientRequestAndResponseInformation.searchQuery){
									
									//See if this query is valid
									if(clientRequestAndResponseInformation.tTl>0){
										//descrease the TTL
										clientRequestAndResponseInformation.tTl--;
										//Store the query for further use
										map.put(clientRequestAndResponseInformation.messageId,clientRequestAndResponseInformation);
									
										//Send this query to all the other clients connected to the current client
										new Thread(){
											public synchronized void run(){
											
												for(int otherClientPort:otherClients){
												
													// Initialize a new object to pass
													ClientRequestAndResponseInformation forOtherClient= new ClientRequestAndResponseInformation();
													//Don't send the query to the client who has started this query
													if(clientRequestAndResponseInformation.sourcePort!=otherClientPort){
														//Set the query to send
														forOtherClient.setQuery(clientRequestAndResponseInformation.sourcePort,clientRequestAndResponseInformation.tTl,
																			clientRequestAndResponseInformation.fileName,clientRequestAndResponseInformation.messageId,1);
													
														System.out.println("Sending to :"+otherClientPort);
														//Send the query
														forwardTheRequestToOtherClients(otherClientPort,forOtherClient);
													}else{
														//Print
														System.out.println("Not forwarding to"+otherClientPort+"as it is the origin");
													}
												
												}
											}
										}.start();
									
										//After sending check if the file is present and If present send the present file respose
										checkTheRequestAndSendTheFileIfExist(clientRequestAndResponseInformation);
								
										/*
										long end = System.currentTimeMillis( );
										long diff = end - start;
										//Get the time difference from the initialization of the thread till now
										System.out.println("Difference to complete request : " + diff + " "+ start + "  "+ end);*/
									}
									
								}else{
									if(clientRequestAndResponseInformation.invalidate){
										System.out.println(clientRequestAndResponseInformation.searchQuery);
										//See if this query is valid
										if(clientRequestAndResponseInformation.tTl>0){
											//descrease the TTL
											clientRequestAndResponseInformation.tTl--;
											//Store the query for further use
											map.put(clientRequestAndResponseInformation.messageId,clientRequestAndResponseInformation);
									
											//Send this query to all the other clients connected to the current client
											new Thread(){
												public synchronized void run(){
											
													for(int otherClientPort:otherClients){
												
														// Initialize a new object to pass
														ClientRequestAndResponseInformation forOtherClient= new ClientRequestAndResponseInformation();
														//Don't send the query to the client who has started this query
														if(clientRequestAndResponseInformation.sourcePort!=otherClientPort){
															//Set the query to send
															forOtherClient.setQuery(clientRequestAndResponseInformation.sourcePort,clientRequestAndResponseInformation.tTl,
																				clientRequestAndResponseInformation.fileName,clientRequestAndResponseInformation.messageId,2);
													
															System.out.println("Sending to :"+otherClientPort);
															//Send the query
															forwardTheRequestToOtherClients(otherClientPort,forOtherClient);
														}else{
															//Print
															System.out.println("Not forwarding to"+otherClientPort+"as it is the origin");
														}
												
													}
												}
											}.start();
									
											//After sending check if the file is present and If present send the present file respose
											checkIfFilePresentInvalidateAndGetNewVersion(clientRequestAndResponseInformation.fileName,clientRequestAndResponseInformation.sourcePort,
																							clientRequestAndResponseInformation.messageId);
								
											/*
											long end = System.currentTimeMillis( );
											long diff = end - start;
											//Get the time difference from the initialization of the thread till now
											System.out.println("Difference to complete request : " + diff + " "+ start + "  "+ end);*/
										}
									}
								}
									
							}else{
								//saw this request now check if it a hitquery or a file request query
								//Since it is seen request check if it is hit Query.
								if(clientRequestAndResponseInformation.hitQuery){
									try{
										//Print out
										System.out.println("Got the reply from "+clientRequestAndResponseInformation.destPort);
										
										//Seens the above client has the file connct to the client to request file
										connectToClientToDownloadFile(clientRequestAndResponseInformation.destPort,clientRequestAndResponseInformation.fileName,clientRequestAndResponseInformation.messageId);
									}catch(Exception e){
										System.out.println(e);
										System.out.println("There's something wrong with the connection");
									}
								}
								//Seen the query and if the query is for getting the file ("obtain(file)" function)
								if(clientRequestAndResponseInformation.getFile){
									
									
									//Get the current time
									long startForFile = System.currentTimeMillis();
									
									File myFile = new File(clientRequestAndResponseInformation.fileName);
									
									System.out.println(myFile.length());
									
									byte[] mybytearray = new byte[(int) myFile.length()];
									
									BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
									
									bis.read(mybytearray, 0, mybytearray.length);
							
									OutputStream os = threadClient.getOutputStream();
									
									os.write(mybytearray, 0, mybytearray.length);
									
									os.close();
									long end = System.currentTimeMillis( );
									long diff = end - startForFile;
									
									System.out.println("Difference to send file : " + diff);
																
								}
							}
						
								
								
						}catch(Exception e){
							System.out.println(e);
						}
					}
				
				}.start();
			}		
			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	//This function in activated when there is invalidate message it checks if the file 
	//is present in the download directory, if present it invalidates the file and
	//initiates the download of the new version of file.
	public static synchronized void checkIfFilePresentInvalidateAndGetNewVersion(String fileName,int port,String messageId){
		try{
			File file = new File(System.getProperty("user.dir")+"/downloads/"+fileName);
		
			if(file.delete()){
				System.out.println("File found!! getting the new version");
			}else{
			
			}
			connectToClientToDownloadFile(port,fileName,messageId);
			
		}catch(Exception e){
			
		}
		
		
	}
	//This function checks if the requesting file is present in the current directory
	public static synchronized void checkTheRequestAndSendTheFileIfExist(ClientRequestAndResponseInformation clientRequestAndResponseInformation){
		
		//LogOut the fileName
		System.out.println(clientRequestAndResponseInformation.fileName);
		//See if the file is present.
		if(filesPresent.contains(clientRequestAndResponseInformation.fileName)){
			//Set the query so as to reply.
			clientRequestAndResponseInformation.hitQuery=true;
			
			clientRequestAndResponseInformation.destPort=port;
			//Connect to that client
			System.out.println("Trying to connect to"+clientRequestAndResponseInformation.sourcePort );
			try{
				//Connect to the client and send the object 
				Socket client = new Socket("localhost",clientRequestAndResponseInformation.sourcePort);
			
				ObjectOutputStream out= new ObjectOutputStream(client.getOutputStream());
				out.writeObject(clientRequestAndResponseInformation);
				out.flush();
				out.close();	
			}catch(Exception e){
				System.out.println(e);
				System.out.println("Client has got the reply from different client");
			}
			
		}else{
			//NOTHING TO DO
		}
		
	}
	
	//This function is called by intermediateclients to forward the given query to clients that are present in its vicinity
	public static synchronized void forwardTheRequestToOtherClients(int serverPort, ClientRequestAndResponseInformation clientRequestAndResponseInformation){
		
		try{
			Socket client = new Socket("localhost",serverPort);
		    
			OutputStream outToServer = client.getOutputStream();
		    ObjectOutputStream out = new ObjectOutputStream(outToServer);
			out.writeObject(clientRequestAndResponseInformation);
			out.flush();
			
		}catch(Exception e){
			System.out.println(e);
		}
		
	}
	
	//This function is called by client who wants to get the given file from other clients
	public static void connectToClientforFileInformation(int serverPort, ClientRequestAndResponseInformation clientRequestAndResponseInformation){
		
		try{
			Socket client = new Socket("localhost",serverPort);
		    
			OutputStream outToServer = client.getOutputStream();
		    ObjectOutputStream out = new ObjectOutputStream(outToServer);
			out.writeObject(clientRequestAndResponseInformation);
			
			ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			ClientRequestAndResponseInformation response= (ClientRequestAndResponseInformation) in.readObject();
			
			if(response.hitQuery){
				System.out.println("Got Reply from "+ response.destPort);
				in.close();
			}else{
				System.out.println(response.destPort);
			}
		}catch(Exception e){
			System.out.println(e);
		}
		
	}
	
	//Call this method when u want to connect to the other client
	public  static synchronized void connectToClientToDownloadFile(int serverPort,String fileName,String messageId){
		
		
		try{
			if(!filesPresent.contains(fileName))
			{
				//Initailize new object to send
				ClientRequestAndResponseInformation sendObjectForFile= new ClientRequestAndResponseInformation();
				sendObjectForFile.getFile=true;
				sendObjectForFile.hitQuery=false;
				sendObjectForFile.fileName=fileName;
				sendObjectForFile.messageId=messageId;
				
				System.out.println("Going to get the file from "+ serverPort);
				
				//Connect to other client
				Socket client = new Socket("localhost", serverPort);
				//Start the time
				long start = System.currentTimeMillis();
				//Get the output stream
			    OutputStream outToServer = client.getOutputStream();
			    ObjectOutputStream out = new ObjectOutputStream(outToServer);
         
			   	//Print the file name of the file which is being requested and savre the file
				System.out.println("File to Get"+ sendObjectForFile.fileName );
				out.writeObject(sendObjectForFile);
			
				byte[] mybytearray = new byte[6022386];
				InputStream is = client.getInputStream();
				FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir")+"/downloads/"+fileName);
			    int bytesRead;
			    int current = 0;
				BufferedOutputStream bos = null;
				bos = new BufferedOutputStream(fos);
				bytesRead = is.read(mybytearray,0,mybytearray.length);
				do {
				        bos.write(mybytearray);
				        bytesRead = is.read(mybytearray);
				 } while (bytesRead != -1);
			
				bos.flush();
			
				client.close();
				//Get the difference between the connection of other client and getting the file
				long end = System.currentTimeMillis( );
				long diff = end - start;
				System.out.println("Difference to get file : " + diff);
				//filesPresent.add(fileName);
			}
			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	//This function is called on initialization of client so as to get the files present in the current directory
	 static void saveFilesinCurrDir(){
		filesPresent = new ArrayList<String>();
		
		File folder = new File(System.getProperty("user.dir"));
		
		File[] listOfFiles = folder.listFiles();
		
		//Get the files present in the directory
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				filesPresent.add(listOfFiles[i].getName());
				        //System.out.println("File " + listOfFiles[i].getName());
			} else if (listOfFiles[i].isDirectory()) {
				        //System.out.println("Directory " + listOfFiles[i].getName());
			}
		}
	}
	
	static void sendThePacketWithQueryOrInvalidate(String query,int flag){
		//Search for the file in the query
		String uuid = UUID.randomUUID().toString();
		//System.out.println(uuid); Print the randomly generted string
		ClientRequestAndResponseInformation clientRequestAndResponseInformation = new ClientRequestAndResponseInformation();
	
		//Iterate through the contents of the config file and send the query to the clients present in config file.
		for(int otherClientPort:otherClients){
			new Thread(){
				public void run(){
					//set the object to be send 
					clientRequestAndResponseInformation.setQuery(port,ttl,query,uuid,flag);
					//store the object in the map
					map.put(uuid,clientRequestAndResponseInformation);
				
					//Send the query to other clients
					connectToClientforFileInformation(otherClientPort,clientRequestAndResponseInformation);
				}
			}.start();
		
		}
	}
	
	//Main method. It is called when the program is runned
	public static void main(String[] args){
		//List of other clients
		otherClients = new ArrayList<Integer>();
		map = new HashMap<String,ClientRequestAndResponseInformation>();
		//Check the inline input
		if(args.length>=2){
			port= Integer.valueOf(args[0]);
		}else{
			//If error exit
			System.out.println("Please enter neccessary information about port and configFile");
			return;
		}
		//Check if the file exists and exit if not.
		try{
			Scanner scanner = new Scanner(new File(args[1]));
			while(scanner.hasNextInt()){
				otherClients.add(scanner.nextInt());
			}
		}catch(Exception e){
			//If error exit
			System.out.println("Please enter the valid configFile");
			return;
		}
		
		saveFilesinCurrDir();
		//Print the  nearby clients
		for(int c:otherClients){
			System.out.println(c);
		}
		//Run the server in background
		
		new Thread(){
			public void run(){
				makeServer(port);
				
			}
		}.start();
		
		
		//Get the user query i.e the file name
		while(true){
			System.out.println("Please choose what would you like to do: ");
			System.out.println("1. To get file");
			System.out.println("2. Enter the changed file name\n3. To exit");
			
			Scanner reader = new Scanner(System.in);  // Reading from System.in
			
			switch(reader.nextInt()){
				case 1:
					System.out.println("Enter the query");
				
					Scanner readQuery = new Scanner(System.in);  // Reading from System.in
					
					String query= readQuery.nextLine();
					
					sendThePacketWithQueryOrInvalidate(query,1);
					
					break;
				
				case 2:
					System.out.println("Enter the file that has changed");
			
					Scanner readInvalidate = new Scanner(System.in);  // Reading from System.in
					
					String invalidate= readInvalidate.nextLine();
					
					sendThePacketWithQueryOrInvalidate(invalidate,2);
				
					break;
				case 3:
					System.exit(0);
					break;
				default:
					System.exit(0);
			}
		}
	}
}