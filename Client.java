import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;
import javafx.application.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.*;
import javafx.application.Application.Parameters;
import javafx.stage.*;
import javafx.geometry.*;
import javafx.event.*;
import javafx.scene.text.*;
import javafx.scene.image.*;

public class Client extends Application
{
	private Map<String, String> users;
	private GridPane gridPane;
	private VBox userPane;
	private TextArea t1;
	private TextArea t2;
	private TextArea t3;
	private Label smajlikLabel;
	private Label activeUserLabel;
	private Button clearHistoryUser;
	private Button clearHistoryServer;
	private Button smajlikStastny;
	private Button smajlikSmutny;
	private Button logout;
	private Button send;
	private OutputStream wr; 
	private InputStream rd;
	private Socket socket;
	private String activeUser;
	private String sprava;
	private String meno;
	private boolean stop;
	private Stage primaryStage;
	private Image smajlik1, smajlik2, smajlik3;
	
	public static void main(String[] args)
	{
		 Application.launch(args);
	}
	
	@Override
	public void start(Stage stage)
	{
		activeUser = "";
		primaryStage = stage;
		final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();
        meno = !parameters.isEmpty() ? parameters.get(0) : "Anonnym";
		
		Image smajlik1 = new Image(getClass().getResourceAsStream("smiling.png"));
		Image smajlik2 = new Image(getClass().getResourceAsStream("sad.png"));
			
		users = new HashMap<>();
		
		createConnection();
		posliSpravu(meno + ":" + "Server:");
		prijmiSpravu();
		spracujZoznamUzivatelov(sprava);
		System.out.println("Spojenie so serverom bolo uspesne.");
		
		//Creating a Grid Pane 
		gridPane = new GridPane();    
		userPane = new VBox();  

		//Setting size for the pane 
		gridPane.setMinSize(640, 480); 

		//Setting the padding    
		gridPane.setPadding(new Insets(10, 10, 10, 10));  
		userPane.setPadding(new Insets(10, 10, 10, 10));  

		//Setting the vertical and horizontal gaps between the columns 
		gridPane.setVgap(5); 
		gridPane.setHgap(5); 
		//userPane.setVgap(5); 
		//userPane.setHgap(5); 

		//Setting the Grid alignment 
		gridPane.setAlignment(Pos.CENTER); 
		//userPane.setAlignment(Pos.CENTER); 

		//----------------------------------------------	
	
		Iterator<String> keyIterator = users.keySet().iterator();
		String name = "";
		
		while (keyIterator.hasNext())
		{
			name = keyIterator.next();
			Button b = new Button(name);
			b.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					smajlikLabel.setGraphic(null);
					users.put(activeUser, t1.getText());
					activeUser = b.getText();
					Platform.runLater(new Runnable() { 
						public void run() { 
							activeUserLabel.setText(activeUser);
						} 
					});
					t1.setText(users.get(activeUser));
					primaryStage.setTitle("Chat - " + meno + " - " + activeUser);
			}});
			userPane.getChildren().add(b);
		}
		gridPane.add(userPane, 0, 1, 1, 2);
		
		logout = new Button("Log out");
		logout.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					koniec();
			}});
		gridPane.add(logout, 0, 4);
		
		activeUserLabel = new Label("");
		activeUserLabel.setMinWidth(100);
		gridPane.add(activeUserLabel, 1, 0);
		
		smajlikLabel = new Label("");
		smajlikLabel.setMinWidth(70);
		smajlikLabel.setMinHeight(40);
		gridPane.add(smajlikLabel, 2, 0);
		
		clearHistoryUser = new Button("Zmaz historiu.");
		gridPane.add(clearHistoryUser, 3, 0);
		
		clearHistoryServer = new Button("Zmaz historiu.");
		gridPane.add(clearHistoryServer, 4, 0);
						
		t1 = new TextArea();
		t1.setEditable(false);
		t1.setPrefColumnCount(40);
		t1.setPrefRowCount(25);
		gridPane.add(t1, 1, 1, 3, 1);
		
		t2 = new TextArea();
		t2.setPrefColumnCount(40);
		t2.setPrefRowCount(5);
		gridPane.add(t2, 1, 2, 3, 2);
		
		t3 = new TextArea();
		t3.setEditable(false);
		t3.setPrefColumnCount(15);
		t3.setPrefRowCount(25);
		gridPane.add(t3, 4, 1, 2, 1);
		
		clearHistoryUser.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					if (activeUser.equals("")) return;
					users.put(activeUser, "");
					t1.setText("");
			}});
		
		clearHistoryServer.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					t3.setText("");
			}});
		
		smajlikStastny = new Button("");
		smajlikStastny.setGraphic(new ImageView(smajlik1));
		smajlikStastny.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					if (activeUser.equals("")) return;
					posliSpravu(meno + ":" + activeUser + ":" + ":)");
			}});
		gridPane.add(smajlikStastny, 4, 2);
		
		smajlikSmutny = new Button("");
		smajlikSmutny.setGraphic(new ImageView(smajlik2));
		smajlikSmutny.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					if (activeUser.equals("")) return;
					posliSpravu(meno + ":" + activeUser + ":" + ":(");
			}});
		gridPane.add(smajlikSmutny, 5, 2);
		
		send = new Button("Send");
		send.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					if (activeUser.equals("")) return;
					String msg = t2.getText();
					users.put(activeUser, users.get(activeUser) + meno + ": " + msg + "\n");
					t1.appendText(meno + ": " + msg + "\n");
					t2.setText("");
					posliSpravu(meno + ":" + activeUser + ":" + msg);
			}});
		gridPane.add(send, 4, 3);
				
		//Creating a scene object 
		Scene scene = new Scene(gridPane); 
		
		//Setting title to the Stage 
		primaryStage.setTitle("Chat - " + meno); 
		 
		//Adding scene to the stage 
		primaryStage.setScene(scene);  

		//Displaying the contents of the stage 
		primaryStage.show(); 
			
		Thread th1 = new Thread()
		{
			public void run()
			{
				while (!stop && socket != null)
				{
					prijmiSpravu();
					String[] msg = parseSprava(sprava);
					if (msg[0].equals("Server"))
					{
						String[] msg2 = parseSprava(msg[1]);
						t3.appendText(msg[0] + ":  " + msg2[1] + "\n");
						if (users.get(msg2[0]) == null)
						{
							addUser(msg2[0]);
						}
						else
						{
							removeUser(msg2[0]);
						}
					}
					else
					{
						if (msg[0].equals(activeUser))
						{
							if (msg[1].equals(":)"))
							{
								Platform.runLater(new Runnable() { 
									public void run() { 
										smajlikLabel.setGraphic(new ImageView(smajlik1));
									} 
								});
							}
							else if (msg[1].equals(":("))
							{
								Platform.runLater(new Runnable() { 
									public void run() { 
										smajlikLabel.setGraphic(new ImageView(smajlik2));
									} 
								});
							}
							else
							{
								Platform.runLater(new Runnable() { 
									public void run() { 
										smajlikLabel.setGraphic(null);
									} 
								});
							}
						}
						users.put(msg[0], users.get(msg[0]) + msg[0] + ":  " + msg[1] + "\n");
						updateTextArea();
					}
					try {sleep(100);} catch (InterruptedException e) {e.printStackTrace();} // docasne riesenie
				}
			}
		};
		th1.start();
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
				koniec();
            }
		});
	}
	
	private void koniec()
	{
		posliSpravu(meno + ":" + "Server:");
		stop = true;
        Platform.exit();
        System.exit(0);
	}
	
	public void addUser(String name)
	{
		Button b = new Button(name);
			b.setOnAction(new EventHandler<ActionEvent>() {
				@Override public void handle(ActionEvent e) {
					smajlikLabel.setGraphic(null);
					users.put(activeUser, t1.getText());
					activeUser = b.getText();
					
					Platform.runLater(new Runnable() { 
						public void run() { 
							activeUserLabel.setText(activeUser);
						} 
					});
					
					t1.setText(users.get(activeUser));
					primaryStage.setTitle("Chat - " + meno + " - " + activeUser);
			}});
		Platform.runLater(new Runnable() { 
			public void run() { 
				userPane.getChildren().add(b);
			} 
		});
		users.put(name, "");
	}
	
	public void removeUser(String name)
	{
		if (name.equals(activeUser))
		{
			activeUser = "";
		}
		List<Node> childrens = userPane.getChildren();
		for(Node node : childrens) {
			Button b = (Button) node; 
			if(b.getText().equals(name)) {
				Platform.runLater(new Runnable() { 
					public void run() { 
						userPane.getChildren().remove(b);
					} 
				});
				users.remove(name);
				break;
			}
		}
	}
	
	public void updateTextArea()
	{
		t1.setText(users.get(activeUser));
	}
	
	public String[] parseSprava(String sp)
	{
		String[] msg = new String[2];
		String buff = "";
		int i = 0;
		while (sp.charAt(i) != ':')
		{
			buff += sp.charAt(i);
			i++;
		}
		i++;
		msg[0] = buff;
		buff = "";
		while (i != sp.length())
		{
			buff += sp.charAt(i);
			i++;
		}
		msg[1] = buff;
		buff = "";
		return msg;
	}
	
	private void spracujZoznamUzivatelov(String s)
	{
		System.out.println(s);
		int i = 0;
		while (i < s.length() && s.charAt(i) != ':')
		{
			i += 1;
		}
		i += 1;
		while (i < s.length())
		{
			String name = "";
			while (i < s.length() && s.charAt(i) != ';')
			{
				name += s.charAt(i);
				i += 1;
			}
			if (!name.equals(""))
			{
				users.put(name, "");
			}
			i += 1;
		}
	}
	
	private void createConnection()
	{
		try {
		    
		    try {
		    	// first try to connect to other party, if it is already listening
			    socket = new Socket("localhost", 1234);
			    System.out.println("Vytvoreny socket pre odosielanie");
		    } catch (ConnectException e) {
				e.printStackTrace();
			}
		    wr = socket.getOutputStream();
            rd = socket.getInputStream();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean prijmiSpravu()
	{
		try {
			int nbts = rd.read() + (rd.read() << 8);
			byte bts[] = new byte[nbts];
			int i = 0; // how many bytes did we read so far
			do {
				int j = rd.read(bts, i, bts.length - i);
				if (j > 0) i += j;
				else break;
			} while (i < bts.length);
			sprava = new String(bts);
			//t1.append(sprava + "\n");
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public void posliSpravu(String message)
	{
		byte bts[] = message.getBytes();
		    try {
			    wr.write(bts.length & 255);
			    wr.write(bts.length >> 8);
			    wr.write(bts, 0, bts.length);
			    wr.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		System.out.println("Poslal som spravu.");
	}
	
}


