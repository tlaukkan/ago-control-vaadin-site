package org.agocontrol.site.component.flot;

import java.util.ArrayList;
import java.util.List;

/**
 * The data set container for flot sate.
 */
public class DataSet {
    /**
     * The data set label.
     */
    private String label;
    /**
     * The data value pairs.
     */
    private List<List<Object>> data = new ArrayList<>();

    /**
     * @return the label
     */
    public final String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public final void setLabel(final String label) {
        this.label = label;
    }

    /**
     * @return the data
     */
    public final List<List<Object>> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public final void setData(final List<List<Object>> data) {
        this.data = data;
    }
}
