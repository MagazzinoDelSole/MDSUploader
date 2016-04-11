package it.magazzinodelsole.mdsuploader;


import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {

    /**
     * Logger
     */
    private static final Logger log = Logger.getLogger(Main.class);

    /**
     * Default configuration file name
     */
    private static final String DEFAULT_CONFIG_FILE = "configuration.properties";

    /**
     * Default logger configuration file
     */
    private static final String DEFAULT_LOG_CONFIG_FILE = "log.conf";

    /**
     * Properties that contains the configuration
     */
    private static final Properties config = new Properties();

    private static MySerial serial;

    private static MyFileUploader uploader;

    /**
     * Create the reader, prepare the uploader and listen for incoming data
     * @param args The first argument can be an alternative config file
     */
    public static void main (String[] args) {

        // Load the logger configuration
        PropertyConfigurator.configure(DEFAULT_LOG_CONFIG_FILE);

        // Register the shutdown interceptor
        registerShutdown();

        // Find the config file name
        String configFile = args.length > 0 ? args[0] : DEFAULT_CONFIG_FILE;

        try {

            // Load the configuration file
            config.load(new FileInputStream(new File(configFile)));

            // Apply the proxy setting
            loadProxy();
        } catch (IOException ex) {

            log.fatal("Cannot read the configuration file", ex);
            return;
        }

        try {

            // Open the serial port
            serial = MySerial.open(config.getProperty("serialPort"));

            // Prepare the uploader
            uploader = new MyFileUploader("", "");

            // Set the listener for the incoming data
            serial.setListener(new MySerial.DataListener() {
                public void onData(String dataTitle, byte[] data) {

                    // Save the file to the disk
                    File file = new File(config.getProperty("files/") + dataTitle);
                    try {

                        FileOutputStream toFile = new FileOutputStream(file);
                        toFile.write(data);

                        // Then upload the file to the server
                        uploader.uploadFile(file);
                    } catch (IOException ex) {

                        log.warn(ex);
                    }
                }
            });

            // Start the reader thread
            serial.start();
        } catch (UnsupportedCommOperationException e) {

            log.fatal("Cannot use serial port", e);
        } catch (MySerial.SerialPortNotFoundException e) {

            log.fatal("Cannot open serial port", e);
        } catch (PortInUseException e) {

            log.fatal("Serial port already in use", e);
        }
    }

    /**
     * Load and apply the proxy configuration reading the config preferences
     */
    private static void loadProxy () {

        // If the proxy is set
        if (config.getProperty("useProxy").equals("true")) {

            System.setProperty("http.proxyHost", config.getProperty("proxyIp"));
            System.setProperty("http.proxyPort", config.getProperty("proxyPort"));
        }
    }

    private static void registerShutdown () {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run () {

                log.info("Shutting down...");

                if (serial != null && serial.isListening()) {

                    try {

                        serial.shutdown();
                    } catch (InterruptedException ex) {

                        log.warn(ex);
                    }
                }
            }
        });
    }
}