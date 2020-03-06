package de.adEditor.routes;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class RoutesManagerPanel extends JPanel {

    public RoutesManagerPanel() {
        super(new BorderLayout());

        JToolBar toolBar = new JToolBar("Still draggable");
        addButtons(toolBar);

//        setPreferredSize(new Dimension(450, 130));
        add(toolBar, BorderLayout.PAGE_START);

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

}
