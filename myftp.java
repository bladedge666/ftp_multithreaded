//updated Project 2

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * This is the FTP client for the project. On executing this file, you are given
 * a "myftp>" prompt where you can type the available commands.
 */
public class myftp {

  private final static String FILE_SEP = System.getProperty("file.separator");
  private final static int BUFFER_SIZE = 1024;
  private static final Scanner scan = new Scanner(System.in);
  public static String commandID;
  
  public static void main(String[] args) throws IOException, ClassNotFoundException {

    int port = Integer.parseInt(args[0]);
    String machineName = args[1];
    Socket server = new Socket(machineName, port);
    String splitCommand[]=null;
    DataOutputStream output = new DataOutputStream(server.getOutputStream());
    DataInputStream input = new DataInputStream(new BufferedInputStream(server.getInputStream()));
    ObjectInputStream objInput = new ObjectInputStream(server.getInputStream());
    //ObjectOutputStream objOutput = new ObjectOutputStream(server.getOutputStream());

    //FileOutputStream fileOutStream;
    //FileInputStream fileInStream;
    

    boolean quit = false;
    while (!quit) {
      output.flush();
      // outStream.flush();
      System.out.print("myftp> ");
      String command = scan.nextLine();
      output.writeUTF(command);
      int cmdLength = command.split(" ").length;
      splitCommand=command.split("\\s+");
     

      if (command.equals("ls")) {
        String listOfFiles[] = (String[]) objInput.readObject();
        for (int i = 0; i < listOfFiles.length; i++) {
          System.out.printf("%s \t", listOfFiles[i]);
        }
        System.out.println();

      }

      else if (command.equals("quit")) {
        server.close();
        quit = true;
      }

      else if ((cmdLength == 2 || cmdLength==3) && splitCommand[0].equals("get")) {
    	  if(command.endsWith("&")) {
    		  new getAndPutWithAmpersand(server,splitCommand[0],input,splitCommand[1],output);
    	  }else {
        	  get(input,splitCommand[1],output);
    	  }
    	  try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      }

      else if ((cmdLength == 2 || cmdLength==3 ) && splitCommand[0].equals("put")) {
    	  if(command.endsWith("&")) {
    		  new getAndPutWithAmpersand(server,splitCommand[0],input,splitCommand[1],output);
    	  }else {
    		  put(output,input,splitCommand[1]);
    	  }try {
  			Thread.sleep(500);
  		} catch (InterruptedException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
    	  
      }
      else if(cmdLength==2 && splitCommand[0].equals("terminate")){
    	  terminate(command);
      }
      else {
        System.out.println(input.readUTF());
      }

    } // end while

  }

  static void put(DataOutputStream output,DataInputStream input,String command) {
	  try {
		  //output.writeUTF(System.getProperty("user.dir"));
		  //System.out.println(System.getProperty("user.dir"));
		  File sendFile = new File(command);
	      output.writeUTF(String.valueOf(sendFile.exists()));
	      commandID=input.readUTF();
	      System.out.println("CommandID "+commandID);
	      if (sendFile.exists()) {
	        FileInputStream fileInStream = new FileInputStream(sendFile.getAbsolutePath());
//	        System.out.println(fileInStream.available());
	        output.writeLong(sendFile.length());
	        byte[] putByteArray = new byte[(int) sendFile.length()];

	        DataInputStream dis = new DataInputStream(fileInStream);
	        boolean terminated=false;
	        int pointer=0,readChars;
	        int fileSize=putByteArray.length;
	        
//	        if(NewThread.runningCommands.contains(commandID)) {
        		for(;pointer<fileSize;pointer+=readChars) {
        			if((fileSize-pointer)>1000) {
        				readChars=1000;
        			}else {
        				readChars=1;
        			}
        			dis.read(putByteArray, pointer, readChars); // readfully is required to read all at once
        			output.write(putByteArray, pointer, readChars); //
        			//System.out.println(readChars+"     "+pointer);
//        			if(readChars%1000==0) {
//	        			if(!NewThread.runningCommands.contains(commandID)) {
//	        				pointer=fileSize;
//	        				terminated=true;
//	        				break;
//	        	        }
//        			}

        		}
//        	}	
//        	else {
//        		terminated=true;
//        	}
	        
	        output.flush();
//	        System.out.println("File successfully uploaded on server!");
	        dis.close();
	        //input.readUTF();
	      } else {
	        System.out.println("File does not exist!");
	      }
	  }
	  catch(Exception e) {
		  System.out.println(e);
	  }
  }
  
  static void get(DataInputStream input, String command,DataOutputStream output) {
	try {
		  if (input.readUTF().equals("true")) {
			  commandID=input.readUTF();
			  System.out.println("Command ID "+commandID);
			  output.writeUTF(System.getProperty("user.dir") + FILE_SEP + command);
	          long size = input.readLong();
	          int bytesRead = 0;
	          byte[] buffer = new byte[BUFFER_SIZE];
//	          System.out.println("Size of get file: " + size);
	          String getFile = command;
//	          System.out.println(getFile);
	          FileOutputStream fileOutStream = new FileOutputStream(System.getProperty("user.dir") + FILE_SEP + getFile);
	          while (size > 0 && (bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
	            fileOutStream.write(buffer, 0, bytesRead);
	            size -= bytesRead;
	          }
	          fileOutStream.close();
	        } else {
//	          System.out.println("File does not exist!");
	        }
	  }
	  catch(Exception e) {
		  System.out.println(e);
	  }   
  }
  
  static void terminate(String command) throws UnknownHostException, IOException {
	  int tport = 5555;
	  String machineName = "localhost";
	  Socket server = new Socket(machineName, tport);
	  //String splitCommand[]=null;
	  DataOutputStream output = new DataOutputStream(server.getOutputStream());
	  DataInputStream input = new DataInputStream(new BufferedInputStream(server.getInputStream()));
	  output.writeUTF(command);
	  System.out.println(input.readUTF());
	  output.close();
	  input.close();
	  server.close();
  }
  
}

class getAndPutWithAmpersand implements Runnable {
	Thread getPutThread;
	String command,fileName;
	DataInputStream input;
	DataOutputStream output;
	//private final static String FILE_SEP = System.getProperty("file.separator");
	getAndPutWithAmpersand(Socket server,String command,DataInputStream input, String fileName, DataOutputStream output){
		this.input=input;
		this.output=output;
		this.command=command;
		this.fileName=fileName;
		getPutThread=new Thread(this);
//		System.out.println("New thread created for get or put operation...");
		//TPORT=tport;
		getPutThread.start();
	}
	
	public void run() {
		if(command.equals("get")) {
			myftp.get(input, fileName,output);
		  }
		else if(command.equals("put")) {
			myftp.put(output, input, fileName);
		}
	}
		//add logic to terminate command 
}