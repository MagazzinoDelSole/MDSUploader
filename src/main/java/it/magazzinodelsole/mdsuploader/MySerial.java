package it.magazzinodelsole.mdsuploader;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

public class MySerial extends Thread {

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(MySerial.class);

    /**
     * Default serial baud rate
     */
    public static final int DEFAULT_BAUD_RATE = 9600;

    /**
     * Default interval for the serial reading timeout
     */
    public static final int DEFAULT_INTERVAL = 2000;

    private static final int READING_TITLE = 1;

    private static final int READING_DATA = 2;

    /**
     * Open the specified serial port
     * @param name Name of the serial port
     * @return Instance of MySerial
     * @throws SerialPortNotFoundException thrown if the specified serial port is not available
     * @throws PortInUseException thrown if the specified port is already in use
     * @throws UnsupportedCommOperationException thrown if the port doesn't support the default configuration
     */
    public static MySerial open (String name) throws SerialPortNotFoundException, PortInUseException, UnsupportedCommOperationException, IOException {

        // Get the available ports
        Enumeration ports = CommPortIdentifier.getPortIdentifiers();

        // Iterate each port
        while (ports.hasMoreElements()) {

            // Get the identifier of this port
            CommPortIdentifier identifier = (CommPortIdentifier) ports.nextElement();

            // Check if the name is the selected one
            if(identifier.getName().equals(name)) {

                // Open the port
                gnu.io.SerialPort serial = (SerialPort) identifier.open("mds", 2000);

                // Set the connection parameters
                serial.setSerialPortParams(DEFAULT_BAUD_RATE, SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1, SerialPort.PARITY_NONE
                );

                // Create a new MySerial
                MySerial mySerial = new MySerial(serial);
            }
        }

        throw new SerialPortNotFoundException(name);
    }

    /**
     * check if the thread is running
     * @return Listening state
     */
    public boolean isListening() {
        return !shutdown;
    }

    /**
     * Exception throw by the open method when the specified serial is not available
     */
    public static class SerialPortNotFoundException extends Exception {

        /**
         * Name of the non available port
         */
        private String portName;

        public SerialPortNotFoundException (String portName) {
            this.portName = portName;
        }

        public String getPortName () {
            return portName;
        }
    }

    /**
     * Raw serial object from the rxtx library
     */
    private final SerialPort rawPort;

    /**
     * Listener to call when data is retrieved from the serial
     */
    private DataListener listener;

    /**
     * Input stream from the serial
     */
    private InputStream fromSerial;

    private volatile boolean shutdown;

    private CountDownLatch countDownLatch;

    /**
     * Create a new MySerial from the serial specified.
     * @param rawPort
     * @throws IOException Thrown while getting the input stream from the serial
     */
    private MySerial (SerialPort rawPort) throws IOException {

        this.rawPort = rawPort;

        // Get the input stream
        this.fromSerial = rawPort.getInputStream();
    }

    /**
     * Set the listener for this serial
     * @param listener Listener to call when the data is retrieved
     */
    public void setListener (DataListener listener) {

        this.listener = listener;
    }

    /**
     * Start a new thread that will be used to read data from the serial
     */
    @Override
    public void start () {

        // Change the flag
        shutdown = false;

        // Prepare the count down latch
        countDownLatch = new CountDownLatch(1);

        // Prepare the buffer from the serial
        byte[] buffer = new byte[255];

        // String builder for the title
        StringBuilder titleBuilder = new StringBuilder();

        // Array list for the temporary data
        LinkedList<Byte> data = new LinkedList<>();

        // Reading state
        int state = -1;

        while (!shutdown) {

            try {

                // If data is coming from the serial port...
                if(fromSerial.available() > 0) {

                    // Read the data
                    int read = fromSerial.read(buffer);

                    // Iterate each byte
                    for (int i = 0;i < read; i++) {

                        switch (buffer[i]) {

                            case 'd': // Title of the data

                                // clear the builder
                                titleBuilder.delete(0, titleBuilder.length());

                                // Change the state
                                state = READING_TITLE;
                                break;
                            case 'n': // New line

                                // Append /n to the data
                                titleBuilder.append("/n");
                                break;
                            case 'i': // Start the data

                                // Clear the array of the data
                                data.clear();

                                // Change the state
                                state = READING_DATA;
                                break;
                            case 'f': // End

                                // Build the title
                                String title = titleBuilder.toString();

                                // Transform the data into a new array
                                byte[] dataArray = new byte[data.size()];

                                // Copy all the byte
                                int j = 0;
                                for(Byte b : data)
                                        dataArray[j++] = b;

                                // Call the listener
                                listener.onData(title, dataArray);
                                break;
                            default:
                                if (state == READING_TITLE) {

                                    // add the character to the title
                                    titleBuilder.append(buffer[i]);
                                } else {

                                    // Add the byte to the data
                                    data.add(buffer[i]);
                                }
                        }
                    }

                } else {

                    // Otherwise sleep
                    Thread.sleep(DEFAULT_INTERVAL);
                }
            } catch (IOException ex) {

                log.warn(ex);
            } catch (InterruptedException ex) {

                // Who cares
                log.warn(ex);
            }
        }

        // Close the shutdown procedure
        countDownLatch.countDown();
    }

    /**
     * Stop the reading thread
     * @throws InterruptedException
     */
    public void shutdown () throws InterruptedException {

        // check if the listener is running
        if(!shutdown)
            throw new IllegalStateException("The listener is not running");

        // Change the flag
        shutdown = true;

        // Wait for the end of the thread
        countDownLatch.await();
    }

    /**
     * Interface of the data listener
     */
    public interface DataListener {

        /**
         * Method called when the data is received from the serial
         * @param dataTitle Title of the data
         * @param data Bytes of data
         */
        void onData (String dataTitle, byte[] data);
    }

    /**
     * Release the serial port
     */
    public void release () {

        if(!shutdown)
            throw new IllegalStateException("Reading thread is running");

        rawPort.close();
    }
}
