/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.cdi.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.jsr352.JobContextSetupListener;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.util.StringHelper;

/**
 * @author Yoann Rodiere
 */
@Named("org.hibernate.search.jsr352.JobContextSetupListener")
public class CDIJobContextSetupListener extends JobContextSetupListener {

	private static final String BEAN_NAME_SCOPE_NAME = "bean-name";

	@Inject
	private EntityManagerFactoryRegistry entityManagerFactoryRegistry;

	@Override
	protected EntityManagerFactoryRegistry getEntityManagerFactoryRegistry(String scopeName) {
		if ( StringHelper.isEmpty( scopeName ) || BEAN_NAME_SCOPE_NAME.equals( scopeName ) ) {
			return entityManagerFactoryRegistry;
		}
		return super.getEntityManagerFactoryRegistry( scopeName );
	}

}
