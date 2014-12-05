Configuration
=============

The configuration of the @PLUGIN@ plugin is done on project level in
the `@PLUGIN@.config` file of the project. Missing values are inherited
from the parent projects. This means a global default configuration can
be done in the `@PLUGIN@.config` file of the `All-Projects` root project.
Other projects can then override the configuration in their own
`@PLUGIN@.config` file.

```
  [plugin "reviewassistant"]
    maxReviewers = 2
```

reviewers.maxReviewers
:	The maximum number of reviewers that should automatically be added to a change.

	By default 2.
