package de.adEditor.routes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.adEditor.routes.dto.*;
import de.adEditor.routes.events.GetRoutesEvent;
import de.adEditor.routes.events.HttpClientEventListener;
import de.adEditor.routes.events.UploadCompletedEvent;
import de.autoDrive.NetworkServer.rest.RoutesRestPath;
import de.autoDrive.NetworkServer.rest.dto_v1.RouteDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointsResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RoutesManagerPanel extends JPanel {

    private static Logger LOG = LoggerFactory.getLogger(RoutesManagerPanel.class);

    private final static String directory = "/home/raupach/AutoDriveEditor_TestData/autoDrive/routesManager/";
    private final static String routesManagerPath = directory+ "routes.xml";
    private final static String routesDirectory = directory+ "routes/";

    private HttpClient httpClient = new HttpClient();
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

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
            public void getWaypoints(GetRoutesEvent evt) {
                WaypointsResponseDto waypointsResponseDto = (WaypointsResponseDto) evt.getSource();
                String filename = writeXmlRouteData(toRouteExport(waypointsResponseDto));

                AutoDriveRoutesManager autoDriveRoutesManager = readXmlRoutesMetaData();
                Route newRoute = new Route();
                newRoute.setName(waypointsResponseDto.getRoute().getName());
                newRoute.setRevision(waypointsResponseDto.getRoute().getRevision());
                newRoute.setMap(waypointsResponseDto.getRoute().getMap());
                SimpleDateFormat sdf = new SimpleDateFormat(RoutesRestPath.DATE_FORMAT);
                try {
                    newRoute.setDate(sdf.parse(waypointsResponseDto.getRoute().getDate()));
                } catch (ParseException e) {
                    LOG.error(e.getMessage(),e);
                }
                newRoute.setFileName(filename);
                newRoute.setServerId(waypointsResponseDto.getRoute().getId());
                Objects.requireNonNull(autoDriveRoutesManager).getRoutes().add(newRoute);
                writeXmlRoutesMetaData(autoDriveRoutesManager, filename);
            }

            @Override
            public void getUploadRouteResponse(UploadCompletedEvent evt) {

            }
        });

//        JToolBar toolBar = new JToolBar("Still draggable");
//        addButtons(toolBar);

