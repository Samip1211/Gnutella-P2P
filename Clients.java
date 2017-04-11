import java.util.*;
import java.net.*;
import java.io.*;

//Object describing the client which is stored in server
/*
class ClientRequestAndResponseInformation implements Serializable{
	int port; // Port of the client
	int getOtherClient; //0 means do not get other client address whereas 1 means to get other client address
	List<String> filesPresent;
	String getFile;
	public ClientRequestAndResponseInformation(int id){
		this.port= id;
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
	
}*/
public class Clients {
	static int port;
	static String fileToGet;
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
							String fileName= (String) in.readObject();
							//Print the file name of the file requested
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
							in.close();
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
	
	//Call this method when u want to connect to the other client
	public  static  void connectToClient(int serverPort){
		
		
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
		List<Integer> otherClients = new ArrayList<Integer>();
		
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
		
		new Thread(){
			public void run(){
				makeServer(port);
				
			}
		}.start();
		String query;
		
		//Get the user query
		while(true){
			Scanner reader = new Scanner(System.in);  // Reading from System.in
			System.out.println("Enter the query: ");
			query = reader.nextLine();
			
			if(query.equals("exit")){
				break;
			}
		}
		System.exit(0);
		
	}
}