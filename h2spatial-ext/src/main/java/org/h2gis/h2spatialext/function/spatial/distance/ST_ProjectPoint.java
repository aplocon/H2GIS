/**
 * h2spatial is a library that brings spatial support to the H2 Java database.
 *
 * h2spatial is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2014 IRSTV (FR CNRS 2488)
 *
 * h2patial is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * h2spatial is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * h2spatial. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */

package org.h2gis.h2spatialext.function.spatial.distance;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.h2gis.h2spatialapi.DeterministicScalarFunction;

/**
 *
 * @author Erwan Bocher
 */
public class ST_ProjectPoint extends DeterministicScalarFunction{

    
    public ST_ProjectPoint(){
        addProperty(PROP_REMARKS, "Projet a point along a linestring. If the point projected is out of line "
                        + "the first or last point on the line will be returned otherwise"
                        + " the input point.");
    }
    @Override
    public String getJavaStaticMethod() {
        return "projectPoint";
    }
    
    
    /**
     * Project a point on a linestring or multilinestring
     * @param point
     * @param geometry
     * @return 
     */
    public static Point projectPoint(Geometry point, Geometry geometry) {
        if (point == null || geometry==null) {
            return null;
        }
        if (point.getDimension()==0 && geometry.getDimension() == 1) {
            LengthIndexedLine ll = new LengthIndexedLine(geometry);
            double index = ll.project(point.getCoordinate());
            Point result = geometry.getFactory().createPoint(ll.extractPoint(index));
            return result;
        } else {
            return null;
        }
    }
    
}
