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

package org.h2gis.utilities.wrapper;

import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSetMetaData;
import org.h2gis.utilities.TableLocation;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author Nicolas Fortin
 */
public class SpatialResultSetMetaDataImpl extends ResultSetMetaDataWrapper implements SpatialResultSetMetaData {
    private int firstGeometryFieldIndex = -1;

    /**
     * Constructor
     * @param resultSetMetaData meta data
     * @param statement Active statement
     */
    public SpatialResultSetMetaDataImpl(ResultSetMetaData resultSetMetaData, StatementWrapper statement) {
        super(resultSetMetaData,statement);
    }

    @Override
    public int getFirstGeometryFieldIndex() throws SQLException {
        if(firstGeometryFieldIndex==-1) {
            for(int idColumn=1;idColumn<=getColumnCount();idColumn++) {
                if(getColumnTypeName(idColumn).equalsIgnoreCase("geometry")) {
                    firstGeometryFieldIndex = idColumn;
                    break;
                }
            }
        }
        return firstGeometryFieldIndex;
    }
    @Override
    public int getGeometryType() throws SQLException {
        return getGeometryType(getFirstGeometryFieldIndex());
    }

    @Override
    public int getGeometryType(int column) throws SQLException {
        return SFSUtilities.getGeometryType(statement.getConnection(),
                new TableLocation(getCatalogName(column), getSchemaName(column), getTableName(column)),
                getColumnName(column));
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if(iface.isInstance(this)) {
            try {
                return iface.cast(this);
            } catch (ClassCastException ex) {
                //Should never happen
                throw new SQLException(ex);
            }
        } else {
            return super.unwrap(iface);
        }
    }
}
