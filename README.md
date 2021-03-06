# ReviewAssistant for Gerrit Code Review

**Note that ReviewAssistant is developed against Gerrit v2.10-rc0. At this time, no other versions are supported.**

ReviewAssistant is a plugin for Gerrit Code Review. ReviewAssistant gives advice to reviewers on how the review should be
performed to be as effective as possible. The review time suggestions are based on the following rules:

* No review should be shorter than 5 minutes.
* Five lines per minute is considered to be the optimum review speed.
* Reviewers should not spend more than 60 minutes reviewing. If the review is expected to take longer, it is recommended
 to split the review into several sessions.

ReviewAssistant is also capable of adding reviewers automatically, based on:

* Git blame
* Submit history - Users with merge rights.
* Open changes - Users with more open changes are less likely to be chosen as reviewer.

## Credits

The rules are based on Jenkins ReviewBuddy by switchgears.

* [ReviewBuddy presentation on slideshare](http://www.slideshare.net/AskeOlsson/jenkins-review-buddy)
* [switchgears on github](https://github.com/switchgears/gerrit-review-buddy)

Other notable sources of inspiration include

* [reviewers-by-blame plugin for Gerrit](https://gerrit.googlesource.com/plugins/reviewers-by-blame/)
* [reviewers plugin for Gerrit](https://gerrit.googlesource.com/plugins/reviewers/)
