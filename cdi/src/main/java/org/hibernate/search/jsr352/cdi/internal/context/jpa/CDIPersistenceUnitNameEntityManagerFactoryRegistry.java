/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.cdi.internal.context.jpa;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;

/**
 * An {@link EntityManagerFactoryRegistry} that retrieves the entity manager factory
 * from the CDI context by using a @PersistenceUnit annotation, thereby not requiring
 * any special configuration from the user (on contrary to {@link CDIBeanNameEntityManagerFactoryRegistry}.
 * <p>
 * <strong>CAUTION:</strong>Calling {@link #get(String)} is not supported, because CDI
 * does not offer any API allowing to retrieve an EntityManagerFactory by its persistence
 * unit name dynamically.
 * Indeed:
 * <ul>
 * <li>{@literal @PersistenceUnit} is not a qualifier annotation, so the usual CDI
 * approaches for retrieving a bean dynamically ({@literal Instance.select},
 * {@literal BeanManager.getBeans}, ...) won't work.
 * <li>there is no way to inject all the persistence units (so we could filter them and
 * select one by its name) because {@literal @PersistenceUnit} does not work on a
 * {@literal Instance<EntityManagerFactory>} (at least not with Weld).
 * </ul>
 *
 * @author Yoann Rodiere
 */
@Singleton
@ByPersistenceUnitName
public class CDIPersistenceUnitNameEntityManagerFactoryRegistry implements EntityManagerFactoryRegistry {

	@Inject
	private BeanManager beanManager;

	@Override
	public EntityManagerFactory getDefault() {
		try {
			return getVetoedBeanReference( beanManager, PersistenceUnitAccessor.class ).entityManagerFactory;
		}
		catch (RuntimeException e) {
			throw new SearchException( "Exception while retrieving the EntityManagerFactory using @PersistenceUnit."
					+ " This generally happens either because the persistence wasn't configured properly"
					+ " or because there are multiple persistence units." );
		}
	}

	@Override
	public EntityManagerFactory get(String reference) {
		throw new SearchException( "Cannot retrieve the entity manager factory from the CDI context using a"
				+ " persistence unit name: CDI does not expose the necessary APIs to do that."
				+ " Please do not provide the persistence unit name (if there is only one persistence unit)"
				+ " or select a different scope using the 'entityManagerFactoryScope' job parameter (if"
				+ " there are multiple persistence units)." );
	}

	/**
	 * Creates an instance of a @Vetoed bean type using the given bean manager.
	 * <p>
	 * This seems overly complicated, but all the usual solutions
	 * fail when you want to create access an {@link EntityManagerFactory}
	 * from the CDI context lazily...
	 * <p>
	 * <ol>
	 * <li>Adding a {@literal @PersistenceUnit} on an {@literal Instance<EntityManagerFactory>}
	 * field or on a {@literal Provider<EntityManagerFactory> field will make Weld throw
	 * an exception (it only allows a field of type {@literal EntityManagerFactory}).
	 * <li>Weld seems to check {@literal @PersistenceUnit} annotations when creating injection
	 * points, not when injecting. This means that a {@literal @PersistenceUnit} without a unitName
	 * will make the application startup fail when there are multiple persistence units, even if
	 * the bean on which this annotation is applied is never instantiated.
	 * </ol>
	 * <p>
	 * Thus:
	 *
	 * <ol>
	 * <li>We access the {@literal @PeristenceUnit} field from a different bean, instantiated only
	 * when (if) we need it
	 * <li>The {@literal @PersistentUnit}-annotated bean is {@literal @Vetoed} so that it's not
	 * processed by the CDI engine by default, but only when we request processing explicitly.
	 * And that's what this method does: it makes the CDI engine process the type and instantiate it.
	 * </ol>
	 */
	private static <T> T getVetoedBeanReference(BeanManager beanManager, Class<T> vetoedType) {
		AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( vetoedType );
		BeanAttributes<T> beanAttributes = beanManager.createBeanAttributes( annotatedType );
		InjectionTargetFactory<T> injectionTargetFactory = beanManager.getInjectionTargetFactory( annotatedType );
		Bean<T> bean = beanManager.createBean( beanAttributes, vetoedType, injectionTargetFactory );
		CreationalContext<T> creationalContext = beanManager.createCreationalContext( bean );
		return vetoedType.cast( beanManager.getReference( bean, vetoedType, creationalContext ) );
	}

	@Vetoed
	private static class PersistenceUnitAccessor {
		@PersistenceUnit
		private EntityManagerFactory entityManagerFactory;
	}

}
