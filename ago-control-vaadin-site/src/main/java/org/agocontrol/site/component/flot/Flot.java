/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agocontrol.site.component.flot;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;

import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * Flot chart component.
 *
 * @author Tommi S.E. Laukkanen
 */
@JavaScript({"flotr2.min.js", "flot.js"})
public class Flot extends AbstractJavaScriptComponent {
    public Flot() {
        setImmediate(true);
        setSizeFull();
        setId("flot2content");
        addSeries("Study", 34, 35, 36, 37, 38, 39);


        final FlotState state = getState();

        state.getOptions("options").put("HtmlText", false);
        state.getOptions("options").put("title", "House Temperature [C]");
        state.getOptions("selection").put("mode", "x");
        state.getOptions("xaxis").put("mode", "time");
        state.getOptions("xaxis").put("labelsAngle", Double.valueOf(45));


    }

    public void addSeries(final String label, double... points) {
        final DataSet dataSet = new DataSet();
        dataSet.setLabel(label);

        TimeZone tz = TimeZone.getTimeZone("Europe/Helsinki");
        Date ret = new Date(System.currentTimeMillis() + tz.getRawOffset());

        // if we are now in DST, back off by the delta.  Note that we are checking the GMT date, this is the KEY.
        if ( tz.inDaylightTime( ret ))
        {
            Date dstDate = new Date( ret.getTime() + tz.getDSTSavings() );

            // check to make sure we have not crossed back into standard time
            // this happens when we are on the cusp of DST (7pm the day before the change for PDT)
            if ( tz.inDaylightTime( dstDate ))
            {
                ret = dstDate;
            }
        }

        for (int i = 0; i < points.length; i++) {
            dataSet.getData().add(Arrays.asList(new Object[]{Long.valueOf(ret.getTime() + 60 * 1000 * i),
                    Double.valueOf(points[i])}));
        }
        getState().getDataSets().add(dataSet);
    }

    @Override
    public FlotState getState() {
        return (FlotState) super.getState(true);
    }
}
