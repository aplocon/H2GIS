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
package org.h2gis.drivers.shp;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import org.h2.util.StringUtils;
import org.h2gis.drivers.DriverManager;
import org.h2gis.drivers.file_table.H2TableIndex;
import org.h2gis.drivers.shp.internal.SHPDriver;
import org.h2gis.h2spatial.CreateSpatialExtension;
import org.h2gis.h2spatial.ut.SpatialH2UT;
import org.h2gis.h2spatialapi.DriverFunction;
import org.h2gis.h2spatialapi.EmptyProgressVisitor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.h2.value.ValueGeometry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test copy data from SHP to database
 * @author Nicolas Fortin
 */
public class SHPImportExportTest {
    private static Connection connection;
    private static final String DB_NAME = "SHPImportTest";

    @BeforeClass
    public static void tearUp() throws Exception {
        // Keep a connection alive to not close the DataBase on each unit test
        connection = SpatialH2UT.createSpatialDataBase(DB_NAME);
        CreateSpatialExtension.registerFunction(connection.createStatement(), new SHPRead(), "");
        CreateSpatialExtension.registerFunction(connection.createStatement(), new SHPWrite(), "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        connection.close();
    }

    @Test
    public void exportTableTestGeomEnd() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/area_export.shp");
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(idarea int primary key, the_geom POLYGON)");
        stat.execute("insert into area values(1, 'POLYGON ((-10 109, 90 109, 90 9, -10 9, -10 109))')");
        stat.execute("insert into area values(2, 'POLYGON ((90 109, 190 109, 190 9, 90 9, 90 109))')");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/area_export.shp', 'AREA')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        SHPDriver shpDriver = new SHPDriver();
        shpDriver.initDriverFromFile(shpFile);
        shpDriver.setGeometryFieldIndex(1);
        assertEquals(2, shpDriver.getFieldCount());
        assertEquals(2, shpDriver.getRowCount());
        Object[] row = shpDriver.getRow(0);
        assertEquals(1, row[0]);
        // The driver can not create POLYGON
        assertEquals("MULTIPOLYGON (((-10 109, 90 109, 90 9, -10 9, -10 109)))", ((Geometry)row[1]).toText());
        row = shpDriver.getRow(1);
        assertEquals(2, row[0]);
        assertEquals("MULTIPOLYGON (((90 109, 190 109, 190 9, 90 9, 90 109)))", ((Geometry)row[1]).toText());
    }

