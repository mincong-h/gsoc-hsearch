/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.context.jpa;

import javax.persistence.EntityManagerFactory;

/**
 * An abstract contract allowing to retrieve an entity manager factory
 * without any assumption about the underlying dependency injection mechanism.
 *
 * @author Yoann Rodiere
 */
public interface EntityManagerFactoryProvider {

	/**
	 * @return The default {@link EntityManagerFactory}.
	 */
	EntityManagerFactory getDefault();

	/**
	 * @return The {@link EntityManagerFactory} for the given reference string.
	 */
	EntityManagerFactory get(String reference);

}
