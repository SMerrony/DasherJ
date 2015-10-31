package components;

/***
 * v. 0.6 IP in status bar
 *        Fix display of serial port in status bar
 *        --host= option added
 *        Rename screen to terminal
 *        Lock main window size
 * v. 0.5 Add ErPage. CR. ErEOL buttons
 *        Implement session logging as per v.0.4
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import components.Status.ConnectionType;

public class DasherJ extends JPanel implements ActionListener {
	
	private static final int CRT_REFRESH_MS = 50;  // Euro screen refresh rate was 50Hz = 20ms, US was 60Hz = 17ms
	private static Timer updateCrtTimer;

	private static final double VERSION = 0.6;
	private static final int COPYRIGHT_YEAR = 2015;
	private static final String RELEASE_STATUS = "Prerelease";
	private static final String HELP_URL_TEXT = "http://stephenmerrony.co.uk/dg/";
	
	private static final String ICON = "/resources/DGlogoOrange.png";
	
	private static boolean haveConnectHost = false;
	private static String  connectHost;
	private static int	   connectPort;
	
	static Status status;
	static JFrame window;
	static JToolBar toolbar;
	KeyboardFocusManager keyFocusManager;
	KeyboardHandler keyHandler;
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
	
	public DasherJ() {
		super( new BorderLayout() );
		
		//setPreferredSize( new Dimension( 750,400 ) );
		
        window.setJMenuBar( createMenuBar() );
        
        toolbar = new JToolBar();
        addToolButtons( toolbar );
        add( toolbar, BorderLayout.PAGE_START );
        
        crt = new Crt( terminal );
        crt.setCharSize( terminal.visible_lines, terminal.visible_cols );
         
        // add the crt canvas to the content pane.
        add( crt, BorderLayout.CENTER );
        crt.setFocusable(true);
        crt.setVisible( true );
        
        // install our keyboard handler
        keyFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyHandler = new KeyboardHandler( fromKbdQ, status );
        keyFocusManager.addKeyEventDispatcher( keyHandler );

        window.pack();
        
        // we don't want the user randomly farting around with the terminal size..
        window.setResizable( false );
             
        statusBar = new DasherStatusBar( status );
        add( statusBar, BorderLayout.SOUTH );
        statusBar.setVisible( true );

	}
	
  	public void getSerialPort() {
		
  		// stop our own keyboard handler so the dialog can get input
  		keyFocusManager.removeKeyEventDispatcher( keyHandler );
  		
		JTextField port = new JTextField( "COM1" );
		final JComponent[] inputs = new JComponent[] {
				new JLabel( "Port:" ), port
		};
		int rc = JOptionPane.showConfirmDialog(window, inputs, "DasherJ - Serial Port", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		//System.out.printf( "SCD Result %d\n", rc );
		if (rc == 0) { // OK
			// initialise the serial port handler
			sc = new SerialClient( fromHostQ, fromKbdQ );
			if (sc.open( port.getText() )) {
				status.connection = ConnectionType.SERIAL_CONNECTED;
				status.serialPort = port.getText();
			} else {
				JOptionPane.showMessageDialog( window, "Could not open " + port.getText() );
				status.connection = ConnectionType.DISCONNECTED;
			}
		}
		
		// restart our own keyboard handler
		keyFocusManager.addKeyEventDispatcher( keyHandler );
	}
	
	public void getTargetHost() {
		
		// stop our own keyboard handler so the dialog can get input
  		keyFocusManager.removeKeyEventDispatcher( keyHandler );
  		
		JTextField host = new JTextField();
		JTextField port = new JTextField( "23" );
		final JComponent[] inputs = new JComponent[] {
				new JLabel( "Host:" ), host,
				new JLabel( "Port:" ), port
		};
		int rc = JOptionPane.showConfirmDialog(window, inputs, "DasherJ - Remote Host", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		//System.out.printf( "SCD Result %d\n", rc );
		if (rc == 0) { // OK
			if (!startTelnet( host.getText(), Integer.parseInt( port.getText() ) )) {
				JOptionPane.showMessageDialog( window, "Could not connect to " + host + ':' + port );
			}
		}
		
		// restart our own keyboard handler
		keyFocusManager.addKeyEventDispatcher( keyHandler );
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
	
	public JMenuBar createMenuBar() {
			
		 //Create the menu bar.
        JMenuBar menuBar = new JMenuBar();  
        
        // declarations up-front so we can refer to other menus if required
        
        final JMenu fileMenu = new JMenu( "File" );     
        final JMenuItem startLoggingMenuItem = new JMenuItem( "Start Logging" );
        final JMenuItem stopLoggingMenuItem = new JMenuItem( "Stop Logging" );
        final JMenuItem exitMenuItem = new JMenuItem( "Exit" );
        
        final JMenu emulMenu = new JMenu( "Emulation" );
        final ButtonGroup emulGroup = new ButtonGroup();
               
        final JMenuItem selfTestMenuItem = new JMenuItem( "Self-Test" );
        
        final JMenu serialMenu = new JMenu( "Serial" );
        final JMenuItem serialConnectMenuItem = new JMenuItem( "Connect" );
		final JMenuItem serialDisconnectMenuItem = new JMenuItem( "Disconnect" );  
        final ButtonGroup baudGroup = new ButtonGroup();
        final JRadioButtonMenuItem b300MenuItem = new JRadioButtonMenuItem( "300 baud" );
        b300MenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae) {
        		sc.baudRate = 300;
        	}
        });
        final JRadioButtonMenuItem b1200MenuItem = new JRadioButtonMenuItem( "1200 baud" );
        b1200MenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae) {
        		sc.baudRate = 1200;
        	}
        });
        final JRadioButtonMenuItem b9600MenuItem = new JRadioButtonMenuItem( "9600 baud" );
        b9600MenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae) {
        		sc.baudRate = 9600;
        	}
        });
        final JRadioButtonMenuItem b19200MenuItem = new JRadioButtonMenuItem( "19200 baud" );
        b19200MenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae) {
        		sc.baudRate = 19200;
        	}
        });
        
        final JMenu networkMenu = new JMenu( "Network" );
		final JMenuItem networkConnectMenuItem = new JMenuItem( "Connect" );
		final JMenuItem networkDisconnectMenuItem = new JMenuItem( "Disconnect" );   
		
        final JMenu helpMenu = new JMenu( "Help" );
        final JMenuItem helpMenuItem = new JMenuItem( "Online Help" );
        final JMenuItem aboutMenuItem = new JMenuItem( "About DasherJ" );
        
        // actually build the menu
        
        menuBar.add( fileMenu ); 
        
        startLoggingMenuItem.addActionListener(
        		new ActionListener() {
        			@Override
        			public void actionPerformed( ActionEvent ae ) {
        				final JFileChooser loggingFileChooser = new JFileChooser( );
        				FileNameExtensionFilter filter = new FileNameExtensionFilter( "Terminal Log Files", "txt", "log" );
        				loggingFileChooser.setFileFilter( filter );
        				// stop our own keyboard handler so the dialog can get input
        		  		keyFocusManager.removeKeyEventDispatcher( keyHandler );
        		  		
        				int retVal = loggingFileChooser.showOpenDialog( DasherJ.this );
        				
        				// restart our own keyboard handler
        				keyFocusManager.addKeyEventDispatcher( keyHandler );
        				
        				if (retVal == JFileChooser.APPROVE_OPTION) {
        					logFile = loggingFileChooser.getSelectedFile();
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
        				    	startLoggingMenuItem.setEnabled( false );
        				    	stopLoggingMenuItem.setEnabled( true );
        						status.logging = true;
        					}
        				}
        			}
        		});
        
        stopLoggingMenuItem.addActionListener(
        		new ActionListener() {
        			@Override
        			public void actionPerformed( ActionEvent ae ) {
        				loggingThread.interrupt();
        				startLoggingMenuItem.setEnabled( true );
				    	stopLoggingMenuItem.setEnabled( false );
        			}
        		});
        
        fileMenu.add( startLoggingMenuItem );
        stopLoggingMenuItem.setEnabled( false );
        fileMenu.add( stopLoggingMenuItem );
        
        
        exitMenuItem.addActionListener( 
        		new ActionListener() {
        			@Override
        			public void actionPerformed( ActionEvent ae) {
        				if (sc != null && sc.connected) {
        					sc.close();
        				}
        				if (tc != null && tc.connected) {
        					tc.close();
        				}
        				window.dispose();
        				System.exit( 0 );
        			}
        		});
        
        fileMenu.addSeparator();
        fileMenu.add( exitMenuItem );
        
        // emulation
        
        menuBar.add( emulMenu );

        boolean firstEmul = true;
        for ( final Status.EmulationType em : Status.EmulationType.values() ) {
        	JRadioButtonMenuItem mi = new JRadioButtonMenuItem( em.toString() );
        	mi.addActionListener(new ActionListener() {
            	@Override
            	public void actionPerformed( ActionEvent ae ) {
            		status.emulation = em;
            	}
            });
        	emulGroup.add( mi );
        	emulMenu.add( mi );
        	if (firstEmul) {
        		mi.setSelected( true );
        		firstEmul = false;
        	}
        }
        
        emulMenu.addSeparator();
        
        emulMenu.add( selfTestMenuItem );
        selfTestMenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae ) {
        		fromKbdQ.offer( Terminal.SELF_TEST );
        	}
        });
        
        // serial i/o
        
        menuBar.add(serialMenu);
        	
        serialConnectMenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae ) {
        		// ask the local echo client to stop
        		fromKbdQ.offer( LocalClient.GO_ONLINE );
        		getSerialPort();
        		serialConnectMenuItem.setEnabled( !sc.connected );
        		networkMenu.setEnabled( !sc.connected );
        		serialDisconnectMenuItem.setEnabled ( sc.connected );
        		// status.serialPort = sc.serialPort.toString();
        	}
        });
        serialMenu.add( serialConnectMenuItem );
        
        serialDisconnectMenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae ) {
        		sc.close();
        		serialConnectMenuItem.setEnabled( !sc.connected );
        		networkMenu.setEnabled( !sc.connected );
        		serialDisconnectMenuItem.setEnabled ( sc.connected );
        		status.connection = Status.ConnectionType.DISCONNECTED;
        		// restart local echo thread
        		(localThread = new Thread(new LocalClient( fromHostQ, fromKbdQ ))).start();
        	}
        });
        serialDisconnectMenuItem.setEnabled( false );
        serialMenu.add( serialDisconnectMenuItem );
        
        serialMenu.addSeparator();
        
        baudGroup.add( b300MenuItem );
        serialMenu.add( b300MenuItem );
        baudGroup.add( b1200MenuItem );
        serialMenu.add( b1200MenuItem );
        b9600MenuItem.setSelected( true ); // default to 9600 baud
        baudGroup.add( b9600MenuItem );
        serialMenu.add( b9600MenuItem );
        baudGroup.add( b19200MenuItem );
        serialMenu.add( b19200MenuItem );
        
        // network (tcp/ip) i/o
        menuBar.add(networkMenu);

        networkConnectMenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae ) {
        		// ask the local echo client to stop
        		fromKbdQ.offer( LocalClient.GO_ONLINE );
        		getTargetHost();
        		networkConnectMenuItem.setEnabled( !tc.connected );
        		serialMenu.setEnabled( !tc.connected );
        		networkDisconnectMenuItem.setEnabled ( tc.connected );
        		// statusBar.updateStatus();
        	}
        });
        networkMenu.add( networkConnectMenuItem );
        
        networkDisconnectMenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae ) {
        		tc.close();
        		networkConnectMenuItem.setEnabled( !tc.connected );
        		serialMenu.setEnabled( !tc.connected );
        		networkDisconnectMenuItem.setEnabled ( tc.connected );
        		status.connection = Status.ConnectionType.DISCONNECTED;
        		// restart local echo thread
        		(localThread = new Thread(new LocalClient( fromHostQ, fromKbdQ ))).start();
        	}
        });
        networkDisconnectMenuItem.setEnabled( false );
        networkMenu.add( networkDisconnectMenuItem );
        
        // Help etc.
        
        menuBar.add( helpMenu );
        
        helpMenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae ) {
        		try {
					openWebpage( new URI( HELP_URL_TEXT ) );
				} catch (URISyntaxException e) {
					// e.printStackTrace();
				}
        	}
        });
        
        helpMenu.add( helpMenuItem );
       
        aboutMenuItem.addActionListener( new ActionListener() {
        	@Override
        	public void actionPerformed( ActionEvent ae ) {
        		showAboutDialog();
        	}
        });
        
        helpMenu.add( aboutMenuItem );
        
        return menuBar;
	}
	
	protected void showAboutDialog() {
		
		final ImageIcon icon = new ImageIcon( DasherJ.class.getResource( ICON ) );
		JOptionPane.showMessageDialog( window, String.format(
									   "<html><center>"+
									   "Dasher Terminal Emulator<br><br>" +
									   "Version %s (%s)<br><br>" +
									   "\u00a9 Steve Merrony" +
									   "</center></html>",
									   VERSION, RELEASE_STATUS, COPYRIGHT_YEAR ),
									   "About DasherJ", 
									   JOptionPane.PLAIN_MESSAGE,
									   icon );
	}
	
	public static void openWebpage( URI uri ) {
	    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
	    if (desktop != null && desktop.isSupported( Desktop.Action.BROWSE )) {
	        try {
	            desktop.browse(uri);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	protected  void addToolButtons( JToolBar tb ) {
		
		tb.add( makeToolbarButton( "Brk", "Command-Break" ) );
		tb.addSeparator();		
		tb.add( makeToolbarButton( "F1" ) );
		tb.add( makeToolbarButton( "F2" ) );
		tb.add( makeToolbarButton( "F3" ) );
		tb.add( makeToolbarButton( "F4" ) );
		tb.add( makeToolbarButton( "F5" ) );
		tb.addSeparator();
		tb.add( makeToolbarButton( "F6" ) );
		tb.add( makeToolbarButton( "F7" ) );
		tb.add( makeToolbarButton( "F8" ) );
		tb.add( makeToolbarButton( "F9" ) );
		tb.add( makeToolbarButton( "F10" ) );
		tb.addSeparator();
		tb.add( makeToolbarButton( "F11" ) );
		tb.add( makeToolbarButton( "F12" ) );
		tb.add( makeToolbarButton( "F13" ) );
		tb.add( makeToolbarButton( "F14" ) );
		tb.add( makeToolbarButton( "F15" ) );
		tb.addSeparator();
		tb.add( makeToolbarButton( "ErPg", "Erase Page" ) );
		tb.add( makeToolbarButton( "CR", "CR" ) );
		tb.add( makeToolbarButton( "ErEOL", "Erase EOL" ) );
		tb.addSeparator();
		tb.add( makeToolbarButton( "LocPrt", "Local Print" ) );
		// tb.add( makeToolbarButton( "SR", "Scroll Rate" ) );
		tb.add( makeToolbarButton( "Hold" ) );

	}
	
	protected JButton makeToolbarButton( String label, String tooltip ) {
		JButton button = makeToolbarButton( label );
		button.setToolTipText( tooltip );
		return button;
	}

	protected JButton makeToolbarButton( String label ) {
		JButton button = new JButton( label );
		button.setActionCommand( label );
		button.addActionListener( this );
		
		return button;
	}
	
	@Override
	public void actionPerformed( ActionEvent ae ) {
		
		byte modifier = 0;
		
		if (status.control_pressed && status.shift_pressed) { modifier = -80; }  // Ctrl-Shift
		if (status.control_pressed && !status.shift_pressed) { modifier = -64; } // Ctrl
		if (!status.control_pressed && status.shift_pressed) { modifier = -16; } // Shift
		
		// these are for the toolbar buttons - not the real function keys
		String cmd = ae.getActionCommand();
		switch (cmd) {
		case "Brk":
			fromKbdQ.offer( (byte) 2 );  // special CMD_BREAK indicator
			break;
		case "F1":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (113 + modifier) );
			break;
		case "F2":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (114 + modifier) );
			break;
		case "F3":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (115 + modifier) );
			break;
		case "F4":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (116 + modifier) );
			break;
		case "F5":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (117 + modifier) );
			break;
		case "F6":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (118 + modifier) );
			break;
		case "F7":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (119 + modifier) );
			break;
		case "F8":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (120 + modifier) );
			break;
		case "F9":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (121 + modifier) );
			break;
		case "F10":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (122 + modifier) );
			break;
		case "F11":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (123 + modifier) );
			break;
		case "F12":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (124 + modifier) );
			break;
		case "F13":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (125 + modifier) );
			break;
		case "F14":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (126 + modifier) );
			break;
		case "F15":
			fromKbdQ.offer( (byte) 30 );
			fromKbdQ.offer( (byte) (112 + modifier) );
			break;
		case "ErPg":
			fromKbdQ.offer( (byte) 12 );
			break;
		case "CR":
			fromKbdQ.offer( (byte) 13 );
			break;
		case "ErEOL":
			fromKbdQ.offer( (byte) 11 );
			break;
		case "LocPrt":
			PrinterJob printJob = PrinterJob.getPrinterJob();
			printJob.setPrintable( crt );
			boolean ok = printJob.printDialog();
			if (ok) {
				try {
					printJob.print();
				} catch (PrinterException pe) {
					
				}
			}
			break;
		case "Hold":
			status.holding = !status.holding;
			break;
		default:
			System.out.printf( "DasherJ - Warning: Unknown ActionEvent (%s) received.\n", cmd );
			break;	
		}

	}

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    /**
     * 
     */
    private static void createAndShowGUI() {
    	
        // Create and set up the window.
        window = new JFrame("DasherJ");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
        window.add( new DasherJ() );
 
        // Display the window.
        window.pack();
        
        // customise icon
        window.setIconImage(new ImageIcon( DasherJ.class.getResource( ICON )).getImage() );
        
        window.setVisible(true);

        if (haveConnectHost) startTelnet( connectHost, connectPort );
        
        /* Euro screen refresh rate was 50Hz = 20ms, US was 60Hz = 17ms */
        updateCrtTimer = new Timer( CRT_REFRESH_MS, new ActionListener() {
        	public void actionPerformed( ActionEvent ae ) {
              		if (status.dirty) {
              			crt.repaint();  
              			status.dirty = false;
              		}
        	}
        });
        updateCrtTimer.start();

        // alternate the blink state every half-second
        Timer blinkTimer = new Timer( 500, new ActionListener() {
        	public void actionPerformed( ActionEvent ae ) {
        		terminal.blinkState = !terminal.blinkState;
        		crt.repaint();
        	}
        });
        blinkTimer.start();
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
    	
    	int argNum = 0;
    	String arg;
    	while ( argNum < args.length && args[argNum].startsWith( "--" ) ) {
    		arg = args[argNum++];
    		
    		if (arg.equals( "--help" )) {
    			System.err.println( "java -cp DasherJ DasherJ [--help] [--host=<hostname>:<port>]" );
    			System.exit( 0 );
    		}
    		
    		if (arg.startsWith( "--host=" )) parseHost( arg );
    	}
    	    	
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });  
    }
    
}
