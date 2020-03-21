package de.adEditor;

import com.formdev.flatlaf.FlatLightLaf;
import de.adEditor.config.AdConfiguration;
import de.adEditor.config.ConfigDialog;
import de.adEditor.helper.IconHelper;
import de.adEditor.routes.RoutesManagerPanel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class AutoDriveEditor extends JFrame {
    public static final int EDITORSTATE_NOOP = -1;
    public static final int EDITORSTATE_MOVING = 0;
    public static final int EDITORSTATE_DELETING = 1;
    public static final int EDITORSTATE_CONNECTING = 2;
    public static final int EDITORSTATE_CREATING = 3;
    public static final int EDITORSTATE_DELETING_DESTINATION = 4;
    public static final int EDITORSTATE_CREATING_DESTINATION = 5;
    public static final String MOVE_NODES = "Move Nodes";
    public static final String CONNECT_NODES = "Connect Nodes";
    public static final String REMOVE_NODES = "Remove Nodes";
    public static final String REMOVE_DESTINATIONS = "Remove Destinations";
    public static final String CREATE_NODES = "Create Nodes";
    public static final String CREATE_DESTINATIONS = "Create Destinations";
    public static final String AUTO_DRIVE_COURSE_EDITOR_TITLE = "AutoDrive Course Editor 0.1";

    private MapPanel mapPanel;
    private RoutesManagerPanel routesManagerPanel;
    private JButton saveButton;
    private JButton loadImageButton;
    private JToggleButton removeNode;
    private JToggleButton removeDestination;
    private JToggleButton moveNode;
    private JToggleButton connectNodes;
    private JToggleButton createNode;
    private JToggleButton createDestination;
    private JRadioButton oneTimesMap;
    private JRadioButton fourTimesMap;
    private JRadioButton sixteenTimesMap;
    private JTabbedPane tabPane;

    public int editorState = EDITORSTATE_NOOP;
    private File xmlConfigFile;
    private boolean stale = false;

    private static Logger LOG = LoggerFactory.getLogger(AutoDriveEditor.class);


    public AutoDriveEditor() {
        super();

        LOG.info("AutoDrive start.............................................................................................");
        setTitle(createTitle());
        setIconImage(loadIcon("/tractor.png"));
        setJMenuBar(createMenuBar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isStale()) {
                    int response = JOptionPane.showConfirmDialog(null, "There are unsaved changes. Should they be saved now?", "AutoDrive", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (response == JOptionPane.YES_OPTION) {
                        saveMap();
                    }
                }
                super.windowClosing(e);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                checkAndLoadProperties();
            }

        });


        tabPane = new JTabbedPane (JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT );
        add(tabPane);
        JPanel editorPanel = new JPanel();
        routesManagerPanel = new RoutesManagerPanel();
        tabPane.addTab("Course Editor", new ImageIcon(IconHelper.getImageUrl("note_edit.png")) , editorPanel);
        tabPane.addTab("Network Manager", new ImageIcon(IconHelper.getImageUrl("note_go.png")), routesManagerPanel);

        editorPanel.setLayout(new BorderLayout());

        // create a new panel with GridBagLayout manager
        mapPanel = new MapPanel(this);

        // set border for the panel
        mapPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Map Panel"));
        mapPanel.setPreferredSize(new Dimension(1024, 768));
        mapPanel.setMinimumSize(new Dimension(1024, 768));

        // add the panel to this frame
        editorPanel.add(mapPanel, BorderLayout.CENTER);

        EditorListener editorListener = new EditorListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JPanel configBox = new JPanel();
        configBox.setBorder(BorderFactory.createTitledBorder("Config"));
        buttonPanel.add(configBox);

        JButton loadRoadMapButton = new JButton("Load");
        loadRoadMapButton.addActionListener(editorListener);
        loadRoadMapButton.setActionCommand("Load");
        configBox.add(loadRoadMapButton);

        saveButton = new JButton("Save");
        saveButton.addActionListener(editorListener);
        saveButton.setActionCommand("Save");
        saveButton.setEnabled(false);
        configBox.add(saveButton);

        JPanel mapBox = new JPanel();
        mapBox.setBorder(BorderFactory.createTitledBorder("Map and zoom factor"));
        buttonPanel.add(mapBox);

        loadImageButton = new JButton("Load Map");
        loadImageButton.addActionListener(editorListener);
        loadImageButton.setActionCommand("Load Image");
        mapBox.add(loadImageButton);

        ButtonGroup zoomGroup = new ButtonGroup();
        oneTimesMap = new JRadioButton(" 1x");
        oneTimesMap.addActionListener(editorListener);
        oneTimesMap.setActionCommand("OneTimesMap");
        oneTimesMap.setSelected(true);
        mapBox.add(oneTimesMap);
        zoomGroup.add(oneTimesMap);

        fourTimesMap = new JRadioButton(" 4x");
        fourTimesMap.addActionListener(editorListener);
        fourTimesMap.setActionCommand("FourTimesMap");
        mapBox.add(fourTimesMap);
        zoomGroup.add(fourTimesMap);

        sixteenTimesMap = new JRadioButton(" 16x");
        sixteenTimesMap.addActionListener(editorListener);
        sixteenTimesMap.setActionCommand("SixteenTimesMap");
        mapBox.add(sixteenTimesMap);
        zoomGroup.add(sixteenTimesMap);

        JPanel nodeBox = new JPanel();
        nodeBox.setBorder(BorderFactory.createTitledBorder("Nodes"));
        buttonPanel.add(nodeBox);

        moveNode = new JToggleButton("Move Nodes");
        moveNode.addActionListener(editorListener);
        moveNode.setActionCommand(MOVE_NODES);
        nodeBox.add(moveNode);

        connectNodes = new JToggleButton("Connect Nodes");
        connectNodes.addActionListener(editorListener);
        connectNodes.setActionCommand(CONNECT_NODES);
        connectNodes.setName(CONNECT_NODES);
        nodeBox.add(connectNodes);

        removeNode = new JToggleButton("Delete Nodes");
        removeNode.addActionListener(editorListener);
        removeNode.setActionCommand(REMOVE_NODES);
        nodeBox.add(removeNode);

        removeDestination = new JToggleButton("Delete Destination");
        removeDestination.addActionListener(editorListener);
        removeDestination.setActionCommand(REMOVE_DESTINATIONS);
        nodeBox.add(removeDestination);

        createNode = new JToggleButton("Create Node");
        createNode.addActionListener(editorListener);
        createNode.setActionCommand(CREATE_NODES);
        nodeBox.add(createNode);

        createDestination = new JToggleButton("Create Destination");
        createDestination.addActionListener(editorListener);
        createDestination.setActionCommand(CREATE_DESTINATIONS);
        nodeBox.add(createDestination);

        updateButtons();
        nodeBoxSetEnabled(false);
        mapBoxSetEnabled(false);

        editorPanel.add(buttonPanel, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Editor");
        menuBar.add(menu);

        JMenuItem menuConfigItem = new JMenuItem("Configuration");
        menuConfigItem.addActionListener(e ->{
            showConfigDialog();
            enableManagerPanel();
        });
        menu.add(menuConfigItem);

        JMenuItem menuQuitItem = new JMenuItem("Quit");
        menuQuitItem.addActionListener(e -> System.exit(0));
        menu.add(menuQuitItem);

        return menuBar;
    }

    private BufferedImage loadIcon(String name) {
        try {
            URL url = AutoDriveEditor.class.getResource(name);
            if (url != null) {
                return ImageIO.read(url);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    private void nodeBoxSetEnabled(boolean enabled) {
        moveNode.setEnabled(enabled);
        connectNodes.setEnabled(enabled);
        removeNode.setEnabled(enabled);
        removeDestination.setEnabled(enabled);
        createNode.setEnabled(enabled);
        createDestination.setEnabled(enabled);
    }

    private void mapBoxSetEnabled(boolean enabled) {
        loadImageButton.setEnabled(enabled);
        oneTimesMap.setEnabled(enabled);
        fourTimesMap.setEnabled(enabled);
        sixteenTimesMap.setEnabled(enabled);
    }

    public void updateButtons() {
        moveNode.setSelected(false);
        connectNodes.setSelected(false);
        removeNode.setSelected(false);
        removeDestination.setSelected(false);
        createNode.setSelected(false);
        createDestination.setSelected(false);

        switch (editorState) {
            case EDITORSTATE_MOVING:
                moveNode.setSelected(true);
                break;
            case EDITORSTATE_DELETING:
                removeNode.setSelected(true);
                break;
            case EDITORSTATE_CONNECTING:
                connectNodes.setSelected(true);
                break;
            case EDITORSTATE_CREATING:
                createNode.setSelected(true);
                break;
            case EDITORSTATE_DELETING_DESTINATION:
                removeDestination.setSelected(true);
                break;
            case EDITORSTATE_CREATING_DESTINATION:
                createDestination.setSelected(true);
                break;
        }
    }

    public static void main(String[] args) {
        // set look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        SwingUtilities.invokeLater(() -> {
            GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
            new AutoDriveEditor().setVisible(true);
        });
    }


    public void loadConfigFile(File fXmlFile) {
        LOG.info("loadFile: {}", fXmlFile.getAbsolutePath());

        try {
            mapPanel.setRoadMap(loadXmlConfigFile(fXmlFile));
            setTitle(AUTO_DRIVE_COURSE_EDITOR_TITLE + " - " + fXmlFile.getAbsolutePath());
            xmlConfigFile = fXmlFile;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "The AutoDrive Config could not be loaded.", "AutoDrive", JOptionPane.ERROR_MESSAGE);
        }
    }

    private RoadMap loadXmlConfigFile(File fXmlFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();

        LOG.info("Root element :{}", doc.getDocumentElement().getNodeName());

        NodeList markerList = doc.getElementsByTagName("mapmarker");
        LinkedList<MapMarker> mapMarkers = new LinkedList<>();

        TreeMap<Integer, MapMarker> mapMarkerTree = new TreeMap<>();
        for (int temp = 0; temp < markerList.getLength(); temp++) {
            Node markerNode = markerList.item(temp);
            if (markerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) markerNode;

                NodeList idNodeList = eElement.getElementsByTagName("id");
                NodeList nameNodeList = eElement.getElementsByTagName("name");
                NodeList groupNodeList = eElement.getElementsByTagName("group");

                for (int markerIndex = 0; markerIndex<idNodeList.getLength(); markerIndex++ ) {
                    Node node = idNodeList.item(markerIndex).getChildNodes().item(0);
                    String markerNodeId = node.getNodeValue();

                    node = nameNodeList.item(markerIndex).getChildNodes().item(0);
                    String markerName = node.getNodeValue();

                    node = groupNodeList.item(markerIndex).getChildNodes().item(0);
                    String markerGroup = node.getNodeValue();

                    MapNode dummyNode = new MapNode((int)Double.parseDouble(markerNodeId), 0, 0, 0);
                    MapMarker mapMarker = new MapMarker(dummyNode, markerName, markerGroup);
                    mapMarkerTree.put((int)Double.parseDouble(markerNodeId), mapMarker);
                }
            }
        }

        NodeList nList = doc.getElementsByTagName("waypoints");

        LinkedList<MapNode> nodes = new LinkedList<>();
        for (int temp = 0; temp < nList.getLength(); temp++) {

            Node nNode = nList.item(temp);

            LOG.info("Current Element :{}", nNode.getNodeName());

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;

                NodeList nodeList = eElement.getElementsByTagName("id").item(0).getChildNodes();
                Node node = nodeList.item(0);
                String idString = node.getNodeValue();
                String[] ids = idString.split(",");

                nodeList = eElement.getElementsByTagName("x").item(0).getChildNodes();
                node = nodeList.item(0);
                String xString = node.getNodeValue();
                String[] xValues = xString.split(",");

                nodeList = eElement.getElementsByTagName("y").item(0).getChildNodes();
                node = nodeList.item(0);
                String yString = node.getNodeValue();
                String[] yValues = yString.split(",");

                nodeList = eElement.getElementsByTagName("z").item(0).getChildNodes();
                node = nodeList.item(0);
                String zString = node.getNodeValue();
                String[] zValues = zString.split(",");

                nodeList = eElement.getElementsByTagName("out").item(0).getChildNodes();
                node = nodeList.item(0);
                String outString = node.getNodeValue();
                String[] outValueArrays = outString.split(";");

                nodeList = eElement.getElementsByTagName("incoming").item(0).getChildNodes();
                node = nodeList.item(0);
                String incomingString = node.getNodeValue();
                String[] incomingValueArrays = incomingString.split(";");

                for (int i=0; i<ids.length; i++) {
                    int id = Integer.parseInt(ids[i]);
                    double x = Double.parseDouble(xValues[i]);
                    double y = Double.parseDouble(yValues[i]);
                    double z = Double.parseDouble(zValues[i]);

                    MapNode mapNode = new MapNode(id, x, y, z);
                    nodes.add(mapNode);
                }


                for (Map.Entry<Integer, MapMarker> entry : mapMarkerTree.entrySet())
                {
                    mapMarkers.add(new MapMarker(nodes.get(entry.getKey()-1), entry.getValue().name, entry.getValue().group));
                }

                for (int i=0; i<ids.length; i++) {
                    MapNode mapNode = nodes.get(i);
                    String[] outNodes = outValueArrays[i].split(",");
                    for (int out=0; out<outNodes.length; out++) {
                        if (Integer.parseInt(outNodes[out]) != -1) {
                            mapNode.outgoing.add(nodes.get(Integer.parseInt(outNodes[out]) - 1));
                        }
                    }
                }

                for (int i=0; i<ids.length; i++) {
                    MapNode mapNode = nodes.get(i);
                    String[] incomingNodes = incomingValueArrays[i].split(",");
                    for (int incoming=0; incoming<incomingNodes.length; incoming++) {
                        if (Integer.parseInt(incomingNodes[incoming]) != -1) {
                            mapNode.incoming.add(nodes.get(Integer.parseInt(incomingNodes[incoming])-1));
                        }
                    }
                }
            }
        }

        RoadMap roadMap = new RoadMap();
        roadMap.mapNodes = nodes;
        roadMap.mapMarkers = mapMarkers;

        Node ADNode = doc.getElementsByTagName("AutoDrive").item(0);
        Node mapNameNode = nList.item(0).getParentNode();
        String mapName = mapNameNode.getNodeName();
        LOG.info("Loaded config for map: {}", mapNameNode.getNodeName());

        String mapPath = "/mapImages/" + mapName + ".png";
        URL url = AutoDriveEditor.class.getResource(mapPath);

        BufferedImage image = null;
        try {
            image = ImageIO.read(url);
        } catch (Exception e) {
            try {
                mapPath = "./mapImages/" + mapName + ".png";
                image = ImageIO.read(new File(mapPath));
            } catch (Exception e1) {
                try {
                    mapPath = "./src/mapImages/" + mapName + ".png";
                    image = ImageIO.read(new File(mapPath));
                } catch (Exception e2) {
                    try {
                        mapPath = "./" + mapName + ".png";
                        image = ImageIO.read(new File(mapPath));
                    } catch (Exception e3) {
                        mapBoxSetEnabled(true);
                        LOG.info("Editor has no map file for map: {}", mapName);
                    }
                }
            }
        }

        if (image != null) {
            mapPanel.setImage(image);
        }

        if (mapPanel.getImage() != null) {
            mapPanel.setPreferredSize(new Dimension(1024, 768));
            mapPanel.setMinimumSize(new Dimension(1024, 768));
            pack();
            repaint();
            mapPanel.repaint();
        }

        saveButton.setEnabled(true);
        nodeBoxSetEnabled(true);
        editorState = EDITORSTATE_MOVING;
        updateButtons();

        LOG.info("loadFile end.");
        return roadMap;

    }

    public void saveMap() {
        LOG.info("SaveMap called");
        RoadMap roadMap = mapPanel.getRoadMap();

        try
        {
            saveXmlConfig(xmlConfigFile, roadMap);
            setStale(false);
            JOptionPane.showMessageDialog(this, xmlConfigFile.getName() + " has been successfully saved.", "AutoDrive", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "The AutoDrive Config could not be saved.", "AutoDrive", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveXmlConfig(File file, RoadMap roadMap) throws ParserConfigurationException, IOException, SAXException, TransformerException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);

        Node AutoDrive = doc.getFirstChild();
        Element root = doc.getDocumentElement();

        Node waypoints = doc.getElementsByTagName("waypoints").item(0);

        // loop the staff child node
        NodeList list = waypoints.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);

            if ("id".equals(node.getNodeName())) {
                StringBuilder ids = new StringBuilder();
                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
                    MapNode mapNode = roadMap.mapNodes.get(j);
                    ids.append(mapNode.id);
                    if (j < (roadMap.mapNodes.size() - 1)) {
                        ids.append(",");
                    }
                }
                node.setTextContent(ids.toString());
            }
            if ("x".equals(node.getNodeName())) {
                StringBuilder xPositions = new StringBuilder();
                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
                    MapNode mapNode = roadMap.mapNodes.get(j);
                    xPositions.append(mapNode.x);
                    if (j < (roadMap.mapNodes.size() - 1)) {
                        xPositions.append(",");
                    }
                }
                node.setTextContent(xPositions.toString());
            }
            if ("y".equals(node.getNodeName())) {
                StringBuilder yPositions = new StringBuilder();
                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
                    MapNode mapNode = roadMap.mapNodes.get(j);
                    yPositions.append(mapNode.y);
                    if (j < (roadMap.mapNodes.size() - 1)) {
                        yPositions.append(",");
                    }
                }
                node.setTextContent(yPositions.toString());
            }
            if ("z".equals(node.getNodeName())) {
                StringBuilder zPositions = new StringBuilder();
                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
                    MapNode mapNode = roadMap.mapNodes.get(j);
                    zPositions.append(mapNode.z);
                    if (j < (roadMap.mapNodes.size() - 1)) {
                        zPositions.append(",");
                    }
                }
                node.setTextContent(zPositions.toString());
            }
            if ("incoming".equals(node.getNodeName())) {
                StringBuilder incomingString = new StringBuilder();
                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
                    MapNode mapNode = roadMap.mapNodes.get(j);
                    StringBuilder incomingsPerNode = new StringBuilder();
                    for (int incomingIndex = 0; incomingIndex < mapNode.incoming.size(); incomingIndex++) {
                        MapNode incomingNode = mapNode.incoming.get(incomingIndex);
                        incomingsPerNode.append(incomingNode.id);
                        if (incomingIndex < (mapNode.incoming.size() - 1)) {
                            incomingsPerNode.append(",");
                        }
                    }
                    if (incomingsPerNode.toString().isEmpty()) {
                        incomingsPerNode = new StringBuilder("-1");
                    }
                    incomingString.append(incomingsPerNode);
                    if (j < (roadMap.mapNodes.size() - 1)) {
                        incomingString.append(";");
                    }
                }
                node.setTextContent(incomingString.toString());
            }
            if ("out".equals(node.getNodeName())) {
                StringBuilder outgoingString = new StringBuilder();
                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
                    MapNode mapNode = roadMap.mapNodes.get(j);
                    StringBuilder outgoingPerNode = new StringBuilder();
                    for (int outgoingIndex = 0; outgoingIndex < mapNode.outgoing.size(); outgoingIndex++) {
                        MapNode outgoingNode = mapNode.outgoing.get(outgoingIndex);
                        outgoingPerNode.append(outgoingNode.id);
                        if (outgoingIndex < (mapNode.outgoing.size() - 1)) {
                            outgoingPerNode.append(",");
                        }
                    }
                    if (outgoingPerNode.toString().isEmpty()) {
                        outgoingPerNode = new StringBuilder("-1");
                    }
                    outgoingString.append(outgoingPerNode);
                    if (j < (roadMap.mapNodes.size() - 1)) {
                        outgoingString.append(";");
                    }
                }
                node.setTextContent(outgoingString.toString());
            }
        }

        for (int markerIndex = 1; markerIndex < roadMap.mapMarkers.size() + 100; markerIndex++) {
            Element element = (Element) doc.getElementsByTagName("mm" + (markerIndex)).item(0);
            if (element != null) {
                Element parent = (Element) element.getParentNode();
                while (parent.hasChildNodes())
                    parent.removeChild(parent.getFirstChild());
            }
        }

        NodeList markerList = doc.getElementsByTagName("mapmarker");
        Node markerNode = markerList.item(0);
        int mapMarkerCount = 1;
        for (MapMarker mapMarker : roadMap.mapMarkers) {
            Element newMapMarker = doc.createElement("mm" + mapMarkerCount);

            Element markerID = doc.createElement("id");
            markerID.appendChild(doc.createTextNode("" + mapMarker.mapNode.id));
            newMapMarker.appendChild(markerID);

            Element markerName = doc.createElement("name");
            markerName.appendChild(doc.createTextNode(mapMarker.name));
            newMapMarker.appendChild(markerName);

            Element markerGroup = doc.createElement("group");
            markerGroup.appendChild(doc.createTextNode(mapMarker.group));
            newMapMarker.appendChild(markerGroup);

            markerNode.appendChild(newMapMarker);
            mapMarkerCount += 1;
        }


        Node mapNameNode = waypoints.getParentNode();
        String newMapName = mapNameNode.getNodeValue();
        String fileName = xmlConfigFile.getName();
        if (fileName.contains("AutoDrive_") && fileName.contains("_config")) {
            int newPathStartIndex = fileName.lastIndexOf("AutoDrive_");
            newPathStartIndex += "AutoDrive_".length();
            int newPathEndIndex = fileName.lastIndexOf("_config");
            if (fileName.endsWith("_init_config")) {
                newPathEndIndex = fileName.lastIndexOf("_init_config");
            }
            newMapName = fileName.substring(newPathStartIndex, newPathEndIndex);
            LOG.info("Found new map name in: {} : {}", fileName, newMapName);
        }
        doc.renameNode(mapNameNode, null, newMapName);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlConfigFile);
        transformer.transform(source, result);

        LOG.info("Done save");
    }

    public void updateMapZoomFactor(int zoomFactor) {
        mapPanel.setMapZoomFactor(zoomFactor);
        mapPanel.repaint();
    }

    private String createTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(AUTO_DRIVE_COURSE_EDITOR_TITLE);
        if (xmlConfigFile != null) {
            sb.append(" - ").append(xmlConfigFile.getAbsolutePath()).append(isStale() ? " *" : "");
        }
        return sb.toString();
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public void setMapPanel(MapPanel mapPanel) {
        this.mapPanel = mapPanel;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        if (isStale() != stale) {
            this.stale = stale;
            setTitle(createTitle());
        }
    }


    private void checkAndLoadProperties() {
        File configFile = new File(AdConfiguration.CONFIG_FILE_NAME);
        if (configFile.exists()) {
            AdConfiguration.getInstance().readConfigFile();
        } else {
            showConfigDialog();
        }
        enableManagerPanel();
    }

    private void enableManagerPanel() {
        if ( StringUtils.isNotBlank(AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY))) {
            tabPane.setEnabledAt(1, true);
            routesManagerPanel.reloadXMLRouteMetaData();
        }
        else {
            tabPane.setEnabledAt(1, false);
        }
    }

    private void showConfigDialog() {
        ConfigDialog configDialog = new ConfigDialog(this, true);
        configDialog.setVisible(true);
        ConfigDialog.DIALOG_STATE dialog_state = configDialog.getState();
        if (dialog_state.equals(ConfigDialog.DIALOG_STATE.OK)) {
            AdConfiguration.getInstance().writeConfigFile();
        }
    }
}
