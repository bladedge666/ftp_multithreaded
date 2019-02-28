//Updated Project 2
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
//import com.google.common.collect.Multimap;
//import com.google.common.collect.ArrayListMultimap;
import java.util.concurrent.locks.ReentrantReadWriteLock;;

/**
 * This is the FTP server for the project. The main method works as the driver
 * and calls the start method which boots up the server and allows it to serve
 * requests from the client.
 */
public class myftpserver{

	//remove these to in the end to make take command line arguements
//  public static final int NPORT = 4445;
//  public static final int TPORT = 5555;

  private static final Scanner scan = new Scanner(System.in);

  public static void main(String args[]) throws IOException {
	  int nport=Integer.parseInt(args[0]);
	  int tport=Integer.parseInt(args[1]);
	  new NormalPortThread(nport);
	  new TerminatePortThread(tport);

  } // end main


  public void closeResources() throws IOException {
    scan.close();
  }

}

//class to create terminal port
class TerminatePortThread implements Runnable{
	public static ServerSocket terminateSocket=null;
	public static Socket terminatingClient=null;
	Thread terminateThread;
	public static int TPORT;
	
	TerminatePortThread(int tport){
		terminateThread=new Thread(this);
		System.out.println("Terminate port thread created...");
		TPORT=tport;
		terminateThread.start();  //starts new thread
	}
	
	public void run() {
	
		try {
			terminateSocket = new ServerSocket(TPORT);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
		
	    while (true) {
	    	try {
	    		terminatingClient = terminateSocket.accept();
	    		
	    		DataInputStream input=new DataInputStream(new BufferedInputStream(terminatingClient.getInputStream()));
	    		DataOutputStream output = new DataOutputStream(terminatingClient.getOutputStream());
	    	  	String commandID = input.readUTF();
	    	  	if(NewThread.runningCommands.contains(commandID)) {  
	    	  		NewThread.runningCommands.remove(commandID);	//removes the commandID from the list 
					output.writeUTF("Process terminated");
	    	  	}else {
	    	  		output.writeUTF("Process not found! or It might have been executed already! ");
	    	  	}
				terminatingClient.close();
	        } catch (Exception e) {
	            System.out.println("Error: " + e);
	        }
	    }
	}
}

//class to create normal port
class NormalPortThread implements Runnable{
	public static ServerSocket serverSocket=null;
	public static Socket client=null;
	Thread normalThread;
	public static int NPORT;
	  
	NormalPortThread(int nport) {
		normalThread=new Thread(this);
		System.out.println("Normal port thread created...");
		NPORT=nport;
		normalThread.start(); 
	}
	
