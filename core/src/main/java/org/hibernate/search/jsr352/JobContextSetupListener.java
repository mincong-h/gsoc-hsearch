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

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.Criterion;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryProvider;
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

	@Inject
	private JobContext jobContext;

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
	 * @return The entity manager factory provider used to convert the entity manager factory reference to an actual instance.
	 */
	protected EntityManagerFactoryProvider getEntityManagerFactoryProvider() {
		return new HibernateRegistryEntityManagerFactoryProvider();
	}

	private void setUpContext() throws ClassNotFoundException, IOException {
		EntityManager em = null;

		try {
			LOGGER.debug( "Creating entity manager ..." );

			EntityManagerFactoryProvider entityManagerFactoryProvider = getEntityManagerFactoryProvider();

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

	/**
	 * An {@link EntityManagerFactoryProvider} that retrieves the entity manager factory
	 * from the internal Hibernate registry.
	 *
	 * @author Yoann Rodiere
	 */
	private static final class HibernateRegistryEntityManagerFactoryProvider implements EntityManagerFactoryProvider {

		@Override
		public EntityManagerFactory get(String reference) {
			SessionFactory factory = SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( reference );
			if ( factory == null ) {
				throw new SearchException( "Invalid reference to an entity manager factory: '" + reference +"';"
						+ " this name does not match any known entity manager factory."
						+ " Please name your entity manager factory"
						+ " (for instance by setting the '" + AvailableSettings.SESSION_FACTORY_NAME + "' option)"
						+ " and provide the name to the batch indexing job through the 'entityManagerFactoryName' parameter."
						+ " Also, ensure the entityManagerFactory has already been created when you launch the job." );
			}
			return factory;
		}

		@Override
		public EntityManagerFactory getDefault() {
			throw new SearchException( "No reference to an entity manager factory provided."
					+ " Please name your entity manager factory "
					+ " (for instance by setting the '" + AvailableSettings.SESSION_FACTORY_NAME + "' option)"
					+ " and provide the name to the batch indexing job through the 'entityManagerFactoryName' parameter."
					+ " Also, ensure the entityManagerFactory has already been created when you launch the job." );
		}
	}

}
