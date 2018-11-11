# How to run

```
# Build
mvn package
# Run test
mvn test
# Run app
java com.github.miltonwright.fly.App
# Access app
curl http://localhost:8080/flyavganger
```

### Parameters

The following parameters are supported as java properties
  - `com.github.miltonwright.fly.daysAgo` - days ago. supported values `0` and `1`.  Default is `1`.
  - `com.github.miltonwright.fly.airport` - IATA airport code. Default is `OSL`.
  - `com.github.miltonwright.fly.port` - port. Default is `8080`.

# TODO

4. Legg til et RESTful endepunkt hvor man kan definere flyplass og tidsrom og får output i JSON format.
