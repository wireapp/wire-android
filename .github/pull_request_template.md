----
#### PR Submission Checklist for internal contributors

- The **PR Title**
    - [ ] conforms to the style of semantic commits messages¹ supported in Wire's Github Workflow²
    - [ ] contains a reference JIRA issue number like `SQPIT-764`
    - [ ] answers the question: _If merged, this PR will: ..._ ³

- The **PR Description**
    - [ ] is free of optional paragraphs and you have filled the relevant parts to the best of your ability
----

# What's new in this PR?

### Issues

_Briefly describe the issue you have solved or implemented with this pull request. If the PR contains multiple issues, use a bullet list._

### Causes (Optional)

_Briefly describe the causes behind the issues. This could be helpful to understand the adopted solutions behind some nasty bugs or complex issues._

### Solutions

_Briefly describe the solutions you have implemented for the issues explained above._

### Dependencies (Optional)

_If there are some other pull requests related to this one (e.g. new releases of frameworks), specify them here._

Needs releases with:

- [ ] GitHub link to other pull request

### Testing

#### Test Coverage (Optional)

- [ ] I have added automated test to this contribution

#### How to Test

_Briefly describe how this change was tested and if applicable the exact steps taken to verify that it works as expected._

### Notes (Optional)

_Specify here any other facts that you think are important for this issue._

### Attachments (Optional)

_Attachments like images, videos, etc. (drag and drop in the text box)_ 
<!-- Uncomment the brackets and place your screenshots on the table below. -->
<!--
| Before | After |
| ----------- | ------------ |
|   |   |
-->

----
#### PR Post Submission Checklist for internal contributors (Optional)

- [ ] Wire's Github Workflow has automatically linked the PR to a JIRA issue
----
#### PR Post Merge Checklist for internal contributors

- [ ] If any soft of configuration variable was introduced by this PR, it has been added to the relevant documents and the CI jobs have been updated.
----
##### References
1. https://sparkbox.com/foundry/semantic_commit_messages
1. https://github.com/wireapp/.github#usage
1. E.g. `feat(conversation-list): Sort conversations by most emojis in the title #SQPIT-764`.
