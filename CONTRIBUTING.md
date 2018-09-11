## Contributing

First of all: thanks for contributing to TrackFind! We really appreciate it.

### 1. Have you got some nice ideas to implement?

If you have some propositions, feel free to [create an issue](https://github.com/elixir-no-nels/trackfind/issues/new) containing feature-request.

### 2. Did you find a bug?

* **Ensure the bug was not already reported** by [searching all
  issues](https://github.com/elixir-no-nels/trackfind/issues?q=).

* If you're unable to find an open issue addressing the problem, [open a new
  one](https://github.com/elixir-no-nels/trackfind/issues/new).  Be sure to
  include a **title and clear description**, as much relevant information as
  possible, and a **code sample** or an **executable test case** demonstrating
  the expected behavior that is not occurring.

* If bug is UI-related, **screenshots are mandatory**.

### 3. Fork & create a branch

If this is something you think you can fix, then
[fork TrackFind](https://help.github.com/articles/fork-a-repo)
and create a branch with a descriptive name.

A good branch name would be (where issue #325 is the ticket you're working on):

```sh
git checkout -b 325-fix-parsing-json
```

### 4. Get the test suite running

Make sure you're using correct Maven version and appropriate Java version.

You should be able to run the tests using:

```sh
mvn clean test
```

If your tests are passing locally but they're failing on Travis, reset your test
environment.

### 5. Test your changes manually

You should be able to run application as follows:

```sh
mvn clean spring-boot:run
```

Next navigate to http://localhost:8080 and perform a smoke-test: make sure that all basic functionality is working properly.

### 6. Keeping your Pull Request updated

If a maintainer asks you to "rebase" your PR, they're saying that a lot of code
has changed, and that you need to update your branch so it's easier to merge.

To learn more about rebasing in Git, there are a lot of
[good](http://git-scm.com/book/en/Git-Branching-Rebasing)
[resources](https://help.github.com/articles/interactive-rebase),
but here's the suggested workflow:

```sh
git checkout 325-fix-parsing-json
git pull --rebase upstream master
git push --force-with-lease 325-fix-parsing-json
```
