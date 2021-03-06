/*
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
package org.h2gis.drivers.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * A class to manage the node element properties.
 *
 * @author Erwan Bocher
 */
public class NodeOSMElement extends OSMElement {

    private double latitude;
    private double longitude;
    private Double elevation = null;

    /**
     * Constructor
     * @param latitude Latitude value
     * @param longitude Longitude value
     */
    public NodeOSMElement(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @param elevation Elevation (also known as altitude or height) above mean sea level in metre,
     *                  based on geoid model EGM 96 which is used by WGS 84 (GPS).
     */
    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    /**
     * The geometry of the node
     *
     * @return Point value
     */
    public Point getPoint(GeometryFactory gf) {
        return gf.createPoint(new Coordinate(longitude,
                latitude));
    }

    /**
     * @return Elevation (also known as altitude or height) above mean sea level in metre,
     *                  based on geoid model EGM 96 which is used by WGS 84 (GPS).
     */
    public Double getElevation() {
        return elevation;
    }

    @Override
    public boolean addTag(String key, String value) {
        if(key.equalsIgnoreCase("ele")) {
            try {
                setElevation(Double.valueOf(value));
                return false;
            } catch (NumberFormatException ex) {
                // Not a number, some user enter "273 m"
                return super.addTag(key, value);
            }
        } else {
            return super.addTag(key, value);
        }
    }
}
