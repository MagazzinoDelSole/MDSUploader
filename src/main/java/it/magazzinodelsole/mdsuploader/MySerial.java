package it.magazzinodelsole.mdsuploader;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;

public class MySerial extends Thread {

    /**
     * Logger
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

    /**
     * Open the specified serial port
     * @param name Name of the serial port
     * @return Instance of MySerial
     * @throws SerialPortNotFoundException thrown if the specified serial port is not available
     * @throws PortInUseException thrown if the specified port is already in use
     * @throws UnsupportedCommOperationException thrown if the port doesn't support the default configuration
     */
    public static MySerial open (String name) throws SerialPortNotFoundException, PortInUseException, UnsupportedCommOperationException {

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
    private SerialPort rawPort;

    /**
     * Listener to call when data is retrived from the serial
     */
    private DataListener listener;

    /**
     * Input stream from the serial
     */
    private InputStream fromSerial;

    private volatile boolean shutdown;

    private CountDownLatch countDownLatch;

    private MySerial (SerialPort rawPort) {

        this.rawPort = rawPort;
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

        int state;

        while (!shutdown) {

            try {

                // If data is coming from the serial port...
                if(fromSerial.available() > 0) {

                    // Read the data
                    int read = fromSerial.read(buffer);

                    // Iterate each byte
                    for(byte b : read) {

                    }

                } else {

                    // Otherwise sleep
                    Thread.sleep(DEFAULT_INTERVAL);
                }
            } catch (IOException ex) {

                log.warn(ex);
            } catch (InterruptedException ex) {

                // Who cares
                log.log(ex);
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

        // Chjange the flag
        shutdown = true;

        // Wait for the end of the thread
        countDownLatch.await();
    }

    /**
     * Interface of the data listener
     */
    public static interface DataListener {

        /**
         * Method called when the data is received from the serial
         * @param dataTitle Title of the data
         * @param data Bytes of data
         */
        void onData (String dataTitle, byte[] data);
    }
}
