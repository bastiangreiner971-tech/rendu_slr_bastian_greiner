package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;

public class RemoteFactoryPersistenceManager extends AbstractCanvasPersistenceManager {

    public RemoteFactoryPersistenceManager(CanvasChooser canvasChooser) {
		super(canvasChooser);
	}
    public RemoteFactoryPersistenceManager() {
		this(null);
    }

	private static final int PORT = 5000;
	
	@Override
	public Canvas read(String canvasId) throws IOException {
		
		try (Socket socket = new Socket("localhost", PORT);
        		ObjectOutputStream  out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream  in = new ObjectInputStream(socket.getInputStream());
        		) {


            // Send the message to the server
            out.writeObject(canvasId);
            out.flush();
            
            // Read the server response
            Canvas response = (Canvas) in.readObject();
            System.out.println(response);
            return response;



        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
		
		
	}
	
	@Override
	public void persist(Canvas canvasModel) throws IOException {
		try (Socket socket = new Socket("localhost", PORT);
        		ObjectOutputStream  out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream  in = new ObjectInputStream(socket.getInputStream());
        		) {


            // Send the message to the server
            out.writeObject(canvasModel);
            out.flush();
            
            // Read the server response
            String response =(String) in.readObject();
            System.out.println(response);

        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
	}
	
	@Override
    public boolean delete(Canvas canvasModel) throws IOException {
		return true;
	}
	
	
	
    /*public static void main(String[] args) {

        try (Socket socket = new Socket("localhost", PORT);
        		ObjectInputStream  in = new ObjectInputStream(socket.getInputStream());
        		ObjectOutputStream  out = new ObjectOutputStream(socket.getOutputStream())) {

            String message = "my message";

            // Send the message to the server
            out.writeObject(message);
            out.flush();
            
            // Read the server response
            Object response = in.readObject();
            
            System.out.println(response);


        } catch (IOException |ClassNotFoundException e) {
            e.printStackTrace();
        }
    }*/
}