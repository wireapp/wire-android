# 3. Use parameterizable tests in JUnit5

Date: 2024-08-05

## Status

Accepted

## Context

Sometimes we need to write multiple tests for the same scenario, changing only the input values.

## Decision

We will use parameterizable tests in JUnit5 to avoid writing multiple tests for the same scenario.

## Consequences

- Introduction of `@ParameterizedTest` annotation in the test class
  and [library](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests).
- The test method will receive the parameters as arguments.
