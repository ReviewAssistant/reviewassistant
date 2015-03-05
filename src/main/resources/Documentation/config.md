Configuration
=============

The configuration of the @PLUGIN@ plugin is done on project level in
the `@PLUGIN@.config` file of the project. Missing values are inherited
from the parent projects. This means a global default configuration can
be done in the `@PLUGIN@.config` file of the `All-Projects` root project.
Other projects can then override the configuration in their own
`@PLUGIN@.config` file.

```
  [reviewers]

    autoAddReviewers = true
    maxReviewers = 3
    enableLoadBalancing = false
    plusTwoRequired = true
    plusTwoAge = 8
    plusTwoLimit = 10


  [time]
    reviewTimeModifier = 100

```

reviewers.maxReviewers
:   The maximum number of reviewers that should automatically be added to a change.

	By default 3.

reviewers.enableLoadBalancing
:   If loadbalancing is enabled, reviewers' other reviews are taken into account, and those with
    fewer other reviews are favored.

	  By default false.

    The query used for finding suitable +2 accounts is

    status:merged -age:<plusTwoAge>weeks limit:<plusTwoLimit> -label:Code-Review=2,<Change Owner>
    label:Code-Review=2 project:<Project Name>

    This query has a potential to slow down performance, but care has been taken to choose sensible defaults.
    The query returns a list of changes, whenever one of the conditions age or limit is fulfilled. The
    account that +2'd the change is then considered as a candidate to review the new change.

reviewers.plusTwoAge
:   How far back (in weeks) in history to look.

    By default 8.

reviewers.plusTwoLimit
:   How many changes to take.

    By default 10.

reviewers.plusTwoRequired
:   Whether a user with merge rights is required to be added as a reviewer.

    By default true.

reviewers.autoAddReviewer
:   If reviewers should be added to a change. If disabled, only review advice is given.

    By default true.

time.reviewTimeModifier
:   Modifier for total review time in percentage. Also affects sessions indirectly.
    Value of 50 will cut the review time in half.

    By default 100.
