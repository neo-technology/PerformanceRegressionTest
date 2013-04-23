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
package org.neo4j.bench.cases.mixedload.workers;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;

public class SampleReadWorker implements Callable<int[]>
{

    private final GraphDatabaseService graphDb;
    private int reads;
    private int ops;
    private final Queue<Node> nodes;
    private final boolean readIndex;

    private final Index<Node> nodeIndex;
    private final Index<Relationship> relIndex;

    public SampleReadWorker( GraphDatabaseService graphDb, Queue<Node> nodes,
            int ops, boolean readIndex )
    {
        this.graphDb = graphDb;
        this.nodes = nodes;
        this.reads = 0;
        this.ops = ops;
        this.readIndex = readIndex;

        this.nodeIndex = graphDb.index().forNodes(
                PropertyAddWorker.NodeIndexName );
        this.relIndex = graphDb.index().forRelationships(
                PropertyAddWorker.RelationshipIndexName );
    }

    @Override
    public int[] call() throws Exception
    {
        Random r = new Random();
        long time = System.currentTimeMillis();
        while ( ops-- > 0 )
        {
            try
            {
                int getAt = r.nextInt( 10 );
                while (getAt-- > 0)
                {
                    nodes.offer(nodes.poll());
                }
                Node read = nodes.poll();
                // This is just the proxy, need to (possibly) load completely
                read = graphDb.getNodeById( read.getId() );

                reads += 1; // Possible re-read if out of cache/mmap

                if ( r.nextDouble() > 0.75 )
                {
                    for ( Relationship rel : read.getRelationships() )
                    {
                        reads += 1; // Possible re-read the rel from storage
                        Object propValue;
                        for ( String propKey : rel.getPropertyKeys() )
                        {
                            propValue = rel.getProperty( propKey );
                            if ( readIndex )
                            {
                                relIndex.get( propKey, propValue ).close();
                            }
                            reads += 2; // Not for the index but for the prop
                            // key/value
                        }
                    }
                }
                else
                {
                    Object propValue;
                    for ( String propKey : read.getPropertyKeys() )
                    {
                        propValue = read.getProperty( propKey );
                        if ( readIndex )
                        {
                            nodeIndex.get( propKey, propValue ).close();
                        }
                        reads += 2; // the prop key and value
                    }
                }
                nodes.offer( read );
            }
            catch (Exception e)
            {
                e.printStackTrace();
                /*
                 *  We need to swallow this since so far we have stepped on
                 *  other workers code and we need to make sure our noise is
                 *  counted against the total.
                 *  The most common exception here will be Invalid Records from
                 *  the kernel, since we might be trying to read something that
                 *  has now been deleted.
                 */
            }
        }
        int[] result = new int[3];
        result[0] = reads;
        result[1] = 0;
        result[2] = reads == 0 ? 0 : (int) ( System.currentTimeMillis() - time );
        return result;
    }
}
