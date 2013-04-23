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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

public class PropertyAddWorker implements Callable<int[]>
{
    public static final String NodeIndexName = "nodes";
    public static final String RelationshipIndexName = "relationships";

    private static final char[] Symbols = ( "1234567890"
            + "abcdefghijklmnopqrstuvwxyz"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "!@#$%^&*()_+=-\\|<>?,./" ).toCharArray();

    private final GraphDatabaseService graphDb;
    private final Queue<Node> nodes;
    private final Random r;
    private int ops;
    private final boolean indexThem;

    private int reads;
    private int writes;

    private final Index<Node> nodeIndex;
    private final Index<Relationship> relIndex;

    public PropertyAddWorker( GraphDatabaseService graphDb, Queue<Node> nodes,
            int ops, boolean indexThem )
    {
        this.graphDb = graphDb;
        this.nodes = nodes;
        this.r = new Random();
        this.ops = ops;
        this.indexThem = indexThem;
        this.reads = 0;
        this.writes = 0;

        this.nodeIndex = graphDb.index().forNodes( NodeIndexName );
        this.relIndex = graphDb.index().forRelationships( RelationshipIndexName );
    }

    @Override
    public int[] call() throws Exception
    {
        long time = System.currentTimeMillis();
        while ( ops-- > 0 )
        {
            Transaction tx = graphDb.beginTx();
            try
            {
                if ( r.nextBoolean() )
                {
                    addPropertyToNode();
                }
                else
                {
                    // addPropertyToRelationship();
                }
                tx.success();
            }
            catch ( Exception e )
            {
                tx.failure();
            }
            finally
            {
                tx.finish();
            }
        }
        int[] result = new int[3];
        result[0] = reads;
        result[1] = writes;
        result[2] = (int) ( System.currentTimeMillis() - time );
        return result;
    }

    private void addPropertyToNode()
    {
        int offset = r.nextInt( nodes.size() / 10 );
        boolean createNew = r.nextBoolean();
        String propToAdd = null;
        while ( offset-- > 0 )
        {
            Node temp = nodes.poll();
            if ( createNew && propToAdd == null
                    && temp.getPropertyKeys().iterator().hasNext() )
            {
                propToAdd = temp.getPropertyKeys().iterator().next();
                reads += 1;
                createNew = false; // we got a name, no need to look anymore
            }
            nodes.offer( temp );
        }
        Node toChange = nodes.poll();
        if ( propToAdd == null || toChange.hasProperty( propToAdd ) )
        {
            propToAdd = getRandomPropertyName();
        }
        Object valueToSet = getRandomPropertyValue();
        toChange.setProperty( propToAdd, valueToSet );
        if ( indexThem )
        {
            nodeIndex.add( toChange, propToAdd, valueToSet );
        }
        nodes.offer( toChange );
        writes += 1;
    }

    private void addPropertyToRelationship()
    {
        int offset = r.nextInt( nodes.size() / 10 );
        boolean createNew = r.nextBoolean();
        String propToAdd = null;
        Node temp = nodes.poll();
        /*
         *  We skip at least offset and then grab the first
         *  node that has a relationship
         */
        while ( offset-- > 0 || !temp.hasRelationship() )
        {
            if ( !temp.hasRelationship() ) continue;
            Relationship rel = temp.getRelationships().iterator().next();
            if ( createNew && propToAdd == null
                    && rel.getPropertyKeys().iterator().hasNext() )
            {
                propToAdd = rel.getPropertyKeys().iterator().next();
                reads += 1;
                createNew = false; // we got a name, no need to look anymore
            }
            nodes.offer( temp );
            temp = nodes.poll();
        }
        // temp now holds a node that will do
        if ( propToAdd == null )
        {
            propToAdd = getRandomPropertyName();
        }
        /*
         *  We have kind of a problem here. The node has been removed from
         *  the queue so it is safe. But, its relationships are rooted also
         *  to another node which might have its relationships removed
         *  as we stand here, removing the relationship we will choose below.
         *  I decide to completely disregard this and merrily throw an
         *  exception. OK?
         */
        List<Relationship> thisNodesRels = new LinkedList<Relationship>();
        for ( Relationship rel : temp.getRelationships() )
        {
            thisNodesRels.add( rel );
        }
        Object valueToSet = getRandomPropertyValue();
        Relationship toChange = thisNodesRels.get( r.nextInt( thisNodesRels.size() ) );
        toChange.setProperty(
                propToAdd, valueToSet );
        toChange.setProperty( propToAdd, valueToSet );
        if ( indexThem )
        {
            relIndex.add( toChange, propToAdd, valueToSet );
        }
        nodes.offer( temp );
    }

    private String getRandomPropertyName()
    {
        return UUID.randomUUID().toString();
    }

    private Object getRandomPropertyValue()
    {
        switch ( r.nextInt( 4 ) )
        {
        case 0:
            return r.nextInt();
        case 1:
            return r.nextLong();
        case 2:
            return r.nextBoolean();
        case 3:
            return getRandomString( r.nextInt( 50 ) );
        default:
            return new String[] { getRandomString( r.nextInt( 20 ) ), "",
                    getRandomString( r.nextInt( 20 ) ) };
        }
    }

    private String getRandomString( int length )
    {
        StringBuffer acc = new StringBuffer( "" );
        int i = 0;
        while ( i++ < length )
        {
            acc.append( Symbols[r.nextInt( length )] );
        }
        return acc.toString();
    }
}
