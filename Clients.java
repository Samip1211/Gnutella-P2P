import java.util.*;
import java.net.*;
import java.io.*;

//Object describing the client which is passed aroundvichar

class ClientRequestAndResponseInformation implements Serializable{
	boolean searchQuery;
	boolean hitQuery;
	String messageId;
	int tTl;
	String fileName;
	int sourcePort;
	int destPort;
	
	public void setQuery(int serverId, int tTl,String fileName,String messageId){
		this.searchQuery=true;
		this.hitQuery= false;
		this.messageId= messageId;
		this.tTl= tTl;
		this.sourcePort= serverId;
		this.fileName=fileName;
	}
	
	
}


public class Clients {
	static int port;
	static String fileToGet;
	static List<Integer> otherClients;
	static Map<String,ClientRequestAndResponseInformation> map;
	static String query;
	static int ttl=3; 
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
							//Get the current time
							long start = System.currentTimeMillis();
							//Read the object stream
							ObjectInputStream in = new ObjectInputStream(threadClient.getInputStream());
							//Get the file to send to the other client
							ClientRequestAndResponseInformation clientRequestAndResponseInformation= (ClientRequestAndResponseInformation) in.readObject();
							//Print to console that serving one object
							System.out.println("Got Object");
							
							
							//Seeing this request for first time
							if(map.get(clientRequestAndResponseInformation.messageId)==null){
								//See if this query is valid
								if(clientRequestAndResponseInformation.searchQuery && clientRequestAndResponseInformation.tTl>0){
									//descrease the TTL
									clientRequestAndResponseInformation.tTl--;
									//Store the query for further use
									map.put(clientRequestAndResponseInformation.messageId,clientRequestAndResponseInformation);
									//Send this query to all the other clients connected to the current client
									new Thread(){
										public synchronized void run(){
											for(int otherClientPort:otherClients){
												//Don't send the query to the client who has started this query
												if(clientRequestAndResponseInformation.sourcePort!=otherClientPort){
													System.out.println("Sending to :"+otherClientPort);
													forwardTheRequestToOtherClients(otherClientPort,clientRequestAndResponseInformation);
												}else{
													//Print
													System.out.println("Not forwarding to"+otherClientPort+"as it is the origin");
												}
												
											}
										}
									}.start();
									
								}
								
								checkTheRequestAndSendTheFileIfExist(clientRequestAndResponseInformation);
								
								
								long end = System.currentTimeMillis( );
								long diff = end - start;
								//Get the time difference from the initialization of the thread till now
								System.out.println("Difference is : " + diff + " "+ start + "  "+ end);
							}else{
								//Since it is seen request check if it is hit Query.
								if(clientRequestAndResponseInformation.hitQuery){
									try{
										System.out.println("Got the reply from "+clientRequestAndResponseInformation.destPort);
									}catch(Exception e){
										System.out.println(e);
										System.out.println("There's something wrong with the connection");
									}
								}
									//System.out.println("Already seen this query and took neccessary actions. No need to do now");					
								
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
	public static synchronized void checkTheRequestAndSendTheFileIfExist(ClientRequestAndResponseInformation clientRequestAndResponseInformation){
		
		//LogOut the fileName
		System.out.println(clientRequestAndResponseInformation.fileName);
		//See if the file is present.
		if(clientRequestAndResponseInformation.fileName.equals("abc")){
			//Set the query so as to reply.
			clientRequestAndResponseInformation.hitQuery=true;
			clientRequestAndResponseInformation.searchQuery=false;
			clientRequestAndResponseInformation.destPort=port;
			//Connect to that client
			System.out.println("Trying to connect to"+clientRequestAndResponseInformation.sourcePort );
			try{
				
				Socket client = new Socket("localhost",clientRequestAndResponseInformation.sourcePort);
			
				ObjectOutputStream out= new ObjectOutputStream(client.getOutputStream());
				out.writeObject(clientRequestAndResponseInformation);
			
				out.close();	
			}catch(Exception e){
				System.out.println(e);
				System.out.println("Client has got the reply from different client");
			}
			
		}else{
			//NOTHING TO DO
		}
		
	}
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
	public  static  void connectToClientToDownloadFile(int serverPort){
		
		
		try{
			//Connect to other client
			Socket client = new Socket("localhost", serverPort);
			//Start the time
			long start = System.currentTimeMillis();
			//Get the output stream
		    OutputStream outToServer = client.getOutputStream();
		    ObjectOutputStream out = new ObjectOutputStream(outToServer);
         
		   	//Print the file name of the file which is being requested and savre the file
			System.out.println("File to Get" + fileToGet);
			out.writeObject(fileToGet);
			
			byte[] mybytearray = new byte[6022386];
			InputStream is = client.getInputStream();
			FileOutputStream fos = new FileOutputStream(fileToGet);
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
			System.out.println("Difference is : " + diff);
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
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
			Scanner reader = new Scanner(System.in);  // Reading from System.in
			System.out.println("Enter the query: ");
			query = reader.nextLine();
			//signal to exit
			if(query.equals("exit")){
				break;
			}else{
				//Search for the file in the query
				String uuid = UUID.randomUUID().toString();
				//System.out.println(uuid); Print the randomly generted string
				ClientRequestAndResponseInformation clientRequestAndResponseInformation = new ClientRequestAndResponseInformation();
			
				for(int otherClientPort:otherClients){
					new Thread(){
						public void run(){
							//set the object to be send 
							clientRequestAndResponseInformation.setQuery(port,ttl,query,uuid);
							//store the object in the map
							map.put(uuid,clientRequestAndResponseInformation);
							connectToClientforFileInformation(otherClientPort,clientRequestAndResponseInformation);
						}
					}.start();
					
				
				}
			}
			
			
		}
		System.exit(0);
		
	}
}
