/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.context.jpa.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Yoann Rodiere
 */
public class SessionFactoryRegistryUpdater implements Integrator {

	private final List<MutableSessionFactoryRegistry> registries = new ArrayList<>();

	public SessionFactoryRegistryUpdater() {
		registries.add( SessionFactoryNameRegistry.getInstance() );
		registries.add( SessionFactoryPersistenceUnitNameRegistry.getInstance() );
	}

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		for ( MutableSessionFactoryRegistry registry : registries ) {
			registry.register( sessionFactory );
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		for ( MutableSessionFactoryRegistry registry : registries ) {
			registry.unregister( sessionFactory );
		}
	}

}
