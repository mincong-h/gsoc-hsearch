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

import org.hibernate.search.exception.SearchException;
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

	@Inject
	private Instance<EntityManagerFactory> entityManagerFactoryInstance;

	@Override
	public EntityManagerFactory getDefault() {
		if ( entityManagerFactoryInstance.isUnsatisfied() ) {
			throw new SearchException( "No entity manager factory available in the CDI context."
					+ " Make sure your entity manager factory is a named bean." );
		}
		else if ( entityManagerFactoryInstance.isAmbiguous() ) {
			throw new SearchException( "Multiple entity manager factories have been registered."
					+ " Please provide the bean name for the selected entity manager factory to the batch indexing job through"
					+ " the 'entityManagerFactoryReference' parameter." );
		}
		else {
			return entityManagerFactoryInstance.get();
		}
	}

	@Override
	public EntityManagerFactory get(String reference) {
		Instance<EntityManagerFactory> instance = entityManagerFactoryInstance.select( new NamedQualifier( reference ) );
		if ( instance.isUnsatisfied() ) {
			throw new SearchException( "No entity manager factory available in the CDI context with this bean name: '" + reference +"'."
					+ " Make sure your entity manager factory is a named bean." );
		}
		else {
			return instance.get();
		}
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
