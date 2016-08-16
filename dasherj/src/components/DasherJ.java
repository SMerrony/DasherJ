/* 
 * Copyright (C) 2016 Stephen Merrony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package components;

/**
 * DasherJ - the main class for the emulator
 * 
 * @author steve
 * 
 * v.1.2  Add D211 emulation, fix D210 emulation now we have documentation.
 *        Add terminal history functionality
 * v.1.1  CRT colour changes
 *        Simplify layout widgets
 *        Fix resizing/rescaling
 * v.1.0  Clean-ups suggested by FindBugs
 *        Add Restart Session option to Telnet menu
 *        Change "Medium" HZoom to be 0.8 to scale 10px wide char cells better
 * v.0.9  Move to JavaFX
 *        Move default zoom factors here from Crt
 *        Add --host= option for auto-connect 
 *        Add preferences store, remember last host 
 *        Implement Local Print function
 *        Reduce SLOC using Java 8 lambdas
 *        Fix showing of online help
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
import java.util.prefs.Preferences;
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
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
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
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import components.Status.ConnectionType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;


public class DasherJ extends Application {

  private static final int CRT_REFRESH_MS = 50;  // Euro screen refresh rate was 50Hz = 20ms, US was 60Hz = 17ms
  private static final int CRT_BLINK_COUNTER = 500 / CRT_REFRESH_MS;
  public static final double DEFAULT_HORIZ_ZOOM = 1.0;
  // For an authentic DASHER look, the characters are stretched vertically, this results
  // in a close approximation of a physical DASHER display ratio
  public static final double DEFAULT_VERT_ZOOM = 2.0; 

  private static final double VERSION = 1.2;
  private static final int COPYRIGHT_YEAR = 2016;
  private static final String RELEASE_STATUS = "Production";
  private static final String HELP_URL_TEXT = "http://stephenmerrony.co.uk/dg/doku.php?id=software:newsoftware:dasherj";

  private static final String ICON = "/resources/DGlogoOrange.png";

  private static final String LAST_HOST_PREF = "LAST_HOST";
  private static final String LAST_PORT_PREF = "LAST_PORT";
  private static final String LAST_SERIAL_PREF = "LAST_SERIAL";
  private static final String LAST_BAUD_PREF = "LAST_BAUD";

  private boolean haveConnectHost = false;
  private String  connectHost;
  private int	  connectPort;

  Status status;
  Clipboard clipboard;

  Preferences prefs;

  FKeyHandler fKeyHandler;
  KeyboardHandler keyHandler;
  LocalPrintHandler locPrHandler;

  FKeyGrid fkeyGrid;
  DasherStatusBar statusBar;
  LocalClient localClient;
  SerialClient serialClient;
  TelnetClient telnetClient;
  BlockingQueue<Byte> fromHostQ, fromKbdQ, logQ;
  Crt crt;
  Terminal terminal;
  File logFile;
  BufferedWriter logBuffWriter;

  Thread screenThread, localThread, loggingThread;

  Timeline updateCrtTimeline;

  // GUI elements
  VBox topVboxPane;
  VBox mainVbox;
  Scale scale;
  Stage mainStage;
  Scene scene;
  Menu networkMenu;
  MenuItem networkConnectMenuItem, networkDisconnectMenuItem, networkRestartMenuItem; 
  Menu serialMenu;
  MenuItem serialConnectMenuItem, serialDisconnectMenuItem;  
  double initialStageWidth;

  @Override
  public void start( Stage mainStage ) {

    this.mainStage = mainStage;

    fromHostQ = new LinkedBlockingQueue<>(); // data from the host
    fromKbdQ  = new LinkedBlockingQueue<>(); // data from the keyboard (or faked data)
    logQ      = new LinkedBlockingQueue<>(); // data to be logged

    status = new Status();
    prefs = Preferences.userRoot().node( this.getClass().getName() );

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

    topVboxPane = new VBox();
    mainVbox = new VBox();
    scene = new Scene( mainVbox );		

    clipboard = Clipboard.getSystemClipboard();

    MenuBar menuBar = createMenuBar( mainStage );
    topVboxPane.getChildren().add( menuBar );

    locPrHandler = new LocalPrintHandler();

    fKeyHandler = new FKeyHandler( fromKbdQ, status );		
    fkeyGrid = new FKeyGrid( status, fKeyHandler, locPrHandler, mainStage, scene );
    topVboxPane.getChildren().add( fkeyGrid.grid );// FIXME how to handle this changing height?

    //borderPane.setTop( vboxPane );
    mainVbox.getChildren().add(topVboxPane );

    crt = new Crt( terminal );
    crt.setWidth( terminal.visible_cols * BDFfont.CHAR_PIXEL_WIDTH * DEFAULT_HORIZ_ZOOM );
    crt.setHeight( terminal.visible_lines * BDFfont.CHAR_PIXEL_HEIGHT * DEFAULT_VERT_ZOOM );
    // System.out.printf( "DEBUG - initial CRT width: %f\n", terminal.visible_cols * BDFfont.CHAR_PIXEL_WIDTH * DEFAULT_HORIZ_ZOOM );
    scale = new Scale( DEFAULT_HORIZ_ZOOM, DEFAULT_VERT_ZOOM );
    crt.getTransforms().add( scale );
    crt.setFocusTraversable( true );
    mainVbox.getChildren().add( crt );
    
    // USEFUL for DEBUGGING LAYOUT: 
    //mainVbox.setStyle( "-fx-background-color: red;" ); 

    // install our keyboard handler
    keyHandler = new KeyboardHandler( fromKbdQ, status );
    scene.addEventHandler( KeyEvent.ANY, keyHandler );

    // we don't want the user randomly farting around with the terminal size..
    mainStage.setResizable( false );

    statusBar = new DasherStatusBar( status );
    mainVbox.getChildren().add( statusBar );

    Timeline updateStatusBarTimeline = new Timeline( new KeyFrame( Duration.millis( DasherStatusBar.STATUS_REFRESH_MS ), 
                                                     (ActionEvent ae) -> statusBar.updateStatus() 
                                                    ));
    updateStatusBarTimeline.setCycleCount( Timeline.INDEFINITE );
    updateStatusBarTimeline.play();

    // customise icon
    mainStage.getIcons().add( new Image( DasherJ.class.getResourceAsStream( ICON )));

    mainStage.setOnCloseRequest((WindowEvent we) -> {
        if (serialClient != null && serialClient.connected) {
            serialClient.close();
        }
        if (telnetClient != null && telnetClient.connected) {
            telnetClient.close();
        }
        System.out.println( "DasherJ clean exit" );
        Platform.exit();
        System.exit( 0 );
    });

    if (haveConnectHost) startTelnet( connectHost, connectPort );

    // sort out the menu state if we are already connected
    if (status.connection == ConnectionType.TELNET_CONNECTED) {
      networkConnectMenuItem.setDisable( true );
      networkDisconnectMenuItem.setDisable( false );
      serialMenu.setDisable( true );
      prefs.put( LAST_HOST_PREF, status.remoteHost );
      prefs.put( LAST_PORT_PREF, status.remotePort );
    } else if (status.connection == ConnectionType.SERIAL_CONNECTED) {
      serialConnectMenuItem.setDisable( true );
      serialDisconnectMenuItem.setDisable( false );
      networkMenu.setDisable( true );
      prefs.put( LAST_SERIAL_PREF, status.serialPort );
    }

    /* Euro screen refresh rate was 50Hz = 20ms, US was 60Hz = 17ms */
      updateCrtTimeline = new Timeline(new KeyFrame(Duration.millis(CRT_REFRESH_MS),
              (ActionEvent ae) -> {
                  status.blinkCountdown--;
                  if (status.dirty || status.blinkCountdown == 0) {
                      crt.paintCrt();
                      status.dirty = false;
                  }
                  if (status.blinkCountdown == 0) {
                      terminal.blinkState = !terminal.blinkState;
                      status.blinkCountdown = CRT_BLINK_COUNTER;
                  }
              }));
    updateCrtTimeline.setCycleCount( Timeline.INDEFINITE );
    updateCrtTimeline.play();

    // Display the window.    
    mainStage.setScene( scene );
    mainStage.sizeToScene();
    initialStageWidth = mainStage.getWidth();

    mainStage.show();
  }

  public void getNewSize() {
    ObservableList<Integer> linesInts = FXCollections.observableArrayList( 24, 25, 36, 48, 66 );
    ObservableList<Integer> colsInts = FXCollections.observableArrayList( 80, 81, 120, 132, 135 );
    ObservableList<String> zoomStrings = FXCollections.observableArrayList( "Normal", "Smaller", "Tiny" );
    ComboBox<Integer> linesCombo, colsCombo;
    ComboBox<String> zoomCombo;
    linesCombo = new ComboBox<>( linesInts );
    linesCombo.setValue( 24 );
    colsCombo = new ComboBox<>( colsInts );
    colsCombo.setValue( 80 );
    zoomCombo = new ComboBox<>( zoomStrings );
    zoomCombo.setValue( "Normal" );
    Dialog<ButtonType> newSizeDialog = new Dialog<>();
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
      double newHzoom = DEFAULT_HORIZ_ZOOM, newVzoom = DEFAULT_VERT_ZOOM;
      switch (zoomCombo.getValue()) {
      case "Normal": // Normal
        newHzoom = DEFAULT_HORIZ_ZOOM;
        newVzoom = DEFAULT_VERT_ZOOM;
        break;
      case "Smaller": // Smaller
        newHzoom = 0.8;  newVzoom = 1.0;
        break;
      case "Tiny": // Tiny
        newHzoom = 0.5; newVzoom = 1.0;
        break;
      }
      updateCrtTimeline.pause();

      terminal.resize( newLines, newCols );
      double newWidth =  (double) ( newCols * BDFfont.CHAR_PIXEL_WIDTH );
      double newHeight = (double) ( newLines * BDFfont.CHAR_PIXEL_HEIGHT * newVzoom );
      crt.setWidth( newWidth );
      crt.setHeight( newHeight );

      scale.setX( newHzoom );
      scale.setY( newVzoom );
      // System.out.printf( "DEBUG - new CRT width: %f\n", newWidth );

      status.dirty = true;
     
      mainStage.sizeToScene();
      mainStage.setWidth( (newWidth * newHzoom) + 6 );
      
      updateCrtTimeline.play();
    }
  }

  public boolean getSerialPort() {

    String lastSerial = prefs.get( LAST_SERIAL_PREF, "n/a" );
    if (lastSerial.equals("n/a")) {
      if (System.getProperty( "os.name" ).toLowerCase().contains( "win" )) {
        lastSerial = "COM1";
      } else {
        lastSerial =  "/dev/ttyS0";
      }
    }
    TextInputDialog serialDialog = new TextInputDialog( lastSerial );
    serialDialog.setTitle( "DasherJ - Serial Port" );
    serialDialog.setContentText( "Port eg. COM1: or /dev/ttyS0 :" );
    Optional<String> rc = serialDialog.showAndWait();
    
    if (rc.isPresent()) { // OK
      // initialise the serial port handler
      serialClient = new SerialClient( fromHostQ, fromKbdQ );
      if (serialClient.open( serialDialog.getEditor().getText(), status.baudRate )) {	
        status.connection = ConnectionType.SERIAL_CONNECTED;
        status.serialPort = serialDialog.getEditor().getText();
        prefs.put( LAST_SERIAL_PREF, status.serialPort );
        prefs.putInt( LAST_BAUD_PREF, status.baudRate );
        return true;
      } else {
        Alert alert = new Alert( AlertType.ERROR);
        alert.setContentText( "Could not open " + serialDialog.getEditor().getText() );
        alert.showAndWait();
        status.connection = ConnectionType.DISCONNECTED;
        return false;
      }
    } else
      return false;
  }

  public boolean getTargetHost() {

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle( "DasherJ - Remote Host" );
    dialog.getDialogPane().getButtonTypes().addAll( ButtonType.CANCEL, ButtonType.OK );
    GridPane grid = new GridPane();
    grid.setHgap( 10 );
    grid.setVgap( 10 );
    grid.setPadding(  new Insets( 20, 20, 10, 10 ) );		
    TextField host = new TextField( prefs.get( LAST_HOST_PREF, "localhost" ) );
    TextField port = new TextField( prefs.get( LAST_PORT_PREF, "23" ) );
    grid.add( new Label( "Host:" ), 0, 0 );
    grid.add( host, 1,  0 );
    grid.add( new Label( "Port:" ), 0, 1 );
    grid.add( port, 1, 1 );
    dialog.getDialogPane().setContent( grid );
    Optional<ButtonType> rc = dialog.showAndWait();

    if (rc.get() == ButtonType.OK) { 
      if (!startTelnet( host.getText(), Integer.parseInt( port.getText() ) )) {
        Alert alert = new Alert( AlertType.ERROR );
        alert.setContentText( "Could not connect to " + host + ':' + port );
        alert.showAndWait();
        return false;
      } else 
        return true;
    } else
      return false;
  }

  private boolean startTelnet( String host, int port ) {
    // initialise the telnet session handler
    telnetClient = new TelnetClient( fromHostQ, fromKbdQ );
    if (telnetClient.open( host, port )) {
      status.remoteHost = host;
      status.remotePort = "" + port;
      status.connection = ConnectionType.TELNET_CONNECTED;
      prefs.put( LAST_HOST_PREF, status.remoteHost );
      prefs.put( LAST_PORT_PREF, status.remotePort );
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
    
    final Menu viewMenu = new Menu( "View" );
    final MenuItem viewHistoryMenuItem = new MenuItem( "View History" );

    final Menu emulMenu = new Menu( "Emulation" );
    final ToggleGroup emulGroup = new ToggleGroup();
    final MenuItem resizeMenuItem = new MenuItem( "Resize" );
    final MenuItem selfTestMenuItem = new MenuItem( "Self-Test" );
    final MenuItem loadTemplateItem = new MenuItem( "Load Template" );

    serialMenu = new Menu( "Serial" );
    serialConnectMenuItem = new MenuItem( "Connect" );
    serialDisconnectMenuItem = new MenuItem( "Disconnect" );  
    final ToggleGroup baudGroup = new ToggleGroup();
    final RadioMenuItem b300MenuItem = new RadioMenuItem( "300 baud" );
    b300MenuItem.setOnAction((ae) -> { 
      status.baudRate = 300;
      if (status.connection == Status.ConnectionType.SERIAL_CONNECTED) serialClient.changeBaudRate( 300 );
    });
    final RadioMenuItem b1200MenuItem = new RadioMenuItem( "1200 baud" );
    b1200MenuItem.setOnAction((ae) -> { 
      status.baudRate = 1200;
      if (status.connection == Status.ConnectionType.SERIAL_CONNECTED) serialClient.changeBaudRate( 1200 );
    });
    final RadioMenuItem b9600MenuItem = new RadioMenuItem( "9600 baud" );
    b9600MenuItem.setOnAction((ae) -> { 
      status.baudRate = 9600;
      if (status.connection == Status.ConnectionType.SERIAL_CONNECTED) serialClient.changeBaudRate( 9600 );
    });
    final RadioMenuItem b19200MenuItem = new RadioMenuItem( "19200 baud" );
    b19200MenuItem.setOnAction((ae) -> { 
      status.baudRate = 19200;
      if (status.connection == Status.ConnectionType.SERIAL_CONNECTED) serialClient.changeBaudRate( 19200 );
    });

    //default
    b9600MenuItem.setSelected( true );

    networkMenu = new Menu( "Network" );
    networkConnectMenuItem = new MenuItem( "Connect" );
    networkDisconnectMenuItem = new MenuItem( "Disconnect" ); 
    networkRestartMenuItem = new MenuItem( "Restart Session" );

    final Menu helpMenu = new Menu( "Help" );
    final MenuItem helpMenuItem = new MenuItem( "Online Help" );
    final MenuItem aboutMenuItem = new MenuItem( "About DasherJ" );

    // actually build the menu and add non-trivial Actions

    menuBar.getMenus().add( fileMenu ); 

    startLoggingMenuItem.setOnAction( new EventHandler<ActionEvent>() {
      @Override
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

    stopLoggingMenuItem.setOnAction( (ae) -> {
      loggingThread.interrupt();
      startLoggingMenuItem.setDisable( false );
      stopLoggingMenuItem.setDisable( true );
    });

    fileMenu.getItems().add( startLoggingMenuItem );
    stopLoggingMenuItem.setDisable( true );
    fileMenu.getItems().add( stopLoggingMenuItem );

    exitMenuItem.setOnAction((ae) -> {
      if (serialClient != null && serialClient.connected) {
        serialClient.close();
      }
      if (telnetClient != null && telnetClient.connected) {
        telnetClient.close();
      }
      System.out.println( "DasherJ clean exit" );
      Platform.exit();
      System.exit( 0 );
    });

    fileMenu.getItems().add( new SeparatorMenuItem() );
    fileMenu.getItems().add( exitMenuItem );

    // edit

    menuBar.getMenus().add( editMenu );
    editMenu.getItems().add( pasteMenuItem );

    pasteMenuItem.setOnAction( (ae) -> {
      if (clipboard.hasString()) {
        String s = clipboard.getString();
        for (int ix = 0; ix < s.length(); ix++) {
          fromKbdQ.offer( (byte) s.charAt( ix ) );
        }
      }
    });    	

    // view
    menuBar.getMenus().add( viewMenu );
    viewMenu.getItems().add( viewHistoryMenuItem );
    
    viewHistoryMenuItem.setOnAction( (ae) -> {
        showHistoryDialog();
    });

    // emulation

    menuBar.getMenus().add( emulMenu );

    boolean firstEmul = true;
    for ( final Status.EmulationType em : Status.EmulationType.values() ) {
      RadioMenuItem mi = new RadioMenuItem( em.toString() );
      mi.setOnAction( (ae) -> status.emulation = em );
      emulGroup.getToggles().add( mi );
      emulMenu.getItems().add( mi );
      if (firstEmul) {
        mi.setSelected( true );
        firstEmul = false;
      }
    }

    emulMenu.getItems().add( new SeparatorMenuItem() );
    emulMenu.getItems().add( resizeMenuItem );
    resizeMenuItem.setOnAction( (ae) -> getNewSize() );
    emulMenu.getItems().add( new SeparatorMenuItem() );
    emulMenu.getItems().add( selfTestMenuItem );
    selfTestMenuItem.setOnAction( (ae) -> fromKbdQ.offer( Terminal.SELF_TEST ) );
    emulMenu.getItems().add( new SeparatorMenuItem() );
    emulMenu.getItems().add( loadTemplateItem );
    loadTemplateItem.setOnAction( (ae) -> fkeyGrid.loadTemplate() );

    // serial i/o
    menuBar.getMenus().add(serialMenu);
    serialConnectMenuItem.setOnAction((ae) -> {
      if (getSerialPort()) {
        // ask the local echo client to stop
        fromKbdQ.offer( LocalClient.GO_ONLINE );
        serialConnectMenuItem.setDisable(serialClient.connected );
        networkMenu.setDisable(serialClient.connected );
        serialDisconnectMenuItem.setDisable (!serialClient.connected );
      }
    });
    serialMenu.getItems().add( serialConnectMenuItem );

    serialDisconnectMenuItem.setOnAction((ae) -> {
      serialClient.close();
      serialConnectMenuItem.setDisable(serialClient.connected );
      networkMenu.setDisable(serialClient.connected );
      serialDisconnectMenuItem.setDisable (!serialClient.connected );
      status.connection = Status.ConnectionType.DISCONNECTED;
      // restart local echo thread
      (localThread = new Thread(new LocalClient( fromHostQ, fromKbdQ ))).start();
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
    networkConnectMenuItem.setOnAction((ae) -> {
      if (getTargetHost()) {
        // ask the local echo client to stop
        fromKbdQ.offer( LocalClient.GO_ONLINE );
        networkConnectMenuItem.setDisable(telnetClient.connected );
        serialMenu.setDisable(telnetClient.connected );
        networkDisconnectMenuItem.setDisable(!telnetClient.connected );
        networkRestartMenuItem.setDisable( !telnetClient.connected );
      }
    });
    networkMenu.getItems().add( networkConnectMenuItem );

    networkDisconnectMenuItem.setOnAction((ae) -> {
      telnetClient.close();
      networkConnectMenuItem.setDisable(telnetClient.connected );
      serialMenu.setDisable(telnetClient.connected );
      networkDisconnectMenuItem.setDisable(!telnetClient.connected );
      networkRestartMenuItem.setDisable( true );
      status.connection = Status.ConnectionType.DISCONNECTED;
      // restart local echo thread
      (localThread = new Thread(new LocalClient( fromHostQ, fromKbdQ ))).start();
    });
    networkDisconnectMenuItem.setDisable( true );
    networkMenu.getItems().add( networkDisconnectMenuItem );
    
    networkMenu.getItems().add( new SeparatorMenuItem() );
    
    networkMenu.getItems().add( networkRestartMenuItem );
    networkRestartMenuItem.setDisable( true );
    networkRestartMenuItem.setOnAction( (ae) -> {
        telnetClient.restart();
    });

    // Help etc.
    menuBar.getMenus().add( helpMenu );
    helpMenuItem.setOnAction( (ae) -> getHostServices().showDocument( HELP_URL_TEXT ) );
    helpMenu.getItems().add( helpMenuItem );
    aboutMenuItem.setOnAction( (ae) -> showAboutDialog() );
    helpMenu.getItems().add( aboutMenuItem );

    return menuBar;
  }

  // GUI Actions

  protected void showAboutDialog() {
    Alert alert = new Alert( AlertType.INFORMATION );
    alert.setTitle( "About DasherJ" );
    alert.setHeaderText( null );
    alert.setContentText( String.format(
        "Dasher Terminal Emulator%n%n" +
            "Version %s (%s)%n%n" +
            "Copyright \u00a9%s Steve Merrony",
            VERSION, RELEASE_STATUS, COPYRIGHT_YEAR ));
    alert.setGraphic( new ImageView( new Image( DasherJ.class.getResourceAsStream( ICON )) ));
    alert.showAndWait();
  }

  private void parseHost( String hostArg ) {
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

    private void showHistoryDialog() {
        Dialog historyDialog = new Dialog();
        historyDialog.setTitle( "DasherJ Terminal History" );
        ButtonType closeButtonType = new ButtonType( "Close", ButtonData.CANCEL_CLOSE );
        historyDialog.getDialogPane().getButtonTypes().add( closeButtonType );
        TextArea historyArea = new TextArea( terminal.history.fetchAllAsString() + terminal.fetchDisplayAsString() );
        historyArea.setPrefColumnCount( terminal.visible_cols );
        historyArea.setPrefRowCount( terminal.visible_lines );
        historyArea.setEditable( false );
        historyArea.setFont( Font.font( "monospace" ) );
        historyDialog.getDialogPane().setContent( historyArea );
        historyDialog.showAndWait();
    }

  public class LocalPrintHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle( ActionEvent arg0 ) {
      WritableImage wImage = crt.snapshot( null, null );
      ImageView imageView = new ImageView( wImage );
      Printer printer = Printer.getDefaultPrinter();
      PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);
      double scaleX = pageLayout.getPrintableWidth() / imageView.getBoundsInParent().getWidth();
      double scaleY = pageLayout.getPrintableHeight() / imageView.getBoundsInParent().getHeight();
      imageView.getTransforms().add(new Scale(scaleX, scaleY));
      PrinterJob job = PrinterJob.createPrinterJob();
      if (job != null) {
        if (job.showPrintDialog( mainStage )) {
          boolean ok = job.printPage( pageLayout, imageView );
          if (ok) job.endJob();
        }
      }		
    }
  }

  // main() should not really be required in a JavaFX program, nevertheless...
  public static void main(String[] args) {	
    launch( args );
  }

}
