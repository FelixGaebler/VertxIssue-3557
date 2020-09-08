# Vertx Issue Reproducer

## GraphQL Queries:

**Create**
```graphql
mutation {
  createSign(
    input: {
      name: "Loadwaawdading"
      frames: [
        { line0: "", line1: "Loading", line2: "o O o", line3: "" }
        { line0: "", line1: "Loading", line2: "o o O", line3: "" }
        { line0: "", line1: "Loading", line2: "o O o", line3: "" }
        { line0: "", line1: "Loading", line2: "O o o", line3: "" }
      ]
    }
  ) {
    name
    frames {
      line0
      line1
      line2
      line3
    }
  }
}
```

**List**
```graphql
query {
  signs {
    name
    frames {
      line0
      line1
      line2
      line3
    }
  }
}
```

**Subscribe**
```graphql
subscription {
  subscribeSigns {
    name
    frames {
      line0
      line1
      line2
      line3
    }
  }
}

```