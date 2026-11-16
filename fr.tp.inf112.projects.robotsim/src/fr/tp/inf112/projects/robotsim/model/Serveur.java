package fr.tp.inf112.projects.robotsim.model;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;

public class Serveur {

    private static final int PORT = 5000;
    private static final FactoryPersistenceManager persistenceManager = new FactoryPersistenceManager(new FileCanvasChooser("factory", "Puck Factory"));

    public static void main(String[] args) {
    	try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server waiting for a client...");
            while (true) {
            	try (Socket socket = serverSocket.accept();
	            		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
	            		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
	            		) {
	
	                System.out.println("Client connected.");
	
	                // Read the message sent by the client
	                Object obj = in.readObject();
	                if (obj instanceof String) {
	                    String id = (String) obj;
	                    if(id.equals("LIST")){
	                    	File directory = new File(".");
	
	                    	File[] files = directory.listFiles();
	
	                    	List<String> fileNames = new ArrayList<>();
	
	                    	if (files != null) {
	                    	    for (File file : files) {
	                    	        if (file.isFile()) {
	                    	            fileNames.add(file.getName());
	                    	        }
	                    	    }
	                    	}
	
	                    	out.writeObject(fileNames);
	                    	out.flush();
	                    }else {
		                    Canvas canvas = (Canvas) persistenceManager.read(id);
		                    System.out.println("String reçue : " + id);
		                    // Send the response
		                    out.writeObject(canvas);
		                    out.flush();
	                    }
	
	                } else if (obj instanceof Factory) {
	                	Factory factory = (Factory) obj;
	                    //System.out.println("Objet reçu : "+factory);
	                    persistenceManager.persist(factory);
	                    // Send the response
	                    out.writeObject("ok");
	                    out.flush();
	                }
	            }
            }
        } catch (IOException | ClassNotFoundException e) {
	        e.printStackTrace();
	    }
    }
}
