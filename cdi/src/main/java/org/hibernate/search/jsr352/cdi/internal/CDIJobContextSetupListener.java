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
import org.hibernate.search.jsr352.cdi.internal.context.jpa.ByBeanName;
import org.hibernate.search.jsr352.cdi.internal.context.jpa.ByPersistenceUnitName;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.util.StringHelper;

/**
 * @author Yoann Rodiere
 */
@Named("org.hibernate.search.jsr352.JobContextSetupListener")
public class CDIJobContextSetupListener extends JobContextSetupListener {

	private static final String PERSISTENCE_UNIT_NAME_SCOPE_NAME = "persistence-unit-name";
	private static final String BEAN_NAME_SCOPE_NAME = "bean-name";

	@Inject
	@ByBeanName
	private EntityManagerFactoryRegistry beanNameRegistry;

	@Inject
	@ByPersistenceUnitName
	private EntityManagerFactoryRegistry persistenceUnitNameRegistry;

	@Override
	protected EntityManagerFactoryRegistry getEntityManagerFactoryRegistry(String scopeName) {
		if ( StringHelper.isEmpty( scopeName ) || PERSISTENCE_UNIT_NAME_SCOPE_NAME.equals( scopeName ) ) {
			return persistenceUnitNameRegistry;
		}
		else if ( BEAN_NAME_SCOPE_NAME.equals( scopeName ) ) {
			return beanNameRegistry;
		}
		return super.getEntityManagerFactoryRegistry( scopeName );
	}

}
