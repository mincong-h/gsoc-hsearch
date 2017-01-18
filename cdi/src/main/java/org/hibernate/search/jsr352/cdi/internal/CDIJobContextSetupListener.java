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
import org.hibernate.search.jsr352.cdi.internal.context.jpa.CDIEntityManagerFactoryProvider;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryProvider;

/**
 * @author Yoann Rodiere
 */
@Named("org.hibernate.search.jsr352.JobContextSetupListener")
public class CDIJobContextSetupListener extends JobContextSetupListener {

	@Inject
	private CDIEntityManagerFactoryProvider entityManagerFactoryProvider;

	@Override
	protected EntityManagerFactoryProvider getEntityManagerFactoryProvider() {
		return entityManagerFactoryProvider;
	}

}
