package de.adEditor.routes;


import de.adEditor.routes.dto.Route;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.Date;
import java.util.List;

public class AutoDriveRoutesTableModel implements TableModel {

    private List<Route> routes;

    public AutoDriveRoutesTableModel(List<Route> routes) {
        this.routes = routes;
    }

    @Override
    public int getRowCount() {
        return routes.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int i) {
        switch (i) {
            case 0:
                return "name";
            case 1:
                return "fileName";
            case 2:
                return "map";
            case 3:
                return "revision";
            case 4:
                return "date";
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
                return String.class;
            case 3:
                return Integer.class;
            case 4:
                return Date.class;
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Route route = routes.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return route.getName();
            case 1:
                return route.getFileName();
            case 2:
                return route.getMap();
            case 3:
                return route.getRevision();
            case 4:
                return route.getDate();
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object o, int i, int i1) {

    }

    @Override
    public void addTableModelListener(TableModelListener tableModelListener) {

    }

    @Override
    public void removeTableModelListener(TableModelListener tableModelListener) {

    }
}