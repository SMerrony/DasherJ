# DasherJ
DG DASHER Terminal Emulator (Java version, see also https://github.com/SMerrony/DasherQ)

DasherJ is a free terminal emulator for the Data General DASHER-series terminals.  From v0.9 it is written using JavaFX with the [JSSC serial library](https://github.com/scream3r/java-simple-serial-connector) and should run on all desktop platforms supporting Java 8.

## Key Features

* Serial interface support at 300, 1200, 9600 & 19200 baud, 7 or 8 data bits (defaults to DG defaults of 9600,8,n,1)
* Network Interface (Telnet) support
* Dasher D200 & D210 Emulation
* Resizable to many useful row/column combinations, three levels of zoom
* 15 (plus Ctrl & Shift) Dasher Function keys, Hold, Local Print, Erase Page, Erase EOL and Cmd-Break keys
* Reverse video, blinking, dim and underlined characters
* Pixel-for-pixel copy of D410 character set
* Session logging to file
* Loadable function-key templates (BROWSE, SED and SMI provided as examples)

## Using DasherJ

From v0.9 you may pass a ```--host=<hostname>:<port>``` option to the program to automatically connect to a telnet server.

### Function Keys
Use the keys simulated on the toolbar in DasherJ - your OS will probably interfere with the F-keys on your keyboard.  
The Shift and Control keys can be used in conjunction with the simulated F-keys just like a real Dasher.  
The "Brk" key sends a Command-Break signal to the host when connected via the serial interface.  
"Hold" and "Local Print" work as you would expect.

### Emulation Details
[See this chart](http://stephenmerrony.co.uk/dg/uploads/Documentation/Third-Party/ImplementationChart.pdf)
