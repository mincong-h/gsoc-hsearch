/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractCheckpointAlgorithm;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This checkpoint algorithm is used to provide a checkpoint decision based on
 * the item count N given by the user. So, the job is ready to checkpoint each N
 * items. If user does not specify the itemCount value, default value described
 * in the mass indexer will be applied.
 *
 * @author Mincong Huang
 */
@Named
public class CheckpointAlgorithm extends AbstractCheckpointAlgorithm {

	@Inject
	@BatchProperty
	private int itemCount;

	private final StepContext stepContext;

	@Inject
	public CheckpointAlgorithm(StepContext stepContext) {
		this.stepContext = stepContext;
	}

	@Override
	public boolean isReadyToCheckpoint() throws Exception {
		Metric[] metrics = stepContext.getMetrics();
		for ( final Metric m : metrics ) {
			if ( m.getType().equals( Metric.MetricType.READ_COUNT ) ) {
				return m.getValue() % itemCount == 0;
			}
		}
		throw new Exception( "Metric READ_COUNT not found" );
	}
}
