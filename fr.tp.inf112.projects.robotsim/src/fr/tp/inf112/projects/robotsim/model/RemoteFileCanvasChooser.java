package fr.tp.inf112.projects.robotsim.model;

import java.awt.Component;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import javax.swing.JOptionPane;

import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;

public class RemoteFileCanvasChooser extends FileCanvasChooser{
    private static final int PORT = 5000;

	public RemoteFileCanvasChooser(Component viewer, String fileExtension, String documentTypeLabel) {
		super(viewer, fileExtension, documentTypeLabel);
	}
	public RemoteFileCanvasChooser(String fileExtension, String documentTypeLabel) {
		super(null, fileExtension, documentTypeLabel);
	}
	public RemoteFileCanvasChooser() {
		this("factory", "Puck Factory");
	}
	protected String browseCanvases(final boolean open) throws IOException {
		if (open) {
			try (Socket socket = new Socket("localhost", PORT);
					ObjectOutputStream  out = new ObjectOutputStream(socket.getOutputStream());
					ObjectInputStream  in = new ObjectInputStream(socket.getInputStream());
					){
				// Send the message to the server
	            out.writeObject("LIST");
	            out.flush();
	            
	            // Read the server response
	            List<String> fileNamesList = (List<String>) in.readObject();
	            String[] fileNames = fileNamesList.toArray(new String[0]);
	            String selectedFile = (String) JOptionPane.showInputDialog(
	                    null,
	                    "Choose a model:",
	                    "Open model",
	                    JOptionPane.QUESTION_MESSAGE,
	                    null,
	                    fileNames,
	                    fileNames.length > 0 ? fileNames[0] : null
	                );

	                return selectedFile;
	        }catch (ClassNotFoundException e) {
	            throw new IOException(e);
	        }
		}else {
			return JOptionPane.showInputDialog(
					null,
					"Enter the model file name:",
					"Save model",
					JOptionPane.QUESTION_MESSAGE
					);
		}	
	} 
}
