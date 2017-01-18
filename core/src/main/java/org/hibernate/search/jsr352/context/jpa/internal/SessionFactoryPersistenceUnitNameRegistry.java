/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.context.jpa.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.search.exception.SearchException;

/**
 * @author Yoann Rodiere
 */
public class SessionFactoryPersistenceUnitNameRegistry implements MutableSessionFactoryRegistry {

	private static final MutableSessionFactoryRegistry INSTANCE = new SessionFactoryPersistenceUnitNameRegistry();

	public static MutableSessionFactoryRegistry getInstance() {
		return INSTANCE;
	}

	private final ConcurrentMap<String, SessionFactoryImplementor> sessionFactoriesByPUName = new ConcurrentHashMap<>();

	private SessionFactoryPersistenceUnitNameRegistry() {
		// Use getInstance()
	}

	@Override
	public synchronized void register(SessionFactoryImplementor sessionFactory) {
		Object persistenceUnitName = sessionFactory.getProperties().get( AvailableSettings.PERSISTENCE_UNIT_NAME );
		if ( persistenceUnitName instanceof String ) {
			sessionFactoriesByPUName.put( (String) persistenceUnitName, sessionFactory );
		}
	}

	@Override
	public synchronized void unregister(SessionFactoryImplementor sessionFactory) {
		/*
		 * Remove by value. This is inefficient, but we don't expect to have billions of session factories anyway,
		 * and it allows to easily handle the case where multiple session factories have been registered with the same name.
		 */
		sessionFactoriesByPUName.values().remove( sessionFactory );
	}

	@Override
	public EntityManagerFactory get(String reference) {
		SessionFactory factory = sessionFactoriesByPUName.get( reference );
		if ( factory == null ) {
			throw new SearchException( "No entity manager factory has been created with this name yet: '" + reference +"'."
					+ " Make sure you use the JPA API to create your entity manager factory (use a 'persistence.xml' file)"
					+ " and that the entity manager factory has already been created and wasn't closed before"
					+ " you launch the job." );
		}
		return factory;
	}

	@Override
	public synchronized EntityManagerFactory getDefault() {
		if ( sessionFactoriesByPUName.isEmpty() ) {
			throw new SearchException( "No entity manager factory has been created yet."
					+ " Make sure you use the JPA API to create your entity manager factory (use a 'persistence.xml' file)"
					+ " and that the entity manager factory has already been created and wasn't closed before"
					+ " you launch the job." );
		}
		else if ( sessionFactoriesByPUName.size() > 1 ) {
			throw new SearchException( "Multiple entity manager factories have been registered."
					+ " Please provide the name of the selected persistence unit to the batch indexing job through"
					+ " the 'entityManagerFactoryReference' parameter." );
		}
		else {
			return sessionFactoriesByPUName.values().iterator().next();
		}
	}

}
