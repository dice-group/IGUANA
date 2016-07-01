package org.aksw.iguana.connection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.jdbc.remote.connections.RemoteEndpointConnection;
import org.apache.jena.jdbc.results.AskResults;
import org.apache.jena.jdbc.results.MaterializedSelectResults;
import org.apache.jena.jdbc.results.SelectResults;
import org.apache.jena.jdbc.results.TripleIteratorResults;
import org.apache.jena.jdbc.results.TripleListResults;
import org.apache.jena.jdbc.statements.JenaStatement;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

/**
 * A Jena JDBC statement against a remote endpoint
 * 
 */
public class Stmt{
	private static final int NO_LIMIT = 0;
	JenaStatement stmt;
	RemoteEndpointConnection connection;
	private int maxRows;
	private int timeout;
	private boolean needsCommit;
	private ResultSet currResults;
	private int type=ResultSet.TYPE_FORWARD_ONLY;
	
	public Stmt(JenaStatement stmt) throws SQLException{
		this.stmt = stmt;
		this.maxRows = stmt.getMaxRows();
		this.timeout = 0;
		this.needsCommit=false;

		this.connection = ((RemoteEndpointConnection)stmt.getConnection());
	}
	
	public final ResultSet execute(String sql) throws SQLException {
        if (stmt.isClosed())
            throw new SQLException("The Statement is closed");

        // Pre-process the command text
        sql = this.connection.applyPreProcessors(sql);

        Query q = null;
        UpdateRequest u = null;
        try {
            // Start by assuming a query
            q = QueryFactory.create(sql);
        } catch (Exception e) {
            try {
                // If that fails try as an update instead
                u = UpdateFactory.create(sql);
            } catch (Exception e2) {
                throw new SQLException("Not a valid SPARQL query/update", e);
            }
        }

        if (q != null) {
            // Execute as a query
            return this.executeQuery(q);
        }else {
            throw new SQLException("Unable to create a SPARQL query/update");
        }
    }
	
    public ResultSet executeQuery(Query q) throws SQLException {
      

        try {
            // Pre-process the query
            q = this.connection.applyPreProcessors(q);

            // Manipulate the query if appropriate
            if (this.maxRows > NO_LIMIT) {
                // If we have no LIMIT or the LIMIT is greater than the
                // permitted max rows
                // then we will set the LIMIT to the max rows
                if (!q.hasLimit() || q.getLimit() > this.maxRows) {
                    q.setLimit(this.maxRows);
                }
            }

            // Create the query execution
            QueryExecution qe = this.createQueryExecution(q);

            // Manipulate the query execution if appropriate
            if (this.timeout > NO_LIMIT) {
                qe.setTimeout(this.timeout, TimeUnit.SECONDS, this.timeout, TimeUnit.SECONDS);
            }

            // Return the appropriate result set type
            if (q.isSelectType()) {
                switch (this.type) {
                case ResultSet.TYPE_SCROLL_INSENSITIVE:
                    this.currResults = new MaterializedSelectResults(stmt, qe, ResultSetFactory.makeRewindable(this.connection
                            .applyPostProcessors(qe.execSelect())), false);
                    break;
                case ResultSet.TYPE_FORWARD_ONLY:
                default:
                    this.currResults = new SelectResults(stmt, qe, this.connection.applyPostProcessors(qe.execSelect()),
                            needsCommit);
                    break;
                }
            } else if (q.isAskType()) {
                boolean askRes = qe.execAsk();
                qe.close();
                this.currResults = new AskResults(stmt, this.connection.applyPostProcessors(askRes), needsCommit);
            } else if (q.isDescribeType()) {
                switch (this.type) {
                case ResultSet.TYPE_SCROLL_INSENSITIVE:
                    this.currResults = new TripleListResults(stmt, qe, Iter.toList(this.connection.applyPostProcessors(qe
                            .execDescribeTriples())), false);
                    break;
                case ResultSet.TYPE_FORWARD_ONLY:
                default:
                    this.currResults = new TripleIteratorResults(stmt, qe, this.connection.applyPostProcessors(qe
                            .execDescribeTriples()), needsCommit);
                    break;
                }
            } else if (q.isConstructType()) {
                switch (this.type) {
                case ResultSet.TYPE_SCROLL_INSENSITIVE:
                    this.currResults = new TripleListResults(stmt, qe, Iter.toList(this.connection.applyPostProcessors(qe
                            .execConstructTriples())), false);
                    break;
                case ResultSet.TYPE_FORWARD_ONLY:
                default:
                    this.currResults = new TripleIteratorResults(stmt, qe, this.connection.applyPostProcessors(qe
                            .execConstructTriples()), needsCommit);
                    break;
                }
            } else {
            	qe.close();
                throw new SQLException("Unknown SPARQL Query type");
            }

            // Can immediately commit when type is
            // TYPE_SCROLL_INSENSITIVE and auto-committing since we have
            // already materialized results so don't need the read
            // transaction
            qe.close();

            return this.currResults;
        } catch (SQLException e) {
            
            throw e;
        } catch (Throwable e) {
           
            throw new SQLException("Error occurred during SPARQL query evaluation", e);
        }
    }

	private QueryExecution createQueryExecution(Query q) throws SQLException {
		if (this.connection.getQueryEndpoint() == null)
            throw new SQLException("This statement is backed by a write-only connection, read operations are not supported");

        // Create basic execution
        QueryEngineHTTP exec = new QueryEngineHTTP(this.connection.getQueryEndpoint(), q.toString().replaceAll(" +", " "), null);
        
        // Apply authentication settings
//        if (connection. != null) {
//            exec.setAuthenticator(authenticator);
//        }

        // Apply default and named graphs if appropriate
        if (this.connection.getDefaultGraphURIs() != null) {
            exec.setDefaultGraphURIs(this.connection.getDefaultGraphURIs());
        }
        if (this.connection.getNamedGraphURIs() != null) {
            exec.setNamedGraphURIs(this.connection.getNamedGraphURIs());
        }

        // Set result types
        if (this.connection.getSelectResultsType() != null) {
            exec.setSelectContentType(this.connection.getSelectResultsType());
        }
        if (this.connection.getModelResultsType() != null) {
            exec.setModelContentType(this.connection.getModelResultsType());
        }
        
        // Return execution
        return exec;
	}


	
}
