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
    maxReviewers = 3
    enableLoadBalancing = false
    plusTwoAge = 8
    plusTwoLimit = 10
```

reviewers.maxReviewers
:	The maximum number of reviewers that should automatically be added to a change.

	By default 3.

reviewers.enableLoadBalancing
:	If loadbalancing is enabled, reviewers' other reviews are taken into acount, and those with
fewer other reviews are favored.

	By default false.

The query used for finding suitable +2 accounts is

    status:merged -owner:<Change Owner> -age:<plusTwoAge>weeks limit:<plusTwoLimit> label:Code-Review=2 project:<Project Name>
This query has a potential to slow down performance, but care has been taken to choose sensible defaults.
The query returns a list of changes, whenever one of the conditions age or limit is fulfilled. The
account that +2'd the change is then considered as a candidate to review the new change.

reviewers.plusTwoAge
:   How far back (in weeks) in history to look.

    By default 8.

reviewers.plusTwoLimit
:   How many changes to take.

    By default 10.
