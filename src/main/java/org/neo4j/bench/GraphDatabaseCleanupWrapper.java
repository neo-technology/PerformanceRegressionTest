package org.neo4j.bench;

import static org.neo4j.kernel.impl.util.FileUtils.deleteRecursively;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
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

public class GraphDatabaseCleanupWrapper implements GraphDatabaseService
{

    private final GraphDatabaseService delegate;
    private final File dbFolder;

    public GraphDatabaseCleanupWrapper(GraphDatabaseService delegate, File dbFolder)
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
}
