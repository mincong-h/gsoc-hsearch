/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.cdi.internal.context.jpa;

import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;

/**
 * An {@link EntityManagerFactoryRegistry} that retrieves the entity manager factory
 * from the CDI context.
 * <p>
 * When calling {@link #get(String)}, the reference will be interpreted as a
 * {@link Named} qualfier.
 *
 * @author Yoann Rodiere
 */
@Singleton
public class CDIBeanNameEntityManagerFactoryRegistry implements EntityManagerFactoryRegistry {

	@PersistenceUnit
	private EntityManagerFactory defaultInstance;

	@Inject
	private Instance<EntityManagerFactory> namedInstance;

	@Override
	public EntityManagerFactory getDefault() {
		return defaultInstance;
	}

	@Override
	public EntityManagerFactory get(String reference) {
		return namedInstance.select( new NamedQualifier( reference ) ).get();
	}

	private static class NamedQualifier extends AnnotationLiteral<Named> implements Named {
		private final String name;

		public NamedQualifier(String name) {
			super();
			this.name = name;
		}

		@Override
		public String value() {
			return name;
		}
	}

}
