package components;

/**
 * DasherJ - the main class for the emulator
 * 
 * @author steve
 * 
 * v. 0.9 Try moving to JavaFX
 * v. 0.8 Add resizing/zooming functionality
 * v. 0.7 Eliminate separate blink timer
 * 		  Abstract toolbar (F-keys) into separate FKeyGrid class
 * 		  Add Function Key template loading
 * 		  Add Edit/Paste function
 * v. 0.6 IP in status bar
 *        Fix display of serial port in status bar
 *        --host= option added
 *        Rename screen to terminal
 *        Lock main window size
 * v. 0.5 Add ErPage. CR. ErEOL buttons
 *        Implement session logging as per v.0.4
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import components.Status.ConnectionType;

public class DasherJ extends Application {
	
	private static final int CRT_REFRESH_MS = 50;  // Euro screen refresh rate was 50Hz = 20ms, US was 60Hz = 17ms
	private static final int CRT_BLINK_COUNTER = 500 / CRT_REFRESH_MS;

	private static final double VERSION = 0.9;
	private static final int COPYRIGHT_YEAR = 2015;
	private static final String RELEASE_STATUS = "Alpha";
	private static final String HELP_URL_TEXT = "http://stephenmerrony.co.uk/dg/";
	
	private static final String ICON = "/resources/DGlogoOrange.png";
	
	private static boolean haveConnectHost = false;
	private static String  connectHost;
	private static int	   connectPort;
	
	VBox vboxPane;
	static Status status;
	Clipboard clipboard;
	//KeyboardFocusManager keyFocusManager;
	KeyboardHandler keyHandler;
	static FKeyGrid fkeyGrid;
	static DasherStatusBar statusBar;
	static LocalClient lc;
	static SerialClient sc;
	static TelnetClient tc;
	static BlockingQueue<Byte> fromHostQ, fromKbdQ, logQ;
	static Crt crt;
	static Terminal terminal;
	static File logFile;
	static BufferedWriter logBuffWriter;
	
	static Thread screenThread, localThread, loggingThread;
	

	@Override
	public void start( Stage mainStage ) throws Exception {
		
	 	fromHostQ = new LinkedBlockingQueue<Byte>(); // data from the host
    	fromKbdQ  = new LinkedBlockingQueue<Byte>(); // data from the keyboard (or faked data)
    	logQ      = new LinkedBlockingQueue<Byte>(); // data to be logged
    	
    	status = new Status();
    	
    	terminal = new Terminal( status, fromHostQ, fromKbdQ, logQ );
    	(screenThread = new Thread( terminal )).start();
    	screenThread.setName( "ScreenThread" );
    	
    	// start off in local mode

    	(localThread = new Thread(new LocalClient( fromHostQ, fromKbdQ ))).start();
    	localThread.setName( "LocalThread" );
    	
    	final Parameters params = getParameters();
    	final List<String> parameters = params.getRaw();
    	int argNum = 0;
    	String arg;
    	while ( argNum < parameters.size() && parameters.get( argNum ).startsWith( "--" )) { 
    		arg = parameters.get( argNum );
    		if (arg.equals( "--help" )) {
    			System.err.println( "java -cp DasherJ DasherJ [--help] [--host=<hostname>:<port>]" );
    			System.exit( 0 );
    		}
    		
    		if (arg.startsWith( "--host=" )) parseHost( arg );
    		
    		argNum++;
    	}
    	   	
        // Create and set up the window.
        mainStage.setTitle( "DasherJ Terminal Emulator" );
        //window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		vboxPane = new VBox( 4 );
		Scene scene = new Scene( vboxPane );
        
		clipboard = Clipboard.getSystemClipboard();
		
		MenuBar menuBar = createMenuBar( mainStage );
		//vboxPane.setFillWidth( true );
		//vboxPane.setMinWidth( terminal.visible_cols * BDFfont.CHAR_PIXEL_WIDTH * Crt.DEFAULT_HORIZ_ZOOM );
		vboxPane.getChildren().add( menuBar );
		
		fkeyGrid = new FKeyGrid( status, mainStage );
		vboxPane.getChildren().add( fkeyGrid.grid );
        
        crt = new Crt( terminal );
        crt.canvas.setWidth( terminal.visible_cols * BDFfont.CHAR_PIXEL_WIDTH * Crt.DEFAULT_HORIZ_ZOOM );
        crt.canvas.setHeight( (terminal.visible_lines + 1) * BDFfont.CHAR_PIXEL_HEIGHT * Crt.DEFAULT_VERT_ZOOM );

        crt.canvas.getTransforms().add( new Scale(1,2) );
        
        //crt.canvas.setScaleY( 2.0 );
        // add the crt canvas to the content pane.
        vboxPane.getChildren().add( crt.canvas );
        //crt.canvas.setFocusable(true);
        
        // install our keyboard handler
        keyHandler = new KeyboardHandler( fromKbdQ, status );
        scene.addEventHandler( KeyEvent.ANY, keyHandler );
      
        // we don't want the user randomly farting around with the terminal size..
        // window.setResizable( false );
             
        statusBar = new DasherStatusBar( status );
        vboxPane.getChildren().add( statusBar );
        
        Timeline updateStatusBarTimeline = new Timeline( new KeyFrame( Duration.millis( DasherStatusBar.STATUS_REFRESH_MS ),
        		new EventHandler<ActionEvent>() {
					@Override
					public void handle( ActionEvent ae ) {
						statusBar.updateStatus();
					}
        }));
        updateStatusBarTimeline.setCycleCount( Timeline.INDEFINITE );
        updateStatusBarTimeline.play();
        
        // customise icon
        mainStage.getIcons().add( new Image( DasherJ.class.getResourceAsStream( ICON )));

        if (haveConnectHost) startTelnet( connectHost, connectPort );
        
        /* Euro screen refresh rate was 50Hz = 20ms, US was 60Hz = 17ms */
        Timeline updateCrtTimeline = new Timeline( new KeyFrame( Duration.millis( CRT_REFRESH_MS ), 
        										   				 new EventHandler<ActionEvent>() {
        	public void handle( ActionEvent ae ) {
        			status.blinkCountdown--;
              		if (status.dirty || status.blinkCountdown < 1) {
              			crt.paintCrt();  
              			status.dirty = false;
              		}
              		if (status.blinkCountdown < 1) {
              			terminal.blinkState = !terminal.blinkState;
              			status.blinkCountdown = CRT_BLINK_COUNTER;
              		}
        	}
        }));
        updateCrtTimeline.setCycleCount( Timeline.INDEFINITE );
        updateCrtTimeline.play();
        
        // Display the window.
        mainStage.setScene( scene );
        mainStage.show();
	}
	
	public void getNewSize() {
		ObservableList<Integer> linesInts = FXCollections.observableArrayList( 24, 25, 36, 48, 66 );
		ObservableList<Integer> colsInts = FXCollections.observableArrayList( 80, 81, 120, 132, 135 );
		ObservableList<String> zoomStrings = FXCollections.observableArrayList( "Normal", "Smaller", "Tiny" );
		ComboBox<Integer> linesCombo, colsCombo;
		ComboBox<String> zoomCombo;
		linesCombo = new ComboBox<Integer>( linesInts );
		linesCombo.setValue( 24 );
		colsCombo = new ComboBox<Integer>( colsInts );
		colsCombo.setValue( 80 );
		zoomCombo = new ComboBox<String>( zoomStrings );
		zoomCombo.setValue( "Normal" );
		Dialog<ButtonType> newSizeDialog = new Dialog<ButtonType>();
		newSizeDialog.setTitle( "Resize" );
		newSizeDialog.getDialogPane().getButtonTypes().addAll( ButtonType.CANCEL, ButtonType.APPLY );
		GridPane grid = new GridPane();
		grid.setHgap( 10 );
		grid.setVgap( 10 );
		grid.setPadding( new Insets( 20, 20, 10, 10 ) );
		grid.add( new Label( "Lines" ), 0, 0 );
		grid.add( linesCombo, 1, 0 );
		grid.add( new Label( "Columns" ), 0, 1 );
		grid.add( colsCombo, 1, 1 );
		grid.add( new Label( "Zoom" ), 0, 2 );
		grid.add( zoomCombo, 1, 2 );
		newSizeDialog.getDialogPane().setContent( grid );
		
		Optional<ButtonType> rc = newSizeDialog.showAndWait();

		if (rc.get() == ButtonType.APPLY) {
			int newLines = linesCombo.getValue();
			int newCols = colsCombo.getValue();
			float newHzoom = Crt.DEFAULT_HORIZ_ZOOM, newVzoom = Crt.DEFAULT_VERT_ZOOM;
			switch (zoomCombo.getValue()) {
			case "Normal": // Normal
				break;
			case "Smaller": // Smaller
				newHzoom = 1.0f;  newVzoom = 1.0f;
				break;
			case "Tiny": // Tiny
				newHzoom = 0.75f;  newVzoom = 0.75f;
				break;
			}
			terminal.resize( newLines, newCols );
			crt.setZoom( newHzoom, newVzoom );
			float newWidth, newHeight;
			newWidth = newCols * BDFfont.CHAR_PIXEL_WIDTH * newHzoom;
			newHeight = (newLines + 1) * BDFfont.CHAR_PIXEL_HEIGHT * newVzoom;
	        crt.canvas.setWidth( newWidth );
	        crt.canvas.setHeight( newHeight );
		}
	}
	
  	public void getSerialPort() {
  		
  	    TextInputDialog serialDialog = new TextInputDialog();
  	    serialDialog.setTitle( "DasherJ - Serial Port" );
  	    serialDialog.setContentText( "Port eg. COM1: or /dev/ttyS0 :" );
		TextField port = new TextField( "COM1" );
		Optional<String> rc = serialDialog.showAndWait();
		//System.out.printf( "SCD Result %d\n", rc );
		if (rc.isPresent()) { // OK
			// initialise the serial port handler
			sc = new SerialClient( fromHostQ, fromKbdQ );
			if (sc.open( rc.get() )) {
				status.connection = ConnectionType.SERIAL_CONNECTED;
				status.serialPort = port.getText();
			} else {
				Alert alert = new Alert( AlertType.ERROR);
				alert.setContentText( "Could not open " + rc.get() );
				alert.showAndWait();
				status.connection = ConnectionType.DISCONNECTED;
			}
		}

	}
	
	public void getTargetHost() {
  		
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle( "DasherJ - Remote Host" );
		dialog.getDialogPane().getButtonTypes().addAll( ButtonType.CANCEL, ButtonType.OK );
		GridPane grid = new GridPane();
		grid.setHgap( 10 );
		grid.setVgap( 10 );
		grid.setPadding(  new Insets( 20, 20, 10, 10 ) );		
		TextField host = new TextField();
		TextField port = new TextField( "23" );
		grid.add( new Label( "Host:" ), 0, 0 );
		grid.add( host, 1,  0 );
		grid.add( new Label( "Port:" ), 0, 1 );
		grid.add( port, 1, 1 );
		dialog.getDialogPane().setContent( grid );
		Optional<ButtonType> rc = dialog.showAndWait();
		//System.out.printf( "SCD Result %d\n", rc );
		if (rc.get() == ButtonType.OK) { 
			if (!startTelnet( host.getText(), Integer.parseInt( port.getText() ) )) {
				Alert alert = new Alert( AlertType.ERROR );
				alert.setContentText( "Could not connect to " + host + ':' + port );
				alert.showAndWait();
			}
		}

	}
	
	private static boolean startTelnet( String host, int port ) {
		// initialise the telnet session handler
    	tc = new TelnetClient( fromHostQ, fromKbdQ );
		if (tc.open( host, port )) {
			status.remoteHost = host;
			status.remotePort = "" + port;
			status.connection = ConnectionType.TELNET_CONNECTED;
			return true;
		} else {
			status.connection = ConnectionType.DISCONNECTED;
			return false;
		}
	}
	
	public MenuBar createMenuBar( final Stage mainStage ) {
			
		 //Create the menu bar.
        MenuBar menuBar = new MenuBar();  
        
        // declarations up-front so we can refer to other menus if required
        // To keep the menu structure clear only trivial actions are declared here, more involved ones below
        
        final Menu fileMenu = new Menu( "File" );     
        final MenuItem startLoggingMenuItem = new MenuItem( "Start Logging" );
        final MenuItem stopLoggingMenuItem = new MenuItem( "Stop Logging" );
        final MenuItem exitMenuItem = new MenuItem( "Exit" );
        
        final Menu editMenu = new Menu( "Edit" );
        final MenuItem pasteMenuItem = new MenuItem( "Paste" );
        
        final Menu emulMenu = new Menu( "Emulation" );
        final ToggleGroup emulGroup = new ToggleGroup();
        
        final MenuItem resizeMenuItem = new MenuItem( "Resize" );
               
        final MenuItem selfTestMenuItem = new MenuItem( "Self-Test" );
        
        final MenuItem loadTemplateItem = new MenuItem( "Load Template" );
        
        final Menu serialMenu = new Menu( "Serial" );
        final MenuItem serialConnectMenuItem = new MenuItem( "Connect" );
		final MenuItem serialDisconnectMenuItem = new MenuItem( "Disconnect" );  
        final ToggleGroup baudGroup = new ToggleGroup();
        final RadioMenuItem b300MenuItem = new RadioMenuItem( "300 baud" );
        b300MenuItem.setOnAction( new EventHandler<ActionEvent>() {
        		public void handle( ActionEvent ae ) { sc.baudRate = 300; }
        });
        final RadioMenuItem b1200MenuItem = new RadioMenuItem( "1200 baud" );
        b1200MenuItem.setOnAction( new EventHandler<ActionEvent>() {
        		public void handle( ActionEvent ae ) { sc.baudRate = 1200; }
        });
        final RadioMenuItem b9600MenuItem = new RadioMenuItem( "9600 baud" );
        b9600MenuItem.setOnAction( new EventHandler<ActionEvent>() {
        		public void handle( ActionEvent ae ) { sc.baudRate = 9600; }
        });
        final RadioMenuItem b19200MenuItem = new RadioMenuItem( "19200 baud" );
        b19200MenuItem.setOnAction( new EventHandler<ActionEvent>() {
        		public void handle( ActionEvent ae ) { sc.baudRate = 19200; }
        });
        
        final Menu networkMenu = new Menu( "Network" );
		final MenuItem networkConnectMenuItem = new MenuItem( "Connect" );
		final MenuItem networkDisconnectMenuItem = new MenuItem( "Disconnect" );   
		
        final Menu helpMenu = new Menu( "Help" );
        final MenuItem helpMenuItem = new MenuItem( "Online Help" );
        final MenuItem aboutMenuItem = new MenuItem( "About DasherJ" );
        
        // actually build the menu and add non-trivial Actions
        
        menuBar.getMenus().add( fileMenu ); 
        
        startLoggingMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	public void handle( ActionEvent ae ) {
        		final FileChooser loggingFileChooser = new FileChooser( );
        		loggingFileChooser.setTitle( "Open Log File" );
        		loggingFileChooser.getExtensionFilters().addAll(
        				new FileChooser.ExtensionFilter( "Terminal Log Files", "*.txt", "*.log"  )
        				);

        		logFile = loggingFileChooser.showSaveDialog( mainStage );

        		if (logFile != null) {
        			String loggingFileName = logFile.getPath();
        			System.out.println( "DEBUG: Opening " + loggingFileName + " for logging." );
        			if (loggingFileName == null ) {
        				status.logging = false;
        			} else {
        				try {
        					logBuffWriter = new BufferedWriter( new FileWriter( logFile ));
        				} catch (IOException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
        				(loggingThread = new Thread( new LogWriter( logBuffWriter, logQ ))).start();
        				loggingThread.setName( "LoggingThread" );
        				startLoggingMenuItem.setDisable( true );
        				stopLoggingMenuItem.setDisable( false );
        				status.logging = true;
        			}
        		}
        	}
        });

        stopLoggingMenuItem.setOnAction(
        		new EventHandler<ActionEvent>() {
        			@Override
        			public void handle( ActionEvent ae ) {
        				loggingThread.interrupt();
        				startLoggingMenuItem.setDisable( false );
				    	stopLoggingMenuItem.setDisable( true );
        			}
        		});
        
        fileMenu.getItems().add( startLoggingMenuItem );
        stopLoggingMenuItem.setDisable( true );
        fileMenu.getItems().add( stopLoggingMenuItem );
        
        
        exitMenuItem.setOnAction( 
        		new EventHandler<ActionEvent>() {
        			@Override
        			public void handle( ActionEvent ae) {
        				if (sc != null && sc.connected) {
        					sc.close();
        				}
        				if (tc != null && tc.connected) {
        					tc.close();
        				}
        				System.out.println( "DasherJ clean exit" );
        			    Platform.exit();
        				System.exit( 0 );
        			}
        		});
        
        fileMenu.getItems().add( new SeparatorMenuItem() );
        fileMenu.getItems().add( exitMenuItem );
        
        // edit
        
        menuBar.getMenus().add( editMenu );
        editMenu.getItems().add( pasteMenuItem );
        
        pasteMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		if (clipboard.hasString()) {
        			String s = clipboard.getString();
        			for (int ix = 0; ix < s.length(); ix++) {
        				fromKbdQ.offer( (byte) s.charAt( ix ) );
        			}
        		}
        	} 	
        });    	

        
        // emulation
        
        menuBar.getMenus().add( emulMenu );

        boolean firstEmul = true;
        for ( final Status.EmulationType em : Status.EmulationType.values() ) {
        	RadioMenuItem mi = new RadioMenuItem( em.toString() );
        	mi.setOnAction(new EventHandler<ActionEvent>() {
            	@Override
            	public void handle( ActionEvent ae ) {
            		status.emulation = em;
            	}
            });
        	//emulGroup.add( mi );
        	emulMenu.getItems().add( mi );
        	if (firstEmul) {
        		mi.setSelected( true );
        		firstEmul = false;
        	}
        }
        
        emulMenu.getItems().add( new SeparatorMenuItem() );
        
        emulMenu.getItems().add( resizeMenuItem );
        resizeMenuItem.setOnAction( new EventHandler<ActionEvent>() {
			public void handle( ActionEvent ae ) {
				getNewSize();
				
			}
       	
        });
        
        emulMenu.getItems().add( new SeparatorMenuItem() );
        
        emulMenu.getItems().add( selfTestMenuItem );
        selfTestMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	public void handle( ActionEvent ae ) {
        		fromKbdQ.offer( Terminal.SELF_TEST );
        	}
        });
        
        emulMenu.getItems().add( new SeparatorMenuItem() );
        
        emulMenu.getItems().add( loadTemplateItem );
        loadTemplateItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		fkeyGrid.loadTemplate();
        		// window.pack();
        	}
        });
        
        // serial i/o
        
        menuBar.getMenus().add(serialMenu);
        	
        serialConnectMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		// ask the local echo client to stop
        		fromKbdQ.offer( LocalClient.GO_ONLINE );
        		getSerialPort();
        		serialConnectMenuItem.setDisable( sc.connected );
        		networkMenu.setDisable( sc.connected );
        		serialDisconnectMenuItem.setDisable ( !sc.connected );
        		// status.serialPort = sc.serialPort.toString();
        	}
        });
        serialMenu.getItems().add( serialConnectMenuItem );
        
        serialDisconnectMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		sc.close();
        		serialConnectMenuItem.setDisable( sc.connected );
        		networkMenu.setDisable( sc.connected );
        		serialDisconnectMenuItem.setDisable ( !sc.connected );
        		status.connection = Status.ConnectionType.DISCONNECTED;
        		// restart local echo thread
        		(localThread = new Thread(new LocalClient( fromHostQ, fromKbdQ ))).start();
        	}
        });
        serialDisconnectMenuItem.setDisable( true );
        serialMenu.getItems().add( serialDisconnectMenuItem );
        
        serialMenu.getItems().add( new SeparatorMenuItem() );
        
        b300MenuItem.setToggleGroup( baudGroup );
        serialMenu.getItems().add( b300MenuItem );
        b1200MenuItem.setToggleGroup( baudGroup );
        serialMenu.getItems().add( b1200MenuItem );
        //b9600MenuItem.setSelected( true ); // default to 9600 baud
        b9600MenuItem.setToggleGroup( baudGroup ); 
        serialMenu.getItems().add( b9600MenuItem );
        b19200MenuItem.setToggleGroup( baudGroup );
        serialMenu.getItems().add( b19200MenuItem );
        
        // network (tcp/ip) i/o
        menuBar.getMenus().add(networkMenu);

        networkConnectMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		// ask the local echo client to stop
        		fromKbdQ.offer( LocalClient.GO_ONLINE );
        		getTargetHost();
        		networkConnectMenuItem.setDisable( tc.connected );
        		serialMenu.setDisable( tc.connected );
        		networkDisconnectMenuItem.setDisable( !tc.connected );
        		// statusBar.updateStatus();
        	}
        });
        networkMenu.getItems().add( networkConnectMenuItem );
        
        networkDisconnectMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		tc.close();
        		networkConnectMenuItem.setDisable( tc.connected );
        		serialMenu.setDisable( tc.connected );
        		networkDisconnectMenuItem.setDisable( !tc.connected );
        		status.connection = Status.ConnectionType.DISCONNECTED;
        		// restart local echo thread
        		(localThread = new Thread(new LocalClient( fromHostQ, fromKbdQ ))).start();
        	}
        });
        networkDisconnectMenuItem.setDisable( true );
        networkMenu.getItems().add( networkDisconnectMenuItem );
        
        // Help etc.
        
        menuBar.getMenus().add( helpMenu );
        
        helpMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		openWebpage( HELP_URL_TEXT );
        	}
        });
        
        helpMenu.getItems().add( helpMenuItem );
       
        aboutMenuItem.setOnAction( new EventHandler<ActionEvent>() {
        	@Override
        	public void handle( ActionEvent ae ) {
        		showAboutDialog();
        	}
        });
        
        helpMenu.getItems().add( aboutMenuItem );
        
        return menuBar;
	}
	
	protected void showAboutDialog() {
		
		Alert alert = new Alert( AlertType.INFORMATION );
		alert.setTitle( "About DasherJ" );
		alert.setHeaderText( null );
		alert.setContentText( String.format(
									   "Dasher Terminal Emulator\n\n" +
									   "Version %s (%s)\n\n" +
									   "\u00a9 Steve Merrony",
									   VERSION, RELEASE_STATUS, COPYRIGHT_YEAR ));
		alert.setGraphic( new ImageView( new Image( DasherJ.class.getResourceAsStream( ICON )) ));
		alert.showAndWait();
	}
	
	public void openWebpage( String uri ) {
//		try {
//			Desktop.getDesktop().browse( new URI( uri ) );
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
		
//			HostServices hostSvcs = getHostServices();
//			hostSvcs.showDocument( uri );
		
		Alert alert = new Alert( AlertType.INFORMATION );
		alert.setTitle( "DasherJ Help" );
		alert.setHeaderText( null );
		alert.setContentText( String.format( "Please see\n\n" + HELP_URL_TEXT ) );
		alert.setGraphic( new ImageView( new Image( DasherJ.class.getResourceAsStream( ICON )) ));
		alert.showAndWait();

	}
 
    private static void parseHost( String hostArg ) {
    	// format of arg is "--host=<hostNameOrIP>:<port>"
    	int colonIx = hostArg.indexOf( ':' );
    	if (colonIx == -1) {
    		System.err.println( "Error - With a host/IP you must specify a port number after colon (:)" );
    		System.exit( 1 );
    	}
    	// TODO: more error checking on hostname/port
    	connectHost = hostArg.substring( 7, colonIx );
    	connectPort = Integer.parseInt( hostArg.substring( colonIx + 1 ) );
    	haveConnectHost = true;   	
    }
    
    public static void main(String[] args) {	
    	launch( args );
    }

    
}
