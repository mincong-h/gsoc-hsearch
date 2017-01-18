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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.exception.SearchException;

/**
 * @author Yoann Rodiere
 */
public class SessionFactoryNameRegistry implements MutableSessionFactoryRegistry {

	private static final MutableSessionFactoryRegistry INSTANCE = new SessionFactoryNameRegistry();

	public static MutableSessionFactoryRegistry getInstance() {
		return INSTANCE;
	}

	private final ConcurrentMap<String, SessionFactoryImplementor> sessionFactoriesByName = new ConcurrentHashMap<>();

	private SessionFactoryNameRegistry() {
		// Use getInstance()
	}

	@Override
	public synchronized void register(SessionFactoryImplementor sessionFactory) {
		String name = sessionFactory.getName();
		if ( name != null ) {
			sessionFactoriesByName.put( name, sessionFactory );
		}
	}

	@Override
	public synchronized void unregister(SessionFactoryImplementor sessionFactory) {
		/*
		 * Remove by value. This is inefficient, but we don't expect to have billions of session factories anyway,
		 * and it allows to easily handle the case where multiple session factories have been registered with the same name.
		 */
		sessionFactoriesByName.values().remove( sessionFactory );
	}

	@Override
	public EntityManagerFactory get(String reference) {
		SessionFactory factory = sessionFactoriesByName.get( reference );
		if ( factory == null ) {
			throw new SearchException( "No entity manager factory has been created with this name yet: '" + reference +"'."
					+ " Make sure your entity manager factory is named"
					+ " (for instance by setting the '" + AvailableSettings.SESSION_FACTORY_NAME + "' option)"
					+ " and that the entity manager factory has already been created and wasn't closed before"
					+ " you launch the job." );
		}
		return factory;
	}

	@Override
	public synchronized EntityManagerFactory getDefault() {
		if ( sessionFactoriesByName.isEmpty() ) {
			throw new SearchException( "No named entity manager factory has been created yet."
					+ " Make sure your entity manager factory is named "
					+ " (for instance by setting the '" + AvailableSettings.SESSION_FACTORY_NAME + "' option)"
					+ " and that the entity manager factory has already been created and wasn't closed before"
					+ " you launch the job." );
		}
		else if ( sessionFactoriesByName.size() > 1 ) {
			throw new SearchException( "Multiple named entity manager factories have been registered."
					+ " Please provide the name of the selected entity manager factory to the batch indexing job through"
					+ " the 'entityManagerFactoryReference' parameter." );
		}
		else {
			return sessionFactoriesByName.values().iterator().next();
		}
	}

}
