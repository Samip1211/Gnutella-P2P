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
	
	
	public void setQuery(int serverId, int tTl,String fileName,String messageId){
		this.searchQuery=true;
		this.hitQuery= false;
		this.messageId= messageId;
		this.tTl= tTl;
		this.sourcePort= serverId;
		this.fileName=fileName;
	}
	public void setHitQuery(){
		
	}
	
}


public class Clients {
	static int port;
	static String fileToGet;
	static List<Integer> otherClients;
	static Map<String,ClientRequestAndResponseInformation> map; 
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
					public synchronized void run(){
						try{
							//Get the current time
							long start = System.currentTimeMillis();
							//Read the object stream
							ObjectInputStream in = new ObjectInputStream(threadClient.getInputStream());
							//Get the file to send to the other client
							ClientRequestAndResponseInformation clientRequestAndResponseInformation= (ClientRequestAndResponseInformation) in.readObject();
							//Print the file name of the file requested
							if(map.get(clientRequestAndResponseInformation.messageId)!=null){
								
								if(clientRequestAndResponseInformation.searchQuery && clientRequestAndResponseInformation.tTl>0){
								clientRequestAndResponseInformation.tTl--;
								
								map.put(clientRequestAndResponseInformation.messageId,clientRequestAndResponseInformation);
								
									for(int otherClientPort:otherClients){
										connectToClientforFileInformation(otherClientPort,clientRequestAndResponseInformation);
									}
								}
							}else{
								DataOutputStream out= new DataOutputStream(threadClient.getOutputStream());
								out.writeUTF("Hey I saw that");
								
							}
							
							//storeTheObject();
							//sendTheObjectToOtherClients();
							
							
							/*
							System.out.println("File NAme" + fileName);
							//Initalize file object and write in the outputstream
							File myFile = new File(fileName);
							System.out.println(myFile.length());
							byte[] mybytearray = new byte[(int) myFile.length()];
							BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
							bis.read(mybytearray, 0, mybytearray.length);
							
							OutputStream os = threadClient.getOutputStream();
							os.write(mybytearray, 0, mybytearray.length);
							os.flush();
							in.close();*/
							//Get the time difference from the initialization of the thread till now
							long end = System.currentTimeMillis( );
							long diff = end - start;
							System.out.println("Difference is : " + diff + " "+ start + "  "+ end);
				
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
	public static void connectToClientforFileInformation(int serverPort, ClientRequestAndResponseInformation clientRequestAndResponseInformation){
		
		try{
			Socket client = new Socket("localhost",serverPort);
		    
			OutputStream outToServer = client.getOutputStream();
		    ObjectOutputStream out = new ObjectOutputStream(outToServer);
			out.writeObject(clientRequestAndResponseInformation);
			while(true){
				DataInputStream in = new DataInputStream(client.getInputStream());
				System.out.println(in.readUTF());
				
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
		
		String query;
		int ttl=3;
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
					//set the object to be send 
					clientRequestAndResponseInformation.setQuery(port,ttl,query,uuid);
					//store the object in the map
					map.put(uuid,clientRequestAndResponseInformation);
					connectToClientforFileInformation(otherClientPort,clientRequestAndResponseInformation);
				
				}
			}
			
			
		}
		System.exit(0);
		
	}
}
