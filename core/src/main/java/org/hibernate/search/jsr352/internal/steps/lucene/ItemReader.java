/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.se.JobSEEnvironment;
import org.hibernate.search.jsr352.internal.util.MassIndexerUtil;
import org.hibernate.search.jsr352.internal.util.PartitionUnit;
import org.jboss.logging.Logger;

/**
 * Item reader reads entities using scrollable results. For each reader, there's
 * only one target entity type. The range to read is defined by the partition
 * unit. This range is always a left-closed interval.
 * <p>
 * For example, there 2 entity types Company and Employee. The number of rows
 * are respectively 5 and 4500. The rowsPerPartition is set to 1000. Then, there
 * will be 6 readers and their ranges are :
 * <ul>
 * <li>partitionID = 0, entityType = Company, range = [null, null[
 * <li>partitionID = 1, entityType = Employee, range = [1, 1000[
 * <li>partitionID = 2, entityType = Employee, range = [1000, 2000[
 * <li>partitionID = 3, entityType = Employee, range = [2000, 3000[
 * <li>partitionID = 4, entityType = Employee, range = [3000, 4000[
 * <li>partitionID = 5, entityType = Employee, range = [4000, null[
 * </ul>
 *
 * @author Mincong Huang
 */
@Named
public class ItemReader extends AbstractItemReader {

	private static final Logger logger = Logger.getLogger( ItemReader.class );

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	@Inject
	@BatchProperty
	private boolean cacheable;

	@Inject
	@BatchProperty
	private boolean isJavaSE;

	@Inject
	@BatchProperty
	private int fetchSize;

	@Inject
	@BatchProperty
	private int maxResults;

	@Inject
	@BatchProperty
	private int partitionID;

	@Inject
	@BatchProperty
	private String entityName;

	@Inject
	private JobContext jobContext;

	@Inject
	private StepContext stepContext;

	private Class<?> entityClazz;
	private Serializable checkpointID;

	// read entities and produce Lucene work
	private Session session;
	private StatelessSession ss;
	private ScrollableResults scroll;
	private SessionFactory sessionFactory;

	public ItemReader() {

	}

	/**
	 * The checkpointInfo method returns the current checkpoint data for this
	 * reader. It is called before a chunk checkpoint is committed.
	 *
	 * @return the checkpoint info
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Serializable checkpointInfo() throws Exception {
		logger.info( "checkpointInfo() called. "
				+ "Saving last read ID to batch runtime..." );
		return checkpointID;
	}

	/**
	 * Close operation(s) before the class destruction.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void close() throws Exception {
		logger.info( "closing everything..." );
		try {
			scroll.close();
			logger.info( "Scrollable results closed." );
		}
		catch (Exception e) {
			logger.error( e );
		}
		try {
			ss.close();
			logger.info( "Stateless session closed." );
		}
		catch (Exception e) {
			logger.error( e );
		}
		try {
			session.close();
		}
		catch (Exception e) {
			logger.error( e );
		}
		// reset the chunk work count to avoid over-count in item collector
		// release session
		StepContextData stepData = (StepContextData) stepContext.getTransientUserData();
		stepData.setSession( null );
		stepContext.setPersistentUserData( stepData );
	}

	/**
	 * Initialize the environment. If checkpoint does not exist, then it should
	 * be the first open. If checkpoint exists, then it isn't the first open,
	 * re-use the input object "checkpoint" as the last ID already read.
	 *
	 * @param checkpoint The last checkpoint info persisted in the batch
	 * runtime, previously given by checkpointInfo(). If this is the first
	 * start, then the checkpoint will be null.
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void open(Serializable checkpointID) throws Exception {

		logger.infof( "[partitionID=%d] open reader for entity %s ...", partitionID, entityName );
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		entityClazz = jobData.getIndexedType( entityName );
		PartitionUnit unit = jobData.getPartitionUnit( partitionID );
		logger.info( unit );

		if ( isJavaSE ) {
			emf = JobSEEnvironment.getEntityManagerFactory();
		}
		sessionFactory = emf.unwrap( SessionFactory.class );
		ss = sessionFactory.openStatelessSession();
		session = sessionFactory.openSession();
		scroll = buildScroll( ss, unit, checkpointID );

		StepContextData stepData = null;
		if ( checkpointID == null ) {
			stepData = new StepContextData( partitionID, entityName );
			stepData.setRestarted( false );
		}
		else {
			stepData = (StepContextData) stepContext.getPersistentUserData();
			stepData.setRestarted( true );
		}

		stepData.setSession( session );
		stepContext.setTransientUserData( stepData );
	}

	private ScrollableResults buildScroll(StatelessSession ss,
			PartitionUnit unit, Object checkpointID) {

		String idName = MassIndexerUtil.getIdName( entityClazz, session );
		Criteria criteria = ss.createCriteria( entityClazz );
		if ( checkpointID != null ) {
			criteria.add( Restrictions.ge( idName, checkpointID ) );
		}

		if ( unit.isUniquePartition() ) {
			// no bounds if the partition unit is unique
		}
		else if ( unit.isFirstPartition() ) {
			criteria.add( Restrictions.lt( idName, unit.getUpperBound() ) );
		}
		else if ( unit.isLastPartition() ) {
			criteria.add( Restrictions.ge( idName, unit.getLowerBound() ) );
		}
		else {
			criteria.add( Restrictions.ge( idName, unit.getLowerBound() ) )
					.add( Restrictions.lt( idName, unit.getUpperBound() ) );
		}
		return criteria.addOrder( Order.asc( idName ) )
				.setReadOnly( true )
				.setCacheable( cacheable )
				.setFetchSize( fetchSize )
				.setMaxResults( maxResults )
				.scroll( ScrollMode.FORWARD_ONLY );
	}

	/**
	 * Read item from database using JPA. Each read, there will be only one
	 * entity fetched.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Object readItem() throws Exception {
		logger.debug( "Reading item ..." );
		Object entity = null;

		if ( scroll.next() ) {
			entity = scroll.get( 0 );
			checkpointID = (Serializable) emf.getPersistenceUnitUtil()
					.getIdentifier( entity );
		}
		else {
			logger.info( "no more result. read ends." );
		}
		return entity;
	}
}
