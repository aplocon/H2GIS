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

package org.h2gis.h2spatialext.function.spatial.convert;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import org.h2gis.h2spatialapi.DeterministicScalarFunction;

/**
 * ST_ToMultiPoint constructs a MultiPoint from the given geometry's coordinates.
 *
 * @author Adam Gouge
 */
public class ST_ToMultiPoint extends DeterministicScalarFunction {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public ST_ToMultiPoint() {
        addProperty(PROP_REMARKS, "Constructs a MultiPoint from the given geometry's " +
                "coordinates.");
    }

    @Override
    public String getJavaStaticMethod() {
        return "createMultiPoint";
    }

    /**
     * Constructs a MultiPoint from the given geometry's coordinates.
     *
     * @param geom Geometry
     * @return A MultiPoint constructed from the given geometry's coordinates
     */
    public static MultiPoint createMultiPoint(Geometry geom) {
        if (geom != null) {
            return GEOMETRY_FACTORY.createMultiPoint(geom.getCoordinates());
        } else {
            return null;
        }
    }
}
