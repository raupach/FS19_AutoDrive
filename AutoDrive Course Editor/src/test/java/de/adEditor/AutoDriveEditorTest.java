package de.adEditor;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JToggleButtonFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class AutoDriveEditorTest extends AssertJSwingJUnitTestCase
{

    private FrameFixture window;

    @Override
    protected void onSetUp()
    {
        AutoDriveEditor frame = GuiActionRunner.execute(AutoDriveEditor::new);
        window = new FrameFixture(robot(), frame);
        window.show();
    }

    @Override
    protected void onTearDown()
    {
        window.cleanUp();
    }

    @Test
    public void checkStates()
    {
        JToggleButtonFixture moveNode = window.toggleButton(AutoDriveEditor.MOVE_NODES);
        JToggleButtonFixture connectNode = window.toggleButton(AutoDriveEditor.CONNECT_NODES);
        JToggleButtonFixture removeNode = window.toggleButton(AutoDriveEditor.REMOVE_NODES);
        JToggleButtonFixture removeDestination = window.toggleButton(AutoDriveEditor.REMOVE_DESTINATIONS);
        JToggleButtonFixture createNode = window.toggleButton(AutoDriveEditor.CREATE_NODES);
        JToggleButtonFixture createDestination = window.toggleButton(AutoDriveEditor.CREATE_DESTINATIONS);

        assertNotNull(moveNode);
        assertNotNull(connectNode);
        assertNotNull(removeNode);
        assertNotNull(removeDestination);
        assertNotNull(createNode);
        assertNotNull(createDestination);

        // Initial-Zustand prüfen
        moveNode.requireSelected();
        connectNode.requireNotSelected();
        removeNode.requireNotSelected();
        removeDestination.requireNotSelected();
        createNode.requireNotSelected();
        createDestination.requireNotSelected();

        // Klick auf ConnectNode
        connectNode.click();
        moveNode.requireNotSelected();
        connectNode.requireSelected();
        removeNode.requireNotSelected();
        removeDestination.requireNotSelected();
        createNode.requireNotSelected();
        createDestination.requireNotSelected();

        // Klick auf RemoveNode
        removeNode.click();
        moveNode.requireNotSelected();
        connectNode.requireNotSelected();
        removeNode.requireSelected();
        removeDestination.requireNotSelected();
        createNode.requireNotSelected();
        createDestination.requireNotSelected();

        // Klick auf RemoveDestination
        removeDestination.click();
        moveNode.requireNotSelected();
        connectNode.requireNotSelected();
        removeNode.requireNotSelected();
        removeDestination.requireSelected();
        createNode.requireNotSelected();
        createDestination.requireNotSelected();

        // Klick auf createNode
        createNode.click();
        moveNode.requireNotSelected();
        connectNode.requireNotSelected();
        removeNode.requireNotSelected();
        removeDestination.requireNotSelected();
        createNode.requireSelected();
        createDestination.requireNotSelected();

        // Klick auf createDestination
        createDestination.click();
        moveNode.requireNotSelected();
        connectNode.requireNotSelected();
        removeNode.requireNotSelected();
        removeDestination.requireNotSelected();
        createNode.requireNotSelected();
        createDestination.requireSelected();
    }

}