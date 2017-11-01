> :warning: **DEPRECATED** This project has been integrated to
> [Hibernate Search][2], and will not be maintained here anymore.

# gsoc-hsearch

This project aims to provide an alternative to the current mass indexer 
implementation, using the Java Batch architecture as defined by JSR 352. This 
standardized tool JSR 352 provides task-and-chunk oriented processing, parallel 
execution and many other optimization features. This batch job should accept 
the entity type(s) to re-index as an input, load the relevant entities from the 
database and rebuild the full-text index from these.

## Mechanism

This project redesigns the mass index job as a chunk-oriented, non-interactive,
long-running, background execution process. Execution contains operational
control (start/stop/restart), logging, checkpointing and parallelization.

![Workflow of the job "mass-index"][1]

[1]: https://raw.githubusercontent.com/mincong-h/gsoc-hsearch/master/img/mass-index.png
[2]: https://github.com/hibernate/hibernate-search
