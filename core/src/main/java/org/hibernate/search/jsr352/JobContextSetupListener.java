/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.criterion.Criterion;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.context.jpa.internal.SessionFactoryNameRegistry;
import org.hibernate.search.jsr352.context.jpa.internal.SessionFactoryPersistenceUnitNameRegistry;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.util.MassIndexerUtil;
import org.hibernate.search.util.StringHelper;
import org.jboss.logging.Logger;

/**
 * Listener before the start of the job. It aims to setup the job context data, shared by all the steps.
 *
 * @author Mincong Huang
 */
public class JobContextSetupListener extends AbstractJobListener {

	private static final Logger LOGGER = Logger.getLogger( JobContextSetupListener.class );

	private static final String PERSISTENCE_UNIT_NAME_SCOPE_NAME = "persistence-unit-name";
	private static final String SESSION_FACTORY_NAME_SCOPE_NAME = "session-factory-name";

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty
	private String entityManagerFactoryScope;

	@Inject
	@BatchProperty
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty
	private String rootEntities;

	@Inject
	@BatchProperty(name = "criteria")
	private String serializedCriteria;

	@Override
	public void beforeJob() throws Exception {
		setUpContext();
	}

	/**
	 * Method to be overridden to retrieve the entity manager factory by different means (CDI, Spring DI, ...).
	 *
	 * @param scopeName The scope chosen in the job parameters. This allows to pick a specific registry.
	 * @return The entity manager factory registry used to convert the entity manager factory reference to an actual instance.
	 */
	protected EntityManagerFactoryRegistry getEntityManagerFactoryRegistry(String scopeName) {
		if ( StringHelper.isEmpty( scopeName ) || PERSISTENCE_UNIT_NAME_SCOPE_NAME.equals( scopeName ) ) {
			return SessionFactoryPersistenceUnitNameRegistry.getInstance();
		}
		else if ( SESSION_FACTORY_NAME_SCOPE_NAME.equals( scopeName ) ) {
			return SessionFactoryNameRegistry.getInstance();
		}
		else {
			throw new SearchException( "Unknown entity manager factory registry: '" + scopeName + "'."
					+ " Please use the name of a supported registry." );
		}
	}

	private void setUpContext() throws ClassNotFoundException, IOException {
		EntityManager em = null;

		try {
			LOGGER.debug( "Creating entity manager ..." );

			EntityManagerFactoryRegistry entityManagerFactoryProvider =
					getEntityManagerFactoryRegistry( entityManagerFactoryScope );

			EntityManagerFactory emf;
			if ( StringHelper.isEmpty( entityManagerFactoryReference ) ) {
				emf = entityManagerFactoryProvider.getDefault();
			}
			else {
				emf = entityManagerFactoryProvider.get( entityManagerFactoryReference );
			}
			em = emf.createEntityManager();
			List<String> entityNamesToIndex = Arrays.asList( rootEntities.split( "," ) );
			Set<Class<?>> entityTypesToIndex = Search
					.getFullTextEntityManager( em )
					.getSearchFactory()
					.getIndexedTypes()
					.stream()
					.filter( clz -> entityNamesToIndex.contains( clz.getName() ) )
					.collect( Collectors.toCollection( HashSet::new ) );

			Set<Criterion> criteria = MassIndexerUtil.deserializeCriteria( serializedCriteria );
			LOGGER.infof( "%d criteria found.", criteria.size() );

			JobContextData jobContextData = new JobContextData();
			jobContextData.setEntityManagerFactory( emf );
			jobContextData.setCriteria( criteria );
			jobContextData.setEntityTypes( entityTypesToIndex );
			jobContext.setTransientUserData( jobContextData );
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
		}
	}

}
