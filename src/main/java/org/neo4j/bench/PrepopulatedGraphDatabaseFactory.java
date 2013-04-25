/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bench;

import static org.neo4j.kernel.impl.util.FileUtils.copyRecursively;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.kernel.impl.util.FileUtils;

public class PrepopulatedGraphDatabaseFactory
{

    public enum DataSet
    {
        CINEASTS("../databases/cineasts");

        private String location;

        private DataSet(String location)
        {
            this.location = location;
        }

        protected String getLocation()
        {
            return location;
        }
    }

    public static GraphDatabaseService create( DataSet dataSet )
    {
        try
        {
            File location = File.createTempFile( "perftest", "regression" );
            location.delete();
            location.mkdir();

            copyRecursively( new File( dataSet.getLocation() ), location );

            deleteAnyOldUpgradeBackup( location );

            GraphDatabaseService db = new GraphDatabaseFactory()
                    .newEmbeddedDatabaseBuilder( location.getAbsolutePath() )
                    .setConfig( GraphDatabaseSettings.allow_store_upgrade, "true" )
                    .newGraphDatabase();

            return new GraphDatabaseCleanupWrapper( db, location );
        } catch(Exception e)
        {
            throw new RuntimeException( e );
        }
    }

    private static void deleteAnyOldUpgradeBackup( File location ) throws IOException
    {
        File upgradeBackup = new File( location, "upgrade_backup" );
        if( upgradeBackup.exists())
        {
            FileUtils.deleteRecursively( upgradeBackup );
        }
    }

}
