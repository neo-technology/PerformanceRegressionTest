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

import static org.neo4j.kernel.impl.util.FileUtils.deleteRecursively;

import java.io.File;
import java.io.IOException;

import javax.transaction.TransactionManager;

import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.IdGeneratorFactory;
import org.neo4j.kernel.KernelData;
import org.neo4j.kernel.TransactionBuilder;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.impl.core.KernelPanicEventGenerator;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.core.RelationshipTypeHolder;
import org.neo4j.kernel.impl.nioneo.store.StoreId;
import org.neo4j.kernel.impl.persistence.PersistenceSource;
import org.neo4j.kernel.impl.transaction.LockManager;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.kernel.impl.transaction.xaframework.TxIdGenerator;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.info.DiagnosticsManager;

public class GraphDatabaseCleanupWrapper implements GraphDatabaseAPI
{

    private final GraphDatabaseAPI delegate;
    private final File dbFolder;

    public GraphDatabaseCleanupWrapper(GraphDatabaseAPI delegate, File dbFolder)
    {
        this.delegate = delegate;
        this.dbFolder = dbFolder;
    }

    @Override
    public void shutdown()
    {
        delegate.shutdown();
        try
        {
            deleteRecursively( dbFolder );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Transaction beginTx()
    {
        return delegate.beginTx();
    }

    @Override
    public <T> TransactionEventHandler<T> registerTransactionEventHandler( TransactionEventHandler<T>
                                                                                       tTransactionEventHandler )
    {
        return delegate.registerTransactionEventHandler( tTransactionEventHandler );
    }

    @Override
    public <T> TransactionEventHandler<T> unregisterTransactionEventHandler( TransactionEventHandler<T>
                                                                                         tTransactionEventHandler )
    {
        return delegate.unregisterTransactionEventHandler( tTransactionEventHandler );
    }

    @Override
    public KernelEventHandler registerKernelEventHandler( KernelEventHandler kernelEventHandler )
    {
        return delegate.registerKernelEventHandler( kernelEventHandler );
    }

    @Override
    public KernelEventHandler unregisterKernelEventHandler( KernelEventHandler kernelEventHandler )
    {
        return delegate.unregisterKernelEventHandler( kernelEventHandler );
    }

    @Override
    public Schema schema()
    {
        return delegate.schema();
    }

    @Override
    public IndexManager index()
    {
        return delegate.index();
    }

    @Override
    public DependencyResolver getDependencyResolver()
    {
        return delegate.getDependencyResolver();
    }

    @Override
    @Deprecated
    public NodeManager getNodeManager()
    {
        return delegate.getNodeManager();
    }

    @Override
    @Deprecated
    public LockManager getLockManager()
    {
        return delegate.getLockManager();
    }

    @Override
    @Deprecated
    public XaDataSourceManager getXaDataSourceManager()
    {
        return delegate.getXaDataSourceManager();
    }

    @Override
    @Deprecated
    public TransactionManager getTxManager()
    {
        return delegate.getTxManager();
    }

    @Override
    @Deprecated
    public DiagnosticsManager getDiagnosticsManager()
    {
        return delegate.getDiagnosticsManager();
    }

    @Override
    @Deprecated
    public StringLogger getMessageLog()
    {
        return delegate.getMessageLog();
    }

    @Override
    @Deprecated
    public RelationshipTypeHolder getRelationshipTypeHolder()
    {
        return delegate.getRelationshipTypeHolder();
    }

    @Override
    @Deprecated
    public IdGeneratorFactory getIdGeneratorFactory()
    {
        return delegate.getIdGeneratorFactory();
    }

    @Override
    @Deprecated
    public TxIdGenerator getTxIdGenerator()
    {
        return delegate.getTxIdGenerator();
    }

    @Override
    @Deprecated
    public String getStoreDir()
    {
        return delegate.getStoreDir();
    }

    @Override
    @Deprecated
    public KernelData getKernelData()
    {
        return delegate.getKernelData();
    }

    @Override
    @Deprecated
    public TransactionBuilder tx()
    {
        return delegate.tx();
    }

    @Override
    @Deprecated
    public PersistenceSource getPersistenceSource()
    {
        return delegate.getPersistenceSource();
    }

    @Override
    @Deprecated
    public KernelPanicEventGenerator getKernelPanicGenerator()
    {
        return delegate.getKernelPanicGenerator();
    }

    @Override
    @Deprecated
    public Guard getGuard()
    {
        return delegate.getGuard();
    }

    @Override
    @Deprecated
    public StoreId getStoreId()
    {
        return delegate.getStoreId();
    }

    @Override
    public Node createNode()
    {
        return delegate.createNode();
    }

    @Override
    public Node createNode( Label... labels )
    {
        return delegate.createNode( labels );
    }

    @Override
    public Node getNodeById( long l )
    {
        return delegate.getNodeById( l );
    }

    @Override
    public Relationship getRelationshipById( long l )
    {
        return delegate.getRelationshipById( l );
    }

    @Override
    @Deprecated
    public Node getReferenceNode()
    {
        return delegate.getReferenceNode();
    }

    @Override
    @Deprecated
    public Iterable<Node> getAllNodes()
    {
        return delegate.getAllNodes();
    }

    @Override
    public ResourceIterable<Node> findNodesByLabelAndProperty( Label label, String s, Object o )
    {
        return delegate.findNodesByLabelAndProperty( label, s, o );
    }

    @Override
    @Deprecated
    public Iterable<RelationshipType> getRelationshipTypes()
    {
        return delegate.getRelationshipTypes();
    }
}
