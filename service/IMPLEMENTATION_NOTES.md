# Implementation Notes

## Updating a PAO
A PAO update is a proposed change to the attribute set of the PAO. The implementation needs to evaluate the effect
of the change on the policy graph and, depending on the update mode (DRY_RUN, FAIL_ON_CONFLICT, ENFORCE_CONFLICT),
make changes to the graph. Since the changes may be tossed, we need to build an in-memory structure of the PAOs.

The graph is a DAG, but not a strict hierarchy; that is, we can have a graph that looks like this:

The pointers represent source-to-dependent links. Since, there may be multiple paths to the same dependent, we
could optimize by not re-evaluating a dependent that has already been evaluated. However, there is an implicit
assumption in that optimization. Suppose a change to A results in a change to C. Now is there a way that a
change to A results in a change to B that results in a different change to C? It seems unlikely, but if there
are combinations of policy inputs that lead to different results, it could happen. Given the unpredictability
of external policies, we should not take that optimization.

The optimization we can take is: if the re-evaluation of C does not change its effective attributes, then we do not 
need to continue to process its dependents. We would have done that on the initial evaluation.

## Memory Model
There are three elements to the memory model:

### Graph Node
A graph node represents one PAO and its links to dependents and sources:
- pao - the Pao object of interest
- dependents - list of dependent graph nodes
- sources - list of source graph nodes
- modified - boolean indicating the Pao has been modified

### Pao Map
This is a Map:
- key - object id of a Pao
- value - graph node

We use the map in building out the graph, so that as we walk we are referring to mutated Paos and not the ones in the
database. If we try a lookup in the map and it is not found, then we know we have to get it from the database
and populate the map.

## Building the Graph - Design Choice
There is a design choice in how we build the graph. We could either build out the whole dependent graph and then do
the walking or we could build it incrementally as we walk.

Imagine a case where we the Pao update is adding a source. If the source does not change the Pao's effective
attribute set, then there is no need to walk the dependents.
That is probably a common case: the source has no policies set so there is no change.

If the Pao is changed, what are the chances that change will need to propagate to all of the dependents?
Or said another way, what are the chances that the dependents already have the policy set,
so this change will have no effect? I don't know.

There might be some advantage to reading everything from the database in one go.

From the implementation point of view, I think it is easier to construct the graph during the recursive walk 
rather than recurse once in the database and again in the walk.

## Walking the Graph - The Algorithm
Call the point of initial change the targetPao. The change may be an update to the targetPao's attribute set or it
may be linking a new source to the targetPao. We build a graph node for the targetPao; call it the targetNode and
launch the walk using targetNode as the inputNode:
1. Populate the source list of the inputNode. We read the object ids from the inputNode and get the source graph nodes 
from the Pao Map or by reading them from the database and constructing graph nodes for them.
2. Compute the new effective attribute set for the inputPao. If there are conflicts, store them and mark the graph node.
3. If the new effective attributes are the same as the existing effective attributes, we are done.
4. Otherwise, there is a change. Mark the graph node as modified. Walk the dependents:
5. Populate the dependents list of the inputNode. We do a database query to find all dependents with this Pao as
a source. Then we either build new graph nodes or we use existing (possibly already modified) graph nodes in the Pao Map.
6. For each dependent, recurse to step 1. We know that the current, modified graph node will be a source of the
dependent, so will be considered in the computation of its new effective attribute set.

The result is a depth-first graph walk, building out the dependent nodes as we walk, and marking the graph nodes
that hold modified Paos. At this point we have a Pao map with graph nodes. The nodes are marked so we know if
they have been modified and if they have new conflicts.

## Performing the Update
The update mode controls what we do next. In all cases we will return policy conflicts from the walk and discard
the in-memory structures.

If the update mode is FAIL_ON_CONFLICT and there are no conflicts, OR if the update mode is ENFORCE_CONFLICTS,
then we will update the database.

The database update is driven from the Pao Map. We can scan the entries and perform database updates for all
graph nodes that have been modified. This update should happen in a single transaction, so we probably want
to gather that list of modified Paos and ask the PaoDao to perform all of the updates.

## Concurrency Control
There are two approaches to concurrency control we could take.

### Option 1: Big Transaction
We could make a database transaction with the same scope as running the update algorithm. The database reads and update
would all happen in the same transaction. That requires running the transaction outside of the DAO code and
holding it open for the whole process.

The potential problem with the big transaction approach is limiting concurrent policy transactions due to internal
concurrency controls in Postgres. Unrelated policy updates could queue and be actually serialized in the database.

### Option 2: Pao Versioning
We could give each PAO in the database a monotonically increasing version number. Updating a PAO would mean reading
the PAO and checking that the incoming version matches the current version; that is, no other changes happened.
This is the same concept as etags in GCP ACLs.

The potential problem with versioning is that it would allow a concurrent change, causing the update to fail and
need to be redone.

I think it is reasonable to start with the Big Transaction approach and let the database do its thing. If we
experience concurrency issues from unrelated changes, then we can implement the versioning approach.