	public void run() {
		try {
			serverSocket = new ServerSocket(NPORT);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
			
	      while (true) {
	          try {
	              client = serverSocket.accept();
	          } catch (Exception e) {
	              System.out.println("Error: " + e);
	              try {
					serverSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	          }

	  		new NewThread(client);	//creates new thread for every client connection
	      }
	  }
}

//class for creating thread on every client connection
class NewThread implements Runnable{
	
	public static Map<String,List<String>> FileManager = new HashMap<>();  //keeps track of file names and commands running on them
	public static ArrayList<String> runningCommands=new ArrayList<String>(); //keeps the track of command IDs, helps in termination of commands
	private final static String FILE_SEP = System.getProperty("file.separator");
	private final static int BUFFER_SIZE = 1000;
	public static int clientCount=0;
	public static int commandID=0;  //increases on every get and put command  
	
	Thread t;
	Socket client;
	NewThread(Socket socket) {
		clientCount++;
		t = new Thread(this,"client "+clientCount);
		client=socket;
	
		System.out.println("New thread: " + t);
		t.start(); // Start the thread
	}
	
	public void run() {
		
		try {

			File currentDir, changeDir, makeDir, delFile;
			int currentThreadCommandID;
			
			DataInputStream input=new DataInputStream(new BufferedInputStream(client.getInputStream()));
			DataOutputStream output = new DataOutputStream(client.getOutputStream());
			ObjectOutputStream objOutput = new ObjectOutputStream(client.getOutputStream());
			
			System.out.println("New client connected " +Thread.currentThread().getName());
			System.out.println("Connection estblished!");
			String presentWD = System.getProperty("user.dir");
			List<String> commands=new ArrayList<>(); //helps in updating the FileManager hashmap 
		
			ReentrantReadWriteLock lock=new ReentrantReadWriteLock(true);
			ReentrantReadWriteLock.ReadLock getLock=lock.readLock(); //readLock
			ReentrantReadWriteLock.WriteLock putLock=lock.writeLock(); //writeLock

			String command[] = null;
			System.out.println("Waiting for a command...");
			
			do {

				String response = input.readUTF();
				command = response.split("\\s+");
				System.out.println(command[0]);
			    switch (command[0]) {
			        case "get":
			          // check if a filename or path is provided after the get command
			          if (command.length == 2 || command.length==3) {
			            File sendFile = new File(presentWD + FILE_SEP + command[1]);
			            
			            output.writeUTF(String.valueOf(sendFile.exists()));
			            System.out.println("FILE---> " + sendFile.getTotalSpace());
			            
			            if (sendFile.exists()) {
			            	currentThreadCommandID=++commandID;
			            	runningCommands.add(""+currentThreadCommandID);
			            	output.writeUTF(""+ currentThreadCommandID);
			            	if (!FileManager.containsKey(command[1])) { //checks if file name exists in FileManager map 
			            		commands.add("get");
			            		FileManager.put(command[1],commands);
				        		get(command[1],presentWD,currentThreadCommandID);
				        		commands=FileManager.get(command[1]);
				        		commands.remove("get");
				        		if(commands.isEmpty()) {
				        			FileManager.remove(command[1]);
				        		}
				        		else {
				        			FileManager.replace(command[1], commands);
				        		}
				        	  }else {
				        		  commands=FileManager.get(command[1]);
				        	  if (commands.contains("put")) {  //if put is waiting for running on the current file go ahead and wait for a lock
				        		  try {
				        			  getLock.lock();
				        			  commands=FileManager.get(command[1]);
				        			  commands.add("get");
				        			  FileManager.put(command[1],commands);
				        			  get(command[1],presentWD,currentThreadCommandID);
				        			  commands=FileManager.get(command[1]);
				        			  commands.remove("get");
				        			  if(commands.isEmpty()) {
						        			FileManager.remove(command[1]);
						        		}
						        		else {
						        			FileManager.replace(command[1], commands);
						        		}
				        		  }
				        		  catch(Exception e) {
				        			  System.out.println(e);
				        		  }
				        		  finally {
				        			  getLock.unlock();
				        		  }
				        		}
				        	  else {  //if get is running on the file then go ahead
				        		  commands=FileManager.get(command[1]);
			        			  commands.add("get");
			        			  FileManager.put(command[1],commands);
			        			  get(command[1],presentWD,currentThreadCommandID);
			        			  commands=FileManager.get(command[1]);
			        			  commands.remove("get");
			        			  if(commands.isEmpty()) {
					        			FileManager.remove(command[1]);
					        		}
					        		else {
					        			FileManager.replace(command[1], commands);
					        		}
				        	  }
				        	}
			            } 
			          }

			          // If no file is specified as the argument
			          else {
			            output.writeUTF("You must specify a path after a get command.");
			          }

			          break;

			        case "put":
			        	
			          if ((command.length == 2 || command.length==3)&& input.readUTF().equals("true")) {

			        	  currentThreadCommandID=++commandID;
			        	  runningCommands.add(""+currentThreadCommandID);
			        	  output.writeUTF(""+ currentThreadCommandID);
			        		  
			        		  if(!FileManager.containsKey(command[1])) {  //checks if file is not in the FileManager 
			        			  
			        			  commands.add("put");
			        			  FileManager.put(command[1],commands);
			        			  put(command[1],presentWD,currentThreadCommandID);
			        			  commands=FileManager.get(command[1]);
			        			  commands.remove("put");
			        			  if(commands.isEmpty()) {
			        				  FileManager.remove(command[1]);
			        			  }else {
			        				  FileManager.put(command[1],commands);
			        			  }  
				        	  }else {
				        		  commands=FileManager.get(command[1]);
				        		  if(commands.contains("get") && !commands.contains("put")) { //if FileManager does not contain put
					        		  commands.add("put");
					        		  FileManager.put(command[1],commands);
			
					        		  try {
					        			  putLock.lock();
					        			  put(command[1],presentWD,currentThreadCommandID);
					        			  commands=FileManager.get(command[1]);
					        			  commands.remove("put");
					        			  if(commands.isEmpty()) {
							        			FileManager.remove(command[1]);
							        		}
							        		else {
							        			FileManager.replace(command[1], commands);
							        		}
					        		  }
					        		  catch(Exception e) {
					        			  System.out.println(e);
					        		  }
					        		  finally {
					        			  putLock.unlock();
					        		  }  
					        	  }		
				        		  else {
				        			  try {
					        			  putLock.lock();
					        			  commands=FileManager.get(command[1]);
					        			  commands.add("put");
					        			  FileManager.put(command[1],commands);
					        			  put(command[1],presentWD,currentThreadCommandID);
					        			  commands=FileManager.get(command[1]);
					        			  commands.remove("put");
					        			  if(commands.isEmpty()) {
							        			FileManager.remove(command[1]);
							        		}
							        		else {
							        			FileManager.replace(command[1], commands);
							        		}
					        		  }
					        		  catch(Exception e) {
					        			  System.out.println(e);
					        		  }
					        		  finally {
					        			  putLock.unlock();
					        		  }
				        		  }
				        	  } 
//			        	  }else {
//			        		  output.writeUTF("File does not exist!");
//			        	  }		            
			          }

			          else {
			            output.writeUTF("You must specify a path after a put command.");
			          }
			          break;

			        case "delete":
			          if (command.length >= 2) {
			            delFile = new File(presentWD + FILE_SEP + command[1]);
			            if (delFile.exists()) {
			              delFile.delete();
			              output.writeUTF(command[1] + " successfully deleted.");
			            } else {
			              output.writeUTF("File does not exist!");
			            }
			          }

			          else {
			            output.writeUTF("You must specify a file after a delete command.");
			          }
			          break; 

			        case "ls":
			          currentDir = new File(presentWD);
			          objOutput.writeObject(currentDir.list());
			          break;

			        case "cd..":
			          currentDir = new File(presentWD);
			          
			          if(currentDir.getAbsoluteFile().getParent()!=null) {
			        	  presentWD = currentDir.getAbsoluteFile().getParent();
				          System.out.println(presentWD);
				          output.writeUTF("Directory changed to " + presentWD);
			          }
			          else {
			        	  output.writeUTF("No parent directory exists!");
			          }
			          
			          break;

			        case "cd":

			          if (command.length <= 1) {
			            output.writeUTF("You must specify a path after a cd command.");
			            break;
			          }

			          if (command[1].equals("..")) {

			        	  currentDir = new File(presentWD);
				          if(currentDir.getAbsoluteFile().getParent()!=null) {
				        	  presentWD = currentDir.getAbsoluteFile().getParent();
					          System.out.println(presentWD);
					          output.writeUTF("Directory changed to " + presentWD);
				          }
				          else {
				        	  output.writeUTF("No parent directory exists!");
				          }

			          } 

			          // when the second param is anything other than ".."
			          else {
			            changeDir = new File(presentWD + FILE_SEP + command[1]);

			            if (changeDir.exists()) {
			              //System.setProperty("user.dir", changeDir.getAbsoluteFile().getPath());			              
			            	presentWD = changeDir.getAbsolutePath();
			            	output.writeUTF("Directory changed to " + presentWD);
			            } else {
			            	output.writeUTF("No such directory exists!");
			            }
			          }

			          break;

			        case "mkdir":
			          if (command.length == 2) {
			            makeDir = new File(presentWD + FILE_SEP + command[1]);
			            makeDir.mkdir();
			            output.writeUTF("New directory named " + command[1] + " created.");
			          }

			          else {
			            output.writeUTF("You must specify a path after a get command.");
			          }
			          break;

			        case "pwd":
			          output.writeUTF(presentWD);
			          break;
			          
			        case "terminate":
			        	break;

			        case "quit":
			          client.close();
			          break;

			        default:
			          System.out.println("Invalid command!");
			          output.writeUTF("Invalid Command!");
			          break;
			        }

			      } while (!command[0].equals("quit"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}	
	
	public void get(String fileName, String presentWD,int commandID) {
		if(runningCommands.contains(""+commandID)) {
			try {
				System.out.println("get command received for "+fileName+"        commandID "+commandID);
				File sendFile = new File(presentWD + FILE_SEP + fileName);
				FileInputStream fileInStream = new FileInputStream(sendFile);
				
				DataInputStream getInput=new DataInputStream(new BufferedInputStream(client.getInputStream()));
				DataOutputStream getOutput = new DataOutputStream(client.getOutputStream());
		        System.out.println(fileInStream.available() + " bytes!");
		        
		        byte[] buffer = new byte[(int) sendFile.length()];
		        System.out.println("Get buffer length: " + buffer.length);
		        DataInputStream dis = new DataInputStream(fileInStream);

		        boolean terminated=false;
		        String getFilePath=getInput.readUTF();
		        String status;
		        System.out.println("Getfilepath "+getFilePath);
		        int pointer=0;
		        String requestStatus;
		        if(NewThread.runningCommands.contains(""+commandID)) {
		        	getOutput.writeLong(buffer.length); // send the size of the file
		        	int fileSize=buffer.length;
		        	int readChars;

		        		for(;pointer<fileSize;pointer+=readChars) {
		        			if((fileSize-pointer)>1000) {
		        				readChars=1000;
		        			}else {
		        				readChars=1;
		        			}
		        			dis.read(buffer, pointer, readChars); 
		        			getOutput.write(buffer, pointer, readChars); 
		        			if(readChars%1000==0) {
			        			if(!NewThread.runningCommands.contains(""+commandID)) { //checks if command is still in the arraylast
				        				terminated=true;
				        				break;
				        			}
			        	        }
		        			}
		        			fileInStream.close();
		        			dis.close();
		        		}
		        if(terminated==true) {
		        	File sendedFile=new File(getFilePath);
		        	if(sendedFile.exists()) {
		        		sendedFile.delete();
		        	}
		        	System.out.println("Command for commandID "+ commandID +" terminated.");
		        }else{
		        	runningCommands.remove(""+commandID);
					System.out.println("Command for commandID "+ commandID +" completed.");
		        }

			}
			catch(Exception e){
				e.printStackTrace(System.out);
			}
		}else {
			System.out.println("Command for commandID "+ commandID +" terminated.");
		}
		
	}
	
	public void put(String fileName, String presentWD,int commandID) {
		boolean terminated=false;

		if(NewThread.runningCommands.contains(""+commandID)) {
			try {
				DataInputStream input=new DataInputStream(new BufferedInputStream(client.getInputStream()));
				DataOutputStream output = new DataOutputStream(client.getOutputStream());
				System.out.println("put command received for "+fileName+"        commandID "+commandID);

				long size=input.readLong();
		        int bytesRead = 0; 
		        byte[] putByteArray = new byte[BUFFER_SIZE];
		        FileOutputStream fileOutStream = new FileOutputStream(presentWD + FILE_SEP + fileName);

		        while (size > 0 && (bytesRead = input.read(putByteArray, 0, (int) Math.min(putByteArray.length, size))) != -1) {
		        	fileOutStream.write(putByteArray, 0, bytesRead);
		        	size -= bytesRead;

        			if(bytesRead%1000==0) {
	        			if(!NewThread.runningCommands.contains(""+commandID)) {
	        				terminated=true;
	        				output.writeUTF("terminate"); //sends to client in order to indicate termination
	        				System.out.println("Command for commandID "+ commandID +" terminated");
	        				break;
	        	        }else {
	        	        	output.writeUTF("continue");
	        	        }
        			}
		        }		        
		        fileOutStream.close();
				if(terminated==true) {
					File delFile=new File(presentWD + FILE_SEP + fileName);
					if(delFile.exists()) {
						delFile.delete();
						System.out.println(delFile.exists());
						System.out.println("File deleted");
					}
					System.out.println("Command for commandID "+ commandID +" terminated.");
				}else {
					runningCommands.remove(""+commandID);
					System.out.println("Command for commandID "+ commandID +" completed.");
				}
			}
			catch(Exception e) {
				System.out.println(e);
			}	
		}else {
			System.out.println("Command for commandID "+ commandID +" terminated.");
		}
	}
}
