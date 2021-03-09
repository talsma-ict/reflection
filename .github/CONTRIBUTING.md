# Contributing to reflection

:+1: Thank you for taking the time to contribute! :+1:

The following is a set of guidelines for contributing to this Talsma ICT open-source project, hosted on GitHub. 
These are largely based on the guidelines from other open-source projects.
Please read this document as guidelines instead of rules.
Use your best judgment and feel free to propose changes to this document in a pull request.

## Code of Conduct

This project and everyone participating in it is governed by the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code.
Please report unacceptable behaviour to [info@talsma-ict.nl](mailto:info@talsma-ict.nl).

## How can I contribute?

### Reporting bugs

Although we hate bugs, we love people taking the trouble to report them! :+1:  

- Before creating bug reports, please check 
  [whether it already exists](https://github.com/talsma-ict/reflection/issues?q=is%3Aissue)
  as you might find out that you don't need to create one.  
  If a report already exists **and it is still open**, please add a comment to this issue 
  instead of opening a new one.  
  In case is was already closed, feel free to open a new issue and link to the closed issue in the description.
- When you _do_ create a bug report, please include as many details as possible.

### How do I submit good bug report?

Bugs are tracked as [GitHub issues](https://guides.github.com/features/issues/).  
New issues can be [created here](https://github.com/talsma-ict/reflection/issues/new).
Explain the problem and include additional details to help maintainers reproduce the problem:

- **Use a clear and descriptive title** for the issue to identify the problem.
- **Describe the exact steps which reproduce the problem** in as many details as possible. When listing steps, _don't just say what you did, but explain how you did it_.
- **Provide specific examples to demonstrate the steps**. Include links to files or GitHub projects, or copy/pasteable snippets, which you use in those examples. If you're providing snippets in the issue, use [Markdown code blocks](https://help.github.com/articles/markdown-basics/#multiple-lines).
- **Describe the behavior you observed after following the steps** and point out what exactly is the problem with that behavior.
- **Explain which behavior you expected to see instead and why.**

If you can, please consider [creating a pull-request](https://github.com/talsma-ict/reflection/compare) 
in which you add a (failing) unit-test that points out the bug.

### Pull requests

If you have code or a bugfix you would like to contribute,
please feel free to create a [GitHub pull request](https://help.github.com/articles/creating-a-pull-request-from-a-fork/)
from [your own fork](https://help.github.com/articles/about-forks/) of this repository.
Please make sure to request the merge towards the `develop` branch.

### Signing off

Each contribution should be signed off under 
the [Developer Certificate of Origin](https://developercertificate.org) (DCO)
using the `--signoff` [git option](https://git-scm.com/docs/git-commit#Documentation/git-commit.txt---signoff)
(or its `-s` shorthand).

Contributions will be checked by the [github DCO bot](https://probot.github.io/apps/dco/).

## License

By contributing your code, you license it under the terms of the APLv2: 
https://github.com/talsma-ict/reflection/blob/master/LICENSE

All files are released with the Apache 2.0 license.

If you are adding a new file it should have a header like below.  
This can be automatically added by running `./mvnw license:format`

```
/*
 * Copyright 2016-2019 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```
