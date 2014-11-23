# ReviewAssistant for Gerrit Code Review

**Note that ReviewAssistant is developed against Gerrit v2.10-rc0. At this time, no other versions are supported.**

ReviewAssistant is a plugin gor Gerrit Code Review. ReviewAssistant gives advice to reviewers on how the review should be
performed to be as effective as possible. The review time suggestions are based on the following rules:

* No review should be shorter than 5 minutes.
* Five lines per minute is considered to be the optimum review speed.
* Reviewers should not spend more than 60 minutes reviewing. If the review is expected to take longer, it is recommended
 to split the review into several sessions.