    @Test
    public void exportTableTestGeomDeb() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/area_export.shp");
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(the_geom POLYGON, idarea int primary key)");
        stat.execute("insert into area values('POLYGON ((-10 109, 90 109, 90 9, -10 9, -10 109))', 1)");
        stat.execute("insert into area values('POLYGON ((90 109, 190 109, 190 9, 90 9, 90 109))', 2)");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/area_export.shp', 'AREA')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        SHPDriver shpDriver = new SHPDriver();
        shpDriver.initDriverFromFile(shpFile);
        shpDriver.setGeometryFieldIndex(0);
        assertEquals(2, shpDriver.getFieldCount());
        assertEquals(2, shpDriver.getRowCount());
        Object[] row = shpDriver.getRow(0);
        assertEquals(1, row[1]);
        // The driver can not create POLYGON
        assertEquals("MULTIPOLYGON (((-10 109, 90 109, 90 9, -10 9, -10 109)))", ((Geometry)row[0]).toText());
        row = shpDriver.getRow(1);
        assertEquals(2, row[1]);
        assertEquals("MULTIPOLYGON (((90 109, 190 109, 190 9, 90 9, 90 109)))", ((Geometry)row[0]).toText());
    }

    @Test
    public void copySHPTest() throws SQLException {
        Statement st = connection.createStatement();
        st.execute("DROP TABLE IF EXISTS WATERNETWORK");
        final String path = StringUtils.quoteStringSQL(SHPEngineTest.class.getResource("waternetwork.shp").getPath());
        st.execute("CALL SHPRead(" + path + ", 'WATERNETWORK');");
        checkSHPReadResult(st);
    }


    @Test
    public void copySHPTestAutomaticTableName() throws SQLException {
        Statement st = connection.createStatement();
        st.execute("DROP TABLE IF EXISTS WATERNETWORK");
        final String path = StringUtils.quoteStringSQL(SHPEngineTest.class.getResource("waternetwork.shp").getPath());
        // No table name is specified:
        st.execute("CALL SHPRead(" + path + ", 'waternetwork');");
        checkSHPReadResult(st);
    }

    private void checkSHPReadResult(Statement st) throws SQLException {
        // Query declared Table columns
        ResultSet rs = st.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS where TABLE_NAME = 'WATERNETWORK'");
        assertTrue(rs.next());
        assertEquals(H2TableIndex.PK_COLUMN_NAME, rs.getString("COLUMN_NAME"));
        assertEquals("INTEGER", rs.getString("TYPE_NAME"));
        assertTrue(rs.next());
        assertEquals("THE_GEOM", rs.getString("COLUMN_NAME"));
        assertEquals("GEOMETRY", rs.getString("TYPE_NAME"));
        assertTrue(rs.next());
        assertEquals("TYPE_AXE",rs.getString("COLUMN_NAME"));
        assertEquals("VARCHAR", rs.getString("TYPE_NAME"));
        assertEquals(254, rs.getInt("CHARACTER_MAXIMUM_LENGTH"));
        assertTrue(rs.next());
        assertEquals("GID",rs.getString("COLUMN_NAME"));
        assertEquals("BIGINT", rs.getString("TYPE_NAME"));
        assertTrue(rs.next());
        assertEquals("LENGTH",rs.getString("COLUMN_NAME"));
        assertEquals("DOUBLE",rs.getString("TYPE_NAME"));
        rs.close();
        // Check content
        rs = st.executeQuery("SELECT * FROM WATERNETWORK");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(H2TableIndex.PK_COLUMN_NAME));
        assertEquals("MULTILINESTRING ((183299.71875 2425074.75, 183304.828125 2425066.75))",rs.getString("the_geom"));
        assertEquals("river",rs.getString("type_axe"));
        assertEquals(9.492402903934545, rs.getDouble("length"), 1e-12);
        assertEquals(1, rs.getInt("GID"));
        assertTrue(rs.next());
        assertEquals("ditch", rs.getString("type_axe"));
        assertEquals(261.62989135452983, rs.getDouble("length"), 1e-12);
        assertEquals(2, rs.getInt("GID"));
        rs.close();
        // Computation
        rs = st.executeQuery("SELECT SUM(length) sumlen FROM WATERNETWORK");
        assertTrue(rs.next());
        assertEquals(28469.778049948833, rs.getDouble(1), 1e-12);
        rs.close();
        st.execute("drop table WATERNETWORK");
    }

    @Test
    public void exportTableWithoutConstraint() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/area_export.shp");
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(the_geom GEOMETRY, idarea int primary key)");
        stat.execute("insert into area values('POLYGON ((-10 109, 90 109, 90 9, -10 9, -10 109))', 1)");
        stat.execute("insert into area values('POLYGON ((90 109, 190 109, 190 9, 90 9, 90 109))', 2)");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/area_export.shp', 'AREA')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        SHPDriver shpDriver = new SHPDriver();
        shpDriver.initDriverFromFile(shpFile);
        shpDriver.setGeometryFieldIndex(0);
        assertEquals(2, shpDriver.getFieldCount());
        assertEquals(2, shpDriver.getRowCount());
        Object[] row = shpDriver.getRow(0);
        assertEquals(1, row[1]);
        // The driver can not create POLYGON
        assertEquals("MULTIPOLYGON (((-10 109, 90 109, 90 9, -10 9, -10 109)))", ((Geometry)row[0]).toText());
        row = shpDriver.getRow(1);
        assertEquals(2, row[1]);
        assertEquals("MULTIPOLYGON (((90 109, 190 109, 190 9, 90 9, 90 109)))", ((Geometry)row[0]).toText());
    }

    @Test(expected = SQLException.class)
    public void exportTableWithoutConstraintException() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(the_geom GEOMETRY, idarea int primary key)");
        stat.execute("insert into area values('POINT (-10 109)', 1)");
        stat.execute("insert into area values('POLYGON ((90 109, 190 109, 190 9, 90 9, 90 109))', 2)");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/area_export_ex.shp', 'AREA')");
    }

    @Test
    public void testDriverManager() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(the_geom GEOMETRY, idarea int primary key)");
        stat.execute("insert into area values('POLYGON ((-10 109, 90 109, 90 9, -10 9, -10 109))', 1)");
        stat.execute("insert into area values('POLYGON ((90 109, 190 109, 190 9, 90 9, 90 109))', 2)");
        // Export in target with special chars
        File shpFile = new File("target/area éxport.shp");
        DriverFunction exp = new SHPDriverFunction();
        exp.exportTable(connection, "AREA", shpFile,new EmptyProgressVisitor());
        stat.execute("DROP TABLE IF EXISTS myshp");
        DriverFunction manager = new DriverManager();
        manager.importFile(connection, "MYSHP", shpFile, new EmptyProgressVisitor());
        ResultSet rs = stat.executeQuery("select SUM(ST_AREA(the_geom)) from myshp");
        try {
            assertTrue(rs.next());
            assertEquals(20000,rs.getDouble(1),1e-6);
        } finally {
            rs.close();
        }
    }

    @Test(expected = SQLException.class)
    public void exportTableWithNullGeom() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(the_geom GEOMETRY, idarea int primary key)");
        stat.execute("insert into area values('POLYGON ((-10 109, 90 109, 90 9, -10 9, -10 109))', 1)");
        stat.execute("insert into area values(NULL, 2)");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/area_export.shp', 'AREA')");
    }

    @Test
    public void exportTableLineString() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/line_export.shp");
        stat.execute("DROP TABLE IF EXISTS LINE");
        stat.execute("create table LINE(idarea int primary key, the_geom LINESTRING)");
        stat.execute("insert into LINE values(1, 'LINESTRING (-10 109, 90 109, 90 9, -10 9)')");
        stat.execute("insert into LINE values(2, 'LINESTRING (90 109, 190 109, 190 9, 90 9)')");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/line_export.shp', 'LINE')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        SHPDriver shpDriver = new SHPDriver();
        shpDriver.initDriverFromFile(shpFile);
        shpDriver.setGeometryFieldIndex(1);
        assertEquals(2, shpDriver.getFieldCount());
        assertEquals(2, shpDriver.getRowCount());
        Object[] row = shpDriver.getRow(0);
        assertEquals(1, row[0]);
        // The driver can not create POLYGON
        assertEquals("MULTILINESTRING ((-10 109, 90 109, 90 9, -10 9))", ((Geometry)row[1]).toText());
        row = shpDriver.getRow(1);
        assertEquals(2, row[0]);
        assertEquals("MULTILINESTRING ((90 109, 190 109, 190 9, 90 9))", ((Geometry)row[1]).toText());
    }

    @Test
    public void readSHPURITest() throws Exception {
        Statement st = connection.createStatement();
        st.execute("DROP TABLE IF EXISTS WATERNETWORK");
        final String path = StringUtils.quoteStringSQL(SHPEngineTest.class.getResource("waternetwork.shp").toURI().toString());
        st.execute("CALL SHPRead(" + path + ", 'WATERNETWORK');");
        checkSHPReadResult(st);
    }
    @Test
    public void testReservedKeyWord() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(the_geom GEOMETRY, idarea int primary key, \"NATURAL\" boolean)");
        stat.execute("insert into area values('POLYGON ((-10 109, 90 109, 90 9, -10 9, -10 109))', 1, True)");
        stat.execute("insert into area values('POLYGON ((90 109, 190 109, 190 9, 90 9, 90 109))', 2, False)");
        // Export in target with special chars
        File shpFile = new File("target/test_export.shp");
        DriverFunction exp = new SHPDriverFunction();
        exp.exportTable(connection, "AREA", shpFile,new EmptyProgressVisitor());
        stat.execute("DROP TABLE IF EXISTS myshp");
        SHPDriverFunction driverFunction = new SHPDriverFunction();
        driverFunction.importFile(connection, "MYSHP", shpFile, new EmptyProgressVisitor());
        ResultSet rs = stat.executeQuery("select * from myshp");
        try {
            assertEquals(4,rs.findColumn("NATURAL"));
        } finally {
            rs.close();
        }
    }

    @Test
    public void exportTableTestZ() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/area_export.shp");
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(idarea int primary key, the_geom POLYGON)");
        stat.execute("insert into area values(1, 'POLYGON ((-10 109 5, 90 109 5, 90 9 5, -10 9 5, -10 109 5))')");
        stat.execute("insert into area values(2, 'POLYGON ((90 109 3, 190 109 3, 190 9 3, 90 9 3, 90 109 3))')");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/area_export.shp', 'AREA')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        SHPDriver shpDriver = new SHPDriver();
        shpDriver.initDriverFromFile(shpFile);
        shpDriver.setGeometryFieldIndex(1);
        assertEquals(2, shpDriver.getFieldCount());
        assertEquals(2, shpDriver.getRowCount());
        Object[] row = shpDriver.getRow(0);
        assertEquals(1, row[0]);
        // The driver can not create POLYGON
        WKTWriter toText = new WKTWriter(3);
        assertEquals("MULTIPOLYGON (((-10 109 5, 90 109 5, 90 9 5, -10 9 5, -10 109 5)))", toText.write((Geometry)row[1]));
        row = shpDriver.getRow(1);
        assertEquals(2, row[0]);
        assertEquals("MULTIPOLYGON (((90 109 3, 190 109 3, 190 9 3, 90 9 3, 90 109 3)))", toText.write((Geometry)row[1]));
    }
    
    @Test
    public void exportImportPolygonZ() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/area_export.shp");
        stat.execute("DROP TABLE IF EXISTS AREA");
        stat.execute("create table area(idarea int primary key, the_geom POLYGON)");
        stat.execute("insert into area values(1, 'POLYGON ((-10 109 5, 90 109 5, 90 9 5, -10 9 5, -10 109 5))')");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/area_export.shp', 'AREA')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        stat.execute("DROP TABLE IF EXISTS IMPORT_AREA;");
        stat.execute("CALL SHPRead('target/area_export.shp', 'IMPORT_AREA')");
        ResultSet res = stat.executeQuery("SELECT THE_GEOM FROM IMPORT_AREA;");
        res.next();
        Geometry geom = (Geometry) res.getObject(1);
        Coordinate[] coords = geom.getCoordinates();
        int count = 0;
        for (Coordinate coord : coords) {
            if (coord.z == 5) {
                count++;
            }
        }
        assertEquals(count, coords.length);
        res.close();

    }

    @Test
    public void exportImportPointZ() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/punctual_export.shp");
        stat.execute("DROP TABLE IF EXISTS PUNCTUAL");
        stat.execute("create table punctual(idarea int primary key, the_geom POINT)");
        stat.execute("insert into punctual values(1, 'POINT(-10 109 5)')");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/punctual_export.shp', 'PUNCTUAL')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        stat.execute("DROP TABLE IF EXISTS IMPORT_PUNCTUAL;");
        stat.execute("CALL SHPRead('target/punctual_export.shp', 'IMPORT_PUNCTUAL')");
        ResultSet res = stat.executeQuery("SELECT THE_GEOM FROM IMPORT_PUNCTUAL;");
        res.next();
        Geometry geom = (Geometry) res.getObject(1);
        Coordinate coord = geom.getCoordinate();
        assertEquals(coord.z, 5, 10E-1);
        res.close();
    }
    
     @Test
    public void exportImportLineStringZ() throws SQLException, IOException {
        Statement stat = connection.createStatement();
        File shpFile = new File("target/lineal_export.shp");
        stat.execute("DROP TABLE IF EXISTS LINEAL");
        stat.execute("create table lineal(idarea int primary key, the_geom LINESTRING)");
        stat.execute("insert into lineal values(1, 'LINESTRING(-10 109 5, 12  6)')");
        // Create a shape file using table area
        stat.execute("CALL SHPWrite('target/lineal_export.shp', 'LINEAL')");
        // Read this shape file to check values
        assertTrue(shpFile.exists());
        stat.execute("DROP TABLE IF EXISTS IMPORT_LINEAL;");
        stat.execute("CALL SHPRead('target/lineal_export.shp', 'IMPORT_LINEAL')");
        ResultSet res = stat.executeQuery("SELECT THE_GEOM FROM IMPORT_LINEAL;");
        res.next();
        Geometry geom = (Geometry) res.getObject(1);
        Coordinate[] coords = geom.getCoordinates();
        assertEquals(coords[0].z, 5, 10E-1);
        //Since the 'NaN' DOUBLE values for Z coordinates is invalid in a shapefile, it is converted to '0.0'. 
        assertEquals(coords[1].z, 0, 10E-1);
        res.close();
    }
}