//        setPreferredSize(new Dimension(450, 130));
//        add(toolBar, BorderLayout.PAGE_START);
        createTable();
        reloadServerRoutes();
    }

    private RouteExport toRouteExport(WaypointsResponseDto waypointsResponseDto) {
        RouteExport routeExport = new RouteExport();
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<Double> z = new ArrayList<>();
        List<String> out = new ArrayList<>();
        List<WaypointDto> waypointDtos = waypointsResponseDto.getWaypoints();
        Map<Integer,List<Integer>> inMap = new HashMap<>();

        waypointDtos.forEach(waypointDto -> {

            x.add(waypointDto.getX());
            y.add(waypointDto.getY());
            z.add(waypointDto.getZ());

            out.add(StringUtils.join(waypointDto.getOut(), ","));

            waypointDto.getOut().forEach(i ->{
                List<Integer> in = inMap.computeIfAbsent(i, k -> new ArrayList<>());
                in.add((waypointDtos.indexOf(waypointDto))+1);
            });

        });

        Waypoints waypoints = new Waypoints();
        waypoints.setC(waypointsResponseDto.getWaypoints().size());
        waypoints.setX(StringUtils.join(x, ";"));
        waypoints.setY(StringUtils.join(y, ";"));
        waypoints.setZ(StringUtils.join(z, ";"));
        waypoints.setOut(StringUtils.join(out, ";"));
        waypoints.setIn(StringUtils.join(inMap.values().stream().map(inIndex -> StringUtils.join(inIndex, ",")).collect(Collectors.toList()),";"));

        routeExport.setWaypoints(waypoints);
        routeExport.setGroups(waypointsResponseDto.getGroups().stream().map(g -> new Group(g.getName())).collect(Collectors.toList()));
        routeExport.setMarkers(waypointsResponseDto.getMarkers().stream().map(m -> new Marker(m.getName(), m.getGroup(), m.getWaypointIndex())).collect(Collectors.toList()));
        return routeExport;
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

    private void downloadFullRoute() {
        int rowIndex = remoteTable.getSelectedRow();
        AutoDriveRemoteRoutesTableModel model = (AutoDriveRemoteRoutesTableModel) remoteTable.getModel();
        RouteDto routeDto = model.get(rowIndex);
        downloadWaypointsForRoute(routeDto.getId());

    }


    private void downloadWaypointsForRoute(String routeId) {

        Runnable runnableTask = () -> {
            try {
                httpClient.getWaypoints(routeId);
            } catch (ExecutionException | InterruptedException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        };
        executorService.execute(runnableTask);
    }

    private void createTable() {

        JPanel panelLeft = createLeftPanel();
        JPanel panelRight = createRightPanel();

        AutoDriveRoutesManager autoDriveRoutesManager = readXmlRoutesMetaData();
        lokalTable = new JTable(new AutoDriveLocalRoutesTableModel(autoDriveRoutesManager != null ? autoDriveRoutesManager.getRoutes() : new ArrayList<>()));
        lokalTable.setComponentPopupMenu(createLocalRoutesPopupMenu());

        remoteTable = new JTable(new AutoDriveRemoteRoutesTableModel());
        remoteTable.setComponentPopupMenu(createRemoteRoutesPopupMenu());

        panelLeft.add(new JScrollPane(lokalTable));
        panelRight.add(new JScrollPane(remoteTable));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
        splitPane.setDividerLocation(0.5);
        add(splitPane);
    }

    private JPopupMenu createLocalRoutesPopupMenu() {
        JPopupMenu localPopupMenu = new JPopupMenu();

        JMenuItem menuItemUpload = new JMenuItem("Upload");
        menuItemUpload.addActionListener(e ->{
            startUploadRoute();
        });
        localPopupMenu.add(menuItemUpload);

        JMenuItem menuItemRefresh = new JMenuItem("Refresh");
        menuItemRefresh.addActionListener(e ->{
            AutoDriveRoutesManager autoDriveRoutesManager = readXmlRoutesMetaData();
            lokalTable.setModel(new AutoDriveLocalRoutesTableModel(autoDriveRoutesManager != null ? autoDriveRoutesManager.getRoutes() : new ArrayList<>()));
        });
        localPopupMenu.add(menuItemRefresh);

        return localPopupMenu;
    }

    private JPopupMenu createRemoteRoutesPopupMenu() {
        JPopupMenu localPopupMenu = new JPopupMenu();

        JMenuItem menuItemDownload = new JMenuItem("Download");
        menuItemDownload.addActionListener(e -> downloadFullRoute());
        localPopupMenu.add(menuItemDownload);

        JMenuItem menuItemRefresh = new JMenuItem("Refresh");
        menuItemRefresh.addActionListener(e ->reloadServerRoutes());
        localPopupMenu.add(menuItemRefresh);

        return localPopupMenu;
    }

    private JPanel createRightPanel() {
        JPanel panelRight= new JPanel();
        panelRight.setLayout(new BorderLayout());

        JPanel topBoxPanel = new JPanel();
        topBoxPanel.setLayout(new BoxLayout(topBoxPanel, BoxLayout.LINE_AXIS));
        topBoxPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 10));

        JLabel remoteText = new JLabel("Remote Routes:", JLabel.LEFT);
        topBoxPanel.add(remoteText);
        topBoxPanel.add(Box.createRigidArea(new Dimension(30, 0)));

        JButton refreshRemoteTable = new JButton();
        refreshRemoteTable.setIcon(new ImageIcon(getImageUrl("arrow_refresh.png"), "refresh"));
        topBoxPanel.add(refreshRemoteTable);
        topBoxPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        refreshRemoteTable.addActionListener(actionEvent -> reloadServerRoutes());

        JButton downloadRemoteRoute = new JButton();
        downloadRemoteRoute.setIcon(new ImageIcon(getImageUrl("arrow_down.png"), "download"));
        topBoxPanel.add(downloadRemoteRoute);

        panelRight.add(topBoxPanel, BorderLayout.NORTH);

        return panelRight;
    }


    private JPanel createLeftPanel() {
        JPanel panelRight= new JPanel();
        panelRight.setLayout(new BorderLayout());

        JPanel topBoxPanel = new JPanel();
        topBoxPanel.setLayout(new BoxLayout(topBoxPanel, BoxLayout.LINE_AXIS));
        topBoxPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 10));

        JLabel remoteText = new JLabel("Lokal Routes:", JLabel.LEFT);
        topBoxPanel.add(remoteText);
        topBoxPanel.add(Box.createRigidArea(new Dimension(30, 0)));

        JButton refreshLocalTable = new JButton();
        refreshLocalTable.setIcon(new ImageIcon(getImageUrl("arrow_refresh.png"), "refresh"));
        topBoxPanel.add(refreshLocalTable);
        topBoxPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        refreshLocalTable.addActionListener(actionEvent -> {
            AutoDriveRoutesManager autoDriveRoutesManager = readXmlRoutesMetaData();
            lokalTable.setModel(new AutoDriveLocalRoutesTableModel(autoDriveRoutesManager != null ? autoDriveRoutesManager.getRoutes() : new ArrayList<>()));
        });

        JButton downloadRemoteRoute = new JButton();
        downloadRemoteRoute.setIcon(new ImageIcon(getImageUrl("arrow_up.png"), "upload"));
        topBoxPanel.add(downloadRemoteRoute);

        panelRight.add(topBoxPanel, BorderLayout.NORTH);

        return panelRight;
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

    private URL getImageUrl(String imageName) {
        String imgLocation = "/icons/" +imageName;
        return RoutesManagerPanel.class.getResource(imgLocation);
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

    private void writeXmlRoutesMetaData(AutoDriveRoutesManager autoDriveRoutesManager, String filename) {

        try {
            ObjectMapper mapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            mapper.writeValue(new File(routesManagerPath), autoDriveRoutesManager);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
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

    private String writeXmlRouteData(RouteExport routeExport) {

        try {
            String fileName = UUID.randomUUID().toString()+".xml";
            ObjectMapper mapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            mapper.writeValue(new File(routesDirectory + fileName), routeExport);
            return fileName;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private void startUploadRoute() {
        int row = lokalTable.getSelectedRow();
        AutoDriveLocalRoutesTableModel model = (AutoDriveLocalRoutesTableModel) lokalTable.getModel();
        Route route = model.get(row);
        RouteExport routeExport = readXmlRouteData(route.getFileName());

        Runnable runnableTask = () -> {
            try {
                httpClient.upload(routeExport, route.getName(), route.getMap(), route.getRevision(), route.getDate());
            } catch (ExecutionException | InterruptedException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        };
        executorService.execute(runnableTask);
    }

}
