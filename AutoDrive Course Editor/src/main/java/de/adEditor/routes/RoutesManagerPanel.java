package de.adEditor.routes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.adEditor.routes.dto.AutoDriveRoutesManager;
import de.adEditor.routes.dto.Route;
import de.adEditor.routes.dto.RouteExport;
import de.adEditor.routes.events.GetRoutesEvent;
import de.adEditor.routes.events.HttpClientEventListener;
import de.adEditor.routes.events.UploadCompletedEvent;
import de.autoDrive.NetworkServer.rest.dto_v1.RouteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoutesManagerPanel extends JPanel implements ActionListener {

    private static Logger LOG = LoggerFactory.getLogger(RoutesManagerPanel.class);

    private final static String directory = "/home/raupach/AutoDriveEditor_TestData/autoDrive/routesManager/";
    private final static String routesManagerPath = directory+ "routes.xml";
    private final static String routesDirectory = directory+ "routes/";

    private HttpClient httpClient = new HttpClient();
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    private JTable lokalTable;
    private JTable remoteTable;

    public RoutesManagerPanel() {
        super(new BorderLayout());

        httpClient.addMyEventListener(new HttpClientEventListener() {
            @Override
            public void getRoutes(GetRoutesEvent evt) {
                remoteTable.setModel(new AutoDriveRemoteRoutesTableModel((List<RouteDto>) evt.getSource()));
            }

            @Override
            public void getUploadRouteResponse(UploadCompletedEvent evt) {
                int y=1;
            }
        });

        JToolBar toolBar = new JToolBar("Still draggable");
        addButtons(toolBar);

//        setPreferredSize(new Dimension(450, 130));
//        add(toolBar, BorderLayout.PAGE_START);
        createTable();
        reloadServerRoutes();
    }

    private void reloadServerRoutes() {

        Runnable runnableTask = () -> {
            try {
                httpClient.getRoutes();
            } catch (ExecutionException | InterruptedException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        };
        executorService.execute(runnableTask);
    }


    private void createTable() {

        AutoDriveRoutesManager autoDriveRoutesManager = readXmlRoutesMetaData();
        AutoDriveRoutesTableModel autoDriveRoutesTableModel = new AutoDriveRoutesTableModel(autoDriveRoutesManager.getRoutes());

        JPanel panelLeft = new JPanel();
        panelLeft.setLayout(new BorderLayout());
        JLabel lokalText = new JLabel("Lokal Routes:", JLabel.CENTER);
        lokalText.setPreferredSize(new Dimension(200, 50));
        panelLeft.add(lokalText, BorderLayout.NORTH);

        JPanel panelRight= new JPanel();
        panelRight.setLayout(new BorderLayout());
        JLabel remoteText = new JLabel("Remote Routes:", JLabel.CENTER);
        remoteText.setPreferredSize(new Dimension(200, 50));
        panelRight.add(remoteText, BorderLayout.NORTH);


        JPopupMenu localPopupMenu = new JPopupMenu();
        JMenuItem menuItemUpload = new JMenuItem("Upload");
        menuItemUpload.setActionCommand("UPLOAD");
        menuItemUpload.addActionListener(this);
        localPopupMenu.add(menuItemUpload);

        lokalTable = new JTable(autoDriveRoutesTableModel);
        lokalTable.setComponentPopupMenu(localPopupMenu);

        remoteTable = new JTable(new AutoDriveRemoteRoutesTableModel());

        panelLeft.add(new JScrollPane(lokalTable));
        panelRight.add(new JScrollPane(remoteTable));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
        splitPane.setDividerLocation(0.5);
        add(splitPane);
    }


    protected void addButtons(JToolBar toolBar) {
        JButton button = null;

        //first button
        button = makeNavigationButton("arrow_refresh", "PREVIOUS",
                "Back to previous something-or-other",
                "Previous");
        toolBar.add(button);

        //second button
        button = makeNavigationButton("arrow_up", "UP",
                "Up to something-or-other",
                "Up");
        toolBar.add(button);


    }

    protected JButton makeNavigationButton(String imageName,
                                           String actionCommand,
                                           String toolTipText,
                                           String altText) {
        //Look for the image.
        String imgLocation = "/" +imageName+ ".png";
        URL imageURL = RoutesManagerPanel.class.getResource(imgLocation);

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
//        button.addActionListener(this);

        if (imageURL != null) {                      //image found
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {                                     //no image found
            button.setText(altText);
            System.err.println("Resource not found: " + imgLocation);
        }

        return button;
    }


    private void checkAndLoadProperties (){
        File configFile = new File("adEditor.config");
        if (configFile.exists()) {
            try {
                FileReader reader = new FileReader(configFile);
                Properties props = new Properties();
                props.load(reader);

                String host = props.getProperty("routesPath");

                System.out.print("Host name is: " + host);
                reader.close();
            } catch (FileNotFoundException ex) {
                // file does not exist
            } catch (IOException ex) {
                // I/O error
            }
        }
        else {
            JFileChooser fc = new JFileChooser();

            fc.setDialogTitle("Select Route Manager Directory");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
//            FileNameExtensionFilter filter = new FileNameExtensionFilter("AutoDrive config", "xml");
//            fc.addChoosableFileFilter(filter);
            int returnVal = fc.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File fileName = fc.getSelectedFile();
               int x=1;
            }
        }
    }


    private AutoDriveRoutesManager readXmlRoutesMetaData() {

        try {
            ObjectMapper mapper = new XmlMapper();
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return mapper.readValue(new File(routesManagerPath), AutoDriveRoutesManager.class);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private RouteExport readXmlRouteData(String fileName) {

        try {
            ObjectMapper mapper = new XmlMapper();
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return mapper.readValue(new File(routesDirectory + fileName), RouteExport.class);

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getActionCommand().equals("UPLOAD")) {
            int row = lokalTable.getSelectedRow();
            String fileName = (String) lokalTable.getValueAt(row, 1);
            RouteExport routeExport = readXmlRouteData(fileName);

            Runnable runnableTask = () -> {
                try {
                    httpClient.upload(routeExport, (String)lokalTable.getValueAt(row, 0), (String)lokalTable.getValueAt(row, 2), (Integer)lokalTable.getValueAt(row, 3), (Date)lokalTable.getValueAt(row, 4));
                } catch (ExecutionException | InterruptedException | IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            };
            executorService.execute(runnableTask);

        }
    }

}
