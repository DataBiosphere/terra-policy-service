# Implementation Notes

These implementation notes assume you are familiar with the
[Policy Design](https://docs.google.com/document/d/1dRtE_nZf8e231DZmk_-9CDbjYTiIN2ZZwI95uF1w2pg/edit#heading=h.1uc5ubqdd419)

## Updating a Policy Attribute Object (PAO)
A PAO update is a proposed change to the attribute set of the PAO. The implementation needs to evaluate the effect
of the change on the policy graph and, depending on the update mode make changes to the graph.

The update mode can be:
- DRY_RUN - evaluate the change, but do not apply it
- FAIL_ON_CONFLICT - evaluate the change and apply it if there are no new conflicts
- ENFORCE_CONFLICT - evaluate the change and apply it, annotating dependent PAOs as having a conflict.
TPS does not define the semantics of what components do if their policy is in conflict.

Since the evaluated changes may be tossed, we need to build an in-memory structure of the PAOs.
The policy graph is a DAG, but not a strict hierarchy; that is, we can have a graph that looks like this:
```
  Pao:A --------------> Pao:C
    |                     ^
    |                     |
     -------> Pao:B ------
```
The pointers represent source-to-dependent links. So Pao:B has Pao:A as a source.
Pao:C has both Pao:A and Pao:B as sources. Or said the other way around,
Pao:A has Pao:B and Pao:C as dependents. Pao:B has Pao:C as a dependent.

Since there may be multiple paths to the same dependent, we
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
- initial pao - the initial state of the Pao from the database
- modified pao - the proposed modification of the Pao; but with an empty conflict set
- dependents - list of dependent graph nodes
- sources - list of source graph nodes
- modified - boolean indicating the Pao has been modified
- newConflict - a new conflict has been found 

### Pao Map
This is an in-memory Map:
- key - object id of a Pao
- value - graph node

As we walk the graph, we need to lookup source and dependent PAOs by object id. When we do that lookup we first
look in the Pao Map to see if we have already read the PAO from the database. If it is not there, then we read
from the database, build a graph node and add it to the Pao Map.

We may revisit the same PAO multiple times in the graph walk. Those PAOs may have changes from the update.
We want to be sure we are evaluating the update based on _updated_ PAOs and not the version in the database.

### Policy Conflict List
After a graph walk evaluating an update to a PAO, we make a pass through the Pao Map and see if we have
found any new conflicts. If so:
- We mark the graph node to ensure the PAO is viewed as modified
- We collect the conflicts in a list to be reported back through the API

**NOTE**: I am not confident that the current form of PolicyConflict is its final form. We will need
feedback from TPS clients to tune it up.

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
2. Compute the new effective attribute set for the inputPao by evaluating the modifiedPao of each source node.
For each source node, for each policy input:
    1. If the source policy input is not in the inputPao effective attribute set, then add it
    2. If the source policy input is in the inputPao effective attribute set, then call the _combiner_ for the
policy type. Store the result of that, and any conflicts from that combination, into the inputPao effective attribute set.
3. If the new effective attributes are the same as the existing effective attributes, we are done.
4. Otherwise, there is a change. Mark the graph node as modified. We need to propagate the modification to
any dependents of this PAO. We walk the dependents like this:
   1. Populate the dependents list of the inputNode. We do a database query to find all dependents with this Pao as
a source. Then we either build new graph nodes or we use existing (possibly already modified) graph nodes in the Pao Map.
   2. For each dependent, recurse to step 1. We know that the current, modified graph node will be a source of the
dependent, so will be considered in the computation of its new effective attribute set.

The result is a depth-first graph walk, building out the dependent nodes as we walk, and marking the graph nodes
that hold modified Paos. At this point we have a Pao map with graph nodes. The nodes are marked so we know if
they have been modified.

Once the graph has been walked, we check for new conflicts. We iterate over the Pao Map looking at each
graph node, and then iterate over the new effective attributes comparing their conflict list with the
initial Pao conflict list. If they are different, we mark the graph node as having new conflicts. That
ensures it is written back to the database even if there is no change to effective policies. We also
mint a new PolicyConflict object and add it to the list; those are returned to the caller.

**TODO**: Make sure that a policy change that _removes_ a conflict gets flagged for writing back, but does
not generate a new PolicyConflict object. Maybe newConflict is a misnomer; it may just mean changed conflict.

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
